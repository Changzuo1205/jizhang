package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.LocalAppBackgroundConfig
import kotlinx.coroutines.delay

/**
 * 广受好评的 Wide 极致美学开屏动效 (Cream / Forest Edition)
 * 根据应用内背景设置的 isLight 自动选择：
 * - Light 模式: 背景 #FAFAF7，标题纯黑，砖红装饰线 (#C4623D)
 * - Dark 模式: 背景 #2D3A2E，标题米白，砖红装饰线 (#C4623D)
 * 时序与属性：总时长 1.4s，采用 ease-out 缓动，零弹跳、零旋转、纯净落定。
 */
@Composable
fun SplashScreen(
    splashDone: Boolean,
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val isLight = bgConfig.isLight

    // 配色定义
    val backgroundColor = if (isLight) Color(0xFFFAFAF7) else Color(0xFF2D3A2E)
    val titleColor = if (isLight) Color(0xFF141414) else Color(0xFFFAFAF7)
    val sloganColor = if (isLight) Color(0x99141414) else Color(0xA6FAFAF7)
    val footerColor = if (isLight) Color(0x4D141414) else Color(0x4DFAFAF7)
    val accentColor = Color(0xFFC4623D)

    // 动画触发状态
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        startAnim = true
    }

    // 阶段控制动画值
    val titleAnimProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 700, delayMillis = 250, easing = FastOutSlowInEasing),
        label = "titleAnim"
    )

    val lineAnimProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 350, delayMillis = 800, easing = FastOutSlowInEasing),
        label = "lineAnim"
    )

    val sloganAnimProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 950, easing = FastOutSlowInEasing),
        label = "sloganAnim"
    )

    val footerAnimProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 1100, easing = FastOutSlowInEasing),
        label = "footerAnim"
    )

    AnimatedVisibility(
        visible = !splashDone,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.zIndex(1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(horizontal = 32.dp, vertical = 50.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 120.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // 主标题 "Ledger" (支持 Serif 衬线斜体)
                Text(
                    text = "Ledger",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Normal,
                    color = titleColor,
                    modifier = Modifier.graphicsLayer {
                        alpha = titleAnimProgress
                        translationY = (1f - titleAnimProgress) * 8.dp.toPx()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 砖红装饰线 (#C4623D)
                Box(
                    modifier = Modifier
                        .height(1.dp)
                        .width(40.dp * lineAnimProgress)
                        .background(accentColor)
                        .graphicsLayer { alpha = lineAnimProgress }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Slogan 副文案
                Text(
                    text = "A quiet bookkeeper,\nfor one.",
                    fontFamily = FontFamily.SansSerif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 20.sp,
                    color = sloganColor,
                    modifier = Modifier.graphicsLayer {
                        alpha = sloganAnimProgress
                        translationY = (1f - sloganAnimProgress) * 4.dp.toPx()
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // 底栏版本号与年份
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = footerAnimProgress
                            translationY = (1f - footerAnimProgress) * 4.dp.toPx()
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "v 1.0",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
                        color = footerColor
                    )
                    Text(
                        text = "EST. 2026",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
                        color = footerColor
                    )
                }
            }
        }
    }
}
