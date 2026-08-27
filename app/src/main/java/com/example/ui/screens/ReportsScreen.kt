package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap as GraphStrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ExpenseEntity
import com.example.ui.components.GlassBackgroundWithGlow
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlowAmber
import com.example.ui.components.GlowCyan
import com.example.ui.components.GlowEmerald
import com.example.ui.components.GlowPink
import com.example.ui.components.GlowViolet
import com.example.ui.theme.LocalAppBackgroundConfig
import com.example.ui.theme.LocalAppColorScheme
import com.example.ui.viewmodel.CategoryStat
import com.example.ui.viewmodel.TrendPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ReportsScreen(
    expenses: List<ExpenseEntity>,
    totalExpense: Double,
    totalIncome: Double,
    categoryStats: List<CategoryStat>,
    incomeCategoryStats: List<CategoryStat>,
    weekTrendPoints: List<TrendPoint>,
    monthTrendPoints: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    val colorScheme = LocalAppColorScheme.current
    val bgConfig = LocalAppBackgroundConfig.current

    var chartMode by remember { mutableStateOf("TREND") } // "TREND" or "PIE"
    var timeFilter by remember { mutableStateOf("THIS_MONTH") } // "ALL", "THIS_WEEK", "THIS_MONTH", "LAST_MONTH", "THIS_YEAR", "CUSTOM"
    var typeFilter by remember { mutableStateOf("ALL") } // "ALL", "EXPENSE", "INCOME"
    var pieType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    var categoryLevel by remember { mutableStateOf("MAJOR") } // "MAJOR" (大类) or "SUB" (小类)

    // Custom Date Range State (default to last 30 days)
    val now = remember { System.currentTimeMillis() }
    var customStartDate by remember { mutableLongStateOf(now - 30L * 24 * 60 * 60 * 1000) }
    var customEndDate by remember { mutableLongStateOf(now) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    // Dynamic Filtered Expenses
    val filteredExpenses by remember(expenses, timeFilter, typeFilter, customStartDate, customEndDate) {
        derivedStateOf {
            val (startMs, endMs) = getTimeRangeBounds(timeFilter, customStartDate, customEndDate)
            expenses.filter { item ->
                val matchesTime = item.dateTimestamp in startMs..endMs
                val matchesType = when (typeFilter) {
                    "EXPENSE" -> item.type == "EXPENSE"
                    "INCOME" -> item.type == "INCOME"
                    else -> true
                }
                matchesTime && matchesType
            }
        }
    }

    // Dynamic Metric Aggregations
    val dynamicTotalExpense by remember(filteredExpenses) {
        derivedStateOf {
            filteredExpenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        }
    }

    val dynamicTotalIncome by remember(filteredExpenses) {
        derivedStateOf {
            filteredExpenses.filter { it.type == "INCOME" }.sumOf { it.amount }
        }
    }

    val dynamicExpenseCategoryStats by remember(filteredExpenses, dynamicTotalExpense, categoryLevel) {
        derivedStateOf {
            val expOnly = filteredExpenses.filter { it.type == "EXPENSE" }
            if (dynamicTotalExpense <= 0) emptyList()
            else {
                if (categoryLevel == "MAJOR") {
                    expOnly.groupBy { it.category }
                        .map { (cat, list) ->
                            val sum = list.sumOf { it.amount }
                            CategoryStat(
                                category = cat,
                                totalAmount = sum,
                                count = list.size,
                                percentage = (sum / dynamicTotalExpense).toFloat(),
                                type = "EXPENSE"
                            )
                        }
                        .sortedByDescending { it.totalAmount }
                } else {
                    expOnly.groupBy { it.subCategory.ifBlank { it.category } }
                        .map { (subCat, list) ->
                            val sum = list.sumOf { it.amount }
                            CategoryStat(
                                category = subCat,
                                totalAmount = sum,
                                count = list.size,
                                percentage = (sum / dynamicTotalExpense).toFloat(),
                                type = "EXPENSE"
                            )
                        }
                        .sortedByDescending { it.totalAmount }
                }
            }
        }
    }

    val dynamicIncomeCategoryStats by remember(filteredExpenses, dynamicTotalIncome, categoryLevel) {
        derivedStateOf {
            val incOnly = filteredExpenses.filter { it.type == "INCOME" }
            if (dynamicTotalIncome <= 0) emptyList()
            else {
                if (categoryLevel == "MAJOR") {
                    incOnly.groupBy { it.category }
                        .map { (cat, list) ->
                            val sum = list.sumOf { it.amount }
                            CategoryStat(
                                category = cat,
                                totalAmount = sum,
                                count = list.size,
                                percentage = (sum / dynamicTotalIncome).toFloat(),
                                type = "INCOME"
                            )
                        }
                        .sortedByDescending { it.totalAmount }
                } else {
                    incOnly.groupBy { it.subCategory.ifBlank { it.category } }
                        .map { (subCat, list) ->
                            val sum = list.sumOf { it.amount }
                            CategoryStat(
                                category = subCat,
                                totalAmount = sum,
                                count = list.size,
                                percentage = (sum / dynamicTotalIncome).toFloat(),
                                type = "INCOME"
                            )
                        }
                        .sortedByDescending { it.totalAmount }
                }
            }
        }
    }

    // Dynamic Trend Points for Chart
    val dynamicTrendPoints by remember(filteredExpenses, timeFilter, customStartDate, customEndDate) {
        derivedStateOf {
            generateDynamicTrendPoints(filteredExpenses, timeFilter, customStartDate, customEndDate)
        }
    }

    val activeCategoryStats = if (pieType == "EXPENSE") dynamicExpenseCategoryStats else dynamicIncomeCategoryStats
    val activeTotalAmount = if (pieType == "EXPENSE") dynamicTotalExpense else dynamicTotalIncome
    val activeColoredCategoryStats by remember(activeCategoryStats) {
        derivedStateOf { assignDistinctPieColors(activeCategoryStats) }
    }

    val timeRangeDescription = remember(timeFilter, customStartDate, customEndDate) {
        getTimeRangeDisplayLabel(timeFilter, customStartDate, customEndDate)
    }

    GlassBackgroundWithGlow(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Title
            item {
                Spacer(modifier = Modifier.statusBarsPadding())
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "财务图表分析",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = bgConfig.textPrimary
                        )
                        Text(
                            text = "多维收支趋势走势与分类构成占比",
                            style = MaterialTheme.typography.bodySmall,
                            color = bgConfig.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Date range picker quick button
                    GlassCard(
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.25f),
                        borderColor = Brush.linearGradient(
                            listOf(
                                Color(0xFF818CF8).copy(alpha = 0.8f),
                                if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.2f)
                            )
                        ),
                        onClick = { showCustomDateDialog = true },
                        modifier = Modifier.testTag("report_custom_date_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "时间范围",
                                tint = if (bgConfig.isLight) Color(0xFF4F46E5) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "自定义时间",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (bgConfig.isLight) Color(0xFF4F46E5) else Color.White
                            )
                        }
                    }
                }
            }

            // 1. Time & Type Filter Chips Section
            item {
                GlassCard(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Time Range Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "统计周期",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = bgConfig.textPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = timeRangeDescription,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan
                            )
                        }

                        // Time Chips Horizontal Scroll
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val timeOptions = listOf(
                                "THIS_MONTH" to "本月",
                                "THIS_WEEK" to "近7天",
                                "LAST_MONTH" to "上月",
                                "THIS_YEAR" to "本年",
                                "ALL" to "全部历史",
                                "CUSTOM" to "自定义 📅"
                            )

                            timeOptions.forEach { (key, label) ->
                                val isSelected = timeFilter == key
                                GlassChip(
                                    selected = isSelected,
                                    onClick = {
                                        timeFilter = key
                                        if (key == "CUSTOM") {
                                            showCustomDateDialog = true
                                        }
                                    },
                                    selectedGlowColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) (if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan) else bgConfig.textSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Type Filter Chips Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FilterAlt,
                                    contentDescription = null,
                                    tint = bgConfig.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "收支类型",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bgConfig.textPrimary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GlassChip(
                                    selected = typeFilter == "ALL",
                                    onClick = { typeFilter = "ALL" },
                                    selectedGlowColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan
                                ) {
                                    Text(
                                        text = "全部收支",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (typeFilter == "ALL") FontWeight.Bold else FontWeight.Normal,
                                        color = if (typeFilter == "ALL") (if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan) else bgConfig.textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                                GlassChip(
                                    selected = typeFilter == "EXPENSE",
                                    onClick = {
                                        typeFilter = "EXPENSE"
                                        pieType = "EXPENSE"
                                    },
                                    selectedGlowColor = colorScheme.expenseColor
                                ) {
                                    Text(
                                        text = "仅支出",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (typeFilter == "EXPENSE") FontWeight.Bold else FontWeight.Normal,
                                        color = if (typeFilter == "EXPENSE") colorScheme.expenseColor else bgConfig.textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                                GlassChip(
                                    selected = typeFilter == "INCOME",
                                    onClick = {
                                        typeFilter = "INCOME"
                                        pieType = "INCOME"
                                    },
                                    selectedGlowColor = colorScheme.incomeColor
                                ) {
                                    Text(
                                        text = "仅收入",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (typeFilter == "INCOME") FontWeight.Bold else FontWeight.Normal,
                                        color = if (typeFilter == "INCOME") colorScheme.incomeColor else bgConfig.textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Chart Type Segmented Switch (趋势图 vs 饼状图)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            if (bgConfig.isLight) Color(0xFFCBD5E1) else Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(4.dp)
                ) {
                    // Trend Chart Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (chartMode == "TREND") Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                                ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { chartMode = "TREND" }
                            .padding(vertical = 10.dp)
                            .testTag("report_tab_trend"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = if (chartMode == "TREND") Color.White else bgConfig.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "收支趋势图",
                                fontWeight = if (chartMode == "TREND") FontWeight.Bold else FontWeight.Normal,
                                color = if (chartMode == "TREND") Color.White else bgConfig.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Pie Chart Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (chartMode == "PIE") Brush.linearGradient(
                                    listOf(Color(0xFF06B6D4), Color(0xFF0284C7))
                                ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { chartMode = "PIE" }
                            .padding(vertical = 10.dp)
                            .testTag("report_tab_pie"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = if (chartMode == "PIE") Color.White else bgConfig.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "分类饼状图",
                                fontWeight = if (chartMode == "PIE") FontWeight.Bold else FontWeight.Normal,
                                color = if (chartMode == "PIE") Color.White else bgConfig.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 3. Dynamic Chart Card Content
            item {
                if (chartMode == "TREND") {
                    // Trend Chart Card
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        backgroundColor = if (bgConfig.isLight) Color.White.copy(alpha = 0.95f) else Color(0xFF131C35).copy(alpha = 0.65f),
                        borderColor = Brush.linearGradient(
                            if (bgConfig.isLight) listOf(
                                Color(0xFFE2E8F0),
                                Color(0xFFCBD5E1)
                            ) else listOf(
                                Color.White.copy(alpha = 0.45f),
                                GlowViolet.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        )
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
                                    text = "收支趋势对比走势 (元)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bgConfig.textPrimary
                                )

                                Text(
                                    text = "共 ${filteredExpenses.size} 笔明细",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = bgConfig.textTertiary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Canvas Trend Graph
                            TrendLineAndBarChart(
                                points = dynamicTrendPoints,
                                expenseColor = colorScheme.expenseColor,
                                incomeColor = colorScheme.incomeColor,
                                isLight = bgConfig.isLight,
                                textTertiary = bgConfig.textTertiary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Legend Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(colorScheme.expenseColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "支出 ¥${String.format(Locale.CHINA, "%,.2f", dynamicTotalExpense)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = bgConfig.textSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(18.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(colorScheme.incomeColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "收入 ¥${String.format(Locale.CHINA, "%,.2f", dynamicTotalIncome)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = bgConfig.textSecondary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Pie Chart Card
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        backgroundColor = if (bgConfig.isLight) Color.White.copy(alpha = 0.95f) else Color(0xFF131C35).copy(alpha = 0.65f),
                        borderColor = Brush.linearGradient(
                            if (bgConfig.isLight) listOf(
                                Color(0xFFE2E8F0),
                                Color(0xFFCBD5E1)
                            ) else listOf(
                                Color.White.copy(alpha = 0.45f),
                                GlowCyan.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Sub filters: Expense vs Income
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (pieType == "EXPENSE") "支出分类饼状图" else "收入分类饼状图",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bgConfig.textPrimary
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GlassChip(
                                        selected = pieType == "EXPENSE",
                                        onClick = { pieType = "EXPENSE" },
                                        selectedGlowColor = colorScheme.expenseColor
                                    ) {
                                        Text(
                                            text = "支出占比",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (pieType == "EXPENSE") colorScheme.expenseColor else bgConfig.textSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                    GlassChip(
                                        selected = pieType == "INCOME",
                                        onClick = { pieType = "INCOME" },
                                        selectedGlowColor = colorScheme.incomeColor
                                    ) {
                                        Text(
                                            text = "收入占比",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (pieType == "INCOME") colorScheme.incomeColor else bgConfig.textSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Interactive Donut Canvas
                            DonutPieChart(
                                coloredStats = activeColoredCategoryStats,
                                totalAmount = activeTotalAmount,
                                typeLabel = if (pieType == "EXPENSE") "当前区间支出" else "当前区间收入",
                                textPrimary = bgConfig.textPrimary,
                                textSecondary = bgConfig.textSecondary,
                                textTertiary = bgConfig.textTertiary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Major Category vs Sub Category Pie Chart Switch (大类饼状图 vs 小类饼状图)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (bgConfig.isLight) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.08f))
                                        .border(
                                            1.dp,
                                            if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.12f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(
                                                if (categoryLevel == "MAJOR") Brush.linearGradient(
                                                    listOf(Color(0xFF06B6D4), Color(0xFF0284C7))
                                                ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                            )
                                            .clickable { categoryLevel = "MAJOR" }
                                            .padding(horizontal = 16.dp, vertical = 7.dp)
                                            .testTag("report_pie_level_major"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "大类饼状图",
                                            fontSize = 12.sp,
                                            fontWeight = if (categoryLevel == "MAJOR") FontWeight.Bold else FontWeight.Medium,
                                            color = if (categoryLevel == "MAJOR") Color.White else bgConfig.textSecondary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(
                                                if (categoryLevel == "SUB") Brush.linearGradient(
                                                    listOf(Color(0xFF06B6D4), Color(0xFF0284C7))
                                                ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                            )
                                            .clickable { categoryLevel = "SUB" }
                                            .padding(horizontal = 16.dp, vertical = 7.dp)
                                            .testTag("report_pie_level_sub"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "小类饼状图",
                                            fontSize = 12.sp,
                                            fontWeight = if (categoryLevel == "SUB") FontWeight.Bold else FontWeight.Medium,
                                            color = if (categoryLevel == "SUB") Color.White else bgConfig.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Summary Metric Highlights
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total In vs Out Savings summary
                    val netSavings = dynamicTotalIncome - dynamicTotalExpense
                    val savingsRate = if (dynamicTotalIncome > 0) ((netSavings / dynamicTotalIncome) * 100).toInt() else 0

                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "区间资金结余率",
                                style = MaterialTheme.typography.labelSmall,
                                color = bgConfig.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$savingsRate %",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (savingsRate >= 0) (if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald) else (if (bgConfig.isLight) Color(0xFFE11D48) else GlowPink)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "净结余 ¥${String.format(Locale.CHINA, "%,.2f", netSavings)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = bgConfig.textTertiary
                            )
                        }
                    }

                    // Max Category Highlight
                    val topCategory = if (pieType == "EXPENSE") dynamicExpenseCategoryStats.firstOrNull() else dynamicIncomeCategoryStats.firstOrNull()
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = if (pieType == "EXPENSE") "最大支出分类项" else "主要收入来源项",
                                style = MaterialTheme.typography.labelSmall,
                                color = bgConfig.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = topCategory?.category ?: "无数据",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = GlowAmber,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (topCategory != null) "¥${String.format(Locale.CHINA, "%.2f", topCategory.totalAmount)} (${(topCategory.percentage * 100).toInt()}%)" else "暂无记录",
                                style = MaterialTheme.typography.labelSmall,
                                color = bgConfig.textTertiary
                            )
                        }
                    }
                }
            }

            // 5. Category Ranking List Breakdown
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (pieType == "EXPENSE") "支出分类排行榜" else "收入分类排行榜",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    Text(
                        text = "共 ${activeCategoryStats.size} 项分类",
                        style = MaterialTheme.typography.labelSmall,
                        color = bgConfig.textTertiary
                    )
                }
            }

            if (activeCategoryStats.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "当前筛选周期内暂无收支明细",
                                style = MaterialTheme.typography.bodyMedium,
                                color = bgConfig.textSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    timeFilter = "ALL"
                                    typeFilter = "ALL"
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("重置筛选条件")
                            }
                        }
                    }
                }
            } else {
                items(activeColoredCategoryStats) { item ->
                    val stat = item.stat
                    val color = item.color
                    GlassCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stat.category,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = bgConfig.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${stat.count}笔",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = bgConfig.textTertiary
                                    )
                                }

                                Text(
                                    text = "¥${String.format(Locale.CHINA, "%,.2f", stat.totalAmount)}  (${(stat.percentage * 100).toInt()}%)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = bgConfig.textPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { stat.percentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = color,
                                trackColor = if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.1f),
                                strokeCap = GraphStrokeCap.Round
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp)) // padding for bottom nav
            }
        }
    }

    // Custom Date Range Dialog
    if (showCustomDateDialog) {
        CustomDateRangeDialog(
            initialStartDate = customStartDate,
            initialEndDate = customEndDate,
            onDismiss = { showCustomDateDialog = false },
            onConfirm = { start, end ->
                customStartDate = start
                customEndDate = end
                timeFilter = "CUSTOM"
                showCustomDateDialog = false
            }
        )
    }
}

/**
 * Custom Date Range Picker Dialog with Preset Ranges and Precise Steppers
 */
@Composable
fun CustomDateRangeDialog(
    initialStartDate: Long,
    initialEndDate: Long,
    onDismiss: () -> Unit,
    onConfirm: (startMs: Long, endMs: Long) -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current

    val calStart = remember {
        Calendar.getInstance().apply { timeInMillis = initialStartDate }
    }
    val calEnd = remember {
        Calendar.getInstance().apply { timeInMillis = initialEndDate }
    }

    var startYear by remember { mutableIntStateOf(calStart.get(Calendar.YEAR)) }
    var startMonth by remember { mutableIntStateOf(calStart.get(Calendar.MONTH) + 1) }
    var startDay by remember { mutableIntStateOf(calStart.get(Calendar.DAY_OF_MONTH)) }

    var endYear by remember { mutableIntStateOf(calEnd.get(Calendar.YEAR)) }
    var endMonth by remember { mutableIntStateOf(calEnd.get(Calendar.MONTH) + 1) }
    var endDay by remember { mutableIntStateOf(calEnd.get(Calendar.DAY_OF_MONTH)) }

    fun buildTimestamp(year: Int, month: Int, day: Int, endOfDay: Boolean): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, day.coerceIn(1, maxDay))
            if (endOfDay) {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            } else {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }.timeInMillis
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = if (bgConfig.isLight) Color.White.copy(alpha = 0.96f) else Color(0xFF131C35).copy(alpha = 0.94f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "自定义报表时间范围",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = bgConfig.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Presets
                Text(
                    text = "快捷时间区间",
                    style = MaterialTheme.typography.labelSmall,
                    color = bgConfig.textSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "近3天" to 3,
                        "近7天" to 7,
                        "近15天" to 15,
                        "近30天" to 30,
                        "近90天" to 90,
                        "近180天" to 180,
                        "近1年" to 365
                    )

                    presets.forEach { (title, days) ->
                        GlassChip(
                            selected = false,
                            onClick = {
                                val endC = Calendar.getInstance()
                                val startC = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, -(days - 1))
                                }
                                startYear = startC.get(Calendar.YEAR)
                                startMonth = startC.get(Calendar.MONTH) + 1
                                startDay = startC.get(Calendar.DAY_OF_MONTH)

                                endYear = endC.get(Calendar.YEAR)
                                endMonth = endC.get(Calendar.MONTH) + 1
                                endDay = endC.get(Calendar.DAY_OF_MONTH)
                            }
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Start Date Selector
                DateUnitSelectorRow(
                    label = "起始日期 (Start)",
                    year = startYear,
                    month = startMonth,
                    day = startDay,
                    onYearChange = { startYear = it },
                    onMonthChange = { startMonth = it },
                    onDayChange = { startDay = it },
                    textColor = bgConfig.textPrimary,
                    secondaryColor = bgConfig.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // End Date Selector
                DateUnitSelectorRow(
                    label = "截止日期 (End)",
                    year = endYear,
                    month = endMonth,
                    day = endDay,
                    onYearChange = { endYear = it },
                    onMonthChange = { endMonth = it },
                    onDayChange = { endDay = it },
                    textColor = bgConfig.textPrimary,
                    secondaryColor = bgConfig.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Range Preview Badge
                val sTime = buildTimestamp(startYear, startMonth, startDay, false)
                val eTime = buildTimestamp(endYear, endMonth, endDay, true)
                val daySpan = ((eTime - sTime) / (1000 * 60 * 60 * 24)).coerceAtLeast(0) + 1

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (bgConfig.isLight) Color(0xFFEEF2FF) else Color(0xFF1E293B))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "统计范围：${String.format(Locale.CHINA, "%04d-%02d-%02d", startYear, startMonth, startDay)} 至 ${String.format(Locale.CHINA, "%04d-%02d-%02d", endYear, endMonth, endDay)} (共 $daySpan 天)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (bgConfig.isLight) Color(0xFF4338CA) else GlowCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("取消", color = bgConfig.textSecondary)
                    }

                    Button(
                        onClick = {
                            val startMs = buildTimestamp(startYear, startMonth, startDay, false)
                            val endMs = buildTimestamp(endYear, endMonth, endDay, true)
                            if (startMs <= endMs) {
                                onConfirm(startMs, endMs)
                            } else {
                                onConfirm(endMs, startMs)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowViolet
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("确认应用", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DateUnitSelectorRow(
    label: String,
    year: Int,
    month: Int,
    day: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit,
    textColor: Color,
    secondaryColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = secondaryColor,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Year Selector
            StepperBox(
                value = "$year 年",
                onMinus = { onYearChange((year - 1).coerceAtLeast(2020)) },
                onPlus = { onYearChange((year + 1).coerceAtMost(2035)) },
                modifier = Modifier.weight(1.3f),
                textColor = textColor
            )

            // Month Selector
            StepperBox(
                value = "${month}月",
                onMinus = { onMonthChange(if (month <= 1) 12 else month - 1) },
                onPlus = { onMonthChange(if (month >= 12) 1 else month + 1) },
                modifier = Modifier.weight(1f),
                textColor = textColor
            )

            // Day Selector
            val maxDaysInMonth = remember(year, month) {
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                }.getActualMaximum(Calendar.DAY_OF_MONTH)
            }

            StepperBox(
                value = "${day}日",
                onMinus = { onDayChange(if (day <= 1) maxDaysInMonth else day - 1) },
                onPlus = { onDayChange(if (day >= maxDaysInMonth) 1 else day + 1) },
                modifier = Modifier.weight(1f),
                textColor = textColor
            )
        }
    }
}

@Composable
private fun StepperBox(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color
) {
    val bgConfig = LocalAppBackgroundConfig.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (bgConfig.isLight) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.08f))
            .border(
                1.dp,
                if (bgConfig.isLight) Color(0xFFCBD5E1) else Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "−",
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            fontSize = 15.sp,
            modifier = Modifier
                .clickable { onMinus() }
                .padding(horizontal = 4.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            text = "+",
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            fontSize = 15.sp,
            modifier = Modifier
                .clickable { onPlus() }
                .padding(horizontal = 4.dp)
        )
    }
}

// Helpers for Date Calculations
private fun getTimeRangeBounds(
    timeFilter: String,
    customStartMs: Long,
    customEndMs: Long
): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()

    return when (timeFilter) {
        "THIS_WEEK" -> {
            cal.timeInMillis = now
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.add(Calendar.DAY_OF_WEEK, 6)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            Pair(start, end)
        }
        "THIS_MONTH" -> {
            cal.timeInMillis = now
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            Pair(start, cal.timeInMillis)
        }
        "LAST_MONTH" -> {
            cal.timeInMillis = now
            cal.add(Calendar.MONTH, -1)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            Pair(start, cal.timeInMillis)
        }
        "THIS_YEAR" -> {
            cal.timeInMillis = now
            cal.set(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.MONTH, Calendar.DECEMBER)
            cal.set(Calendar.DAY_OF_MONTH, 31)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            Pair(start, cal.timeInMillis)
        }
        "CUSTOM" -> {
            val minMs = minOf(customStartMs, customEndMs)
            val maxMs = maxOf(customStartMs, customEndMs)
            val cStart = Calendar.getInstance().apply {
                timeInMillis = minMs
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val cEnd = Calendar.getInstance().apply {
                timeInMillis = maxMs
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            Pair(cStart, cEnd)
        }
        else -> Pair(0L, Long.MAX_VALUE)
    }
}

private fun getTimeRangeDisplayLabel(
    timeFilter: String,
    customStartMs: Long,
    customEndMs: Long
): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.CHINA)
    return when (timeFilter) {
        "THIS_WEEK" -> "本周收支"
        "THIS_MONTH" -> "本月收支"
        "LAST_MONTH" -> "上月收支"
        "THIS_YEAR" -> "本年度全景"
        "CUSTOM" -> "${sdf.format(Date(customStartMs))} ~ ${sdf.format(Date(customEndMs))}"
        else -> "全部历史数据"
    }
}

private fun generateDynamicTrendPoints(
    expenses: List<ExpenseEntity>,
    timeFilter: String,
    customStartMs: Long,
    customEndMs: Long
): List<TrendPoint> {
    val (startMs, endMs) = getTimeRangeBounds(timeFilter, customStartMs, customEndMs)

    when (timeFilter) {
        "THIS_WEEK" -> {
            val cal = Calendar.getInstance().apply { timeInMillis = startMs }
            val points = mutableListOf<TrendPoint>()
            val weekDayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
            val dateFmt = SimpleDateFormat("MM/dd", Locale.CHINA)

            for (i in 0 until 7) {
                val dayStart = cal.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val dayEnd = cal.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                val dayExp = expenses.filter { it.type == "EXPENSE" && it.dateTimestamp in dayStart..dayEnd }.sumOf { it.amount }
                val dayInc = expenses.filter { it.type == "INCOME" && it.dateTimestamp in dayStart..dayEnd }.sumOf { it.amount }
                val label = weekDayNames.getOrElse(i) { dateFmt.format(Date(dayStart)) }

                points.add(TrendPoint(label = label, expense = dayExp, income = dayInc, timestamp = dayStart))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return points
        }
        "THIS_MONTH", "LAST_MONTH" -> {
            val cal = Calendar.getInstance().apply { timeInMillis = startMs }
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val points = mutableListOf<TrendPoint>()

            val intervals = listOf(
                Pair(1, 5),
                Pair(6, 10),
                Pair(11, 15),
                Pair(16, 20),
                Pair(21, 25),
                Pair(26, maxDay)
            )

            for (interval in intervals) {
                val pStart = Calendar.getInstance().apply {
                    timeInMillis = startMs
                    set(Calendar.DAY_OF_MONTH, interval.first)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val pEnd = Calendar.getInstance().apply {
                    timeInMillis = startMs
                    set(Calendar.DAY_OF_MONTH, interval.second)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                val exp = expenses.filter { it.type == "EXPENSE" && it.dateTimestamp in pStart..pEnd }.sumOf { it.amount }
                val inc = expenses.filter { it.type == "INCOME" && it.dateTimestamp in pStart..pEnd }.sumOf { it.amount }
                val label = "${interval.first}-${interval.second}日"

                points.add(TrendPoint(label = label, expense = exp, income = inc, timestamp = pStart))
            }
            return points
        }
        "THIS_YEAR" -> {
            val cal = Calendar.getInstance().apply { timeInMillis = startMs }
            val points = mutableListOf<TrendPoint>()

            for (m in 0 until 12) {
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val mStart = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val mEnd = cal.timeInMillis

                val exp = expenses.filter { it.type == "EXPENSE" && it.dateTimestamp in mStart..mEnd }.sumOf { it.amount }
                val inc = expenses.filter { it.type == "INCOME" && it.dateTimestamp in mStart..mEnd }.sumOf { it.amount }
                val label = "${m + 1}月"

                points.add(TrendPoint(label = label, expense = exp, income = inc, timestamp = mStart))
            }
            return points
        }
        else -> {
            val spanDays = ((endMs - startMs) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
            val dateFormatDay = SimpleDateFormat("MM/dd", Locale.CHINA)
            val dateFormatMonth = SimpleDateFormat("yyyy/MM", Locale.CHINA)

            if (spanDays <= 14) {
                val cal = Calendar.getInstance().apply { timeInMillis = startMs }
                val points = mutableListOf<TrendPoint>()
                for (i in 0..spanDays) {
                    val dStart = cal.apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val dEnd = cal.apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis

                    val exp = expenses.filter { it.type == "EXPENSE" && it.dateTimestamp in dStart..dEnd }.sumOf { it.amount }
                    val inc = expenses.filter { it.type == "INCOME" && it.dateTimestamp in dStart..dEnd }.sumOf { it.amount }
                    points.add(TrendPoint(label = dateFormatDay.format(Date(dStart)), expense = exp, income = inc, timestamp = dStart))
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                return points
            } else {
                val numIntervals = 6
                val stepMs = (endMs - startMs) / numIntervals
                val points = mutableListOf<TrendPoint>()

                for (i in 0 until numIntervals) {
                    val iStart = startMs + i * stepMs
                    val iEnd = if (i == numIntervals - 1) endMs else startMs + (i + 1) * stepMs

                    val expSum = expenses.filter { it.type == "EXPENSE" && it.dateTimestamp in iStart..iEnd }.sumOf { it.amount }
                    val incSum = expenses.filter { it.type == "INCOME" && it.dateTimestamp in iStart..iEnd }.sumOf { it.amount }

                    val label = if (spanDays > 120) dateFormatMonth.format(Date(iStart)) else dateFormatDay.format(Date(iStart))
                    points.add(TrendPoint(label = label, expense = expSum, income = incSum, timestamp = iStart))
                }
                return points
            }
        }
    }
}

/**
 * Custom Canvas Smooth Curve Trend Chart with Dual Comparison
 */
@Composable
fun TrendLineAndBarChart(
    points: List<TrendPoint>,
    expenseColor: Color,
    incomeColor: Color,
    isLight: Boolean,
    textTertiary: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("暂无趋势数据", color = textTertiary)
        }
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingBottom = 28f
        val paddingTop = 16f
        val chartHeight = height - paddingBottom - paddingTop

        val maxVal = points.maxOf { maxOf(it.expense, it.income) }.coerceAtLeast(100.0)
        val stepX = width / (points.size.coerceAtLeast(1))

        // Draw horizontal grid guide lines
        for (i in 0..3) {
            val y = paddingTop + (chartHeight / 3f) * i
            drawLine(
                color = if (isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Draw Expense Smooth Path
        val expensePath = Path()
        val expenseFillPath = Path()

        // Draw Income Smooth Path
        val incomePath = Path()

        val expensePoints = points.mapIndexed { index, pt ->
            val x = index * stepX + (stepX / 2f)
            val y = paddingTop + chartHeight * (1f - (pt.expense / maxVal).toFloat())
            Offset(x, y)
        }

        val incomePoints = points.mapIndexed { index, pt ->
            val x = index * stepX + (stepX / 2f)
            val y = paddingTop + chartHeight * (1f - (pt.income / maxVal).toFloat())
            Offset(x, y)
        }

        // Build smooth Expense Path
        if (expensePoints.isNotEmpty()) {
            expensePath.moveTo(expensePoints[0].x, expensePoints[0].y)
            expenseFillPath.moveTo(expensePoints[0].x, height - paddingBottom)
            expenseFillPath.lineTo(expensePoints[0].x, expensePoints[0].y)

            for (i in 0 until expensePoints.size - 1) {
                val p0 = expensePoints[i]
                val p1 = expensePoints[i + 1]
                val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                expensePath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                expenseFillPath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
            }
            expenseFillPath.lineTo(expensePoints.last().x, height - paddingBottom)
            expenseFillPath.close()

            // Draw Area Gradient Fill
            drawPath(
                path = expenseFillPath,
                brush = Brush.verticalGradient(
                    listOf(expenseColor.copy(alpha = 0.25f), Color.Transparent),
                    startY = paddingTop,
                    endY = height - paddingBottom
                )
            )

            // Draw Stroke
            drawPath(
                path = expensePath,
                color = expenseColor,
                style = Stroke(width = 3.dp.toPx(), cap = GraphStrokeCap.Round)
            )
        }

        // Build smooth Income Path
        if (incomePoints.isNotEmpty()) {
            incomePath.moveTo(incomePoints[0].x, incomePoints[0].y)
            for (i in 0 until incomePoints.size - 1) {
                val p0 = incomePoints[i]
                val p1 = incomePoints[i + 1]
                val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                incomePath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
            }
            drawPath(
                path = incomePath,
                color = incomeColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = GraphStrokeCap.Round)
            )
        }

        // Draw Point indicators & X-axis Labels
        points.forEachIndexed { index, _ ->
            val ep = expensePoints[index]
            val ip = incomePoints[index]

            // Expense point dot
            drawCircle(
                color = if (isLight) Color.White else Color(0xFF0F172A),
                radius = 5.dp.toPx(),
                center = ep
            )
            drawCircle(
                color = expenseColor,
                radius = 3.5.dp.toPx(),
                center = ep
            )

            // Income point dot
            drawCircle(
                color = if (isLight) Color.White else Color(0xFF0F172A),
                radius = 4.dp.toPx(),
                center = ip
            )
            drawCircle(
                color = incomeColor,
                radius = 2.5.dp.toPx(),
                center = ip
            )
        }
    }

    // Bottom Date Labels
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        points.forEach { pt ->
            Text(
                text = pt.label,
                style = MaterialTheme.typography.labelSmall,
                color = textTertiary,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Data structure holding a category stat with an assigned distinct color
 */
data class ColoredCategoryStat(
    val stat: CategoryStat,
    val color: Color
)

/**
 * Dedicated high-contrast color palette ensuring strong visual contrast between adjacent categories
 */
val PieChartDistinctPalette = listOf(
    Color(0xFF6366F1), // 1. Deep Indigo
    Color(0xFFF59E0B), // 2. Warm Amber
    Color(0xFF10B981), // 3. Emerald Green
    Color(0xFFEC4899), // 4. Rose Pink
    Color(0xFF06B6D4), // 5. Ocean Cyan
    Color(0xFF8B5CF6), // 6. Violet Purple
    Color(0xFFFF6B4A), // 7. Sunset Coral
    Color(0xFF0EA5E9), // 8. Sky Blue
    Color(0xFF84CC16), // 9. Lime Green
    Color(0xFFD946EF), // 10. Magenta
    Color(0xFF0D9488), // 11. Deep Teal
    Color(0xFFF43F5E), // 12. Crimson
    Color(0xFFEAB308), // 13. Goldenrod
    Color(0xFF3B82F6)  // 14. Azure Blue
)

/**
 * Assigns distinct colors so that no two adjacent categories (including first and last) share the same color.
 */
fun assignDistinctPieColors(stats: List<CategoryStat>): List<ColoredCategoryStat> {
    if (stats.isEmpty()) return emptyList()
    val paletteSize = PieChartDistinctPalette.size
    val result = mutableListOf<ColoredCategoryStat>()

    for (i in stats.indices) {
        var colorIndex = i % paletteSize
        if (i == stats.size - 1 && i > 0 && colorIndex == 0) {
            colorIndex = 1 % paletteSize
            val prevIndex = (i - 1) % paletteSize
            if (colorIndex == prevIndex) {
                colorIndex = (colorIndex + 1) % paletteSize
            }
        }
        result.add(ColoredCategoryStat(stat = stats[i], color = PieChartDistinctPalette[colorIndex]))
    }
    return result
}

/**
 * Custom Canvas Interactive Donut / Pie Chart with on-chart category text labels and adjacent distinct colors
 */
@Composable
fun DonutPieChart(
    coloredStats: List<ColoredCategoryStat>,
    totalAmount: Double,
    typeLabel: String,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    modifier: Modifier = Modifier
) {
    if (coloredStats.isEmpty() || totalAmount <= 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("暂无分类占比数据", color = textTertiary)
        }
        return
    }

    var selectedIndex by remember(coloredStats) { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(220.dp)
        ) {
            val chartSize = size.minDimension
            val strokeWidth = 36.dp.toPx()
            val diameter = chartSize - strokeWidth - 4.dp.toPx()
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val arcRadius = diameter / 2f

            var startAngle = -90f

            // 1. Draw Arcs
            coloredStats.forEachIndexed { index, item ->
                val sweepAngle = item.stat.percentage * 360f
                val isSelected = selectedIndex == index
                val currentStroke = if (isSelected) strokeWidth + 4.dp.toPx() else strokeWidth

                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle.coerceAtLeast(1.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = currentStroke, cap = GraphStrokeCap.Butt)
                )

                // Draw subtle slice separator divider if multiple slices
                if (coloredStats.size > 1 && sweepAngle < 359f) {
                    val angleRad = Math.toRadians(startAngle.toDouble())
                    val innerR = arcRadius - currentStroke / 2f
                    val outerR = arcRadius + currentStroke / 2f
                    val x1 = centerOffset.x + (innerR * cos(angleRad)).toFloat()
                    val y1 = centerOffset.y + (innerR * sin(angleRad)).toFloat()
                    val x2 = centerOffset.x + (outerR * cos(angleRad)).toFloat()
                    val y2 = centerOffset.y + (outerR * sin(angleRad)).toFloat()
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                startAngle += sweepAngle
            }

            // 2. Draw Category & Percentage Text Labels Directly on Slices
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas

                val primaryTextPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 10.sp.toPx()
                    color = android.graphics.Color.WHITE
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    setShadowLayer(4f, 0f, 1f, android.graphics.Color.argb(220, 0, 0, 0))
                }

                val subTextPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 8.5.sp.toPx()
                    color = android.graphics.Color.argb(245, 255, 255, 255)
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    setShadowLayer(4f, 0f, 1f, android.graphics.Color.argb(220, 0, 0, 0))
                }

                var textStartAngle = -90f

                coloredStats.forEach { item ->
                    val sweepAngle = item.stat.percentage * 360f
                    val midAngle = textStartAngle + sweepAngle / 2f
                    val midAngleRad = Math.toRadians(midAngle.toDouble())

                    val labelX = centerOffset.x + (arcRadius * cos(midAngleRad)).toFloat()
                    val labelY = centerOffset.y + (arcRadius * sin(midAngleRad)).toFloat()

                    val percentInt = (item.stat.percentage * 100).toInt()
                    val percentStr = "$percentInt%"
                    val catName = if (item.stat.category.length > 4) item.stat.category.take(3) + "…" else item.stat.category

                    if (item.stat.percentage >= 0.08f) {
                        // Slices with >= 8%: display Category Name & Percentage
                        val textYOffset = (primaryTextPaint.descent() + primaryTextPaint.ascent()) / 2f
                        nativeCanvas.drawText(catName, labelX, labelY - 4.5.dp.toPx() - textYOffset, primaryTextPaint)
                        nativeCanvas.drawText(percentStr, labelX, labelY + 6.dp.toPx() - textYOffset, subTextPaint)
                    } else if (item.stat.percentage >= 0.045f) {
                        // Slices between 4.5% and 8%: display Percentage
                        val textYOffset = (primaryTextPaint.descent() + primaryTextPaint.ascent()) / 2f
                        nativeCanvas.drawText(percentStr, labelX, labelY - textYOffset, primaryTextPaint)
                    }

                    textStartAngle += sweepAngle
                }
            }
        }

        // Center Content Card (Clickable to reset or view selected category)
        val selectedItem = selectedIndex?.let { coloredStats.getOrNull(it) }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape)
                .clickable { selectedIndex = null }
                .padding(4.dp)
        ) {
            if (selectedItem != null) {
                Text(
                    text = selectedItem.stat.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = selectedItem.color,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "¥${String.format(Locale.CHINA, "%,.1f", selectedItem.stat.totalAmount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${(selectedItem.stat.percentage * 100).toInt()}% (${selectedItem.stat.count}笔)",
                    style = MaterialTheme.typography.labelSmall,
                    color = textTertiary,
                    fontSize = 9.5.sp
                )
            } else {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "¥${String.format(Locale.CHINA, "%,.1f", totalAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${coloredStats.size} 个分类",
                    style = MaterialTheme.typography.labelSmall,
                    color = textTertiary,
                    fontSize = 9.5.sp
                )
            }
        }
    }
}
