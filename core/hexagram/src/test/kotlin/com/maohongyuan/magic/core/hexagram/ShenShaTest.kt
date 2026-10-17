package com.maohongyuan.magic.core.hexagram

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 神煞测试。
 *
 * 期望值直接抄自 `docs/神煞取法.md` 的三合局表 —— 那张表是按「日支组」列的，
 * 与实现里「按十二长生位推」是两条独立路径。两者对上，才说明推法没写错。
 */
class ShenShaTest {

    /** 日支 → (驿马, 桃花, 华盖, 将星, 劫煞, 亡神) */
    private val table: Map<EarthlyBranch, List<EarthlyBranch>> = mapOf(
        // 申子辰 → 寅 酉 辰 子 巳 亥
        EarthlyBranch.SHEN to listOf(EarthlyBranch.YIN, EarthlyBranch.YOU, EarthlyBranch.CHEN, EarthlyBranch.ZI, EarthlyBranch.SI, EarthlyBranch.HAI),
        EarthlyBranch.ZI to listOf(EarthlyBranch.YIN, EarthlyBranch.YOU, EarthlyBranch.CHEN, EarthlyBranch.ZI, EarthlyBranch.SI, EarthlyBranch.HAI),
        EarthlyBranch.CHEN to listOf(EarthlyBranch.YIN, EarthlyBranch.YOU, EarthlyBranch.CHEN, EarthlyBranch.ZI, EarthlyBranch.SI, EarthlyBranch.HAI),
        // 寅午戌 → 申 卯 戌 午 亥 巳
        EarthlyBranch.YIN to listOf(EarthlyBranch.SHEN, EarthlyBranch.MAO, EarthlyBranch.XU, EarthlyBranch.WU, EarthlyBranch.HAI, EarthlyBranch.SI),
        EarthlyBranch.WU to listOf(EarthlyBranch.SHEN, EarthlyBranch.MAO, EarthlyBranch.XU, EarthlyBranch.WU, EarthlyBranch.HAI, EarthlyBranch.SI),
        EarthlyBranch.XU to listOf(EarthlyBranch.SHEN, EarthlyBranch.MAO, EarthlyBranch.XU, EarthlyBranch.WU, EarthlyBranch.HAI, EarthlyBranch.SI),
        // 巳酉丑 → 亥 午 丑 酉 寅 申
        EarthlyBranch.SI to listOf(EarthlyBranch.HAI, EarthlyBranch.WU, EarthlyBranch.CHOU, EarthlyBranch.YOU, EarthlyBranch.YIN, EarthlyBranch.SHEN),
        EarthlyBranch.YOU to listOf(EarthlyBranch.HAI, EarthlyBranch.WU, EarthlyBranch.CHOU, EarthlyBranch.YOU, EarthlyBranch.YIN, EarthlyBranch.SHEN),
        EarthlyBranch.CHOU to listOf(EarthlyBranch.HAI, EarthlyBranch.WU, EarthlyBranch.CHOU, EarthlyBranch.YOU, EarthlyBranch.YIN, EarthlyBranch.SHEN),
        // 亥卯未 → 巳 子 未 卯 申 寅
        EarthlyBranch.HAI to listOf(EarthlyBranch.SI, EarthlyBranch.ZI, EarthlyBranch.WEI, EarthlyBranch.MAO, EarthlyBranch.SHEN, EarthlyBranch.YIN),
        EarthlyBranch.MAO to listOf(EarthlyBranch.SI, EarthlyBranch.ZI, EarthlyBranch.WEI, EarthlyBranch.MAO, EarthlyBranch.SHEN, EarthlyBranch.YIN),
        EarthlyBranch.WEI to listOf(EarthlyBranch.SI, EarthlyBranch.ZI, EarthlyBranch.WEI, EarthlyBranch.MAO, EarthlyBranch.SHEN, EarthlyBranch.YIN),
    )

    private val order = listOf(
        ShenSha.YI_MA, ShenSha.TAO_HUA, ShenSha.HUA_GAI,
        ShenSha.JIANG_XING, ShenSha.JIE_SHA, ShenSha.WANG_SHEN,
    )

    @Test
    fun `十二支的三合神煞与表一致`() {
        table.forEach { (dayBranch, expected) ->
            val hits = ShenSha.of(HeavenlyStem.JIA, dayBranch).associate { it.shenSha to it.branch }
            order.forEachIndexed { i, sha ->
                assertEquals("日支 ${dayBranch.label} 的 ${sha.label} 不对", expected[i], hits[sha])
            }
        }
    }

    @Test
    fun `驿马必是日支三合局的冲`() {
        // 独立验证：驿马 = 局之冲，与「病地」推法互为佐证
        EarthlyBranch.entries.forEach { b ->
            val ma = ShenSha.of(HeavenlyStem.JIA, b).first { it.shenSha == ShenSha.YI_MA }.branch
            val chong = EarthlyBranch.entries[(b.ordinal + 6) % 12]
            // 只有局的首支（长生位）才直接相冲，其余同局者共享同一个驿马
            assertTrue("${b.label} 的驿马 ${ma.label} 与 ${chong.label} 不属同一局",
                sameGroup(ma, chong) || ma == chong)
        }
    }

    @Test
    fun `禄神与羊刃按日干取`() {
        val lu = ShenSha.of(HeavenlyStem.JIA, EarthlyBranch.ZI).first { it.shenSha == ShenSha.LU_SHEN }
        assertEquals(EarthlyBranch.YIN, lu.branch)          // 甲禄在寅

        val ren = ShenSha.of(HeavenlyStem.JIA, EarthlyBranch.ZI).first { it.shenSha == ShenSha.YANG_REN }
        assertEquals(EarthlyBranch.MAO, ren.branch)         // 甲刃在卯

        // 丙戊同禄（巳）
        assertEquals(EarthlyBranch.SI, ShenSha.of(HeavenlyStem.BING, EarthlyBranch.ZI).first { it.shenSha == ShenSha.LU_SHEN }.branch)
        assertEquals(EarthlyBranch.SI, ShenSha.of(HeavenlyStem.WU, EarthlyBranch.ZI).first { it.shenSha == ShenSha.LU_SHEN }.branch)
    }

    @Test
    fun `阴干不给羊刃`() {
        val yi = ShenSha.of(HeavenlyStem.YI, EarthlyBranch.ZI)
        assertTrue("阴干不应有羊刃", yi.none { it.shenSha == ShenSha.YANG_REN })
        // 但禄神仍要有
        assertTrue(yi.any { it.shenSha == ShenSha.LU_SHEN })
    }

    private fun sameGroup(a: EarthlyBranch, b: EarthlyBranch): Boolean {
        val groups = listOf(
            setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN),
            setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU),
            setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU),
            setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI),
        )
        return groups.any { a in it && b in it }
    }
}
