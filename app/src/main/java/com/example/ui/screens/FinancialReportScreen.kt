package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.theme.LocalAppBackgroundConfig
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EditorialPageHeader
import com.example.data.local.AccountEntity
import com.example.data.local.ExpenseEntity
import com.example.model.AmountFormatter
import com.example.ui.components.charts.AnimatedDonutChart
import com.example.ui.components.charts.CurveTrendChart
import com.example.ui.components.charts.DonutSliceData
import com.example.ui.components.charts.MonthAssetPoint
import com.example.ui.components.charts.MonthlyCompareItem
import com.example.ui.components.charts.MonthlyIncomeExpenseBarChart
import com.example.ui.components.charts.WeekdaySpendingBarChart
import com.example.ui.components.charts.WeekdaySpendingItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// 极简手账双色调与自然调色盘
private val ForestGreen = Color(0xFF2D6A4F) // 墨绿 (支出主色)
private val ClayAccent = Color(0xFFC4623D)  // 陶红 (收入主色)
private val SoftSage = Color(0xFF7FA893)    // 浅墨绿过渡
private val SoftTerracotta = Color(0xFFD9A088) // 浅陶红过渡
private val TaupeNeutral = Color(0xFFC9C0A8) // 中性灰褐
private val WarmPaperBg = Color(0xFFF9F8F5) // 暖纸白

private val TextMain = Color(0xFF1C1917)
private val TextMuted = Color(0xFFA8A29E)



