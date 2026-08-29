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
            // 独立迁移分支：老用户（库已存在）的自定义分类一次性补插，幂等标记在 seeder 内维护
            DatabaseSeeder.importLegacyCustomCategoriesIfPending(appContext, database)
            // 同步确保所有「漏记款」归于「居家」一级分类下（支出端）
            DatabaseSeeder.syncMissedCategoryUnderHome(appContext, database)
            // 同步确保扁平化 19 个标准收入分类，移除收入端「居家」，迁移「漏记款」为收入一级分类
            DatabaseSeeder.syncFlatIncomeCategories(appContext, database)
            // 同步并校准七个标准资产账户信息与目标余额（已禁用种子数据，保持初始状态）
            // DatabaseSeeder.syncUserAccountBalances(appContext, database)
        }
    }

    override val repository: ToolboxRepositoryV2 by lazy { ToolboxRepositoryV2(database) }
}
