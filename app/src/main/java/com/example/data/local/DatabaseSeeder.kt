package com.example.data.local

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.entity.AccountEntityV2
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.UserEntity
import java.util.UUID

/**
 * 六表结构的首启种子程序（事务化，幂等）。
 *
 * 相比旧版 populateIfEmpty 的改进：
 * - 全部包进 [withTransaction]，并发双开不会重复种入
 * - 内置分类树从 CategoryManager 同步写入 category 表，
 *   使过渡期的分类名解析可以直接 JOIN 获得
 * - Phase 4 将把 assets 的真实 803 笔样本并入本流程（另行实现转换管线）
 */
object DatabaseSeeder {

    /** 默认账本（「日常账本」）的固定 uuid，保证跨重装可识别 */
    const val DEFAULT_BOOK_UUID = "seed-book-default-000000000001"

    /** 老用户自定义分类是否已完成 v11 导入的幂等标记（存于 category_preferences） */
    private const val LEGACY_CUSTOM_IMPORT_FLAG = "custom_imported_v11"

    suspend fun seedIfEmpty(context: Context, db: DailyToolboxDatabase) {
        db.withTransaction {
            // 1. 单机用户
            if (db.userDao().count() == 0) {
                db.userDao().insert(UserEntity(id = 1L, name = "我", defaultCurrency = "CNY"))
            }

            // 2. 默认账本
            if (db.bookDao().count() == 0) {
                db.bookDao().insert(
                    BookEntity(
                        uuid = DEFAULT_BOOK_UUID,
                        userId = 1L,
                        name = "日常账本",
                        currency = "CNY",
                        isDefault = true,
                        sortOrder = 0
                    )
                )
            }
            val book = db.bookDao().getDefault() ?: return@withTransaction

            // 3. 八个默认资产账户（沿用旧版预置口径；基金/黄金/币安归为 investment）
            if (db.accountDao().count() == 0) {
                fun hex(seed: String): String = UUID.nameUUIDFromBytes(seed.toByteArray()).toString().replace("-", "")
                val defaults = listOf(
                    Triple("币安", "OTHER", "#F3BA2F"),
                    Triple("现金", "CASH", "#F59E0B"),
                    Triple("农业银行储蓄卡", "BANK_CARD", "#009688"),
                    Triple("微信钱包", "WECHAT", "#07C160"),
                    Triple("黄金", "OTHER", "#FFC107"),
                    Triple("招商银行储蓄卡", "BANK_CARD", "#E60012"),
                    Triple("基金", "OTHER", "#3F51B5"),
                    Triple("支付宝", "ALIPAY", "#1677FF")
                )
                db.accountDao().upsertAll(
                    defaults.mapIndexed { index, (name, type, color) ->
                        AccountEntityV2(
                            uuid = hex("account-$index-$name"),
                            userId = 1L,
                            bookId = book.id,
                            name = name,
                            type = mapAccountType(name, type),
                            initialBalance = 0,
                            color = color,
                            sortOrder = index
                        )
                    }
                )
            }

            // 4. 内置分类树（支出一/二级 + 收入一/二级；自定义分类由 Phase 2 导入）
            if (db.categoryDao().count() == 0) {
                val catRows = mutableListOf<CategoryEntity>()
                var sortIdx = 0
                for (type in listOf("EXPENSE", "INCOME")) {
                    CategoryManager.getCategories(context, type).forEach { item ->
                        val parentIdSeed = "$type-${item.name}"
                        val parent = CategoryEntity(
                            uuid = UUID.nameUUIDFromBytes(parentIdSeed.toByteArray()).toString().replace("-", ""),
                            userId = 1L,
                            bookId = book.id,
                            name = item.name,
                            parentId = null,
                            type = type.lowercase(),
                            sortOrder = sortIdx++
                        )
                        catRows.add(parent)
                    }
                }
                db.categoryDao().insertAll(catRows)

                // 二级：先取回父级 id 再插子级（同事务内一致快照）
                var subSort = 1000
                for (type in listOf("EXPENSE", "INCOME")) {
                    CategoryManager.getCategories(context, type).forEach { item ->
                        val parent = db.categoryDao().findParentByName(item.name, type.lowercase()) ?: return@forEach
                        val subs = CategoryManager.getSubcategories(context, item.name, type)
                        val rows = subs.distinct().map { sub ->
                            CategoryEntity(
                                uuid = UUID.nameUUIDFromBytes("$type-${item.name}-$sub".toByteArray()).toString().replace("-", ""),
                                userId = 1L,
                                bookId = book.id,
                                name = sub,
                                parentId = parent.id,
                                type = type.lowercase(),
                                sortOrder = subSort++
                            )
                        }.filter { it.parentId != null }
                        if (rows.isNotEmpty()) db.categoryDao().insertAll(rows)
                    }
                }
            }
        }
    }

