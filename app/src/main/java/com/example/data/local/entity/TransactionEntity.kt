package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.local.TransactionType
import java.util.UUID

/**
 * 交易表（核心表）。对应交接包 02-数据库设计.md 的 transaction 表，
 * 表名使用 [transactions] 以避开 SQLite 保留字 `TRANSACTION`。
 *
 * 金额单位为「分」（Int），时间戳为 Unix 毫秒；软删除 isDeleted 替代物理删除。
 * syncStatus / remoteId / source 为云同步预留字段（当前恒 local/manual）。
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["user_id", "book_id", "occurred_at"]),
        Index(value = ["account_id"]),
        Index(value = ["category_id"])
    ],
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["book_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = AccountEntityV2::class, parentColumns = ["id"], childColumns = ["account_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["category_id"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 业务唯一标识，32 位 hex（无连字符），同步友好 */
    @ColumnInfo(name = "uuid")
    val uuid: String = UUID.randomUUID().toString().replace("-", ""),
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    /** 转账时的对方账户 id，仅 type=TRANSFER 时非空 */
    @ColumnInfo(name = "transfer_to_account_id")
    val transferToAccountId: Long? = null,
    val type: TransactionType,
    /** 金额（分，恒为正数） */
    val amount: Int,
    /** 分类 id；支出/收入必填，转账为空 */
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,
    val note: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** 实际发生时间（Unix 毫秒） */
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    /** 软删除标记：删除仅置位，保留外键与历史分析价值 */
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    /** 同步状态：local / pending / synced */
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "local",
    /** 远端主键（云同步时回填） */
    @ColumnInfo(name = "remote_id")
    val remoteId: String? = null,
    /** 来源：manual / wechat / alipay */
    val source: String = "manual"
)
