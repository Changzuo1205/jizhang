package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.DailyToolboxDatabase
import com.example.data.local.TransactionType
import com.example.data.local.entity.AccountEntityV2
import com.example.data.local.entity.BalanceHistoryEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.model.AmountFormatter
import kotlinx.coroutines.flow.Flow

/**
 * 数据仓库层 v2：协调交易与账户的规范化更新。
 *
 * 与旧版 [已删除] ToolboxRepository 的关键差异：
 * - 账户不再保存「随交易累加」的实时余额；实时余额由 ViewModel 映射层按
 *   「initial_balance ± Σ未删除交易」派生 —— 删除/软删任何记录都自动保持守恒，
 *   从根上消灭旧版双路径漂移问题
 * - 所有跨表写操作包进 [withTransaction]
 * - 对 UI 过渡期提供「遗留形态」适配方法（以元为单位的 DTO 参数），Phase 3 后收敛
 */
class ToolboxRepositoryV2(private val db: DailyToolboxDatabase) {

    private val transactionDao get() = db.transactionDao()
    private val accountDao get() = db.accountDao()
    private val categoryDao get() = db.categoryDao()
    private val bookDao get() = db.bookDao()

    // ---------- 观察流 ----------

    fun observeActiveTransactions(): Flow<List<TransactionEntity>> = transactionDao.observeActive()
    fun observeActiveAccounts(): Flow<List<AccountEntityV2>> = accountDao.observeActive()
    fun observeActiveCategories(): Flow<List<CategoryEntity>> = categoryDao.observeActive()

    /** 默认账本（记账/导入的挂靠点）；种子保证存在，缺失视为初始化异常 */
    suspend fun defaultBookId(): Long =
        bookDao.getDefault()?.id ?: error("默认账本不存在：数据库种子未完成初始化")

    // ---------- 交易写入（遗留 DTO 形态入口） ----------

