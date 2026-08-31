package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EditorialPageHeader
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AccountEntity
import com.example.data.local.ExpenseEntity
import com.example.model.AmountFormatter
import com.example.ui.theme.LocalAppBackgroundConfig
import com.example.ui.viewmodel.BudgetConfig
import com.example.ui.viewmodel.BudgetProgressInfo
import com.example.ui.viewmodel.CategoryStat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * 预计算好的按天统计与数据，消除滑动过程中重复遍历和 Calendar 实例化的开销
 */
@Immutable
data class DayAggregate(
    val dayOfMonth: Int,
    val timeInMillis: Long,
    val isToday: Boolean,
    val incomeSum: Double,
    val expenseSum: Double,
    val hasRecords: Boolean
)

@Immutable
data class MonthCalendarData(
    val year: Int,
    val month: Int, // 0-based
    val totalExpense: Double,
    val totalIncome: Double,
    val firstDayOfWeek: Int, // 0-based: 0=Sun, 1=Mon...
    val maxDaysInMonth: Int,
    val dayAggregates: Map<Int, DayAggregate>
)

@Composable
fun EditorialPreviewScreen(
    expenses: List<ExpenseEntity>,
    accounts: List<AccountEntity>,
    allExpenses: List<ExpenseEntity>,
    thisMonthExpense: Double,
    thisMonthIncome: Double,
    totalExpense: Double,
    totalIncome: Double,
    todayExpense: Double,
    totalNetAssets: Double,
    totalPositiveAssets: Double,
    totalDebts: Double,
    categoryStats: List<CategoryStat>,
    budgetConfig: BudgetConfig,
    budgetProgress: BudgetProgressInfo,
    onOpenBudgetSettings: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit = {},
    selectedDateMillis: Long?,
    onSelectedDateChange: (Long?) -> Unit,
    isCalendarExpanded: Boolean,
    onCalendarExpandedChange: (Boolean) -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState,
    forceDarkPreview: Boolean?,
    onForceDarkPreviewChange: (Boolean?) -> Unit,
    playEntranceAnimation: Boolean,
    onEntranceAnimationPlayed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val globalBgConfig = LocalAppBackgroundConfig.current
    val isLight = forceDarkPreview?.let { !it } ?: globalBgConfig.isLight

    // 配色令牌（平滑主题过渡）
    val themeAnimSpec = tween<Color>(durationMillis = 400, easing = FastOutSlowInEasing)
    val canvasBg by animateColorAsState(if (isLight) Color(0xFFFAFAF7) else Color(0xFF242E24), animationSpec = themeAnimSpec, label = "canvasBg")
    val dividerColor by animateColorAsState(globalBgConfig.dividerColor, animationSpec = themeAnimSpec, label = "dividerColor")
    val inkPrimary by animateColorAsState(if (isLight) Color(0xFF141414) else Color(0xFFFAFAF7), animationSpec = themeAnimSpec, label = "inkPrimary")
    val inkSecondary by animateColorAsState(if (isLight) Color(0xFF5A5852) else Color(0xFFB5B3AA), animationSpec = themeAnimSpec, label = "inkSecondary")
    val inkMuted by animateColorAsState(if (isLight) Color(0xFF8A8780) else Color(0xFF889689), animationSpec = themeAnimSpec, label = "inkMuted")
    val clayAccent = Color(0xFFC4623D)
    val forestGreen by animateColorAsState(if (isLight) Color(0xFF2D6A4F) else Color(0xFF52B788), animationSpec = themeAnimSpec, label = "forestGreen")
    val togglePillBg by animateColorAsState(if (isLight) Color(0xFFF0ECE1) else Color(0xFF1B231B), animationSpec = themeAnimSpec, label = "togglePillBg")
    val lightIconTint by animateColorAsState(if (isLight) clayAccent else inkMuted, animationSpec = themeAnimSpec, label = "lightIconTint")
    val darkIconTint by animateColorAsState(if (!isLight) clayAccent else inkMuted, animationSpec = themeAnimSpec, label = "darkIconTint")
    val warningAmber = Color(0xFFD97706)

    val todayDateStr = remember {
        SimpleDateFormat("yyyy.MM.dd · EEEE", Locale.CHINESE).format(Date())
    }

    val listState = rememberLazyListState()

    val currentMonthOffset by remember(isCalendarExpanded) {
        derivedStateOf { if (isCalendarExpanded) pagerState.currentPage - 500 else 0 }
    }

    // 预计算各月份数据的高性能缓存（O(N) 仅在 allExpenses 变动时执行一次分组，滑动时为 O(1)）
    val monthDataCache = remember(allExpenses) {
        val cache = mutableMapOf<Int, MonthCalendarData>() // key: monthOffset
        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)
        val todayDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

        // 预聚合所有交易按 yearMonth (year * 100 + month)
        val expensesByYearMonth = allExpenses.groupBy { exp ->
            val c = Calendar.getInstance().apply { timeInMillis = exp.dateTimestamp }
            c.get(Calendar.YEAR) * 100 + c.get(Calendar.MONTH)
        }

        fun buildMonthData(offset: Int): MonthCalendarData {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, offset)
            }
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val firstDow = (dow + 5) % 7
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            val monthExpenses = expensesByYearMonth[y * 100 + m] ?: emptyList()
            var sumExpense = 0.0
            var sumIncome = 0.0

            val expensesByDay = monthExpenses.groupBy { exp ->
                val c = Calendar.getInstance().apply { timeInMillis = exp.dateTimestamp }
                c.get(Calendar.DAY_OF_MONTH)
            }

            val dayAggregates = mutableMapOf<Int, DayAggregate>()
            for (day in 1..maxDays) {
                val dayList = expensesByDay[day] ?: emptyList()
                val dayExpense = dayList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val dayIncome = dayList.filter { it.type == "INCOME" }.sumOf { it.amount }
                sumExpense += dayExpense
                sumIncome += dayIncome

                val dayCal = cal.clone() as Calendar
                dayCal.set(Calendar.DAY_OF_MONTH, day)
                val isToday = (y == currentYear && dayCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear)

                dayAggregates[day] = DayAggregate(
                    dayOfMonth = day,
                    timeInMillis = dayCal.timeInMillis,
                    isToday = isToday,
                    incomeSum = dayIncome,
                    expenseSum = dayExpense,
                    hasRecords = dayList.isNotEmpty()
                )
            }

            return MonthCalendarData(
                year = y,
                month = m,
                totalExpense = sumExpense,
                totalIncome = sumIncome,
                firstDayOfWeek = firstDow,
                maxDaysInMonth = maxDays,
                dayAggregates = dayAggregates
            )
        }

        // 默认预构建前后 6 个月，其余按需计算并存入
        for (offset in -6..6) {
            cache[offset] = buildMonthData(offset)
        }
        cache
    }

    // 辅助获取指定 offset 的月份数据
    val getMonthData: (Int) -> MonthCalendarData = remember(monthDataCache, allExpenses) {
        { offset ->
            monthDataCache.getOrPut(offset) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, offset)
                }
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH)
                val firstDow = cal.get(Calendar.DAY_OF_WEEK) - 1
                val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                val monthExpenses = allExpenses.filter { exp ->
                    val c = Calendar.getInstance().apply { timeInMillis = exp.dateTimestamp }
                    c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
                }
                val sumExpense = monthExpenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val sumIncome = monthExpenses.filter { it.type == "INCOME" }.sumOf { it.amount }

                val expensesByDay = monthExpenses.groupBy { exp ->
                    val c = Calendar.getInstance().apply { timeInMillis = exp.dateTimestamp }
                    c.get(Calendar.DAY_OF_MONTH)
                }

                val nowCal = Calendar.getInstance()
                val currentYear = nowCal.get(Calendar.YEAR)
                val todayDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

                val dayAggregates = mutableMapOf<Int, DayAggregate>()
                for (day in 1..maxDays) {
                    val dayList = expensesByDay[day] ?: emptyList()
                    val dayExpense = dayList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    val dayIncome = dayList.filter { it.type == "INCOME" }.sumOf { it.amount }

                    val dayCal = cal.clone() as Calendar
                    dayCal.set(Calendar.DAY_OF_MONTH, day)
                    val isToday = (y == currentYear && dayCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear)

                    dayAggregates[day] = DayAggregate(
                        dayOfMonth = day,
                        timeInMillis = dayCal.timeInMillis,
                        isToday = isToday,
                        incomeSum = dayIncome,
                        expenseSum = dayExpense,
                        hasRecords = dayList.isNotEmpty()
                    )
                }

                MonthCalendarData(
                    year = y,
                    month = m,
                    totalExpense = sumExpense,
                    totalIncome = sumIncome,
                    firstDayOfWeek = firstDow,
                    maxDaysInMonth = maxDays,
                    dayAggregates = dayAggregates
                )
            }
        }
    }

    val currentMonthData = getMonthData(currentMonthOffset)
    val displayMonthExpense = currentMonthData.totalExpense
    val displayMonthIncome = currentMonthData.totalIncome

    val nowCal = Calendar.getInstance()
    val isCurrentRealMonth = currentMonthOffset == 0
    val balanceLabel = if (isCurrentRealMonth) "本月结余" else "当月结余"

    // 预算与日均计算
    val monthlyLimit = budgetConfig.monthlyLimit
    val activeSpent = if (isCalendarExpanded || !isCurrentRealMonth) displayMonthExpense else thisMonthExpense
    val activeIncome = if (isCalendarExpanded || !isCurrentRealMonth) displayMonthIncome else thisMonthIncome
    val isOverBudget = monthlyLimit > 0 && activeSpent > monthlyLimit
    val overAmount = if (isOverBudget) activeSpent - monthlyLimit else 0.0
    val progressPercent = if (monthlyLimit <= 0) 0f else (activeSpent / monthlyLimit).toFloat().coerceAtLeast(0f)

    // 日均文案与计算：
    // 若为当前月份：显示“日均剩余”，数值为 剩余预算 / 剩余天数
    // 若非当前月份（历史或未来月份）：显示“日均使用”，数值为 当月支出 / 当月总天数
    val dailySpendLabel: String
    val dailyAverageValue: Double

    if (isCurrentRealMonth) {
        dailySpendLabel = "日均剩余"
        val totalDays = nowCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = nowCal.get(Calendar.DAY_OF_MONTH)
        val remainingDays = (totalDays - currentDay + 1).coerceAtLeast(1)
        val remainingBudget = monthlyLimit - activeSpent
        dailyAverageValue = if (!isOverBudget && remainingBudget > 0) remainingBudget / remainingDays else 0.0
    } else {
        dailySpendLabel = "日均使用"
        val totalDaysInDisplayMonth = currentMonthData.maxDaysInMonth.coerceAtLeast(1)
        dailyAverageValue = if (activeSpent > 0) activeSpent / totalDaysInDisplayMonth else 0.0
    }

    var expenseForActionDialog by remember { mutableStateOf<ExpenseEntity?>(null) }

    // 下方明细：
    // 1. 展开日历时未选中日期则默认显示当前日历展示月份的全部明细，若选中日期则显示所选日期明细；
    // 2. 收拢日历时若未选日期则默认展示本周记录，若选中日期则展示所选日期明细。
    val displayedExpenses = remember(allExpenses, selectedDateMillis, isCalendarExpanded, currentMonthOffset) {
        if (isCalendarExpanded) {
            if (selectedDateMillis != null) {
                val targetCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis!! }
                allExpenses.filter { exp ->
                    val expCal = Calendar.getInstance().apply { timeInMillis = exp.dateTimestamp }
                    isSameDay(expCal, targetCal)
                }
            } else {
                val targetCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, currentMonthOffset)
                }
                val targetYear = targetCal.get(Calendar.YEAR)
                val targetMonth = targetCal.get(Calendar.MONTH)
                allExpenses.filter { exp ->
                    val expCal = Calendar.getInstance().apply { timeInMillis = exp.dateTimestamp }
                    expCal.get(Calendar.YEAR) == targetYear && expCal.get(Calendar.MONTH) == targetMonth
                }
            }
        } else {
            if (selectedDateMillis != null) {
                val targetCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis!! }
                allExpenses.filter { exp ->
                    val expCal = Calendar.getInstance().apply { timeInMillis = exp.dateTimestamp }
                    isSameDay(expCal, targetCal)
                }
            } else {
                val now = Calendar.getInstance()
                val cal = now.clone() as Calendar
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
                val weekStart = cal.timeInMillis
                val weekEnd = weekStart + 7 * 24 * 60 * 60 * 1000L

                allExpenses.filter { it.dateTimestamp in weekStart until weekEnd }
            }
        }
    }

    // 选择/取消选择日期统一方法
    val handleDateSelection: (Long) -> Unit = { clickedMillis ->
        val currentSelected = selectedDateMillis
        if (currentSelected != null && isSameDay(currentSelected, clickedMillis)) {
            onSelectedDateChange(null)
        } else {
            val cal = Calendar.getInstance().apply {
                timeInMillis = clickedMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onSelectedDateChange(cal.timeInMillis)
        }
    }

    // 进入应用时的分层优雅级联进场动效 (Entrance Animation)
    val headerAnimAlpha = remember { Animatable(if (playEntranceAnimation) 0f else 1f) }
    val headerAnimSlide = remember { Animatable(if (playEntranceAnimation) -18f else 0f) }
    val heroAnimAlpha = remember { Animatable(if (playEntranceAnimation) 0f else 1f) }
    val heroAnimSlide = remember { Animatable(if (playEntranceAnimation) 26f else 0f) }
    val rulerAnimAlpha = remember { Animatable(if (playEntranceAnimation) 0f else 1f) }
    val rulerAnimSlide = remember { Animatable(if (playEntranceAnimation) 26f else 0f) }
    val listAnimAlpha = remember { Animatable(if (playEntranceAnimation) 0f else 1f) }
    val listAnimSlide = remember { Animatable(if (playEntranceAnimation) 26f else 0f) }

    LaunchedEffect(playEntranceAnimation) {
        if (playEntranceAnimation) {
            // 顶部 Header 优雅落定 (0~450ms)
            launch {
                headerAnimAlpha.animateTo(1f, animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing))
            }
            launch {
                headerAnimSlide.animateTo(0f, animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing))
            }

            // Hero 区域级联上浮淡入 (延时 100ms)
            launch {
                kotlinx.coroutines.delay(100)
                heroAnimAlpha.animateTo(1f, animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing))
            }
            launch {
                kotlinx.coroutines.delay(100)
                heroAnimSlide.animateTo(0f, animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing))
            }

            // 日历标尺区域级联上浮淡入 (延时 200ms)
            launch {
                kotlinx.coroutines.delay(200)
                rulerAnimAlpha.animateTo(1f, animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing))
            }
            launch {
                kotlinx.coroutines.delay(200)
                rulerAnimSlide.animateTo(0f, animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing))
            }

            // 账目流水明细列表级联上浮淡入 (延时 300ms)
            launch {
                kotlinx.coroutines.delay(300)
                listAnimAlpha.animateTo(1f, animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing))
            }
            launch {
                kotlinx.coroutines.delay(300)
                listAnimSlide.animateTo(0f, animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing))
                
                // 动画全部结束
                onEntranceAnimationPlayed()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 顶部 Header (应用进场动效)
            EditorialPageHeader(
                title = "Ledger",
                subtitle = todayDateStr,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = headerAnimAlpha.value
                        translationY = headerAnimSlide.value.dp.toPx()
                    }
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(togglePillBg)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onForceDarkPreviewChange(false) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LightMode,
                            contentDescription = "Cream Edition",
                            tint = lightIconTint,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = { onForceDarkPreviewChange(true) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = "Forest Edition",
                            tint = darkIconTint,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = dividerColor.copy(alpha = dividerColor.alpha * headerAnimAlpha.value)
            )

            // 2. 主体流式排版内容
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 2.1 融合版 Hero Section (带进场级联动画 & 滑动平滑动画过渡)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = heroAnimAlpha.value
                                translationY = heroAnimSlide.value.dp.toPx()
                            }
                    ) {
                        MergedEditorialHeroSection(
                            targetExpense = activeSpent,
                            targetIncome = activeIncome,
                            budgetLimit = monthlyLimit,
                            progressPercent = progressPercent,
                            isOverBudget = isOverBudget,
                            overAmount = overAmount,
                            dailySpendLabel = dailySpendLabel,
                            dailySpendValue = dailyAverageValue,
                            balanceLabel = balanceLabel,
                            onOpenBudgetSettings = onOpenBudgetSettings,
                            dividerColor = dividerColor,
                            inkPrimary = inkPrimary,
                            inkSecondary = inkSecondary,
                            inkMuted = inkMuted,
                            clayAccent = clayAccent,
                            forestGreen = forestGreen,
                            warningAmber = warningAmber,
                            isLight = isLight,
                            playEntranceAnimation = playEntranceAnimation
                        )
                    }
                }

                // 2.2 日历标尺组件（带进场级联动画 & 预渲染左右两侧视图）
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = rulerAnimAlpha.value
                                translationY = rulerAnimSlide.value.dp.toPx()
                            }
                    ) {
                        JournalDateRuler(
                            allExpenses = allExpenses,
                            selectedDateMillis = selectedDateMillis,
                            isExpanded = isCalendarExpanded,
                            onToggleExpand = { onCalendarExpandedChange(!isCalendarExpanded) },
                            onSelectDate = handleDateSelection,
                            pagerState = pagerState,
                            getMonthData = getMonthData,
                            dividerColor = dividerColor,
                            inkPrimary = inkPrimary,
                            inkMuted = inkMuted,
                            clayAccent = clayAccent,
                            forestGreen = forestGreen,
                            isLight = isLight
                        )
                    }
                }

                // 2.3 过滤状态提示条（选中日期或展开日历时展示）
                if (selectedDateMillis != null || isCalendarExpanded) {
                    item {
                        val headerLabel: String
                        val totalAmount = displayedExpenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                        val totalStr = AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(totalAmount))
                        if (selectedDateMillis != null) {
                            val dLabel = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE).format(Date(selectedDateMillis!!))
                            headerLabel = "$dLabel · 当日支出 ¥$totalStr"
                        } else {
                            val mLabel = "${currentMonthData.year}年${String.format(Locale.getDefault(), "%02d", currentMonthData.month + 1)}月"
                            headerLabel = "$mLabel · 当月支出 ¥$totalStr"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = listAnimAlpha.value
                                    translationY = listAnimSlide.value.dp.toPx()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(4.5.dp)
                                        .clip(CircleShape)
                                        .background(inkPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = headerLabel,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = inkPrimary
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                    }
                }

                // 2.4 流水明细区域 (带进场级联动画)
                if (displayedExpenses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = listAnimAlpha.value
                                    translationY = listAnimSlide.value.dp.toPx()
                                }
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (selectedDateMillis != null) "该日暂无支出记录" else if (isCalendarExpanded) "该月份暂无交易记录" else "本周暂无交易记录",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = inkMuted
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "+ 补记一笔",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = clayAccent,
                                    modifier = Modifier
                                        .clickable { onOpenAddExpense() }
                                        .padding(6.dp)
                                )
                            }
                        }
                    }
                } else {
                    val grouped = displayedExpenses.groupBy {
                        SimpleDateFormat("yyyy.MM.dd · EEEE", Locale.CHINESE).format(Date(it.dateTimestamp))
                    }
                    grouped.forEach { (dateHeader, itemsInDay) ->
                        item(key = "j_header_$dateHeader") {
                            Row(
                                modifier = Modifier
                                    .animateItem(
                                        fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f)
                                    )
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = listAnimAlpha.value
                                        translationY = listAnimSlide.value.dp.toPx()
                                    }
                                    .padding(top = 10.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dateHeader,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = inkMuted,
                                    letterSpacing = 0.8.sp
                                )
                                val daySum = itemsInDay.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                                if (daySum > 0) {
                                    Text(
                                        text = "小计 ¥${AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(daySum))}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.5.sp,
                                        color = inkMuted
                                    )
                                }
                            }
                        }
                        items(itemsInDay, key = { "j_${it.id}" }) { expense ->
                            Box(
                                modifier = Modifier
                                    .animateItem(
                                        fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f)
                                    )
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = listAnimAlpha.value
                                        translationY = listAnimSlide.value.dp.toPx()
                                    }
                            ) {
                                JournalTransactionRow(
                                    expense = expense,
                                    canvasBg = canvasBg,
                                    dividerColor = dividerColor,
                                    inkPrimary = inkPrimary,
                                    inkSecondary = inkSecondary,
                                    inkMuted = inkMuted,
                                    clayAccent = clayAccent,
                                    forestGreen = forestGreen,
                                    onItemClick = { expenseForActionDialog = expense },
                                    onDeleteExpense = { onDeleteExpense(expense) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 3. 明细操作选项弹窗（独立窗口覆盖状态栏，仅背景渐变与弹窗卡片缩放淡入） ───────────────
        if (expenseForActionDialog != null) {
            val exp = expenseForActionDialog!!
            var isDismissing by remember { mutableStateOf(false) }
            val animProgress by animateFloatAsState(
                targetValue = if (isDismissing) 0f else 1f,
                animationSpec = if (isDismissing) tween(160) else tween(220),
                label = "dialog_anim",
                finishedListener = { value ->
                    if (value == 0f && isDismissing) {
                        expenseForActionDialog = null
                    }
                }
            )

            val dialogScale by animateFloatAsState(
                targetValue = if (isDismissing) 0.90f else 1f,
                animationSpec = if (isDismissing) tween(160) else spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                label = "dialog_scale"
            )

            Dialog(
                onDismissRequest = {
                    if (!isDismissing) isDismissing = true
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                val isExpenseType = exp.type == "EXPENSE"
                val isIncomeType = exp.type == "INCOME"
                val amountCents = AmountFormatter.yuanToCents(exp.amount)
                val amountStr = AmountFormatter.formatCentsAsYuan(abs(amountCents))
                val dateStr = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()).format(Date(exp.dateTimestamp))
                val categoryTitle = exp.displaySubCategory.ifEmpty { exp.displayCategory }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f * animProgress))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isDismissing) isDismissing = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .graphicsLayer {
                                scaleX = dialogScale
                                scaleY = dialogScale
                                alpha = animProgress
                            }
                            .clip(RoundedCornerShape(20.dp))
                            .background(canvasBg)
                            .border(1.dp, dividerColor, RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* 阻止冒泡 */ }
                            .padding(horizontal = 22.dp, vertical = 18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (!isDismissing) isDismissing = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "关闭",
                                        tint = inkMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // 账目信息小面板
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(dividerColor.copy(alpha = 0.35f))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = categoryTitle,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = inkPrimary
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = dateStr,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.5.sp,
                                        color = inkMuted
                                    )
                                    if (exp.note.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = exp.note,
                                            fontSize = 12.sp,
                                            color = inkSecondary
                                        )
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "${if (isExpenseType) "-" else if (isIncomeType) "+" else ""}¥$amountStr",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpenseType) inkPrimary else if (isIncomeType) clayAccent else inkSecondary
                                    )
                                    if (exp.accountName.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = exp.accountName,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.5.sp,
                                            color = inkMuted
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // 选项 1: 编辑账目
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(clayAccent.copy(alpha = 0.12f))
                                    .clickable {
                                        val targetExp = exp
                                        expenseForActionDialog = null
                                        onEditExpense(targetExp)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = clayAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "编辑账目",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = clayAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 选项 2: 删除账目
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFDC2626).copy(alpha = 0.12f))
                                    .clickable {
                                        val targetExp = exp
                                        expenseForActionDialog = null
                                        onDeleteExpense(targetExp)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "删除账目",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MergedEditorialHeroSection(
    targetExpense: Double,
    targetIncome: Double,
    budgetLimit: Double,
    progressPercent: Float,
    isOverBudget: Boolean,
    overAmount: Double,
    dailySpendLabel: String,
    dailySpendValue: Double,
    balanceLabel: String,
    onOpenBudgetSettings: () -> Unit,
    dividerColor: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    inkMuted: Color,
    clayAccent: Color,
    forestGreen: Color,
    warningAmber: Color,
    isLight: Boolean,
    playEntranceAnimation: Boolean
) {
    var startAnim by remember { mutableStateOf(!playEntranceAnimation) }
    LaunchedEffect(playEntranceAnimation) {
        if (playEntranceAnimation) {
            kotlinx.coroutines.delay(100)
            startAnim = true
        }
    }

    // 平滑动画过渡（进场时从 0 开始数字跳动与进度条生长，平时为敏捷响应）
    val durationExp = if (playEntranceAnimation) 800 else 350
    val durationProg = if (playEntranceAnimation) 1000 else 350

    val animatedExpense by animateFloatAsState(
        targetValue = if (startAnim) targetExpense.toFloat() else 0f,
        animationSpec = tween(durationMillis = durationExp, easing = FastOutSlowInEasing),
        label = "hero_expense_anim"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnim) progressPercent.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = durationProg, easing = FastOutSlowInEasing),
        label = "hero_progress_anim"
    )
    val animatedPercent by animateFloatAsState(
        targetValue = if (startAnim) (progressPercent * 100f).coerceAtLeast(0f) else 0f,
        animationSpec = tween(durationMillis = durationProg, easing = FastOutSlowInEasing),
        label = "hero_percent_anim"
    )
    val animatedDailyAvg by animateFloatAsState(
        targetValue = if (startAnim) dailySpendValue.toFloat() else 0f,
        animationSpec = tween(durationMillis = durationExp, easing = FastOutSlowInEasing),
        label = "hero_daily_avg_anim"
    )
    val animatedOverAmt by animateFloatAsState(
        targetValue = if (startAnim) overAmount.toFloat() else 0f,
        animationSpec = tween(durationMillis = durationExp, easing = FastOutSlowInEasing),
        label = "hero_over_amt_anim"
    )
    val animatedNetBalance by animateFloatAsState(
        targetValue = if (startAnim) (targetIncome - targetExpense).toFloat() else 0f,
        animationSpec = tween(durationMillis = durationExp, easing = FastOutSlowInEasing),
        label = "hero_net_balance_anim"
    )

    val expenseFormatted = AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(animatedExpense.toDouble()))
    val budgetLimitFormatted = AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(budgetLimit))
    val dailyAvgFormatted = AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(animatedDailyAvg.toDouble()))
    val overAmountFormatted = AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(animatedOverAmt.toDouble()))

    val netBalanceFormatted = AmountFormatter.formatCentsAsYuan(abs(AmountFormatter.yuanToCents(animatedNetBalance.toDouble())))
    val isPositiveBalance = animatedNetBalance >= 0

    val progressColor = if (isOverBudget) clayAccent else if (progressPercent > 0.8f) warningAmber else inkPrimary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MONTHLY SPEND",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = inkMuted
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onOpenBudgetSettings() }
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "预算设定",
                    tint = inkMuted,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "预算设定",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    color = inkMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "¥",
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 30.sp,
                color = inkMuted,
                modifier = Modifier.padding(bottom = 8.dp, end = 6.dp)
            )
            Text(
                text = expenseFormatted,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 56.sp,
                fontWeight = FontWeight.Normal,
                color = inkPrimary,
                letterSpacing = (-1.8).sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(2.5.dp)
                    .width(46.dp)
                    .background(clayAccent)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$balanceLabel ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    color = inkMuted
                )
                Text(
                    text = "${if (isPositiveBalance) "+" else "-"}¥$netBalanceFormatted",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPositiveBalance) forestGreen else clayAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(dividerColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(2.5.dp)
                    .background(progressColor)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "预算 ¥$budgetLimitFormatted · 已用 ${animatedPercent.toInt()}%",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = inkMuted
            )

            if (isOverBudget) {
                Text(
                    text = "⚠️ 超支 ¥$overAmountFormatted",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = clayAccent
                )
            } else {
                Text(
                    text = "$dailySpendLabel ¥$dailyAvgFormatted",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = forestGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
    }
}

