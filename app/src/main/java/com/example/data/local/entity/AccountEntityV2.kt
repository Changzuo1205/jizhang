package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 资产账户表（v2）。
 *
 * 语义变更（Phase 1/2）：旧的 accounts 表把余额随每笔交易原地累加；
 * 新结构以 initial_balance 为基准余额，实时余额由
 * 「initial_balance ± Σ(未删除交易)」派生得出，杜绝漂移。
 */
@Entity(
    tableName = "account",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["book_id"])
    ],
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["user_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["book_id"], onDelete = ForeignKey.RESTRICT)
    ]
)
data class AccountEntityV2(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 业务唯一标识，32 位 hex */
    @ColumnInfo(name = "uuid")
    val uuid: String,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    val name: String,
    /** cash / debit / credit / alipay / wechat / investment ... */
    val type: String,
    /** 基准余额（分）。用户手动校准或首次设置时写入 */
    @ColumnInfo(name = "initial_balance")
    val initialBalance: Int = 0,
    val icon: String? = null,
    val color: String? = null,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
