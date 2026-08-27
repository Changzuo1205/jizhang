package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 资产账户数据访问对象 (Data Access Object - DAO)
 *
 * 封装对 `accounts` 数据表的操作，支持账户查询、创建、编辑、删除及余额增减更新。
 */
@Dao
interface AccountDao {
    /**
     * 响应式监听所有资产账户列表，按 ID 升序排列
     */
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    /**
     * 根据账户 ID 查询单个账户详情
     */
    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    /**
     * 插入单个账户
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    /**
     * 批量插入账户（用于应用启动时初始化默认资产账户）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    /**
     * 获取数据库中现有账户总数
     */
    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int

    /**
     * 更新已有账户信息（名称、类型、卡号、颜色等）
     */
    @Update
    suspend fun updateAccount(account: AccountEntity)

    /**
     * 删除指定账户
     */
    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    /**
     * 更新指定账户的基准余额增量
     */
    @Query("UPDATE accounts SET balance = balance + :amountChange WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, amountChange: Double)
}

