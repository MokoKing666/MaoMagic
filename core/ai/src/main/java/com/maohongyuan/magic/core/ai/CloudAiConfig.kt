package com.maohongyuan.magic.core.ai

/**
 * 云端 AI 配置。
 *
 * ── 隐私边界（重要） ──
 * 这是**唯一**会触发网络请求的功能，且必须由用户主动开启：
 * [enabled] 默认为 false，[baseUrl] / [apiKey] 默认为空。
 * 三者缺一，[CloudAiInterpreter.isAvailable] 就返回 false，降级链直接落到 Tier1，
 * **不会发出任何网络请求**。
 *
 * 端点由用户自填，不预置任何服务商 —— 用户可以用官方 API，也可以指向自建服务。
 * 我们不做任何默认收集、不上报任何数据。
 */
data class CloudAiConfig(
    /** 是否启用云端解卦。默认关闭 */
    val enabled: Boolean = false,

    /**
     * OpenAI 兼容的 API 根地址，例如 `https://api.openai.com/v1`。
     *
     * 只填根地址，具体路径由客户端拼（`/chat/completions`）——
     * 让用户少填错一个地方。
     */
    val baseUrl: String = "",

    /** API Key。存储在 Android Keystore 加密的偏好设置中，不落明文 */
    val apiKey: String = "",

    /** 模型名，例如 `gpt-4o-mini` */
    val model: String = "",

    /**
     * 采样温度。默认 0.3 —— 解卦需要稳定而非发散，
     * 同一卦象两次问不应给出完全不同的结论。
     */
    val temperature: Float = 0.3f,

    /** 单次请求超时（秒） */
    val timeoutSeconds: Long = 60L,
) {
    /**
     * 配置是否完整到可以发起请求。
     *
     * 刻意**不检查网络连通性** —— 那是 [CloudAiInterpreter.isAvailable] 的职责，
     * 且网络状态随时会变，不适合放进不可变配置。
     */
    val isComplete: Boolean
        get() = enabled &&
            baseUrl.isNotBlank() &&
            apiKey.isNotBlank() &&
            model.isNotBlank()

    /** 拼出 chat completions 的完整地址，容忍用户填了或没填结尾斜杠 */
    val chatCompletionsUrl: String
        get() = baseUrl.trimEnd('/') + "/chat/completions"

    companion object {
        val DEFAULT = CloudAiConfig()

        val TEMPERATURE_RANGE = 0f..1f
    }
}
