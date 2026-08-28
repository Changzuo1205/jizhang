package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LocalAppBackgroundConfig
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class DateItem(
    val dayOffset: Int,
    val displayText: String,
    val year: Int,
    val month: Int, // 1..12
    val day: Int
)

@Composable
fun WheelTimePickerSheet(
    initialTimestamp: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    accentColor: Color = Color(0xFFF97316)
) {
    val bgConfig = LocalAppBackgroundConfig.current

    // Segment tab: 0 = "时间" (相对日期 + 时 + 分), 1 = "日期" (年 + 月 + 日)
    var selectedTab by remember { mutableIntStateOf(0) }

    val initialCal = remember(initialTimestamp) {
        Calendar.getInstance().apply { timeInMillis = initialTimestamp }
    }

    var selectedYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH) + 1) } // 1..12
    var selectedDay by remember { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }

    // Generate date list spanning -730 days to +365 days for "时间" mode
    val dateList = remember {
        val list = mutableListOf<DateItem>()
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sdfOther = SimpleDateFormat("M月d日 E", Locale.CHINA)

        for (offset in -730..365) {
            val c = Calendar.getInstance().apply {
                timeInMillis = today.timeInMillis
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val text = when (offset) {
                0 -> "今天"
                -1 -> "昨天"
                -2 -> "前天"
                1 -> "明天"
                else -> sdfOther.format(c.time)
            }
            list.add(
                DateItem(
                    dayOffset = offset,
                    displayText = text,
                    year = c.get(Calendar.YEAR),
                    month = c.get(Calendar.MONTH) + 1,
                    day = c.get(Calendar.DAY_OF_MONTH)
                )
            )
        }
        list
    }

    // Find date index corresponding to (selectedYear, selectedMonth, selectedDay)
    val currentDateIndex = remember(selectedYear, selectedMonth, selectedDay, dateList) {
        val idx = dateList.indexOfFirst {
            it.year == selectedYear && it.month == selectedMonth && it.day == selectedDay
        }
        if (idx >= 0) idx else dateList.indexOfFirst { it.dayOffset == 0 }.coerceAtLeast(0)
    }

    // Years: currentYear - 15 .. currentYear + 10
    val startYear = remember { Calendar.getInstance().get(Calendar.YEAR) - 15 }
    val years = remember { (startYear..(startYear + 25)).toList() }
    val yearStrings = remember(years) { years.map { "${it}年" } }

    val months = remember { (1..12).toList() }
    val monthStrings = remember { months.map { "${it}月" } }

    // Dynamic days count in selected year & month
    val maxDaysInMonth = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val dayStrings = remember(maxDaysInMonth) { (1..maxDaysInMonth).map { "${it}日" } }

    val hours = remember { (0..23).map { String.format(Locale.US, "%02d", it) } }
    val minutes = remember { (0..59).map { String.format(Locale.US, "%02d", it) } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (bgConfig.isLight) Color.White else Color(0xFF1E222B),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with dismiss arrow and centered segmented switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "收起",
                            tint = bgConfig.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Segmented Tab [ 时间 | 日期 ]
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (bgConfig.isLight) Color(0xFFF1F5F9) else Color(0xFF2C313E))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedTab == 0) (if (bgConfig.isLight) Color.White else Color(0xFF3B4252)) else Color.Transparent)
                                .clickable { selectedTab = 0 }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "时间",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) accentColor else bgConfig.textSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedTab == 1) (if (bgConfig.isLight) Color.White else Color(0xFF3B4252)) else Color.Transparent)
                                .clickable { selectedTab = 1 }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "日期",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) accentColor else bgConfig.textSecondary
                            )
                        }
                    }

                    // Placeholder to balance top bar layout
                    Spacer(modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Wheel Picker Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Center Highlight Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (bgConfig.isLight) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.08f)
                            )
                    )

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(180)).togetherWith(fadeOut(animationSpec = tween(150)))
                        },
                        label = "WheelTabTransition"
                    ) { tab ->
                        if (tab == 0) {
                            // "时间" Mode: [ 日期 (如 今天/8月28日) | 时 | 分 ]
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Date column
                                Box(modifier = Modifier.weight(1.8f)) {
                                    WheelColumn(
                                        items = dateList.map { it.displayText },
                                        selectedIndex = currentDateIndex,
                                        onSelectedIndexChange = { idx ->
                                            val item = dateList.getOrNull(idx) ?: return@WheelColumn
                                            selectedYear = item.year
                                            selectedMonth = item.month
                                            selectedDay = item.day
                                        },
                                        itemHeight = 44.dp,
                                        visibleItemCount = 5
                                    )
                                }

                                // Hour column
                                Box(modifier = Modifier.weight(1f)) {
                                    WheelColumn(
                                        items = hours,
                                        selectedIndex = selectedHour.coerceIn(0, 23),
                                        onSelectedIndexChange = { selectedHour = it },
                                        itemHeight = 44.dp,
                                        visibleItemCount = 5
                                    )
                                }

                                // Minute column
                                Box(modifier = Modifier.weight(1f)) {
                                    WheelColumn(
                                        items = minutes,
                                        selectedIndex = selectedMinute.coerceIn(0, 59),
                                        onSelectedIndexChange = { selectedMinute = it },
                                        itemHeight = 44.dp,
                                        visibleItemCount = 5
                                    )
                                }
                            }
                        } else {
                            // "日期" Mode: [ 年 | 月 | 日 ]
                            val yearIndex = (selectedYear - startYear).coerceIn(0, years.size - 1)
                            val monthIndex = (selectedMonth - 1).coerceIn(0, 11)
                            val dayIndex = (selectedDay - 1).coerceIn(0, dayStrings.size - 1)

                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Year column
                                Box(modifier = Modifier.weight(1.2f)) {
                                    WheelColumn(
                                        items = yearStrings,
                                        selectedIndex = yearIndex,
                                        onSelectedIndexChange = { idx ->
                                            val newYear = years.getOrNull(idx) ?: return@WheelColumn
                                            selectedYear = newYear
                                            // Coerce day if month has fewer days in leap year
                                            val maxD = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, newYear)
                                                set(Calendar.MONTH, selectedMonth - 1)
                                                set(Calendar.DAY_OF_MONTH, 1)
                                            }.getActualMaximum(Calendar.DAY_OF_MONTH)
                                            if (selectedDay > maxD) selectedDay = maxD
                                        },
                                        itemHeight = 44.dp,
                                        visibleItemCount = 5
                                    )
                                }

                                // Month column
                                Box(modifier = Modifier.weight(1f)) {
                                    WheelColumn(
                                        items = monthStrings,
                                        selectedIndex = monthIndex,
                                        onSelectedIndexChange = { idx ->
                                            val newMonth = idx + 1
                                            selectedMonth = newMonth
                                            val maxD = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, selectedYear)
                                                set(Calendar.MONTH, newMonth - 1)
                                                set(Calendar.DAY_OF_MONTH, 1)
                                            }.getActualMaximum(Calendar.DAY_OF_MONTH)
                                            if (selectedDay > maxD) selectedDay = maxD
                                        },
                                        itemHeight = 44.dp,
                                        visibleItemCount = 5
                                    )
                                }

                                // Day column
                                Box(modifier = Modifier.weight(1f)) {
                                    WheelColumn(
                                        items = dayStrings,
                                        selectedIndex = dayIndex,
                                        onSelectedIndexChange = { idx ->
                                            selectedDay = idx + 1
                                        },
                                        itemHeight = 44.dp,
                                        visibleItemCount = 5
                                    )
                                }
                            }
                        }
                    }

                    // Top & bottom gradient fades for smooth optical wheel illusion
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            if (bgConfig.isLight) Color.White else Color(0xFF1E222B),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            if (bgConfig.isLight) Color.White else Color(0xFF1E222B)
                                        )
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Button
                Button(
                    onClick = {
                        val finalCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, selectedYear)
                            set(Calendar.MONTH, selectedMonth - 1)
                            set(Calendar.DAY_OF_MONTH, selectedDay.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
                            set(Calendar.HOUR_OF_DAY, selectedHour)
                            set(Calendar.MINUTE, selectedMinute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onConfirm(finalCal.timeInMillis)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = "确 定",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    itemHeight: Dp = 44.dp,
    visibleItemCount: Int = 5
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val coroutineScope = rememberCoroutineScope()
    val paddingCount = visibleItemCount / 2 // 2

    // When list item k is at center slot:
    // With padding items at indices 0 and 1, items[k] is at index 2 + k.
    // When firstVisibleItemIndex == k and offset == 0, items[k] is exactly at slot 2 (center).
    val initialTarget = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialTarget)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Calculate current center index
    val currentCenterIndex by remember(items.size) {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val densityOffset = if (offset > 45) 1 else 0
            (firstVisible + densityOffset).coerceIn(0, (items.size - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(currentCenterIndex) {
        if (currentCenterIndex in items.indices && currentCenterIndex != selectedIndex) {
            onSelectedIndexChange(currentCenterIndex)
        }
    }

    LaunchedEffect(selectedIndex) {
        if (listState.firstVisibleItemIndex != selectedIndex && !listState.isScrollInProgress) {
            listState.scrollToItem(selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight * visibleItemCount),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top empty padding items
        items(paddingCount) {
            Spacer(modifier = Modifier.height(itemHeight))
        }

        items(items.size) { index ->
            val isSelected = index == currentCenterIndex
            val distance = abs(index - currentCenterIndex)
            val alpha = (1f - (distance * 0.35f)).coerceIn(0.25f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable {
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[index],
                    fontSize = if (isSelected) 17.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) bgConfig.textPrimary else bgConfig.textSecondary,
                    modifier = Modifier.alpha(alpha),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom empty padding items
        items(paddingCount) {
            Spacer(modifier = Modifier.height(itemHeight))
        }
    }
}

