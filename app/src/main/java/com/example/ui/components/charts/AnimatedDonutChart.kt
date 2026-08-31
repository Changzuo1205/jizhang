package com.example.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * 构成项数据实体
 */
data class DonutSliceData(
    val id: String,
    val name: String,
    val amount: Double,
    val percentage: Float, // 0f ~ 100f
    val color: Color
)

/**
 * 极简手账美学马克笔逐段展开环形图组件
 *
 * 核心绘制与动画逻辑：
 * 1. drawArc 分段绘制，起始角度固定 -90f（12 点钟方向），顺时针连续扫描
 * 2. 两部分/各扇区分段之间完全无留白间隙 (gapAngle = 0f)，使用 StrokeCap.Butt 紧密贴合
 * 3. 动画：1300ms LinearEasing，通过全局进度 progress (0..1) 计算各扇区的出现时间窗，
 *    实现像“马克笔一笔画圆”般连续顺时针扫描绘制，而非独立淡入
 * 4. 环心实时叠加显示指标标签与当前选中分类（或总计）金额
 * 5. 点击图例时切换高亮，其余圆弧降低透明度至 0.25f
 */
@Composable
fun AnimatedDonutChart(
    slices: List<DonutSliceData>,
    centerLabel: String,
    totalAmount: Double,
    modifier: Modifier = Modifier,
    chartSize: Dp = 120.dp,
    strokeWidth: Dp = 14.dp,
    gapAngle: Float = 0f, // 取消留白，无缝连接
    primaryInkColor: Color = Color(0xFF1C1917),
    mutedTextColor: Color = Color(0xFFA8A29E),
    emptyChartColor: Color = Color(0xFFE5E0D8),
    selectedCategory: String? = null,
    onCategorySelected: (String?) -> Unit = {}
) {
    var internalSelectedIndex by remember { mutableIntStateOf(-1) }

    val selectedIndex = remember(slices, selectedCategory, internalSelectedIndex) {
        if (selectedCategory != null) {
            slices.indexOfFirst { it.name == selectedCategory }
        } else {
            internalSelectedIndex
        }
    }

    // 动画驱动器：0f -> 1f (动画加快 1.2 倍至 1625ms)
    val progress = remember { Animatable(if (selectedCategory != null) 1f else 0f) }
    LaunchedEffect(selectedCategory) {
        if (selectedCategory == null) {
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(durationMillis = 1625, easing = LinearEasing))
        } else {
            progress.snapTo(1f)
        }
    }

    val currentTotal = slices.sumOf { it.amount }
    val displayAmount = if (selectedIndex in slices.indices) {
        slices[selectedIndex].amount
    } else {
        totalAmount.coerceAtLeast(currentTotal)
    }

    val displayLabel = if (selectedIndex in slices.indices) {
        slices[selectedIndex].name
    } else {
        centerLabel
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：自绘环形图 + 环心文字叠加
        Box(
            modifier = Modifier.size(chartSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(chartSize)) {
                val strokePx = strokeWidth.toPx()
                val radius = (size.minDimension - strokePx) / 2f
                val topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)
                val arcSize = Size(radius * 2, radius * 2)

                if (slices.isEmpty() || currentTotal <= 0.0) {
                    // 无数据时绘制浅灰色底环
                    drawArc(
                        color = emptyChartColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                    )
                } else {
                    val count = slices.size
                    // 总间隙角度（gapAngle = 0f 时 totalGap = 0f）
                    val totalGap = if (count > 1) gapAngle * count else 0f
                    val availableDegrees = (360f - totalGap).coerceAtLeast(0f)

                    var accumulatedDegree = 0f

                    slices.forEachIndexed { index, slice ->
                        // 精确计算该分段角度，最后一个分段吸收所有剩余角度，确保绝对闭合 360 度无缝
                        val rawSweep = if (currentTotal > 0.0) {
                            ((slice.amount / currentTotal) * availableDegrees).toFloat()
                        } else {
                            (slice.percentage / 100f) * availableDegrees
                        }

                        val sweepTarget = if (index == slices.lastIndex) {
                            (availableDegrees - accumulatedDegree).coerceAtLeast(0f)
                        } else {
                            rawSweep
                        }

                        val startAngle = -90f + accumulatedDegree + (index * gapAngle)

                        // 计算当前分段在全局 0f~1f 动画进度中的起始与结束时间窗
                        val windowStart = (accumulatedDegree + index * gapAngle) / 360f
                        val windowEnd = (accumulatedDegree + index * gapAngle + sweepTarget) / 360f

                        val currentProgress = progress.value
                        val currentSweep = when {
                            currentProgress <= windowStart -> 0f
                            currentProgress >= windowEnd -> sweepTarget
                            else -> {
                                val segmentFraction = (currentProgress - windowStart) / (windowEnd - windowStart).coerceAtLeast(0.0001f)
                                sweepTarget * segmentFraction
                            }
                        }

                        // 判断是否处于高亮模式
                        val alpha = if (selectedIndex == -1 || selectedIndex == index) 1f else 0.25f

                        if (currentSweep > 0.01f) {
                            drawArc(
                                color = slice.color.copy(alpha = alpha),
                                startAngle = startAngle,
                                sweepAngle = currentSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                            )
                        }

                        accumulatedDegree += sweepTarget
                    }
                }
            }

            // 环心文本
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    text = displayLabel,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = mutedTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = String.format(Locale.getDefault(), "¥%,.0f", displayAmount),
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = primaryInkColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        // 右侧：图例列表 (1fr 权重)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (slices.isEmpty()) {
                Text(
                    text = "暂无相关分类明细",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = mutedTextColor
                )
            } else {
                slices.forEachIndexed { index, slice ->
                    val isSelected = selectedIndex == index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val newIndex = if (isSelected) -1 else index
                                internalSelectedIndex = newIndex
                                val clickedName = if (newIndex >= 0 && newIndex < slices.size) slices[newIndex].name else null
                                onCategorySelected(clickedName)
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 6dp 分类颜色小圆点
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Spacer(modifier = Modifier.width(7.dp))

                        // 分类名称
                        Text(
                            text = slice.name,
                            fontSize = 11.5.sp,
                            color = if (isSelected) primaryInkColor else primaryInkColor.copy(alpha = 0.85f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // 占比
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", slice.percentage),
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = mutedTextColor,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(42.dp)
                        )

                        // 金额
                        Text(
                            text = String.format(Locale.getDefault(), "¥%,.1f", slice.amount),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = primaryInkColor,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(68.dp)
                        )
                    }
                }
            }
        }
    }
}
