package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ExpenseEntity::class, AccountEntity::class],
    version = 10,
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
                    "daily_expense_v10.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                
                // Ensure initial data is populated if database is empty
                scope.launch(Dispatchers.IO) {
                    populateIfEmpty(context.applicationContext, instance)
                }
                
                instance
            }
        }

        private suspend fun populateIfEmpty(context: Context, db: DailyToolboxDatabase) {
            try {
                val accountCount = db.accountDao().getAccountCount()
                if (accountCount == 0) {
                    val accounts = listOf(
                        AccountEntity(id = 1, name = "币安", type = "OTHER", balance = 0.0, cardSuffix = "", colorHex = "#F3BA2F", note = ""),
                        AccountEntity(id = 2, name = "现金", type = "CASH", balance = 0.0, cardSuffix = "", colorHex = "#F59E0B", note = ""),
                        AccountEntity(id = 3, name = "农业银行储蓄卡", type = "BANK_CARD", balance = 0.0, cardSuffix = "", colorHex = "#009688", note = ""),
                        AccountEntity(id = 4, name = "微信钱包", type = "WECHAT", balance = 0.0, cardSuffix = "", colorHex = "#07C160", note = ""),
                        AccountEntity(id = 5, name = "黄金", type = "OTHER", balance = 0.0, cardSuffix = "", colorHex = "#FFC107", note = ""),
                        AccountEntity(id = 6, name = "招商银行储蓄卡", type = "BANK_CARD", balance = 0.0, cardSuffix = "", colorHex = "#E60012", note = ""),
                        AccountEntity(id = 7, name = "基金", type = "OTHER", balance = 0.0, cardSuffix = "", colorHex = "#3F51B5", note = ""),
                        AccountEntity(id = 8, name = "支付宝", type = "ALIPAY", balance = 0.0, cardSuffix = "", colorHex = "#1677FF", note = "")
                    )
                    db.accountDao().insertAccounts(accounts)
                }

                val expenseCount = db.expenseDao().getExpenseCount()
                if (expenseCount == 0) {
                    val jsonString = context.assets.open("initial_expenses.json").bufferedReader().use { it.readText() }
                    val jsonArray = org.json.JSONArray(jsonString)
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    val expenseList = mutableListOf<ExpenseEntity>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val dateStr = obj.getString("date_str")
                        val timestamp = dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                        
                        expenseList.add(
                            ExpenseEntity(
                                type = obj.getString("type"),
                                category = obj.getString("category"),
                                subCategory = obj.optString("subCategory", ""),
                                amount = obj.getDouble("amount"),
                                note = obj.optString("note", ""),
                                dateTimestamp = timestamp,
                                accountId = obj.getLong("accountId"),
                                accountName = obj.getString("accountName")
                            )
                        )
                    }

                    if (expenseList.isNotEmpty()) {
                        db.expenseDao().insertExpenses(expenseList)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
