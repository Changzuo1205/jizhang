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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.ui.components.AppTab
import com.example.ui.components.GlassBottomNavBar
import com.example.ui.theme.LocalAppBackgroundConfig
import com.example.ui.theme.LocalAppColorScheme
import com.example.ui.theme.LocalAppFontScale
import com.example.ui.viewmodel.ToolboxViewModel

/**
 * 次级全屏页面路由枚举
 */
enum class ActiveSubScreen {
    NONE,
    BILL_CALENDAR,     // 账单日历视图
    BUDGET_SETTINGS    // 预算配置管理视图
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
    var triggerAddExpenseInHome by remember { mutableStateOf(false) }

    val colorScheme by viewModel.colorScheme.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val backgroundConfig by viewModel.backgroundConfig.collectAsState()

    // Data States
    val expenses by viewModel.filteredExpenses.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()

    val totalExpense by viewModel.totalExpense.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val todayExpense by viewModel.todayExpense.collectAsState()
    val thisMonthExpense by viewModel.thisMonthExpense.collectAsState()
    val thisMonthIncome by viewModel.thisMonthIncome.collectAsState()
    val budgetConfig by viewModel.budgetConfig.collectAsState()
    val budgetProgress by viewModel.budgetProgress.collectAsState()

    val totalNetAssets by viewModel.totalNetAssets.collectAsState()
    val totalPositiveAssets by viewModel.totalPositiveAssets.collectAsState()
    val totalDebts by viewModel.totalDebts.collectAsState()

    val categoryStats by viewModel.categoryStats.collectAsState()
    val incomeCategoryStats by viewModel.incomeCategoryStats.collectAsState()
    val weekTrendPoints by viewModel.weekTrendPoints.collectAsState()
    val monthTrendPoints by viewModel.monthTrendPoints.collectAsState()

    val filterType by viewModel.filterType.collectAsState()
    val filterTime by viewModel.filterTime.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

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
                    // Outer transition between Main Tabs and Sub-Screens (Bill Calendar / Budget Settings)
                    AnimatedContent(
                        targetState = activeSubScreen,
                        transitionSpec = {
                            if (targetState != ActiveSubScreen.NONE) {
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
                            ActiveSubScreen.BILL_CALENDAR -> {
                                BillCalendarScreen(
                                    allExpenses = allExpenses,
                                    accounts = accounts,
                                    onAddExpense = { type, cat, subCat, amount, note, accId, accName, timestamp ->
                                        viewModel.addExpense(type, cat, subCat, amount, note, accId, accName, timestamp)
                                    },
                                    onUpdateExpense = { oldExp, newExp ->
                                        viewModel.updateExpense(oldExp, newExp)
                                    },
                                    onDeleteExpense = { viewModel.deleteExpense(it) },
                                    onBack = { activeSubScreen = ActiveSubScreen.NONE }
                                )
                            }

                            ActiveSubScreen.BUDGET_SETTINGS -> {
                                BudgetSettingsScreen(
                                    budgetConfig = budgetConfig,
                                    budgetProgress = budgetProgress,
                                    thisMonthExpense = thisMonthExpense,
                                    onSelectPeriod = { viewModel.setActiveBudgetPeriod(it) },
                                    onUpdateBudgetLimits = { m, q, y -> viewModel.updateBudgetLimits(m, q, y) },
                                    onBack = { activeSubScreen = ActiveSubScreen.NONE }
                                )
                            }

                            ActiveSubScreen.NONE -> {
                                // Tab Content Switcher with smooth crossfade and zero shadow flicker
                                AnimatedContent(
                                    targetState = currentTab,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(200, easing = LinearOutSlowInEasing))
                                            .togetherWith(fadeOut(animationSpec = tween(150, easing = FastOutLinearInEasing)))
                                    },
                                    label = "TabContent"
                                ) { tab ->
                                when (tab) {
                                    AppTab.HOME -> {
                                        ExpenseScreen(
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
                                            showAddDialogTrigger = triggerAddExpenseInHome,
                                            onSetFilterType = { viewModel.setFilterType(it) },
                                            onSetFilterTime = { viewModel.setFilterTime(it) },
                                            onSetSearchQuery = { viewModel.setSearchQuery(it) },
                                            onSelectBudgetPeriod = { viewModel.setActiveBudgetPeriod(it) },
                                            onUpdateBudgetLimits = { m, q, y -> viewModel.updateBudgetLimits(m, q, y) },
                                            onOpenBillCalendar = { activeSubScreen = ActiveSubScreen.BILL_CALENDAR },
                                            onOpenBudgetSettings = { activeSubScreen = ActiveSubScreen.BUDGET_SETTINGS },
                                            onAddExpense = { type, cat, subCat, amount, note, accId, accName, timestamp ->
                                                viewModel.addExpense(type, cat, subCat, amount, note, accId, accName, timestamp)
                                            },
                                            onUpdateExpense = { oldExp, newExp ->
                                                viewModel.updateExpense(oldExp, newExp)
                                            },
                                            onDeleteExpense = { viewModel.deleteExpense(it) },
                                            onCloseAddDialogTrigger = { triggerAddExpenseInHome = false }
                                        )
                                    }

                                    AppTab.ACCOUNTS -> {
                                        AccountsScreen(
                                            accounts = accounts,
                                            expenses = allExpenses,
                                            totalNetAssets = totalNetAssets,
                                            totalPositiveAssets = totalPositiveAssets,
                                            totalDebts = totalDebts,
                                            onAddAccount = { name, type, balance, suffix, color, note ->
                                                viewModel.addAccount(name, type, balance, suffix, color, note)
                                            },
                                            onUpdateAccount = { account, saveAsMissedRecord, oldBalance ->
                                                viewModel.updateAccount(account, saveAsMissedRecord, oldBalance)
                                            },
                                            onDeleteAccount = { viewModel.deleteAccount(it) }
                                        )
                                    }

                                    AppTab.REPORTS -> {
                                        ReportsScreen(
                                            expenses = allExpenses,
                                            categoryStats = categoryStats,
                                            incomeCategoryStats = incomeCategoryStats,
                                            weekTrendPoints = weekTrendPoints,
                                            monthTrendPoints = monthTrendPoints,
                                            totalExpense = totalExpense,
                                            totalIncome = totalIncome
                                        )
                                    }

                                    AppTab.MINE -> {
                                        MineScreen(
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
                                            onDeleteExpense = { viewModel.deleteExpense(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Frosted Glass Bottom Navigation Bar: Hidden when inside sub-screens (e.g. BillCalendarScreen)
                AnimatedVisibility(
                    visible = activeSubScreen == ActiveSubScreen.NONE,
                    enter = fadeIn() + slideInVertically { height -> height },
                    exit = fadeOut() + slideOutVertically { height -> height },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    GlassBottomNavBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        onOpenAddExpense = {
                            // User clicked the highlighted glowing "+" button while on HOME tab
                            triggerAddExpenseInHome = true
                        }
                    )
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
