package com.maohongyuan.magic.core.ai

import android.os.Build

/**
 * 端上模型的清单与选型。
 *
 * ── 为什么不是「一个模型」 ──
 * 同一份 gemma-4-E2B 权重，谷歌发布了多个 `.litertlm` 变体，
 * 区别**不在质量，在用哪个加速器**：
 *
 * ```
 * qualcomm_sm8750   NPU 专用，最快，只认骁龙 8 Elite
 * -gpu              GPU delegate，几乎所有 Android 都能用
 * 通用              CPU 兜底，兼容性最好、最慢
 * ```
 *
 * ⚠️ **「通用版」不是「普通手机版」，是「CPU 兜底版」。**
 * 普通手机（天玑、麒麟、老骁龙）也该优先下 GPU 版 —— 比 CPU 快得多。
 *
 * ⚠️ **体积不能用来推断质量。** GPU 版（1915 MB）反而比通用版（2468 MB）小，
 * 说明是量化策略不同，不是「精简版」。真实差异只能实测输出与速度才能定。
 *
 * ── 数据来源 ──
 * 魔搭镜像（HuggingFace 国内不可达）：`litert-community/gemma-4-E2B-it-litert-lm`
 * 直链实测支持断点续传（`Accept-Ranges: bytes`，Range 请求返回 206）。
 */
enum class LocalModel(
    val fileName: String,
    val sizeBytes: Long,
    val label: String,
    val note: String,
    /** sha256。为空表示尚未登记 —— 下载后只校验大小，不校验内容 */
    val sha256: String? = null,
    /**
     * 这个变体**只在这些 SoC 上**能用。空集表示通用（不依赖 NPU）。
     *
     * ⚠️ NPU 模型是按具体 SoC 的 NPU 编译的，**换一代就不兼容**。
     * `qualcomm_sm8750` 只认骁龙 8 Elite（SM8750）；
     * 塞给 8 Elite Gen 5（SM8850）或 Gen 6 大概率初始化失败 ——
     * 用户会白下 2.9 GB，然后看到报错。
     *
     * 所以判断不能是「只要是骁龙就用骁龙版」，
     * 必须是「SoC 精确命中才用，否则退 GPU」。
     */
    val npuSocCodes: Set<String> = emptySet(),
) {
    /**
     * 骁龙 8 Elite（SM8750）NPU 专用。
     *
     * 比 GPU 版大 900 MB，因为 NPU 需要预变换的张量布局。
     * 换一颗芯片就用不了 —— 必须按 SoC 精确匹配。
     */
    QUALCOMM_SM8750(
        fileName = "gemma-4-E2B-it_qualcomm_sm8750.litertlm",
        sizeBytes = 2_877L * 1024 * 1024,
        label = "骁龙专用",
        note = "NPU 加速，最快。仅骁龙 8 Elite（SM8750）可用。",
        // 官方只发布了 SM8750 这一版。8 Elite Gen 5 / Gen 6 暂无对应文件，
        // 不能复用 —— NPU 模型不向前兼容。
        npuSocCodes = setOf("SM8750", "SM8750P", "SM8750-AB"),
    ),

    /**
     * 主推变体：LiteRT-LM 的 `.litertlm` 容器。
     *
     * ⚠️ **格式不能混用**，两者文件头不同：
     * ```
     * .litertlm  → magic "LITERTLM"   由 LiteRT-LM 运行时读
     * .task      → magic "TFL3"       由 MediaPipe tasks-genai 读
     * ```
     * 喂错运行时，两边都只报「model is null」这类不含格式信息的错，极难定位。
     *
     * 本项目用 **LiteRT-LM**（`com.google.ai.edge.litertlm:litertlm-android`）：
     * 它支持 NPU 后端，MediaPipe 没有。
     */
    GPU(
        fileName = "gemma-4-E2B-it-gpu.litertlm",
        sizeBytes = 2_008_432_640L,   // 实测 Content-Length
        label = "GPU 加速",
        note = "通用 GPU 加速，绝大多数手机可用。",
    ),

    /** 通用版：CPU 兜底 */
    GENERIC(
        fileName = "gemma-4-E2B-it.litertlm",
        sizeBytes = 2_588_147_712L,   // 实测 Content-Length
        label = "通用",
        note = "含 CPU 图，无 OpenCL 的设备只能用它（最慢但能跑）。",
    ),
    ;

    /** 魔搭下载入口（每次请求会 302 到带签名的 CDN，所以不能缓存 CDN 直链） */
    val downloadUrl: String
        get() = "https://www.modelscope.cn/api/v1/models/litert-community/gemma-4-E2B-it-litert-lm/repo" +
            "?Revision=master&FilePath=$fileName"

    companion object {

        /**
         * 按设备挑最合适的变体。
         *
         * **只做「推荐」，不做「保证」** —— 名字匹配不代表加速器真能用
         * （驱动版本、系统裁剪都可能让 delegate 初始化失败）。
         * 所以下载后必须跑一次自检，失败则降级到下一个候选。
         *
         * @return 按优先级排好的候选，第一个是推荐
         */
        /**
         * 按设备排优先级。
         *
         * 规则只有一条：**NPU 版只在 SoC 精确命中它的 `npuSocCodes` 时进候选**。
         *
         * 为什么不做「骁龙就用骁龙版」：
         * NPU 模型针对具体 NPU 编译，8 Elite（SM8750）的文件拿到
         * 8 Elite Gen 5 / Gen 6 上跑不起来。让用户下 2.9 GB 再失败，
         * 比一开始就给 GPU 版糟糕得多。
         *
         * 新增机型支持的方式：官方发布对应 SoC 的 `.litertlm` 后，
         * 新建一个 [LocalModel] 条目并填好 `npuSocCodes`。
         */
        /** 用户手动指定的变体名；AUTO 表示按设备推荐 */
        fun byName(name: String?): LocalModel? =
            entries.firstOrNull { it.name == name }

        fun candidatesFor(
            device: DeviceProfile = DeviceProfile.current(),
            /** 用户手动指定的变体。非 AUTO 时排在候选最前 */
            variant: String = "AUTO",
        ): List<LocalModel> {
            val manual = byName(variant)?.takeIf { variant != "AUTO" }
            if (manual != null) {
                // 手动选定：先用它，失败再按常规顺序兜底
                return listOf(manual) + candidatesFor(device, "AUTO").filter { it != manual }
            }
            val list = mutableListOf<LocalModel>()
            values()
                .filter { it.npuSocCodes.isNotEmpty() && device.socModel.uppercase() in it.npuSocCodes.map(String::uppercase) }
                .forEach { list += it }
            list += GPU
            list += GENERIC
            return list
        }
    }
}

