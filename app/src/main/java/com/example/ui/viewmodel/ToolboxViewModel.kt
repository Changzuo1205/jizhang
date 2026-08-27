package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    /**
     * 过渡期映射层：TransactionEntity（分/外键）→ ExpenseEntity DTO（元/显示名）。
     * 一级分类 = 行分类或其父级；二级分类 = 子级自身；未命中落「其他」。
     * Phase 3 拆分后 UI 直接消费结构化模型，本适配层退役。
     */
    val allExpenses: StateFlow<List<ExpenseEntity>> =
        combine(transactionRows, categoryRows, accountRows) { txs, cats, accs ->
            val catById = cats.associateBy { it.id }
            val accById = accs.associateBy { it.id }
            txs.map { tx ->
                val cat = tx.categoryId?.let { catById[it] }
                val parent = cat?.parentId?.let { catById[it] }
                ExpenseEntity(
                    id = tx.id,
                    type = tx.type.name,
                    category = when {
                        parent != null -> parent.name
                        cat != null -> cat.name
                        else -> "其他"
                    },
                    subCategory = if (parent != null && cat != null) cat.name else "",
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
     * 映射回旧版 AccountEntity DTO 形态供现有页面渲染。
     */
    val allAccounts: StateFlow<List<AccountEntity>> =
        combine(accountRows, transactionRows) { rows, txs ->
            rows.map { a ->
                var inc = 0; var exp = 0; var out = 0; var inn = 0
                for (tx in txs) {
                    when (tx.type) {
                        TransactionType.INCOME -> if (tx.accountId == a.id) inc += tx.amount
                        TransactionType.EXPENSE -> if (tx.accountId == a.id) exp += tx.amount
                        TransactionType.TRANSFER -> {
                            if (tx.accountId == a.id) out += tx.amount
                            if (tx.transferToAccountId == a.id) inn += tx.amount
                        }
                    }
                }
                AccountEntity(
                    id = a.id,
                    name = a.name,
                    type = ToolboxRepositoryV2.v2TypeToLegacy(a.type),
                    balance = AmountFormatter.centsToYuan(a.initialBalance + inc - exp - out + inn),
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
        val current = _backgroundConfig.value
        val newConfig = current.copy(
            type = BackgroundOptionType.CUSTOM_IMAGE,
            title = "自定义背景图片",
            subtitle = "个性化壁纸与毛玻璃卡片",
            imageUri = uriString,
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

    // Overall Totals（由过渡期 DTO 列表派生；数据量大时可下探 DAO SQL 聚合）
    val totalExpense: StateFlow<Double> = allExpenses
        .map { list -> list.filter { it.type == "EXPENSE" }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = allExpenses
        .map { list -> list.filter { it.type == "INCOME" }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayExpense: StateFlow<Double> = allExpenses.map { list ->
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfToday = calendar.timeInMillis
            list.filter { it.type == "EXPENSE" && it.dateTimestamp >= startOfToday }.sumOf { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Current Month Expense (1st of this month to end of this month)
    val thisMonthExpense: StateFlow<Double> = allExpenses.map { list ->
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfMonth = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfMonth = cal.timeInMillis

            list.filter { it.type == "EXPENSE" && it.dateTimestamp in startOfMonth..endOfMonth }.sumOf { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Current Month Income
    val thisMonthIncome: StateFlow<Double> = allExpenses.map { list ->
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfMonth = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfMonth = cal.timeInMillis

            list.filter { it.type == "INCOME" && it.dateTimestamp in startOfMonth..endOfMonth }.sumOf { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Current Quarter Expense
    val thisQuarterExpense: StateFlow<Double> = allExpenses.map { list ->
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val cal = Calendar.getInstance()
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

            list.filter { it.type == "EXPENSE" && it.dateTimestamp in startOfQuarter..endOfQuarter }.sumOf { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Current Year Expense
    val thisYearExpense: StateFlow<Double> = allExpenses.map { list ->
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val cal = Calendar.getInstance()
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

            list.filter { it.type == "EXPENSE" && it.dateTimestamp in startOfYear..endOfYear }.sumOf { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Dynamic Budget Progress State
    val budgetProgress: StateFlow<BudgetProgressInfo> = combine(
        _budgetConfig,
        thisMonthExpense,
        thisQuarterExpense,
        thisYearExpense
    ) { config, mExp, qExp, yExp ->
        val (limit, spent) = when (config.activePeriod) {
            BudgetPeriod.MONTH -> Pair(config.monthlyLimit, mExp)
            BudgetPeriod.QUARTER -> Pair(config.quarterlyLimit, qExp)
            BudgetPeriod.YEAR -> Pair(config.yearlyLimit, yExp)
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
    val categoryStats: StateFlow<List<CategoryStat>> = allExpenses.map { list ->
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val expenseList = list.filter { it.type == "EXPENSE" }
            val sum = expenseList.sumOf { it.amount }
            if (sum == 0.0) {
                emptyList()
            } else {
                expenseList.groupBy { it.category }
                    .map { (cat, items) ->
                        val catSum = items.sumOf { it.amount }
                        CategoryStat(
                            category = cat,
                            totalAmount = catSum,
                            count = items.size,
                            percentage = (catSum / sum).toFloat(),
                            type = "EXPENSE"
                        )
                    }
                    .sortedByDescending { it.totalAmount }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category Income Breakdown
    val incomeCategoryStats: StateFlow<List<CategoryStat>> = allExpenses.map { list ->
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val incomeList = list.filter { it.type == "INCOME" }
            val sum = incomeList.sumOf { it.amount }
            if (sum == 0.0) {
                emptyList()
            } else {
                incomeList.groupBy { it.category }
                    .map { (cat, items) ->
                        val catSum = items.sumOf { it.amount }
                        CategoryStat(
                            category = cat,
                            totalAmount = catSum,
                            count = items.size,
                            percentage = (catSum / sum).toFloat(),
                            type = "INCOME"
                        )
                    }
                    .sortedByDescending { it.totalAmount }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7-day Trend Points
    val weekTrendPoints: StateFlow<List<TrendPoint>> = allExpenses.map { list ->
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val sdf = SimpleDateFormat("MM-dd", Locale.CHINA)
            val points = mutableListOf<TrendPoint>()

            for (i in 6 downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = start + 86400000L

                val dayExpenses = list.filter { it.dateTimestamp in start until end }
                val expSum = dayExpenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val incSum = dayExpenses.filter { it.type == "INCOME" }.sumOf { it.amount }

                val label = if (i == 0) "今日" else sdf.format(Date(start))
                points.add(TrendPoint(label, expSum, incSum, start))
            }
            points
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6-Month Trend Points
    val monthTrendPoints: StateFlow<List<TrendPoint>> = allExpenses.map { list ->
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val labelSdf = SimpleDateFormat("M月", Locale.CHINA)
            val points = mutableListOf<TrendPoint>()

            for (i in 5 downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -i)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.add(Calendar.MONTH, 1)
                val end = cal.timeInMillis

                val monthExpenses = list.filter { it.dateTimestamp in start until end }
                val expSum = monthExpenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val incSum = monthExpenses.filter { it.type == "INCOME" }.sumOf { it.amount }

                points.add(TrendPoint(labelSdf.format(Date(start)), expSum, incSum, start))
            }
            points
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            val isTransfer = transferToAccountId != null &&
                (type.equals("TRANSFER", ignoreCase = true) || transferToAccountId != accountId)
            if (isTransfer && transferToAccountId != null && transferToAccountId != accountId) {
                repository.addTransfer(
                    fromAccountId = accountId,
                    toAccountId = transferToAccountId,
                    amountYuan = amount,
                    note = note.ifBlank { null },
                    timestamp = timestamp
                )
            } else {
                repository.insertLegacyExpense(
                    type = type,
                    category = category,
                    subCategory = subCategory,
                    amountYuan = amount,
                    note = note,
                    accountId = accountId,
                    timestamp = timestamp
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
            repository.addTransfer(
                fromAccountId = fromId,
                toAccountId = toId,
                amountYuan = amountYuan,
                note = note.ifBlank { null }
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

    /** 新建账本 */
    fun saveBook(name: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            seedReady.await()
            if (name.isNotBlank()) {
                repository.insertBook(name)
                onSuccess()
            }
        }
    }

    /** 重命名账本 */
    fun updateBook(bookId: Long, name: String) {
        viewModelScope.launch {
            seedReady.await()
            repository.updateBookMeta(bookId, name = name)
        }
    }

    /** 设为默认账本（事务内先清旧默认再置位） */
    fun setDefaultBook(bookId: Long) {
        viewModelScope.launch {
            seedReady.await()
            repository.setDefaultBook(bookId)
        }
    }

    /** 归档账本（默认账本会被 Repository 层拒绝，UI 不必预判） */
    fun archiveBook(bookId: Long) {
        viewModelScope.launch {
            seedReady.await()
            repository.archiveBook(bookId)
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
            repository.addAccount(name, type, initialBalance, colorHex, note)
        }
    }

    /**
     * 更新账户元信息并处理余额校准：
     * 新旧展示余额不一致时插入「漏记款」调整记录，保持派生守恒。
     * [saveAsMissedRecord] 为旧 API 兼容参数，当前统一按可见记录口径实现。
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
                previousBalanceYuan = oldBalance
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
        val list = allExpenses.value
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        val sb = StringBuilder()
        sb.append("v2,uuid,日期时间,类型,一级分类,二级分类,账户,对方账户,金额(元),备注,状态\n")
        fun esc(raw: String): String =
            "\"" + raw.replace(",", "，").replace("\"", "'") + "\""
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

        viewModelScope.launch {
            seedReady.await()
            for (line in lines) {
                // Skip header lines
                if (line.contains("收支类型") || line.contains("一级分类") || line.startsWith("ID,") || line.startsWith("ID，")) {
                    continue
                }

                val tokens = splitCsvRow(line)
                if (tokens.size < 3) continue

                var dateTimestamp = System.currentTimeMillis()
                var type = "EXPENSE"
                var category = "餐饮"
                var subCategory = "午餐"
                var accountName = "默认账户"
                var amount = 0.0
                var note = ""
                var transferToAccountId: Long? = null

                try {
                    if (tokens.size >= 11 && tokens[0].equals("v2", ignoreCase = true)) {
                        // v2 定位解析：v2,uuid,日期时间,类型,一级,二级,账户,对方账户,金额,备注,状态
                        dateTimestamp = parseTimestamp(tokens[2])
                        type = when {
                            tokens[3].contains("收") || tokens[3].equals("INCOME", ignoreCase = true) -> "INCOME"
                            tokens[3].contains("转") || tokens[3].equals("TRANSFER", ignoreCase = true) -> "TRANSFER"
                            else -> "EXPENSE"
                        }
                        category = tokens[4].ifBlank { "其他" }
                        subCategory = tokens[5]
                        accountName = tokens[6].ifBlank { "默认账户" }
                        amount = tokens[8].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                        note = tokens[9]
                        if ((tokens[10]).contains("删")) {
                            // v2 导出的均为有效行；若手工置为已删除则跳过导入
                            continue
                        }
                    } else if (tokens.size >= 8) {
                        // ID, 日期时间, 收支类型, 一级分类, 二级细分, 账户, 金额, 备注
                        dateTimestamp = parseTimestamp(tokens[1])
                        type = if (tokens[2].contains("收") || tokens[2].equals("INCOME", ignoreCase = true)) "INCOME" else "EXPENSE"
                        category = tokens[3].ifBlank { "其他" }
                        subCategory = tokens[4].ifBlank { "默认" }
                        accountName = tokens[5].ifBlank { "默认账户" }
                        amount = tokens[6].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                        note = tokens[7]
                    } else if (tokens.size == 7) {
                        // 日期时间, 收支类型, 一级分类, 二级细分, 账户, 金额, 备注
                        dateTimestamp = parseTimestamp(tokens[0])
                        type = if (tokens[1].contains("收") || tokens[1].equals("INCOME", ignoreCase = true)) "INCOME" else "EXPENSE"
                        category = tokens[2].ifBlank { "其他" }
                        subCategory = tokens[3].ifBlank { "默认" }
                        accountName = tokens[4].ifBlank { "默认账户" }
                        amount = tokens[5].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                        note = tokens[6]
                    } else if (tokens.size == 6) {
                        // 日期时间, 收支类型, 一级分类, 账户, 金额, 备注
                        dateTimestamp = parseTimestamp(tokens[0])
                        type = if (tokens[1].contains("收") || tokens[1].equals("INCOME", ignoreCase = true)) "INCOME" else "EXPENSE"
                        category = tokens[2].ifBlank { "其他" }
                        subCategory = "默认"
                        accountName = tokens[3].ifBlank { "默认账户" }
                        amount = tokens[4].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                        note = tokens[5]
                    } else if (tokens.size == 5) {
                        // 日期, 分类, 账户, 金额, 备注
                        if (tokens[0].contains("20") || tokens[0].contains("-") || tokens[0].contains("/")) {
                            dateTimestamp = parseTimestamp(tokens[0])
                            category = tokens[1].ifBlank { "餐饮" }
                            accountName = tokens[2].ifBlank { "默认账户" }
                            amount = tokens[3].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                            note = tokens[4]
                            type = if (amount > 0 && (category.contains("工资") || category.contains("奖金") || category.contains("收入"))) "INCOME" else "EXPENSE"
                        } else {
                            type = if (tokens[0].contains("收")) "INCOME" else "EXPENSE"
                            category = tokens[1].ifBlank { "其他" }
                            accountName = tokens[2].ifBlank { "默认账户" }
                            amount = tokens[3].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                            note = tokens[4]
                        }
                    } else if (tokens.size == 4) {
                        category = tokens[0].ifBlank { "餐饮" }
                        amount = tokens[1].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                        accountName = tokens[2].ifBlank { "默认账户" }
                        note = tokens[3]
                        type = if (category.contains("工资") || category.contains("奖金") || category.contains("收入")) "INCOME" else "EXPENSE"
                    } else if (tokens.size == 3) {
                        category = tokens[0].ifBlank { "餐饮" }
                        amount = tokens[1].replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                        note = tokens[2]
                        type = if (category.contains("工资") || category.contains("奖金") || category.contains("收入")) "INCOME" else "EXPENSE"
                    }

                    if (amount <= 0.0) continue

                    val matchedAcc = currentAccounts.find { it.name.equals(accountName, ignoreCase = true) }
                        ?: currentAccounts.firstOrNull()

                    val accId = matchedAcc?.id ?: 1L
                    val safeAccName = matchedAcc?.name ?: accountName

                    if (type == "TRANSFER") {
                        // 对端账户按名称回查 id；找不到则放弃本行
                        val targetId = currentAccounts.find { it.name.equals(tokens.getOrNull(7)?.trim() ?: "", ignoreCase = true) }?.id
                        if (targetId == null || targetId == accId) continue
                        transferToAccountId = targetId
                    }

                    repository.insertLegacyExpense(
                        type = type,
                        category = category,
                        subCategory = subCategory,
                        amountYuan = amount,
                        note = note,
                        accountId = accId,
                        timestamp = dateTimestamp,
                        transferToAccountId = transferToAccountId
                    )
                    successCount++
                } catch (e: Exception) {
                    // Skip malformed item
                }
            }
        }

        return if (successCount > 0) {
            Pair(successCount, "已成功导入 $successCount 笔账目记录！")
        } else {
            Pair(0, "未解析到符合格式的有效账目记录，请核对格式")
        }
    }
}