/**
 * 极简手账美学 —— 开放式留白报表主页面 (FinancialReportScreen)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialReportScreen(
    accounts: List<AccountEntity>,
    expenses: List<ExpenseEntity>,
    totalNetAssets: Double,
    onEditExpense: ((ExpenseEntity) -> Unit)? = null,
    onDeleteExpense: ((ExpenseEntity) -> Unit)? = null,
    selectedTimeRange: ReportTimeRange = ReportTimeRange.ALL,
    onSelectedTimeRangeChange: (ReportTimeRange) -> Unit = {},
    customStartDate: Long = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis,
    onCustomStartDateChange: (Long) -> Unit = {},
    customEndDate: Long = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis,
    onCustomEndDateChange: (Long) -> Unit = {},
    selectedAccountIds: Set<Long> = emptySet(),
    onSelectedAccountIdsChange: (Set<Long>) -> Unit = {},
    selectedCategoryNames: Set<String> = emptySet(),
    onSelectedCategoryNamesChange: (Set<String>) -> Unit = {},
    minAmountInput: String = "",
    onMinAmountInputChange: (String) -> Unit = {},
    maxAmountInput: String = "",
    onMaxAmountInputChange: (String) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    showSearchRow: Boolean = false,
    onShowSearchRowChange: (Boolean) -> Unit = {},
    selectedExpensePieCategory: String? = null,
    onSelectedExpensePieCategoryChange: (String?) -> Unit = {},
    selectedIncomePieCategory: String? = null,
    onSelectedIncomePieCategoryChange: (String?) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    var showFilterSheet by remember { mutableStateOf(false) }

    val bgConfig = LocalAppBackgroundConfig.current
    val isLight = bgConfig.isLight

    val canvasBg = if (isLight) Color(0xFFFAFAF7) else Color(0xFF242E24)
    val cardBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF1E281E)
    val dividerColor = LocalAppBackgroundConfig.current.dividerColor
    val textMain = if (isLight) Color(0xFF141414) else Color(0xFFFAFAF7)
    val textSecondary = if (isLight) Color(0xFF5A5852) else Color(0xFFB5B3AA)
    val textMuted = if (isLight) Color(0xFF8A8780) else Color(0xFF889689)
    val forestGreen = if (isLight) Color(0xFF2D6A4F) else Color(0xFF52B788)
    val clayAccent = if (isLight) Color(0xFFC4623D) else Color(0xFFE07A5F)

    val minAmountVal = minAmountInput.toDoubleOrNull()
    val maxAmountVal = maxAmountInput.toDoubleOrNull()

    // 基础过滤后的数据源
    val filteredExpenses = remember(
        expenses,
        selectedTimeRange,
        customStartDate,
        customEndDate,
        selectedAccountIds,
        selectedCategoryNames,
        minAmountVal,
        maxAmountVal,
        searchQuery
    ) {
        val bounds = selectedTimeRange.getBounds(customStartDate, customEndDate)
        expenses.filter { exp ->
            val timeMatch = bounds == null || (exp.dateTimestamp in bounds.first..bounds.second)
            val isTransfer = exp.type == "TRANSFER"
            val accountMatch = selectedAccountIds.isEmpty() ||
                    exp.accountId in selectedAccountIds ||
                    (isTransfer && exp.transferToAccountId in selectedAccountIds)
            val categoryMatch = selectedCategoryNames.isEmpty() || exp.displayCategory in selectedCategoryNames
            val minMatch = minAmountVal == null || exp.amount >= minAmountVal
            val maxMatch = maxAmountVal == null || exp.amount <= maxAmountVal
            val searchMatch = searchQuery.isBlank() ||
                    exp.displayCategory.contains(searchQuery, ignoreCase = true) ||
                    exp.displaySubCategory.contains(searchQuery, ignoreCase = true) ||
                    exp.note.contains(searchQuery, ignoreCase = true) ||
                    exp.accountName.contains(searchQuery, ignoreCase = true) ||
                    (isTransfer && exp.transferToAccountName.contains(searchQuery, ignoreCase = true))
            timeMatch && accountMatch && categoryMatch && minMatch && maxMatch && searchMatch
        }
    }

    // ----------------- 筛选条件与图表展示逻辑 -----------------
    val hasCategoryFilter = selectedCategoryNames.isNotEmpty()
    val hasAmountFilter = minAmountVal != null || maxAmountVal != null
    val hasSearchFilter = searchQuery.isNotBlank()
    val hasAccountFilter = selectedAccountIds.isNotEmpty()
    val hasTimeFilter = selectedTimeRange != ReportTimeRange.ALL

    // 规则 1：账户 / 分类 / 金额 / 关键词 任一被填 → 5 个图表全部隐藏,只剩 chip + 明细
    val hasCategoryAmountOrSearch = hasCategoryFilter || hasAmountFilter || hasSearchFilter

    // 时间跨度校验：消费习惯要求 ≥ 7 天，收支对比要求 ≥ 2 个自然月
    val (isTimeSpanAtLeast7Days, isTimeSpanAtLeast2Months) = remember(selectedTimeRange) {
        when (selectedTimeRange) {
            ReportTimeRange.ALL -> Pair(true, true)
            ReportTimeRange.THIS_MONTH -> Pair(true, false)
            ReportTimeRange.LAST_MONTH -> Pair(true, false)
            ReportTimeRange.LAST_7_DAYS -> Pair(true, false)
            ReportTimeRange.LAST_30_DAYS -> Pair(true, false)
            ReportTimeRange.LAST_6_MONTHS -> Pair(true, true)
            ReportTimeRange.THIS_YEAR -> Pair(true, true)
            ReportTimeRange.CUSTOM -> Pair(false, false)
        }
    }

    // 图表可见性（有时间筛选条件时隐藏消费习惯和收支对比两个图）
    val showTrendChart = !hasCategoryAmountOrSearch
    val showExpenseBreakdown = !hasCategoryAmountOrSearch && !hasAccountFilter
    val showIncomeBreakdown = !hasCategoryAmountOrSearch && !hasAccountFilter
    val showSpendingHabits = !hasCategoryAmountOrSearch && !hasAccountFilter && !hasTimeFilter && isTimeSpanAtLeast7Days
    val showMonthlyComparison = !hasCategoryAmountOrSearch && !hasAccountFilter && !hasTimeFilter && isTimeSpanAtLeast2Months

    // 1. 资产走势 / 账户余额走势数据计算
    val selectedAccountsBalance = remember(accounts, selectedAccountIds) {
        if (selectedAccountIds.isNotEmpty()) {
            accounts.filter { it.id in selectedAccountIds }.sumOf { it.balance }
        } else {
            totalNetAssets
        }
    }

    val selectedAccountExpenses = remember(expenses, selectedAccountIds) {
        if (selectedAccountIds.isNotEmpty()) {
            expenses.filter { it.accountId in selectedAccountIds }
        } else {
            expenses
        }
    }

    val trendPoints = remember(selectedAccountExpenses, selectedAccountsBalance, hasAccountFilter) {
        calculateAssetTrendHistory(selectedAccountExpenses, selectedAccountsBalance)
    }

    val trendTitle = remember(selectedAccountIds, accounts, hasAccountFilter) {
        if (hasAccountFilter) {
            if (selectedAccountIds.size == 1) {
                val accName = accounts.firstOrNull { it.id in selectedAccountIds }?.name ?: "账户"
                "BALANCE TREND / $accName·余额走势"
            } else {
                "BALANCE TREND / 已选账户·余额走势"
            }
        } else {
            "ASSET TREND / 资产走势"
        }
    }

    // 本月变化额计算
    val monthlyChangeInfo = remember(trendPoints) {
        if (trendPoints.size >= 2) {
            val current = trendPoints.last().assetValue
            val prev = trendPoints[trendPoints.size - 2].assetValue
            val diff = current - prev
            val percent = if (prev != 0.0) (diff / kotlin.math.abs(prev)) * 100 else 0.0
            Pair(diff, percent)
        } else {
            Pair(0.0, 0.0)
        }
    }

    // 2. 支出与收入分类构成
    val expenseSlices = remember(filteredExpenses) {
        calculateCategorySlices(filteredExpenses, "EXPENSE", isExpense = true)
    }
    val incomeSlices = remember(filteredExpenses) {
        calculateCategorySlices(filteredExpenses, "INCOME", isExpense = false)
    }
    val currentPeriodExpenseTotal = remember(expenseSlices) { expenseSlices.sumOf { it.amount } }
    val currentPeriodIncomeTotal = remember(incomeSlices) { incomeSlices.sumOf { it.amount } }

    // 3. 消费习惯：星期分布柱状数据（周一至周日）
    val weekdaySpending = remember(filteredExpenses) {
        calculateWeekdaySpending(filteredExpenses)
    }

    // 4. 收支对比：近 6 个月双柱数据与环比结论
    val (monthlyCompareList, expenseChangeRatio) = remember(expenses) {
        calculateMonthlyComparison(expenses)
    }

    // 专用饼图分类筛选逻辑 (独立于通用条件筛选)
    val pieFilteredExpenses = remember(
        filteredExpenses,
        selectedExpensePieCategory,
        selectedIncomePieCategory,
        expenseSlices,
        incomeSlices
    ) {
        when {
            selectedExpensePieCategory != null -> {
                if (selectedExpensePieCategory == "其他") {
                    val explicitCategories = expenseSlices.map { it.name }.filter { it != "其他" }.toSet()
                    filteredExpenses.filter { it.type == "EXPENSE" && it.displayCategory.ifBlank { "其他" }.let { cat -> cat == "其他" || cat == "其它" || cat !in explicitCategories } }
                } else {
                    filteredExpenses.filter { it.type == "EXPENSE" && it.displayCategory.ifBlank { "其他" } == selectedExpensePieCategory }
                }
            }
            selectedIncomePieCategory != null -> {
                if (selectedIncomePieCategory == "其他") {
                    val explicitCategories = incomeSlices.map { it.name }.filter { it != "其他" }.toSet()
                    filteredExpenses.filter { it.type == "INCOME" && it.displayCategory.ifBlank { "其他" }.let { cat -> cat == "其他" || cat == "其它" || cat !in explicitCategories } }
                } else {
                    filteredExpenses.filter { it.type == "INCOME" && it.displayCategory.ifBlank { "其他" } == selectedIncomePieCategory }
                }
            }
            else -> filteredExpenses
        }
    }

    // ----------------- 明细数据分页与预加载逻辑 -----------------
    val sortedExpenses = remember(pieFilteredExpenses) {
        pieFilteredExpenses.sortedByDescending { it.dateTimestamp }
    }
    var displayedLimit by remember(pieFilteredExpenses) { mutableStateOf(10) }
    val pagedExpenses = remember(sortedExpenses, displayedLimit) {
        sortedExpenses.take(displayedLimit)
    }

    // 自动滚动到顶部：当饼图筛选激活时，让饼图顶部对齐屏幕上方
    LaunchedEffect(selectedExpensePieCategory, selectedIncomePieCategory) {
        if (selectedExpensePieCategory != null || selectedIncomePieCategory != null) {
            listState.animateScrollToItem(0)
        }
    }

    // 距离底部还有空间时即触发无感预加载 (提前 3-4 项触发)
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalCount = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalCount > 0 && lastVisibleIndex >= totalCount - 4
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && displayedLimit < sortedExpenses.size) {
            displayedLimit = (displayedLimit + 10).coerceAtMost(sortedExpenses.size)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ==================== 1. 顶部固定区域 (Header + Active Filter Row + Divider) ====================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(canvasBg)
            ) {
                // Analysis 标头栏 (右上角只保留搜索按钮，移至筛选按钮位置)
                ReportEditorialHeader(
                    onSearchClick = { onShowSearchRowChange(!showSearchRow) },
                    textMain = textMain,
                    textMuted = textMuted
                )

                // 搜索输入框（展开态）
                if (showSearchRow) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text(
                                    "搜索分类、备注或账户...",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = textMuted
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = forestGreen,
                                unfocusedBorderColor = dividerColor,
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {
                            onSearchQueryChange("")
                            onShowSearchRowChange(false)
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭搜索", tint = textMuted)
                        }
                    }
                }

                // 当前生效筛选条件提示文字标签（账户 · 分类 · 金额 · 时间）
                val timeRangeDisplayLabel = remember(selectedTimeRange, customStartDate, customEndDate) {
                    if (selectedTimeRange == ReportTimeRange.CUSTOM) {
                        val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                        "${sdf.format(Date(customStartDate))}~${sdf.format(Date(customEndDate))}"
                    } else {
                        selectedTimeRange.label
                    }
                }

                ReportActiveFilterRow(
                    selectedAccountCount = selectedAccountIds.size,
                    selectedCategoryCount = selectedCategoryNames.size,
                    minAmount = minAmountVal,
                    maxAmount = maxAmountVal,
                    timeRangeText = timeRangeDisplayLabel,
                    onClick = { showFilterSheet = true }
                )

                // 固定边界横线
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            }

            // ==================== 2. 下方平滑滚动内容区域 ====================
            val isExpenseFilterActive = selectedExpensePieCategory != null
            val isIncomeFilterActive = selectedIncomePieCategory != null

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isExpenseFilterActive) {
                    item(key = "chart_expense_breakdown") {
                        Column(modifier = Modifier.animateItem(placementSpec = spring(stiffness = Spring.StiffnessMediumLow))) {
                            ExpenseBreakdownSection(
                                slices = expenseSlices,
                                totalExpense = currentPeriodExpenseTotal,
                                selectedCategory = selectedExpensePieCategory,
                                onCategorySelected = { cat ->
                                    onSelectedExpensePieCategoryChange(cat)
                                    if (cat != null) onSelectedIncomePieCategoryChange(null)
                                },
                                textMain = textMain,
                                textMuted = textMuted
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                        }
                    }
                } else if (isIncomeFilterActive) {
                    item(key = "chart_income_breakdown") {
                        Column(modifier = Modifier.animateItem(placementSpec = spring(stiffness = Spring.StiffnessMediumLow))) {
                            IncomeBreakdownSection(
                                slices = incomeSlices,
                                totalIncome = currentPeriodIncomeTotal,
                                selectedCategory = selectedIncomePieCategory,
                                onCategorySelected = { cat ->
                                    onSelectedIncomePieCategoryChange(cat)
                                    if (cat != null) onSelectedExpensePieCategoryChange(null)
                                },
                                textMain = textMain,
                                textMuted = textMuted
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                        }
                    }
                } else {
                    // 1. 资产/余额走势模块 (平滑动效)
                    item(key = "chart_trend") {
                        AnimatedVisibility(
                            visible = showTrendChart,
                            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
                            modifier = Modifier.animateItem(placementSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        ) {
                            Column {
                                AssetTrendSection(
                                    totalNetAssets = selectedAccountsBalance,
                                    monthlyChangeDiff = monthlyChangeInfo.first,
                                    monthlyChangePercent = monthlyChangeInfo.second,
                                    trendPoints = trendPoints,
                                    title = trendTitle,
                                    textMain = textMain,
                                    textMuted = textMuted,
                                    canvasBg = canvasBg
                                )
                                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                            }
                        }
                    }

                    // 2. 支出构成模块 (平滑动效)
                    item(key = "chart_expense_breakdown") {
                        AnimatedVisibility(
                            visible = showExpenseBreakdown,
                            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
                            modifier = Modifier.animateItem(placementSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        ) {
                            Column {
                                ExpenseBreakdownSection(
                                    slices = expenseSlices,
                                    totalExpense = currentPeriodExpenseTotal,
                                    selectedCategory = selectedExpensePieCategory,
                                    onCategorySelected = { cat ->
                                        onSelectedExpensePieCategoryChange(cat)
                                        if (cat != null) onSelectedIncomePieCategoryChange(null)
                                    },
                                    textMain = textMain,
                                    textMuted = textMuted
                                )
                                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                            }
                        }
                    }

                    // 3. 收入构成模块 (平滑动效)
                    item(key = "chart_income_breakdown") {
                        AnimatedVisibility(
                            visible = showIncomeBreakdown,
                            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
                            modifier = Modifier.animateItem(placementSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        ) {
                            Column {
                                IncomeBreakdownSection(
                                    slices = incomeSlices,
                                    totalIncome = currentPeriodIncomeTotal,
                                    selectedCategory = selectedIncomePieCategory,
                                    onCategorySelected = { cat ->
                                        onSelectedIncomePieCategoryChange(cat)
                                        if (cat != null) onSelectedExpensePieCategoryChange(null)
                                    },
                                    textMain = textMain,
                                    textMuted = textMuted
                                )
                                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                            }
                        }
                    }

                    // 4. 消费习惯模块 (星期分布 - 平滑动效)
                    item(key = "chart_spending_habits") {
                        AnimatedVisibility(
                            visible = showSpendingHabits,
                            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
                            modifier = Modifier.animateItem(placementSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        ) {
                            Column {
                                SpendingHabitsSection(weekdayItems = weekdaySpending)
                                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                            }
                        }
                    }

                    // 5. 收支对比模块 (近 6 个月双柱 - 平滑动效)
                    item(key = "chart_monthly_comparison") {
                        AnimatedVisibility(
                            visible = showMonthlyComparison,
                            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
                            modifier = Modifier.animateItem(placementSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        ) {
                            Column {
                                MonthlyComparisonSection(
                                    compareItems = monthlyCompareList,
                                    expenseChangeRatio = expenseChangeRatio
                                )
                                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                            }
                        }
                    }
                }

                // 6. 流水明细标题栏
                item(key = "report_details_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRANSACTIONS / 流水明细",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = textMuted
                        )
                    }
                }

                // 8. 流水明细列表项（按天聚合）
                if (pagedExpenses.isEmpty()) {
                    item(key = "report_empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无符合条件的交易记录",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = textMuted
                            )
                        }
                    }
                } else {
                    val grouped = pagedExpenses.groupBy {
                        SimpleDateFormat("yyyy.MM.dd · EEEE", Locale.CHINESE).format(Date(it.dateTimestamp))
                    }
                    grouped.forEach { (dateHeader, itemsInDay) ->
                        item(key = "date_header_$dateHeader") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dateHeader,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textMuted,
                                    letterSpacing = 0.8.sp
                                )
                                val daySum = itemsInDay.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                                if (daySum > 0) {
                                    Text(
                                        text = "支出小计 ¥${AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(daySum))}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.5.sp,
                                        color = textMuted
                                    )
                                }
                            }
                        }

                        items(itemsInDay, key = { "report_item_${it.id}" }) { expense ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 22.dp)
                            ) {
                                JournalTransactionRow(
                                    expense = expense,
                                    canvasBg = canvasBg,
                                    dividerColor = dividerColor,
                                    inkPrimary = textMain,
                                    inkSecondary = textSecondary,
                                    inkMuted = textMuted,
                                    clayAccent = clayAccent,
                                    forestGreen = forestGreen,
                                    onItemClick = { onEditExpense?.invoke(expense) },
                                    onDeleteExpense = { onDeleteExpense?.invoke(expense) }
                                )
                            }
                        }
                    }
                }

                // 9. 底部加载状态 / 已加载全部提示
                if (pagedExpenses.isNotEmpty()) {
                    item(key = "report_pagination_footer") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (displayedLimit < sortedExpenses.size) {
                                Text(
                                    text = "正在载入更多明细...",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted
                                )
                            } else {
                                Text(
                                    text = "· 已加载全部 ${sortedExpenses.size} 笔明细 ·",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                // 底部留白
                item(key = "report_bottom_spacer") {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }

        // ==================== 筛选 ModalBottomSheet ====================
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = sheetState,
                containerColor = WarmPaperBg,
                dragHandle = null
            ) {
                ReportFilterBottomSheetContent(
                    accounts = accounts,
                    allCategories = expenses.map { it.displayCategory }.filter { it.isNotBlank() }.distinct(),
                    selectedAccounts = selectedAccountIds,
                    selectedCategories = selectedCategoryNames,
                    selectedTimeRange = selectedTimeRange,
                    customStartDate = customStartDate,
                    customEndDate = customEndDate,
                    minAmount = minAmountInput,
                    maxAmount = maxAmountInput,
                    filteredCount = filteredExpenses.size,
                    onSelectTimeRange = { onSelectedTimeRangeChange(it) },
                    onCustomStartDateChange = { onCustomStartDateChange(it) },
                    onCustomEndDateChange = { onCustomEndDateChange(it) },
                    onMinAmountChange = { onMinAmountInputChange(it) },
                    onMaxAmountChange = { onMaxAmountInputChange(it) },
                    onToggleAccount = { id ->
                        val newAccounts = if (id in selectedAccountIds) selectedAccountIds - id else selectedAccountIds + id
                        onSelectedAccountIdsChange(newAccounts)
                    },
                    onToggleCategory = { cat ->
                        val newCats = if (cat in selectedCategoryNames) selectedCategoryNames - cat else selectedCategoryNames + cat
                        onSelectedCategoryNamesChange(newCats)
                    },
                    onReset = {
                        onSelectedTimeRangeChange(ReportTimeRange.ALL)
                        onSelectedAccountIdsChange(emptySet())
                        onSelectedCategoryNamesChange(emptySet())
                        onMinAmountInputChange("")
                        onMaxAmountInputChange("")
                    },
                    onApply = {
                        coroutineScope.launch {
                            sheetState.hide()
                            showFilterSheet = false
                        }
                    }
                )
            }
        }
    }
}

/**
 * 顶部 Analysis 杂志标头（与首页/账户页最上方风格对齐）
 */
