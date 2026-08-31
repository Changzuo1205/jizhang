package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

/** 账本表 DAO。 */
@Dao
interface BookDao {

    /** 观察未归档账本（按 sortOrder 升序） */
    @Query(
        "SELECT * FROM book WHERE is_archived = 0 " +
            "ORDER BY is_default DESC, sort_order ASC, created_at ASC"
    )
    fun observeActive(): Flow<List<BookEntity>>

    @Query("SELECT * FROM book WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BookEntity?

    @Query("SELECT * FROM book WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): BookEntity?

    @Query("SELECT * FROM book WHERE is_default = 1 LIMIT 1")
    suspend fun getDefault(): BookEntity?

    @Query("SELECT COUNT(*) FROM book")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    /** 切换默认账本：先清掉旧默认，再置位新默认（在 Repository 的 withTransaction 中调用） */
    @Query("UPDATE book SET is_default = 0, updated_at = :now WHERE is_default = 1")
    suspend fun clearDefaultFlag(now: Long)

    @Query("UPDATE book SET is_default = 1, updated_at = :now WHERE id = :bookId")
    suspend fun setDefault(bookId: Long, now: Long)

    /** 归档（软语义，不物理删除以保护交易外键） */
    @Query("UPDATE book SET is_archived = 1, updated_at = :now WHERE id = :bookId")
    suspend fun archive(bookId: Long, now: Long)

    @Query("SELECT * FROM book WHERE is_archived = 0 ORDER BY is_default DESC, sort_order ASC, created_at ASC")
    suspend fun getActiveOnce(): List<BookEntity>

    @Query("DELETE FROM book WHERE id = :bookId")
    suspend fun deleteById(bookId: Long)
}