@Composable
private fun JournalDateRuler(
    allExpenses: List<ExpenseEntity>,
    selectedDateMillis: Long?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectDate: (Long) -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState,
    getMonthData: (Int) -> MonthCalendarData,
    dividerColor: Color,
    inkPrimary: Color,
    inkMuted: Color,
    clayAccent: Color,
    forestGreen: Color,
    isLight: Boolean
) {
    val currentMonthOffset by remember(isExpanded) {
        derivedStateOf { if (isExpanded) pagerState.currentPage - 500 else 0 }
    }
    val currentMonthData = getMonthData(currentMonthOffset)

    val daysOfWeek = listOf("一", "二", "三", "四", "五", "六", "日")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val monthHeader = "${currentMonthData.year} · ${String.format(Locale.getDefault(), "%02d", currentMonthData.month + 1)}月"
            Text(
                text = monthHeader,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = inkPrimary,
                letterSpacing = 0.5.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onToggleExpand() }
            ) {
                Text(
                    text = if (isExpanded) "收拢周条" else "展开全月",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = clayAccent
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = clayAccent,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 统一固定表头（周一至周日），与下方无论是周条还是全月网格的 7 列严格同宽对齐，避免展开/收拢时的重渲染与跳动
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { d ->
                Text(
                    text = d,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = inkMuted,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) togetherWith
                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
            },
            label = "calendar_expansion"
        ) { expanded ->
            if (!expanded) {
                val weekDays = remember {
                    val cal = Calendar.getInstance()
                    cal.firstDayOfWeek = Calendar.MONDAY
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    (0..6).map {
                        val dayCal = cal.clone() as Calendar
                        dayCal.add(Calendar.DAY_OF_WEEK, it)
                        dayCal
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDays.forEach { dayCal ->
                        val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
                        val isToday = isSameDay(dayCal, Calendar.getInstance())
                        val isSelected = selectedDateMillis != null && isSameDay(dayCal.timeInMillis, selectedDateMillis)

                        val hasRecord = allExpenses.any { exp ->
                            val expCal = Calendar.getInstance().apply { timeInMillis = exp.dateTimestamp }
                            isSameDay(expCal, dayCal)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) clayAccent.copy(alpha = 0.15f)
                                    else if (isToday) (if (isLight) Color(0xFFEFECE4) else Color(0xFF1F291F))
                                    else Color.Transparent
                                )
                                .clickable { onSelectDate(dayCal.timeInMillis) }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "$dayNum",
                                fontSize = 13.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) clayAccent else inkPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (hasRecord) clayAccent else if (isSelected) clayAccent else Color.Transparent)
                            )
                            if (isToday) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .height(1.5.dp)
                                        .background(clayAccent)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(3.5.dp))
                            }
                        }
                    }
                }
            } else {
                // beyondViewportPageCount = 1: 在渲染本月的同时静默预渲染左右两侧日历与数据，彻底杜绝滑动瞬间卡顿
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    val pageOffset = page - 500
                    val monthData = getMonthData(pageOffset)

                    EditorialMonthGrid(
                        monthData = monthData,
                        selectedDateMillis = selectedDateMillis,
                        onSelectDate = onSelectDate,
                        inkPrimary = inkPrimary,
                        inkMuted = inkMuted,
                        clayAccent = clayAccent,
                        forestGreen = forestGreen,
                        isLight = isLight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
    }
}