@Composable
fun ReportEditorialHeader(
    onSearchClick: () -> Unit,
    textMain: Color,
    textMuted: Color,
    modifier: Modifier = Modifier
) {
    EditorialPageHeader(
        title = "Analysis",
        subtitle = "FINANCIAL REPORT",
        modifier = modifier
    ) {
        // 右上角只保留搜索按钮 (原筛选按钮位置)
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索明细",
                tint = textMain,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * 筛选栏下方一行生效标签（纯文字 + 小箭头，无背景色块）
 */
@Composable
fun ReportActiveFilterRow(
    selectedAccountCount: Int,
    selectedCategoryCount: Int,
    minAmount: Double?,
    maxAmount: Double?,
    timeRangeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accountText = if (selectedAccountCount == 0) "全部账户" else "已选${selectedAccountCount}个账户"
    val categoryText = if (selectedCategoryCount == 0) "全部分类" else "已选${selectedCategoryCount}个分类"
    val amountText = when {
        minAmount != null && maxAmount != null -> "¥%.0f-¥%.0f".format(minAmount, maxAmount)
        minAmount != null -> "≥¥%.0f".format(minAmount)
        maxAmount != null -> "≤¥%.0f".format(maxAmount)
        else -> "全部金额"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(start = 22.dp, end = 22.dp, top = 2.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$accountText · $categoryText · $amountText · $timeRangeText",
            fontSize = 11.5.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "展开筛选",
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * 模块 1：资产/余额走势 Composable
 */
@Composable
fun AssetTrendSection(
    totalNetAssets: Double,
    monthlyChangeDiff: Double,
    monthlyChangePercent: Double,
    trendPoints: List<MonthAssetPoint>,
    title: String = "ASSET TREND / 资产走势",
    textMain: Color = TextMain,
    textMuted: Color = TextMuted,
    canvasBg: Color = WarmPaperBg,
    modifier: Modifier = Modifier
) {
    val animatedAmount = remember { Animatable(0f) }
    LaunchedEffect(totalNetAssets) {
        animatedAmount.snapTo(0f)
        animatedAmount.animateTo(
            targetValue = totalNetAssets.toFloat(),
            animationSpec = tween(durationMillis = 1425, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        // 区块大写等宽标签
        Text(
            text = title,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = textMuted,
            modifier = Modifier.padding(horizontal = 22.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 当前总资产/账户余额大字号等宽数字 + 资产下方本月变化指标
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "¥",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    color = textMuted,
                    modifier = Modifier.padding(bottom = 3.dp, end = 4.dp)
                )
                Text(
                    text = AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(animatedAmount.value.toDouble())),
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.8).sp,
                    color = textMain
                )
            }

            // 本月变化指标（移至资产下方，正数墨绿+向上，负数陶红+向下）
            if (monthlyChangeDiff != 0.0) {
                val isPositive = monthlyChangeDiff >= 0
                val changeColor = if (isPositive) ForestGreen else ClayAccent
                val sign = if (isPositive) "+" else "-"
                val diffFormatted = AmountFormatter.formatCentsAsYuan(
                    AmountFormatter.yuanToCents(kotlin.math.abs(monthlyChangeDiff))
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = changeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "本月 $sign¥$diffFormatted (${String.format(Locale.getDefault(), "%.1f", monthlyChangePercent)}%)",
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = changeColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 可横向滑动 Catmull-Rom 贝塞尔平滑曲线（仅视口区域执行平滑 reveal 动画）
        CurveTrendChart(
            points = trendPoints,
            primaryColor = ForestGreen,
            canvasBgColor = canvasBg,
            textColorMuted = textMuted
        )
    }
}

/**
 * 筛选结果摘要卡片 (SUMMARY)
 */
@Composable
fun ReportSummarySection(dividerColor: Color = LocalAppBackgroundConfig.current.dividerColor, 
    filteredExpensesCount: Int,
    totalExpense: Double,
    totalIncome: Double,
    modifier: Modifier = Modifier
) {
    val netBalance = totalIncome - totalExpense
    val isNetPositive = netBalance >= 0
    val netSign = if (isNetPositive) "+" else "-"
    val netColor = if (isNetPositive) ClayAccent else TextMain

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SUMMARY / 筛选摘要",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )
            Text(
                text = "共 $filteredExpensesCount 笔",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = ForestGreen,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.6f))
                .border(0.6.dp, dividerColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 筛选支出
            Column {
                Text(
                    text = "筛选支出",
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "¥${AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(totalExpense))}",
                    fontSize = 14.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen
                )
            }

            // 筛选收入
            Column {
                Text(
                    text = "筛选收入",
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "¥${AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(totalIncome))}",
                    fontSize = 14.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ClayAccent
                )
            }

            // 净结余
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "收支结余",
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "$netSign¥${AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(kotlin.math.abs(netBalance)))}",
                    fontSize = 14.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = netColor
                )
            }
        }
    }
}

/**
 * 手账报刊式流水单行 Composable (带侧滑删除与点击编辑支持)
 */
@Composable
private fun JournalTransactionRow(
    expense: ExpenseEntity,
    canvasBg: Color,
    dividerColor: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    inkMuted: Color,
    clayAccent: Color,
    forestGreen: Color,
    onItemClick: () -> Unit,
    onDeleteExpense: () -> Unit
) {
    val isExpense = expense.type == "EXPENSE"
    val isIncome = expense.type == "INCOME"
    val isTransfer = expense.type == "TRANSFER"
    val amountCents = AmountFormatter.yuanToCents(expense.amount)
    val amountFormatted = AmountFormatter.formatCentsAsYuan(abs(amountCents))
    val timeFormatted = remember(expense.dateTimestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(expense.dateTimestamp))
    }

    var offsetX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val deleteBtnWidthDp = 60.dp
    val gapDp = 12.dp
    val maxSwipePx = with(density) { (deleteBtnWidthDp + gapDp).toPx() }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "report_row_swipe_offset"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        ) {
            // 删除按钮：左滑时实时展示
            if (animatedOffsetX < -0.5f) {
                val revealWidthDp = remember(animatedOffsetX) {
                    val swipedDp = with(density) { (-animatedOffsetX).toDp() }
                    (swipedDp - gapDp).coerceAtLeast(0.dp).coerceAtMost(deleteBtnWidthDp)
                }

                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(revealWidthDp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE53935))
                            .clickable {
                                offsetX = 0f
                                onDeleteExpense()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (revealWidthDp >= 35.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "删除",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "删除",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 明细内容主体
            Box(
                modifier = Modifier
                    .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                    .fillMaxWidth()
                    .pointerInput(expense.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX < -maxSwipePx * 0.4f) {
                                    offsetX = -maxSwipePx
                                } else {
                                    offsetX = 0f
                                }
                            },
                            onDragCancel = { offsetX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val newOffset = (offsetX + dragAmount).coerceIn(-maxSwipePx, 0f)
                                offsetX = newOffset
                            }
                        )
                    }
                    .clickable {
                        if (offsetX < -10f) {
                            offsetX = 0f
                        } else {
                            onItemClick()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isExpense) forestGreen else if (isIncome) clayAccent else Color(0xFF4361EE))
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (isTransfer) "转账" else expense.displaySubCategory.ifEmpty { expense.displayCategory },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = inkPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = timeFormatted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = inkMuted
                                )
                                if (expense.note.isNotBlank()) {
                                    Text(
                                        text = " · ${expense.note}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = inkMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${if (isExpense) "-" else if (isIncome) "+" else ""}¥$amountFormatted",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isExpense) inkPrimary else if (isIncome) clayAccent else Color(0xFF4361EE)
                        )
                        val accountDisplay = if (isTransfer && expense.transferToAccountName.isNotBlank()) {
                            "${expense.accountName} ➔ ${expense.transferToAccountName}"
                        } else {
                            expense.accountName
                        }
                        if (accountDisplay.isNotBlank()) {
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = accountDisplay,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = inkMuted
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = dividerColor.copy(alpha = 0.6f))
    }
}

