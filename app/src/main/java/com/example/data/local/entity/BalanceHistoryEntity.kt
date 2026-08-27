package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 余额历史表：记录每次手动设置基准余额的快照（Phase 2 的余额校准会写入一条）。
 */
@Entity(
    tableName = "balance_history",
    indices = [
        Index(value = ["account_id"]),
        Index(value = ["effective_at"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntityV2::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BalanceHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    /** 当时的基准余额（分） */
    val balance: Int,
    /** 生效时间（Unix 毫秒） */
    @ColumnInfo(name = "effective_at")
    val effectiveAt: Long,
    val note: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
