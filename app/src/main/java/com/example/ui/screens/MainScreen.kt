package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import com.example.ui.components.AppTab
import com.example.ui.components.GlassBottomNavBar
import com.example.data.local.ExpenseEntity
import com.example.ui.theme.LocalAppBackgroundConfig
import com.example.ui.theme.LocalAppColorScheme
import com.example.ui.theme.LocalAppFontScale
import com.example.ui.viewmodel.ToolboxViewModel
import java.util.Calendar

/**
 * 次级全屏页面路由枚举
 */
enum class ActiveSubScreen {
    NONE,
    ADD_EXPENSE,       // 记一笔 / 编辑账目全屏视图
    BILL_CALENDAR,     // 账单日历视图
    BUDGET_SETTINGS,   // 预算配置管理视图
    BOOKS,             // 账本管理视图（Phase 2）
    CATEGORIES         // 分类管理视图（Phase 2）
}

/**
 * 应用程序根级 Composable 容器 (MainScreen)
 *
 * 核心架构特性：
 * 1. 提供 CompositionLocal 注入全局主题色、字体缩放因子与动态壁纸配置。
 * 2. 管理底部四大主 Tab（首页、账户、图表统计、我的）与次级全屏子页面（账单日历、预算设置）之间的平滑转场切换。
 * 3. 承载悬浮半透明玻璃态底部导航栏 ([GlassBottomNavBar])。
 */
