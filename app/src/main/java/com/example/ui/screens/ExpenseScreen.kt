package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryItem
import com.example.data.local.CategoryManager
import com.example.data.local.ExpenseEntity
import com.example.ui.components.DaySummary
import com.example.ui.components.ExpenseAddEditDialog
import com.example.ui.components.GlassBackgroundWithGlow
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassGlowFab
import com.example.ui.components.GlowAmber
import com.example.ui.components.GlowCyan
import com.example.ui.components.GlowEmerald
import com.example.ui.components.GlowPink
import com.example.ui.components.GlowViolet
import com.example.ui.components.MonthCalendarView
import com.example.ui.components.AnimatedNumberText
import com.example.ui.theme.LocalAppBackgroundConfig
import com.example.ui.theme.LocalAppColorScheme
import com.example.ui.theme.ProgressColorTween
import com.example.ui.theme.ProgressFillSpring
import com.example.ui.viewmodel.BudgetConfig
import com.example.ui.viewmodel.BudgetPeriod
import com.example.ui.viewmodel.BudgetProgressInfo
import com.example.ui.viewmodel.CategoryStat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ExpenseScreen(
    expenses: List<ExpenseEntity>,
    accounts: List<AccountEntity>,
    allExpenses: List<ExpenseEntity> = expenses,
    thisMonthExpense: Double = 0.0,
    thisMonthIncome: Double = 0.0,
    totalExpense: Double,
    totalIncome: Double,
    todayExpense: Double,
    budgetConfig: BudgetConfig = BudgetConfig(),
    budgetProgress: BudgetProgressInfo = BudgetProgressInfo(
        period = BudgetPeriod.MONTH,
        budgetLimit = 3000.0,
        spentAmount = thisMonthExpense,
        remainingAmount = 3000.0 - thisMonthExpense,
        isOverBudget = thisMonthExpense > 3000.0,
        overAmount = if (thisMonthExpense > 3000.0) thisMonthExpense - 3000.0 else 0.0,
        progressPercent = (thisMonthExpense / 3000.0).toFloat().coerceAtLeast(0f)
    ),
    categoryStats: List<CategoryStat>,
    filterType: String,
    filterTime: String,
    searchQuery: String,
    showAddDialogTrigger: Boolean,
    selectedCalendarDay: Int? = null,
    onSelectCalendarDay: (Int?) -> Unit = {},
    isCategoryAnalysisExpanded: Boolean = true,
    onToggleCategoryAnalysisExpanded: () -> Unit = {},
    onSetFilterType: (String) -> Unit,
    onSetFilterTime: (String) -> Unit,
    onSetSearchQuery: (String) -> Unit,
    onSelectBudgetPeriod: (BudgetPeriod) -> Unit = {},
    onUpdateBudgetLimits: (Double, Double, Double) -> Unit = { _, _, _ -> },
    onOpenBillCalendar: () -> Unit = {},
    onOpenBudgetSettings: () -> Unit = {},
    onAddExpense: (type: String, category: String, subCategory: String, amount: Double, note: String, accountId: Long, accountName: String, timestamp: Long, transferToAccountId: Long?) -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit = {},
    onUpdateExpense: (ExpenseEntity, ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onCloseAddDialogTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = LocalAppColorScheme.current
    val bgConfig = LocalAppBackgroundConfig.current

    val showStatsPanel = isCategoryAnalysisExpanded
    var showSearchInput by remember { mutableStateOf(false) }

    // If calendar day is selected, filter display list to that day
    val displayedExpenses = remember(expenses, selectedCalendarDay) {
        if (selectedCalendarDay == null) {
            expenses
        } else {
            val thisCal = Calendar.getInstance()
            val tYear = thisCal.get(Calendar.YEAR)
            val tMonth = thisCal.get(Calendar.MONTH)
            expenses.filter { exp ->
                val expCal = Calendar.getInstance().apply { timeInMillis = exp.dateTimestamp }
                expCal.get(Calendar.YEAR) == tYear &&
                expCal.get(Calendar.MONTH) == tMonth &&
                expCal.get(Calendar.DAY_OF_MONTH) == selectedCalendarDay
            }
        }
    }

    // 默认预加载最近 20 条，按需增加加载量以节省资源
    val pageSize = 20
    var visibleLimit by remember(expenses.size, selectedCalendarDay, filterType, filterTime, searchQuery) {
        mutableIntStateOf(pageSize)
    }

    val paginatedExpenses = remember(displayedExpenses, visibleLimit) {
        displayedExpenses.take(visibleLimit)
    }
    val hasMore = displayedExpenses.size > paginatedExpenses.size
    val remainingCount = displayedExpenses.size - paginatedExpenses.size
    
    val groupedExpenses = remember(paginatedExpenses) {
        val sdf = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA)
        paginatedExpenses.groupBy { sdf.format(Date(it.dateTimestamp)) }
    }

    val listState = rememberLazyListState()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    GlassBackgroundWithGlow(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Area
                item {
                    Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "日常记账",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = bgConfig.textPrimary
                            )
                            Text(
                                text = "掌握每一笔收支 · 掌控理想生活",
                                style = MaterialTheme.typography.bodySmall,
                                color = bgConfig.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Top Right Action Buttons (Search Icon + Toggle Category Stats Button)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Toggle Category Stats Button
                            GlassCard(
                                shape = RoundedCornerShape(14.dp),
                                backgroundColor = if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.20f),
                                borderColor = Brush.linearGradient(
                                    listOf(
                                        Color(0xFF818CF8).copy(alpha = 0.6f),
                                        if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.2f)
                                    )
                                ),
                                onClick = { onToggleCategoryAnalysisExpanded() },
                                modifier = Modifier.testTag("toggle_stats_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (showStatsPanel) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "构成分析",
                                        tint = if (bgConfig.isLight) Color(0xFF4F46E5) else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showStatsPanel) "收起图表" else "分类分析",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (bgConfig.isLight) Color(0xFF4F46E5) else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Unified Panoramic Month Expense & Budget Card (Merged, bolder & larger amount, click to open settings)
                item {
                    // 显式 Animatable：splash 结束后首次进入 composition 时强制从 0 起步，
                    // 保证 budgetProgress.progressPercent == initial(0f) 时 spring 也能启动。
                    val animatedBudgetProgress = remember { androidx.compose.animation.core.Animatable(0f) }
                    LaunchedEffect(budgetProgress.progressPercent) {
                        animatedBudgetProgress.animateTo(
                            targetValue = budgetProgress.progressPercent.coerceIn(0f, 1f),
                            animationSpec = ProgressFillSpring
                        )
                    }
                    val animatedBudgetProgressValue = animatedBudgetProgress.value

                    val isBudgetOver = budgetProgress.isOverBudget
                    val budgetWarningColor = Color(0xFFEF4444)
                    val budgetSafeColor = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan

                    val budgetBarColor by animateColorAsState(
                        targetValue = if (isBudgetOver) budgetWarningColor else if (budgetProgress.progressPercent > 0.8f) Color(0xFFF59E0B) else budgetSafeColor,
                        animationSpec = ProgressColorTween,
                        label = "budgetBarColor"
                    )

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenBudgetSettings() }
                            .testTag("budget_progress_card"),
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = if (isBudgetOver) {
                            if (bgConfig.isLight) Color(0xFFFEF2F2).copy(alpha = 0.95f) else Color(0xFF331520).copy(alpha = 0.70f)
                        } else {
                            if (bgConfig.isLight) Color.White.copy(alpha = 0.95f) else Color(0xFF131C35).copy(alpha = 0.65f)
                        },
                        borderColor = Brush.linearGradient(
                            if (isBudgetOver) {
                                listOf(budgetWarningColor.copy(alpha = 0.5f), budgetWarningColor.copy(alpha = 0.2f))
                            } else if (bgConfig.isLight) {
                                listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.45f),
                                    Color(0xFF818CF8).copy(alpha = 0.4f),
                                    GlowCyan.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            }
                        ),
                        borderWidth = 1.2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp)
                        ) {
                            // Header Row: Indicator + "本月支出" & Right Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(if (bgConfig.isLight) Color(0xFF0284C7) else GlowCyan, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = "本月支出",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = bgConfig.textSecondary
                                    )
                                }

                                if (isBudgetOver) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(budgetWarningColor.copy(alpha = 0.15f))
                                            .border(1.dp, budgetWarningColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = budgetWarningColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "超支 ¥${String.format(Locale.CHINA, "%,.2f", budgetProgress.overAmount)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = budgetWarningColor
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (bgConfig.isLight) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.08f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "实时统计",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = bgConfig.textTertiary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Large & Bold Expense Amount (加粗加大)
                            // 显式 Animatable：splash 结束后首次进入 composition 时强制从 0 起步，
                            // 保证 thisMonthExpense == initial(0f) 时 tween(800) 数字滚动也能启动
                            val animatedThisMonthExpense = remember { androidx.compose.animation.core.Animatable(0f) }
                            LaunchedEffect(thisMonthExpense) {
                                animatedThisMonthExpense.animateTo(
                                    targetValue = thisMonthExpense.toFloat(),
                                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 800)
                                )
                            }
                            Text(
                                text = "¥${String.format(java.util.Locale.CHINA, "%,.2f", animatedThisMonthExpense.value)}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.6).sp
                                ),
                                color = bgConfig.textPrimary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Budget Info Row: Budget Limit on Left, Remaining Daily Avg / Overspend on Right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${budgetProgress.period.title}预算 ¥${String.format(Locale.CHINA, "%,.2f", budgetProgress.budgetLimit)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = bgConfig.textSecondary
                                )

                                if (!isBudgetOver) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "剩余日均 ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = bgConfig.textSecondary
                                        )
                                        Text(
                                            text = "¥${String.format(Locale.CHINA, "%,.2f", budgetProgress.remainingDailyAverage)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (bgConfig.isLight) Color(0xFF059669) else Color(0xFF34D399)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Animated Budget Progress Indicator (Clicking whole card triggers onOpenBudgetSettings)
                            LinearProgressIndicator(
                                progress = { animatedBudgetProgressValue },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = budgetBarColor,
                                trackColor = if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.12f),
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val monthNetBalance = thisMonthIncome - thisMonthExpense
                                Text(
                                    text = "本月结余 ¥${String.format(java.util.Locale.CHINA, "%,.2f", monthNetBalance)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (monthNetBalance < 0) budgetWarningColor else bgConfig.textSecondary
                                )
                                AnimatedNumberText(
                                    value = (budgetProgress.progressPercent * 100).toDouble(),
                                    prefix = "已用 ",
                                    suffix = "%",
                                    fractionDigits = 0,
                                    textStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = budgetBarColor
                                )
                            }
                        }
                    }
                }

                // 2. Category Stats Breakdown (Ultra-smooth animated transition)
                item {
                    AnimatedVisibility(
                        visible = showStatsPanel,
                        enter = expandVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                            expandFrom = Alignment.Top
                        ) + fadeIn(animationSpec = tween(200)),
                        exit = shrinkVertically(
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(animationSpec = tween(160))
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "支出分类构成占比",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = bgConfig.textPrimary
                                    )
                                    Text(
                                        text = "按金额降序",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = bgConfig.textTertiary
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (categoryStats.isEmpty()) {
                                    Text(
                                        text = "暂无支出分类数据",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = bgConfig.textTertiary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    categoryStats.forEach { stat ->
                                        val catColor = getCategoryGlowColor(stat.category)
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(catColor, CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = stat.category,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                        color = bgConfig.textPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "(${stat.count}笔)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = bgConfig.textTertiary
                                                    )
                                                }
                                                Text(
                                                    text = "¥${String.format(Locale.CHINA, "%.2f", stat.totalAmount)}  (${(stat.percentage * 100).toInt()}%)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = bgConfig.textPrimary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.1f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(stat.percentage.coerceIn(0f, 1f))
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(catColor)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Month Calendar View (Above transactions list)
                item {
                    MonthCalendarView(
                        expenses = allExpenses,
                        selectedDay = selectedCalendarDay,
                        onSelectDay = { day ->
                            onSelectCalendarDay(day)
                        },
                        onOpenBillCalendar = onOpenBillCalendar
                    )
                }

                // 4 & 5. Sticky Transactions List Header and Filters
                stickyHeader {
                    val pinnedAlpha by animateFloatAsState(
                        targetValue = if (listState.firstVisibleItemIndex >= 4) 0.95f else 0f,
                        label = "pinnedAlpha"
                    )
                    val stickyBgColor = if (bgConfig.isLight) Color.White.copy(alpha = pinnedAlpha) else Color(0xFF131C35).copy(alpha = pinnedAlpha)
                
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawRect(
                                    color = stickyBgColor,
                                    topLeft = Offset(-16.dp.toPx(), 0f),
                                    size = Size(size.width + 32.dp.toPx(), size.height)
                                )
                            }
                    ) {
                        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (selectedCalendarDay != null) "${selectedCalendarDay}日收支明细" else "收支明细记录",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bgConfig.textPrimary
                                )
                                if (selectedCalendarDay != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "· 清除筛选",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onSelectCalendarDay(null) }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                            .testTag("clear_day_filter_btn")
                                    )
                                }
                            }
                            Text(
                                text = if (hasMore) "已显 ${paginatedExpenses.size} / 共 ${displayedExpenses.size} 条" else "共 ${displayedExpenses.size} 条",
                                style = MaterialTheme.typography.labelSmall,
                                color = bgConfig.textTertiary
                            )
                        }

                        // 5. Quick Filter Buttons Row (Positioned between transaction title and cards for filtering records)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Type filter chips
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GlassChip(
                                        selected = filterType == "ALL",
                                        onClick = { onSetFilterType("ALL") },
                                        selectedGlowColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                                        modifier = Modifier.testTag("filter_type_all")
                                    ) {
                                        Text(
                                            text = "全部",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (filterType == "ALL") (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan) else bgConfig.textSecondary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                    GlassChip(
                                        selected = filterType == "EXPENSE",
                                        onClick = { onSetFilterType("EXPENSE") },
                                        selectedGlowColor = colorScheme.expenseColor,
                                        modifier = Modifier.testTag("filter_type_expense")
                                    ) {
                                        Text(
                                            text = "支出",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (filterType == "EXPENSE") colorScheme.expenseColor else bgConfig.textSecondary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                    GlassChip(
                                        selected = filterType == "INCOME",
                                        onClick = { onSetFilterType("INCOME") },
                                        selectedGlowColor = colorScheme.incomeColor,
                                        modifier = Modifier.testTag("filter_type_income")
                                    ) {
                                        Text(
                                            text = "收入",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (filterType == "INCOME") colorScheme.incomeColor else bgConfig.textSecondary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            // Circular Search Icon Button
                            GlassCard(
                                shape = CircleShape,
                                backgroundColor = if (showSearchInput || searchQuery.isNotBlank()) {
                                    if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.22f) else Color(0xFF6366F1).copy(alpha = 0.40f)
                                } else {
                                    if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.20f)
                                },
                                borderColor = Brush.linearGradient(
                                    listOf(
                                        Color(0xFF818CF8).copy(alpha = 0.7f),
                                        if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.25f)
                                    )
                                ),
                                onClick = {
                                    showSearchInput = !showSearchInput
                                    if (!showSearchInput && searchQuery.isNotBlank()) {
                                        onSetSearchQuery("")
                                    }
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("expense_search_icon_button")
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (showSearchInput || searchQuery.isNotBlank()) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = "搜索账目",
                                        tint = if (bgConfig.isLight) Color(0xFF4F46E5) else Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            }


                    // Expandable Search Box (sibling of the filter row so the button never moves)
                    AnimatedVisibility(
                        visible = showSearchInput || searchQuery.isNotBlank(),
                        enter = expandVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                            expandFrom = Alignment.Top
                        ) + fadeIn(tween(200)),
                        exit = shrinkVertically(
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(tween(160))
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSetSearchQuery,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "搜索",
                                    tint = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { onSetSearchQuery("") }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "清除",
                                            tint = bgConfig.textSecondary
                                        )
                                    }
                                }
                            },
                            placeholder = {
                                Text(
                                    "搜索分类、账户、金额或备注说明...",
                                    color = bgConfig.textTertiary,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    maxLines = 1
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = bgConfig.textPrimary,
                                unfocusedTextColor = bgConfig.textPrimary,
                                focusedContainerColor = bgConfig.inputFieldBg,
                                unfocusedContainerColor = bgConfig.inputFieldBg,
                                focusedBorderColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                                unfocusedBorderColor = bgConfig.inputFieldBorder,
                                cursorColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp)
                                .testTag("expense_search_field")
                        )
                    }
                        }
                    }
                }

                // 6. Items List or Empty State
                if (displayedExpenses.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan).copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (selectedCalendarDay != null) "该日暂无收支明细" else "暂无匹配的账目记录",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bgConfig.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "点击右下角「+」按钮，即刻添加一笔账单",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = bgConfig.textSecondary
                                )
                            }
                        }
                    }
                } else {
                    groupedExpenses.forEach { (dateStr, expensesForDate) ->
                        item(key = dateStr) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bgConfig.textSecondary
                                )
                            }
                        }
                        items(expensesForDate, key = { it.id }) { expense ->
                            GlassExpenseItemCard(
                                expense = expense,
                                expenseTextColor = colorScheme.expenseText,
                                incomeTextColor = colorScheme.incomeText,
                                onEdit = {
                                    onEditExpense(expense)
                                },
                                onDelete = { onDeleteExpense(expense) },
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                    }

                    if (hasMore) {
                        item(key = "load_more_footer") {
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .testTag("load_more_expenses_btn"),
                                shape = RoundedCornerShape(18.dp),
                                onClick = {
                                    visibleLimit += pageSize
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 13.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(
                                                    (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan).copy(alpha = 0.15f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "加载更多",
                                                tint = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "加载更多账单明细",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = bgConfig.textPrimary
                                            )
                                            Text(
                                                text = "已加载 ${paginatedExpenses.size} 条 · 剩余 $remainingCount 条未显示",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = bgConfig.textTertiary
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan).copy(alpha = 0.14f),
                                            modifier = Modifier
                                                .clickable { visibleLimit += pageSize }
                                                .testTag("load_next_20_btn")
                                        ) {
                                            Text(
                                                text = "+20条",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                        if (remainingCount > pageSize) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (bgConfig.isLight) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.08f),
                                                modifier = Modifier
                                                    .clickable { visibleLimit = displayedExpenses.size }
                                                    .testTag("load_all_expenses_btn")
                                            ) {
                                                Text(
                                                    text = "全部",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = bgConfig.textSecondary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (displayedExpenses.size > pageSize) {
                        item(key = "all_loaded_footer") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = bgConfig.textTertiary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "已展示全部 ${displayedExpenses.size} 笔账目记录",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = bgConfig.textTertiary
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(96.dp)) // padding for bottom nav
                }
            }
        }
    }
}



private fun formatAmount(amount: Double): String {
    val l = (amount * 100).toLong()
    val i = l / 100
    val d = l % 100
    return "$i.${if (d < 10) "0$d" else d}"
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GlassExpenseItemCard(
    expense: ExpenseEntity,
    expenseTextColor: Color,
    incomeTextColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val density = LocalDensity.current
    val isExpense = expense.type == "EXPENSE"
    val formattedDate = remember(expense.dateTimestamp) {
        java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA).format(java.util.Date(expense.dateTimestamp))
    }

    // 转账行使用专属图标与紫罗兰语义色（与记账弹窗「转账」Tab 一致）
    val categoryIcon = remember(expense.category, expense.type) {
        if (expense.type == "TRANSFER") Icons.Default.SwapHoriz else getCategoryIcon(expense.category)
    }
    val glowColor = remember(expense.category, expense.type) {
        if (expense.type == "TRANSFER") Color(0xFF8B5CF6) else getCategoryGlowColor(expense.category)
    }
    val itemTransferColor = Color(0xFF8B5CF6)

    // Swipe-to-delete/edit state (0: none, 1: edit/right, -1: delete/left)
    var swipeState by remember { mutableIntStateOf(0) }
    val maxSwipeDp = 84.dp
    val maxSwipePx = with(density) { maxSwipeDp.toPx() }
    var dragAmountAccumulated by remember { mutableFloatStateOf(0f) }

    val animatedOffsetPx by animateFloatAsState(
        targetValue = when (swipeState) {
            1 -> maxSwipePx
            -1 -> -maxSwipePx
            else -> 0f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "expenseItemSwipeOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        if (swipeState != 0 || kotlin.math.abs(animatedOffsetPx) > 1f) {
            val swipeProgress = (kotlin.math.abs(animatedOffsetPx) / maxSwipePx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = swipeProgress }
                    .clip(RoundedCornerShape(18.dp)),
                contentAlignment = if (animatedOffsetPx < 0) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                if (animatedOffsetPx < 0) {
                    // DELETE
                    Box(
                        modifier = Modifier
                            .width(maxSwipeDp)
                            .fillMaxHeight()
                            .background(Color(0xFFEF4444))
                            .clickable { swipeState = 0; onDelete() }
                            .testTag("expense_delete_button_${expense.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.White, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("删除", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // EDIT
                    Box(
                        modifier = Modifier
                            .width(maxSwipeDp)
                            .fillMaxHeight()
                            .background(Color(0xFF3B82F6))
                            .clickable { swipeState = 0; onEdit() }
                            .testTag("expense_edit_button_${expense.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.White, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("编辑", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Sliding Foreground Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetPx.roundToInt(), 0) }
                .pointerInput(expense.id) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragAmountAccumulated = when (swipeState) {
                                1 -> maxSwipePx
                                -1 -> -maxSwipePx
                                else -> 0f
                            }
                        },
                        onDragEnd = {
                            swipeState = if (dragAmountAccumulated < -maxSwipePx * 0.4f) -1
                                         else if (dragAmountAccumulated > maxSwipePx * 0.4f) 1
                                         else 0
                        },
                        onDragCancel = {
                            swipeState = 0
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragAmountAccumulated = (dragAmountAccumulated + dragAmount).coerceIn(-maxSwipePx * 1.2f, maxSwipePx * 1.2f)
                            if (dragAmountAccumulated < -maxSwipePx * 0.7f) swipeState = -1
                            else if (dragAmountAccumulated > maxSwipePx * 0.7f) swipeState = 1
                            else if (kotlin.math.abs(dragAmountAccumulated) < maxSwipePx * 0.2f) swipeState = 0
                        }
                    )
                }
        ) {
            GlassCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (swipeState != 0) {
                            swipeState = 0
                        } else {
                            onEdit()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Icon bubble
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                glowColor.copy(alpha = if (bgConfig.isLight) 0.15f else 0.18f),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                glowColor.copy(alpha = 0.45f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = expense.category,
                            tint = glowColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Category, Subcategory, Account Tag & Note info
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (expense.type == "TRANSFER") {
                                    if (expense.transferToAccountName.isNotBlank()) "转账 → ${expense.transferToAccountName}"
                                    else "账户间转账"
                                } else if (expense.subCategory.isNotBlank()) expense.subCategory else expense.category,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = bgConfig.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))

                            // Account Tag Badge
                            if (expense.accountName.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (bgConfig.isLight) Color(0xFF0284C7).copy(alpha = 0.10f) else Color(0xFF38BDF8).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.8.dp,
                                        if (bgConfig.isLight) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF38BDF8).copy(alpha = 0.35f)
                                    )
                                ) {
                                    Text(
                                        text = expense.accountName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (bgConfig.isLight) Color(0xFF0284C7) else Color(0xFF38BDF8),
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        if (expense.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = expense.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = bgConfig.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = bgConfig.textTertiary
                        )
                    }

                    // Amount & Quick Edit button
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = when {
                                expense.type == "TRANSFER" -> "⇌ ¥${formatAmount(expense.amount)}"
                                isExpense -> "- ¥${formatAmount(expense.amount)}"
                                else -> "+ ¥${formatAmount(expense.amount)}"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = when {
                                expense.type == "TRANSFER" -> itemTransferColor
                                isExpense -> expenseTextColor
                                else -> incomeTextColor
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑",
                                tint = bgConfig.textTertiary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * [已废弃] 无调用方，Phase 4 清理候选。
 */
@Deprecated("无调用方，Phase 4 清理候选")
@Composable
fun GlassAddOrEditExpenseDialog(
    expenseToEdit: ExpenseEntity?,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (type: String, category: String, subCategory: String, amount: Double, note: String, accountId: Long, accountName: String, timestamp: Long, transferToAccountId: Long?) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = LocalAppColorScheme.current
    val bgConfig = LocalAppBackgroundConfig.current

    // 0 = 支出, 1 = 收入, 2 = 转账
    var selectedTypeIndex by remember {
        mutableStateOf(
            when (expenseToEdit?.type) {
                "INCOME" -> 1
                "TRANSFER" -> 2
                else -> 0
            }
        )
    }
    val isTransfer = selectedTypeIndex == 2
    val currentType = if (selectedTypeIndex == 1) "INCOME" else "EXPENSE"
    // 转账 Tab 的紫罗兰语义色（与分类发光映射中「转账」一致）
    val transferColor = Color(0xFF8B5CF6)
    val fieldActiveColor = when (selectedTypeIndex) {
        1 -> colorScheme.incomeColor
        2 -> transferColor
        else -> colorScheme.expenseColor
    }

    // 转账对端账户；编辑既有转账回填，新建默认取与转出不同的第一个账户
    var transferToAccountId by remember {
        mutableStateOf(
            expenseToEdit?.transferToAccountId?.takeIf { it != 0L }
                ?: accounts.firstOrNull { it.id != (expenseToEdit?.accountId ?: accounts.firstOrNull()?.id) }?.id
                ?: accounts.firstOrNull()?.id
                ?: 0L
        )
    }

    /** 更换转出端后保证对端有效且不等于转出端 */
    fun ensureTransferTargetValid(fromId: Long) {
        if (!accounts.any { it.id == transferToAccountId } || transferToAccountId == fromId) {
            transferToAccountId = accounts.firstOrNull { it.id != fromId }?.id ?: fromId
        }
    }

    // Version counter to trigger refresh when custom categories or subcategories are added
    var categoriesRefreshKey by remember { mutableStateOf(0) }

    val allCategories = remember(currentType, categoriesRefreshKey) {
        CategoryManager.getCategories(context, currentType)
    }

    var selectedCategory by remember(currentType, categoriesRefreshKey) {
        mutableStateOf(
            if (expenseToEdit != null && allCategories.any { it.name == expenseToEdit.category }) {
                expenseToEdit.category
            } else if (selectedTypeIndex == 0) {
                "餐饮"
            } else {
                allCategories.firstOrNull()?.name ?: "工资"
            }
        )
    }

    val currentSubcategories = remember(selectedCategory, currentType, categoriesRefreshKey) {
        CategoryManager.getSubcategories(context, selectedCategory, currentType)
    }

    var selectedSubCategory by remember(selectedCategory, currentType, categoriesRefreshKey) {
        mutableStateOf(
            if (expenseToEdit != null && expenseToEdit.category == selectedCategory && expenseToEdit.subCategory.isNotBlank()) {
                expenseToEdit.subCategory
            } else {
                CategoryManager.getDefaultSubcategory(
                    context = context,
                    categoryName = selectedCategory,
                    type = currentType,
                    isFreshCreation = (expenseToEdit == null && selectedCategory == "餐饮")
                )
            }
        )
    }

    var selectedAccountId by remember {
        mutableStateOf(expenseToEdit?.accountId ?: (accounts.firstOrNull()?.id ?: 0L))
    }

    val selectedAccount = accounts.find { it.id == selectedAccountId }

    var amountInput by remember {
        mutableStateOf(if (expenseToEdit != null) expenseToEdit.amount.toString() else "")
    }
    var noteInput by remember {
        mutableStateOf(expenseToEdit?.note ?: "")
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddSubCategoryDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            shape = RoundedCornerShape(26.dp),
            backgroundColor = bgConfig.dialogBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expenseToEdit == null) "记一笔新账目" else "编辑账目",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = bgConfig.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type Segmented Glass Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f))
                        .padding(4.dp)
                ) {
                    // Expense Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedTypeIndex == 0) colorScheme.expenseColor.copy(alpha = 0.85f)
                                else Color.Transparent
                            )
                            .clickable {
                                selectedTypeIndex = 0
                                selectedCategory = "餐饮"
                                selectedSubCategory = CategoryManager.getDefaultSubcategory(context, "餐饮", "EXPENSE", true)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "支出 (-)",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTypeIndex == 0) Color.White else bgConfig.textSecondary
                        )
                    }

                    // Income Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedTypeIndex == 1) colorScheme.incomeColor.copy(alpha = 0.85f)
                                else Color.Transparent
                            )
                            .clickable {
                                selectedTypeIndex = 1
                                val incCats = CategoryManager.getCategories(context, "INCOME")
                                selectedCategory = incCats.firstOrNull()?.name ?: "工资"
                                selectedSubCategory = CategoryManager.getDefaultSubcategory(context, selectedCategory, "INCOME", false)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "收入 (+)",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTypeIndex == 1) Color.White else bgConfig.textSecondary
                        )
                    }

                    // Transfer Tab（账户间资金划转：隐藏分类，双端一条记录）
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedTypeIndex == 2) transferColor.copy(alpha = 0.9f)
                                else Color.Transparent
                            )
                            .clickable {
                                selectedTypeIndex = 2
                                ensureTransferTargetValid(selectedAccountId)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "转账 (⇌)",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTypeIndex == 2) Color.White else bgConfig.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount Input Field
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("金额 (¥)", color = bgConfig.textSecondary) },
                    placeholder = { Text("0.00", color = bgConfig.textTertiary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = bgConfig.textPrimary,
                        unfocusedTextColor = bgConfig.textPrimary,
                        focusedContainerColor = bgConfig.inputFieldBg,
                        unfocusedContainerColor = bgConfig.inputFieldBg,
                        focusedBorderColor = fieldActiveColor,
                        unfocusedBorderColor = bgConfig.inputFieldBorder,
                        cursorColor = fieldActiveColor
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Account Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTransfer) "转出账户" else "结算账户",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    if (isTransfer && accounts.size < 2) {
                        Text(
                            text = "转账需要至少两个不同账户",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                if (accounts.isEmpty()) {
                    Text(
                        text = "⚠️ 暂无可用账户，请前往「账户」页添加",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEF4444)
                    )
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(accounts, key = { it.id }) { acc ->
                            val isSelected = acc.id == selectedAccountId
                            GlassChip(
                                selected = isSelected,
                                onClick = {
                                    selectedAccountId = acc.id
                                    if (isTransfer) ensureTransferTargetValid(acc.id)
                                },
                                selectedGlowColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (acc.type) {
                                            "WECHAT" -> Icons.Default.Payment
                                            "ALIPAY" -> Icons.Default.CreditCard
                                            "BANK" -> Icons.Default.AccountBalance
                                            else -> Icons.Default.Payment
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) (if (bgConfig.isLight) Color(0xFF6366F1) else Color.White) else bgConfig.textTertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) (if (bgConfig.isLight) Color(0xFF6366F1) else Color.White) else bgConfig.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 转账专属：转入账户横滑选择器
                if (isTransfer) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "转入账户",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = bgConfig.textPrimary
                        )
                        if (!accounts.any { it.id == transferToAccountId && it.id != selectedAccountId }) {
                            Text(
                                text = "请选择收款账户",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(accounts.filter { it.id != selectedAccountId }, key = { it.id }) { acc ->
                            val isTargetSelected = acc.id == transferToAccountId
                            GlassChip(
                                selected = isTargetSelected,
                                onClick = { transferToAccountId = acc.id },
                                selectedGlowColor = transferColor
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (acc.type) {
                                            "WECHAT" -> Icons.Default.Payment
                                            "ALIPAY" -> Icons.Default.CreditCard
                                            "BANK" -> Icons.Default.AccountBalance
                                            else -> Icons.Default.Payment
                                        },
                                        contentDescription = null,
                                        tint = if (isTargetSelected) transferColor else bgConfig.textTertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isTargetSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isTargetSelected) transferColor else bgConfig.textSecondary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 2. Category Selection (Major)（转账无分类语义，整段隐藏）
                if (!isTransfer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTypeIndex == 0) "支出主分类 (默认: 餐饮)" else "收入主分类",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    Text(
                        text = "+ 自定义大类",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                        modifier = Modifier
                            .clickable { showAddCategoryDialog = true }
                            .padding(4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Major Category Chips Grid/List
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allCategories, key = { it.name }) { catItem ->
                        val isSelected = selectedCategory == catItem.name
                        val catGlow = getCategoryGlowColor(catItem.name)
                        GlassChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = catItem.name
                                selectedSubCategory = CategoryManager.getDefaultSubcategory(
                                    context = context,
                                    categoryName = catItem.name,
                                    type = currentType,
                                    isFreshCreation = (catItem.name == "餐饮" && expenseToEdit == null)
                                )
                            },
                            selectedGlowColor = catGlow
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(catItem.name),
                                    contentDescription = null,
                                    tint = if (isSelected) catGlow else bgConfig.textTertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = catItem.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) catGlow else bgConfig.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. SubCategory Selection (细分选项)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "「$selectedCategory」细分项目",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    Text(
                        text = "+ 添加细分",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                        modifier = Modifier
                            .clickable { showAddSubCategoryDialog = true }
                            .padding(4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(currentSubcategories, key = { it }) { sub ->
                        val isSelected = selectedSubCategory == sub
                        val catGlow = getCategoryGlowColor(selectedCategory)
                        GlassChip(
                            selected = isSelected,
                            onClick = {
                                selectedSubCategory = sub
                                CategoryManager.saveLastSelectedSubcategory(context, selectedCategory, sub)
                            },
                            selectedGlowColor = catGlow
                        ) {
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) catGlow else bgConfig.textSecondary,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
                } // end of !isTransfer category/subcategory sections

                Spacer(modifier = Modifier.height(14.dp))

                // Note Input Field
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("备注说明 (选填)", color = bgConfig.textSecondary) },
                    placeholder = { Text("如：朋友聚餐、买咖啡、乘地铁", color = bgConfig.textTertiary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = bgConfig.textPrimary,
                        unfocusedTextColor = bgConfig.textPrimary,
                        focusedContainerColor = bgConfig.inputFieldBg,
                        unfocusedContainerColor = bgConfig.inputFieldBg,
                        focusedBorderColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                        unfocusedBorderColor = bgConfig.inputFieldBorder,
                        cursorColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_note_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Save Action Button
                val isValidAmount = (amountInput.toDoubleOrNull() ?: 0.0) > 0
                val isAccountSelected = selectedAccount != null
                val transferTargetValid = !isTransfer ||
                    (transferToAccountId != selectedAccountId &&
                        accounts.any { it.id == transferToAccountId })

                if (!isAccountSelected && accounts.isNotEmpty()) {
                    Text(
                        text = "⚠️ 请先选择一个结算账户",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else if (isTransfer && !transferTargetValid) {
                    Text(
                        text = "⚠️ " + if (accounts.size < 2) "转账需要至少两个不同账户" else "请选择与转出不同的转入账户",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull() ?: 0.0
                        if (amount > 0 && selectedAccount != null && transferTargetValid) {
                            if (!isTransfer && selectedSubCategory.isNotBlank()) {
                                CategoryManager.saveLastSelectedSubcategory(context, selectedCategory, selectedSubCategory)
                            }
                            onConfirm(
                                when (selectedTypeIndex) {
                                    1 -> "INCOME"
                                    2 -> "TRANSFER"
                                    else -> "EXPENSE"
                                },
                                if (isTransfer) "" else selectedCategory,
                                if (isTransfer) "" else selectedSubCategory,
                                amount,
                                noteInput.trim(),
                                selectedAccount.id,
                                selectedAccount.name,
                                System.currentTimeMillis(),
                                if (isTransfer) transferToAccountId else null
                            )
                        }
                    },
                    enabled = isValidAmount && isAccountSelected && transferTargetValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = fieldActiveColor,
                        disabledContainerColor = if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_expense_button")
                ) {
                    Text(
                        text = if (expenseToEdit == null) "保存账目记录" else "更新账目记录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Dialog for Adding Custom Major Category
    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newSubCatListText by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddCategoryDialog = false }) {
            GlassCard(
                shape = RoundedCornerShape(22.dp),
                backgroundColor = bgConfig.dialogBackground,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "添加自定义大类",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("分类名称", color = bgConfig.textSecondary) },
                        placeholder = { Text("如：宠物、数码、公益", color = bgConfig.textTertiary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newSubCatListText,
                        onValueChange = { newSubCatListText = it },
                        label = { Text("细分子类别 (逗号分隔，选填)", color = bgConfig.textSecondary) },
                        placeholder = { Text("如：猫粮, 玩具, 疫苗, 其他", color = bgConfig.textTertiary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showAddCategoryDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("取消", color = bgConfig.textSecondary)
                        }

                        Button(
                            onClick = {
                                val trimmed = newCatName.trim()
                                if (trimmed.isNotBlank()) {
                                    val subList = newSubCatListText.split(",", "，", " ")
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                                    CategoryManager.addCustomCategory(
                                        context = context,
                                        name = trimmed,
                                        type = currentType,
                                        subcategories = if (subList.isEmpty()) listOf("默认", "其他") else subList
                                    )
                                    selectedCategory = trimmed
                                    selectedSubCategory = subList.firstOrNull() ?: "默认"
                                    categoriesRefreshKey++
                                    showAddCategoryDialog = false
                                }
                            },
                            enabled = newCatName.trim().isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan
                            )
                        ) {
                            Text("确认添加", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Dialog for Adding Custom Subcategory to Current Category
    if (showAddSubCategoryDialog) {
        var newSubName by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddSubCategoryDialog = false }) {
            GlassCard(
                shape = RoundedCornerShape(22.dp),
                backgroundColor = bgConfig.dialogBackground,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "为「$selectedCategory」添加细分",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newSubName,
                        onValueChange = { newSubName = it },
                        label = { Text("细分名称", color = bgConfig.textSecondary) },
                        placeholder = { Text("如：下午茶、过路费、文具", color = bgConfig.textTertiary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showAddSubCategoryDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("取消", color = bgConfig.textSecondary)
                        }

                        Button(
                            onClick = {
                                val trimmed = newSubName.trim()
                                if (trimmed.isNotBlank()) {
                                    CategoryManager.addCustomSubcategory(
                                        context = context,
                                        categoryName = selectedCategory,
                                        newSubcategory = trimmed
                                    )
                                    selectedSubCategory = trimmed
                                    CategoryManager.saveLastSelectedSubcategory(context, selectedCategory, trimmed)
                                    categoriesRefreshKey++
                                    showAddSubCategoryDialog = false
                                }
                            },
                            enabled = newSubName.trim().isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan
                            )
                        ) {
                            Text("确认添加", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when {
        category.contains("餐饮") || category.contains("美食") || category.contains("咖啡") -> Icons.Default.Restaurant
        category.contains("交通") || category.contains("出行") || category.contains("车") -> Icons.Default.DirectionsCar
        category.contains("购物") || category.contains("百货") || category.contains("超市") -> Icons.Default.ShoppingBag
        category.contains("娱乐") || category.contains("游戏") || category.contains("电影") -> Icons.Default.SportsEsports
        category.contains("医教") || category.contains("医疗") || category.contains("学") -> Icons.Default.MedicalServices
        category.contains("居家") || category.contains("水电") || category.contains("房") -> Icons.Default.Home
        category.contains("投资") || category.contains("理财") || category.contains("股票") -> Icons.Default.MonetizationOn
        category.contains("人情") || category.contains("礼金") || category.contains("红包") -> Icons.Default.CardGiftcard
        category.contains("工资") || category.contains("薪") || category.contains("工作") -> Icons.Default.Work
        category.contains("奖金") || category.contains("补贴") -> Icons.Default.Payment
        category.contains("漏记") || category.contains("校准") -> Icons.Default.Bookmark
        else -> Icons.Default.Category
    }
}

fun getCategoryGlowColor(category: String): Color {
    return when {
        category.contains("餐饮") || category.contains("美食") -> GlowAmber
        category.contains("交通") || category.contains("出行") -> GlowCyan
        category.contains("购物") || category.contains("百货") -> GlowPink
        category.contains("娱乐") || category.contains("游戏") -> GlowViolet
        category.contains("医教") || category.contains("医疗") || category.contains("学") -> Color(0xFF38BDF8)
        category.contains("居家") || category.contains("水电") || category.contains("房") -> Color(0xFF34D399)
        category.contains("投资") || category.contains("理财") || category.contains("股票") -> Color(0xFFF59E0B)
        category.contains("人情") || category.contains("礼金") || category.contains("红包") -> Color(0xFFEC4899)
        category.contains("工资") || category.contains("薪") -> GlowEmerald
        category.contains("奖金") || category.contains("补贴") -> Color(0xFFFBBF24)
        category.contains("漏记") || category.contains("校准") -> Color(0xFFF97316)
        else -> Color(0xFF818CF8)
    }
}
