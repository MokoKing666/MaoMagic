package com.maohongyuan.magic.core.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.BackendType
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
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
 * ── ⚠️ 三条硬约束（都是实测踩出来的，改代码前先读）──
 *
 * **1. 一次解卦 = 一个 Engine。**
 * Qwen3.5 是混合架构（GatedDeltaNet 线性注意力 + 6 层全注意力），
 * 线性注意力块带常量大小的循环状态，而 LiteRT-LM 新建 Conversation 时
 * **不重置它**（LiteRT-LM#3165）。模型卡实测：在跑过对话的 Engine 上开新会话，
 * 9 次里 0 次遵守系统提示词 —— 等于第一卦人设对、第二卦起就变成上一卦的口气。
 *
 * **2. 不要「先探测再生成」，那会白跑一次初始化。**
 * 每次 `Engine.initialize()` 在手机上要十几到几十秒（见下条）。
 * 早期实现先建一个 Engine 探后端、再建一个做推理，等于把首次等待翻倍。
 * 现在探测与生成合并：候选后端逐个「建 Engine + 直接生成」，第一个成功的就记住。
 *
 * **3. NPU 只在模型自述支持时才试；GPU 是否优先看 SoC。**
 * - `Backend.NPU(...)` 的 `initialize()` 在不支持的机器上会**死等不返回**
 *   （不是抛异常），`withTimeout` 也取消不了阻塞的 native 调用。
 *   唯一可靠的防线是根本不去试。Qwen3.5-0.8B 自述 `backends=CPU/GPU`，不含 NPU。
 * - 天玑 9400（MT6991）实测：GPU 初始化 **198 秒**，CPU **25 秒**。
 *   GPU 委派在这种混合架构上对 Mali 的图切分开销极大。
 *   模型卡上的 GPU 数据来自 Adreno / Apple，不能外推到 Mali。
 *   所以 AUTO 按 GPU 厂商分流，见 [autoOrder]。
 */
