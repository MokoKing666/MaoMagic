package com.maohongyuan.magic.core.ai

import android.os.Build

/**
 * 端上模型的清单。
 *
 * ── 为什么现在只剩一个 ──
 * 上一版是 Gemma 4 E2B，谷歌按加速器发了三个 `.litertlm` 变体
 * （骁龙 NPU / 通用 GPU / CPU 兜底），选型逻辑很重。
 *
 * 现在换成 **Qwen3.5-0.8B**：魔搭 `litert-community` 只发两个文件 ——
 * 文本版与视觉版（VL）。我们用文本版，**一个文件通吃 NPU / GPU / CPU**，
 * 没有按 SoC 分发的必要，选型逻辑随之简化。
 *
 * ── 为什么不要 VL 版 ──
 * VL 版带 ViT 视觉编码器（1.30 GB），本项目只做文字解卦，用不上，
 * 白占 340 MB 内存和磁盘。
 *
 * ── 为什么换 0.8B ──
 * 解卦的输出是「把算好的盘面讲成人话」，不需要模型会推理数学题。
 * 0.8B 在这件事上够用，而且比 2.4 GB 的 Gemma 4 E2B 快得多、门槛低得多
 * （4 GB 内存的机器也能跑）。
 *
 * ── 数据来源（2026-10-16 版本，已核对仓库 API）──
 * `https://www.modelscope.cn/models/litert-community/Qwen3.5-0.8B`
 * 文本版：964,566,992 B（约 920 MB），sha256 `25447e25…`，Apache-2.0。
 * 要求 LiteRT-LM ≥ 0.15（本项目 0.18.0）。
 *
 * ⚠️ **Qwen3.5 是混合架构（GatedDeltaNet 线性注意力 + 少量全注意力）**，
 * 带常量大小的循环状态。模型卡明确写了：**一个 Engine 只能跑一段会话**，
 * 在跑过对话的 Engine 上新建 Conversation 会继承上一段的状态，
 * 连系统提示词都会被忽略（实测 9 次里 0 次遵守）。见 [LiteRtLmBackend]。
 */
enum class LocalModel(
    val fileName: String,
    val sizeBytes: Long,
    val label: String,
    val note: String,
    /** sha256。为空表示尚未登记 —— 下载后只校验大小，不校验内容 */
    val sha256: String? = null,
) {
    QWEN35_08B(
        fileName = "Qwen3.5-0.8B_int8.litertlm",
        sizeBytes = 964_566_992L,
        label = "Qwen3.5 0.8B",
        note = "文本版 int8，约 920 MB。NPU / GPU / CPU 都能跑，4 GB 内存起。",
        sha256 = "25447e2550082aed6145bbc755ec4da5d535f98f6a0c386865fdae2ccb88aed1",
    ),
    ;

    /** 魔搭下载入口（每次请求会 302 到带签名的 CDN，所以不能缓存 CDN 直链） */
    val downloadUrl: String
        get() = "https://www.modelscope.cn/api/v1/models/litert-community/Qwen3.5-0.8B/repo" +
            "?Revision=master&FilePath=$fileName"

    companion object {

        /** 用户手动指定的变体名；AUTO 表示按设备推荐 */
        fun byName(name: String?): LocalModel? =
            entries.firstOrNull { it.name == name }

        /**
         * 候选列表。
         *
         * 只有一个模型，所以永远返回单元素列表 —— 保留这个签名是为了
         * 不动调用方（`OnDeviceInterpreter.fromDevice`、设置页摘要）。
         *
         * @param variant 用户手动指定的变体；非 AUTO 且名字有效时排在最前
         */
        fun candidatesFor(
            @Suppress("UNUSED_PARAMETER") device: DeviceProfile = DeviceProfile.current(),
            variant: String = "AUTO",
        ): List<LocalModel> {
            val manual = byName(variant)?.takeIf { variant != "AUTO" }
            return listOf(manual ?: QWEN35_08B)
        }
    }
}

/**
 * 设备画像 —— 只用于日志与排查。
 *
 * 换成单模型之后，它不再参与选型；留着是因为出问题时
 * 「用户是什么机器」这一行日志最有用。
 *
 * 注意 `Build.SOC_MODEL` / `SOC_MANUFACTURER` 在部分 ROM 上会被改写成空串
 * 或厂商代号，**不能当作可靠依据**。
 */
data class DeviceProfile(
    val socModel: String,
    val socManufacturer: String,
) {
    /** 给用户看的摘要。诊断问题时让用户截图这一行就够了。 */
    val summary: String
        get() = buildString {
            append(socManufacturer.ifBlank { "未知厂商" })
            append(" ")
            append(socModel.ifBlank { "未知型号" })
            val pick = LocalModel.candidatesFor(this@DeviceProfile).firstOrNull()
            if (pick != null) {
                append(" · 模型「")
                append(pick.label)
                append("」")
            }
        }

    companion object {
        fun current(): DeviceProfile = DeviceProfile(
            socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else "",
            socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Build.SOC_MANUFACTURER
            } else {
                ""
            },
        )
    }
}
