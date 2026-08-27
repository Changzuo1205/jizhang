package com.example.di

import android.content.Context
import com.example.data.local.DailyToolboxDatabase
import com.example.data.local.DatabaseSeeder
import com.example.data.migration.LegacyBackup
import com.example.data.repository.ToolboxRepositoryV2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * 手动依赖注入容器接口。
 *
 * 说明：Hilt Gradle 插件与 AGP 9.x 不兼容（BaseExtension 已移除），
 * 项目按计划降级为手写构造注入；容器在 [JizhangApplication] 中创建，
 * 仅 MainActivity 一个消费方，未来迁移 Hilt 时本类即 AppModule 蓝图。
 */
interface AppContainer {
    /** 规范化数据层门面 */
    val repository: ToolboxRepositoryV2

    /** 应用级作用域（脱离页面生命周期的种子/备份任务使用） */
    val applicationScope: CoroutineScope

    /** 六表种子是否已灌入完成；任何依赖默认账本的写操作需先 await 此句柄 */
    val seedCompleted: Deferred<Unit>
}

class AppContainerImpl(context: Context) : AppContainer {

    private val appContext = context.applicationContext

    override val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: DailyToolboxDatabase

    override val seedCompleted: Deferred<Unit>

    init {
        // 旧库(v10/v6)文件先整体备份到私有目录再清理 —— 禁止静默丢数据
        runCatching { LegacyBackup.backupAndRemoveLegacyDatabases(appContext) }

        database = DailyToolboxDatabase.build(appContext)

        seedCompleted = applicationScope.async {
            DatabaseSeeder.seedIfEmpty(appContext, database)
        }
    }

    override val repository: ToolboxRepositoryV2 by lazy { ToolboxRepositoryV2(database) }
}
