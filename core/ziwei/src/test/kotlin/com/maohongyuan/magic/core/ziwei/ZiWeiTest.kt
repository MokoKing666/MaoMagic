package com.maohongyuan.magic.core.ziwei

import com.maohongyuan.magic.core.calendar.Gender
import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 紫微斗数测试。
 *
 * ── 为什么重点测不变量而不是逐星对照 ──
 * 安星诀各派略有出入，没有权威测试集时，逐星断言等于把我的理解当成标准。
 * 但**结构上的不变量必须成立**：14 主星不重不漏、紫微与天府对称、
 * 十二宫齐备。这些一旦破了，盘一定是错的。
 *
 * 起紫微有明确的传世星表可对，所以那部分做精确锚点。
 */
class ZiWeiTest {

    private fun chartOf(y: Int, m: Int, d: Int, h: Int = 12) =
        ZiWeiService.chart(y, m, d, h, gender = Gender.MALE)

    // ── 起紫微：对传世星表 ──────────────────────────────────

    @Test
    fun `水二局初一至初四的紫微位置`() {
        // 传世星表：水二局 初一丑、初二寅、初三寅、初四卯
        val zi = EarthlyBranch.ZI.ordinal   // 0
        assertEquals("初一", zi + 1, ZiWeiService.ziWeiIndex(2, 1))   // 丑
        assertEquals("初二", zi + 2, ZiWeiService.ziWeiIndex(2, 2))   // 寅
        assertEquals("初三", zi + 2, ZiWeiService.ziWeiIndex(2, 3))   // 寅
        assertEquals("初四", zi + 3, ZiWeiService.ziWeiIndex(2, 4))   // 卯
    }

    @Test
    fun `木三局初一在辰`() {
        assertEquals(EarthlyBranch.CHEN.ordinal, ZiWeiService.ziWeiIndex(3, 1))
    }

    @Test
    fun `金四局初一在亥`() {
        assertEquals(EarthlyBranch.HAI.ordinal, ZiWeiService.ziWeiIndex(4, 1))
    }

    @Test
    fun `起紫微结果恒在十二支内`() {
        for (ju in 2..6) {
            for (day in 1..30) {
                val idx = ZiWeiService.ziWeiIndex(ju, day)
                assertTrue("局$ju 日$day 越界：$idx", idx in 0..11)
            }
        }
    }

    @Test
    fun `非法输入被拒`() {
        assertTrue(runCatching { ZiWeiService.ziWeiIndex(1, 1) }.isFailure)
        assertTrue(runCatching { ZiWeiService.ziWeiIndex(7, 1) }.isFailure)
        assertTrue(runCatching { ZiWeiService.ziWeiIndex(2, 0) }.isFailure)
        assertTrue(runCatching { ZiWeiService.ziWeiIndex(2, 31) }.isFailure)
    }

    // ── 五行局 ──────────────────────────────────────────────

    @Test
    fun `五行局落在五个合法值内`() {
        for (gan in HeavenlyStem.entries) {
            for (idx in 0..11) {
                val ju = ZiWeiService.wuXingJuOf(gan, idx)
                assertTrue("${gan.label} 命宫在${idx} 得局 ${ju.ju}", ju.ju in 2..6)
            }
        }
    }

    // ── 盘的结构不变量 ──────────────────────────────────────

    @Test
    fun `十四主星不重不漏`() {
        // 连测一年中的多个日子，任何一个盘都必须是 14 颗
        for (m in 1..12) {
            val c = chartOf(1990, m, 15)
            assertEquals("${m}月盘主星数不对", 14, c.mainStarCount)
            val names = c.palaces.flatMap { it.starNames }
            assertEquals("主星有重复", 14, names.toSet().size)
            assertEquals(ZiWeiService.MAIN_STAR_ORDER.toSet(), names.toSet())
        }
    }

    @Test
    fun `十二宫齐备且宫名不重复`() {
        val c = chartOf(1990, 6, 15)
        assertEquals(12, c.palaces.size)
        assertEquals(12, c.palaces.map { it.name }.toSet().size)
        assertEquals(PalaceName.entries.toSet(), c.palaces.map { it.name }.toSet())
        // 地支按子丑寅…顺序排
        assertEquals(EarthlyBranch.entries.toList(), c.palaces.map { it.branch })
    }

    @Test
    fun `命宫那一宫的名字就是命宫`() {
        val c = chartOf(1990, 6, 15)
        val ming = c.palaces.first { it.branch == c.mingPalaceBranch }
        assertEquals(PalaceName.MING, ming.name)
        assertNotNull(c.palaceOf(PalaceName.MING))
    }

    @Test
    fun `紫微与天府关于寅申轴对称`() {
        for (m in 1..12) {
            for (d in listOf(1, 5, 10, 15, 20, 25, 28)) {
                val c = chartOf(1990, m, d)
                val ziWei = c.ziWeiBranch.ordinal
                // 直接算：天府 = (2*寅 - 紫微) mod 12
                val expectTianFu = (2 * 2 - ziWei).mod(12)
                val actualTianFu = c.palaces.first { "天府" in it.starNames }.branch.ordinal
                assertEquals("${m}月${d}日 天府位置不对", expectTianFu, actualTianFu)
            }
        }
    }

    @Test
    fun `紫微星确实落在紫微宫位`() {
        val c = chartOf(1990, 6, 15)
        val palace = c.palaces.first { "紫微" in it.starNames }
        assertEquals(c.ziWeiBranch, palace.branch)
    }

    // ── 四化 ────────────────────────────────────────────────

    @Test
    fun `四化只落在主星上且不超过四个`() {
        for (m in 1..12) {
            val c = chartOf(1990, m, 15)
            val huas = c.palaces.flatMap { it.stars }.filter { it.hua != null }
            // 戊己辛壬各有一化落辅星，本版未安辅星，所以最多 4 个、可能更少
            assertTrue("四化超过四个", huas.size <= 4)
            huas.forEach {
                assertTrue("${it.name} 不是主星", it.name in ZiWeiService.MAIN_STAR_ORDER)
                assertTrue("化${it.hua} 不合法", it.hua in listOf("禄", "权", "科", "忌"))
            }
            assertEquals("四化有重复", huas.size, huas.map { it.hua }.toSet().size)
        }
    }

    @Test
    fun `同一天反复排盘结果一致`() {
        val a = chartOf(1990, 6, 15)
        val b = chartOf(1990, 6, 15)
        assertEquals(a.ziWeiBranch, b.ziWeiBranch)
        assertEquals(a.mingPalaceBranch, b.mingPalaceBranch)
        assertEquals(a.wuXingJu, b.wuXingJu)
        assertEquals(
            a.palaces.map { p -> p.branch to p.starNames },
            b.palaces.map { p -> p.branch to p.starNames },
        )
    }

    @Test
    fun `不同生辰排出不同的盘`() {
        val a = chartOf(1990, 6, 15)
        val b = chartOf(1985, 3, 20)
        assertTrue(
            "不同生辰的紫微位置应可能不同",
            a.ziWeiBranch != b.ziWeiBranch || a.mingPalaceBranch != b.mingPalaceBranch,
        )
    }
}
