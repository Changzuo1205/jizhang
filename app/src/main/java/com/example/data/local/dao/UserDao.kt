package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/** 用户表 DAO。单机场景恒只有 id=1 一条记录。 */
@Dao
interface UserDao {

    @Query("SELECT * FROM `user` WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM `user` ORDER BY id ASC LIMIT 1")
    suspend fun getFirst(): UserEntity?

    @Query("SELECT COUNT(*) FROM `user`")
    suspend fun count(): Int

    @Query("SELECT * FROM `user` ORDER BY id ASC")
    fun observeAll(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(users: List<UserEntity>)
}