/**
 * 手帐报刊式流水单行
 * 备注显示在现在钱包的位置（字体也用现在钱包的字体），钱包显示在右侧数字下方
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
        label = "row_swipe_offset"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        ) {
            // 删除按钮：仅在左滑（animatedOffsetX < -0.5f）时实时渲染，位于右侧留白区域，不添加任何明细背景色
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

            // 明细内容：无背景色（保持透明度），随着手势左移，右侧留出与删除按钮的间隙
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
                                .background(if (isExpense) forestGreen else clayAccent)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = expense.displaySubCategory.ifEmpty { expense.displayCategory },
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
                            color = if (isExpense) inkPrimary else if (isIncome) clayAccent else inkSecondary
                        )
                        if (expense.accountName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = expense.accountName,
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

@Composable
private fun EditorialMonthGrid(
    monthData: MonthCalendarData,
    selectedDateMillis: Long?,
    onSelectDate: (Long) -> Unit,
    inkPrimary: Color,
    inkMuted: Color,
    clayAccent: Color,
    forestGreen: Color,
    isLight: Boolean
) {
    val firstDayOfWeek = monthData.firstDayOfWeek
    val maxDaysInMonth = monthData.maxDaysInMonth
    val dayAggregates = monthData.dayAggregates

    Column(modifier = Modifier.fillMaxWidth()) {
        var dayCounter = 1
        for (week in 0..5) {
            if (dayCounter > maxDaysInMonth) break
            Row(modifier = Modifier.fillMaxWidth()) {
                for (d in 0..6) {
                    if ((week == 0 && d < firstDayOfWeek) || dayCounter > maxDaysInMonth) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val currentDay = dayCounter
                        val dayData = dayAggregates[currentDay]
                        val dayTime = dayData?.timeInMillis ?: 0L
                        val isToday = dayData?.isToday ?: false
                        val isSelected = selectedDateMillis != null && isSameDay(dayTime, selectedDateMillis)

                        val hasRecord = dayData?.hasRecords ?: false
                        val dayIncome = dayData?.incomeSum ?: 0.0
                        val dayExpense = dayData?.expenseSum ?: 0.0

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) clayAccent.copy(alpha = 0.15f)
                                    else if (isToday) (if (isLight) Color(0xFFEFECE4) else Color(0xFF1F291F))
                                    else Color.Transparent
                                )
                                .clickable { onSelectDate(dayTime) }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = "$currentDay",
                                fontSize = 12.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) clayAccent else inkPrimary
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            if (hasRecord) {
                                if (dayIncome > 0) {
                                    Text(
                                        text = AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(dayIncome), withThousandsSeparator = false),
                                        fontSize = 7.5.sp,
                                        lineHeight = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = clayAccent,
                                        maxLines = 1
                                    )
                                }
                                if (dayExpense > 0) {
                                    Text(
                                        text = AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(dayExpense), withThousandsSeparator = false),
                                        fontSize = 7.5.sp,
                                        lineHeight = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = forestGreen,
                                        maxLines = 1
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.height(8.dp))
                            }
                        }
                        dayCounter++
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return isSameDay(cal1, cal2)
}

