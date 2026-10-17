package com.maohongyuan.magic.core.ai

import com.maohongyuan.magic.core.interpret.DivinationInterpreter
import com.maohongyuan.magic.core.interpret.EngineTier
import com.maohongyuan.magic.core.interpret.Interpretation
import com.maohongyuan.magic.core.interpret.InterpretationRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 端上模型引擎（Tier 3）。
 *
 * ── 分工 ──
 * **它不负责算卦。** 用神、旺衰、旬空、动变爻全部由规则引擎先算好，
 * 通过 [InterpretationRequest.facts] 传进来；本引擎只把这些事实**说成人话**。
 *
 * 这条分工是硬性的：小参数量模型做多步五行生克推理并不可靠，
 * 但它把已经确定的事实写成通顺的中文，做得很好。
 *
 * ── 当前状态 ──
 * 推理后端尚未接入。[LocalModelBackend] 是一个空位 ——
 * 接入 LiteRT-LM（或其它端上推理运行时）后，把 [isAvailable] 改成
 * 「模型已下载且运行时初始化成功」，[interpret] 改成流式调用即可。
 *
 * 在那之前本引擎恒不可用，降级链会跳过它 —— 不会影响现有功能。
 */
class OnDeviceInterpreter(
    private val backend: LocalModelBackend? = null,
) : DivinationInterpreter {

    override val tier: EngineTier = EngineTier.ON_DEVICE

    override suspend fun isAvailable(): Boolean =
        backend?.isReady() == true

    override fun interpret(request: InterpretationRequest): Flow<Interpretation> = flow {
        val b = backend ?: return@flow
        if (!b.isReady()) return@flow

        val raw = b.generate(
            systemPrompt = ChartPrompt.SYSTEM_PROMPT,
            userMessage = ChartPrompt.userMessage(request),
        ) ?: return@flow

        // 端上模型输出的是自由文本，结构化靠 ChartPrompt 里约定的格式。
        // 解析失败就放弃 —— 让降级链继续往下走，不要产出半成品。
        val parsed = runCatching { parse(raw) }.getOrNull() ?: return@flow
        emit(parsed)
    }

    private fun parse(raw: String): Interpretation {
        // TODO: 等接入真实模型后再定输出格式（建议让模型输出 JSON）。
        // 现在抛出，使 flow 为空 → 降级链跳过本引擎。
        throw UnsupportedOperationException("端上模型输出解析尚未实现")
    }
}

/**
 * 端上推理后端的抽象。
 *
 * 抽出来是为了：接 LiteRT-LM 只改一个实现类，不动引擎逻辑；
 * 也方便在测试里塞一个假后端验证链路。
 */
interface LocalModelBackend {

    /** 模型已下载且运行时初始化成功 */
    suspend fun isReady(): Boolean

    /**
     * 生成一段文本。
     *
     * @return 模型输出；失败返回 null（由调用方决定降级）
     */
    suspend fun generate(systemPrompt: String, userMessage: String): String?
}
