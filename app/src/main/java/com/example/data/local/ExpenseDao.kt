package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 记账数据访问对象 (Data Access Object - DAO)
 *
 * 封装对 `expenses` 数据表的所有 CRUD 操作，利用 Kotlin Flow 提供响应式实时数据流。
 */
@Dao
interface ExpenseDao {
    /**
     * 获取所有记账明细，默认按记账时间倒序排列（最新记录在前）
     */
    @Query("SELECT * FROM expenses ORDER BY dateTimestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>
    
    /**
     * 根据类型统计历史总金额（支出或收入）
     */
    @Query("SELECT SUM(amount) FROM expenses WHERE type = :type")
    fun getTotalAmountByType(type: String): Flow<Double?>
    
    /**
     * 统计指定起始时间戳之后的总金额（用于本月或本周聚合统计）
     */
    @Query("SELECT SUM(amount) FROM expenses WHERE type = :type AND dateTimestamp >= :start")
    fun getTotalAmountByTypeSince(type: String, start: Long): Flow<Double?>

    /**
     * 统计指定时间区间内的总金额（用于月度报表或自定义周期计算）
     */
    @Query("SELECT SUM(amount) FROM expenses WHERE type = :type AND dateTimestamp BETWEEN :start AND :end")
    fun getTotalAmountByTypeAndDate(type: String, start: Long, end: Long): Flow<Double?>

    /**
     * 插入单条记账记录，冲突时直接替换
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    /**
     * 批量插入记账记录（用于初始化数据及数据导入）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    /**
     * 获取数据库中记账记录的总条数
     */
    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getExpenseCount(): Int

    /**
     * 更新单条记账记录
     */
    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    /**
     * 删除单条记账记录
     */
    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    /**
     * 根据 ID 删除记账记录
     */
    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)
}