/**
 * 模块 2：支出构成 Composable (对换为墨绿色调为主)
 */
@Composable
fun ExpenseBreakdownSection(
    slices: List<DonutSliceData>,
    totalExpense: Double,
    selectedCategory: String? = null,
    onCategorySelected: (String?) -> Unit = {},
    textMain: Color = TextMain,
    textMuted: Color = TextMuted,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 24.dp)
    ) {
        Text(
            text = "EXPENSE BREAKDOWN / 支出构成",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = textMuted
        )

        Spacer(modifier = Modifier.height(18.dp))

        AnimatedDonutChart(
            slices = slices,
            centerLabel = "总支出",
            totalAmount = totalExpense,
            gapAngle = 0f, // 取消扇区留白
            primaryInkColor = textMain,
            mutedTextColor = textMuted,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )
    }
}

/**
 * 模块 3：收入构成 Composable (对换为陶红色调为主)
 */
@Composable
fun IncomeBreakdownSection(
    slices: List<DonutSliceData>,
    totalIncome: Double,
    selectedCategory: String? = null,
    onCategorySelected: (String?) -> Unit = {},
    textMain: Color = TextMain,
    textMuted: Color = TextMuted,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 24.dp)
    ) {
        Text(
            text = "INCOME BREAKDOWN / 收入构成",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = textMuted
        )

        Spacer(modifier = Modifier.height(18.dp))

        AnimatedDonutChart(
            slices = slices,
            centerLabel = "总收入",
            totalAmount = totalIncome,
            gapAngle = 0f, // 取消扇区留白
            primaryInkColor = textMain,
            mutedTextColor = textMuted,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )
    }
}

