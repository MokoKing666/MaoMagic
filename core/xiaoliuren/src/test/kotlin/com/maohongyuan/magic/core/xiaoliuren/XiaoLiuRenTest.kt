package com.maohongyuan.magic.core.xiaoliuren

import com.maohongyuan.magic.core.model.EarthlyBranch
import kotlinx.coroutines.flow.firstOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 小六壬的算法测试。
 *
 * 重点不在「能跑」，而在**起数起点**：
 * 三次起数必须接着上一个落宫继续数。写成各自从大安起，结果会全错且不报错。
 */
class XiaoLiuRenTest {

    // ── 数据完整性 ──────────────────────────────────────────

    @Test
    fun `六宫恰好六个且不重复`() {
        val palaces = SixPalaceData.palaces
        assertEquals(6, palaces.size)
        assertEquals(6, palaces.map { it.name }.toSet().size)
        // 顺序必须固定：取模全靠它
        assertEquals(
            listOf("大安", "留连", "速喜", "赤口", "小吉", "空亡"),
            palaces.map { it.name },
        )
    }

    @Test
    fun `六宫断语都不为空`() {
        SixPalaceData.palaces.forEach {
            assertTrue("${it.name} 缺宫义", it.meaning.isNotBlank())
            assertTrue("${it.name} 缺断法", it.advice.isNotBlank())
            assertTrue("${it.name} 缺五行", it.wuXing.isNotBlank())
        }
    }

    // ── 锚点 ────────────────────────────────────────────────

    @Test
    fun `正月初一子时三宫都落大安`() {
        val c = XiaoLiuRenDivination.byTime(1, 1, EarthlyBranch.ZI)
        assertEquals("大安", c.first.name)
        assertEquals("大安", c.second.name)
        assertEquals("大安", c.third.name)
    }

    /**
     * 这条是本算法最容易写错的地方。
     *
     * 二月 → 月宫落「留连」（序号 1）。
     * 初一自**留连**起数 → 仍落留连。
     * 若误写成「自大安起初一」，就会落到大安 —— 差一宫，且不会报错。
     */
    @Test
    fun `初一起数接着月宫而不是从大安重来`() {
        val c = XiaoLiuRenDivination.byTime(2, 1, EarthlyBranch.ZI)
        assertEquals("留连", c.first.name)
        assertEquals("自留连起初一，仍应落留连", "留连", c.second.name)
        assertEquals("留连", c.third.name)
    }

    @Test
    fun `子时落日宫本身`() {
        // 子时是第一个时辰，自日宫起数应原地不动
        val c = XiaoLiuRenDivination.byTime(3, 5, EarthlyBranch.ZI)
        assertEquals(c.second.name, c.third.name)
    }

    @Test
    fun `丑时自日宫顺数一位`() {
        val zi = XiaoLiuRenDivination.byTime(3, 5, EarthlyBranch.ZI)
        val chou = XiaoLiuRenDivination.byTime(3, 5, EarthlyBranch.CHOU)
        val ziIdx = SixPalaceData.palaces.indexOfFirst { it.name == zi.third.name }
        val chouIdx = SixPalaceData.palaces.indexOfFirst { it.name == chou.third.name }
        assertEquals((ziIdx + 1) % 6, chouIdx)
    }

    // ── 循环 ────────────────────────────────────────────────

    @Test
    fun `月份满六循环回大安`() {
        // 正月与七月都落大安
        assertEquals("大安", XiaoLiuRenDivination.byTime(1, 1, EarthlyBranch.ZI).first.name)
        assertEquals("大安", XiaoLiuRenDivination.byTime(7, 1, EarthlyBranch.ZI).first.name)
    }

    @Test
    fun `三宫落点永远在六宫之内`() {
        val names = SixPalaceData.palaces.map { it.name }.toSet()
        for (m in 1..12) {
            for (d in 1..30) {
                val c = XiaoLiuRenDivination.byTime(m, d, EarthlyBranch.ZI)
                assertTrue("$m/$d 月宫越界", c.first.name in names)
                assertTrue("$m/$d 日宫越界", c.second.name in names)
                assertTrue("$m/$d 时宫越界", c.third.name in names)
            }
        }
    }

    // ── 报数 ────────────────────────────────────────────────

    @Test
    fun `报数与时间走同一套取模`() {
        // 报数 1,1,1 与 正月/初一/子时 应完全一致
        val byTime = XiaoLiuRenDivination.byTime(1, 1, EarthlyBranch.ZI)
        val byNum = XiaoLiuRenDivination.byNumbers(1, 1, 1)
        assertEquals(byTime.first.name, byNum.first.name)
        assertEquals(byTime.second.name, byNum.second.name)
        assertEquals(byTime.third.name, byNum.third.name)
    }

    @Test
    fun `报数也必须接着上一个落宫数`() {
        // 2,1,1 → 与 二月/初一/子时 同构
        val c = XiaoLiuRenDivination.byNumbers(2, 1, 1)
        assertEquals("留连", c.first.name)
        assertEquals("留连", c.second.name)
    }

    @Test
    fun `报数大于六也能取模`() {
        val a = XiaoLiuRenDivination.byNumbers(13, 1, 1)
        val b = XiaoLiuRenDivination.byNumbers(1, 1, 1)
        assertEquals(b.first.name, a.first.name)
    }

    @Test
    fun `非法输入被拒绝`() {
        assertTrue(runCatching { XiaoLiuRenDivination.byTime(0, 1, EarthlyBranch.ZI) }.isFailure)
        assertTrue(runCatching { XiaoLiuRenDivination.byTime(13, 1, EarthlyBranch.ZI) }.isFailure)
        assertTrue(runCatching { XiaoLiuRenDivination.byTime(1, 0, EarthlyBranch.ZI) }.isFailure)
        assertTrue(runCatching { XiaoLiuRenDivination.byNumbers(0, 1, 1) }.isFailure)
    }

    // ── 引擎 ────────────────────────────────────────────────

    @Test
    fun `引擎能产出解读且带三宫`() {
        val chart = XiaoLiuRenDivination.byTime(2, 1, EarthlyBranch.ZI, "问工作")
        val result = XiaoLiuRenRuleEngine().analyzeForTest(chart)
        assertNotNull(result)
        assertTrue("应包含三宫落点", result.sections.any { it.title == "三宫落点" })
        assertTrue("应包含结果宫", result.sections.any { it.title.startsWith("结果") })
        assertTrue("headline 应含结果宫名", result.headline.contains(chart.third.name))
    }

    @Test
    fun `引擎对非小六壬的盘返回空流`() {
        // 这是降级链的前提：取不到自己的盘就静默跳过，不能抛
        val other = com.maohongyuan.magic.core.interpret.InterpretationRequest(
            question = "测试",
            chart = FakeChart,
        )
        val flow = XiaoLiuRenRuleEngine().interpret(other)
        assertTrue(
            "应返回空流",
            kotlinx.coroutines.runBlocking { flow.firstOrNull() } == null,
        )
    }

    private object FakeChart : com.maohongyuan.magic.core.model.DivinationChart {
        override val system = com.maohongyuan.magic.core.model.DivinationSystem.BA_ZI
    }
}

/** 测试用：跑一次引擎并取首个结果 */
private fun XiaoLiuRenRuleEngine.analyzeForTest(chart: XiaoLiuRenChart) =
    kotlinx.coroutines.runBlocking {
        interpret(
            com.maohongyuan.magic.core.interpret.InterpretationRequest(
                question = chart.question,
                chart = chart,
            )
        ).firstOrNull()
    } ?: error("引擎未产出结果")
