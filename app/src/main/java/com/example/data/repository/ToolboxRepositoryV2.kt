package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.DailyToolboxDatabase
import com.example.data.local.TransactionType
import com.example.data.local.entity.AccountEntityV2
import com.example.data.local.entity.BalanceHistoryEntity
import com.example.data.local.entity.BookEntity
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
    fun observeActiveBooks(): Flow<List<BookEntity>> = bookDao.observeActive()

    /** 默认账本（记账/导入的挂靠点）；种子保证存在，缺失视为初始化异常 */
    suspend fun defaultBookId(): Long =
        bookDao.getDefault()?.id ?: error("默认账本不存在：数据库种子未完成初始化")

    // ---------- 交易写入（遗留 DTO 形态入口） ----------

    /**
     * 以旧版字段签名落一笔交易。分类 id 通过智能解析与动态创建：
     * 存在二级则取二级 id，否则取一级 id；支持复合分类名与模糊别名匹配。
     */
    suspend fun insertLegacyExpense(
        type: String,
        category: String,
        subCategory: String,
        amountYuan: Double,
        note: String,
        accountId: Long,
        timestamp: Long,
        transferToAccountId: Long? = null,
        bookId: Long? = null
    ): Long {
        val txType = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE)
        val targetBook = bookId ?: defaultBookId()
        val categoryId = if (txType == TransactionType.TRANSFER) {
            null
        } else {
            resolveOrCreateCategoryId(txType, category, subCategory, targetBook)
        }
        val entity = TransactionEntity(
            userId = USER_ID_LOCAL,
            bookId = targetBook,
            accountId = accountId,
            transferToAccountId = if (txType == TransactionType.TRANSFER) transferToAccountId?.takeIf { it != accountId } else null,
            type = txType,
            amount = AmountFormatter.yuanToCents(amountYuan),
            categoryId = categoryId,
            note = note.ifBlank { null },
            occurredAt = timestamp
        )
        return transactionDao.insert(entity)
    }

    /**
     * 更新旧版编辑对（old/new 均为过渡 DTO），保留行 uuid 与创建信息。
     * 转账类型：分类字段保持为空，以 [ExpenseSnapshot.transferToAccountId] 维护对端账户。
     */
    suspend fun updateLegacyExpense(old: ExpenseSnapshot, new: ExpenseSnapshot, bookId: Long? = null) {
        val existing = transactionDao.getById(old.id) ?: return
        val txType = runCatching { TransactionType.valueOf(new.type) }.getOrDefault(existing.type)
        val isTransfer = txType == TransactionType.TRANSFER
        val targetBook = bookId ?: existing.bookId
        val categoryId =
            if (isTransfer) null else resolveOrCreateCategoryId(txType, new.category, new.subCategory, targetBook)
        val targetAccountId =
            if (isTransfer && new.transferToAccountId != 0L) new.transferToAccountId
            else if (isTransfer) existing.transferToAccountId
            else null
        transactionDao.update(
            existing.copy(
                type = txType,
                amount = AmountFormatter.yuanToCents(new.amount),
                categoryId = categoryId,
                transferToAccountId = targetAccountId,
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

    /**
     * 落一笔账户间转账：双端复用同一条记录（amount 恒为正数），
     * 出账端为 [accountId]、入账端记入 transferToAccountId，分类置空。
     * 余额派生公式（基准 ± Σ交易 含转账对冲）保证两端自动守恒，无需手工改余额。
     */
    suspend fun addTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amountYuan: Double,
        note: String?,
        timestamp: Long = System.currentTimeMillis(),
        bookId: Long? = null
    ): Long {
        val entity = TransactionEntity(
            userId = USER_ID_LOCAL,
            bookId = bookId ?: defaultBookId(),
            accountId = fromAccountId,
            transferToAccountId = toAccountId,
            type = TransactionType.TRANSFER,
            amount = AmountFormatter.yuanToCents(amountYuan),
            categoryId = null,
            note = note?.takeIf { it.isNotBlank() },
            occurredAt = timestamp
        )
        return transactionDao.insert(entity)
    }

    // ---------- 账户操作 ----------

    /** 新增账户（基准余额以元传入） */
    suspend fun addAccount(
        name: String,
        legacyType: String,
        initialBalanceYuan: Double,
        colorHex: String,
        note: String,
        bookId: Long? = null
    ): Long {
        val targetBook = bookId ?: defaultBookId()
        val existingCount = accountDao.count()
        return accountDao.insert(
            AccountEntityV2(
                uuid = java.util.UUID.randomUUID().toString().replace("-", ""),
                userId = USER_ID_LOCAL,
                bookId = targetBook,
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

    // ---------- 分类管理（Phase 2 可视化维护入口） ----------

    /**
     * 新增分类：[parentName] 为空即一级分类；否则挂靠到同名一级（支出/收入内）。
     * type 使用 "expense"/"income"（大小写不敏感）。uuid 复用种子的
     * nameUUIDFromBytes 方案，同名同级自动去重复用既有行。
     *
     * @return 新插入或已存在分类的 id；参数非法（空名/父级缺失）返回 -1
     */
    suspend fun insertCategory(
        parentName: String?,
        name: String,
        type: String,
        colorHex: String? = null
    ): Long = db.withTransaction {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return@withTransaction -1L
        val dbType = type.trim().lowercase().ifBlank { "expense" }
            .takeIf { it == "expense" || it == "income" } ?: return@withTransaction -1L

        val parentId = if (parentName.isNullOrBlank()) {
            null
        } else {
            categoryDao.findParentByName(parentName.trim(), dbType)?.id ?: return@withTransaction -1L
        }

        // 幂等：同名同级已存在（含种子内置与历史导入）直接复用
        val existing = if (parentId != null) {
            categoryDao.findChildByName(cleanName, parentId)
        } else {
            categoryDao.findParentByName(cleanName, dbType)
        }
        if (existing != null) return@withTransaction existing.id

        val uuidSeed = if (parentId != null) {
            "$dbType-${parentName!!.trim()}-$cleanName"
        } else {
            "$dbType-$cleanName"
        }
        categoryDao.insert(
            CategoryEntity(
                uuid = java.util.UUID.nameUUIDFromBytes(uuidSeed.toByteArray()).toString().replace("-", ""),
                userId = USER_ID_LOCAL,
                bookId = defaultBookId(),
                name = cleanName,
                parentId = parentId,
                type = dbType,
                color = colorHex?.takeIf { it.isNotBlank() },
                sortOrder = categoryDao.count()
            )
        )
    }

    /** 归档分类（软删除：历史交易外键经由 SET_NULL 仍可追溯，不再物理清除） */
    suspend fun archiveCategory(categoryId: Long) = db.withTransaction {
        categoryDao.archive(categoryId)
    }

    /** 更新分类元信息；传 null 表示保持对应字段不变 */
    suspend fun updateCategoryMeta(
        categoryId: Long,
        name: String? = null,
        colorHex: String? = null
    ) = db.withTransaction {
        val row = categoryDao.getById(categoryId) ?: return@withTransaction
        categoryDao.update(
            row.copy(
                name = name?.trim()?.takeIf { it.isNotEmpty() } ?: row.name,
                color = colorHex?.takeIf { it.isNotBlank() } ?: row.color
            )
        )
    }

    // ---------- 账本管理（Phase 2 多账本入口） ----------

    /** 观察未归档账本（默认账本置顶） */
    fun observeBooks(): Flow<List<BookEntity>> = bookDao.observeActive()

    /** 新建账本（默认挂单机用户；并自动预设基础账户） */
    suspend fun insertBook(name: String, currency: String = "CNY"): Long = db.withTransaction {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "账本名称不能为空" }
        val newBookId = bookDao.insert(
            BookEntity(
                uuid = java.util.UUID.randomUUID().toString().replace("-", ""),
                userId = USER_ID_LOCAL,
                name = cleanName,
                currency = currency.ifBlank { "CNY" },
                sortOrder = bookDao.count()
            )
        )
        // 为新账本预设标准资产账户（仅微信钱包与支付宝两个网络账户）
        val defaultAccounts = listOf(
            Triple("微信钱包", "wechat", "#07C160"),
            Triple("支付宝", "alipay", "#1677FF")
        )
        defaultAccounts.forEachIndexed { index, (accName, accType, color) ->
            accountDao.insert(
                AccountEntityV2(
                    uuid = java.util.UUID.randomUUID().toString().replace("-", ""),
                    userId = USER_ID_LOCAL,
                    bookId = newBookId,
                    name = accName,
                    type = accType,
                    initialBalance = 0,
                    color = color,
                    sortOrder = index
                )
            )
        }
        newBookId
    }

    /** 确保指定账本至少拥有基础可用账户 */
    suspend fun ensureBookAccounts(bookId: Long) = db.withTransaction {
        val existing = accountDao.getByBookId(bookId)
        if (existing.isEmpty()) {
            val defaultAccounts = listOf(
                Triple("微信钱包", "wechat", "#07C160"),
                Triple("支付宝", "alipay", "#1677FF")
            )
            defaultAccounts.forEachIndexed { index, (accName, accType, color) ->
                accountDao.insert(
                    AccountEntityV2(
                        uuid = java.util.UUID.randomUUID().toString().replace("-", ""),
                        userId = USER_ID_LOCAL,
                        bookId = bookId,
                        name = accName,
                        type = accType,
                        initialBalance = 0,
                        color = color,
                        sortOrder = index
                    )
                )
            }
        }
    }

    /** 清空指定账本的所有交易流水，并将该账本下所有账户余额归零 */
    suspend fun clearBookData(bookId: Long) = db.withTransaction {
        transactionDao.deleteByBookId(bookId)
        accountDao.resetBalancesByBookId(bookId, System.currentTimeMillis())
    }

    /** 彻底删除指定账本及其所有明细数据与独立账户 */
    suspend fun deleteBook(bookId: Long): Boolean = db.withTransaction {
        val allBooks = bookDao.getActiveOnce()
        if (allBooks.size <= 1) {
            // 最后一个账本不允许删除，避免应用失去主体账本
            return@withTransaction false
        }
        val isDefault = bookDao.getDefault()?.id == bookId
        transactionDao.deleteByBookId(bookId)
        accountDao.deleteByBookId(bookId)
        categoryDao.deleteByBookId(bookId)
        bookDao.deleteById(bookId)
        if (isDefault) {
            val remaining = bookDao.getActiveOnce().firstOrNull()
            if (remaining != null) {
                bookDao.setDefault(remaining.id, System.currentTimeMillis())
            }
        }
        true
    }

    /** 更新账本元信息；传 null 表示保持对应字段不变 */
    suspend fun updateBookMeta(bookId: Long, name: String? = null, currency: String? = null) =
        db.withTransaction {
            val row = bookDao.getById(bookId) ?: return@withTransaction
            bookDao.update(
                row.copy(
                    name = name?.trim()?.takeIf { it.isNotEmpty() } ?: row.name,
                    currency = currency?.takeIf { it.isNotBlank() } ?: row.currency,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

    /** 切换默认账本：事务内先清旧默认再置位，保证任意时刻仅一个默认 */
    suspend fun setDefaultBook(bookId: Long) = db.withTransaction {
        val now = System.currentTimeMillis()
        bookDao.clearDefaultFlag(now)
        bookDao.setDefault(bookId, now)
    }

    /**
     * 归档账本。默认账本是记账/导入的必选挂靠点，禁止归档；
     * 归档不影响其下既有交易（外键保留），余额统计仍以交易事实为准。
     */
    suspend fun archiveBook(bookId: Long): Boolean = db.withTransaction {
        if (bookDao.getDefault()?.id == bookId) return@withTransaction false
        bookDao.archive(bookId, System.currentTimeMillis())
        true
    }

    // ---------- 内部工具 ----------

    /**
     * 更新账户元信息，并在展示余额变化时校准。
     * [createCalibrationTx] 为 true 时插入「漏记款」交易；为 false 时直接更新 initialBalance。
     */
    suspend fun updateAccountMeta(
        accountId: Long,
        name: String,
        legacyType: String,
        colorHex: String,
        note: String,
        targetBalanceYuan: Double?,
        previousBalanceYuan: Double,
        createCalibrationTx: Boolean = true
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
            if (createCalibrationTx) {
                calibrateToBalanceLocked(accountId, targetBalanceYuan)
            } else {
                val currentCents = derivedBalanceCents(accountId, row.initialBalance)
                val targetCents = AmountFormatter.yuanToCents(targetBalanceYuan)
                val newInitialCents = row.initialBalance + (targetCents - currentCents)
                accountDao.updateInitialBalance(accountId, newInitialCents, System.currentTimeMillis())
            }
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
            resolveCategoryId(TransactionType.INCOME, "居家", "漏记款")
        else
            resolveCategoryId(TransactionType.EXPENSE, "居家", "漏记款")

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

    /**
     * 智能分类解析与动态落库：
     * 1. 自动拆分复合分类名（如 "餐饮/午餐", "餐饮 - 晚餐", "交通:地铁"）
     * 2. 数据库精确命中（一级分类/二级分类）
     * 3. 常见记账分类同义词与关键词语义字典智能归类
     * 4. 自定义/新分类自动在 category 表中创建，杜绝变成“未分类”
     */
    suspend fun resolveOrCreateCategoryId(
        type: TransactionType,
        rawCategory: String,
        rawSubCategory: String,
        bookId: Long? = null
    ): Long? {
        if (type == TransactionType.TRANSFER) return null
        val dbType = type.name.lowercase()
        val targetBook = bookId ?: defaultBookId()

        var catName = rawCategory.trim()
        var subName = rawSubCategory.trim()

        // 1. 拆分复合分类名
        val delimiters = listOf(" / ", "/", " - ", "-", " : ", ":", "：", " > ", ">", "_")
        for (d in delimiters) {
            if (catName.contains(d)) {
                val parts = catName.split(d, limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    catName = parts[0].trim()
                    if (subName.isBlank() || subName == "默认" || subName == "其他") {
                        subName = parts[1].trim()
                    }
                    break
                }
            }
        }

        if (catName.isBlank() && subName.isNotBlank()) {
            catName = subName
            subName = ""
        }

        if (catName.isBlank()) {
            return categoryDao.findFallbackCategory(dbType)?.id
                ?: categoryDao.findFirstByNameAndType("其他", dbType)?.id
        }

        // 2. 优先查一级分类匹配
        val parent = categoryDao.findParentByName(catName, dbType)
        if (parent != null) {
            if (subName.isNotBlank() && subName != "默认" && subName != catName) {
                val child = categoryDao.findChildByName(subName, parent.id)
                if (child != null) return child.id
                // 动态新建二级分类
                val uuidSeed = "$dbType-${parent.name}-$subName"
                return categoryDao.insert(
                    CategoryEntity(
                        uuid = java.util.UUID.nameUUIDFromBytes(uuidSeed.toByteArray()).toString().replace("-", ""),
                        userId = USER_ID_LOCAL,
                        bookId = targetBook,
                        name = subName,
                        parentId = parent.id,
                        type = dbType,
                        sortOrder = categoryDao.count()
                    )
                )
            }
            return parent.id
        }

        // 3. 查是否已有同名二级/通用分类（若为二级分类，直接关联返回）
        val existingMatch = categoryDao.findFirstByNameAndType(catName, dbType)
        if (existingMatch != null) {
            return existingMatch.id
        }

        // 3.1 查 subName 是否已有直接匹配项
        if (subName.isNotBlank() && subName != "默认" && subName != catName) {
            val subMatch = categoryDao.findFirstByNameAndType(subName, dbType)
            if (subMatch != null) {
                return subMatch.id
            }
        }

        // 4. 语义同义词与关键词词典映射
        val aliasMatch = mapCategoryKeywords(catName, subName, type)
        if (aliasMatch != null) {
            val (mappedParent, mappedChild) = aliasMatch
            val targetParent = categoryDao.findParentByName(mappedParent, dbType)
                ?: categoryDao.getById(
                    categoryDao.insert(
                        CategoryEntity(
                            uuid = java.util.UUID.nameUUIDFromBytes("$dbType-$mappedParent".toByteArray()).toString().replace("-", ""),
                            userId = USER_ID_LOCAL,
                            bookId = targetBook,
                            name = mappedParent,
                            parentId = null,
                            type = dbType,
                            sortOrder = categoryDao.count()
                        )
                    )
                )
            if (targetParent != null) {
                if (mappedChild.isNotBlank() && mappedChild != mappedParent) {
                    val child = categoryDao.findChildByName(mappedChild, targetParent.id)
                    if (child != null) return child.id
                    val childUuidSeed = "$dbType-${targetParent.name}-$mappedChild"
                    return categoryDao.insert(
                        CategoryEntity(
                            uuid = java.util.UUID.nameUUIDFromBytes(childUuidSeed.toByteArray()).toString().replace("-", ""),
                            userId = USER_ID_LOCAL,
                            bookId = targetBook,
                            name = mappedChild,
                            parentId = targetParent.id,
                            type = dbType,
                            sortOrder = categoryDao.count()
                        )
                    )
                }
                return targetParent.id
            }
        }

        // 5. 自定义分类动态落库，保证绝对被系统识别
        val newParentId = categoryDao.insert(
            CategoryEntity(
                uuid = java.util.UUID.nameUUIDFromBytes("$dbType-$catName".toByteArray()).toString().replace("-", ""),
                userId = USER_ID_LOCAL,
                bookId = targetBook,
                name = catName,
                parentId = null,
                type = dbType,
                sortOrder = categoryDao.count()
            )
        )
        if (subName.isNotBlank() && subName != "默认" && subName != catName) {
            return categoryDao.insert(
                CategoryEntity(
                    uuid = java.util.UUID.nameUUIDFromBytes("$dbType-$catName-$subName".toByteArray()).toString().replace("-", ""),
                    userId = USER_ID_LOCAL,
                    bookId = targetBook,
                    name = subName,
                    parentId = newParentId,
                    type = dbType,
                    sortOrder = categoryDao.count()
                )
            )
        }
        return newParentId
    }

    private fun mapCategoryKeywords(cat: String, sub: String, type: TransactionType): Pair<String, String>? {
        val s = "$cat $sub".trim()
        if (type == TransactionType.INCOME) {
            return when {
                s.contains("工资") || s.contains("薪") || s.contains("年终奖") || s.contains("底薪") || s.contains("劳务") -> Pair("工资薪水", "工资薪水")
                s.contains("兼职") || s.contains("外快") || s.contains("副业") || s.contains("众包") || s.contains("稿费") || s.contains("提成") -> Pair("兼职外快", "兼职外快")
                s.contains("利息") || s.contains("收益") || s.contains("余额宝") -> Pair("利息", "利息")
                s.contains("分红") || s.contains("股票") || s.contains("基金") || s.contains("理财") -> Pair("分红股票", "分红股票")
                s.contains("红包") || s.contains("转账收款") -> Pair("红包", "红包")
                s.contains("礼金") || s.contains("随礼") || s.contains("压岁") -> Pair("礼金", "礼金")
                s.contains("退款") || s.contains("返款") || s.contains("退货") || s.contains("押金") -> Pair("退款返款", "退款返款")
                s.contains("报销") || s.contains("差旅") -> Pair("报销款", "报销款")
                s.contains("福利") || s.contains("补贴") || s.contains("餐补") || s.contains("房补") -> Pair("福利补贴", "福利补贴")
                s.contains("生活费") -> Pair("生活费", "生活费")
                s.contains("公积金") -> Pair("公积金", "公积金")
                s.contains("营业") || s.contains("经营") || s.contains("店铺") || s.contains("商户") -> Pair("营业收入", "营业收入")
                s.contains("销售") || s.contains("闲鱼") || s.contains("转转") || s.contains("二手") || s.contains("货款") -> Pair("销售款", "销售款")
                s.contains("赔付") || s.contains("理赔") || s.contains("保险金") -> Pair("赔付款", "赔付款")
                s.contains("漏记") || s.contains("调整") || s.contains("平账") -> Pair("漏记款", "漏记款")
                else -> null
            }
        }

        // 支出类语义识别
        return when {
            // 餐饮类
            s.contains("早餐") || s.contains("早饭") || s.contains("早点") || s.contains("包子") || s.contains("油条") || s.contains("豆浆") -> Pair("餐饮", "早餐")
            s.contains("午餐") || s.contains("中餐") || s.contains("午饭") || s.contains("中饭") || s.contains("快餐") || s.contains("外卖") || s.contains("饿了么") || s.contains("美团外卖") || s.contains("堂食") -> Pair("餐饮", "午餐")
            s.contains("晚餐") || s.contains("晚饭") || s.contains("夜宵") || s.contains("宵夜") || s.contains("烧烤") || s.contains("火锅") || s.contains("大排档") || s.contains("自助") -> Pair("餐饮", "晚餐")
            s.contains("零食") || s.contains("水果") || s.contains("饮料") || s.contains("奶茶") || s.contains("咖啡") || s.contains("星巴克") || s.contains("瑞幸") || s.contains("甜品") || s.contains("面包") || s.contains("蛋糕") -> Pair("餐饮", "零食")
            s.contains("买菜") || s.contains("生鲜") || s.contains("蔬菜") || s.contains("粮油") || s.contains("调料") || s.contains("原料") || s.contains("肉类") || s.contains("海鲜") -> Pair("餐饮", "买菜原料")
            s.contains("餐饮") || s.contains("吃饭") || s.contains("美食") || s.contains("饭局") -> Pair("餐饮", "餐饮其他")

            // 交通类
            s.contains("打车") || s.contains("滴滴") || s.contains("网约车") || s.contains("出租") || s.contains("高德") || s.contains("曹操") || s.contains("T3") -> Pair("交通", "打车")
            s.contains("公交") || s.contains("巴士") -> Pair("交通", "公交")
            s.contains("地铁") || s.contains("轻轨") -> Pair("交通", "地铁")
            s.contains("加油") || s.contains("油费") || s.contains("充电") || s.contains("加气") -> Pair("交通", "加油")
            s.contains("停车") || s.contains("泊车") -> Pair("交通", "停车费")
            s.contains("火车") || s.contains("高铁") || s.contains("动车") || s.contains("12306") || s.contains("大巴") -> Pair("交通", "火车")
            s.contains("飞机") || s.contains("机票") || s.contains("航班") || s.contains("机场") -> Pair("交通", "飞机")
            s.contains("单车") || s.contains("自行车") || s.contains("哈啰") || s.contains("美团单车") -> Pair("交通", "自行车")
            s.contains("高速") || s.contains("过路") || s.contains("过桥") || s.contains("ETC") -> Pair("交通", "过路过桥")
            s.contains("保养") || s.contains("修车") || s.contains("洗车") || s.contains("车险") || s.contains("年检") || s.contains("驾照") -> Pair("交通", "保养维修")
            s.contains("交通") || s.contains("出行") || s.contains("路费") -> Pair("交通", "交通其他")

            // 购物类
            s.contains("衣服") || s.contains("鞋") || s.contains("包") || s.contains("服饰") || s.contains("服装") || s.contains("裤") || s.contains("内衣") || s.contains("外套") || s.contains("裙") -> Pair("购物", "服饰鞋包")
            s.contains("日用") || s.contains("百货") || s.contains("家居") || s.contains("生活用品") || s.contains("纸巾") || s.contains("洗洁") || s.contains("洗衣") || s.contains("超市") || s.contains("便利店") -> Pair("购物", "家居百货")
            s.contains("美妆") || s.contains("护肤") || s.contains("化妆") || s.contains("面膜") || s.contains("口红") || s.contains("防晒") || s.contains("香水") -> Pair("购物", "化妆护肤")
            s.contains("数码") || s.contains("电子") || s.contains("手机") || s.contains("电脑") || s.contains("平板") || s.contains("耳机") || s.contains("充电宝") || s.contains("数据线") || s.contains("相机") -> Pair("购物", "电子数码")
            s.contains("烟酒") || s.contains("香烟") || s.contains("白酒") || s.contains("啤酒") || s.contains("红酒") || s.contains("抽烟") -> Pair("购物", "烟酒")
            s.contains("母婴") || s.contains("宝宝") || s.contains("儿童") || s.contains("奶粉") || s.contains("尿不湿") || s.contains("玩具") || s.contains("童装") -> Pair("购物", "宝宝用品")
            s.contains("书") || s.contains("杂志") || s.contains("报刊") || s.contains("教材") || s.contains("绘本") -> Pair("购物", "报刊书籍")
            s.contains("首饰") || s.contains("珠宝") || s.contains("手表") || s.contains("项链") || s.contains("戒指") || s.contains("黄金") -> Pair("购物", "珠宝首饰")
            s.contains("家电") || s.contains("电器") || s.contains("冰箱") || s.contains("洗衣机") || s.contains("空调") || s.contains("电视") -> Pair("购物", "电器")
            s.contains("购物") || s.contains("网购") || s.contains("淘宝") || s.contains("京东") || s.contains("拼多多") || s.contains("天猫") || s.contains("唯品会") || s.contains("抖音") -> Pair("购物", "购物其他")

            // 娱乐类
            s.contains("电影") || s.contains("影院") || s.contains("影票") || s.contains("观影") -> Pair("娱乐", "电影")
            s.contains("游戏") || s.contains("网游") || s.contains("Steam") || s.contains("充值") || s.contains("电玩") || s.contains("PS5") || s.contains("Switch") || s.contains("皮肤") -> Pair("娱乐", "网游电玩")
            s.contains("旅游") || s.contains("度假") || s.contains("门票") || s.contains("景点") || s.contains("酒店") || s.contains("民宿") || s.contains("旅行") -> Pair("娱乐", "旅游度假")
            s.contains("健身") || s.contains("运动") || s.contains("游泳") || s.contains("羽毛球") || s.contains("篮球") || s.contains("瑜伽") || s.contains("私教") || s.contains("健身房") -> Pair("娱乐", "运动健身")
            s.contains("宠物") || s.contains("猫粮") || s.contains("狗粮") || s.contains("猫砂") || s.contains("猫咪") || s.contains("狗狗") || s.contains("宠物医院") -> Pair("娱乐", "花鸟宠物")
            s.contains("KTV") || s.contains("酒吧") || s.contains("密室") || s.contains("剧本杀") || s.contains("洗浴") || s.contains("足疗") || s.contains("按摩") || s.contains("聚会") -> Pair("娱乐", "聚会玩乐")
            s.contains("娱乐") || s.contains("休闲") || s.contains("玩乐") -> Pair("娱乐", "娱乐其他")

            // 医教类
            s.contains("看病") || s.contains("医院") || s.contains("挂号") || s.contains("门诊") || s.contains("就医") || s.contains("体检") -> Pair("医教", "挂号门诊")
            s.contains("药") || s.contains("买药") || s.contains("药房") || s.contains("药店") || s.contains("西药") || s.contains("中药") -> Pair("医教", "医疗药品")
            s.contains("学费") || s.contains("培训") || s.contains("辅导") || s.contains("课程") || s.contains("考试") || s.contains("教育") -> Pair("医教", "学杂教材")
            s.contains("医疗") || s.contains("医教") || s.contains("健康") -> Pair("医教", "医教其他")

            // 居家类
            s.contains("话费") || s.contains("手机费") || s.contains("电话费") || s.contains("流量") -> Pair("居家", "手机电话")
            s.contains("水电") || s.contains("电费") || s.contains("水费") || s.contains("燃气") || s.contains("煤气") || s.contains("暖气") -> Pair("居家", "水电燃气")
            s.contains("房租") || s.contains("租房") || s.contains("房贷") || s.contains("月供") || s.contains("住宿") || s.contains("物业") -> Pair("居家", "住宿房租")
            s.contains("宽带") || s.contains("网费") || s.contains("WiFi") || s.contains("光纤") -> Pair("居家", "电脑宽带")
            s.contains("快递") || s.contains("顺丰") || s.contains("邮寄") || s.contains("跑腿") || s.contains("寄件") -> Pair("居家", "快递邮政")
            s.contains("美发") || s.contains("理发") || s.contains("剪发") || s.contains("美容") || s.contains("美甲") || s.contains("染发") || s.contains("做脸") -> Pair("居家", "美发美容")
            s.contains("家政") || s.contains("保洁") || s.contains("开锁") || s.contains("疏通") || s.contains("钟点工") -> Pair("居家", "家政服务")
            s.contains("生活费") || s.contains("居家") || s.contains("日常") -> Pair("居家", "生活费")

            // 人情类
            s.contains("红包") || s.contains("份子钱") || s.contains("礼金") || s.contains("随礼") || s.contains("压岁钱") -> Pair("人情", "礼金红包")
            s.contains("请客") || s.contains("送礼") || s.contains("孝敬") || s.contains("礼物") -> Pair("人情", "请客")
            s.contains("人情") || s.contains("往来") -> Pair("人情", "人情其他")

            // 投资 / 资金
            s.contains("基金") || s.contains("股票") || s.contains("证券") || s.contains("理财") || s.contains("投资") -> Pair("投资", "基金")
            s.contains("还款") || s.contains("还贷") || s.contains("信用卡还款") || s.contains("借款") || s.contains("应收") -> Pair("资金流转", "应收款")

            else -> null
        }
    }

    private suspend fun resolveCategoryId(
        type: TransactionType,
        categoryName: String,
        subCategory: String
    ): Long? {
        return resolveOrCreateCategoryId(type, categoryName, subCategory)
    }

    /**
     * 查找回退分类：当分类映射失败时使用的备用分类
     */
    suspend fun findFallbackCategory(type: String): CategoryEntity? {
        return categoryDao.findFallbackCategory(type)
    }

    /** 根据导入的账户目标余额与明细流水反推并校准各账户 initialBalance，确保资产变化趋势及实时余额绝对精确 */
    suspend fun calibrateImportedAccounts(accountTargetBalances: Map<String, Double>, bookId: Long? = null) = db.withTransaction {
        val targetBook = bookId ?: defaultBookId()
        val allActiveTxs = transactionDao.getByBookIdOnce(targetBook).ifEmpty { transactionDao.getActiveOnce() }
        val accounts = accountDao.getByBookId(targetBook).ifEmpty { accountDao.getActive() }
        for (acc in accounts) {
            val targetYuan = accountTargetBalances[acc.name] ?: continue
            var inc = 0L; var exp = 0L; var out = 0L; var inn = 0L
            for (tx in allActiveTxs) {
                when (tx.type) {
                    TransactionType.INCOME -> if (tx.accountId == acc.id) inc += tx.amount
                    TransactionType.EXPENSE -> if (tx.accountId == acc.id) exp += tx.amount
                    TransactionType.TRANSFER -> {
                        if (tx.accountId == acc.id) out += tx.amount
                        if (tx.transferToAccountId == acc.id) inn += tx.amount
                    }
                }
            }
            val netDelta = inc - exp - out + inn
            val targetCents = AmountFormatter.yuanToCents(targetYuan)
            val calculatedInitialBalance = (targetCents - netDelta).toInt()
            accountDao.updateInitialBalance(acc.id, calculatedInitialBalance, System.currentTimeMillis())
        }
    }

    data class ExpenseSnapshot(
        val id: Long,
        val type: String,
        val category: String,
        val subCategory: String,
        val amount: Double,
        val note: String,
        val dateTimestamp: Long,
        val accountId: Long,
        /** 转账对端账户 id；0 表示非转账或未指定（保持原值） */
        val transferToAccountId: Long = 0L
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
