package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.local.entity.BalanceHistoryEntity
import kotlinx.coroutines.flow.Flow

/** 余额历史表 DAO。 */
@Dao
interface BalanceHistoryDao {

    @Insert
    suspend fun insert(history: BalanceHistoryEntity): Long

    @Query("SELECT * FROM balance_history WHERE account_id = :accountId ORDER BY effective_at DESC, id DESC")
    fun observeForAccount(accountId: Long): Flow<List<BalanceHistoryEntity>>

    @Query("SELECT * FROM balance_history WHERE account_id = :accountId ORDER BY effective_at DESC, id DESC LIMIT 1")
    suspend fun latestForAccount(accountId: Long): BalanceHistoryEntity?
}
