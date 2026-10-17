package com.maohongyuan.magic.core.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * MediaPipe（LiteRT-LM）推理后端。
 *
 * ── 坐标坑 ──
 * `com.google.mediapipe:tasks-genai` 在 **Google Maven**，不在 Maven Central。
 * 用 `repo1.maven.org` 探测会 404 —— 别据此判断「库不存在」。
 *
 * ── 它只负责「说」，不负责「算」 ──
 * 卦理（用神/旺衰/旬空/动变爻）由规则引擎算好后，通过
 * [com.maohongyuan.magic.core.interpret.InterpretationRequest.facts] 传进来。
 * 本后端拿到的是「已经定论的事实 + 盘面」，任务是写成通顺中文，**不许重推**。
 *
 * ── GPU 降级 ──
 * `setPreferredBackend(GPU)` 在某些机型上会直接抛异常（驱动缺失、内存不足、
 * Mali 上 OpenCL 被裁）。所以 [load] 会先试 GPU，失败再退 CPU。
 * **这是「下载了但跑不起来」的唯一兜底**，不能省。
 */
class LiteRtLmBackend(
    private val context: Context,
    private val modelFile: File,
    /** 是否优先用 GPU。CPU 兜底永远保留 */
    private val preferGpu: Boolean = true,
    private val maxTokens: Int = 1024,
) : LocalModelBackend {

    private var inference: LlmInference? = null
    private var usingGpu = false
    private var lastError: String? = null

    /** 实际用上的后端，供 UI 展示与排查 */
    val backendLabel: String
        get() = when {
            inference == null -> "未加载"
            usingGpu -> "GPU"
            else -> "CPU"
        }

    val error: String? get() = lastError

    /**
     * 加载模型。**首次调用会花几秒到几十秒**（2 GB 权重映射），
     * 不要在 UI 线程调，也不要在启动时无条件调 —— 用户点了「智能解卦」再加载。
     */
    suspend fun load(): Boolean = withContext(Dispatchers.IO) {
        if (inference != null) return@withContext true
        if (!modelFile.exists()) {
            lastError = "模型文件不存在：${modelFile.absolutePath}"
            Log.e(TAG, lastError!!)
            return@withContext false
        }
        Log.i(TAG, "开始加载：${modelFile.absolutePath} (${modelFile.length() / 1048576} MB), preferGpu=$preferGpu")

        if (preferGpu) {
            runCatching { create(LlmInference.Backend.GPU) }
                .onSuccess {
                    inference = it; usingGpu = true; lastError = null
                    Log.i(TAG, "GPU 后端加载成功")
                    return@withContext true
                }
                .onFailure { gpuError ->
                    // GPU 起不来是常态（机型差异大），不是异常情况，不往上抛
                    lastError = "GPU 初始化失败，改用 CPU：${gpuError.message}"
                    Log.w(TAG, "GPU 失败，转 CPU", gpuError)
                }
        }

        runCatching { create(LlmInference.Backend.CPU) }
            .onSuccess { inference = it; usingGpu = false; Log.i(TAG, "CPU 后端加载成功") }
            .onFailure {
                lastError = "模型加载失败：${it.message}"
                Log.e(TAG, "CPU 也失败了", it)
            }
        inference != null
    }

    private fun create(backend: LlmInference.Backend): LlmInference {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(maxTokens)
            .setPreferredBackend(backend)
            .build()
        return LlmInference.createFromOptions(context, options)
    }

    override suspend fun isReady(): Boolean = inference != null

    /**
     * 生成。
     *
     * ⚠️ MediaPipe 这一版没有 chat template API，只吃一个字符串。
     * Gemma 需要 `<start_of_turn>` 标记，所以这里手工拼。
     * 具体格式要在真机上试 —— 不同模型版本对格式敏感，拼错了会输出乱码或空串。
     */
    override suspend fun generate(systemPrompt: String, userMessage: String): String? =
        withContext(Dispatchers.IO) {
            // 懒加载：模型 2 GB，不能一进页面就吃内存
            if (inference == null && !load()) return@withContext null
            val llm = inference ?: return@withContext null
            val prompt = buildPrompt(systemPrompt, userMessage)
            Log.i(TAG, "开始推理，prompt ${prompt.length} 字符")
            val t0 = System.currentTimeMillis()
            runCatching { llm.generateResponse(prompt) }
                .onSuccess { Log.i(TAG, "推理完成，${it.length} 字符，耗时 ${System.currentTimeMillis() - t0} ms") }
                .onFailure {
                    lastError = "推理失败：${it.message}"
                    Log.e(TAG, "推理失败", it)
                }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        }

    private fun buildPrompt(systemPrompt: String, userMessage: String): String = buildString {
        append("<start_of_turn>user\n")
        append(systemPrompt)
        append("\n\n")
        append(userMessage)
        append("<end_of_turn>\n<start_of_turn>model\n")
    }

    private companion object {
        const val TAG = "MaoMagicLLM"
    }

    /** 释放。2 GB 模型常驻会拖垮系统，切走就要放 */
    fun close() {
        runCatching { inference?.close() }
        inference = null
    }
}