/**
 * 模块 4：消费习惯 (星期分布柱状图)
 */
@Composable
fun SpendingHabitsSection(dividerColor: Color = LocalAppBackgroundConfig.current.dividerColor, 
    weekdayItems: List<WeekdaySpendingItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 24.dp)
    ) {
        Text(
            text = "SPENDING HABITS / 消费习惯",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(18.dp))

        WeekdaySpendingBarChart(
            items = weekdayItems,
            forestGreen = ForestGreen,
            clayAccent = ClayAccent,
            mutedTextColor = TextMuted,
            gridLineColor = dividerColor
        )
    }
}

/**
 * 模块 5：收支对比 (近 6 个月双柱图：收入陶红、支出墨绿)
 */
@Composable
fun MonthlyComparisonSection(dividerColor: Color = LocalAppBackgroundConfig.current.dividerColor, 
    compareItems: List<MonthlyCompareItem>,
    expenseChangeRatio: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MONTHLY COMPARISON / 收支对比",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )

            // 结论性徽标（如：支出较上月 +12% 陶红，-8% 墨绿）
            val isIncrease = expenseChangeRatio > 0
            val badgeColor = if (isIncrease) ClayAccent else ForestGreen
            val badgeSign = if (isIncrease) "+" else ""

            Text(
                text = "支出较上月 $badgeSign${String.format(Locale.getDefault(), "%.1f", expenseChangeRatio)}%",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        MonthlyIncomeExpenseBarChart(
            items = compareItems,
            incomeColor = ClayAccent,
            expenseColor = ForestGreen,
            mutedTextColor = TextMuted
        )
    }
}

