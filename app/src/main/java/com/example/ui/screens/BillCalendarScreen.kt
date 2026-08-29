package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountEntity
import com.example.data.local.ExpenseEntity
import com.example.ui.components.DaySummary
import com.example.ui.screens.EditorialExpenseAddEditScreen
import com.example.ui.components.GlassBackgroundWithGlow
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowAmber
import com.example.ui.components.GlowCyan
import com.example.ui.theme.LocalAppBackgroundConfig
import com.example.ui.theme.LocalAppColorScheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun BillCalendarScreen(
    allExpenses: List<ExpenseEntity>,
    accounts: List<AccountEntity>,
    onAddExpense: (type: String, category: String, subCategory: String, amount: Double, note: String, accountId: Long, accountName: String, timestamp: Long, transferToAccountId: Long?) -> Unit,
    onUpdateExpense: (ExpenseEntity, ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept hardware / gesture back navigation to return home
    BackHandler(enabled = true) {
        onBack()
    }
    val bgConfig = LocalAppBackgroundConfig.current
    val colorScheme = LocalAppColorScheme.current

    val nowCal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(nowCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(nowCal.get(Calendar.MONTH) + 1) } // 1-12
    var selectedDayNumber by remember { mutableIntStateOf(nowCal.get(Calendar.DAY_OF_MONTH)) }

    // Dialog state for adding or editing
    var showAddDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }

    // Calculate month data and daily totals
    val (monthDaysData, monthTotalExpense, monthTotalIncome) = remember(allExpenses, selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, selectedYear)
        cal.set(Calendar.MONTH, selectedMonth - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val startOffset = (firstDayOfWeek + 5) % 7 // Monday based (0=Mon...6=Sun)

        val dailyMap = mutableMapOf<Int, Pair<Double, Double>>()
        var mExpense = 0.0
        var mIncome = 0.0

        allExpenses.forEach { exp ->
            val expCal = Calendar.getInstance().apply { timeInMillis = exp.dateTimestamp }
            if (expCal.get(Calendar.YEAR) == selectedYear && (expCal.get(Calendar.MONTH) + 1) == selectedMonth) {
                val d = expCal.get(Calendar.DAY_OF_MONTH)
                val current = dailyMap[d] ?: Pair(0.0, 0.0)
                if (exp.type == "INCOME") {
                    dailyMap[d] = Pair(current.first + exp.amount, current.second)
                    mIncome += exp.amount
                } else {
                    dailyMap[d] = Pair(current.first, current.second + exp.amount)
                    mExpense += exp.amount
                }
            }
        }

        val list = mutableListOf<DaySummary?>()
        for (i in 0 until startOffset) {
            list.add(null)
        }

        val todayCal = Calendar.getInstance()
        val isCurrentYearMonth = (todayCal.get(Calendar.YEAR) == selectedYear && (todayCal.get(Calendar.MONTH) + 1) == selectedMonth)
        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

        for (d in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, d)
            val sums = dailyMap[d] ?: Pair(0.0, 0.0)
            list.add(
                DaySummary(
                    dayNumber = d,
                    dateTimestamp = cal.timeInMillis,
                    income = sums.first,
                    expense = sums.second,
                    isToday = isCurrentYearMonth && (d == todayDay),
                    isCurrentMonth = true
                )
            )
        }

        Triple(list, mExpense, mIncome)
    }

    // Filter transactions for the selected day
    val selectedDayExpenses = remember(allExpenses, selectedYear, selectedMonth, selectedDayNumber) {
        allExpenses.filter { exp ->
            val expCal = Calendar.getInstance().apply { timeInMillis = exp.dateTimestamp }
            expCal.get(Calendar.YEAR) == selectedYear &&
            (expCal.get(Calendar.MONTH) + 1) == selectedMonth &&
            expCal.get(Calendar.DAY_OF_MONTH) == selectedDayNumber
        }.sortedByDescending { it.dateTimestamp }
    }

    val selectedDayExpenseTotal = selectedDayExpenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val selectedDayIncomeTotal = selectedDayExpenses.filter { it.type == "INCOME" }.sumOf { it.amount }

    GlassBackgroundWithGlow(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Top Header / Navigation Bar
                item {
                    Spacer(modifier = Modifier.statusBarsPadding())
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassCard(
                                shape = CircleShape,
                                backgroundColor = if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.20f),
                                borderColor = Brush.linearGradient(
                                    listOf(
                                        Color(0xFF818CF8).copy(alpha = 0.6f),
                                        Color.White.copy(alpha = 0.15f)
                                    )
                                ),
                                onClick = onBack,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("bill_calendar_back_button")
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回",
                                        tint = if (bgConfig.isLight) Color(0xFF4F46E5) else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = "账单日历",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = bgConfig.textPrimary
                            )
                        }

                        // Today Quick Reset Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.10f) else Color(0xFF6366F1).copy(alpha = 0.25f)
                                )
                                .border(
                                    1.dp,
                                    (if (bgConfig.isLight) Color(0xFF818CF8) else GlowCyan).copy(alpha = 0.35f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    val tCal = Calendar.getInstance()
                                    selectedYear = tCal.get(Calendar.YEAR)
                                    selectedMonth = tCal.get(Calendar.MONTH) + 1
                                    selectedDayNumber = tCal.get(Calendar.DAY_OF_MONTH)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                tint = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "回到今日",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan
                            )
                        }
                    }
                }

                // Month Switcher Navigator & Month Overview Card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = if (bgConfig.isLight) Color.White.copy(alpha = 0.95f) else Color(0xFF131C35).copy(alpha = 0.65f),
                        borderColor = Brush.linearGradient(
                            listOf(
                                (if (bgConfig.isLight) Color(0xFF818CF8) else GlowCyan).copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Month Selector Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (selectedMonth == 1) {
                                            selectedYear -= 1
                                            selectedMonth = 12
                                        } else {
                                            selectedMonth -= 1
                                        }
                                        selectedDayNumber = 1
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBackIosNew,
                                        contentDescription = "上一月",
                                        tint = bgConfig.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = String.format(Locale.CHINA, "%d年%02d月", selectedYear, selectedMonth),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = bgConfig.textPrimary
                                )

                                IconButton(
                                    onClick = {
                                        if (selectedMonth == 12) {
                                            selectedYear += 1
                                            selectedMonth = 1
                                        } else {
                                            selectedMonth += 1
                                        }
                                        selectedDayNumber = 1
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = "下一月",
                                        tint = bgConfig.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 3 Month Metrics: Expense, Income, Balance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "本月支出",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.expenseText.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "¥${String.format(Locale.CHINA, "%,.2f", monthTotalExpense)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.expenseColor
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "本月收入",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.incomeText.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "¥${String.format(Locale.CHINA, "%,.2f", monthTotalIncome)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.incomeColor
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "本月结余",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = bgConfig.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "¥${String.format(Locale.CHINA, "%,.2f", monthTotalIncome - monthTotalExpense)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = bgConfig.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Month Calendar Grid Card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = if (bgConfig.isLight) Color.White.copy(alpha = 0.95f) else Color(0xFF131C33).copy(alpha = 0.65f),
                        borderColor = Brush.linearGradient(
                            listOf(
                                (if (bgConfig.isLight) Color(0xFF818CF8) else GlowCyan).copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // Weekday Titles
                            val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                weekDays.forEachIndexed { index, dayName ->
                                    val isWeekend = (index == 5 || index == 6)
                                    Text(
                                        text = dayName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isWeekend) (if (bgConfig.isLight) Color(0xFFEA580C) else GlowAmber) else bgConfig.textTertiary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Grid of days
                            val chunkedDays = monthDaysData.chunked(7)
                            chunkedDays.forEach { rowDays ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.5.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    for (i in 0 until 7) {
                                        val dayItem = rowDays.getOrNull(i)
                                        if (dayItem == null) {
                                            Box(modifier = Modifier.weight(1f))
                                        } else {
                                            val isSelected = selectedDayNumber == dayItem.dayNumber
                                            val hasIncome = dayItem.income > 0
                                            val hasExpense = dayItem.expense > 0

                                            val cellBg = when {
                                                isSelected -> (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan).copy(alpha = 0.22f)
                                                dayItem.isToday -> (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan).copy(alpha = 0.08f)
                                                else -> Color.Transparent
                                            }

                                            val cellBorderColor = when {
                                                isSelected -> (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan)
                                                dayItem.isToday -> (if (bgConfig.isLight) Color(0xFF818CF8) else GlowCyan).copy(alpha = 0.4f)
                                                else -> Color.Transparent
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 1.dp)
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .background(cellBg)
                                                    .border(1.dp, cellBorderColor, RoundedCornerShape(7.dp))
                                                    .clickable { selectedDayNumber = dayItem.dayNumber }
                                                    .padding(vertical = 2.5.dp, horizontal = 1.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                // Date circle badge
                                                Box(
                                                    modifier = Modifier
                                                        .size(19.dp)
                                                        .background(
                                                            if (dayItem.isToday) (if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan) else Color.Transparent,
                                                            CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = dayItem.dayNumber.toString(),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.5.sp,
                                                            fontWeight = if (dayItem.isToday || isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                                        ),
                                                        color = if (dayItem.isToday) Color.White else (if (isSelected) (if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan) else bgConfig.textPrimary)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(1.dp))

                                                // Income line (Row 1)
                                                Text(
                                                    text = if (hasIncome) formatCalendarAmount(dayItem.income) else "-",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 8.sp,
                                                        fontWeight = if (hasIncome) FontWeight.Bold else FontWeight.Normal,
                                                        lineHeight = 10.sp
                                                    ),
                                                    color = if (hasIncome) colorScheme.incomeColor else bgConfig.textTertiary.copy(alpha = 0.22f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )

                                                // Expense line (Row 2)
                                                Text(
                                                    text = if (hasExpense) formatCalendarAmount(dayItem.expense) else "-",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 8.sp,
                                                        fontWeight = if (hasExpense) FontWeight.Bold else FontWeight.Normal,
                                                        lineHeight = 10.sp
                                                    ),
                                                    color = if (hasExpense) colorScheme.expenseColor else bgConfig.textTertiary.copy(alpha = 0.22f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Day Transaction Details Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${selectedMonth}月${selectedDayNumber}日 账目清单",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = bgConfig.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(共 ${selectedDayExpenses.size} 笔)",
                                style = MaterialTheme.typography.labelSmall,
                                color = bgConfig.textTertiary
                            )
                        }

                        // Day balance summary tags
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectedDayExpenseTotal > 0) {
                                Text(
                                    text = "支 ¥${String.format(Locale.CHINA, "%.2f", selectedDayExpenseTotal)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.expenseColor
                                )
                            }
                            if (selectedDayIncomeTotal > 0) {
                                Text(
                                    text = "收 ¥${String.format(Locale.CHINA, "%.2f", selectedDayIncomeTotal)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.incomeColor
                                )
                            }
                        }
                    }
                }

                // Selected Day Transactions List
                if (selectedDayExpenses.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background((if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan).copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "当日暂无账目记录",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = bgConfig.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "点击下方“记一笔”快速添加此日账单",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = bgConfig.textTertiary
                                )
                            }
                        }
                    }
                } else {
                    items(selectedDayExpenses, key = { it.id }) { expense ->
                        val isExpense = expense.type == "EXPENSE"
                        val sdfTime = SimpleDateFormat("HH:mm", Locale.CHINA)
                        val timeStr = sdfTime.format(Date(expense.dateTimestamp))

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expenseToEdit = expense },
                            shape = RoundedCornerShape(18.dp),
                            backgroundColor = if (bgConfig.isLight) Color.White.copy(alpha = 0.92f) else Color(0xFF161F38).copy(alpha = 0.60f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Category Pill
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(
                                                if (isExpense) colorScheme.expenseContainer else colorScheme.incomeContainer,
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isExpense) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (isExpense) colorScheme.expenseColor else colorScheme.incomeColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = expense.category,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = bgConfig.textPrimary
                                            )
                                            if (expense.subCategory.isNotBlank() && expense.subCategory != "默认") {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "· ${expense.subCategory}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = bgConfig.textSecondary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = timeStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = bgConfig.textTertiary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = expense.accountName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = bgConfig.textSecondary
                                            )
                                            if (expense.note.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "“${expense.note}”",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = bgConfig.textTertiary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${if (isExpense) "-" else "+"}¥${String.format(Locale.CHINA, "%.2f", expense.amount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isExpense) colorScheme.expenseColor else colorScheme.incomeColor
                                    )

                                    IconButton(
                                        onClick = { onDeleteExpense(expense) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "删除",
                                            tint = bgConfig.textTertiary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dedicated Floating "记一笔" Action Button (账单日历记账入口)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                        contentColor = if (bgConfig.isLight) Color.White else Color(0xFF0F172A)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("bill_calendar_add_expense_fab")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "记一笔",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "记一笔 (${selectedMonth}月${selectedDayNumber}日)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Expense Dialog (Preset with selected calendar date)
    if (showAddDialog) {
        val presetTimestamp = remember(selectedYear, selectedMonth, selectedDayNumber) {
            val c = Calendar.getInstance()
            c.set(Calendar.YEAR, selectedYear)
            c.set(Calendar.MONTH, selectedMonth - 1)
            c.set(Calendar.DAY_OF_MONTH, selectedDayNumber)
            c.timeInMillis
        }

        EditorialExpenseAddEditScreen(
            expenseToEdit = null,
            allExpenses = allExpenses,
            accounts = accounts,
            initialTimestamp = presetTimestamp,
            isPreviewMode = false,
            onDismiss = { showAddDialog = false },
            onConfirm = { type, category, subCategory, amount, note, accountId, accountName, timestamp, transferToAccountId ->
                onAddExpense(type, category, subCategory, amount, note, accountId, accountName, timestamp, transferToAccountId)
                showAddDialog = false
            }
        )
    }

    if (expenseToEdit != null) {
        EditorialExpenseAddEditScreen(
            expenseToEdit = expenseToEdit,
            allExpenses = allExpenses,
            accounts = accounts,
            initialTimestamp = expenseToEdit!!.dateTimestamp,
            isPreviewMode = false,
            onDismiss = { expenseToEdit = null },
            onConfirm = { type, category, subCategory, amount, note, accountId, accountName, timestamp, transferToAccountId ->
                onUpdateExpense(
                    expenseToEdit!!,
                    expenseToEdit!!.copy(
                        type = type,
                        category = category,
                        subCategory = subCategory,
                        amount = amount,
                        note = note,
                        accountId = accountId,
                        accountName = accountName,
                        dateTimestamp = timestamp,
                        transferToAccountId = transferToAccountId ?: 0L
                    )
                )
                expenseToEdit = null
            }
        )
    }
}

private fun formatCalendarAmount(amount: Double): String {
    return String.format(Locale.CHINA, "%.2f", amount)
}
