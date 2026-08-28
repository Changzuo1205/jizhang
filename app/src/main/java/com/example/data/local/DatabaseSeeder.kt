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

    data class UserAccountSpec(
        val name: String,
        val type: String,
        val color: String,
        val targetBalanceCents: Long,
        val aliases: List<String>
    )

    /** 七个标准资产账户及其当前准确余额口径（单位：分） */
    val DEFAULT_USER_ACCOUNTS = listOf(
        UserAccountSpec("现金", "cash", "#F59E0B", 0L, listOf("现金")),
        UserAccountSpec("农业银行储蓄卡 9278", "debit", "#009688", 90L, listOf("农业银行储蓄卡", "农业银行储蓄卡 9278", "农业银行卡", "农业银行")),
        UserAccountSpec("招商银行储蓄卡 0741", "debit", "#E60012", 282084L, listOf("招商银行储蓄卡", "招商银行储蓄卡 0741", "招商银行卡", "招商银行")),
        UserAccountSpec("支付宝", "alipay", "#1677FF", 860840L, listOf("支付宝", "余额宝")),
        UserAccountSpec("微信钱包", "wechat", "#07C160", 209599L, listOf("微信钱包", "微信")),
        UserAccountSpec("基金", "investment", "#3F51B5", 3943096L, listOf("基金")),
        UserAccountSpec("黄金", "investment", "#FFC107", 134L, listOf("黄金"))
    )

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

            // 3. 七个标准资产账户（现金、农业银行储蓄卡 9278、招商银行储蓄卡 0741、支付宝、微信钱包、基金、黄金）
            if (db.accountDao().count() == 0) {
                fun hex(seed: String): String = UUID.nameUUIDFromBytes(seed.toByteArray()).toString().replace("-", "")
                db.accountDao().upsertAll(
                    DEFAULT_USER_ACCOUNTS.mapIndexed { index, spec ->
                        AccountEntityV2(
                            uuid = hex("account-$index-${spec.name}"),
                            userId = 1L,
                            bookId = book.id,
                            name = spec.name,
                            type = spec.type,
                            initialBalance = 0,
                            color = spec.color,
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
            val accByName = mutableMapOf<String, AccountEntityV2>()
            for (acc in accounts) {
                accByName[acc.name] = acc
                if (acc.name.contains("农业银行")) {
                    accByName["农业银行储蓄卡"] = acc
                    accByName["农业银行"] = acc
                }
                if (acc.name.contains("招商银行")) {
                    accByName["招商银行储蓄卡"] = acc
                    accByName["招商银行"] = acc
                }
                if (acc.name.contains("微信")) {
                    accByName["微信钱包"] = acc
                    accByName["微信"] = acc
                }
                if (acc.name.contains("支付宝")) {
                    accByName["支付宝"] = acc
                    accByName["余额宝"] = acc
                }
            }

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

            fun resolveCategoryId(typeLower: String, categoryName: String, subName: String): Long? {
                val effectiveCat = if (categoryName == "漏记款" || (categoryName.isBlank() && subName == "漏记款")) {
                    if (typeLower == "income") "漏记款" else "居家"
                } else if (typeLower == "income" && (categoryName == "工资" || subName == "工资")) {
                    "工资薪水"
                } else categoryName
                val effectiveSub = if (typeLower == "income") effectiveCat else subName
                return when {
                    effectiveSub.isNotBlank() && effectiveCat.isNotBlank() ->
                        childIdIdx["$typeLower|$effectiveCat|$effectiveSub"]
                            ?: parentIdIdx["$typeLower|$effectiveCat"]
                    effectiveSub.isNotBlank() -> {
                        // 修复：category为空但subCategory有值时，先按子分类名匹配父级分类
                        // CSV中 "一级分类=餐饮, 二级分类=晚餐" 被错误转换成了 category=""
                        // 此时应通过二级名称"晚餐"反查父分类"餐饮"
                        parentIdIdx["$typeLower|$effectiveSub"]
                            ?: run {
                                // 在所有分类中查找名字等于subName的子分类
                                val matchChild = catById.values.firstOrNull { cat ->
                                    cat.parentId != null && cat.name == effectiveSub && cat.type == typeLower
                                }
                                matchChild?.id
                            }
                    }
                    effectiveCat.isNotBlank() ->
                        parentIdIdx["$typeLower|$effectiveCat"]
                    else -> null
                }
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

    /**
     * 将支出端「漏记款」二级分类归为「居家」一级分类中。
     * 同时将数据库中旧的独立支出「漏记款」关联的交易重新绑定至「居家 -> 漏记款」，并清理冗余分类。
     */
    suspend fun syncMissedCategoryUnderHome(context: Context, db: DailyToolboxDatabase) {
        db.withTransaction {
            val book = db.bookDao().getDefault() ?: return@withTransaction

            // 1. 处理支出端「居家 -> 漏记款」
            val homeExpenseParent = ensureCategory(db, book.id, "居家", "expense", null, 50)
            val missedExpenseChild = ensureCategory(db, book.id, "漏记款", "expense", homeExpenseParent.id, 1050)

            val oldExpenseMissed = db.categoryDao().findByName("漏记款")
                .filter { it.type == "expense" && (it.parentId == null || it.parentId != homeExpenseParent.id) }
            for (oldCat in oldExpenseMissed) {
                db.transactionDao().updateCategoryId(oldCat.id, missedExpenseChild.id)
                val subCats = db.categoryDao().getChildrenByParentId(oldCat.id)
                for (sub in subCats) {
                    db.transactionDao().updateCategoryId(sub.id, missedExpenseChild.id)
                    db.categoryDao().deleteById(sub.id)
                }
                db.categoryDao().deleteById(oldCat.id)
            }
        }
    }

    /**
     * 同步扁平化收入分类体系：
     * 1. 19个标准收入分类（工资薪水、利息、兼职外快、营业收入、红包、销售款、退款返款、报销款、福利补贴、余额宝、应收款、生活费、基金、礼金、分红股票、公积金、赔付款、漏记款、其他）
     * 2. 取消收入类型下的「居家」一级分类，将「漏记款」作为收入一级分类
     * 3. 已有数据的收入类型将一级分类与二级分类一致化
     */
    suspend fun syncFlatIncomeCategories(context: Context, db: DailyToolboxDatabase) {
        val standardIncomeCategories = listOf(
            "工资薪水", "利息", "兼职外快", "营业收入", "红包",
            "销售款", "退款返款", "报销款", "福利补贴", "余额宝",
            "应收款", "生活费", "基金", "礼金", "分红股票",
            "公积金", "赔付款", "漏记款", "其他"
        )

        db.withTransaction {
            val book = db.bookDao().getDefault() ?: return@withTransaction

            // 1. 创建/确保 19 个标准收入分类（一级 + 同名二级）
            val catMap = mutableMapOf<String, CategoryEntity>()
            standardIncomeCategories.forEachIndexed { index, name ->
                val parent = ensureCategory(db, book.id, name, "income", null, index * 10)
                ensureCategory(db, book.id, name, "income", parent.id, 1000 + index * 10)
                catMap[name] = parent
            }

            val missedIncomeCat = catMap["漏记款"] ?: return@withTransaction
            val livingIncomeCat = catMap["生活费"] ?: return@withTransaction
            val salaryIncomeCat = catMap["工资薪水"] ?: return@withTransaction

            // 2. 清理旧收入分类中名为「居家」的分类及其子分类，将关联的交易迁移至「漏记款」或「生活费」
            val oldHomeIncomeCats = db.categoryDao().findByName("居家").filter { it.type == "income" }
            for (homeCat in oldHomeIncomeCats) {
                val subCats = db.categoryDao().getChildrenByParentId(homeCat.id)
                for (sub in subCats) {
                    val subTxs = db.transactionDao().getByTypeOnce(TransactionType.INCOME)
                        .filter { it.categoryId == sub.id }
                    for (tx in subTxs) {
                        val target = if (tx.note?.contains("生活费") == true) livingIncomeCat.id else missedIncomeCat.id
                        db.transactionDao().update(tx.copy(categoryId = target))
                    }
                    db.categoryDao().deleteById(sub.id)
                }
                val homeTxs = db.transactionDao().getByTypeOnce(TransactionType.INCOME)
                    .filter { it.categoryId == homeCat.id }
                for (tx in homeTxs) {
                    val target = if (tx.note?.contains("生活费") == true) livingIncomeCat.id else missedIncomeCat.id
                    db.transactionDao().update(tx.copy(categoryId = target))
                }
                db.categoryDao().deleteById(homeCat.id)
            }

            // 3. 将旧的名为「工资」的收入分类交易迁移至「工资薪水」
            val oldSalaryIncomeCats = db.categoryDao().findByName("工资").filter { it.type == "income" }
            for (oldSalary in oldSalaryIncomeCats) {
                val subCats = db.categoryDao().getChildrenByParentId(oldSalary.id)
                for (sub in subCats) {
                    db.transactionDao().updateCategoryId(sub.id, salaryIncomeCat.id)
                    db.categoryDao().deleteById(sub.id)
                }
                db.transactionDao().updateCategoryId(oldSalary.id, salaryIncomeCat.id)
                db.categoryDao().deleteById(oldSalary.id)
            }

            // 4. 对所有存量收入交易，确保分类归入标准 19 个分类
            val incomeTxs = db.transactionDao().getByTypeOnce(TransactionType.INCOME)
            val accById = db.accountDao().getActive().associateBy { it.id }

            for (tx in incomeTxs) {
                val currentCat = tx.categoryId?.let { db.categoryDao().getById(it) }
                val currentParent = currentCat?.parentId?.let { db.categoryDao().getById(it) }
                val catName = currentParent?.name ?: currentCat?.name ?: ""

                val note = tx.note.orEmpty()
                val accName = accById[tx.accountId]?.name.orEmpty()

                val resolvedName = when {
                    catName in standardIncomeCategories -> catName
                    catName == "工资" -> "工资薪水"
                    catName == "居家" -> if (note.contains("生活费")) "生活费" else "漏记款"
                    note.contains("利息") -> "利息"
                    note.contains("兼职") || note in listOf("众包保证金", "大叹号") -> "兼职外快"
                    note.contains("营业") -> "营业收入"
                    note.contains("红包") -> "红包"
                    note.contains("销售") -> "销售款"
                    note.contains("退款") || note.contains("返款") -> "退款返款"
                    note.contains("报销") -> "报销款"
                    note.contains("福利") || note.contains("补贴") -> "福利补贴"
                    note.contains("余额宝") -> "余额宝"
                    note.contains("应收") -> "应收款"
                    note.contains("生活费") -> "生活费"
                    note.contains("基金") || note == "001423" || (accName == "基金" && !note.contains("余额调整")) -> "基金"
                    note.contains("礼金") || note in listOf("娘", "压岁") -> "礼金"
                    note.contains("分红") || note.contains("股票") -> "分红股票"
                    note.contains("公积金") -> "公积金"
                    note.contains("赔付") || note.contains("赔偿") -> "赔付款"
                    note.contains("余额调整") || note.contains("漏记") -> "漏记款"
                    note.contains("工资") || note.contains("薪") -> "工资薪水"
                    else -> "其他"
                }

                val targetCatId = catMap[resolvedName]?.id ?: missedIncomeCat.id
                if (tx.categoryId != targetCatId) {
                    db.transactionDao().update(tx.copy(categoryId = targetCatId))
                }
            }
        }
    }

    private suspend fun ensureCategory(
        db: DailyToolboxDatabase,
        bookId: Long,
        name: String,
        type: String,
        parentId: Long? = null,
        sortOrder: Int = 100
    ): CategoryEntity {
        val existing = if (parentId == null) {
            db.categoryDao().findParentByName(name, type)
        } else {
            db.categoryDao().findChildByName(name, parentId)
        }
        if (existing != null) return existing

        val seedKey = if (parentId == null) "$type-$name" else "$type-$parentId-$name"
        val newCat = CategoryEntity(
            uuid = UUID.nameUUIDFromBytes(seedKey.toByteArray()).toString().replace("-", ""),
            userId = 1L,
            bookId = bookId,
            name = name,
            parentId = parentId,
            type = type,
            sortOrder = sortOrder
        )
        val id = db.categoryDao().insert(newCat)
        return newCat.copy(id = id)
    }

    /**
     * 同步并校准用户指定账户信息及其实际余额：
     * 现金：0
     * 农业银行储蓄卡 9278：0.90
     * 招商银行储蓄卡 0741：2820.84
     * 支付宝：8608.40
     * 微信钱包：2095.99
     * 基金：39430.96
     * 黄金：1.34
     * 
     * 通过事务根据所有活跃交易反推并设置 initialBalance，使得当前各账户余额精确等于指定值。
     */
    suspend fun syncUserAccountBalances(context: Context, db: DailyToolboxDatabase) {
        db.withTransaction {
            val book = db.bookDao().getDefault() ?: return@withTransaction
            val existingAccounts = db.accountDao().getActive().toMutableList()

            val matchedAccountIds = mutableSetOf<Long>()
            val specToAccountMap = mutableMapOf<String, AccountEntityV2>()

            for (spec in DEFAULT_USER_ACCOUNTS) {
                var found = existingAccounts.firstOrNull { acc ->
                    acc.id !in matchedAccountIds && (
                        acc.name == spec.name || spec.aliases.any { alias ->
                            acc.name.contains(alias) || alias.contains(acc.name)
                        }
                    )
                }

                if (found != null) {
                    matchedAccountIds.add(found.id)
                    if (found.name != spec.name || found.type != spec.type || found.color != spec.color) {
                        val updated = found.copy(name = spec.name, type = spec.type, color = spec.color)
                        db.accountDao().upsert(updated)
                        found = updated
                    }
                    specToAccountMap[spec.name] = found
                } else {
                    val uuid = UUID.nameUUIDFromBytes("account-spec-${spec.name}".toByteArray()).toString().replace("-", "")
                    val newAcc = AccountEntityV2(
                        uuid = uuid,
                        userId = 1L,
                        bookId = book.id,
                        name = spec.name,
                        type = spec.type,
                        initialBalance = 0,
                        color = spec.color,
                        sortOrder = DEFAULT_USER_ACCOUNTS.indexOf(spec)
                    )
                    val id = db.accountDao().insert(newAcc)
                    val inserted = newAcc.copy(id = id)
                    specToAccountMap[spec.name] = inserted
                }
            }

            // 归档无活跃流水的历史冗余账户（如币安）
            val allActiveTxs = db.transactionDao().getActiveOnce()
            for (acc in existingAccounts) {
                if (acc.id !in matchedAccountIds && acc.name !in specToAccountMap.keys) {
                    val txCount = allActiveTxs.count { it.accountId == acc.id || it.transferToAccountId == acc.id }
                    if (txCount == 0) {
                        db.accountDao().archive(acc.id, System.currentTimeMillis())
                    }
                }
            }

            // 获取所有生效交易，精准校准各账户 initialBalance
            for (spec in DEFAULT_USER_ACCOUNTS) {
                val acc = specToAccountMap[spec.name] ?: continue
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
                val calculatedInitialBalance = (spec.targetBalanceCents - netDelta).toInt()
                if (acc.initialBalance != calculatedInitialBalance) {
                    db.accountDao().updateInitialBalance(acc.id, calculatedInitialBalance, System.currentTimeMillis())
                }
            }
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
