package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户表。单机场景固定一条记录（id=1），字段随目标 schema 预留，
 * 为未来多用户 / 云同步做准备（见交接包 02-数据库设计.md）。
 */
@Entity(tableName = "user", indices = [Index(value = ["name"], unique = false)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "default_currency")
    val defaultCurrency: String = "CNY",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
