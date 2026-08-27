package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 分类表：一级 + 二级树形结构（parentId 为空即一级分类），按 type 区分支出/收入。
 *
 * 注意：parent_id 故意不加外键约束 —— Room 对自引用外键的级联行为存在限制，
 * 层级完整性在 Repository 层保证。
 */
@Entity(
    tableName = "category",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["book_id"]),
        Index(value = ["parent_id"])
    ],
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["user_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["book_id"], onDelete = ForeignKey.RESTRICT)
    ]
)
data class CategoryEntity(
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
    /** NULL = 一级分类；否则指向父级分类 id */
    @ColumnInfo(name = "parent_id")
    val parentId: Long? = null,
    /** expense / income */
    val type: String,
    val icon: String? = null,
    val color: String? = null,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
