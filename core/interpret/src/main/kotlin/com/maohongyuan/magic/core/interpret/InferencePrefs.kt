package com.maohongyuan.magic.core.interpret

/**
 * 推理偏好（全局缓存）。
 *
 * ── 为什么放在 interpret 而不是 ai ──
 * 依赖方向是 `ai → interpret`，反过来不行。偏好既要被 [InterpreterChain] 读
 * （选档），又要被端上后端读（选 NPU/GPU/CPU）。放在 interpret 里，
 * 两边都能读，且**调用点一行都不用改**。
 *
 * ── 为什么是全局 ──
 * 调用方分散在 5 个 feature 模块，逐个传参要在每个模块加 settings 依赖，
 * 改 5 处还容易漏。这里只是**缓存**：真值在
 * [com.maohongyuan.magic.core.data.MagicSettings]，启动与改动时回填。
 */
object InferencePrefs {

    /**
     * 端上模型的后端偏好。
     *
     * `AUTO` 按 NPU → GPU → CPU 依次尝试。
     * 手动选定时**只试那一个** —— 否则「选了 CPU 却偷偷用 GPU」，
     * 用户的选择就不可信了。
     */
    enum class BackendPref(val label: String, val description: String) {
        AUTO("自动", "按 NPU → GPU → CPU 依次尝试，取第一个能用的。"),
        NPU(
            "NPU",
            "只试 NPU。⚠️ 当前不可用 —— litertlm-android 只带了 JNI 库，" +
                "NPU 运行时（联发科/高通的专有 .so）需要单独引入，尚未接入。" +
                "选了会退回规则引擎。",
        ),
        GPU("GPU", "只试 GPU。兼容性最好的一档加速。"),
        CPU("CPU", "只试 CPU。最慢但最稳，发热最低。"),
        ;

        companion object {
            fun of(name: String?): BackendPref = entries.firstOrNull { it.name == name } ?: AUTO
        }
    }

    /** 解卦引擎偏好。null = 自动（本地 → 云端 → 规则） */
    @Volatile
    var preferredTier: EngineTier? = null

    @Volatile
    var backend: BackendPref = BackendPref.AUTO

    fun update(tierName: String?, backendName: String?) {
        preferredTier = when (tierName) {
            "ON_DEVICE" -> EngineTier.ON_DEVICE
            "CLOUD_AI" -> EngineTier.CLOUD_AI
            "RULE" -> EngineTier.RULE
            else -> null
        }
        backend = BackendPref.of(backendName)
    }
}
