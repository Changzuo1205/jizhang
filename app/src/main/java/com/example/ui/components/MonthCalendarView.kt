package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExpenseEntity
import com.example.ui.theme.LocalAppBackgroundConfig
import com.example.ui.theme.LocalAppColorScheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DaySummary(
    val dayNumber: Int,
    val dateTimestamp: Long,
    val income: Double,
    val expense: Double,
    val isToday: Boolean,
    val isCurrentMonth: Boolean
)

@Composable
fun MonthCalendarView(
    expenses: List<ExpenseEntity>,
    selectedDay: Int? = null,
    onSelectDay: (Int?) -> Unit,
    onOpenBillCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val colorScheme = LocalAppColorScheme.current

    val currentCal = remember { Calendar.getInstance() }
    val currentYear = currentCal.get(Calendar.YEAR)
    val currentMonth = currentCal.get(Calendar.MONTH) + 1 // 1-12
    val todayDayNumber = currentCal.get(Calendar.DAY_OF_MONTH)

    // Left Top Date Display: Pure Digits (YYYY/MM)
    val numericYearMonth = remember(currentYear, currentMonth) {
        String.format(Locale.CHINA, "%d/%02d", currentYear, currentMonth)
    }

    // Compute daily income and expense for the current month
    val monthDaysData = remember(expenses, currentYear, currentMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonth - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Day of week for 1st day of month (Sunday=1, Monday=2, ... Saturday=7)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Monday based offset (0 for Mon, 1 for Tue, ... 6 for Sun)
        val startOffset = (firstDayOfWeek + 5) % 7

        val dailyMap = mutableMapOf<Int, Pair<Double, Double>>() // day -> (income, expense)

        expenses.forEach { exp ->
            val expCal = Calendar.getInstance().apply { timeInMillis = exp.dateTimestamp }
            if (expCal.get(Calendar.YEAR) == currentYear && (expCal.get(Calendar.MONTH) + 1) == currentMonth) {
                val d = expCal.get(Calendar.DAY_OF_MONTH)
                val current = dailyMap[d] ?: Pair(0.0, 0.0)
                if (exp.type == "INCOME") {
                    dailyMap[d] = Pair(current.first + exp.amount, current.second)
                } else {
                    dailyMap[d] = Pair(current.first, current.second + exp.amount)
                }
            }
        }

        val list = mutableListOf<DaySummary?>()
        // Leading blank slots
        for (i in 0 until startOffset) {
            list.add(null)
        }

        // Days of current month
        for (d in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, d)
            val sums = dailyMap[d] ?: Pair(0.0, 0.0)
            list.add(
                DaySummary(
                    dayNumber = d,
                    dateTimestamp = cal.timeInMillis,
                    income = sums.first,
                    expense = sums.second,
                    isToday = (d == todayDayNumber),
                    isCurrentMonth = true
                )
            )
        }

        list
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = if (bgConfig.isLight) Color.White.copy(alpha = 0.92f) else Color(0xFF131C33).copy(alpha = 0.65f),
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
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Header Row: Top-Left Pure Digits Date (YYYY/MM) + Top-Right Bill Calendar Entrance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left-top: Pure Digits (Year/Month e.g. 2026/08)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan, CircleShape)
                    )
                    Text(
                        text = numericYearMonth,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = bgConfig.textPrimary
                    )
                }

                // Right-top: Bill Calendar Entry Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.28f)
                        )
                        .border(
                            1.dp,
                            (if (bgConfig.isLight) Color(0xFF818CF8) else GlowCyan).copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onOpenBillCalendar() }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "账单日历",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Weekday Column Titles (一 二 三 四 五 六 日)
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

            // Month Days Grid (Chunks of 7 days per row)
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
                            val isSelected = selectedDay == dayItem.dayNumber
                            CalendarDayCell(
                                day = dayItem,
                                isSelected = isSelected,
                                onClick = {
                                    if (selectedDay == dayItem.dayNumber) {
                                        onSelectDay(null)
                                    } else {
                                        onSelectDay(dayItem.dayNumber)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Optional filter hint when a specific date is selected
            if (selectedDay != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan).copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已筛选 ${currentMonth}月${selectedDay}日 账目记录",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "清除筛选",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = bgConfig.textSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onSelectDay(null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: DaySummary,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val colorScheme = LocalAppColorScheme.current

    val hasIncome = day.income > 0
    val hasExpense = day.expense > 0

    val cellBg = when {
        isSelected -> (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan).copy(alpha = 0.22f)
        day.isToday -> (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan).copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    val cellBorderColor = when {
        isSelected -> (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan)
        day.isToday -> (if (bgConfig.isLight) Color(0xFF818CF8) else GlowCyan).copy(alpha = 0.4f)
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(cellBg)
            .border(1.dp, cellBorderColor, RoundedCornerShape(7.dp))
            .clickable { onClick() }
            .padding(vertical = 2.5.dp, horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Date Number Badge
        Box(
            modifier = Modifier
                .size(19.dp)
                .background(
                    if (day.isToday) (if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan) else Color.Transparent,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.dayNumber.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = if (day.isToday || isSelected) FontWeight.ExtraBold else FontWeight.Medium
                ),
                color = if (day.isToday) Color.White else (if (isSelected) (if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan) else bgConfig.textPrimary)
            )
        }

        Spacer(modifier = Modifier.height(1.dp))

        // Daily Income / Expense
        Text(
            text = if (hasIncome) formatShortAmount(day.income) else "-",
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

        Text(
            text = if (hasExpense) formatShortAmount(day.expense) else "-",
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

/**
 * Format amounts cleanly for compact calendar display with 2 decimal places
 */
private fun formatShortAmount(amount: Double): String {
    return String.format(Locale.CHINA, "%.2f", amount)
}
