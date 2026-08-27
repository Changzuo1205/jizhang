package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ToolboxViewModel

/**
 * 应用程序主入口 Activity
 *
 * 核心职责：
 * 1. 开启沉浸式 Edge-to-Edge 视觉渲染。
 * 2. 注入全局 [ToolboxViewModel] 实例。
 * 3. 根据壁纸方案（深色/浅色），动态自适应状态栏与导航栏文字图标对比度。
 * 4. 挂载 [MainScreen] 核心主页面。
 */
class MainActivity : ComponentActivity() {
    private val viewModel: ToolboxViewModel by viewModels {
        ToolboxViewModel.factory(application as JizhangApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val bgConfig by viewModel.backgroundConfig.collectAsState()

            // 动态适配系统状态栏和底部导航栏的文字/图标色彩
            DisposableEffect(bgConfig.isLight) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = bgConfig.isLight
                insetsController.isAppearanceLightNavigationBars = bgConfig.isLight
                onDispose { }
            }

            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
