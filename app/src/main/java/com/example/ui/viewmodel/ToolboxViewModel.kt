package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.TransactionType
import com.example.data.local.entity.AccountEntityV2
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.ToolboxRepositoryV2
import com.example.di.AppContainer
import com.example.JizhangApplication
import com.example.model.AmountFormatter
import com.example.ui.theme.BackgroundConfig
import com.example.ui.theme.BackgroundOptionType
import com.example.ui.theme.ColorSchemeOption
import com.example.ui.theme.FontScaleOption
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 分类消费/收入统计汇总模型
 *
 * @property category 分类名称
 * @property totalAmount 该分类总计金额
 * @property count 交易笔数
 * @property percentage 占总收支的比例 (0.0 ~ 1.0)
 * @property type "EXPENSE" 或 "INCOME"
 */
data class CategoryStat(
    val category: String,
    val totalAmount: Double,
    val count: Int,
    val percentage: Float,
    val type: String
)

/**
 * 趋势折线图/柱状图数据点模型
 *
 * @property label X 轴标签（如："08/21"、"周三"、"8月"）
 * @property expense 当期支出总额
 * @property income 当期收入总额
 * @property timestamp 时间点时间戳
 */
data class TrendPoint(
    val label: String,
    val expense: Double,
    val income: Double,
    val timestamp: Long
)

/**
 * 预算统计周期枚举
 */
enum class BudgetPeriod(val title: String, val shortName: String) {
    MONTH("月度", "月"),
    QUARTER("季度", "季"),
    YEAR("年度", "年")
}

/**
 * 预算配置参数模型
 */
data class BudgetConfig(
    val monthlyLimit: Double = 5000.0,
    val quarterlyLimit: Double = 15000.0,
    val yearlyLimit: Double = 60000.0,
    val activePeriod: BudgetPeriod = BudgetPeriod.MONTH
)

/**
 * 预算执行进度与预警信息
 */
data class BudgetProgressInfo(
    val period: BudgetPeriod,
    val budgetLimit: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val isOverBudget: Boolean,
    val overAmount: Double,
    val progressPercent: Float,
    val remainingDailyAverage: Double = 0.0,
    val remainingDays: Int = 1
)

/**
 * 核心业务 ViewModel (ToolboxViewModel)
 *
 * 聚合应用全部状态与业务流：
 * 1. 记账 CRUD、日历筛选、月份筛选、类型过滤、模糊搜索。
 * 2. 资产账户余额联动计算、净资产/总负债/正资产聚合。
 * 3. 月度/季度/年度预算监控与超支智能预警、每日建议可支配预算推算。
 * 4. 统计图表（饼图扇区占比、周/月/年收支趋势折线）流式派生计算。
 * 5. 全局个性化设置（6大主题色、5大背景壁纸体系、字体缩放大小）持久化。
 */
class ToolboxViewModel(application: Application, container: AppContainer) : AndroidViewModel(application) {
    private val repository: ToolboxRepositoryV2

    /** 六表种子完成信号：任何依赖默认账本的写操作须先 await（种子在 Application 启动即灌入） */
    private val seedReady: Deferred<Unit> = container.seedCompleted

    init {
        repository = container.repository
    }

    /** 手动注入工厂（Hilt 不可用时代的等价物，见 JizhangApplication 注释） */
    companion object {
        fun factory(app: JizhangApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    ToolboxViewModel(app, app.container) as T
            }
    }

    /** 存储层行数据（规范化实体，金额为分） */
    private val transactionRows: StateFlow<List<TransactionEntity>> =
        repository.observeActiveTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val accountRows: StateFlow<List<AccountEntityV2>> =
        repository.observeActiveAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val categoryRows: StateFlow<List<CategoryEntity>> =
        repository.observeActiveCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 分类表原始树（一级+二级混合平面，parentId 关联），供分类管理页自行组树 */
    val categories: StateFlow<List<CategoryEntity>> get() = categoryRows

    /** 账本观察流（默认账本置顶），供账本管理页渲染 */
    val books: StateFlow<List<BookEntity>> =
        repository.observeActiveBooks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeBookId = MutableStateFlow<Long?>(null)

