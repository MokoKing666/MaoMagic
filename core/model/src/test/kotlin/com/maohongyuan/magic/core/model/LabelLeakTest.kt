package com.maohongyuan.magic.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 标签泄漏防护。
 *
 * 领域枚举的 `toString()` 默认返回**枚举常量名**（`TU` / `SHUI` / `QIAN`…）。
 * 这些枚举在 UI 里到处被写进字符串插值，一旦漏出去就是肉眼可见的错误 ——
 * 实测出现过黄历课盘显示「戊戌月 · TU旺」而不是「戊戌月 · 土旺」。
 *
 * 这组测试把「toString 必须等于中文标签」钉死，避免回归。
 */
class LabelLeakTest {

    @Test
    fun `五行与阴阳的 toString 返回中文`() {
        WuXing.entries.forEach { assertEquals(it.label, it.toString()) }
        YinYang.entries.forEach { assertEquals(it.label, it.toString()) }
        WuXingRelation.entries.forEach { assertEquals(it.label, it.toString()) }
    }

    @Test
    fun `天干地支的 toString 返回中文`() {
        HeavenlyStem.entries.forEach { assertEquals(it.label, it.toString()) }
        EarthlyBranch.entries.forEach { assertEquals(it.label, it.toString()) }
    }

    @Test
    fun `八卦与六亲六神的 toString 返回中文`() {
        Trigram.entries.forEach { assertEquals(it.label, it.toString()) }
        LiuQin.entries.forEach { assertEquals(it.label, it.toString()) }
        LiuShen.entries.forEach { assertEquals(it.label, it.toString()) }
    }

    @Test
    fun `爻位的 toString 返回中文序数`() {
        YaoPosition.entries.forEach { assertEquals(it.ordinalLabel, it.toString()) }
        assertEquals("初", YaoPosition.CHU.toString())
        assertEquals("上", YaoPosition.SHANG.toString())
    }

    @Test
    fun `toString 都不是枚举常量名`() {
        // 断言「返回值与常量名不同」—— 这条能抓住「忘了重写 toString」的新枚举
        WuXing.entries.forEach { assertNotEquals(it.name, it.toString()) }
        YinYang.entries.forEach { assertNotEquals(it.name, it.toString()) }
        WuXingRelation.entries.forEach { assertNotEquals(it.name, it.toString()) }
        HeavenlyStem.entries.forEach { assertNotEquals(it.name, it.toString()) }
        EarthlyBranch.entries.forEach { assertNotEquals(it.name, it.toString()) }
        Trigram.entries.forEach { assertNotEquals(it.name, it.toString()) }
        LiuQin.entries.forEach { assertNotEquals(it.name, it.toString()) }
        LiuShen.entries.forEach { assertNotEquals(it.name, it.toString()) }
        YaoPosition.entries.forEach { assertNotEquals(it.name, it.toString()) }
    }

    @Test
    fun `字符串插值不会漏出 ASCII 常量名`() {
        // 模拟真实场景：把枚举直接插进字符串
        val text = "月建 ${EarthlyBranch.XU}月 · ${EarthlyBranch.XU.wuXing}旺"
        assertEquals("月建 戌月 · 土旺", text)
        assertTrue("不应含 ASCII 字母", text.none { it in 'A'..'Z' })

        val text2 = "日辰 ${GanZhi(59)}日 · ${GanZhi(59).branch.wuXing}"
        assertEquals("日辰 癸亥日 · 水", text2)
        assertTrue(text2.none { it in 'A'..'Z' })

        val text3 = "体 ${Trigram.DUI} 用 ${Trigram.LI}"
        assertEquals("体 兑 用 离", text3)
    }
}
