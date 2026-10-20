package com.maohongyuan.magic.core.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.BackendType
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Modality
import com.google.ai.edge.litertlm.ModelInfo
import com.maohongyuan.magic.core.interpret.InferencePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * LiteRT-LM 推理后端（Tier 3）。
 *
 * ── 为什么是它，不是 MediaPipe ──
 * MediaPipe `tasks-genai` 只认 `.task`（TFL3）。LiteRT-LM 认 `.litertlm`，
 * 而且有 **NPU 后端**（`Backend.NPU`）—— MediaPipe 没有。
 *
 * 坐标：`com.google.ai.edge.litertlm:litertlm-android`，
 * 在 **Google Maven**（不在 Maven Central，用 repo1 探测会 404）。
 *
 * ── 它只负责「说」，不负责「算」 ──
 * 卦理由规则引擎算好后经 `InterpretationRequest.facts` 传进来。
 * 本后端拿到的是「已定论的事实 + 盘面」，任务是写成通顺中文，**不许重推**。
 *
 * ── 后端降级顺序 ──
 * `NPU → GPU → CPU`。NPU 起不来是常态（机型、驱动、运行时缺失），
 * 不是异常，所以逐级 runCatching，最后一定能落到 CPU。
 * **这是「下载了但跑不起来」的唯一兜底，不能省。**
 *
 * ── ⚠️ 每次生成都新建 Engine ──
 * 换成 Qwen3.5 之后这条变成了硬约束。Qwen3.5 是混合架构
 * （GatedDeltaNet 线性注意力 + 6 层全注意力），线性注意力块带**常量大小的
 * 循环状态**，而 LiteRT-LM 新建 Conversation 时**不会重置它**
 * （[google-ai-edge/LiteRT-LM#3165]）。
 *
 * 模型卡实测：在跑过对话的 Engine 上开新会话，9 次里 **0 次**遵守新的系统提示词，
 * 而且 9 次里有 7 次吐空的 `<think></think>` 块。
 *
 * 对本项目来说这就是灾难 —— 第一卦人设对，第二卦开始变成上一卦的口气，
 * 用户会以为「提示词没生效」。
 *
 * 所以：**一次解卦 = 一个 Engine**，用完立刻关。代价是每次多花一两秒重新
 * 映射权重（0.8B int8 约 920 MB，比原来的 2.4 GB 快得多），换来的是人设稳定。
 */
class LiteRtLmBackend(
    private val context: Context,
    private val modelFile: File,
    /** 是否尝试 NPU。天玑/骁龙的 NPU 需要对应运行时，缺了就自动退 GPU */
    private val preferNpu: Boolean = true,
) : LocalModelBackend {

    /** 探测出来的可用后端名。null = 还没探测过 */
    private var chosenBackendName: String? = null
    private var lastError: String? = null

    /** 实际用上的后端，供 UI 展示与排查 */
    val backendLabel: String get() = chosenBackendName?.let { "$it（每次重建）" } ?: "未加载"

    override val errorMessage: String? get() = lastError

    /**
     * 探测可用后端并记住它。
     *
     * 首次会花几秒（要真的初始化一次 Engine 才知道 NPU/GPU 能不能用）。
     * **不要在 UI 线程调，也不要在启动时无条件调** —— 用户点了智能解卦再加载。
     *
     * 探测完**不留常驻 Engine**：每次生成都要新建，见类注释。
     */
    suspend fun load(): Boolean = withContext(Dispatchers.IO) {
        LlmLog.init(context)
        if (chosenBackendName != null) return@withContext true
        if (!modelFile.exists()) {
            lastError = "模型文件不存在：${modelFile.absolutePath}"
            LlmLog.e(lastError!!)
            return@withContext false
        }
        LlmLog.i("开始探测：${modelFile.absolutePath} (${modelFile.length() / 1048576} MB)")

        // 读模型自己声明的能力 —— 只用于日志与排查，**不参与后端选择**。
        // 之前用它过滤候选，结果模型声明与实际能起什么不一致，导致整链失效。
        val info = withTimeoutOrNull(15_000) { readModelInfo() }
        LlmLog.i("模型自述 → ${info ?: "读取超时或失败"}")

        val names = backendCandidates()
        LlmLog.i("后端偏好=${InferencePrefs.backend.name}，候选=$names")

        for (name in names) {
            val outcome = runCatching { withFreshEngine(name) { } }
            if (outcome.isSuccess) {
                chosenBackendName = name
                lastError = null
                LlmLog.i("$name 后端探测成功")
                return@withContext true
            }
            LlmLog.w("$name 后端失败，尝试下一个", outcome.exceptionOrNull())
        }

        lastError = "后端 ${InferencePrefs.backend.name} 初始化失败"
        LlmLog.e(lastError!!)
        false
    }

    /**
     * 直接问模型：你支持什么。
     *
     * `ModelInfo.from()` 只解析元数据不加载权重，但在 GB 级文件上实测要几秒，
     * 所以放宽到 15 秒。**结果只写日志**，不参与后端选择。
     */
    private suspend fun readModelInfo(): String? = withContext(Dispatchers.IO) {
        runCatching {
            ModelInfo.from(modelFile.absolutePath).use { m ->
                val backends = runCatching { m.supportedBackends(Modality.TEXT) }
                    .getOrNull()?.joinToString("/") ?: "?"
                val npu = runCatching { m.npuBrand(Modality.TEXT) }.getOrNull()
                val soc = runCatching { m.socName(Modality.TEXT) }.getOrNull()
                buildString {
                    append("backends=$backends")
                    append(" npuBrand=$npu")
                    append(" soc=$soc")
                    append(" minRuntime=${m.minRuntimeVersion()}")
                    append(" maxCtx=${m.maxContextTokens()}")
                    append(" gpu=${BackendType.GPU in (runCatching { m.supportedBackends(Modality.TEXT) }.getOrNull() ?: emptyList())}")
                }
            }
        }.onFailure { LlmLog.w("读取模型元数据失败", it) }.getOrNull()
    }

    override suspend fun isReady(): Boolean = chosenBackendName != null

    /**
     * 生成一段文本。
     *
     * ⚠️ 每次都新建 Engine + Conversation，用完立刻关 —— 见类注释。
     * 复用 Engine 会让 Qwen3.5 带着上一卦的循环状态继续跑，系统提示词直接失效。
     */
    override suspend fun generate(systemPrompt: String, userMessage: String): String? =
        withContext(Dispatchers.IO) {
            if (chosenBackendName == null && !load()) return@withContext null
            val name = chosenBackendName ?: return@withContext null

            val prompt = "$systemPrompt\n\n$userMessage"
            LlmLog.i("开始推理（$name），prompt ${prompt.length} 字符")
            val t0 = System.currentTimeMillis()
            val outcome = runCatching {
                withFreshEngine(name) { conv -> conv.sendMessage(prompt) }
            }

            outcome.onFailure {
                lastError = "推理失败：${it.message}"
                LlmLog.e("推理失败", it)
                // 后端可能已经不可用（被系统回收、驱动异常）—— 清掉缓存，下次重新探测
                chosenBackendName = null
            }

            val text = outcome.getOrNull()
                ?.contents
                ?.contents
                ?.filterIsInstance<Content.Text>()
                ?.joinToString("") { it.text }
                ?.trim()

            if (text.isNullOrBlank()) {
                lastError = lastError ?: "模型返回空文本"
                LlmLog.w(lastError!!)
                return@withContext null
            }
            LlmLog.i("推理完成，${text.length} 字符，耗时 ${System.currentTimeMillis() - t0} ms")
            text
        }

    /**
     * 建一个全新的 Engine 跑一段逻辑，结束时一定关掉。
     *
     * 关的顺序与开相反：先 Conversation 后 Engine。漏关会泄漏 native 内存，
     * 连开几次就把应用撑爆。
     */
    private inline fun <T> withFreshEngine(backendName: String, block: (Conversation) -> T): T {
        val engine = Engine(
            EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = makeBackend(backendName),
                cacheDir = context.cacheDir.absolutePath,
            )
        )
        try {
            engine.initialize()
            val conv = engine.createConversation()
            try {
                return block(conv)
            } finally {
                runCatching { conv.close() }
            }
        } finally {
            runCatching { engine.close() }
        }
    }

    private fun makeBackend(name: String): Backend = when (name) {
        "NPU" -> Backend.NPU(context.applicationInfo.nativeLibraryDir)
        "GPU" -> Backend.GPU()
        else -> Backend.CPU()
    }

    private fun backendCandidates(): List<String> = when (InferencePrefs.backend) {
        InferencePrefs.BackendPref.NPU -> listOf("NPU")
        InferencePrefs.BackendPref.GPU -> listOf("GPU")
        InferencePrefs.BackendPref.CPU -> listOf("CPU")
        InferencePrefs.BackendPref.AUTO -> buildList {
            if (preferNpu) add("NPU")
            add("GPU")
            add("CPU")
        }
    }

    /** 释放。Engine 是每次生成临时建的，这里只清探测结果 */
    fun close() {
        chosenBackendName = null
    }

    private companion object {
        const val TAG = "MaoMagicLLM"
    }
}
