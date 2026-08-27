package com.example

import com.example.model.AmountFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 金额分↔元换算契约测试（交接包 05-反模式 #3：杜绝 Double 误差）。
 */
class AmountFormatterTest {

    @Test
    fun `yuanToCents 四舍五入边界`() {
        assertEquals(699, AmountFormatter.yuanToCents(6.99))
        assertEquals(100, AmountFormatter.yuanToCents(1.0))
        assertEquals(1, AmountFormatter.yuanToCents(0.01))
        // 银行家讨厌的 .005 边界按 ROUND_HALF_UP 处理
        assertEquals(101, AmountFormatter.yuanToCents(1.005))
    }

    @Test
    fun `分累加后换算无浮点漂移`() {
        val centsList = listOf(3, 3, 7, 33)   // 0.03+0.03+0.07+0.33 = 0.46
        val sumCents = centsList.sum()
        assertEquals(46, sumCents)
        assertEquals("0.46", AmountFormatter.centsToYuan(sumCents).toString().take(4))
    }

    @Test
    fun `负数与符号`() {
        assertEquals(-1250, AmountFormatter.yuanToCents(-12.50))
        assertEquals("-", AmountFormatter.signFor("EXPENSE"))
        assertEquals("+", AmountFormatter.signFor("INCOME"))
        assertEquals("", AmountFormatter.signFor("TRANSFER"))
    }
}
