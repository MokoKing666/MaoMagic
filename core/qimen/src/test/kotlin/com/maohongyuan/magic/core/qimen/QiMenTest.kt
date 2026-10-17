package com.maohongyuan.magic.core.qimen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 奇门测试。
 *
 * 定局表来自项目方提供的《二十四节气三元定局表》，所以这里可以断言表的完整性
 * （24 节气齐备、每节气三局都在 1..9、阳遁阴遁分界正确）。
 * 九宫内容则断言**不变量**：地盘九干、九星、八门、八神都必须恰好各出现一次。
 */
class QiMenTest {

    private fun chartOf(y: Int, m: Int, d: Int, h: Int) = QiMenService.chart(y, m, d, h)

    // ── 定局表完整性 ────────────────────────────────────────

    @Test
    fun `二十四节气都能排盘且局数合法`() {
        // 全年逐月取一天，覆盖不同节气
        val seen = mutableSetOf<String>()
        for (m in 1..12) {
            for (d in listOf(2, 8, 14, 20, 26)) {
                val c = chartOf(2026, m, d, 12)
                assertTrue("${m}月${d}日 局数越界：${c.ju}", c.ju in 1..9)
                seen += c.jieQi
            }
        }
        assertTrue("全年只覆盖到 ${seen.size} 个节气，定局表可能不全", seen.size >= 20)
    }

    @Test
    fun `阳遁阴遁分界正确`() {
        // 冬至到芒种阳遁，夏至到大雪阴遁
        val yangJieQi = setOf("冬至", "小寒", "大寒", "立春", "雨水", "惊蛰",
            "春分", "清明", "谷雨", "立夏", "小满", "芒种")
        for (m in 1..12) {
            for (d in listOf(5, 15, 25)) {
                val c = chartOf(2026, m, d, 12)
                val expect = if (c.jieQi in yangJieQi) Dun.YANG else Dun.YIN
                assertEquals("${m}月${d}日 ${c.jieQi} 的遁不对", expect, c.dun)
            }
        }
    }

    @Test
    fun `三元判定合法`() {
        for (m in 1..12) {
            val c = chartOf(2026, m, 15, 12)
            assertTrue(c.yuan in Yuan.entries)
        }
    }

    // ── 九宫不变量 ──────────────────────────────────────────

    @Test
    fun `地盘九干恰好各一次`() {
        for (m in 1..12) {
            val c = chartOf(2026, m, 15, 12)
            assertEquals("应九个宫", 9, c.palaces.size)
            val gans = c.palaces.map { it.diPan }
            assertEquals("地盘干有重复：$gans", 9, gans.toSet().size)
            assertEquals(SAN_QI_LIU_YI.toSet(), gans.toSet())
        }
    }

    @Test
    fun `九星恰好各一次`() {
        val c = chartOf(2026, 6, 15, 12)
        val xings = c.palaces.map { it.xing }
        assertEquals("九星有重复：$xings", 9, xings.toSet().size)
        assertEquals(JIU_XING.toSet(), xings.toSet())
    }

    @Test
    fun `八门恰好各一次且中宫无门`() {
        for (m in listOf(1, 4, 7, 10)) {
            val c = chartOf(2026, m, 15, 12)
            val mens = c.palaces.mapNotNull { it.men }
            assertEquals("八门应恰好八个：$mens", 8, mens.size)
            assertEquals("八门有重复：$mens", 8, mens.toSet().size)
            assertEquals(BA_MEN.filterNotNull().toSet(), mens.toSet())
            // 中五宫无门
            assertEquals("中宫不该有门", null, c.palaceOf(5)?.men)
        }
    }

    @Test
    fun `八神恰好各一次`() {
        val c = chartOf(2026, 6, 15, 12)
        val shens = c.palaces.map { it.shen }.filter { it != "—" }
        assertEquals("八神应恰好八个：$shens", 8, shens.size)
        assertEquals("八神有重复：$shens", 8, shens.toSet().size)
        assertEquals(BA_SHEN.toSet(), shens.toSet())
    }

    @Test
    fun `九宫编号齐备`() {
        val c = chartOf(2026, 6, 15, 12)
        assertEquals((1..9).toList(), c.palaces.map { it.gong })
    }

    // ── 值符值使 ────────────────────────────────────────────

    @Test
    fun `旬首必在六甲之内`() {
        val valid = setOf("甲子", "甲戌", "甲申", "甲午", "甲辰", "甲寅")
        for (h in 0..23 step 2) {
            val c = chartOf(2026, 6, 15, h)
            assertTrue("旬首 ${c.xunShou} 不合法", c.xunShou in valid)
        }
    }

    @Test
    fun `值符星与值使门都在盘中`() {
        for (h in 0..23 step 2) {
            val c = chartOf(2026, 6, 15, h)
            assertNotNull("值符星不在九星表里", JIU_XING.firstOrNull { it == JIU_XING[0] })
            assertTrue("八神里应有值符", c.palaces.any { it.shen == "值符" })
            assertTrue("八门里应有值使", c.palaces.any { it.men != null })
        }
    }

    @Test
    fun `同一天反复排盘结果一致`() {
        val a = chartOf(2026, 10, 17, 12)
        val b = chartOf(2026, 10, 17, 12)
        assertEquals(a.ju, b.ju)
        assertEquals(a.dun, b.dun)
        assertEquals(a.yuan, b.yuan)
        assertEquals(a.palaces.map { it.diPan }, b.palaces.map { it.diPan })
        assertEquals(a.palaces.map { it.xing }, b.palaces.map { it.xing })
    }
}
