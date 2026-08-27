package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ExpenseEntity::class, AccountEntity::class],
    version = 7,
    exportSchema = false
)
abstract class DailyToolboxDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: DailyToolboxDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): DailyToolboxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DailyToolboxDatabase::class.java,
                    "daily_expense_db_v6"
                ).fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(context.applicationContext, scope)).build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context,
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(context, database)
                    }
                }
            }

            private suspend fun populateInitialData(context: Context, db: DailyToolboxDatabase) {
                val acc1 = AccountEntity(id = 1, name = "币安", type = "OTHER", balance = 0.0, cardSuffix = "", colorHex = "#F3BA2F", note = "")
                val acc2 = AccountEntity(id = 2, name = "现金", type = "CASH", balance = 0.0, cardSuffix = "", colorHex = "#F59E0B", note = "")
                val acc3 = AccountEntity(id = 3, name = "农业银行储蓄卡", type = "BANK_CARD", balance = 0.0, cardSuffix = "", colorHex = "#009688", note = "")
                val acc4 = AccountEntity(id = 4, name = "微信钱包", type = "WECHAT", balance = 0.0, cardSuffix = "", colorHex = "#07C160", note = "")
                val acc5 = AccountEntity(id = 5, name = "黄金", type = "OTHER", balance = 0.0, cardSuffix = "", colorHex = "#FFC107", note = "")
                val acc6 = AccountEntity(id = 6, name = "招商银行储蓄卡", type = "BANK_CARD", balance = 0.0, cardSuffix = "", colorHex = "#E60012", note = "")
                val acc7 = AccountEntity(id = 7, name = "基金", type = "OTHER", balance = 0.0, cardSuffix = "", colorHex = "#3F51B5", note = "")
                val acc8 = AccountEntity(id = 8, name = "支付宝", type = "ALIPAY", balance = 0.0, cardSuffix = "", colorHex = "#1677FF", note = "")

                db.accountDao().insertAccount(acc1)
                db.accountDao().insertAccount(acc2)
                db.accountDao().insertAccount(acc3)
                db.accountDao().insertAccount(acc4)
                db.accountDao().insertAccount(acc5)
                db.accountDao().insertAccount(acc6)
                db.accountDao().insertAccount(acc7)
                db.accountDao().insertAccount(acc8)

                try {
                    val jsonString = context.assets.open("initial_expenses.json").bufferedReader().use { it.readText() }
                    val jsonArray = org.json.JSONArray(jsonString)
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val dateStr = obj.getString("date_str")
                        val timestamp = dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                        
                        val expense = ExpenseEntity(
                            type = obj.getString("type"),
                            category = obj.getString("category"),
                            subCategory = obj.optString("subCategory", ""),
                            amount = obj.getDouble("amount"),
                            note = obj.optString("note", ""),
                            dateTimestamp = timestamp,
                            accountId = obj.getLong("accountId"),
                            accountName = obj.getString("accountName")
                        )
                        db.expenseDao().insertExpense(expense)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
