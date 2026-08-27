package com.example

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 真实种子资产完整性断言（tools/seed_converter.py 的产出）。
 * 资源副本位于 app/src/test/resources/seed_transactions.json。
 */
class SeedFixtureIntegrityTest {

    private fun loadFixture(): JSONObject {
        val stream = javaClass.getResourceAsStream("/seed_transactions.json")
            ?: error("缺少测试资源 seed_transactions.json")
        return JSONObject(stream.bufferedReader().use { it.readText() })
    }

    @Test
    fun `种子总量与分布符合挖财真实样本画像`() {
        val root = loadFixture()
        val rows = root.getJSONArray("transactions")
        assertEquals(803, root.getJSONObject("meta").getInt("count"))
        assertEquals(803, rows.length())

        var expense = 0; var income = 0; var transfer = 0; var deleted = 0
        var badAmount = 0
        for (i in 0 until rows.length()) {
            val o = rows.getJSONObject(i)
            when (o.getString("type")) {
                "EXPENSE" -> expense++
                "INCOME" -> income++
                "TRANSFER" -> transfer++
            }
            if (o.getBoolean("isDeleted")) deleted++
            if (o.getInt("amount_cents") <= 0) badAmount++
        }
        assertEquals(724, expense)
        assertEquals(79, income)          // 含挖财资产调整按漏记款口径转化的行
        assertEquals(0, transfer)         // 全部调整行 counter_account 为空 → 无真转账
        assertEquals(4, deleted)
        assertEquals(0, badAmount)
    }

    @Test
    fun `全部 uuid 唯一且非空`() {
        val rows = loadFixture().getJSONArray("transactions")
        val seen = HashSet<String>()
        for (i in 0 until rows.length()) {
            val uuid = rows.getJSONObject(i).getString("uuid")
            assertTrue(uuid.isNotBlank())
            assertTrue("重复 uuid: $uuid", seen.add(uuid))
        }
        assertEquals(rows.length(), seen.size)
    }
}
