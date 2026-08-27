package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable

/**
 * 资产账户数据实体 (Room Database Entity)
 *
 * 对应 SQLite 数据库中的 `accounts` 数据表，用于管理用户的各资产账户（微信钱包、支付宝、银行卡等）。
 *
 * @property id 唯一自增主键
 * @property name 账户名称（如："微信钱包"、"支付宝"、"招商银行储蓄卡"、"基金" 等）
 * @property type 账户类型类别标识，支持："WECHAT", "ALIPAY", "BANK_CARD", "CREDIT_CARD", "CASH", "INVESTMENT", "OTHER"
 * @property balance 账户初始基准余额
 * @property cardSuffix 银行卡或账号后四位尾号（可选）
 * @property colorHex 账户代表色 Hex 字符串（例如："#1677FF" 支付宝蓝，"#07C160" 微信绿）
 * @property note 账户备注说明
 */
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

