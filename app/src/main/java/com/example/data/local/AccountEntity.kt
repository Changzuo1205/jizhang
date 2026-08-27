package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable

@Immutable
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "WECHAT", "ALIPAY", "BANK_CARD", "CREDIT_CARD", "CASH", "INVESTMENT", "OTHER"
    val balance: Double = 0.0,
    val cardSuffix: String = "",
    val colorHex: String = "#3B82F6",
    val note: String = ""
)
