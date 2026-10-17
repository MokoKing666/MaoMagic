package com.maohongyuan.magic.core.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.maohongyuan.magic.core.interpret.InferencePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
 */
class LiteRtLmBackend(
    private val context: Context,
    private val modelFile: File,
    /** 是否尝试 NPU。天玑/骁龙的 NPU 需要对应运行时，缺了就自动退 GPU */
    private val preferNpu: Boolean = true,
) : LocalModelBackend {

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var usedBackend: String = ""
    private var lastError: String? = null

    /** 实际用上的后端，供 UI 展示与排查 */
    val backendLabel: String get() = usedBackend.ifBlank { "未加载" }

    val error: String? get() = lastError

    /**
     * 加载模型。
     *
     * 首次会花几秒到几十秒（2 GB 权重映射 + 图初始化）。
     * **不要在 UI 线程调，也不要在启动时无条件调** —— 用户点了智能解卦再加载。
     */
    suspend fun load(): Boolean = withContext(Dispatchers.IO) {
        if (engine != null) return@withContext true
        if (!modelFile.exists()) {
            lastError = "模型文件不存在：${modelFile.absolutePath}"
            Log.e(TAG, lastError!!)
            return@withContext false
        }
        Log.i(TAG, "开始加载：${modelFile.absolutePath} (${modelFile.length() / 1048576} MB)")

        // 用户手动选了后端就**只试那一个** —— 否则「选了 CPU 却偷偷用 GPU」，
        // 用户的选择就不可信。AUTO 才逐级尝试。
        val pref = InferencePrefs.backend
        val candidates = buildList {
            when (pref) {
                InferencePrefs.BackendPref.NPU ->
                    add("NPU" to Backend.NPU(context.applicationInfo.nativeLibraryDir))
                InferencePrefs.BackendPref.GPU -> add("GPU" to Backend.GPU())
                InferencePrefs.BackendPref.CPU -> add("CPU" to Backend.CPU())
                InferencePrefs.BackendPref.AUTO -> {
                    if (preferNpu) {
                        // NPU 运行时 .so 通常在应用的 nativeLibraryDir 下
                        add("NPU" to Backend.NPU(context.applicationInfo.nativeLibraryDir))
                    }
                    add("GPU" to Backend.GPU())
                    add("CPU" to Backend.CPU())
                }
            }
        }
        Log.i(TAG, "后端偏好=${pref.name}，候选=${candidates.map { it.first }}")

        for ((name, backend) in candidates) {
            val outcome = runCatching {
                val config = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = backend,
                    cacheDir = context.cacheDir.absolutePath,
                )
                val e = Engine(config)
                e.initialize()
                val conv = e.createConversation()
                engine = e
                conversation = conv
                usedBackend = name
            }
            if (outcome.isSuccess) {
                lastError = null
                Log.i(TAG, "$name 后端加载成功")
                return@withContext true
            }
            Log.w(TAG, "$name 后端失败，尝试下一个", outcome.exceptionOrNull())
        }

        lastError = "后端 ${pref.name} 初始化失败"
        Log.e(TAG, lastError!!)
        false
    }

    override suspend fun isReady(): Boolean = engine != null

    /**
     * 生成一段文本。
     *
     * 一次会话只用一个 prompt：系统约束 + 已算定事实 + 盘面，
     * 见 [ChartPrompt]。不做多轮 —— 解卦是一次性的，多轮会引入状态与漂移。
     */
    override suspend fun generate(systemPrompt: String, userMessage: String): String? =
        withContext(Dispatchers.IO) {
            if (engine == null && !load()) return@withContext null
            val conv = conversation ?: return@withContext null

            val prompt = "$systemPrompt\n\n$userMessage"
            Log.i(TAG, "开始推理（${backendLabel}），prompt ${prompt.length} 字符")
            val t0 = System.currentTimeMillis()
            val outcome = runCatching { conv.sendMessage(prompt) }

            outcome.onFailure {
                lastError = "推理失败：${it.message}"
                Log.e(TAG, "推理失败", it)
            }

            val text = outcome.getOrNull()
                ?.contents
                ?.contents
                ?.filterIsInstance<Content.Text>()
                ?.joinToString("") { it.text }
                ?.trim()

            if (text.isNullOrBlank()) {
                lastError = lastError ?: "模型返回空文本"
                Log.w(TAG, lastError!!)
                return@withContext null
            }
            Log.i(TAG, "推理完成，${text.length} 字符，耗时 ${System.currentTimeMillis() - t0} ms")
            text
        }

    /** 释放。2 GB 模型常驻会拖垮系统，切走就要放 */
    fun close() {
        runCatching { conversation?.close() }
        runCatching { engine?.close() }
        conversation = null
        engine = null
        usedBackend = ""
    }

    private companion object {
        const val TAG = "MaoMagicLLM"
    }
}
