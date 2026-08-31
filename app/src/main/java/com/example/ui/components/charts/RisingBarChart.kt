package com.example.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 星期消费项
 */
data class WeekdaySpendingItem(
    val dayName: String, // "周一", "周二", ...
    val amount: Double,
    val isWeekend: Boolean
)

/**
 * 月度收支双柱数据项
 */
data class MonthlyCompareItem(
    val monthLabel: String, // "24.03", "24.04", ...
    val income: Double,
    val expense: Double,
    val isCurrentMonth: Boolean
)

/**
 * 消费习惯 —— 星期分布柱状图
 */
@Composable
fun WeekdaySpendingBarChart(
    items: List<WeekdaySpendingItem>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 140.dp,
    forestGreen: Color = Color(0xFF2D6A4F),
    clayAccent: Color = Color(0xFFC4623D),
    mutedTextColor: Color = Color(0xFFA8A29E),
    gridLineColor: Color = Color(0xFFE4DFD3)
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current

    // 为 7 根柱子各创建一个 Animatable
    val animatables = remember(items) {
        List(items.size) { Animatable(0f) }
    }

    LaunchedEffect(items) {
        animatables.forEachIndexed { index, animatable ->
            launch {
                delay(index * 90L) // 错开 90ms 启动 (放慢 1.5 倍)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 825, easing = FastOutSlowInEasing) // 550 * 1.5 = 825ms
                )
            }
        }
    }

    val maxAmount = remember(items) { (items.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(10.0) }
    val avgAmount = remember(items) { if (items.isNotEmpty()) items.sumOf { it.amount } / items.size else 0.0 }
    val highestDayIndex = remember(items) {
        val maxVal = items.maxOfOrNull { it.amount } ?: 0.0
        if (maxVal > 0) items.indexOfFirst { it.amount == maxVal } else -1
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
    ) {
        val w = size.width
        val h = size.height
        val labelAreaHeight = 22.dp.toPx()
        val topAreaPadding = 24.dp.toPx()
        val usableHeight = h - topAreaPadding - labelAreaHeight
        val baselineY = topAreaPadding + usableHeight

        val count = items.size
        val colWidth = w / count
        val barWidth = 14.dp.toPx()

        // 1. 绘制“周平均消费”虚线基准线
        if (avgAmount > 0) {
            val avgY = baselineY - (avgAmount.toFloat() / maxAmount.toFloat() * usableHeight)
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f)

            drawLine(
                color = forestGreen.copy(alpha = 0.5f),
                start = Offset(0f, avgY),
                end = Offset(w - 56.dp.toPx(), avgY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = pathEffect
            )

            // 右侧周均标注文字
            val avgPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                textSize = with(density) { 8.5.sp.toPx() }
                color = forestGreen.toArgb()
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
                textAlign = android.graphics.Paint.Align.RIGHT
            }

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    "周均 ¥%.0f".format(avgAmount),
                    w,
                    avgY + 3.dp.toPx(),
                    avgPaint
                )
            }
        }

        // 2. 绘制 7 根柱子与星期标签
        val labelPaintNormal = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = with(density) { 9.sp.toPx() }
            color = mutedTextColor.toArgb()
            typeface = android.graphics.Typeface.DEFAULT
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val labelPaintHighlight = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = with(density) { 9.5.sp.toPx() }
            color = forestGreen.toArgb()
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val maxValPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = with(density) { 9.5.sp.toPx() }
            color = forestGreen.toArgb()
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
        }

        items.forEachIndexed { index, item ->
            val centerX = colWidth * index + colWidth / 2f
            val targetBarHeight = (item.amount.toFloat() / maxAmount.toFloat() * usableHeight)
            val currentProgress = animatables.getOrNull(index)?.value ?: 1f
            val currentBarHeight = targetBarHeight * currentProgress
            val barTop = baselineY - currentBarHeight

            // 工作日浅墨绿 45% 透明度，周末墨绿 100%
            val barColor = if (item.isWeekend) {
                forestGreen
            } else {
                forestGreen.copy(alpha = 0.45f)
            }

            // 绘制圆角柱子
            drawRoundRect(
                color = barColor,
                topLeft = Offset(centerX - barWidth / 2f, barTop),
                size = Size(barWidth, currentBarHeight.coerceAtLeast(1f)),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )

            // 最高天标注具体金额
            if (index == highestDayIndex && item.amount > 0 && currentProgress > 0.8f) {
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        "¥%.0f".format(item.amount),
                        centerX,
                        barTop - 6.dp.toPx(),
                        maxValPaint
                    )
                }
            }

            // 星期标签
            val isHighlight = index == highestDayIndex
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    item.dayName,
                    centerX,
                    baselineY + 15.dp.toPx(),
                    if (isHighlight) labelPaintHighlight else labelPaintNormal
                )
            }
        }
    }
}