/**
 * 筛选抽屉弹窗内容（极简手账美学与舒适网格排版）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportFilterBottomSheetContent(dividerColor: Color = LocalAppBackgroundConfig.current.dividerColor, 
    accounts: List<AccountEntity>,
    allCategories: List<String>,
    selectedAccounts: Set<Long>,
    selectedCategories: Set<String>,
    selectedTimeRange: ReportTimeRange,
    customStartDate: Long = System.currentTimeMillis(),
    customEndDate: Long = System.currentTimeMillis(),
    minAmount: String,
    maxAmount: String,
    filteredCount: Int = 0,
    onSelectTimeRange: (ReportTimeRange) -> Unit,
    onCustomStartDateChange: (Long) -> Unit = {},
    onCustomEndDateChange: (Long) -> Unit = {},
    onMinAmountChange: (String) -> Unit,
    onMaxAmountChange: (String) -> Unit,
    onToggleAccount: (Long) -> Unit,
    onToggleCategory: (String) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // 顶部居中把手 (Drag Handle)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD5D0C5))
            )
        }

        // 头部标题与重置按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FILTER CRITERIA",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Text(
                    text = "账单明细高级筛选",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Default,
                    color = TextMuted
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onReset() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "重置",
                    tint = ForestGreen,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "清空重置",
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForestGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

        // 可滑动的筛选选项区域（包含账户、分类、金额、时间四个条件）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // 1. 账户筛选
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "01 / ACCOUNTS (关联账户)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
                if (selectedAccounts.isNotEmpty()) {
                    Text(
                        text = "已选 ${selectedAccounts.size} 个",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForestGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 全部账户快捷标签
                val isAllAccounts = selectedAccounts.isEmpty()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isAllAccounts) ForestGreen.copy(alpha = 0.12f) else WarmPaperBg)
                        .border(
                            width = if (isAllAccounts) 1.dp else 0.5.dp,
                            color = if (isAllAccounts) ForestGreen else dividerColor,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            selectedAccounts.forEach { onToggleAccount(it) }
                        }
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "全部账户",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isAllAccounts) ForestGreen else TextMain,
                        fontWeight = if (isAllAccounts) FontWeight.Bold else FontWeight.Normal
                    )
                }

                accounts.forEach { acc ->
                    val isSelected = acc.id in selectedAccounts
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) ForestGreen.copy(alpha = 0.12f) else WarmPaperBg)
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) ForestGreen else dividerColor,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onToggleAccount(acc.id) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已选",
                                tint = ForestGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = acc.name,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) ForestGreen else TextMain,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 2. 分类筛选
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "02 / CATEGORIES (交易分类)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
                if (selectedCategories.isNotEmpty()) {
                    Text(
                        text = "已选 ${selectedCategories.size} 类",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ClayAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 全部分类快捷标签
                val isAllCats = selectedCategories.isEmpty()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isAllCats) ClayAccent.copy(alpha = 0.12f) else WarmPaperBg)
                        .border(
                            width = if (isAllCats) 1.dp else 0.5.dp,
                            color = if (isAllCats) ClayAccent else dividerColor,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            selectedCategories.forEach { onToggleCategory(it) }
                        }
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "全部分类",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isAllCats) ClayAccent else TextMain,
                        fontWeight = if (isAllCats) FontWeight.Bold else FontWeight.Normal
                    )
                }

                allCategories.forEach { cat ->
                    val isSelected = cat in selectedCategories
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) ClayAccent.copy(alpha = 0.12f) else WarmPaperBg)
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) ClayAccent else dividerColor,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onToggleCategory(cat) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已选",
                                tint = ClayAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) ClayAccent else TextMain,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 3. 金额区间输入
            Text(
                text = "03 / AMOUNT RANGE (金额区间)",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 自定义金额输入框 (使用 BasicTextField 保证文字垂直居中且绝对不被截断)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 最小金额
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.65f))
                        .border(0.8.dp, if (minAmount.isNotEmpty()) ForestGreen else dividerColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (minAmount.isEmpty()) {
                            Text(
                                text = "¥ 最小金额",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }
                        BasicTextField(
                            value = minAmount,
                            onValueChange = onMinAmountChange,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(
                                fontSize = 12.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = TextMain
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (minAmount.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除最小金额",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { onMinAmountChange("") }
                        )
                    }
                }

                Text(
                    text = "—",
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                // 最大金额
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.65f))
                        .border(0.8.dp, if (maxAmount.isNotEmpty()) ForestGreen else dividerColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (maxAmount.isEmpty()) {
                            Text(
                                text = "¥ 最大金额",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }
                        BasicTextField(
                            value = maxAmount,
                            onValueChange = onMaxAmountChange,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(
                                fontSize = 12.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = TextMain
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (maxAmount.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除最大金额",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { onMaxAmountChange("") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 4. 时间范围筛选
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "04 / TIME RANGE (时间范围)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
                if (selectedTimeRange != ReportTimeRange.ALL) {
                    Text(
                        text = selectedTimeRange.label,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForestGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportTimeRange.values().forEach { range ->
                    val isSelected = selectedTimeRange == range
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) ForestGreen.copy(alpha = 0.12f) else WarmPaperBg)
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) ForestGreen else dividerColor,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelectTimeRange(range) }
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已选",
                                tint = ForestGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = range.label,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) ForestGreen else TextMain,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // 自定义时间范围面板
            if (selectedTimeRange == ReportTimeRange.CUSTOM) {
                Spacer(modifier = Modifier.height(12.dp))
                val context = LocalContext.current
                val startCal = remember(customStartDate) {
                    Calendar.getInstance().apply { timeInMillis = customStartDate }
                }
                val endCal = remember(customEndDate) {
                    Calendar.getInstance().apply { timeInMillis = customEndDate }
                }
                val sdf = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.75f))
                        .border(0.8.dp, ForestGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CUSTOM RANGE / 自定义起止时间",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForestGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "点击可修改日期",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 起始日期
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(WarmPaperBg)
                                .border(0.6.dp, dividerColor, RoundedCornerShape(6.dp))
                                .clickable {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val c = Calendar.getInstance().apply {
                                                set(y, m, d, 0, 0, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            onCustomStartDateChange(c.timeInMillis)
                                        },
                                        startCal.get(Calendar.YEAR),
                                        startCal.get(Calendar.MONTH),
                                        startCal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "START / 起始",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = ForestGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = sdf.format(Date(customStartDate)),
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMain
                                )
                            }
                        }

                        Text(
                            text = "至",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )

                        // 截止日期
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(WarmPaperBg)
                                .border(0.6.dp, dividerColor, RoundedCornerShape(6.dp))
                                .clickable {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val c = Calendar.getInstance().apply {
                                                set(y, m, d, 23, 59, 59)
                                                set(Calendar.MILLISECOND, 999)
                                            }
                                            onCustomEndDateChange(c.timeInMillis)
                                        },
                                        endCal.get(Calendar.YEAR),
                                        endCal.get(Calendar.MONTH),
                                        endCal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "END / 截止",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = ForestGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = sdf.format(Date(customEndDate)),
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMain
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 常用快捷跨度
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickSpans = listOf(
                            "近3天" to 3,
                            "近14天" to 14,
                            "近60天" to 60,
                            "近90天" to 90
                        )
                        quickSpans.forEach { (name, days) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(WarmPaperBg)
                                    .border(0.5.dp, dividerColor, RoundedCornerShape(4.dp))
                                    .clickable {
                                        val nowCal = Calendar.getInstance()
                                        val endMs = nowCal.apply {
                                            set(Calendar.HOUR_OF_DAY, 23)
                                            set(Calendar.MINUTE, 59)
                                            set(Calendar.SECOND, 59)
                                            set(Calendar.MILLISECOND, 999)
                                        }.timeInMillis
                                        val startMs = Calendar.getInstance().apply {
                                            add(Calendar.DAY_OF_YEAR, -(days - 1))
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                        onCustomStartDateChange(startMs)
                                        onCustomEndDateChange(endMs)
                                    }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMain
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 底部操作按钮栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 重置按钮
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(0.8.dp, dividerColor, RoundedCornerShape(8.dp))
                        .clickable { onReset() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "重置全部",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMain
                    )
                }

                // 确认应用按钮
                val applyButtonText = if (filteredCount > 0) {
                    "确认应用 ($filteredCount 笔明细)"
                } else {
                    "确认应用"
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ForestGreen)
                        .clickable { onApply() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = applyButtonText,
                        fontSize = 12.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ==================== 数据分析与辅助计算工具函数 ====================

/**
 * 严格按照实际数据计算账本有记录以来的所有月份资产净值曲线
 * 最新月份（当前月）在最右端实时变动，往期月份按流水收支严格反推
 */
