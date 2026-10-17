package com.maohongyuan.magic.core.ai

import com.maohongyuan.magic.core.model.DivinationChart
import com.maohongyuan.magic.core.interpret.Interpretation
import com.maohongyuan.magic.core.interpret.InterpretationRequest
import com.maohongyuan.magic.core.interpret.InterpretationSection
import com.maohongyuan.magic.core.interpret.Tone
import com.maohongyuan.magic.core.interpret.Verdict
import com.maohongyuan.magic.core.model.DivinationSystem
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 端上模型输出解析。
 *
 * 这是最容易出问题的一环：模型输出的是自由文本，可能包 JSON、可能加前言、
 * 可能残缺。解析错了用户就看到花括号。
 */
class OnDeviceParseTest {

    private class FakeChart : DivinationChart {
        override val system: DivinationSystem = DivinationSystem.LIU_YAO
    }

    private class FakeBackend(private val output: String?) : LocalModelBackend {
        override suspend fun isReady() = true
        override suspend fun generate(systemPrompt: String, userMessage: String) = output
    }

    private fun run(output: String?, facts: Interpretation? = null): Interpretation? = runBlocking {
        OnDeviceInterpreter(FakeBackend(output))
            .interpret(InterpretationRequest(question = "测试", chart = FakeChart(), facts = facts))
            .firstOrNull()
    }

    @Test
    fun `标准 JSON 被解析成结构化段落`() {
        val json = """
            {"verdict":"平","headline":"模型写的判词","sections":[
              {"title":"用神","body":"用神取官鬼。","tone":"NEUTRAL"},
              {"title":"旺衰","body":"得月建生扶。","tone":"POSITIVE"}
            ]}
        """.trimIndent()
        val r = run(json)!!
        assertEquals(2, r.sections.size)
        assertEquals("用神", r.sections[0].title)
        assertEquals(Tone.POSITIVE, r.sections[1].tone)
    }

    @Test
    fun `判词与吉凶取算法结果，不取模型的`() {
        val json = """{"verdict":"吉","headline":"模型说很好","sections":[{"title":"x","body":"y"}]}"""
        val facts = Interpretation(
            verdict = Verdict.INAUSPICIOUS,
            headline = "算法判为凶",
            sections = emptyList(),
            tier = com.maohongyuan.magic.core.interpret.EngineTier.RULE,
        )
        val r = run(json, facts)!!
        // 模型说「吉」，但算法判「凶」—— 以算法为准
        assertEquals(Verdict.INAUSPICIOUS, r.verdict)
        assertEquals("算法判为凶", r.headline)
    }

    @Test
    fun `JSON 前后有前言也能解析`() {
        val out = """好的，以下是解读：
            {"sections":[{"title":"结论","body":"宜守。"}]}
            希望有帮助。"""
        val r = run(out)!!
        assertEquals(1, r.sections.size)
        assertEquals("结论", r.sections[0].title)
    }

    @Test
    fun `像 JSON 但残缺时降级而不是把花括号显示给用户`() {
        // 缺右括号 —— 解析必失败。此时不能把原文当正文渲染
        assertNull(run("""{"sections":[{"title":"x","body":"y"}"""))
    }

    @Test
    fun `纯文本输出会被清洗`() {
        val r = run("## 解读\n**用神有力**，可进。\n- 第一条\n- 第二条")!!
        assertEquals(1, r.sections.size)
        val body = r.sections[0].body
        assertTrue("不应残留井号：$body", !body.contains("#"))
        assertTrue("不应残留加粗：$body", !body.contains("**"))
        assertTrue("列表应转成·：$body", body.contains("·"))
    }

    @Test
    fun `空输出不产出结果`() {
        assertNull(run(""))
        assertNull(run("   \n  "))
        assertNull(run(null))
    }

    @Test
    fun `sections 为空数组时视为解析失败`() {
        assertNull(run("""{"sections":[]}"""))
    }
}
