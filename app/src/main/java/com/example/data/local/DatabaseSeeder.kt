package com.example.data.local

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.entity.AccountEntityV2
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import java.util.UUID

/**
 * 六表结构的首启种子程序（事务化，幂等）。
 *
 * 相比旧版 populateIfEmpty 的改进：
 * - 全部包进 [withTransaction]，并发双开不会重复种入
 * - 内置分类树从 CategoryManager 同步写入 category 表，
 *   使过渡期的分类名解析可以直接 JOIN 获得
 * - 真实历史流水由 [seedRealTransactionsIfEmpty] 从 assets/seed_transactions.json
 *   灌入（该文件由 tools/seed_converter.py 从交接包真实样本转换生成，
 *   含挖财 6 个月 803 笔实际账目与软删除标记）
 */
object DatabaseSeeder {

    /** 默认账本（「日常账本」）的固定 uuid，保证跨重装可识别 */
    const val DEFAULT_BOOK_UUID = "seed-book-default-000000000001"

    /** 转换器产出的真实流水种子文件名 */
    private const val REAL_SEED_ASSET = "seed_transactions.json"

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

            // 5. 真实历史流水（tools/seed_converter.py 的产物；无则保持空库）
            seedRealTransactionsIfEmpty(context, db, book)
        }
    }

    /**
     * 从 assets/[REAL_SEED_ASSET] 灌入真实历史交易。
     *
     * 规则（对应 tools/seed_converter.py）：
     * - 金额为 Int 分、时间戳 Unix 毫秒、isDeleted 原样保留
     * - account 名称直配种子账户；空缺回退「现金」
     * - 分类按 一级+二级 在内存索引解析外键；「漏记款」类调整行 category 为空、
     *   仅二级，以二级名作为一级命中
     * - TRANSFER 行的 counterAccount 解析为 transfer_to_account_id
     * 资产文件缺失或解析失败时静默跳过（不影响其余种子）。
     */
    private suspend fun seedRealTransactionsIfEmpty(context: Context, db: DailyToolboxDatabase, book: BookEntity) {
        if (db.transactionDao().activeCount() > 0) return

        val raw = try {
            context.assets.open(REAL_SEED_ASSET).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        try {
            val root = org.json.JSONObject(raw)
            val rows = root.optJSONArray("transactions") ?: return

            // ---- 内存索引：账户 / 分类树 ----
            val accounts = db.accountDao().getActive()
            val accByName = accounts.associateBy { it.name }

            val catById = mutableMapOf<Long, CategoryEntity>()
            for (type in listOf("expense", "income")) {
                db.categoryDao().getByType(book.id, type).forEach { catById[it.id] = it }
            }
            val parentIdIdx = mutableMapOf<String, Long>()
            val childIdIdx = mutableMapOf<String, Long>()
            for (cat in catById.values) {
                val parent = cat.parentId?.let { catById[it] }
                if (cat.parentId == null) {
                    parentIdIdx["${cat.type}|${cat.name}"] = cat.id
                } else if (parent != null) {
                    childIdIdx["${cat.type}|${parent.name}|${cat.name}"] = cat.id
                }
            }

            fun resolveCategoryId(typeLower: String, categoryName: String, subName: String): Long? =
                when {
                    subName.isNotBlank() && categoryName.isNotBlank() ->
                        childIdIdx["$typeLower|$categoryName|$subName"]
                            ?: parentIdIdx["$typeLower|$categoryName"]
                    subName.isNotBlank() -> {
                        // 修复：category为空但subCategory有值时，先按子分类名匹配父级分类
                        // CSV中 "一级分类=餐饮, 二级分类=晚餐" 被错误转换成了 category=""
                        // 此时应通过二级名称"晚餐"反查父分类"餐饮"
                        parentIdIdx["$typeLower|$subName"]
                            ?: run {
                                // 在所有分类中查找名字等于subName的子分类
                                val matchChild = catById.values.firstOrNull { cat ->
                                    cat.parentId != null && cat.name == subName && cat.type == typeLower
                                }
                                matchChild?.id
                            }
                    }
                    categoryName.isNotBlank() ->
                        parentIdIdx["$typeLower|$categoryName"]
                    else -> null
                }

            fun rawTypeToEnum(raw: String): TransactionType =
                runCatching { TransactionType.valueOf(raw.trim()) }.getOrDefault(TransactionType.EXPENSE)

            val entities = mutableListOf<TransactionEntity>()
            var insertedIndex = 0L
            for (i in 0 until rows.length()) {
                val o = rows.getJSONObject(i)
                insertedIndex += 1
                val txType = rawTypeToEnum(o.optString("type", "EXPENSE"))
                val typeLower = txType.name.lowercase()
                val categoryName = o.optString("category", "")
                val subName = o.optString("subCategory", "")
                val account = accByName[o.optString("account")] ?: accByName["现金"] ?: continue
                val counterRaw = if (o.isNull("counterAccount")) null else o.optString("counterAccount")
                val counterId = if (txType == TransactionType.TRANSFER && !counterRaw.isNullOrBlank()) {
                    accByName[counterRaw]?.id
                } else null

                entities.add(
                    TransactionEntity(
                        uuid = o.optString("uuid") ?: "seed-$insertedIndex",
                        userId = 1L,
                        bookId = book.id,
                        accountId = account.id,
                        transferToAccountId = counterId,
                        type = txType,
                        amount = o.getInt("amount_cents"),
                        categoryId = resolveCategoryId(typeLower, categoryName, subName),
                        note = if (o.isNull("note")) null else o.optString("note"),
                        occurredAt = o.getLong("occurredAt"),
                        isDeleted = o.optBoolean("isDeleted", false),
                        source = o.optString("source", "manual")
                    )
                )
            }

            // 分批插入避免单语句过大
            entities.chunked(400).forEach { chunk -> db.transactionDao().insertAll(chunk) }
        } catch (e: Exception) {
            e.printStackTrace()
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