    val currentBook: StateFlow<BookEntity?> =
        combine(books, activeBookId) { bookList, selectedId ->
            if (selectedId != null) {
                bookList.find { it.id == selectedId } ?: bookList.find { it.isDefault } ?: bookList.firstOrNull()
            } else {
                bookList.find { it.isDefault } ?: bookList.firstOrNull()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * 过渡期映射层：TransactionEntity（分/外键）→ ExpenseEntity DTO（元/显示名）。
     * 按当前激活账本进行数据隔离与切换。
     */
    val allExpenses: StateFlow<List<ExpenseEntity>> =
        combine(transactionRows, categoryRows, accountRows, currentBook) { txs, cats, accs, curBook ->
            val activeId = curBook?.id
            val filteredTxs = if (activeId == null) txs else {
                txs.filter { it.bookId == activeId }
            }
            val catById = cats.associateBy { it.id }
            val accById = accs.associateBy { it.id }
            
            filteredTxs.map { tx ->
                val cat = tx.categoryId?.let { catById[it] }
                val parent = cat?.parentId?.let { catById[it] }
                
                val (finalCategory, finalSubCategory) = if (tx.type == TransactionType.INCOME) {
                    val rawName = when {
                        parent != null && parent.type == "income" && parent.name != "居家" -> parent.name
                        cat != null && cat.type == "income" && cat.name != "居家" -> cat.name
                        cat != null && cat.name.isNotBlank() && cat.name != "其他" && cat.name != "默认" -> cat.name
                        else -> {
                            val note = tx.note.orEmpty()
                            inferIncomeCategoryName(note)
                        }
                    }
                    val normalized = when (rawName) {
                        "工资" -> "工资薪水"
                        "居家" -> if (tx.note?.contains("生活费") == true) "生活费" else "漏记款"
                        else -> rawName
                    }
                    // 收入明细的一级分类与二级分类相同
                    Pair(normalized, normalized)
                } else if (tx.type == TransactionType.TRANSFER) {
                    // 转账类型：一级分类与二级分类均规范标识为“转账”，杜绝被显示为未分类
                    Pair("转账", "转账")
                } else {
                    // 支出：分类映射逻辑：父级存在则为 (一级, 二级)，否则自身如果为一级分类则显示 (分类, 二级)，均无则根据备注推断
                    val (resolvedCategory, resolvedSubCategory) = when {
                        parent != null -> {
                            Pair(parent.name, cat?.name.orEmpty())
                        }
                        cat != null -> {
                            val cName = cat.name.trim()
                            if (cName != "其他" && cName != "默认" && cName.isNotBlank()) {
                                Pair(cName, cName)
                            } else {
                                val note = tx.note.orEmpty()
                                val inferred = inferExpenseCategoryPair(note)
                                if (inferred.first != "其他") inferred else Pair(cName.ifBlank { "其他" }, "")
                            }
                        }
                        else -> {
                            val note = tx.note.orEmpty()
                            inferExpenseCategoryPair(note)
                        }
                    }
                    Pair(resolvedCategory, resolvedSubCategory)
                }
                
                ExpenseEntity(
                    id = tx.id,
                    type = tx.type.name,
                    category = finalCategory,
                    subCategory = finalSubCategory,
                    amount = AmountFormatter.centsToYuan(tx.amount),
                    note = tx.note.orEmpty(),
                    dateTimestamp = tx.occurredAt,
                    accountId = tx.accountId,
                    accountName = accById[tx.accountId]?.name ?: "",
                    transferToAccountId = tx.transferToAccountId ?: 0L,
                    transferToAccountName =
                        tx.transferToAccountId?.let { accById[it]?.name }.orEmpty(),
                    uuid = tx.uuid
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 账户展示余额＝ initial_balance ± Σ未删除交易（含转账两端对冲），派生值。
     * 按当前激活账本隔离账户列表与账本内交易流水。
     * 映射回旧版 AccountEntity DTO 形态供现有页面渲染。
     * O(N+M) 单次遍历汇总账户净额变动，避免反复循环扫描。
     */
    val allAccounts: StateFlow<List<AccountEntity>> =
        combine(accountRows, transactionRows, currentBook) { rows, txs, curBook ->
            val activeId = curBook?.id
            val filteredAccs = if (activeId == null) rows else {
                rows.filter { it.bookId == activeId }
            }
            val filteredTxs = if (activeId == null) txs else {
                txs.filter { it.bookId == activeId }
            }
            val deltas = mutableMapOf<Long, Long>()
            for (tx in filteredTxs) {
                when (tx.type) {
                    TransactionType.INCOME -> deltas[tx.accountId] = (deltas[tx.accountId] ?: 0L) + tx.amount
                    TransactionType.EXPENSE -> deltas[tx.accountId] = (deltas[tx.accountId] ?: 0L) - tx.amount
                    TransactionType.TRANSFER -> {
                        deltas[tx.accountId] = (deltas[tx.accountId] ?: 0L) - tx.amount
                        val targetId = tx.transferToAccountId
                        if (targetId != null) {
                            deltas[targetId] = (deltas[targetId] ?: 0L) + tx.amount
                        }
                    }
                }
            }
            filteredAccs.map { a ->
                val netDelta = deltas[a.id] ?: 0L
                AccountEntity(
                    id = a.id,
                    name = a.name,
                    type = ToolboxRepositoryV2.v2TypeToLegacy(a.type),
                    balance = AmountFormatter.centsToYuan((a.initialBalance + netDelta).toInt()),
                    cardSuffix = "",
                    colorHex = a.color ?: "#3B82F6",
                    note = ""
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budget Settings with SharedPreferences persistence
    private val budgetPrefs = application.getSharedPreferences("app_budget_prefs", Context.MODE_PRIVATE)

    private val _budgetConfig = MutableStateFlow(
        BudgetConfig(
            monthlyLimit = budgetPrefs.getFloat("budget_monthly", 5000.0f).toDouble(),
            quarterlyLimit = budgetPrefs.getFloat("budget_quarterly", 15000.0f).toDouble(),
            yearlyLimit = budgetPrefs.getFloat("budget_yearly", 60000.0f).toDouble(),
            activePeriod = try {
                BudgetPeriod.valueOf(budgetPrefs.getString("budget_active_period", "MONTH") ?: "MONTH")
            } catch (e: Exception) {
                BudgetPeriod.MONTH
            }
        )
    )
    val budgetConfig: StateFlow<BudgetConfig> = _budgetConfig.asStateFlow()

    fun setActiveBudgetPeriod(period: BudgetPeriod) {
        val current = _budgetConfig.value.copy(activePeriod = period)
        _budgetConfig.value = current
        budgetPrefs.edit().putString("budget_active_period", period.name).apply()
    }

    fun updateBudgetLimits(monthly: Double, quarterly: Double, yearly: Double) {
        val current = _budgetConfig.value.copy(
            monthlyLimit = monthly,
            quarterlyLimit = quarterly,
            yearlyLimit = yearly
        )
        _budgetConfig.value = current
        budgetPrefs.edit()
            .putFloat("budget_monthly", monthly.toFloat())
            .putFloat("budget_quarterly", quarterly.toFloat())
            .putFloat("budget_yearly", yearly.toFloat())
            .apply()
    }

    // Settings
    private val settingsPrefs = application.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    private val _colorScheme = MutableStateFlow(
        try {
            ColorSchemeOption.valueOf(settingsPrefs.getString("color_scheme", ColorSchemeOption.INTERNATIONAL.name) ?: ColorSchemeOption.INTERNATIONAL.name)
        } catch (e: Exception) { ColorSchemeOption.INTERNATIONAL }
    )
    val colorScheme: StateFlow<ColorSchemeOption> = _colorScheme.asStateFlow()

    private val _fontScale = MutableStateFlow(
        try {
            FontScaleOption.valueOf(settingsPrefs.getString("font_scale", FontScaleOption.STANDARD.name) ?: FontScaleOption.STANDARD.name)
        } catch (e: Exception) { FontScaleOption.STANDARD }
    )
    val fontScale: StateFlow<FontScaleOption> = _fontScale.asStateFlow()

    private val _isCategoryAnalysisExpanded = MutableStateFlow(
        settingsPrefs.getBoolean("home_category_analysis_expanded", true)
    )
    val isCategoryAnalysisExpanded: StateFlow<Boolean> = _isCategoryAnalysisExpanded.asStateFlow()

    fun setCategoryAnalysisExpanded(expanded: Boolean) {
        _isCategoryAnalysisExpanded.value = expanded
        settingsPrefs.edit().putBoolean("home_category_analysis_expanded", expanded).apply()
    }

    private fun loadBackgroundConfig(): BackgroundConfig {
        val hasSaved = settingsPrefs.getBoolean("bg_has_saved", false)
        if (!hasSaved) {
            return BackgroundConfig()
        }
        val typeStr = settingsPrefs.getString("bg_type", BackgroundOptionType.PURE_WHITE.name) ?: BackgroundOptionType.PURE_WHITE.name
        val type = try { BackgroundOptionType.valueOf(typeStr) } catch(e: Exception) { BackgroundOptionType.PURE_WHITE }
        val title = settingsPrefs.getString("bg_title", "极简纯白 (默认)") ?: "极简纯白 (默认)"
        val subtitle = settingsPrefs.getString("bg_subtitle", "极致纯粹净白，无暇纯色") ?: "极致纯粹净白，无暇纯色"
        val customHex = settingsPrefs.getString("bg_customHex", "#FFFFFF") ?: "#FFFFFF"
        val isLight = settingsPrefs.getBoolean("bg_isLight", true)
        val imageUri = settingsPrefs.getString("bg_imageUri", null)
        val cardAlpha = settingsPrefs.getFloat("bg_cardAlpha", 0.95f)
        val blurRadius = settingsPrefs.getFloat("bg_blurRadius", 0f)
        val frostAlpha = settingsPrefs.getFloat("bg_frostAlpha", 0.0f)
        val solidColorInt = try { android.graphics.Color.parseColor(customHex) } catch(e: Exception) { android.graphics.Color.WHITE }
        return BackgroundConfig(
            type = type, title = title, subtitle = subtitle, solidColor = Color(solidColorInt),
            isLight = isLight, customHex = customHex, imageUri = imageUri,
            cardAlpha = cardAlpha, blurRadius = blurRadius, frostAlpha = frostAlpha
        )
    }

    private fun saveBackgroundConfig(config: BackgroundConfig) {
        settingsPrefs.edit()
            .putBoolean("bg_has_saved", true)
            .putString("bg_type", config.type.name)
            .putString("bg_title", config.title)
            .putString("bg_subtitle", config.subtitle)
            .putString("bg_customHex", config.customHex)
            .putBoolean("bg_isLight", config.isLight)
            .putString("bg_imageUri", config.imageUri)
            .putFloat("bg_cardAlpha", config.cardAlpha)
            .putFloat("bg_blurRadius", config.blurRadius)
            .putFloat("bg_frostAlpha", config.frostAlpha)
            .apply()
    }

    private val _backgroundConfig = MutableStateFlow(loadBackgroundConfig())
    val backgroundConfig: StateFlow<BackgroundConfig> = _backgroundConfig.asStateFlow()

    fun setColorScheme(option: ColorSchemeOption) {
        _colorScheme.value = option
        settingsPrefs.edit().putString("color_scheme", option.name).apply()
    }

    fun setFontScale(option: FontScaleOption) {
        _fontScale.value = option
        settingsPrefs.edit().putString("font_scale", option.name).apply()
    }

    fun setBackgroundConfig(config: BackgroundConfig) {
        _backgroundConfig.value = config
        saveBackgroundConfig(config)
    }

    fun setCustomBackgroundImage(uriString: String, isLight: Boolean = false) {
        var finalPath = uriString
        try {
            val app = getApplication<Application>()
            val uri = Uri.parse(uriString)
            val inputStream = if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                app.contentResolver.openInputStream(uri)
            } else {
                val file = File(uriString)
                if (file.exists()) file.inputStream() else null
            }

            if (inputStream != null) {
                val filesDir = app.filesDir
                filesDir.listFiles { file -> file.name.startsWith("custom_wallpaper_") }?.forEach { it.delete() }
                val destFile = File(filesDir, "custom_wallpaper_${System.currentTimeMillis()}.jpg")
                destFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputStream.close()
                finalPath = destFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val current = _backgroundConfig.value
        val newConfig = current.copy(
            type = BackgroundOptionType.CUSTOM_IMAGE,
            title = "自定义背景图片",
            subtitle = "个性化壁纸与毛玻璃卡片",
            imageUri = finalPath,
            isLight = isLight
        )
        setBackgroundConfig(newConfig)
    }

    fun setCardAlpha(alpha: Float) {
        val clamped = alpha.coerceIn(0.10f, 0.98f)
        val current = _backgroundConfig.value
        setBackgroundConfig(current.copy(cardAlpha = clamped))
    }

    fun setBlurRadius(radius: Float) {
        val clamped = radius.coerceIn(0f, 30f)
        val current = _backgroundConfig.value
        setBackgroundConfig(current.copy(blurRadius = clamped))
    }

    fun setFrostAlpha(alpha: Float) {
        val clamped = alpha.coerceIn(0f, 0.85f)
        val current = _backgroundConfig.value
        setBackgroundConfig(current.copy(frostAlpha = clamped))
    }

    fun setIsLightBackground(isLight: Boolean) {
        val current = _backgroundConfig.value
        setBackgroundConfig(current.copy(isLight = isLight))
    }

    fun toggleThemeMode(isDark: Boolean) {
        val current = _backgroundConfig.value
        val newType = if (isDark) BackgroundOptionType.SLATE_DARK else BackgroundOptionType.PURE_WHITE
        val newTitle = if (isDark) "墨绿深林 (深色模式)" else "极简纯白 (浅色模式)"
        val newSubtitle = if (isDark) "沉稳深邃暗黑，极佳夜间视觉" else "极致纯粹净白，无暇纯色"
        val newSolidColor = if (isDark) Color(0xFF242E24) else Color(0xFFFFFFFF)
        val newHex = if (isDark) "#242E24" else "#FFFFFF"

        val newConfig = current.copy(
            type = newType,
            title = newTitle,
            subtitle = newSubtitle,
            solidColor = newSolidColor,
            customHex = newHex,
            isLight = !isDark
        )
        setBackgroundConfig(newConfig)
    }

    fun setCustomBackgroundColor(hex: String, name: String = "自定义纯色") {
        try {
            val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
            val parsed = android.graphics.Color.parseColor(cleanHex)
            val color = Color(parsed)
            val r = ((parsed shr 16) and 0xFF) / 255f
            val g = ((parsed shr 8) and 0xFF) / 255f
            val b = (parsed and 0xFF) / 255f
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            val isLight = lum > 0.45f

            val newConfig = BackgroundConfig(
                type = BackgroundOptionType.CUSTOM_SOLID,
                title = name,
                subtitle = "自定义色值: $cleanHex",
                solidColor = color,
                isLight = isLight,
                customHex = cleanHex
            )
            setBackgroundConfig(newConfig)
        } catch (e: Exception) {
            // Ignore format error
        }
    }

    // Filter states
    private val _filterType = MutableStateFlow("ALL") // "ALL", "EXPENSE", "INCOME"
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _filterTime = MutableStateFlow("ALL") // "ALL", "MONTH", "WEEK", "TODAY"
    val filterTime: StateFlow<String> = _filterTime.asStateFlow()

    private val _selectedCategory = MutableStateFlow("ALL")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedAccountFilter = MutableStateFlow<Long?>(null)
    val selectedAccountFilter: StateFlow<Long?> = _selectedAccountFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun setFilterTime(time: String) {
        _filterTime.value = time
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedAccountFilter(accountId: Long?) {
        _selectedAccountFilter.value = accountId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun resetFilters() {
        _filterType.value = "ALL"
        _filterTime.value = "ALL"
        _selectedCategory.value = "ALL"
        _selectedAccountFilter.value = null
        _searchQuery.value = ""
    }

    // Combined filter params
    private data class FilterParams(
        val type: String,
        val time: String,
        val category: String,
        val accountId: Long?,
        val query: String
    )

    private val filterParams = combine(
        _filterType,
        _filterTime,
        _selectedCategory,
        _selectedAccountFilter,
        _searchQuery
    ) { type, time, category, accountId, query ->
        FilterParams(type, time, category, accountId, query)
    }

    // Filtered Expenses for Home & Transactions
    val filteredExpenses: StateFlow<List<ExpenseEntity>> = combine(
        allExpenses,
        filterParams
    ) { list, params ->
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // Today start
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis
        val startOfWeek = now - 7 * 86400000L
        val startOfMonth = now - 30 * 86400000L

        list.filter { item ->
            val matchesType = when (params.type) {
                "EXPENSE" -> item.type == "EXPENSE"
                "INCOME" -> item.type == "INCOME"
                else -> true
            }

            val matchesTime = when (params.time) {
                "TODAY" -> item.dateTimestamp >= startOfToday
                "WEEK" -> item.dateTimestamp >= startOfWeek
                "MONTH" -> item.dateTimestamp >= startOfMonth
                else -> true
            }

            val matchesCategory = (params.category == "ALL" || item.category == params.category)
            val matchesAccount = (params.accountId == null || item.accountId == params.accountId)

            val matchesQuery = if (params.query.isBlank()) true else {
                item.category.contains(params.query, ignoreCase = true) ||
                item.subCategory.contains(params.query, ignoreCase = true) ||
                item.note.contains(params.query, ignoreCase = true) ||
                item.accountName.contains(params.query, ignoreCase = true) ||
                item.amount.toString().contains(params.query)
            }

            matchesType && matchesTime && matchesCategory && matchesAccount && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Consolidated Aggregation Data Model
    data class AggregatedLedgerStats(
        val totalExpense: Double = 0.0,
        val totalIncome: Double = 0.0,
        val todayExpense: Double = 0.0,
        val thisMonthExpense: Double = 0.0,
        val thisMonthIncome: Double = 0.0,
        val thisQuarterExpense: Double = 0.0,
        val thisYearExpense: Double = 0.0,
        val categoryStats: List<CategoryStat> = emptyList(),
        val incomeCategoryStats: List<CategoryStat> = emptyList(),
        val weekTrendPoints: List<TrendPoint> = emptyList(),
        val monthTrendPoints: List<TrendPoint> = emptyList()
    )

    // 单次 O(N) 遍历预先汇总所有收支统计与图表数据，彻底消除 11 个独立 StateFlow 重复全表扫描与 GC 压力
    val aggregatedStats: StateFlow<AggregatedLedgerStats> = allExpenses.map { list ->
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val cal = Calendar.getInstance()

            // Today
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfToday = cal.timeInMillis

            // This Month
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val startOfMonth = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfMonth = cal.timeInMillis

            // This Quarter
            val currentMonth = cal.get(Calendar.MONTH)
            val quarterStartMonth = (currentMonth / 3) * 3
            cal.set(Calendar.MONTH, quarterStartMonth)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfQuarter = cal.timeInMillis
            cal.set(Calendar.MONTH, quarterStartMonth + 2)
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfQuarter = cal.timeInMillis

            // This Year
            cal.set(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfYear = cal.timeInMillis
            cal.set(Calendar.MONTH, Calendar.DECEMBER)
            cal.set(Calendar.DAY_OF_MONTH, 31)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfYear = cal.timeInMillis

            // 7-day trend buckets
            val sdfDay = SimpleDateFormat("MM-dd", Locale.CHINA)
            class DayBucket(val label: String, val start: Long, val end: Long, var expSum: Double = 0.0, var incSum: Double = 0.0)
            val weekBuckets = Array(7) { i ->
                val offset = 6 - i
                val bCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -offset)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val bStart = bCal.timeInMillis
                val bEnd = bStart + 86400000L
                val label = if (offset == 0) "今日" else sdfDay.format(Date(bStart))
                DayBucket(label, bStart, bEnd)
            }

            // 6-month trend buckets
            val sdfMonth = SimpleDateFormat("M月", Locale.CHINA)
            class MonthBucket(val label: String, val start: Long, val end: Long, var expSum: Double = 0.0, var incSum: Double = 0.0)
            val monthBuckets = Array(6) { i ->
                val offset = 5 - i
                val bCal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -offset)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val bStart = bCal.timeInMillis
                bCal.add(Calendar.MONTH, 1)
                val bEnd = bCal.timeInMillis
                MonthBucket(sdfMonth.format(Date(bStart)), bStart, bEnd)
            }

            var totExp = 0.0
            var totInc = 0.0
            var todayExp = 0.0
            var thisMonthExp = 0.0
            var thisMonthInc = 0.0
            var thisQuarterExp = 0.0
            var thisYearExp = 0.0

            class CatAccumulator(var total: Double = 0.0, var count: Int = 0)
            val expCatMap = mutableMapOf<String, CatAccumulator>()
            val incCatMap = mutableMapOf<String, CatAccumulator>()

            for (exp in list) {
                val amt = exp.amount
                val ts = exp.dateTimestamp
                val isExpense = exp.type == "EXPENSE"
                val isIncome = exp.type == "INCOME"

                if (isExpense) {
                    totExp += amt
                    if (ts >= startOfToday) todayExp += amt
                    if (ts in startOfMonth..endOfMonth) thisMonthExp += amt
                    if (ts in startOfQuarter..endOfQuarter) thisQuarterExp += amt
                    if (ts in startOfYear..endOfYear) thisYearExp += amt

                    val acc = expCatMap.getOrPut(exp.category) { CatAccumulator() }
                    acc.total += amt
                    acc.count += 1
                } else if (isIncome) {
                    totInc += amt
                    if (ts in startOfMonth..endOfMonth) thisMonthInc += amt

                    val acc = incCatMap.getOrPut(exp.category) { CatAccumulator() }
                    acc.total += amt
                    acc.count += 1
                }

                // week buckets
                for (b in weekBuckets) {
                    if (ts in b.start until b.end) {
                        if (isExpense) b.expSum += amt
                        else if (isIncome) b.incSum += amt
                        break
                    }
                }

                // month buckets
                for (b in monthBuckets) {
                    if (ts in b.start until b.end) {
                        if (isExpense) b.expSum += amt
                        else if (isIncome) b.incSum += amt
                        break
                    }
                }
            }

            val finalCatStats = if (totExp == 0.0) emptyList() else {
                expCatMap.map { (cat, acc) ->
                    CategoryStat(
                        category = cat,
                        totalAmount = acc.total,
                        count = acc.count,
                        percentage = (acc.total / totExp).toFloat(),
                        type = "EXPENSE"
                    )
                }.sortedByDescending { it.totalAmount }
            }

            val finalIncStats = if (totInc == 0.0) emptyList() else {
                incCatMap.map { (cat, acc) ->
                    CategoryStat(
                        category = cat,
                        totalAmount = acc.total,
                        count = acc.count,
                        percentage = (acc.total / totInc).toFloat(),
                        type = "INCOME"
                    )
                }.sortedByDescending { it.totalAmount }
            }

            val finalWeekPoints = weekBuckets.map { TrendPoint(it.label, it.expSum, it.incSum, it.start) }
            val finalMonthPoints = monthBuckets.map { TrendPoint(it.label, it.expSum, it.incSum, it.start) }

            AggregatedLedgerStats(
                totalExpense = totExp,
                totalIncome = totInc,
                todayExpense = todayExp,
                thisMonthExpense = thisMonthExp,
                thisMonthIncome = thisMonthInc,
                thisQuarterExpense = thisQuarterExp,
                thisYearExpense = thisYearExp,
                categoryStats = finalCatStats,
                incomeCategoryStats = finalIncStats,
                weekTrendPoints = finalWeekPoints,
                monthTrendPoints = finalMonthPoints
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AggregatedLedgerStats())

    // Overall Totals（由 aggregatedStats 派生，无需重复计算）
    val totalExpense: StateFlow<Double> = aggregatedStats
        .map { it.totalExpense }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = aggregatedStats
        .map { it.totalIncome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayExpense: StateFlow<Double> = aggregatedStats
        .map { it.todayExpense }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val thisMonthExpense: StateFlow<Double> = aggregatedStats
        .map { it.thisMonthExpense }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val thisMonthIncome: StateFlow<Double> = aggregatedStats
        .map { it.thisMonthIncome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val thisQuarterExpense: StateFlow<Double> = aggregatedStats
        .map { it.thisQuarterExpense }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val thisYearExpense: StateFlow<Double> = aggregatedStats
        .map { it.thisYearExpense }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Dynamic Budget Progress State
    val budgetProgress: StateFlow<BudgetProgressInfo> = combine(
        _budgetConfig,
        aggregatedStats
    ) { config, stats ->
        val (limit, spent) = when (config.activePeriod) {
            BudgetPeriod.MONTH -> Pair(config.monthlyLimit, stats.thisMonthExpense)
            BudgetPeriod.QUARTER -> Pair(config.quarterlyLimit, stats.thisQuarterExpense)
            BudgetPeriod.YEAR -> Pair(config.yearlyLimit, stats.thisYearExpense)
        }

        val remaining = limit - spent
        val isOver = spent > limit
        val overAmt = if (isOver) spent - limit else 0.0
        val percent = if (limit <= 0) 1.0f else (spent / limit).toFloat().coerceAtLeast(0f)

        val remainingDays = when (config.activePeriod) {
            BudgetPeriod.MONTH -> {
                val cal = Calendar.getInstance()
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                (maxDays - currentDay + 1).coerceAtLeast(1)
            }
            BudgetPeriod.QUARTER -> {
                val cal = Calendar.getInstance()
                val currentMonth = cal.get(Calendar.MONTH)
                val quarterStartMonth = (currentMonth / 3) * 3
                val endCal = Calendar.getInstance()
                endCal.set(Calendar.MONTH, quarterStartMonth + 2)
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val currentDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
                val endDayOfYear = endCal.get(Calendar.DAY_OF_YEAR)
                (endDayOfYear - currentDayOfYear + 1).coerceAtLeast(1)
            }
            BudgetPeriod.YEAR -> {
                val cal = Calendar.getInstance()
                val currentDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
                val maxDaysOfYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
                (maxDaysOfYear - currentDayOfYear + 1).coerceAtLeast(1)
            }
        }

        val dailyAvg = if (!isOver && remaining > 0) remaining / remainingDays else 0.0

        BudgetProgressInfo(
            period = config.activePeriod,
            budgetLimit = limit,
            spentAmount = spent,
            remainingAmount = remaining,
            isOverBudget = isOver,
            overAmount = overAmt,
            progressPercent = percent,
            remainingDailyAverage = dailyAvg,
            remainingDays = remainingDays
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BudgetProgressInfo(BudgetPeriod.MONTH, 5000.0, 0.0, 5000.0, false, 0.0, 0f, 5000.0 / 30, 30)
    )

    // Account Asset Aggregations（单数据源聚合，直接 map 计算，无需冗余 combine）
    val totalNetAssets: StateFlow<Double> = allAccounts.map { accounts ->
        accounts.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPositiveAssets: StateFlow<Double> = allAccounts.map { accounts ->
        accounts.filter { it.balance > 0 }.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalDebts: StateFlow<Double> = allAccounts.map { accounts ->
        accounts.filter { it.balance < 0 }.sumOf { -it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category Expense Breakdown
    val categoryStats: StateFlow<List<CategoryStat>> = aggregatedStats
        .map { it.categoryStats }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category Income Breakdown
    val incomeCategoryStats: StateFlow<List<CategoryStat>> = aggregatedStats
        .map { it.incomeCategoryStats }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7-day Trend Points
    val weekTrendPoints: StateFlow<List<TrendPoint>> = aggregatedStats
        .map { it.weekTrendPoints }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6-Month Trend Points
    val monthTrendPoints: StateFlow<List<TrendPoint>> = aggregatedStats
        .map { it.monthTrendPoints }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expense Actions

    /**
     * 记一笔账（统一入口）。
     *
     * [transferToAccountId] 非空且 type 为 TRANSFER 时走转账路径：
     * 双端复用一条记录、amount 恒为正数，分类字段忽略；
     * 否则按旧版支出/收入语义落库。
     */
    fun addExpense(
        type: String,
        category: String,
        subCategory: String = "",
        amount: Double,
        note: String,
        accountId: Long = 1L,
        accountName: String = "默认账户",
        timestamp: Long = System.currentTimeMillis(),
        transferToAccountId: Long? = null
    ) {
        viewModelScope.launch {
            seedReady.await()
            val targetBookId = currentBook.value?.id ?: repository.defaultBookId()
            val isTransfer = transferToAccountId != null &&
                (type.equals("TRANSFER", ignoreCase = true) || transferToAccountId != accountId)
            if (isTransfer && transferToAccountId != null && transferToAccountId != accountId) {
                repository.addTransfer(
                    fromAccountId = accountId,
                    toAccountId = transferToAccountId,
                    amountYuan = amount,
                    note = note.ifBlank { null },
                    timestamp = timestamp,
                    bookId = targetBookId
                )
            } else {
                repository.insertLegacyExpense(
                    type = type,
                    category = category,
                    subCategory = subCategory,
                    amountYuan = amount,
                    note = note,
                    accountId = accountId,
                    timestamp = timestamp,
                    bookId = targetBookId
                )
            }
        }
    }

    /** 账户间转账独立入口（弹窗「转账」Tab 直连；[onSuccess] 供 UI 关闭动画衔接） */
    fun addTransfer(
        fromId: Long,
        toId: Long,
        amountYuan: Double,
        note: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            seedReady.await()
            if (fromId == toId || amountYuan <= 0.0) return@launch
            val targetBookId = currentBook.value?.id ?: repository.defaultBookId()
            repository.addTransfer(
                fromAccountId = fromId,
                toAccountId = toId,
                amountYuan = amountYuan,
                note = note.ifBlank { null },
                bookId = targetBookId
            )
            onSuccess()
        }
    }

    fun updateExpense(oldExpense: ExpenseEntity, newExpense: ExpenseEntity) {
        viewModelScope.launch {
            seedReady.await()
            repository.updateLegacyExpense(
                old = oldExpense.toSnapshot(),
                new = newExpense.toSnapshot()
            )
        }
    }

    /** 软删除：物理行保留，余额由派生公式自动守恒，无需手工回滚 */
    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            seedReady.await()
            repository.softDeleteTransaction(expense.id)
        }
    }

    private fun ExpenseEntity.toSnapshot() = ToolboxRepositoryV2.ExpenseSnapshot(
        id = id,
        type = type,
        category = category,
        subCategory = subCategory,
        amount = amount,
        note = note,
        dateTimestamp = dateTimestamp,
        accountId = accountId,
        transferToAccountId = transferToAccountId
    )

    // ---------- 分类管理（Phase 2） ----------

    /**
     * 新增分类：[parentName] 为空即一级，否则挂靠到对应一级之下。
     * [type] 传 "expense"/"income"（大小写不敏感）。
     */
    fun addCategory(
        parentName: String?,
        name: String,
        type: String,
        colorHex: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            seedReady.await()
            repository.insertCategory(parentName, name, type, colorHex)
            onSuccess()
        }
    }

    /** 归档分类（软删除语义） */
    fun archiveCategory(categoryId: Long) {
        viewModelScope.launch {
            seedReady.await()
            repository.archiveCategory(categoryId)
        }
    }

    /** 更新分类元信息；传 null 表示保持不变 */
    fun updateCategoryMeta(categoryId: Long, name: String? = null, colorHex: String? = null) {
        viewModelScope.launch {
            seedReady.await()
            repository.updateCategoryMeta(categoryId, name, colorHex)
        }
    }

    // ---------- 账本管理（Phase 2） ----------

    /** 新建账本并自动切换 */
    fun saveBook(name: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            seedReady.await()
            if (name.isNotBlank()) {
                val newId = repository.insertBook(name)
                selectBook(newId)
                onSuccess()
            }
        }
    }

    /** 切换当前选中的账本 */
    fun selectBook(bookId: Long) {
        activeBookId.value = bookId
        viewModelScope.launch {
            seedReady.await()
            repository.setDefaultBook(bookId)
            repository.ensureBookAccounts(bookId)
        }
    }

    /** 重命名账本 */
    fun updateBook(bookId: Long, name: String) {
        viewModelScope.launch {
            seedReady.await()
            repository.updateBookMeta(bookId, name = name)
        }
    }

    /** 设为默认账本（事务内先清旧默认再置位）并同步切换 */
    fun setDefaultBook(bookId: Long) {
        selectBook(bookId)
    }

    /** 归档账本（默认账本会被 Repository 层拒绝，UI 不必预判） */
    fun archiveBook(bookId: Long) {
        viewModelScope.launch {
            seedReady.await()
            repository.archiveBook(bookId)
        }
    }

    /** 清空指定账本的所有数据（流水与账户余额） */
    fun clearBookData(bookId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            seedReady.await()
            repository.clearBookData(bookId)
            onSuccess()
        }
    }

    /** 彻底删除指定账本及其所有明细与账户 */
    fun deleteBook(bookId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            seedReady.await()
            val wasActive = currentBook.value?.id == bookId
            val ok = repository.deleteBook(bookId)
            if (ok && wasActive) {
                val remaining = repository.observeBooks().firstOrNull()?.firstOrNull()
                if (remaining != null) {
                    selectBook(remaining.id)
                }
            }
            onSuccess()
        }
    }

    // Account Actions
    fun addAccount(
        name: String,
        type: String,
        initialBalance: Double,
        cardSuffix: String = "",
        colorHex: String = "#3B82F6",
        note: String = ""
    ) {
        viewModelScope.launch {
            seedReady.await()
            val targetBookId = currentBook.value?.id ?: repository.defaultBookId()
            repository.addAccount(name, type, initialBalance, colorHex, note, targetBookId)
        }
    }

    /**
     * 更新账户元信息并处理余额校准：
     * [saveAsMissedRecord] 为 true 时插入「漏记款」交易；为 false 时直接更新 initialBalance。
     */
    fun updateAccount(
        account: AccountEntity,
        saveAsMissedRecord: Boolean = false,
        oldBalance: Double = account.balance
    ) {
        viewModelScope.launch {
            seedReady.await()
            repository.updateAccountMeta(
                accountId = account.id,
                name = account.name,
                legacyType = account.type,
                colorHex = account.colorHex,
                note = account.note,
                targetBalanceYuan = account.balance,
                previousBalanceYuan = oldBalance,
                createCalibrationTx = saveAsMissedRecord
            )
        }
    }

    /** 归档账户（v2 语义：保留行以保护历史交易外键，不再物理删除） */
    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            seedReady.await()
            repository.archiveAccount(account.id)
        }
    }

    // Generate CSV data for export
    /**
     * v2 版本化导出：首列为格式版本号，导入端据此分派定位解析器。
     * 列序：v2,uuid,日期时间,类型,一级分类,二级分类,账户,对方账户,金额(元),备注,状态
     * 注：allExpenses 为未删除流，状态恒「有效」；导出已删除数据属回收站功能范围。
     */
    fun generateCsvData(): String {
        val accountsList = allAccounts.value
        val list = allExpenses.value
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        val sb = StringBuilder()
        fun esc(raw: String): String =
            "\"" + raw.replace(",", "，").replace("\"", "'") + "\""

        // 1. Accounts Section
        sb.append("# === 资产账户记录 (ACCOUNTS) ===\n")
        sb.append("v3_account,uuid,账户名称,类型,初始余额(元),颜色,备注\n")
        accountsList.forEach { acc ->
            sb.append(
                listOf(
                    "v3_account",
                    esc(""),
                    esc(acc.name),
                    esc(acc.type),
                    String.format(Locale.US, "%.2f", acc.balance),
                    esc(acc.colorHex),
                    esc(acc.note)
                ).joinToString(",") + "\n"
            )
        }

        sb.append("\n# === 收支明细记录 (TRANSACTIONS) ===\n")
        sb.append("v2,uuid,日期时间,类型,一级分类,二级分类,账户,对方账户,金额(元),备注,状态\n")
        list.forEach { item ->
            val dateStr = sdf.format(Date(item.dateTimestamp))
            val typeStr = when (item.type) {
                "INCOME" -> "收入"
                "TRANSFER" -> "转账"
                else -> "支出"
            }
            sb.append(
                listOf(
                    "v2",
                    esc(item.uuid),
                    esc(dateStr),
                    typeStr,
                    esc(item.category),
                    esc(item.subCategory),
                    esc(item.accountName),
                    esc(item.transferToAccountName),
                    String.format(Locale.US, "%.2f", item.amount),
                    esc(item.note),
                    "有效"
                ).joinToString(",") + "\n"
            )
        }
        return sb.toString()
    }

    // Import CSV / text data for ledger
    fun importCsvData(csvContent: String): Pair<Int, String> {
        if (csvContent.isBlank()) {
            return Pair(0, "导入内容为空，请输入或选择有效的数据")
        }

        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return Pair(0, "未检测到有效数据行")
        }

        val currentAccounts = allAccounts.value.toMutableList()
        val sdfFull = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        val sdfDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        val sdfDateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val sdfSlashFull = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINA)
        val sdfSlash = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA)
        val sdfSlashDate = SimpleDateFormat("yyyy/MM/dd", Locale.CHINA)

        fun parseTimestamp(str: String): Long {
            val clean = str.replace("\"", "").trim()
            return try {
                sdfFull.parse(clean)?.time
                    ?: sdfDateTime.parse(clean)?.time
                    ?: sdfSlashFull.parse(clean)?.time
                    ?: sdfSlash.parse(clean)?.time
                    ?: sdfDateOnly.parse(clean)?.time
                    ?: sdfSlashDate.parse(clean)?.time
                    ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }

        fun splitCsvRow(line: String): List<String> {
            val result = mutableListOf<String>()
            var inQuotes = false
            val sb = StringBuilder()
            for (ch in line) {
                when (ch) {
                    '\"' -> inQuotes = !inQuotes
                    ',' -> {
                        if (inQuotes) {
                            sb.append(ch)
                        } else {
                            result.add(sb.toString().trim())
                            sb.clear()
                        }
                    }
                    '，' -> {
                        if (inQuotes) {
                            sb.append(ch)
                        } else {
                            result.add(sb.toString().trim())
                            sb.clear()
                        }
                    }
                    '\t' -> {
                        if (inQuotes) {
                            sb.append(ch)
                        } else {
                            result.add(sb.toString().trim())
                            sb.clear()
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            result.add(sb.toString().trim())
            return result.map { it.removeSurrounding("\"").trim() }
        }

        var successCount = 0
        var accountCount = 0
        val accountTargetBalances = mutableMapOf<String, Double>()
        val targetBookId = currentBook.value?.id ?: 1L

        runBlocking(Dispatchers.IO) {
            seedReady.await()
            for (line in lines) {
                val trimmed = line.trim()
                val lower = trimmed.lowercase()
                if (trimmed.startsWith("#") ||
                    trimmed.contains("收支类型") ||
                    trimmed.contains("一级分类") ||
                    trimmed.contains("交易类型") ||
                    trimmed.contains("子分类") ||
                    trimmed.contains("二级分类") ||
                    trimmed.contains("账户名称") ||
                    lower.startsWith("id,") ||
                    lower.startsWith("id，") ||
                    (lower.contains("时间") && (lower.contains("金额") || lower.contains("分类") || lower.contains("类型")))
                ) {
                    continue
                }

                val tokens = splitCsvRow(line)
                if (tokens.isEmpty()) continue

                try {
                    // Check if account record: v3_account,uuid,账户名称,类型,初始余额,颜色,备注
                    if (tokens[0].equals("v3_account", ignoreCase = true) && tokens.size >= 5) {
                        val accName = tokens[2].ifBlank { "导入账户" }
                        val accType = tokens[3].ifBlank { "储蓄卡" }
                        val initBal = tokens[4].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                        val colorHex = tokens.getOrNull(5)?.takeIf { it.isNotBlank() } ?: "#3B82F6"
                        val note = tokens.getOrNull(6) ?: ""

                        accountTargetBalances[accName] = initBal
                        val existing = currentAccounts.find { it.name.equals(accName, ignoreCase = true) }
                        if (existing == null) {
                            val newId = repository.addAccount(accName, accType, initBal, colorHex, note, bookId = targetBookId)
                            currentAccounts.add(AccountEntity(id = newId, name = accName, type = accType, balance = initBal, colorHex = colorHex, note = note))
                            accountCount++
                        }
                        continue
                    }

                    if (tokens.size < 3) continue

                    var dateTimestamp = System.currentTimeMillis()
                    var type = "EXPENSE"
                    var category = "餐饮"
                    var subCategory = "午餐"
                    var accountName = "默认账户"
                    var amount = 0.0
                    var note = ""
                    var transferToAccountId: Long? = null

                    if (tokens.size >= 11 && tokens[0].equals("v2", ignoreCase = true)) {
                        dateTimestamp = parseTimestamp(tokens[2])
                        type = when {
                            tokens[3].contains("收") || tokens[3].equals("INCOME", ignoreCase = true) -> "INCOME"
                            tokens[3].contains("转") || tokens[3].equals("TRANSFER", ignoreCase = true) -> "TRANSFER"
                            else -> "EXPENSE"
                        }
                        category = tokens[4].ifBlank { if (type == "TRANSFER") "转账" else "其他" }
                        subCategory = tokens[5].ifBlank { if (type == "TRANSFER") "转账" else "" }
                        accountName = tokens[6].ifBlank { "默认账户" }
                        amount = tokens[8].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                        note = tokens[9]
                        if ((tokens[10]).contains("删")) continue
                    } else if (tokens.size >= 8) {
                        dateTimestamp = parseTimestamp(tokens[1])
                        type = when {
                            tokens[2].contains("转") || tokens[2].equals("TRANSFER", ignoreCase = true) || tokens[3].contains("转账") -> "TRANSFER"
                            tokens[2].contains("收") || tokens[2].equals("INCOME", ignoreCase = true) || tokens[3].contains("收入") || tokens[3].contains("工资") -> "INCOME"
                            else -> "EXPENSE"
                        }
                        category = tokens[3].ifBlank { if (type == "TRANSFER") "转账" else "其他" }
                        subCategory = tokens[4].ifBlank { if (type == "TRANSFER") "转账" else "默认" }
                        accountName = tokens[5].ifBlank { "默认账户" }
                        amount = tokens[6].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                        note = tokens[7]
                    } else if (tokens.size == 7) {
                        dateTimestamp = parseTimestamp(tokens[0])
                        type = when {
                            tokens[1].contains("转") || tokens[1].equals("TRANSFER", ignoreCase = true) || tokens[2].contains("转账") -> "TRANSFER"
                            tokens[1].contains("收") || tokens[1].equals("INCOME", ignoreCase = true) || tokens[2].contains("收入") || tokens[2].contains("工资") -> "INCOME"
                            else -> "EXPENSE"
                        }
                        category = tokens[2].ifBlank { if (type == "TRANSFER") "转账" else "其他" }
                        subCategory = tokens[3].ifBlank { if (type == "TRANSFER") "转账" else "默认" }
                        accountName = tokens[4].ifBlank { "默认账户" }
                        amount = tokens[5].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                        note = tokens[6]
                    } else if (tokens.size == 6) {
                        dateTimestamp = parseTimestamp(tokens[0])
                        type = when {
                            tokens[1].contains("转") || tokens[1].equals("TRANSFER", ignoreCase = true) || tokens[2].contains("转账") -> "TRANSFER"
                            tokens[1].contains("收") || tokens[1].equals("INCOME", ignoreCase = true) || tokens[2].contains("收入") || tokens[2].contains("工资") -> "INCOME"
                            else -> "EXPENSE"
                        }
                        category = tokens[2].ifBlank { if (type == "TRANSFER") "转账" else "其他" }
                        subCategory = if (type == "TRANSFER") "转账" else "默认"
                        accountName = tokens[3].ifBlank { "默认账户" }
                        amount = tokens[4].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                        note = tokens[5]
                    } else if (tokens.size == 5) {
                        if (tokens[0].contains("20") || tokens[0].contains("-") || tokens[0].contains("/")) {
                            dateTimestamp = parseTimestamp(tokens[0])
                            category = tokens[1].ifBlank { "餐饮" }
                            accountName = tokens[2].ifBlank { "默认账户" }
                            amount = tokens[3].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                            note = tokens[4]
                            type = when {
                                category.contains("转账") || category.contains("转入") || category.contains("转出") -> "TRANSFER"
                                amount > 0 && (category.contains("工资") || category.contains("奖金") || category.contains("收入")) -> "INCOME"
                                else -> "EXPENSE"
                            }
                        } else {
                            type = when {
                                tokens[0].contains("转") || tokens[0].contains("TRANSFER") -> "TRANSFER"
                                tokens[0].contains("收") || tokens[0].contains("INCOME") -> "INCOME"
                                else -> "EXPENSE"
                            }
                            category = tokens[1].ifBlank { if (type == "TRANSFER") "转账" else "其他" }
                            accountName = tokens[2].ifBlank { "默认账户" }
                            amount = tokens[3].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                            note = tokens[4]
                        }
                    } else if (tokens.size == 4) {
                        category = tokens[0].ifBlank { "餐饮" }
                        amount = tokens[1].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                        accountName = tokens[2].ifBlank { "默认账户" }
                        note = tokens[3]
                        type = when {
                            category.contains("转账") || category.contains("转入") || category.contains("转出") -> "TRANSFER"
                            category.contains("工资") || category.contains("奖金") || category.contains("收入") -> "INCOME"
                            else -> "EXPENSE"
                        }
                    } else if (tokens.size == 3) {
                        category = tokens[0].ifBlank { "餐饮" }
                        amount = tokens[1].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                        note = tokens[2]
                        type = when {
                            category.contains("转账") || category.contains("转入") || category.contains("转出") -> "TRANSFER"
                            category.contains("工资") || category.contains("奖金") || category.contains("收入") -> "INCOME"
                            else -> "EXPENSE"
                        }
                    }

                    // 拆分复合分类名（如 "餐饮/午餐" 或 "交通 - 地铁"）
                    val splitDelimiters = listOf(" / ", "/", " - ", "-", " : ", ":", "：", " > ", ">")
                    for (del in splitDelimiters) {
                        if (category.contains(del)) {
                            val parts = category.split(del, limit = 2)
                            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                                category = parts[0].trim()
                                if (subCategory.isBlank() || subCategory == "默认" || subCategory == "其他") {
                                    subCategory = parts[1].trim()
                                }
                                break
                            }
                        }
                    }

                    if (amount <= 0.0) continue

                    var matchedAcc = currentAccounts.find { it.name.equals(accountName, ignoreCase = true) }
                    if (matchedAcc == null) {
                        val newAccId = repository.addAccount(accountName, "储蓄卡", 0.0, "#3B82F6", "导入自动创建", bookId = targetBookId)
                        val newAcc = AccountEntity(id = newAccId, name = accountName, type = "储蓄卡", balance = 0.0, colorHex = "#3B82F6", note = "导入自动创建")
                        currentAccounts.add(newAcc)
                        matchedAcc = newAcc
                        accountCount++
                    }

                    val accId = matchedAcc.id

                    if (type == "TRANSFER") {
                        category = "转账"
                        subCategory = "转账"
                        val targetAccName = tokens.getOrNull(7)?.trim() ?: ""
                        var targetMatched = currentAccounts.find { it.name.equals(targetAccName, ignoreCase = true) }
                        if (targetMatched == null && targetAccName.isNotBlank()) {
                            val newTargetId = repository.addAccount(targetAccName, "储蓄卡", 0.0, "#8B5CF6", "转账导入自动创建", bookId = targetBookId)
                            val newTargetAcc = AccountEntity(id = newTargetId, name = targetAccName, type = "储蓄卡", balance = 0.0, colorHex = "#8B5CF6", note = "转账导入自动创建")
                            currentAccounts.add(newTargetAcc)
                            targetMatched = newTargetAcc
                            accountCount++
                        }
                        var targetId = targetMatched?.id
                        if (targetId == null || targetId == accId) {
                            val otherAcc = currentAccounts.find { it.id != accId }
                            if (otherAcc != null) {
                                targetId = otherAcc.id
                            } else {
                                val newTargetId = repository.addAccount("目标账户", "储蓄卡", 0.0, "#8B5CF6", "转账导入自动创建", bookId = targetBookId)
                                val newTargetAcc = AccountEntity(id = newTargetId, name = "目标账户", type = "储蓄卡", balance = 0.0, colorHex = "#8B5CF6", note = "转账导入自动创建")
                                currentAccounts.add(newTargetAcc)
                                targetId = newTargetId
                                accountCount++
                            }
                        }
                        transferToAccountId = targetId
                    }

                    if (category == "漏记款" || (category.isBlank() && subCategory == "漏记款")) {
                        category = "居家"
                        subCategory = "漏记款"
                    }

                    repository.insertLegacyExpense(
                        type = type,
                        category = category,
                        subCategory = subCategory,
                        amountYuan = amount,
                        note = note,
                        accountId = accId,
                        timestamp = dateTimestamp,
                        transferToAccountId = transferToAccountId,
                        bookId = targetBookId
                    )
                    successCount++
                } catch (e: Exception) {
                    // Skip malformed item safely ensuring 100% continuation
                }
            }

            // Reverse engineer initial balances based on target balances and transaction history for asset trends
            if (accountTargetBalances.isNotEmpty()) {
                repository.calibrateImportedAccounts(accountTargetBalances, bookId = targetBookId)
            }
        }

        return if (successCount > 0 || accountCount > 0) {
            Pair(successCount, "成功导入 $successCount 笔账目记录" + if (accountCount > 0) "及 $accountCount 个账户" else "")
        } else {
            Pair(0, "未解析到符合格式的有效记录，请核对格式")
        }
    }

    private fun inferIncomeCategoryName(note: String): String {
        return when {
            note.contains("利息") || note.contains("收益") || note.contains("余额宝") -> "利息"
            note.contains("兼职") || note in listOf("众包保证金", "大叹号") || note.contains("外快") || note.contains("副业") -> "兼职外快"
            note.contains("营业") || note.contains("经营") || note.contains("店铺") -> "营业收入"
            note.contains("红包") || note.contains("转账") -> "红包"
            note.contains("销售") || note.contains("闲鱼") || note.contains("二手") -> "销售款"
            note.contains("退款") || note.contains("返款") || note.contains("退货") -> "退款返款"
            note.contains("报销") || note.contains("差旅") -> "报销款"
            note.contains("福利") || note.contains("补贴") || note.contains("餐补") || note.contains("房补") -> "福利补贴"
            note.contains("应收") -> "应收款"
            note.contains("生活费") -> "生活费"
            note.contains("基金") || note.contains("001423") || note.contains("理财") -> "基金"
            note.contains("礼金") || note in listOf("娘", "压岁") || note.contains("随礼") -> "礼金"
            note.contains("分红") || note.contains("股票") -> "分红股票"
            note.contains("公积金") -> "公积金"
            note.contains("赔付") || note.contains("理赔") -> "赔付款"
            note.contains("余额调整") || note.contains("漏记") || note.contains("平账") -> "漏记款"
            note.contains("工资") || note.contains("薪") || note.contains("年终奖") -> "工资薪水"
            else -> "其他"
        }
    }

    private fun inferExpenseCategoryPair(note: String): Pair<String, String> {
        return when {
            note.contains("早") || note.contains("早餐") || note.contains("包子") || note.contains("油条") -> Pair("餐饮", "早餐")
            note.contains("午") || note.contains("午餐") || note.contains("快餐") || note.contains("美团") || note.contains("饿了么") -> Pair("餐饮", "午餐")
            note.contains("晚") || note.contains("晚餐") || note.contains("火锅") || note.contains("烧烤") || note.contains("夜宵") -> Pair("餐饮", "晚餐")
            note.contains("奶茶") || note.contains("咖啡") || note.contains("星巴克") || note.contains("瑞幸") || note.contains("零食") || note.contains("水果") -> Pair("餐饮", "零食")
            note.contains("买菜") || note.contains("生鲜") || note.contains("蔬菜") -> Pair("餐饮", "买菜原料")
            note.contains("吃") || note.contains("餐") || note.contains("饭") -> Pair("餐饮", "餐饮其他")

            note.contains("打车") || note.contains("滴滴") || note.contains("出租") || note.contains("高德") -> Pair("交通", "打车")
            note.contains("公交") || note.contains("巴士") -> Pair("交通", "公交")
            note.contains("地铁") -> Pair("交通", "地铁")
            note.contains("加油") || note.contains("油费") || note.contains("充电") -> Pair("交通", "加油")
            note.contains("停车") -> Pair("交通", "停车费")
            note.contains("火车") || note.contains("高铁") || note.contains("机票") || note.contains("飞机") -> Pair("交通", "火车")
            note.contains("单车") || note.contains("哈啰") -> Pair("交通", "自行车")
            note.contains("交通") || note.contains("出行") || note.contains("路费") -> Pair("交通", "交通其他")

            note.contains("衣服") || note.contains("裤") || note.contains("鞋") || note.contains("包") -> Pair("购物", "服饰鞋包")
            note.contains("超市") || note.contains("日用") || note.contains("纸巾") || note.contains("便利店") -> Pair("购物", "家居百货")
            note.contains("护肤") || note.contains("化妆") || note.contains("口红") || note.contains("面膜") -> Pair("购物", "化妆护肤")
            note.contains("手机") || note.contains("数码") || note.contains("电脑") || note.contains("充电器") -> Pair("购物", "电子数码")
            note.contains("淘宝") || note.contains("京东") || note.contains("拼多多") || note.contains("网购") -> Pair("购物", "购物其他")

            note.contains("电影") || note.contains("影院") -> Pair("娱乐", "电影")
            note.contains("游戏") || note.contains("Steam") || note.contains("充值") -> Pair("娱乐", "网游电玩")
            note.contains("旅游") || note.contains("酒店") || note.contains("民宿") || note.contains("门票") -> Pair("娱乐", "旅游度假")
            note.contains("运动") || note.contains("健身") || note.contains("游泳") || note.contains("羽毛球") -> Pair("娱乐", "运动健身")
            note.contains("猫") || note.contains("狗") || note.contains("宠物") -> Pair("娱乐", "花鸟宠物")

            note.contains("看病") || note.contains("医院") || note.contains("门诊") || note.contains("挂号") -> Pair("医教", "挂号门诊")
            note.contains("药") || note.contains("药店") -> Pair("医教", "医疗药品")
            note.contains("学费") || note.contains("培训") || note.contains("课程") -> Pair("医教", "学杂教材")

            note.contains("话费") || note.contains("手机费") -> Pair("居家", "手机电话")
            note.contains("电费") || note.contains("水费") || note.contains("燃气") || note.contains("水电") -> Pair("居家", "水电燃气")
            note.contains("房租") || note.contains("租房") || note.contains("房贷") -> Pair("居家", "住宿房租")
            note.contains("宽带") || note.contains("网费") -> Pair("居家", "电脑宽带")
            note.contains("理发") || note.contains("剪发") || note.contains("美发") -> Pair("居家", "美发美容")
            note.contains("快递") || note.contains("顺丰") -> Pair("居家", "快递邮政")
            note.contains("日常") || note.contains("生活") -> Pair("居家", "生活费")

            note.contains("红包") || note.contains("随礼") || note.contains("份子") -> Pair("人情", "礼金红包")
            note.contains("请客") || note.contains("送礼") -> Pair("人情", "请客")

            else -> Pair("其他", "")
        }
    }
}