class LiteRtLmBackend(
    private val context: Context,
    private val modelFile: File,
    /** 是否允许在模型声明支持时尝试 NPU */
    private val preferNpu: Boolean = true,
) : LocalModelBackend {

    /** 上次成功的后端名。null = 还没成功过 */
    private var chosenBackendName: String? = null
    private var lastError: String? = null

    /** 实际用上的后端，供 UI 展示与排查 */
    val backendLabel: String get() = chosenBackendName?.let { "$it（每次重建）" } ?: "未加载"

    override val errorMessage: String? get() = lastError

    override suspend fun isReady(): Boolean = chosenBackendName != null

    /**
     * 生成一段文本。
     *
     * 每次都新建 Engine + Conversation，用完立刻关 —— 见类注释第 1 条。
     * 候选后端逐个实测：第一个能跑通的就记住，下次直接用。
     */
    override suspend fun generate(systemPrompt: String, userMessage: String): String? =
        withContext(Dispatchers.IO) {
            LlmLog.init(context)
            if (!modelFile.exists()) {
                lastError = "模型文件不存在：${modelFile.absolutePath}"
                LlmLog.e(lastError!!)
                return@withContext null
            }

            val names = resolveCandidates()
            if (names.isEmpty()) {
                lastError = "模型不支持所选后端（${InferencePrefs.backend.name}）"
                LlmLog.e(lastError!!)
                return@withContext null
            }

            val prompt = "$systemPrompt\n\n$userMessage"
            LlmLog.i("开始推理，候选=$names，prompt ${prompt.length} 字符")

            for (name in names) {
                val t0 = System.currentTimeMillis()
                val outcome = runCatching {
                    withFreshEngine(name) { conv -> conv.sendMessage(prompt) }
                }

                val text = outcome.getOrNull()?.let(::extractText)
                if (outcome.isSuccess && !text.isNullOrBlank()) {
                    chosenBackendName = name
                    lastError = null
                    LlmLog.i(
                        "推理完成（$name），${text.length} 字符，" +
                            "总耗时 ${System.currentTimeMillis() - t0} ms",
                    )
                    return@withContext text
                }

                // 空文本也算失败：换个后端还有机会，直接返回会让用户看到空白。
                if (outcome.isSuccess) {
                    LlmLog.w("$name 返回空文本，尝试下一个")
                } else {
                    LlmLog.w("$name 推理失败，尝试下一个", outcome.exceptionOrNull())
                }
            }

            chosenBackendName = null
            lastError = "所有后端都未能产出结果"
            LlmLog.e(lastError!!)
            null
        }

    /** 从响应里取出纯文本 */
    private fun extractText(response: Message): String =
        response.contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString("") { it.text }
            .trim()

    /**
     * 候选后端名。
     *
     * 已经成功过就只用那一个 —— 换后端意味着重新初始化，没必要。
     * 第一次才需要读模型自述 + 按 SoC 排序。
     */
    private suspend fun resolveCandidates(): List<String> {
        chosenBackendName?.let { return listOf(it) }

        // 模型自述只用来判断「要不要试 NPU」，别的判断一概不做。
        // 之前用它过滤 GPU 候选，结果声明与实际不一致，整链失效。
        val info = withTimeoutOrNull(15_000) { readModelInfo() }
        LlmLog.i("模型自述 → ${info?.summary ?: "读取超时或失败"}")

        val names = backendCandidates(info?.backends)
        LlmLog.i("后端偏好=${InferencePrefs.backend.name}，候选=$names")
        return names
    }

    /**
     * 建一个全新的 Engine 跑一段逻辑，结束时一定关掉。
     *
     * 关的顺序与开相反：先 Conversation 后 Engine。漏关会泄漏 native 内存，
     * 连开几次就把应用撑爆。
     */
    private inline fun <T> withFreshEngine(backendName: String, block: (Conversation) -> T): T {
        val t0 = System.currentTimeMillis()
        val engine = Engine(
            EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = makeBackend(backendName),
                cacheDir = context.cacheDir.absolutePath,
            )
        )
        try {
            LlmLog.i("[$backendName] Engine 已构造，开始 initialize")
            engine.initialize()
            LlmLog.i("[$backendName] initialize 完成，耗时 ${System.currentTimeMillis() - t0} ms")
            val conv = engine.createConversation()
            LlmLog.i("[$backendName] 会话已建，耗时 ${System.currentTimeMillis() - t0} ms")
            try {
                return block(conv)
            } finally {
                runCatching { conv.close() }
            }
        } finally {
            runCatching { engine.close() }
            LlmLog.i("[$backendName] Engine 已释放，总耗时 ${System.currentTimeMillis() - t0} ms")
        }
    }

    private fun makeBackend(name: String): Backend = when (name) {
        "NPU" -> Backend.NPU(context.applicationInfo.nativeLibraryDir)
        "GPU" -> Backend.GPU()
        else -> Backend.CPU()
    }

    /** 模型自述：声明支持的后端集合 + 一段给人看的摘要 */
    private data class ModelFacts(val backends: Set<BackendType>, val summary: String)

    /**
     * 直接问模型：你支持什么。
     *
     * `ModelInfo.from()` 只解析元数据不加载权重，但在 GB 级文件上实测要几秒，
     * 所以放宽到 15 秒。
     */
    private suspend fun readModelInfo(): ModelFacts? = withContext(Dispatchers.IO) {
        runCatching {
            ModelInfo.from(modelFile.absolutePath).use { m ->
                val declared = runCatching { m.supportedBackends(Modality.TEXT) }
                    .getOrNull()?.toSet() ?: emptySet()
                val npu = runCatching { m.npuBrand(Modality.TEXT) }.getOrNull()
                val soc = runCatching { m.socName(Modality.TEXT) }.getOrNull()
                ModelFacts(
                    backends = declared,
                    summary = buildString {
                        append("backends=")
                        append(declared.joinToString("/") { it.name }.ifEmpty { "?" })
                        append(" npuBrand=$npu")
                        append(" soc=$soc")
                        append(" minRuntime=${m.minRuntimeVersion()}")
                        append(" maxCtx=${m.maxContextTokens()}")
                    },
                )
            }
        }.onFailure { LlmLog.w("读取模型元数据失败", it) }.getOrNull()
    }

    /**
     * 候选后端。
     *
     * ⚠️ **NPU 只有在模型自己声明支持时才试**，其余情况一律跳过。
     *
     * Qwen3.5-0.8B 自述 `backends=CPU/GPU`，不含 NPU；而在天玑 9400 上
     * `Backend.NPU(...)` 的 `initialize()` **死等不返回** —— 不是抛异常，是永远挂着。
     * 用户点了解卦，界面一直空着，日志停在「[NPU] Engine 已构造，开始 initialize」。
     *
     * `initialize()` 是阻塞的 native 调用，`withTimeout` 取消不了它，
     * 所以唯一可靠的防线是**根本不去试**。声明读不到（null）时同样跳过：
     * GPU/CPU 一定能覆盖，而错试一次 NPU 的代价是整个解卦卡死。
     */
    private fun backendCandidates(declared: Set<BackendType>?): List<String> {
        val wanted = when (InferencePrefs.backend) {
            InferencePrefs.BackendPref.NPU -> listOf("NPU")
            InferencePrefs.BackendPref.GPU -> listOf("GPU")
            InferencePrefs.BackendPref.CPU -> listOf("CPU")
            InferencePrefs.BackendPref.AUTO -> autoOrder()
        }
        val npuSupported = preferNpu && declared != null && BackendType.NPU in declared
        if (npuSupported) return listOf("NPU") + wanted.filterNot { it == "NPU" }
        if (wanted.contains("NPU")) {
            LlmLog.w("模型未声明支持 NPU，跳过（避免 initialize 挂死）")
        }
        return wanted.filterNot { it == "NPU" }
    }

    /**
     * AUTO 的后端顺序。
     *
     * ⚠️ 默认**不是**「GPU 优先」—— 实测踩过：
     * 天玑 9400（MT6991）+ Qwen3.5-0.8B，GPU 后端的 `initialize()` 要 **198 秒**，
     * 同机 CPU 只要 **25 秒**。GPU 委派在这种混合架构上对 Mali 的图切分开销极大。
     *
     * 模型卡上的 GPU 数据来自 Galaxy S26（Adreno）与 iPhone，不能外推到 Mali。
     *
     * 所以按厂商分流：高通 / Google Tensor → GPU 优先；其余 → CPU 优先，GPU 兜底。
     * 拿不准就 CPU 优先：慢一点但一定跑得起来，总比让用户等三分钟强。
     */
    private fun autoOrder(): List<String> {
        val gpuFirst = runCatching {
            val p = DeviceProfile.current()
            val vendor = p.socManufacturer.uppercase()
            vendor.contains("QTI") || vendor.contains("QUALCOMM") ||
                vendor.contains("GOOGLE") || p.socModel.uppercase().startsWith("TENSOR")
        }.getOrDefault(false)
        return if (gpuFirst) listOf("GPU", "CPU") else listOf("CPU", "GPU")
    }

    /** 释放。Engine 是每次生成临时建的，这里只清记忆 */
    fun close() {
        chosenBackendName = null
    }
}
