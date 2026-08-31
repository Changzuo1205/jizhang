package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppBackgroundConfig

@Composable
fun DesignDraftPlaceholderScreen() {
    val bgConfig = LocalAppBackgroundConfig.current
    val isLight = bgConfig.isLight
    
    // 暖纸白背景 #F5F1E6
    val backgroundColor = if (isLight) Color(0xFFF5F1E6) else Color(0xFF1E281E)
    val contentColor = Color(0xFF8A8270) // 中性灰褐色
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 虚线描边的圆形图标容器
        Box(
            modifier = Modifier
                .size(64.dp)
                .drawBehind {
                    drawCircle(
                        color = contentColor,
                        radius = size.minDimension / 2f,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "设计稿",
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 文字
        Text(
            text = "设计稿 · 敬请期待",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = contentColor
        )
    }
}
