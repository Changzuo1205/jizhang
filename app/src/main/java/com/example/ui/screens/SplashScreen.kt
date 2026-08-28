package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush as GraphicsBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.LocalAppBackgroundConfig

/**
 * 开屏等待页：纯延时 600ms 显示品牌 logo + 加载圈，让背景等数据先行加载完成。
 * 由 [splashDone] 控制显隐；splash 阶段覆盖在 MainScreen 顶层。
 *
 * 设计目标：splash 期间用户看到的不应该是带动画的首页（背景加载与动画竞争主线程会卡顿），
 * 而是一个静态的等待页面，加载完后淡出，首页进度条动画才开始跑。
 */
@Composable
fun SplashScreen(
    splashDone: Boolean,
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current

    AnimatedVisibility(
        visible = !splashDone,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.zIndex(1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    GraphicsBrush.verticalGradient(
                        listOf(
                            if (bgConfig.isLight) Color(0xFFF6F8FC) else Color(0xFF090D16),
                            if (bgConfig.isLight) Color(0xFFEEF2F8) else Color(0xFF131C35)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "\uD83D\uDCB0",
                    fontSize = 56.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "日常记账",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = bgConfig.textPrimary
                )
            }
        }
    }
}