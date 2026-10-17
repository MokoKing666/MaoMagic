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
        note = "NPU 加速，最快。仅骁龙 8 Elite 可用。",
    ),

    /** GPU 版：绝大多数 Android 的首选 */
    GPU(
        fileName = "gemma-4-E2B-it-gpu.litertlm",
        sizeBytes = 2_008_432_640L,   // 实测 Content-Length
        label = "GPU 加速",
        note = "通用 GPU 加速，绝大多数手机可用。",
    ),

    /** 通用版：CPU 兜底 */
    GENERIC(
        fileName = "gemma-4-E2B-it.litertlm",
        sizeBytes = 2_468L * 1024 * 1024,
        label = "通用",
        note = "CPU 运行，兼容性最好但最慢。",
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
        fun candidatesFor(device: DeviceProfile = DeviceProfile.current()): List<LocalModel> {
            val list = mutableListOf<LocalModel>()
            // NPU 版只在 SoC 精确匹配时才推荐
            if (device.isSnapdragon8Elite) list += QUALCOMM_SM8750
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
    /** 骁龙 8 Elite 的 SoC 代号是 SM8750 */
    val isSnapdragon8Elite: Boolean
        get() = socModel.equals("SM8750", ignoreCase = true) ||
            socModel.equals("SM8750P", ignoreCase = true)

    val isQualcomm: Boolean
        get() = socManufacturer.equals("QTI", ignoreCase = true) ||
            socManufacturer.contains("Qualcomm", ignoreCase = true)

    /** 给用户看的摘要 */
    val summary: String
        get() = buildString {
            append(socManufacturer.ifBlank { "未知厂商" })
            append(" ")
            append(socModel.ifBlank { "未知型号" })
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