/**
 * 收支对比 —— 近 6 个月分组双柱状图
 *
 * 核心特性：
 * 支出和收入配色对换：收入用陶红 (ClayAccent)，支出用墨绿 (ForestGreen)
 */
@Composable
fun MonthlyIncomeExpenseBarChart(
    items: List<MonthlyCompareItem>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 150.dp,
    incomeColor: Color = Color(0xFFC4623D),  // 收入：陶红
    expenseColor: Color = Color(0xFF2D6A4F), // 支出：墨绿
    mutedTextColor: Color = Color(0xFFA8A29E)
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current

    // 每个月份包含 2 根柱子的动画控制器 (incomeAnim, expenseAnim)
    val incomeAnims = remember(items) { List(items.size) { Animatable(0f) } }
    val expenseAnims = remember(items) { List(items.size) { Animatable(0f) } }

    LaunchedEffect(items) {
        items.indices.forEach { index ->
            launch {
                delay(index * 90L) // 月份之间错开 90ms (放慢 1.5 倍)
                incomeAnims[index].animateTo(1f, tween(825, easing = FastOutSlowInEasing)) // 550 * 1.5 = 825ms
            }
            launch {
                delay(index * 90L + 60L) // 收入与支出之间错开 60ms (40 * 1.5 = 60ms)
                expenseAnims[index].animateTo(1f, tween(825, easing = FastOutSlowInEasing)) // 550 * 1.5 = 825ms
            }
        }
    }

    val maxVal = remember(items) {
        val highest = items.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0
        highest.coerceAtLeast(10.0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            val w = size.width
            val h = size.height
            val labelAreaHeight = 22.dp.toPx()
            val usableHeight = h - labelAreaHeight
            val baselineY = usableHeight

            val count = items.size
            val groupWidth = w / count
            val singleBarWidth = 10.dp.toPx()
            val innerGap = 3.dp.toPx()

            val textPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                textSize = with(density) { 8.5.sp.toPx() }
                typeface = android.graphics.Typeface.MONOSPACE
                textAlign = android.graphics.Paint.Align.CENTER
            }

            // 绘制底部分隔基准线
            drawLine(
                color = mutedTextColor.copy(alpha = 0.2f),
                start = Offset(0f, baselineY),
                end = Offset(w, baselineY),
                strokeWidth = 0.8.dp.toPx()
            )

            items.forEachIndexed { index, item ->
                val groupCenterX = groupWidth * index + groupWidth / 2f
                val isCurrent = item.isCurrentMonth
                val groupAlpha = if (isCurrent) 1f else 0.45f

                // 1. 绘制收入柱（左，陶红）
                val incomeProgress = incomeAnims.getOrNull(index)?.value ?: 1f
                val incomeHeight = (item.income.toFloat() / maxVal.toFloat() * usableHeight) * incomeProgress
                val incomeX = groupCenterX - singleBarWidth - innerGap / 2f
                val incomeY = baselineY - incomeHeight

                drawRoundRect(
                    color = incomeColor.copy(alpha = groupAlpha),
                    topLeft = Offset(incomeX, incomeY),
                    size = Size(singleBarWidth, incomeHeight.coerceAtLeast(1f)),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )

                // 2. 绘制支出柱（右，墨绿）
                val expenseProgress = expenseAnims.getOrNull(index)?.value ?: 1f
                val expenseHeight = (item.expense.toFloat() / maxVal.toFloat() * usableHeight) * expenseProgress
                val expenseX = groupCenterX + innerGap / 2f
                val expenseY = baselineY - expenseHeight

                drawRoundRect(
                    color = expenseColor.copy(alpha = groupAlpha),
                    topLeft = Offset(expenseX, expenseY),
                    size = Size(singleBarWidth, expenseHeight.coerceAtLeast(1f)),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )

                // 3. 绘制月份标签
                textPaint.color = if (isCurrent) expenseColor.toArgb() else mutedTextColor.toArgb()
                if (isCurrent) {
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                } else {
                    textPaint.typeface = android.graphics.Typeface.MONOSPACE
                }

                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        item.monthLabel,
                        groupCenterX,
                        baselineY + 15.dp.toPx(),
                        textPaint
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 极简图例：收入（陶红）与支出（墨绿）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(incomeColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "收入",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = mutedTextColor
            )

            Spacer(modifier = Modifier.width(20.dp))

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(expenseColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "支出",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = mutedTextColor
            )
        }
    }
}
