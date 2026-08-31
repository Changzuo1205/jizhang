package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.data.local.entity.AccountEntityV2
import kotlinx.coroutines.flow.Flow

/** 资产账户表 DAO（v2 结构，余额派生见 Repository）。 */
@Dao
interface AccountDaoV2 {

    /** 观察未归档账户 */
    @Query("SELECT * FROM account WHERE is_archived = 0 ORDER BY sort_order ASC, id ASC")
    fun observeActive(): Flow<List<AccountEntityV2>>

    @Query("SELECT * FROM account WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AccountEntityV2?

    @Query("SELECT * FROM account WHERE name = :name AND is_archived = 0 ORDER BY id ASC LIMIT 1")
    suspend fun getByName(name: String): AccountEntityV2?

    /** 一次性取全部未归档账户（种子/批量解析用） */
    @Query("SELECT * FROM account WHERE is_archived = 0 ORDER BY sort_order ASC, id ASC")
    suspend fun getActive(): List<AccountEntityV2>

    @Query("SELECT COUNT(*) FROM account")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntityV2): Long

    @Upsert
    suspend fun upsertAll(accounts: List<AccountEntityV2>)

    @Query("UPDATE account SET initial_balance = :balanceCents, updated_at = :now WHERE id = :id")
    suspend fun updateInitialBalance(id: Long, balanceCents: Int, now: Long)

    /** 元信息整行更新（名称/类型/颜色等），按主键存在则替换 */
    @Upsert
    suspend fun upsert(account: AccountEntityV2)

    @Query("UPDATE account SET is_archived = 1, updated_at = :now WHERE id = :id")
    suspend fun archive(id: Long, now: Long)

    @Query("SELECT * FROM account WHERE book_id = :bookId AND is_archived = 0 ORDER BY sort_order ASC, id ASC")
    suspend fun getByBookId(bookId: Long): List<AccountEntityV2>

    @Query("DELETE FROM account WHERE book_id = :bookId")
    suspend fun deleteByBookId(bookId: Long)

    @Query("UPDATE account SET initial_balance = 0, updated_at = :now WHERE book_id = :bookId")
    suspend fun resetBalancesByBookId(bookId: Long, now: Long)
}