@Composable
fun MainScreen(
    viewModel: ToolboxViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(AppTab.HOME) }
    var activeSubScreen by remember { mutableStateOf(ActiveSubScreen.NONE) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
    var addExpenseTimestamp by remember { mutableStateOf<Long?>(null) }

    // Splash overlay：总时长 2200ms（1.4s 动效 + 800ms 停留），确保完整展示动效
    var splashDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(2200)
        splashDone = true
    }

    val colorScheme by viewModel.colorScheme.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val backgroundConfig by viewModel.backgroundConfig.collectAsStateWithLifecycle()

    // Data States
    val expenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()

    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val todayExpense by viewModel.todayExpense.collectAsStateWithLifecycle()
    val thisMonthExpense by viewModel.thisMonthExpense.collectAsStateWithLifecycle()
    val thisMonthIncome by viewModel.thisMonthIncome.collectAsStateWithLifecycle()
    val budgetConfig by viewModel.budgetConfig.collectAsStateWithLifecycle()
    val budgetProgress by viewModel.budgetProgress.collectAsStateWithLifecycle()

    val totalNetAssets by viewModel.totalNetAssets.collectAsStateWithLifecycle()
    val totalPositiveAssets by viewModel.totalPositiveAssets.collectAsStateWithLifecycle()
    val totalDebts by viewModel.totalDebts.collectAsStateWithLifecycle()

    val categoryStats by viewModel.categoryStats.collectAsStateWithLifecycle()
    val incomeCategoryStats by viewModel.incomeCategoryStats.collectAsStateWithLifecycle()
    val weekTrendPoints by viewModel.weekTrendPoints.collectAsStateWithLifecycle()
    val monthTrendPoints by viewModel.monthTrendPoints.collectAsStateWithLifecycle()

    // Phase 2 管理页数据源
    val books by viewModel.books.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val filterTime by viewModel.filterTime.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isCategoryAnalysisExpanded by viewModel.isCategoryAnalysisExpanded.collectAsStateWithLifecycle()

    // Dynamic scaled typography based on fontScale setting
    val baseTypography = MaterialTheme.typography
    val scale = fontScale.scale
    val scaledTypography = remember(scale, baseTypography) {
        if (scale == 1.0f) baseTypography else {
            Typography(
                displayLarge = baseTypography.displayLarge.scale(scale),
                displayMedium = baseTypography.displayMedium.scale(scale),
                displaySmall = baseTypography.displaySmall.scale(scale),
                headlineLarge = baseTypography.headlineLarge.scale(scale),
                headlineMedium = baseTypography.headlineMedium.scale(scale),
                headlineSmall = baseTypography.headlineSmall.scale(scale),
                titleLarge = baseTypography.titleLarge.scale(scale),
                titleMedium = baseTypography.titleMedium.scale(scale),
                titleSmall = baseTypography.titleSmall.scale(scale),
                bodyLarge = baseTypography.bodyLarge.scale(scale),
                bodyMedium = baseTypography.bodyMedium.scale(scale),
                bodySmall = baseTypography.bodySmall.scale(scale),
                labelLarge = baseTypography.labelLarge.scale(scale),
                labelMedium = baseTypography.labelMedium.scale(scale),
                labelSmall = baseTypography.labelSmall.scale(scale)
            )
        }
    }

    CompositionLocalProvider(
        LocalAppColorScheme provides colorScheme,
        LocalAppFontScale provides fontScale,
        LocalAppBackgroundConfig provides backgroundConfig
    ) {
        MaterialTheme(
            typography = scaledTypography
        ) {
            Surface(
                modifier = modifier.fillMaxSize(),
                color = if (backgroundConfig.isLight) Color(0xFFF6F8FC) else Color(0xFF090D16)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = splashDone,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing))
                                .togetherWith(fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)))
                        },
                        label = "SplashToMainTransition"
                    ) { isDone ->
                        if (isDone) {
                            MainScreenContent(
                                currentTab = currentTab,
                                onCurrentTabChange = { currentTab = it },
                                activeSubScreen = activeSubScreen,
                                onActiveSubScreenChange = { activeSubScreen = it },
                                expenseToEdit = expenseToEdit,
                                addExpenseTimestamp = addExpenseTimestamp,
                                onOpenAddExpenseWithItem = { ts, exp ->
                                    expenseToEdit = exp
                                    addExpenseTimestamp = ts
                                    activeSubScreen = ActiveSubScreen.ADD_EXPENSE
                                },
                                onCloseAddExpense = {
                                    expenseToEdit = null
                                    addExpenseTimestamp = null
                                    activeSubScreen = ActiveSubScreen.NONE
                                },
                                isCategoryAnalysisExpanded = isCategoryAnalysisExpanded,
                                onToggleCategoryAnalysisExpanded = { viewModel.setCategoryAnalysisExpanded(!isCategoryAnalysisExpanded) },
                                expenses = expenses,
                                accounts = accounts,
                                allExpenses = allExpenses,
                                thisMonthExpense = thisMonthExpense,
                                thisMonthIncome = thisMonthIncome,
                                totalExpense = totalExpense,
                                totalIncome = totalIncome,
                                todayExpense = todayExpense,
                                budgetConfig = budgetConfig,
                                budgetProgress = budgetProgress,
                                categoryStats = categoryStats,
                                filterType = filterType,
                                filterTime = filterTime,
                                searchQuery = searchQuery,
                                totalNetAssets = totalNetAssets,
                                totalPositiveAssets = totalPositiveAssets,
                                totalDebts = totalDebts,
                                incomeCategoryStats = incomeCategoryStats,
                                weekTrendPoints = weekTrendPoints,
                                monthTrendPoints = monthTrendPoints,
                                books = books,
                                categories = categories,
                                colorScheme = colorScheme,
                                fontScale = fontScale,
                                backgroundConfig = backgroundConfig,
                                viewModel = viewModel
                            )
                        } else {
                            // Splash phase: render only splash, skip home composition entirely
                            SplashScreen(splashDone = splashDone)
                        }
                    }
                }
            }
        }
    }
}

private fun TextStyle.scale(factor: Float): TextStyle {
    return this.copy(
        fontSize = this.fontSize * factor,
        lineHeight = this.lineHeight * factor
    )
}

