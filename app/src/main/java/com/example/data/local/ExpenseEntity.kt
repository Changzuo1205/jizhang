package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable

@Immutable
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val category: String,
    val subCategory: String = "",
    val amount: Double,
    val note: String = "",
    val dateTimestamp: Long = System.currentTimeMillis(),
    val accountId: Long = 1L,
    val accountName: String = "默认账户"
)
