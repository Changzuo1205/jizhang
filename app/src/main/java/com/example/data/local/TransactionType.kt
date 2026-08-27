package com.example.data.local

/**
 * 交易类型。目标 schema 采用字符串枚举（取代挖财的数字 tradeType，见交接包 05-反模式 #13）。
 */
enum class TransactionType {
    /** 支出 */
    EXPENSE,

    /** 收入 */
    INCOME,

    /** 转账（账户之间资金划转，category_id 为空） */
    TRANSFER
}