/**
 * 设备画像 —— 决定推哪个模型变体。
 *
 * 用 `Build.SOC_MODEL` / `SOC_MANUFACTURER`（API 31+，本项目 minSdk 33）。
 * 注意这两个字段在部分 ROM 上会被改写成空串或厂商代号，**不能当作可靠依据**，
 * 所以只用来「推荐」，最终以自检结果为准。
 */
data class DeviceProfile(
    val socModel: String,
    val socManufacturer: String,
) {
    /** 骁龙 8 Elite（SM8750）—— 目前唯一有官方 NPU 模型的机型 */
    val isSnapdragon8Elite: Boolean
        get() = socModel.equals("SM8750", ignoreCase = true) ||
            socModel.equals("SM8750P", ignoreCase = true)

    /**
     * 是否高通平台。
     *
     * ⚠️ **注意：是高通 ≠ 能用骁龙专用模型。**
     * 8 Elite Gen 5（SM8850）、Gen 6 都是高通，但目前没有对应的 `.litertlm`。
     * 这类机型应走 GPU 版 —— Adreno 的 GPU delegate 表现通常很好。
     */
    val isQualcomm: Boolean
        get() = socManufacturer.equals("QTI", ignoreCase = true) ||
            socManufacturer.contains("Qualcomm", ignoreCase = true)

    /** 给用户看的摘要。诊断问题时让用户截图这一行就够了。 */
    val summary: String
        get() = buildString {
            append(socManufacturer.ifBlank { "未知厂商" })
            append(" ")
            append(socModel.ifBlank { "未知型号" })
            val pick = LocalModel.candidatesFor(this@DeviceProfile).firstOrNull()
            if (pick != null) {
                append(" · 推荐「")
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