private fun calculateAssetTrendHistory(
    expenses: List<ExpenseEntity>,
    currentNetAssets: Double
): List<MonthAssetPoint> {
    val sdf = SimpleDateFormat("yy.MM", Locale.getDefault())
    val cal = Calendar.getInstance()

    // 1. 当前月份（最新月份点）
    val currentYear = cal.get(Calendar.YEAR)
    val currentMonth = cal.get(Calendar.MONTH)

    // 2. 找到所有流水发生的最早月份
    val minTimestamp = expenses.minOfOrNull { it.dateTimestamp } ?: System.currentTimeMillis()
    val startCal = Calendar.getInstance().apply {
        timeInMillis = minTimestamp
        set(Calendar.DAY_OF_MONTH, 1)
    }

    // 确保至少有 6 个月时间跨度
    val tempCal = startCal.clone() as Calendar
    val totalMonthSpan = (currentYear - tempCal.get(Calendar.YEAR)) * 12 + (currentMonth - tempCal.get(Calendar.MONTH))
    if (totalMonthSpan < 5) {
        tempCal.timeInMillis = System.currentTimeMillis()
        tempCal.add(Calendar.MONTH, -5)
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
    }

    // 3. 构建月份序列 [StartMonth ... CurrentMonth]
    val monthList = mutableListOf<Calendar>()
    val iterCal = tempCal.clone() as Calendar
    while (true) {
        val y = iterCal.get(Calendar.YEAR)
        val m = iterCal.get(Calendar.MONTH)
        monthList.add(iterCal.clone() as Calendar)
        if (y == currentYear && m == currentMonth) {
            break
        }
        iterCal.add(Calendar.MONTH, 1)
    }

    // 4. 按月份汇总每月净收支现金流 NetFlow = Income - Expense
    val monthlyNetFlowMap = mutableMapOf<String, Double>()
    expenses.forEach { exp ->
        val expCal = Calendar.getInstance().apply { timeInMillis = exp.dateTimestamp }
        val key = sdf.format(expCal.time)
        val currentFlow = monthlyNetFlowMap[key] ?: 0.0
        val change = when (exp.type) {
            "INCOME" -> exp.amount
            "EXPENSE" -> -exp.amount
            else -> 0.0
        }
        monthlyNetFlowMap[key] = currentFlow + change
    }

    // 5. 从最新月份（资产 = currentNetAssets）严格倒推历史各月资产净值
    val resultPoints = mutableListOf<MonthAssetPoint>()
    var runningAsset = currentNetAssets

    for (i in monthList.indices.reversed()) {
        val c = monthList[i]
        val key = sdf.format(c.time)

        if (i == monthList.lastIndex) {
            // 当月期末资产严格等于当前总净资产
            resultPoints.add(0, MonthAssetPoint(key, currentNetAssets))
        } else {
            // 上一月期末资产 = 本月期末资产 - 本月净流入 (即 runningAsset - netFlow(nextMonth))
            val nextMonthKey = sdf.format(monthList[i + 1].time)
            val nextMonthNetFlow = monthlyNetFlowMap[nextMonthKey] ?: 0.0
            runningAsset -= nextMonthNetFlow
            resultPoints.add(0, MonthAssetPoint(key, runningAsset))
        }
    }

    return resultPoints
}