    /**
     * 以旧版字段签名落一笔交易。分类 id 通过名称解析：
     * 存在二级则取二级 id，否则取一级 id；完全未命中则置空（SET_NULL 可空设计）。
     */
    suspend fun insertLegacyExpense(
        type: String,
        category: String,
        subCategory: String,
        amountYuan: Double,
        note: String,
        accountId: Long,
        timestamp: Long
    ): Long {
        val txType = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE)
        val categoryId = resolveCategoryId(txType, category, subCategory)
        val entity = TransactionEntity(
            userId = USER_ID_LOCAL,
            bookId = defaultBookId(),
            accountId = accountId,
            type = txType,
            amount = AmountFormatter.yuanToCents(amountYuan),
            categoryId = categoryId,
            note = note.ifBlank { null },
            occurredAt = timestamp
        )
        return transactionDao.insert(entity)
    }

    /** 更新旧版编辑对（old/new 均为过渡 DTO），保留行 uuid 与创建信息 */
    suspend fun updateLegacyExpense(old: ExpenseSnapshot, new: ExpenseSnapshot) {
        val existing = transactionDao.getById(old.id) ?: return
        val txType = runCatching { TransactionType.valueOf(new.type) }.getOrDefault(existing.type)
        val categoryId = resolveCategoryId(txType, new.category, new.subCategory)
        transactionDao.update(
            existing.copy(
                type = txType,
                amount = AmountFormatter.yuanToCents(new.amount),
                categoryId = categoryId,
                note = new.note.ifBlank { null },
                accountId = new.accountId,
                occurredAt = new.dateTimestamp,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** 软删除交易 */
    suspend fun softDeleteTransaction(id: Long) {
        transactionDao.softDelete(id, System.currentTimeMillis())
    }

    // ---------- 账户操作 ----------

    /** 新增账户（基准余额以元传入） */
    suspend fun addAccount(
        name: String,
        legacyType: String,
        initialBalanceYuan: Double,
        colorHex: String,
        note: String
    ): Long {
        val existingCount = accountDao.count()
        return accountDao.insert(
            AccountEntityV2(
                uuid = java.util.UUID.randomUUID().toString().replace("-", ""),
                userId = USER_ID_LOCAL,
                bookId = defaultBookId(),
                name = name,
                type = legacyTypeToV2(name, legacyType),
                initialBalance = AmountFormatter.yuanToCents(initialBalanceYuan),
                color = colorHex,
                sortOrder = existingCount
            )
        )
    }

    /**
     * 余额校准：插入一笔「漏记款」调整记录把实时余额推到目标值。
     * 补足方向记为收入、回落方向记为支出；initial_balance 保持不动 ——
     * 用户显式设置基准余额时应走 [recordInitialBalance]。
     */
    suspend fun calibrateToBalance(accountId: Long, targetBalanceYuan: Double) = db.withTransaction {
        calibrateToBalanceLocked(accountId, targetBalanceYuan)
    }

    /** 记录一次手动设置的基准余额快照 */
    suspend fun recordInitialBalance(accountId: Long, balanceCents: Int, note: String?) {
        accountDao.updateInitialBalance(accountId, balanceCents, System.currentTimeMillis())
        db.balanceHistoryDao().insert(
            BalanceHistoryEntity(
                accountId = accountId,
                balance = balanceCents,
                effectiveAt = System.currentTimeMillis(),
                note = note
            )
        )
    }

    /** 归档账户（替代旧的物理删除：RESTRICT 外键保护历史交易引用） */
    suspend fun archiveAccount(accountId: Long) = accountDao.archive(accountId, System.currentTimeMillis())

    suspend fun getAccountById(accountId: Long): AccountEntityV2? = accountDao.getById(accountId)

    // ---------- 内部工具 ----------

    /**
     * 更新账户元信息，并在展示余额变化时插入「漏记款」校准记录。
     * 事务内先改元信息再校准，保证余额派生口径一致。
     */
    suspend fun updateAccountMeta(
        accountId: Long,
        name: String,
        legacyType: String,
        colorHex: String,
        note: String,
        targetBalanceYuan: Double?,
        previousBalanceYuan: Double
    ) = db.withTransaction {
        val row = accountDao.getById(accountId) ?: return@withTransaction
        accountDao.upsert(
            row.copy(
                name = name,
                type = legacyTypeToV2(name, legacyType),
                color = colorHex,
                updatedAt = System.currentTimeMillis()
            )
        )
        if (targetBalanceYuan != null &&
            kotlin.math.abs(targetBalanceYuan - previousBalanceYuan) > 0.001
        ) {
            calibrateToBalanceLocked(accountId, targetBalanceYuan)
        }
    }

    /** 校准核心（须在事务内调用） */
    private suspend fun calibrateToBalanceLocked(accountId: Long, targetBalanceYuan: Double) {
        val account = accountDao.getById(accountId) ?: return
        val currentCents = derivedBalanceCents(accountId, account.initialBalance)
        val targetCents = AmountFormatter.yuanToCents(targetBalanceYuan)
        val diffCents = targetCents - currentCents
        if (diffCents == 0) return

        val topUp = diffCents > 0
        val categoryId = if (topUp)
            resolveCategoryId(TransactionType.INCOME, "漏记款", "漏记款")
        else
            resolveCategoryId(TransactionType.EXPENSE, "漏记款", "漏记款")

        transactionDao.insert(
            TransactionEntity(
                userId = USER_ID_LOCAL,
                bookId = account.bookId,
                accountId = accountId,
                type = if (topUp) TransactionType.INCOME else TransactionType.EXPENSE,
                amount = kotlin.math.abs(diffCents),
                categoryId = categoryId,
                note = "余额调整产生的差额",
                occurredAt = System.currentTimeMillis()
            )
        )
    }

    /** 实时余额（分）＝ 基准 + Σ收入 − Σ支出 − Σ转出 + Σ转入 */
    suspend fun derivedBalanceCents(accountId: Long, initialCents: Int): Int =
        initialCents +
            transactionDao.lifetimeNetCentsFor(accountId) +
            transactionDao.transferInTotalFor(accountId) -
            transactionDao.transferOutTotalFor(accountId)

    private suspend fun resolveCategoryId(
        type: TransactionType,
        categoryName: String,
        subCategory: String
    ): Long? {
        if (categoryName.isBlank()) return null
        val dbType = type.name.lowercase()
        val parent = categoryDao.findParentByName(categoryName.trim(), dbType) ?: return null
        if (subCategory.isBlank()) return parent.id
        return categoryDao.findChildByName(subCategory.trim(), parent.id)?.id ?: parent.id
    }

    data class ExpenseSnapshot(
        val id: Long,
        val type: String,
        val category: String,
        val subCategory: String,
        val amount: Double,
        val note: String,
        val dateTimestamp: Long,
        val accountId: Long
    )

    companion object {
        /** 单机固定用户 id（user 表种子恒定） */
        const val USER_ID_LOCAL = 1L

        /** 展示层旧类型常量 ↔ 新小写类型 的映射（icon/颜色资源依赖旧常量名） */
        fun legacyTypeToV2(name: String, legacyType: String): String = when {
            name in setOf("基金", "黄金", "币安") -> "investment"
            else -> when (legacyType.uppercase()) {
                "WECHAT" -> "wechat"
                "ALIPAY" -> "alipay"
                "BANK_CARD", "DEBIT" -> "debit"
                "CREDIT_CARD" -> "credit"
                "CASH" -> "cash"
                else -> "other"
            }
        }

        fun v2TypeToLegacy(v2Type: String): String = when (v2Type.lowercase()) {
            "wechat" -> "WECHAT"
            "alipay" -> "ALIPAY"
            "debit" -> "BANK_CARD"
            "credit" -> "CREDIT_CARD"
            "cash" -> "CASH"
            "investment" -> "INVESTMENT"
            else -> "OTHER"
        }
    }
}
