package com.maohongyuan.magic.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 五行与六亲。
 *
 * 这些测试**不重复实现算术**，而是把传统规定的关系逐条写死。
 * 因此它们能真正抓住「枚举顺序写错」这类错误 —— 如果顺序错了，
 * 算术仍然自洽，但会与下面的传统关系不符。
 */
class WuXingTest {

    @Test
    fun `相生关系与传统的五组完全一致`() {
        val expected = listOf(
            WuXing.MU to WuXing.HUO,    // 木生火
            WuXing.HUO to WuXing.TU,    // 火生土
            WuXing.TU to WuXing.JIN,    // 土生金
            WuXing.JIN to WuXing.SHUI,  // 金生水
            WuXing.SHUI to WuXing.MU,   // 水生木
        )
        expected.forEach { (a, b) ->
            assertEquals("$a 应生 $b", b, a.generates)
            assertEquals("$b 应由 $a 所生", a, b.generatedBy)
        }
    }

    @Test
    fun `相克关系与传统的五组完全一致`() {
        val expected = listOf(
            WuXing.MU to WuXing.TU,     // 木克土
            WuXing.TU to WuXing.SHUI,   // 土克水
            WuXing.SHUI to WuXing.HUO,  // 水克火
            WuXing.HUO to WuXing.JIN,   // 火克金
            WuXing.JIN to WuXing.MU,    // 金克木
        )
        expected.forEach { (a, b) ->
            assertEquals("$a 应克 $b", b, a.overcomes)
            assertEquals("$b 应被 $a 所克", a, b.overcomeBy)
        }
    }

    @Test
    fun `生克往返恒等式对全部五行成立`() {
        WuXing.entries.forEach { w ->
            assertEquals(w, w.generates.generatedBy)
            assertEquals(w, w.generatedBy.generates)
            assertEquals(w, w.overcomes.overcomeBy)
            assertEquals(w, w.overcomeBy.overcomes)
        }
    }

    @Test
    fun `任一五行的四个关系对象互不相同且都不是自身`() {
        WuXing.entries.forEach { w ->
            val others = listOf(w.generates, w.generatedBy, w.overcomes, w.overcomeBy)
            assertTrue("$w 的四个关系对象不应含自身", others.none { it == w })
            assertEquals("$w 的四个关系对象应互不相同", 4, others.toSet().size)
        }
    }

    @Test
    fun `生与克不重叠`() {
        WuXing.entries.forEach { w ->
            assertNotEquals("$w 的生与克不应是同一个", w.generates, w.overcomes)
        }
    }

    @Test
    fun `二元关系覆盖全部 25 种组合且与四个属性一致`() {
        WuXing.entries.forEach { self ->
            WuXing.entries.forEach { other ->
                val relation = WuXingRelation.between(self, other)
                val expected = when (other) {
                    self -> WuXingRelation.SAME
                    self.generates -> WuXingRelation.I_GENERATE
                    self.generatedBy -> WuXingRelation.IT_GENERATES
                    self.overcomes -> WuXingRelation.I_OVERCOME
                    else -> WuXingRelation.IT_OVERCOMES
                }
                assertEquals("$self / $other", expected, relation)
            }
        }
    }

    // ── 六亲 ──────────────────────────────────────────────────

    @Test
    fun `六亲推导符合传统规定`() {
        // 以「金」为卦宫五行（乾兑宫）
        assertEquals(LiuQin.FU_MU, LiuQin.of(WuXing.JIN, WuXing.TU))       // 土生金 → 父母
        assertEquals(LiuQin.XIONG_DI, LiuQin.of(WuXing.JIN, WuXing.JIN))   // 同金 → 兄弟
        assertEquals(LiuQin.GUAN_GUI, LiuQin.of(WuXing.JIN, WuXing.HUO))   // 火克金 → 官鬼
        assertEquals(LiuQin.QI_CAI, LiuQin.of(WuXing.JIN, WuXing.MU))      // 金克木 → 妻财
        assertEquals(LiuQin.ZI_SUN, LiuQin.of(WuXing.JIN, WuXing.SHUI))    // 金生水 → 子孙
    }

    @Test
    fun `六亲推导对全部宫位五行都恰好覆盖五种六亲`() {
        WuXing.entries.forEach { palace ->
            val qin = WuXing.entries.map { LiuQin.of(palace, it) }.toSet()
            assertEquals("宫位 $palace 应恰好推出五种六亲", 5, qin.size)
        }
    }

    @Test
    fun `天河水与乾宫金的六亲对得上`() {
        // 天风姤属乾宫（金），其六爻纳甲为 辛丑土 辛亥水 辛酉金 壬午火 壬申金 壬戌土
        val palace = WuXing.JIN
        val expected = listOf(
            LiuQin.FU_MU,     // 辛丑土
            LiuQin.ZI_SUN,    // 辛亥水
            LiuQin.XIONG_DI,  // 辛酉金
            LiuQin.GUAN_GUI,  // 壬午火
            LiuQin.XIONG_DI,  // 壬申金
            LiuQin.FU_MU,     // 壬戌土
        )
        val actual = listOf(
            EarthlyBranch.CHOU, EarthlyBranch.HAI, EarthlyBranch.YOU,
            EarthlyBranch.WU, EarthlyBranch.SHEN, EarthlyBranch.XU,
        ).map { LiuQin.of(palace, it.wuXing) }

        assertEquals(expected, actual)
        // 乾宫金缺「妻财」（我克者 = 木），故天风姤必须借伏神
        assertTrue("乾宫不应有妻财", actual.none { it == LiuQin.QI_CAI })
    }
}
