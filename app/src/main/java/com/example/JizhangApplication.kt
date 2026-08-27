package com.example

import android.app.Application
import com.example.di.AppContainer
import com.example.di.AppContainerImpl

/**
 * 应用入口：以【手动依赖注入容器】替代 Hilt。
 *
 * 历史说明（Phase 1, 2026-02）：
 * 交接包要求引入 Hilt，但其 Gradle 插件在 AGP 9.1.1 下报
 * "Android BaseExtension not found"（插件仍依赖 AGP 已移除的旧版 BaseExtension API，
 * 见构建日志与计划文件 A2 预案），故按已批准的降级方案改用轻量手写 DI；
 * 改造面与 Hilt 方案一致（构造注入 + 单例容器），未来 AGP 兼容解决后可平迁回注解风格。
 */
class JizhangApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
    }
}
