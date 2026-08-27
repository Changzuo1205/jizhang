package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable

/**
 * 记账明细数据实体 (Room Database Entity)
 *
 * 对应 SQLite 数据库中的 `expenses` 数据表，记录每一笔收入或支出的明细数据。
 *
 * @property id 唯一自增主键
 * @property type 账目类型："EXPENSE" (支出) 或 "INCOME" (收入)
 * @property category 一级分类（例如："餐饮"、"交通"、"兼职外快"、"漏记款" 等）
 * @property subCategory 二级细分分类（例如："午餐"、"晚餐"、"学费"、"漏记款" 等）
 * @property amount 记账金额（数值类型 Double，始终为正值）
 * @property note 账目备注或补充说明
 * @property dateTimestamp 记账发生时间戳（毫秒数），用于日历、月份筛选及排序
 * @property accountId 关联的账户 ID（外键关联至 accounts 表的 id）
 * @property accountName 关联的账户名称（冗余存储以提升查询与显示性能）
 */
@Immutable
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String = "EXPENSE", // "EXPENSE" (支出) 或 "INCOME" (收入)
    val category: String,
    val subCategory: String = "",
    val amount: Double,
    val note: String = "",
    val dateTimestamp: Long = System.currentTimeMillis(),
    val accountId: Long = 1L,
    val accountName: String = "默认账户"
)

