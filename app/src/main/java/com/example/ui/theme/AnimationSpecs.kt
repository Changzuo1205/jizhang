package com.example.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color

/** 月度支出进度条专用 spring：临界阻尼、慢收敛（~900-1100ms） */
val ProgressFillSpring = spring<Float>(
    stiffness = Spring.StiffnessLow,
    dampingRatio = Spring.DampingRatioNoBouncy
)

/** 月度支出进度条专用颜色 tween：略快于 fill，颜色先到、填充收尾 */
val ProgressColorTween = tween<Color>(
    durationMillis = 600,
    easing = FastOutSlowInEasing
)