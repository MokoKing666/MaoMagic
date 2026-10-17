package com.maohongyuan.magic.core.interpret

import com.maohongyuan.magic.core.model.DivinationChart
import com.maohongyuan.magic.core.model.DivinationSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrictInterpreterSelectionTest {

    private class FakeChart : DivinationChart {
        override val system = DivinationSystem.LIU_YAO
    }

    private class FakeInterpreter(
        override val tier: EngineTier,
        private val available: Boolean = true,
        private val result: Interpretation? = null,
    ) : DivinationInterpreter {
        var calls = 0

        override suspend fun isAvailable() = available

        override fun interpret(request: InterpretationRequest): Flow<Interpretation> = flow {
            calls++
            result?.let { emit(it) }
        }
    }

    private fun result(tier: EngineTier, headline: String) = Interpretation(
        verdict = Verdict.NEUTRAL,
        headline = headline,
        sections = listOf(InterpretationSection("解读", headline)),
        tier = tier,
    )

    private fun request() = InterpretationRequest("测试", FakeChart())

    @Test
    fun `AUTO 仍会在端上失败后落到规则引擎`() = runBlocking {
        val local = FakeInterpreter(EngineTier.ON_DEVICE, result = null)
        val rule = FakeInterpreter(EngineTier.RULE, result = result(EngineTier.RULE, "规则结果"))

        val actual =             InterpreterChain(listOf(local, rule)).interpret(request(), preferred = null)

        assertEquals(EngineTier.RULE, actual.tier)
        assertEquals("规则结果", actual.headline)
    }

    @Test
    fun `手动选择端上模型时不得静默降级`() = runBlocking {
        val local = FakeInterpreter(EngineTier.ON_DEVICE, result = null)
        val cloud = FakeInterpreter(EngineTier.CLOUD_AI, result = result(EngineTier.CLOUD_AI, "云端结果"))
        val rule = FakeInterpreter(EngineTier.RULE, result = result(EngineTier.RULE, "规则结果"))

        val failure = runCatching {
            InterpreterChain(listOf(local, cloud, rule)).interpret(
                request(),
                preferred = EngineTier.ON_DEVICE,
            )
        }.exceptionOrNull()

        assertNotNull("手动选择端上模式应暴露失败", failure)
        assertTrue(failure!!.message.orEmpty().contains("未切换到其它模式"))
        assertEquals("不能偷偷调用云端", 0, cloud.calls)
        // 规则引擎只预先计算 facts，不作为最终结果返回。
        assertEquals("规则结果不得伪装成端上结果", 1, rule.calls)
    }

    @Test
    fun `手动选择不可用引擎时直接报错且不尝试其它档`() = runBlocking {
        val local = FakeInterpreter(EngineTier.ON_DEVICE, available = false)
        val rule = FakeInterpreter(EngineTier.RULE, result = result(EngineTier.RULE, "规则结果"))

        val failure = runCatching {
            InterpreterChain(listOf(local, rule)).interpret(
                request(),
                preferred = EngineTier.ON_DEVICE,
            )
        }.exceptionOrNull()

        assertNotNull(failure)
        assertTrue(failure!!.message.orEmpty().contains("本地模型"))
        assertEquals("不可用时不应跑规则结果", 1, rule.calls) // 仅预计算 facts
    }
}
