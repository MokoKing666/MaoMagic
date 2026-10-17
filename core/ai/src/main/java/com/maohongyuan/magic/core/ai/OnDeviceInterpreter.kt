package com.maohongyuan.magic.core.ai

import android.content.Context
import android.util.Log
import com.maohongyuan.magic.core.interpret.DivinationInterpreter
import com.maohongyuan.magic.core.interpret.EngineTier
import com.maohongyuan.magic.core.interpret.Interpretation
import com.maohongyuan.magic.core.interpret.InterpretationRequest
import com.maohongyuan.magic.core.interpret.InterpretationSection
import com.maohongyuan.magic.core.interpret.Tone
import com.maohongyuan.magic.core.interpret.Verdict
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * 端上模型引擎（Tier 3）。
 *
 * ── 分工 ──
 * **它不负责算卦。** 用神、旺衰、旬空、动变爻全部由规则引擎先算好，
 * 通过 [InterpretationRequest.facts] 传进来；本引擎只把这些事实**说成人话**。
 *
 * ── 输出解析：先试 JSON，失败退纯文本 ──
 * `ChartPrompt.SYSTEM_PROMPT` 是给云端写的，里面**要求输出 JSON**。
 * 端上模型照做，所以拿到的通常是：
 * ```
 * {"verdict":"…","headline":"…","sections":[{"title":"…","body":"…","tone":"…"}]}
 * ```
 * 直接当文本渲染，用户会看到一坨花括号 —— 之前就是这个 bug。
 *
 * 但也不能假定一定能解析：小模型偶尔会加前言、漏括号。
 * 所以**先试 JSON，成功就用结构化段落，失败则把原文压成纯文本**，
 * 两者都不行就返回空 Flow 让降级链落到规则引擎。
 *
 * ── 判词不取模型的 ──
 * `verdict` 与 `headline` **一律用 [InterpretationRequest.facts]** ——
 * 吉凶是算法算的，不能让模型改。模型的 JSON 里也有 verdict，但那是它的判断，
 * 与规则引擎冲突时以算法为准。只有 `sections` 是模型写的。
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

        val facts = request.facts

        // 1. 结构化优先
        val structured = runCatching { parseJson(text, facts) }.getOrNull()
        if (structured != null) {
            emit(structured)
            return@flow
        }

        // 2. 看起来像 JSON 却解析不了 —— 说明模型输出残缺。
        //    此时把花括号内容当正文渲染只会更糟，直接降级。
        if (text.startsWith("{") || text.contains("\"sections\"")) {
            Log.w("MaoMagicLLM", "模型输出像 JSON 但解析失败，降级到规则引擎")
            return@flow
        }

        // 3. 纯文本兜底
        val plain = PlainText.sanitize(text)
        if (plain.isEmpty()) return@flow

        emit(
            Interpretation(
                verdict = facts?.verdict ?: Verdict.NEUTRAL,
                headline = facts?.headline ?: "解读",
                sections = listOf(InterpretationSection("解读", plain, Tone.NEUTRAL)),
                tier = EngineTier.ON_DEVICE,
                yongShenNote = facts?.yongShenNote,
            )
        )
    }

    /**
     * 解析模型输出的 JSON。
     *
     * 容错：模型常在前后加一句「好的，以下是解读：」，所以先截取第一个 `{`
     * 到最后一个 `}` 之间的内容再解析。
     */
    private fun parseJson(raw: String, facts: Interpretation?): Interpretation? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null

        // 用 kotlinx.serialization 而不是 org.json ——
        // org.json 在 JVM 单测里是 Android 的空壳实现，测不了。
        val obj = Json.parseToJsonElement(raw.substring(start, end + 1)).jsonObject
        val arr = obj["sections"]?.jsonArray ?: return null
        if (arr.isEmpty()) return null

        val sections = arr.mapNotNull { el ->
            val s = el.jsonObject
            val title = s["title"]?.jsonPrimitive?.contentOrNull?.ifBlank { "解读" } ?: "解读"
            val body = PlainText.sanitize(s["body"]?.jsonPrimitive?.contentOrNull ?: "")
            if (body.isBlank()) return@mapNotNull null
            val tone = s["tone"]?.jsonPrimitive?.contentOrNull ?: ""
            InterpretationSection(title = title, body = body, tone = toneOf(tone))
        }
        if (sections.isEmpty()) return null

        return Interpretation(
            // 判词与吉凶取算法结果，不取模型判断
            verdict = facts?.verdict ?: Verdict.NEUTRAL,
            headline = facts?.headline
                ?: obj["headline"]?.jsonPrimitive?.contentOrNull?.ifBlank { "解读" }
                ?: "解读",
            sections = sections,
            tier = EngineTier.ON_DEVICE,
            yongShenNote = facts?.yongShenNote,
        )
    }

    private fun toneOf(name: String): Tone = when (name.uppercase()) {
        "POSITIVE" -> Tone.POSITIVE
        "NEGATIVE" -> Tone.NEGATIVE
        "WARNING" -> Tone.WARNING
        else -> Tone.NEUTRAL
    }

    companion object {
        /**
         * 按当前设备装配。模型不存在时返回一个 `isAvailable() == false` 的实例，
         * 降级链会自动跳过它 —— 不需要调用方判断。
         */
        fun fromDevice(context: Context): OnDeviceInterpreter {
            val device = DeviceProfile.current()
            val model = LocalModel.candidatesFor(device).first()
            val dir = ModelDownloader.modelDir(context.filesDir)
            val file = File(dir, model.fileName)
            val exists = file.exists()
            Log.i(
                "MaoMagicLLM",
                "fromDevice: soc=${device.socModel}/${device.socManufacturer} " +
                    "pick=${model.label} path=${file.absolutePath} exists=$exists " +
                    "len=${if (exists) file.length() else -1}",
            )
            return if (exists) {
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
 * 抽出来是为了：换运行时只改一个实现类，不动引擎逻辑；
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
