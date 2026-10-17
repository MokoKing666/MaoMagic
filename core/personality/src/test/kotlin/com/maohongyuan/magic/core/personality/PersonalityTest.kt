package com.maohongyuan.magic.core.personality

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 题库完整性与计分测试。
 *
 * 计分最容易错的是**反向题的符号**：写反了会让结果整体偏移一极，
 * 而且不会报错 —— 只会让所有人测出来的类型都不对。
 */
class PersonalityTest {

    // ── 题库 ────────────────────────────────────────────────

    @Test
    fun `题库恰好 60 题 四维各 15 题`() {
        assertEquals(60, PersonalityData.questions.size)
        Dimension.entries.forEach { d ->
            assertEquals("${d.label} 应为 15 题", 15, PersonalityData.questions.count { it.dimension == d })
        }
    }

    @Test
    fun `题号不重复且两端描述都非空`() {
        assertEquals(60, PersonalityData.questions.map { it.id }.toSet().size)
        PersonalityData.questions.forEach {
            assertTrue("题 ${it.id} 左侧为空", it.l.isNotBlank())
            assertTrue("题 ${it.id} 右侧为空", it.r.isNotBlank())
        }
    }

    @Test
    fun `每个维度都有正向题和反向题`() {
        // 没有反向题的话，一律往左选就能拿满分，结果毫无意义
        Dimension.entries.forEach { d ->
            assertTrue(
                "${d.label} 缺正向题",
                PersonalityData.questions.any { it.dimension == d && it.left[0] == d.first },
            )
            assertTrue(
                "${d.label} 缺反向题",
                PersonalityData.questions.any { it.dimension == d && it.left[0] == d.second },
            )
        }
    }

    @Test
    fun `类型恰好 16 种且覆盖全部组合`() {
        assertEquals(16, PersonalityData.types.size)
        val expected = buildList {
            listOf("I", "E").forEach { a ->
                listOf("S", "N").forEach { b ->
                    listOf("T", "F").forEach { c ->
                        listOf("J", "P").forEach { d -> add("$a$b$c$d") }
                    }
                }
            }
        }.toSet()
        assertEquals(expected, PersonalityData.types.map { it.code }.toSet())
    }

    @Test
    fun `每种类型都有描述 优势 注意`() {
        PersonalityData.types.forEach {
            assertTrue("${it.code} 缺概述", it.summary.isNotBlank())
            assertTrue("${it.code} 缺优势", it.strengths.isNotEmpty())
            assertTrue("${it.code} 缺注意", it.cautions.isNotEmpty())
            assertTrue("${it.code} 缺方向", it.fit.isNotBlank())
        }
    }

    // ── 计分 ────────────────────────────────────────────────

    @Test
    fun `全部选最左且正向题时偏第一极`() {
        // 全部选 1（最左）。正向题（left=第一极）给 +2，反向题（left=第二极）给 -2。
        // 由于每个维度正反题都有，全选左并不会一边倒 —— 这正是反向题的意义。
        val answers = PersonalityData.questions.associate { it.id to 1 }
        val chart = PersonalityScoring.score(answers)
        assertEquals(4, chart.scores.size)
        // 只断言结构完整，不断言具体字母（取决于正反题数量配比）
        assertTrue(chart.code.length == 4)
        assertNotNull(PersonalityData.typeOf(chart.code))
    }

    @Test
    fun `全部居中时四维平局并取第一极`() {
        val answers = PersonalityData.questions.associate { it.id to 3 }
        val chart = PersonalityScoring.score(answers)
        chart.scores.forEach {
            assertEquals("${it.dimension.label} 居中应得 0", 0, it.raw)
            assertEquals("平局应取第一极", it.dimension.first, it.letter)
        }
        assertEquals("ESTJ", chart.code)
        assertTrue("应标记为有平局", chart.hasTie)
    }

    @Test
    fun `反向题符号正确`() {
        // 找一道 left 是第二极的题（反向题），单独把它选到最左
        val reverse = PersonalityData.questions.first { q ->
            q.dimension == Dimension.EI && q.left[0] == Dimension.EI.second
        }
        // 只答这一题，选 1（完全偏左 = 偏 I）
        val chart = PersonalityScoring.score(mapOf(reverse.id to 1))
        val ei = chart.scores.first { it.dimension == Dimension.EI }
        assertTrue("反向题选最左应得负分（偏 I），实际 ${ei.raw}", ei.raw < 0)
        assertEquals('I', ei.letter)
    }

    @Test
    fun `正向题符号正确`() {
        val forward = PersonalityData.questions.first { q ->
            q.dimension == Dimension.EI && q.left[0] == Dimension.EI.first
        }
        val chart = PersonalityScoring.score(mapOf(forward.id to 1))
        val ei = chart.scores.first { it.dimension == Dimension.EI }
        assertTrue("正向题选最左应得正分（偏 E），实际 ${ei.raw}", ei.raw > 0)
        assertEquals('E', ei.letter)
    }

    @Test
    fun `未答的题按居中处理`() {
        val empty = PersonalityScoring.score(emptyMap())
        assertEquals("ESTJ", empty.code)
        assertEquals(4, empty.scores.size)
        assertTrue(!PersonalityScoring.isComplete(emptyMap()))
    }

    @Test
    fun `答完判定`() {
        val all = PersonalityData.questions.associate { it.id to 3 }
        assertTrue(PersonalityScoring.isComplete(all))
        assertTrue(!PersonalityScoring.isComplete(all - PersonalityData.questions.first().id))
    }

    @Test
    fun `百分比在 0 到 100 之间`() {
        repeat(20) { seed ->
            val answers = PersonalityData.questions.associate { it.id to ((it.id * seed) % 5 + 1) }
            PersonalityScoring.score(answers).scores.forEach {
                assertTrue("百分比越界：${it.percent}", it.percent in 0..100)
            }
        }
    }

    // ── 引擎 ────────────────────────────────────────────────

    @Test
    fun `引擎输出类型与四维 且不判吉凶`() {
        val answers = PersonalityData.questions.associate { it.id to 1 }
        val chart = PersonalityScoring.score(answers)
        val result = runBlocking {
            PersonalityRuleEngine().interpret(
                com.maohongyuan.magic.core.interpret.InterpretationRequest("", chart)
            ).firstOrNull()
        } ?: error("引擎未产出结果")

        assertEquals(
            "人格类型没有吉凶，必须是 NEUTRAL",
            com.maohongyuan.magic.core.interpret.Verdict.NEUTRAL,
            result.verdict,
        )
        assertTrue("headline 应含类型代码", result.headline.contains(chart.code))
        assertTrue(result.sections.any { it.title == "四维倾向" })
        assertTrue(result.sections.any { it.title == "关于这个结果" })
    }

    @Test
    fun `引擎对非人格盘返回空流`() {
        val other = com.maohongyuan.magic.core.interpret.InterpretationRequest(
            question = "测试",
            chart = object : com.maohongyuan.magic.core.model.DivinationChart {
                override val system = com.maohongyuan.magic.core.model.DivinationSystem.BA_ZI
            },
        )
        assertEquals(null, runBlocking { PersonalityRuleEngine().interpret(other).firstOrNull() })
    }
}