/**
 * 计算分类构成与占比（严格从占比大到占比小排序）
 * 确保所有分类金额及剩余“其他”完全覆盖 100% 总额，保证圆环绝对闭合，且视觉展示与图例严格降序
 */
private fun calculateCategorySlices(
    expenses: List<ExpenseEntity>,
    type: String,
    isExpense: Boolean
): List<DonutSliceData> {
    val items = expenses.filter { it.type == type }
    val total = items.sumOf { it.amount }

    // 主色调序列（对换）：支出用墨绿调，收入用陶红调，渐进展开
    val palette = if (isExpense) {
        listOf(
            ForestGreen,              // #2D6A4F 墨绿深
            Color(0xFF40916C),        // 墨绿中
            SoftSage,                 // #7FA893 浅墨绿
            Color(0xFF95D5B2),        // 柔青绿
            TaupeNeutral,             // #C9C0A8 灰褐
            Color(0xFFB7A99A),        // 暖褐
            SoftTerracotta,           // #D9A088 浅陶红
            ClayAccent                // #C4623D 陶红
        )
    } else {
        listOf(
            ClayAccent,               // #C4623D 陶红深
            Color(0xFFE07A5F),        // 陶红中
            SoftTerracotta,           // #D9A088 浅陶红
            Color(0xFFF2CC8F),        // 暖金黄
            TaupeNeutral,             // #C9C0A8 灰褐
            Color(0xFFB7A99A),        // 暖褐
            SoftSage,                 // #7FA893 浅墨绿
            ForestGreen               // #2D6A4F 墨绿
        )
    }

    if (total <= 0.0 || items.isEmpty()) {
        return emptyList()
    }

    val grouped = items.groupBy { it.displayCategory.ifBlank { "其他" } }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
        .filter { it.value > 0.0 }
        .toList()

    val otherEntries = grouped.filter { it.first == "其他" || it.first == "其它" }
    val regularEntries = grouped.filter { it.first != "other" && it.first != "其他" && it.first != "其它" }.sortedByDescending { it.second }

    val dbOtherAmount = otherEntries.sumOf { it.second }
    val maxRegularCount = if (dbOtherAmount > 0.0) 4 else 5
    val topRegular = regularEntries.take(maxRegularCount)
    val overflowAmount = regularEntries.drop(maxRegularCount).sumOf { it.second }

    val totalOtherAmount = dbOtherAmount + overflowAmount

    val finalGrouped = mutableListOf<Pair<String, Double>>()
    finalGrouped.addAll(topRegular)
    if (totalOtherAmount > 0.0) {
        finalGrouped.add("其他" to totalOtherAmount)
    }

    val sortedItems = finalGrouped.sortedByDescending { it.second }

    // 严格按占比从大到小生成扇区分片与对应渐进配色
    return sortedItems.mapIndexed { index, (catName, amount) ->
        val percentage = if (total > 0.0) ((amount / total) * 100).toFloat() else 0f
        DonutSliceData(
            id = "cat_$index",
            name = catName,
            amount = amount,
            percentage = percentage,
            color = palette[index % palette.size]
        )
    }
}

/**
 * 计算星期分布（周一至周日）
 */
private fun calculateWeekdaySpending(expenses: List<ExpenseEntity>): List<WeekdaySpendingItem> {
    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val dayAmounts = DoubleArray(7)

    val expenseOnly = expenses.filter { it.type == "EXPENSE" }
    val cal = Calendar.getInstance()

    expenseOnly.forEach { exp ->
        cal.timeInMillis = exp.dateTimestamp
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=周日, 2=周一...
        val mappedIndex = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        dayAmounts[mappedIndex] += exp.amount
    }

    return days.mapIndexed { index, name ->
        WeekdaySpendingItem(
            dayName = name,
            amount = dayAmounts[index],
            isWeekend = index >= 5 // 周六、周日
        )
    }
}

/**
 * 计算近 6 个月收支双柱数据
 */
private fun calculateMonthlyComparison(
    expenses: List<ExpenseEntity>
): Pair<List<MonthlyCompareItem>, Double> {
    val sdf = SimpleDateFormat("yy.MM", Locale.getDefault())
    val cal = Calendar.getInstance()

    val months = mutableListOf<MonthlyCompareItem>()
    val currentMonthSdf = sdf.format(Date())

    for (i in 5 downTo 0) {
        val monthCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -i)
        }
        val label = sdf.format(monthCal.time)
        val isCurrent = label == currentMonthSdf

        val monthExpenses = expenses.filter {
            val expCal = Calendar.getInstance().apply { timeInMillis = it.dateTimestamp }
            expCal.get(Calendar.YEAR) == monthCal.get(Calendar.YEAR) &&
                    expCal.get(Calendar.MONTH) == monthCal.get(Calendar.MONTH)
        }

        val inc = monthExpenses.filter { it.type == "INCOME" }.sumOf { it.amount }
        val exp = monthExpenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        months.add(
            MonthlyCompareItem(
                monthLabel = label,
                income = inc,
                expense = exp,
                isCurrentMonth = isCurrent
            )
        )
    }

    // 计算支出环比变化
    val currentExp = months.lastOrNull()?.expense ?: 0.0
    val prevExp = months.getOrNull(months.size - 2)?.expense ?: currentExp
    val changeRatio = if (prevExp > 0) ((currentExp - prevExp) / prevExp) * 100.0 else 0.0

    return Pair(months, changeRatio)
}
