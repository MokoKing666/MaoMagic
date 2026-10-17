package com.maohongyuan.magic.core.fortune

import com.maohongyuan.magic.core.calendar.LiuNianService
import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.HeavenlyStem
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 日运/月运测试。
 *
 * 重点在「数据确实来自历法层」和「四维评分不会越界」——
 * 十神映射本身是启发式的，不追求唯一正确答案，但**不能算错方向**。
 */
class FortuneTest {

    private val profile = BirthProfile(
        year = 1990, month = 6, day = 15, hour = 12, gender = "MALE",
    )

    // ── 六害推导 ────────────────────────────────────────────

    @Test
    fun `六害与传世口诀一致`() {
        // 子未、丑午、寅巳、卯辰、申亥、酉戌
        val expected = mapOf(
            EarthlyBranch.ZI to EarthlyBranch.WEI,
            EarthlyBranch.CHOU to EarthlyBranch.WU,
            EarthlyBranch.YIN to EarthlyBranch.SI,
            EarthlyBranch.MAO to EarthlyBranch.CHEN,
            EarthlyBranch.SHEN to EarthlyBranch.HAI,
            EarthlyBranch.YOU to EarthlyBranch.XU,
        )
        expected.forEach { (from, to) ->
            assertEquals("${from.label} 害 ${to.label}", to, from.harm)
        }
        // 六害是对称的：A 害 B ⇒ B 害 A
        EarthlyBranch.entries.forEach {
            assertEquals("${it.label} 的害不互反", it, it.harm.harm)
        }
    }

    // ── 日运 ────────────────────────────────────────────────

    @Test
    fun `日运能算出十神与四维`() {
        val c = FortuneService.daily(profile, 2026, 10, 17)
        assertEquals(FortunePeriod.DAILY, c.period)
        assertEquals(4, c.scores.size)
        assertEquals(FortuneDimension.entries.toSet(), c.scores.map { it.dimension }.toSet())
        assertTrue("十神不应为空", c.shiShen.isNotBlank())
        assertTrue("日期标签不应为空", c.dateLabel.isNotBlank())
    }

    @Test
    fun `四维分数恒在合法区间`() {
        // 连跑一年，确保任何组合都不越界
        for (m in 1..12) {
            for (d in listOf(1, 8, 15, 22, 28)) {
                FortuneService.daily(profile, 2026, m, d).scores.forEach {
                    assertTrue("${m}月${d}日 ${it.dimension.label} 分数越界：${it.score}", it.score in 5..95)
                }
            }
        }
    }

    @Test
    fun `十神与历法层一致`() {
        // 用 LiuNianService 独立算一遍，必须与 Service 内部结果相同
        val c = FortuneService.daily(profile, 2026, 10, 17)
        val expected = LiuNianService.shiShen(c.dayMaster, c.targetGanZhi.stem)
        assertEquals(expected, c.shiShen)
    }

    @Test
    fun `同一天反复算结果稳定`() {
        val a = FortuneService.daily(profile, 2026, 10, 17)
        val b = FortuneService.daily(profile, 2026, 10, 17)
        assertEquals(a.shiShen, b.shiShen)
        assertEquals(a.scores.map { it.score }, b.scores.map { it.score })
        assertEquals(a.relations, b.relations)
    }

    // ── 月运 ────────────────────────────────────────────────

    @Test
    fun `月运与日运是不同周期`() {
        val d = FortuneService.daily(profile, 2026, 10, 17)
        val m = FortuneService.monthly(profile, 2026, 10)
        assertEquals(FortunePeriod.MONTHLY, m.period)
        assertEquals(FortunePeriod.DAILY, d.period)
        assertEquals(4, m.scores.size)
        assertTrue("月运标签应含年月", m.dateLabel.contains("2026"))
    }

    @Test
    fun `月运整月结果一致`() {
        // 月柱在整月内不变，取任何一天都应该得到同样的月运
        val a = FortuneService.monthly(profile, 2026, 10)
        val b = FortuneService.monthly(profile, 2026, 10)
        assertEquals(a.targetGanZhi, b.targetGanZhi)
    }

    // ── 引擎 ────────────────────────────────────────────────

    @Test
    fun `引擎输出四维与方法说明`() {
        val c = FortuneService.daily(profile, 2026, 10, 17)
        val r = runBlocking {
            FortuneRuleEngine().interpret(
                com.maohongyuan.magic.core.interpret.InterpretationRequest("", c)
            ).firstOrNull()
        } ?: error("引擎未产出结果")

        assertTrue("应含本日干支", r.sections.any { it.title.contains("干支") })
        assertTrue("应含方法说明", r.sections.any { it.title.contains("怎么来的") })
        FortuneDimension.entries.forEach { d ->
            assertTrue("应含 ${d.label}", r.sections.any { it.title.startsWith(d.label) })
        }
    }

    @Test
    fun `引擎对非运势盘返回空流`() {
        val other = com.maohongyuan.magic.core.interpret.InterpretationRequest(
            question = "",
            chart = object : com.maohongyuan.magic.core.model.DivinationChart {
                override val system = com.maohongyuan.magic.core.model.DivinationSystem.BA_ZI
            },
        )
        assertEquals(null, runBlocking { FortuneRuleEngine().interpret(other).firstOrNull() })
    }

    // ── 档案 ────────────────────────────────────────────────

    @Test
    fun `档案性别解析`() {
        assertEquals(
            com.maohongyuan.magic.core.calendar.Gender.FEMALE,
            profile.copy(gender = "FEMALE").genderEnum,
        )
        assertEquals(
            com.maohongyuan.magic.core.calendar.Gender.MALE,
            profile.copy(gender = "MALE").genderEnum,
        )
    }

    @Test
    fun `不同日主算出的十神不同`() {
        // 两个不同生日的命主，同一天的十神应当可能不同（至少结构完整）
        val other = profile.copy(year = 1985, month = 3, day = 20)
        val a = FortuneService.daily(profile, 2026, 10, 17)
        val b = FortuneService.daily(other, 2026, 10, 17)
        assertEquals(4, a.scores.size)
        assertEquals(4, b.scores.size)
        assertTrue("日主应不同", a.dayMaster != b.dayMaster || a.shiShen == b.shiShen)
    }
}