    /**
     * 老用户自定义分类一次性导入（v11 迁移，独立于 [seedIfEmpty] 的幂等分支）。
     *
     * 与种子灌库的区别：只要导入标记未置位就执行 —— 即使六表种子早已完成
     * （categoryDao.count() > 0）也会补插老版本遗留的自定义一级/二级分类，
     * 然后写回标记保证只跑一次。uuid 复用种子的 nameUUIDFromBytes 方案 +
     * OnConflictStrategy.IGNORE，重复执行天然幂等。
     */
    suspend fun importLegacyCustomCategoriesIfPending(context: Context, db: DailyToolboxDatabase) {
        val prefs = context.getSharedPreferences("category_preferences", Context.MODE_PRIVATE)
        if (prefs.getBoolean(LEGACY_CUSTOM_IMPORT_FLAG, false)) return

        val imported = mutableListOf<Pair<String, CategoryEntity>>()
        db.withTransaction {
            val book = db.bookDao().getDefault() ?: return@withTransaction
            var sortIdx = 9000

            // 1. 自定义一级分类（支出/收入两端各取一次）
            for (type in listOf("EXPENSE", "INCOME")) {
                CategoryManager.getCustomCategories(context, type).forEach { item ->
                    val parentSeed = "${type}-${item.name}"
                    val existing = db.categoryDao().findParentByName(item.name, type.lowercase())
                    if (existing == null) {
                        imported.add(
                            item.name to CategoryEntity(
                                uuid = UUID.nameUUIDFromBytes(parentSeed.toByteArray()).toString().replace("-", ""),
                                userId = 1L,
                                bookId = book.id,
                                name = item.name,
                                parentId = null,
                                type = type.lowercase(),
                                sortOrder = sortIdx++
                            )
                        )
                    }
                }
            }
            if (imported.isNotEmpty()) {
                db.categoryDao().insertAll(imported.map { it.second })
            }

            // 2. 自定义二级细分：键不含类型，归并挂到所有同名一级（含上一步新导入的父级）
            var subSortIdx = sortIdx + 1000
            CategoryManager.getCustomSubcategoriesMap(context).forEach { (catName, subs) ->
                val parents = listOf("expense", "income")
                    .mapNotNull { db.categoryDao().findParentByName(catName, it) }
                    .distinctBy { it.id }
                if (parents.isEmpty()) return@forEach

                val rows = parents.flatMap { parent ->
                    subs.mapNotNull { sub ->
                        // 幂等：同名子分类已存在则跳过（uuid 冲突由 IGNORE 兜底）
                        val duplicated = db.categoryDao().findChildByName(sub, parent.id) != null
                        if (duplicated) null else CategoryEntity(
                            uuid = UUID.nameUUIDFromBytes("${parent.type}-${parent.name}-$sub".toByteArray())
                                .toString().replace("-", ""),
                            userId = 1L,
                            bookId = book.id,
                            name = sub,
                            parentId = parent.id,
                            type = parent.type,
                            sortOrder = subSortIdx++
                        )
                    }
                }
                if (rows.isNotEmpty()) {
                    db.categoryDao().insertAll(rows)
                }
            }

            prefs.edit().putBoolean(LEGACY_CUSTOM_IMPORT_FLAG, true).apply()
        }
    }

    /** 旧类型标识 → 新账户类型（目标 schema 使用小写语义标签） */
    private fun mapAccountType(name: String, legacyType: String): String = when {
        name in setOf("基金", "黄金", "币安") -> "investment"
        else -> when (legacyType) {
            "WECHAT" -> "wechat"
            "ALIPAY" -> "alipay"
            "BANK_CARD" -> "debit"
            "CREDIT_CARD" -> "credit"
            "CASH" -> "cash"
            else -> "other"
        }
    }
}
