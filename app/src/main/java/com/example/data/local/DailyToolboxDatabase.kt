package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.local.dao.AccountDaoV2
import com.example.data.local.dao.BalanceHistoryDao
import com.example.data.local.dao.BookDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AccountEntityV2
import com.example.data.local.entity.BalanceHistoryEntity
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity

/**
 * 记账工具核心 Room 本地数据库（v11，规范化六表结构）。
 *
 * 与 v10 的关系（见交接包 02-数据库设计.md）：
 * - 存储层完全替换：expenses/accounts 两表 → user/book/account/category/transactions/balance_history 六表
 * - 金额 Double 元 → Int 分；时间戳统一 Unix 毫秒；软删除 isDeleted 取代物理删除
 * - [exportSchema] 开启且 schema 落盘到 app/schemas/，此后任何版本升级必须编写正式 Migration
 *   （旧版 fallbackToDestructiveMigration 已移除——升级即清库是明令禁止的行为）
 * - 老库文件由 [com.example.data.migration.LegacyBackup] 在首次启动时备份后清理
 */
@Database(
    entities = [
        UserEntity::class,
        BookEntity::class,
        AccountEntityV2::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BalanceHistoryEntity::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(DailyToolboxDatabase.Converters::class)
abstract class DailyToolboxDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun bookDao(): BookDao
    abstract fun accountDao(): AccountDaoV2
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun balanceHistoryDao(): BalanceHistoryDao

    /** 枚举类型与 TEXT 列的互转 */
    class Converters {
        @TypeConverter
        fun transactionTypeToString(type: TransactionType): String = type.name

        @TypeConverter
        fun stringToTransactionType(raw: String): TransactionType =
            runCatching { TransactionType.valueOf(raw) }.getOrDefault(TransactionType.EXPENSE)
    }

    companion object {
        const val DB_NAME = "daily_expense_v11.db"

        /**
         * 构建 Room 实例。不再持有双检锁单例（生命周期由 AppContainer 管理），
         * 也不注册任何破坏性迁移策略。
         */
        fun build(context: Context): DailyToolboxDatabase =
            Room.databaseBuilder(context.applicationContext, DailyToolboxDatabase::class.java, DB_NAME)
                .build()
    }
}
