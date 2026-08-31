package com.example.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * 资产净值数据项
 */
data class MonthAssetPoint(
    val monthLabel: String, // 格式如 "24.08", "26.08"
    val assetValue: Double
)

/**
 * Catmull-Rom 样条平滑曲线图表
 *
 * 核心特性：
 * 1. 严格按照实际数据绘制，最新月份（如当前8月）实时显示在最右端
 * 2. 支持水平滚动查看所有历史月份
 * 3. 动画：组件首次加载时自动滚到最右侧，并对当前屏幕显示部分执行从左向右逐渐显现的平滑动画；
 *    未显示的历史部分不添加多余动画但直接在底层绘制完毕，用户左滑可即时查看。
 */
@Composable
fun CurveTrendChart(
    points: List<MonthAssetPoint>,
    modifier: Modifier = Modifier,
    pointSpacing: Dp = 46.dp,
    chartHeight: Dp = 150.dp,
    primaryColor: Color = Color(0xFF2D6A4F), // 墨绿
    canvasBgColor: Color = Color(0xFFF9F8F5), // 纸白背景
    textColorMuted: Color = Color(0xFFA8A29E),
    autoScrollToLatest: Boolean = true
) {
    if (points.isEmpty()) return

    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // 动画控制器：0f -> 1f (从屏幕左侧到右侧逐渐展开)
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(points, autoScrollToLatest) {
        if (autoScrollToLatest && points.isNotEmpty()) {
            scrollState.scrollTo(scrollState.maxValue)
        }
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1425, easing = FastOutSlowInEasing)
        )
    }

    val pointSpacingPx = with(density) { pointSpacing.toPx() }
    val horizontalPaddingPx = with(density) { 36.dp.toPx() }
    val bottomLabelHeightPx = with(density) { 24.dp.toPx() }
    val topPaddingPx = with(density) { 28.dp.toPx() }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val viewportWidthPx = with(density) { maxWidth.toPx() }

        // 计算实际总宽度（保证至少填满一屏，超出则横向滑动）
        val contentWidthDp = remember(points.size, pointSpacing) {
            val calculated = 72.dp + pointSpacing * (points.size - 1).coerceAtLeast(0)
            calculated.coerceAtLeast(maxWidth)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(contentWidthDp)
                    .height(chartHeight)
            ) {
                val w = size.width
                val h = size.height
                val usableHeight = h - topPaddingPx - bottomLabelHeightPx

                val lowestVal = minOf(0.0, points.minOfOrNull { it.assetValue } ?: 0.0)
                val highestVal = maxOf(1.0, points.maxOfOrNull { it.assetValue } ?: 1.0)
                val valRange = (highestVal - lowestVal).coerceAtLeast(1.0)

                val baselineY = topPaddingPx + usableHeight

                // 计算所有点在 Canvas 上的物理坐标（底线为 0%，最高点为 100%）
                val screenPoints = points.mapIndexed { index, item ->
                    val x = horizontalPaddingPx + index * pointSpacingPx
                    val normalizedY = ((item.assetValue - lowestVal) / valRange).toFloat()
                    val y = topPaddingPx + usableHeight * (1f - normalizedY)
                    Offset(x, y)
                }

                // 1. 绘制水平轻微基准线 (0% 资产底线)
                drawLine(
                    color = textColorMuted.copy(alpha = 0.2f),
                    start = Offset(horizontalPaddingPx / 2, baselineY),
                    end = Offset(w - horizontalPaddingPx / 2, baselineY),
                    strokeWidth = 0.8.dp.toPx()
                )

                // 2. 用 Catmull-Rom 样条构建平滑曲线路径（严格限制在 0% 基线以上，避免无负债时下潜溢出）
                val curvePath = catmullRomToBezierPath(
                    points = screenPoints,
                    minY = topPaddingPx,
                    maxY = baselineY
                )

                // 3. 构建面积闭合路径
                val fillPath = Path()
                if (screenPoints.isNotEmpty()) {
                    fillPath.addPath(curvePath)
                    fillPath.lineTo(screenPoints.last().x, baselineY)
                    fillPath.lineTo(screenPoints.first().x, baselineY)
                    fillPath.close()
                }

                // 4. 计算当前屏幕可视区域的左边界与动态 reveal 边界
                // scrollState.value 对应当前视口左侧在总 Canvas 上的像素偏移
                val visibleLeftX = scrollState.value.toFloat()
                val visibleRightX = (visibleLeftX + viewportWidthPx).coerceAtMost(w)
                val currentProgress = animProgress.value

                // 未显示部分（视口左侧 x < visibleLeftX）不受动画影响直接完整绘制；
                // 当前视口部分随 progress 进度从 visibleLeftX 向 visibleRightX 展开
                val revealBoundaryX = if (currentProgress >= 1f) {
                    w + 100f
                } else {
                    visibleLeftX + (visibleRightX - visibleLeftX) * currentProgress
                }

                // 在 clipRect 中绘制曲线、面积、节点和数值
                clipRect(left = 0f, top = 0f, right = revealBoundaryX, bottom = h) {
                    // 面积填充 (墨绿 alpha 0.07f)
                    if (screenPoints.isNotEmpty()) {
                        drawPath(
                            path = fillPath,
                            color = primaryColor.copy(alpha = 0.07f),
                            style = Fill
                        )
                    }

                    // 曲线描边
                    drawPath(
                        path = curvePath,
                        color = primaryColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 绘制数据点与标签
                    val textPaint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        textSize = with(density) { 8.5.sp.toPx() }
                        color = textColorMuted.toArgb()
                        typeface = android.graphics.Typeface.MONOSPACE
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    val latestValuePaint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        textSize = with(density) { 10.sp.toPx() }
                        color = primaryColor.toArgb()
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    screenPoints.forEachIndexed { index, pointOffset ->
                        val isLast = index == screenPoints.lastIndex
                        val item = points[index]

                        if (isLast) {
                            // 最新月份：实心大圆
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.25f),
                                radius = 6.dp.toPx(),
                                center = pointOffset
                            )
                            drawCircle(
                                color = primaryColor,
                                radius = 4.dp.toPx(),
                                center = pointOffset
                            )

                            // 标注具体金额数值（如 "¥128,450"）
                            val formattedVal = String.format(Locale.getDefault(), "¥%,.0f", item.assetValue)
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawText(
                                    formattedVal,
                                    pointOffset.x,
                                    pointOffset.y - 10.dp.toPx(),
                                    latestValuePaint
                                )
                            }
                        } else {
                            // 往期月份：空心小圆（墨绿边，纸白底）
                            drawCircle(
                                color = canvasBgColor,
                                radius = 2.8.dp.toPx(),
                                center = pointOffset
                            )
                            drawCircle(
                                color = primaryColor,
                                radius = 2.8.dp.toPx(),
                                center = pointOffset,
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                        }

                        // 月份标签标注在基线下方（如 "24.08"）
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                item.monthLabel,
                                pointOffset.x,
                                baselineY + 14.dp.toPx(),
                                textPaint
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 将一系列离散点通过 Catmull-Rom 样条转换为光滑的三次贝塞尔曲线路径
 */
fun catmullRomToBezierPath(
    points: List<Offset>,
    minY: Float = Float.NEGATIVE_INFINITY,
    maxY: Float = Float.POSITIVE_INFINITY
): Path {
    val path = Path()
    if (points.isEmpty()) return path
    if (points.size == 1) {
        path.moveTo(points[0].x, points[0].y)
        return path
    }
    if (points.size == 2) {
        path.moveTo(points[0].x, points[0].y)
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    path.moveTo(points[0].x, points[0].y)

    for (i in 0 until points.size - 1) {
        val p0 = if (i > 0) points[i - 1] else Offset(2 * points[0].x - points[1].x, 2 * points[0].y - points[1].y)
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else Offset(2 * points[i + 1].x - points[i].x, 2 * points[i + 1].y - points[i].y)

        val control1X = p1.x + (p2.x - p0.x) / 6f
        val rawControl1Y = p1.y + (p2.y - p0.y) / 6f
        val control1Y = rawControl1Y.coerceIn(minY, maxY)

        val control2X = p2.x - (p3.x - p1.x) / 6f
        val rawControl2Y = p2.y - (p3.y - p1.y) / 6f
        val control2Y = rawControl2Y.coerceIn(minY, maxY)

        path.cubicTo(control1X, control1Y, control2X, control2Y, p2.x, p2.y)
    }

    return path
}
