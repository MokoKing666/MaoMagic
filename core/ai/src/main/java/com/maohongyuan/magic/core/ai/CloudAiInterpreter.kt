package com.maohongyuan.magic.core.ai

import com.maohongyuan.magic.core.interpret.DivinationInterpreter
import com.maohongyuan.magic.core.interpret.EngineTier
import com.maohongyuan.magic.core.interpret.Interpretation
import com.maohongyuan.magic.core.interpret.InterpretationRequest
import com.maohongyuan.magic.core.interpret.InterpretationSection
import com.maohongyuan.magic.core.interpret.Tone
import com.maohongyuan.magic.core.interpret.Verdict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Tier2：云端 OpenAI 兼容 API 解卦。
 *
 * ── 隐私 ──
 * 只有在用户**主动开启**并填好 BaseURL / Key / 模型名之后才会发起请求
 * （见 [CloudAiConfig.isComplete]）。默认配置下 [isAvailable] 恒为 false，
 * 降级链直接落到 Tier1，**不会有任何网络流量**。
 *
 * 请求只发往用户自己填的地址，携带的内容是**卦盘本身**（所问 + 排好的卦），
 * 不携带任何设备标识、账号或位置信息。
 *
 * ── 为什么不做流式（SSE） ──
 * [com.maohongyuan.magic.core.interpret.InterpreterChain] 用 `firstOrNull()`
 * 取第一个结果，流式输出会被截断成第一段。
 * 要做流式必须同时改链与 UI（边生成边渲染），那是另一件事，不在本次范围。
 * 这里一次性取完整响应再 emit 一个 [Interpretation]。
 *
 * ── 失败即降级 ──
 * 任何异常（网络、超时、鉴权、JSON 解析）都返回**空 Flow**，
 * 由降级链自动落到下一个引擎。**绝不让云端故障影响用户看到结果**。
 */
class CloudAiInterpreter private constructor(
    private val configProvider: () -> CloudAiConfig,
    private val client: OkHttpClient,
) : DivinationInterpreter {

    /**
     * 公开构造。
     *
     * ⚠️ **不要**把 [OkHttpClient] 放进公开构造函数的默认参数 ——
     * OkHttp 是 `implementation` 依赖，它的类型一旦出现在公开签名里，
     * 消费模块编译时就会报「Cannot access class 'okhttp3.OkHttpClient'」。
     * 这里把 client 收进私有主构造，对外只暴露配置提供者。
     */
    constructor(configProvider: () -> CloudAiConfig) : this(configProvider, defaultClient())

    override val tier: EngineTier = EngineTier.CLOUD_AI

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 只检查配置完整性，**不探测网络**。
     *
     * 降级链在每次解读时都会调用本方法；如果这里做网络探测，
     * 每次解卦都要多等一个 RTT，且离线时会反复超时。
     * 真正的网络失败交给 [interpret] 处理 —— 它失败就返回空 Flow，链会继续降级。
     */
    override suspend fun isAvailable(): Boolean = configProvider().isComplete

    override fun interpret(request: InterpretationRequest): Flow<Interpretation> = flow {
        val config = configProvider()
        if (!config.isComplete) return@flow

        val result = runCatching { callApi(config, request) }.getOrNull() ?: return@flow
        emit(result)
    }.flowOn(Dispatchers.IO)

    // ── 内部实现 ──────────────────────────────────────────

    private fun callApi(config: CloudAiConfig, request: InterpretationRequest): Interpretation {
        val payload = json.encodeToString(
            ChatRequest(
                model = config.model,
                temperature = config.temperature,
                messages = listOf(
                    ChatMessage("system", ChartPrompt.systemPrompt(request)),
                    ChatMessage("user", ChartPrompt.userMessage(request)),
                ),
            )
        )

        val httpRequest = Request.Builder()
            .url(config.chatCompletionsUrl)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody(JSON_MEDIA))
            .build()

        client.newBuilder()
            .callTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
            .build()
            .newCall(httpRequest)
            .execute()
            .use { response ->
                val body = response.body?.string().orEmpty()
                check(response.isSuccessful) {
                    "云端返回 ${response.code}：${body.take(200)}"
                }
                val content = json.decodeFromString<ChatResponse>(body)
                    .choices.firstOrNull()?.message?.content
                    ?: error("响应里没有 choices[0].message.content")
                return parseInterpretation(content)
            }
    }

    /**
     * 解析模型返回的 JSON。
     *
     * 模型经常会不听话地套一层 ```json 代码块，或者前后带解释文字，
     * 所以先做一次**宽松提取**：取第一个 `{` 到最后一个 `}` 之间的内容。
     */
    private fun parseInterpretation(raw: String): Interpretation {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        check(start in 0 until end) { "模型输出里找不到 JSON 对象" }
        val dto = json.decodeFromString<InterpretationDto>(raw.substring(start, end + 1))

        val verdict = when (dto.verdict.trim()) {
            "吉" -> Verdict.AUSPICIOUS
            "凶" -> Verdict.INAUSPICIOUS
            "吉中带阻" -> Verdict.MIXED
            else -> Verdict.NEUTRAL
        }

        return Interpretation(
            verdict = verdict,
            headline = dto.headline.ifBlank { "（模型未给出结论）" },
            sections = dto.sections.map {
                InterpretationSection(
                    title = it.title,
                    body = it.body,
                    tone = when (it.tone.uppercase()) {
                        "POSITIVE" -> Tone.POSITIVE
                        "NEGATIVE" -> Tone.NEGATIVE
                        "WARNING" -> Tone.WARNING
                        else -> Tone.NEUTRAL
                    },
                )
            },
            tier = EngineTier.CLOUD_AI,
            yongShenNote = dto.yongShen?.takeIf { it.isNotBlank() },
        )
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}

// ── 请求 / 响应 DTO ──────────────────────────────────────

@Serializable
private data class ChatMessage(val role: String, val content: String)

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float,
    @SerialName("response_format") val responseFormat: Map<String, String> =
        mapOf("type" to "json_object"),
)

@Serializable
private data class ChatResponse(val choices: List<Choice> = emptyList()) {
    @Serializable
    data class Choice(val message: ChatMessage? = null)
}

/** 模型返回的解读结构。字段缺失一律给默认值，避免一个字段错就整条失败。 */
@Serializable
private data class InterpretationDto(
    val verdict: String = "平",
    val headline: String = "",
    val yongShen: String? = null,
    val sections: List<SectionDto> = emptyList(),
) {
    @Serializable
    data class SectionDto(
        val title: String = "",
        val body: String = "",
        val tone: String = "NEUTRAL",
    )
}