@Composable
private fun MainScreenContent(
    currentTab: AppTab,
    onCurrentTabChange: (AppTab) -> Unit,
    activeSubScreen: ActiveSubScreen,
    onActiveSubScreenChange: (ActiveSubScreen) -> Unit,
    expenseToEdit: com.example.data.local.ExpenseEntity?,
    addExpenseTimestamp: Long?,
    onOpenAddExpenseWithItem: (Long?, com.example.data.local.ExpenseEntity?) -> Unit,
    onCloseAddExpense: () -> Unit,
    isCategoryAnalysisExpanded: Boolean,
    onToggleCategoryAnalysisExpanded: () -> Unit,
    expenses: List<com.example.data.local.ExpenseEntity>,
    accounts: List<com.example.data.local.AccountEntity>,
    allExpenses: List<com.example.data.local.ExpenseEntity>,
    thisMonthExpense: Double,
    thisMonthIncome: Double,
    totalExpense: Double,
    totalIncome: Double,
    todayExpense: Double,
    budgetConfig: com.example.ui.viewmodel.BudgetConfig,
    budgetProgress: com.example.ui.viewmodel.BudgetProgressInfo,
    categoryStats: List<com.example.ui.viewmodel.CategoryStat>,
    filterType: String,
    filterTime: String,
    searchQuery: String,
    totalNetAssets: Double,
    totalPositiveAssets: Double,
    totalDebts: Double,
    incomeCategoryStats: List<com.example.ui.viewmodel.CategoryStat>,
    weekTrendPoints: List<com.example.ui.viewmodel.TrendPoint>,
    monthTrendPoints: List<com.example.ui.viewmodel.TrendPoint>,
    books: List<com.example.data.local.entity.BookEntity>,
    categories: List<com.example.data.local.entity.CategoryEntity>,
    colorScheme: com.example.ui.theme.ColorSchemeOption,
    fontScale: com.example.ui.theme.FontScaleOption,
    backgroundConfig: com.example.ui.theme.BackgroundConfig,
    viewModel: ToolboxViewModel
) {
    var selectedCalendarDay by remember { mutableStateOf<Int?>(null) }

    var homeSelectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var homeIsCalendarExpanded by remember { mutableStateOf(false) }
    val homePagerState = rememberPagerState(initialPage = 500, pageCount = { 1000 })
    var homeForceDarkPreview by remember { mutableStateOf<Boolean?>(null) }
    var homeEntranceAnimationPlayed by remember { mutableStateOf(false) }

    // Outer transition between Main Tabs and Sub-Screens (Add Expense / Bill Calendar / Budget Settings)
    AnimatedContent(
        targetState = activeSubScreen,
        transitionSpec = {
            if (targetState == ActiveSubScreen.ADD_EXPENSE) {
                (slideInVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { height -> height } + fadeIn(animationSpec = tween(200)))
                    .togetherWith(
                        slideOutVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)) { height -> (height * 0.15f).toInt() } + fadeOut(animationSpec = tween(150))
                    )
            } else if (initialState == ActiveSubScreen.ADD_EXPENSE) {
                (slideInVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)) { height -> -(height * 0.15f).toInt() } + fadeIn(animationSpec = tween(150)))
                    .togetherWith(
                        slideOutVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { height -> height } + fadeOut(animationSpec = tween(200))
                    )
            } else if (targetState != ActiveSubScreen.NONE) {
                (slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { width -> (width * 0.35f).toInt() } + fadeIn(animationSpec = tween(240)))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { width -> -(width * 0.15f).toInt() } + fadeOut(animationSpec = tween(180))
                    )
            } else {
                (slideInHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { width -> -(width * 0.15f).toInt() } + fadeIn(animationSpec = tween(180)))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { width -> (width * 0.35f).toInt() } + fadeOut(animationSpec = tween(200))
                    )
            }
        },
        label = "SubScreenTransition"
    ) { subScreen ->
        when (subScreen) {
            ActiveSubScreen.ADD_EXPENSE -> {
                EditorialExpenseAddEditScreen(
                    expenseToEdit = expenseToEdit,
                    allExpenses = allExpenses,
                    accounts = accounts,
                    initialTimestamp = expenseToEdit?.dateTimestamp ?: (addExpenseTimestamp ?: System.currentTimeMillis()),
                    isPreviewMode = false,
                    onDismiss = onCloseAddExpense,
                    onConfirm = { type, cat, subCat, amount, note, accId, accName, timestamp, transferToAccountId ->
                        if (expenseToEdit == null) {
                            viewModel.addExpense(type, cat, subCat, amount, note, accId, accName, timestamp, transferToAccountId)
                        } else {
                            viewModel.updateExpense(
                                expenseToEdit,
                                expenseToEdit.copy(
                                    type = type,
                                    category = cat,
                                    subCategory = subCat,
                                    amount = amount,
                                    note = note,
                                    accountId = accId,
                                    accountName = accName,
                                    dateTimestamp = timestamp,
                                    transferToAccountId = transferToAccountId ?: 0L
                                )
                            )
                        }
                        onCloseAddExpense()
                    }
                )
            }
            ActiveSubScreen.BILL_CALENDAR -> {
                BillCalendarScreen(
                    allExpenses = allExpenses,
                    accounts = accounts,
                    onAddExpense = { type, cat, subCat, amount, note, accId, accName, timestamp, transferToAccountId ->
                        viewModel.addExpense(type, cat, subCat, amount, note, accId, accName, timestamp, transferToAccountId)
                    },
                    onUpdateExpense = { oldExp, newExp -> viewModel.updateExpense(oldExp, newExp) },
                    onDeleteExpense = { viewModel.deleteExpense(it) },
                    onBack = { onActiveSubScreenChange(ActiveSubScreen.NONE) }
                )
            }
            ActiveSubScreen.BUDGET_SETTINGS -> {
                BudgetSettingsScreen(
                    budgetConfig = budgetConfig,
                    budgetProgress = budgetProgress,
                    thisMonthExpense = thisMonthExpense,
                    onSelectPeriod = { viewModel.setActiveBudgetPeriod(it) },
                    onUpdateBudgetLimits = { m, q, y -> viewModel.updateBudgetLimits(m, q, y) },
                    onBack = { onActiveSubScreenChange(ActiveSubScreen.NONE) }
                )
            }
            ActiveSubScreen.BOOKS -> {
                BooksScreen(
                    books = books,
                    onCreateBook = { name -> viewModel.saveBook(name) },
                    onRenameBook = { id, name -> viewModel.updateBook(id, name) },
                    onSetDefaultBook = { viewModel.setDefaultBook(it) },
                    onArchiveBook = { viewModel.archiveBook(it) },
                    onBack = { onActiveSubScreenChange(ActiveSubScreen.NONE) }
                )
            }
            ActiveSubScreen.CATEGORIES -> {
                CategoryManageScreen(
                    categories = categories,
                    onCreateCategory = { parentName, name, type -> viewModel.addCategory(parentName, name, type) },
                    onArchiveCategory = { viewModel.archiveCategory(it) },
                    onBack = { onActiveSubScreenChange(ActiveSubScreen.NONE) }
                )
            }
            ActiveSubScreen.NONE -> {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200, easing = LinearOutSlowInEasing))
                            .togetherWith(fadeOut(animationSpec = tween(150, easing = FastOutLinearInEasing)))
                    },
                    label = "TabContent"
                ) { tab ->
                    when (tab) {
                        AppTab.HOME -> EditorialPreviewScreen(
                            expenses = expenses,
                            accounts = accounts,
                            allExpenses = allExpenses,
                            thisMonthExpense = thisMonthExpense,
                            thisMonthIncome = thisMonthIncome,
                            totalExpense = totalExpense,
                            totalIncome = totalIncome,
                            todayExpense = todayExpense,
                            totalNetAssets = totalNetAssets,
                            totalPositiveAssets = totalPositiveAssets,
                            totalDebts = totalDebts,
                            categoryStats = categoryStats,
                            budgetConfig = budgetConfig,
                            budgetProgress = budgetProgress,
                            onOpenBudgetSettings = { onActiveSubScreenChange(ActiveSubScreen.BUDGET_SETTINGS) },
                            onOpenAddExpense = { onOpenAddExpenseWithItem(null, null) },
                            onEditExpense = { exp -> onOpenAddExpenseWithItem(exp.dateTimestamp, exp) },
                            selectedDateMillis = homeSelectedDateMillis,
                            onSelectedDateChange = { homeSelectedDateMillis = it },
                            isCalendarExpanded = homeIsCalendarExpanded,
                            onCalendarExpandedChange = { homeIsCalendarExpanded = it },
                            pagerState = homePagerState,
                            forceDarkPreview = homeForceDarkPreview,
                            onForceDarkPreviewChange = { homeForceDarkPreview = it },
                            playEntranceAnimation = !homeEntranceAnimationPlayed,
                            onEntranceAnimationPlayed = { homeEntranceAnimationPlayed = true }
                        )
                        AppTab.DESIGN -> EditorialAccountsDesignPreviewScreen(
                            realAccounts = accounts,
                            realExpenses = allExpenses,
                            realTotalNetAssets = totalNetAssets,
                            realTotalPositiveAssets = totalPositiveAssets,
                            realTotalDebts = totalDebts,
                            onAddAccount = { name, type, balance, suffix, color, note -> viewModel.addAccount(name, type, balance, suffix, color, note) },
                            onUpdateAccount = { account, saveAsMissedRecord, oldBalance -> viewModel.updateAccount(account, saveAsMissedRecord, oldBalance) },
                            onDeleteAccount = { viewModel.deleteAccount(it) }
                        )
                        AppTab.ACCOUNTS -> AccountsScreen(
                            accounts = accounts,
                            expenses = allExpenses,
                            totalNetAssets = totalNetAssets,
                            totalPositiveAssets = totalPositiveAssets,
                            totalDebts = totalDebts,
                            onAddAccount = { name, type, balance, suffix, color, note -> viewModel.addAccount(name, type, balance, suffix, color, note) },
                            onUpdateAccount = { account, saveAsMissedRecord, oldBalance -> viewModel.updateAccount(account, saveAsMissedRecord, oldBalance) },
                            onDeleteAccount = { viewModel.deleteAccount(it) }
                        )
                        AppTab.REPORTS -> ReportsScreen(
                            expenses = allExpenses,
                            categoryStats = categoryStats,
                            incomeCategoryStats = incomeCategoryStats,
                            weekTrendPoints = weekTrendPoints,
                            monthTrendPoints = monthTrendPoints,
                            totalExpense = totalExpense,
                            totalIncome = totalIncome
                        )
                        AppTab.MINE -> MineScreen(
                            expenses = allExpenses,
                            accountsCount = accounts.size,
                            currentColorScheme = colorScheme,
                            currentFontScale = fontScale,
                            currentBackgroundConfig = backgroundConfig,
                            onSelectColorScheme = { viewModel.setColorScheme(it) },
                            onSelectFontScale = { viewModel.setFontScale(it) },
                            onSelectBackgroundConfig = { viewModel.setBackgroundConfig(it) },
                            onSetCustomColor = { hex, name -> viewModel.setCustomBackgroundColor(hex, name) },
                            onSetCustomImage = { uri, isLight -> viewModel.setCustomBackgroundImage(uri, isLight) },
                            onSetCardAlpha = { viewModel.setCardAlpha(it) },
                            onSetBlurRadius = { viewModel.setBlurRadius(it) },
                            onSetFrostAlpha = { viewModel.setFrostAlpha(it) },
                            onSetIsLight = { viewModel.setIsLightBackground(it) },
                            onGenerateCsv = { viewModel.generateCsvData() },
                            onImportCsv = { viewModel.importCsvData(it) },
                            onUpdateExpense = { oldExp, newExp -> viewModel.updateExpense(oldExp, newExp) },
                            onDeleteExpense = { viewModel.deleteExpense(it) },
                            onOpenBooks = { onActiveSubScreenChange(ActiveSubScreen.BOOKS) },
                            onOpenCategories = { onActiveSubScreenChange(ActiveSubScreen.CATEGORIES) }
                        )
                    }
                }
            }
        }
    }

    // Frosted Glass Bottom Navigation Bar
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = activeSubScreen == ActiveSubScreen.NONE,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            GlassBottomNavBar(
                currentTab = currentTab,
                onTabSelected = onCurrentTabChange,
                onOpenAddExpense = {
                    val addTimestamp = if (selectedCalendarDay != null) {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.DAY_OF_MONTH, selectedCalendarDay!!)
                        cal.timeInMillis
                    } else {
                        null
                    }
                    onOpenAddExpenseWithItem(addTimestamp, null)
                }
            )
        }
    }
}
