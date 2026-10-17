package com.maohongyuan.magic.core.ai

import android.content.Context
import com.maohongyuan.magic.core.interpret.DivinationInterpreter
import com.maohongyuan.magic.core.interpret.EngineTier
import com.maohongyuan.magic.core.interpret.Interpretation
import com.maohongyuan.magic.core.interpret.InterpretationRequest
import com.maohongyuan.magic.core.interpret.InterpretationSection
import com.maohongyuan.magic.core.interpret.Tone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

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
 * ── 为什么不做 JSON 解析 ──
 * 让模型输出 JSON 看似更「结构化」，实际很脆：多一个小括号、多一句开场白，
 * 解析就失败，用户看到的是空白。而这里要的只是**一段话**。
 *
 * 所以：**判词与吉凶直接取自 [InterpretationRequest.facts]（算法算的，一定对）**，
 * 模型的输出整段作为「解读」正文。模型写崩了也不会影响判词。
 */
class OnDeviceInterpreter(
    private val backend: LocalModelBackend? = null,
) : DivinationInterpreter {

    override val tier: EngineTier = EngineTier.ON_DEVICE

    /** 模型文件存在就算可用。真正的加载推迟到第一次 [interpret] */
    override suspend fun isAvailable(): Boolean = backend != null

    override fun interpret(request: InterpretationRequest): Flow<Interpretation> = flow {
        val b = backend ?: return@flow

        val raw = b.generate(
            systemPrompt = ChartPrompt.SYSTEM_PROMPT,
            userMessage = ChartPrompt.userMessage(request),
        ) ?: return@flow

        val text = raw.trim()
        if (text.isEmpty()) return@flow

        // 判词、吉凶、用神说明一律取算法结果 —— 模型不参与判断
        val facts = request.facts
        emit(
            Interpretation(
                verdict = facts?.verdict ?: com.maohongyuan.magic.core.interpret.Verdict.NEUTRAL,
                headline = facts?.headline ?: "解读",
                sections = listOf(
                    InterpretationSection(
                        title = "解读",
                        body = text,
                        tone = Tone.NEUTRAL,
                    )
                ),
                tier = EngineTier.ON_DEVICE,
                yongShenNote = facts?.yongShenNote,
            )
        )
    }

    companion object {
        /**
         * 按当前设备装配。模型不存在时返回一个 `isAvailable() == false` 的实例，
         * 降级链会自动跳过它 —— 不需要调用方判断。
         */
        fun fromDevice(context: Context): OnDeviceInterpreter {
            val model = LocalModel.candidatesFor(DeviceProfile.current()).first()
            val file = File(ModelDownloader.modelDir(context.filesDir), model.fileName)
            return if (file.exists()) {
                OnDeviceInterpreter(LiteRtLmBackend(context.applicationContext, file))
            } else {
                OnDeviceInterpreter(null)
            }
        }
    }
}

/**
 * 端上推理后端的抽象。
 *
 * 抽出来是为了：接 LiteRT-LM 只改一个实现类，不动引擎逻辑；
 * 也方便在测试里塞一个假后端验证链路。
 */
interface LocalModelBackend {

    /** 运行时是否已就绪（模型已加载） */
    suspend fun isReady(): Boolean

    /**
     * 生成一段文本。
     *
     * @return 模型输出；失败返回 null（由调用方决定降级）
     */
    suspend fun generate(systemPrompt: String, userMessage: String): String?
}
