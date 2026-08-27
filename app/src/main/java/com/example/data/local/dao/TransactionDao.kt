package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.TransactionType
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/** 交易表 DAO（核心表，软删除语义）。 */
@Dao
interface TransactionDao {

    /** 全部未删除交易（按发生时间倒序），供首页/报表/日历消费 */
    @Query("SELECT * FROM transactions WHERE is_deleted = 0 ORDER BY occurred_at DESC, id DESC")
    fun observeActive(): Flow<List<TransactionEntity>>

    /** 含已删除的全量观察（导出 CSV / 回收站类功能用） */
    @Query("SELECT * FROM transactions ORDER BY occurred_at DESC, id DESC")
    fun observeIncludingDeleted(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE is_deleted = 0")
    suspend fun getActiveOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE is_deleted = 0")
    suspend fun activeCount(): Int

    /** 汇总：某账户在某区间、某类型下已确认交易的净额（分）。
     *  expense 取负贡献，income 取正贡献，transfer 由 Repository 拆两端处理。 */
    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END), 0) " +
            "FROM transactions " +
            "WHERE is_deleted = 0 AND account_id = :accountId AND occurred_at BETWEEN :fromMillis AND :toMillis " +
            "AND type IN ('EXPENSE','INCOME')"
    )
    suspend fun netIncomeExpenseFor(accountId: Long, fromMillis: Long, toMillis: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tx: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(txs: List<TransactionEntity>)

    @Update
    suspend fun update(tx: TransactionEntity)

    /** 软删除：仅置位标记并刷新更新时间，物理行保留以维护外键与历史 */
    @Query("UPDATE transactions SET is_deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long)

    /** 类型便捷过滤（VM 内存筛选为主路径，此查询供定向场景） */
    @Query(
        "SELECT * FROM transactions WHERE is_deleted = 0 AND type = :type " +
            "ORDER BY occurred_at DESC"
    )
    fun observeByType(type: TransactionType): Flow<List<TransactionEntity>>

    /** 某账户作为转出方的累计转出额（分），余额派生用 */
    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE is_deleted = 0 AND type = 'TRANSFER' AND account_id = :accountId"
    )
    suspend fun transferOutTotalFor(accountId: Long): Int

    /** 某账户作为转入方（对方账户）的累计转入额（分），余额派生用 */
    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE is_deleted = 0 AND type = 'TRANSFER' AND transfer_to_account_id = :accountId"
    )
    suspend fun transferInTotalFor(accountId: Long): Int

    /** 某账户全期「收入−支出」净贡献（分，不含转账），校准基准余额用 */
    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END), 0) " +
            "FROM transactions WHERE is_deleted = 0 AND account_id = :accountId AND type IN ('EXPENSE','INCOME')"
    )
    suspend fun lifetimeNetCentsFor(accountId: Long): Int
}
