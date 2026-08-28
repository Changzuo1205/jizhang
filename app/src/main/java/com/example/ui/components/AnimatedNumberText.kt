package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import java.util.Locale

@Composable
fun AnimatedNumberText(
    value: Double,
    prefix: String = "¥ ",
    suffix: String = "",
    fractionDigits: Int = 2,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 800),
        label = "number_animation"
    )

    val fmt = "%,.${fractionDigits}f"
    Text(
        text = "$prefix${String.format(Locale.CHINA, fmt, animatedValue)}$suffix",
        style = textStyle,
        color = color,
        modifier = modifier
    )
}
