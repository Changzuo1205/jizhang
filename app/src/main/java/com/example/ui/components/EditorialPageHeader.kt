package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppBackgroundConfig

@Composable
fun EditorialPageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    rightContent: @Composable () -> Unit = {}
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val textMain = bgConfig.textPrimary
    val textMuted = bgConfig.textTertiary

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                color = textMain,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = textMuted,
                letterSpacing = 0.5.sp
            )
        }
        rightContent()
    }
}
