package com.example.model

import kotlin.math.abs

/**
 * 金额唯一换算入口。
 *
 * 存储层一律使用 Int 分（避免浮点误差），UI 层使用 Double 元；
 * 所有双向换算必须经过本对象，禁止散落手写 `* 100` / `/ 100`。
 */
object AmountFormatter {

    /** 元 → 分（十进制 HALF_UP；经 BigDecimal 规避二进制浮点尾差，如 1.005→101 分） */
    fun yuanToCents(yuan: Double): Int {
        val scaled = java.math.BigDecimal(yuan.toString())
            .multiply(java.math.BigDecimal(100))
            .setScale(0, java.math.RoundingMode.HALF_UP)
        return scaled.toInt().coerceIn(Int.MIN_VALUE / 4, Int.MAX_VALUE / 4)
    }

    /** 分 → 元 */
    fun centsToYuan(cents: Int): Double = cents / 100.0

    /**
     * 千分位两位小数格式化（中文场景默认 Locale.CHINA）。
     * @param cents 金额（分），正负皆可
     */
    fun formatCentsAsYuan(cents: Int, withThousandsSeparator: Boolean = true): String {
        val formatted = if (withThousandsSeparator) {
            String.format(java.util.Locale.CHINA, "%,.2f", centsToYuan(cents))
        } else {
            String.format(java.util.Locale.CHINA, "%.2f", centsToYuan(cents))
        }
        return formatted
    }

    /** UI 展示型符号：支出 −、收入 +、转账无符号（供过渡期 UI 复用） */
    fun signFor(type: String): String = when (type) {
        "EXPENSE" -> "-"
        "INCOME" -> "+"
        else -> ""
    }

    /** 绝对值展示辅助（配合 signFor） */
    fun absYuanText(cents: Int, type: String): String =
        "${signFor(type)}¥${formatCentsAsYuan(abs(cents))}"
}
