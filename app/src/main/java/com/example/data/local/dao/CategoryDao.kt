package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/** 分类表 DAO（一级+二级树形）。 */
@Dao
interface CategoryDao {

    /** 观察未归档分类（全量，内存中组树；分类总量 < 200 条） */
    @Query("SELECT * FROM category WHERE is_archived = 0 ORDER BY sort_order ASC, id ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CategoryEntity?

    @Query(
        "SELECT * FROM category WHERE book_id = :bookId AND type = :type AND is_archived = 0 " +
            "ORDER BY sort_order ASC, id ASC"
    )
    suspend fun getByType(bookId: Long, type: String): List<CategoryEntity>

    @Query("SELECT * FROM category WHERE name = :name AND parent_id IS NULL AND type = :type LIMIT 1")
    suspend fun findParentByName(name: String, type: String): CategoryEntity?

    @Query("SELECT * FROM category WHERE name = :name AND parent_id = :parentId LIMIT 1")
    suspend fun findChildByName(name: String, parentId: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM category")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("UPDATE category SET is_archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}
