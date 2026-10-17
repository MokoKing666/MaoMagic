package com.maohongyuan.magic.core.data

import kotlinx.serialization.Serializable

/**
 * 玻璃质感档位。
 *
 * 与设计系统的 `GlassMode` 一一对应，但定义在 core 层是为了让设置能被持久化 ——
 * 设计系统不该知道存储的存在。
 */
enum class GlassQuality(val label: String, val description: String) {
    FULL("完整", "全部玻璃效果，最贴近 iOS 观感"),
    REDUCED("简化", "去掉折射，只保留模糊与表面"),
    OFF("关闭", "实色表面，性能最省"),
}

/** 明暗模式 */
enum class ThemeMode(val label: String) {
    /** 跟随系统（默认） */
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
}

/** 吉凶配色方案 */
enum class ColorScheme(val label: String) {
    /** 中式：吉=红、凶=绿 */
    CHINESE("中式"),

    /** 西式：吉=绿、凶=红 */
    WESTERN("西式"),
}

/**
 * 应用设置。
 *
 * 全部字段都有默认值 —— 旧版本升级上来时缺字段也能正常反序列化
 * （`Json { ignoreUnknownKeys = true; encodeDefaults = true }` + 默认值）。
 */
@Serializable
data class MagicSettings(
    // ── 外观 ──
    /**
     * 明暗模式。默认跟随系统 —— 用户没主动选过时，不该替他决定。
     */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val glassQuality: GlassQuality = GlassQuality.FULL,
    val colorScheme: ColorScheme = ColorScheme.CHINESE,
    /**
     * 玻璃折射强度倍率，0.2..1.6。
     *
     * 1.0 = 设计标定值。调低更「平」，调高更「液体」——
     * 但过高会让色散把周围明暗差读成色差（整片发灰蓝），故上限只到 1.6。
     */
    val refractionScale: Float = 1.0f,
    /**
     * 玻璃表面不透明度倍率，0.6..1.4。
     *
     * 可读性的底线是表面不透明度 0.62（亮色）。允许调低是为了照顾
     * 「就要那个通透感」的用户，但低于 0.8 倍时会显示可读性提示。
     */
    val surfaceAlphaScale: Float = 1.0f,

    // ── 解卦 ──
    /**
     * 解卦引擎档位。
     *
     * 存字符串而非枚举，避免将来枚举改名导致旧数据反序列化失败。
     */
    val engineTier: String = "RULE",

    /** 起卦后自动保存到历史 */
    val autoSaveHistory: Boolean = true,

    // ── 智能模式（云端 AI，**可选且默认关闭**）──────────────
    //
    // ⚠️ 这是本应用唯一会联网的功能。以下四项全部为空/关闭时，
    //    CloudAiInterpreter.isAvailable() 恒为 false，降级链直接落到 Tier1，
    //    **不会产生任何网络流量**。
    //
    // API Key **不在这里** —— 它存在 SecretStore（Android Keystore 加密），
    // 因为这个 data class 会被序列化成明文 JSON 落盘。
    val cloudAiEnabled: Boolean = false,
    val cloudAiBaseUrl: String = "",
    val cloudAiModel: String = "",
    val cloudAiTemperature: Float = 0.3f,
) {
    companion object {
        val DEFAULT = MagicSettings()

        /** 折射倍率的可调范围 */
        val REFRACTION_RANGE = 0.2f..1.6f

        /** 表面不透明度倍率的可调范围 */
        val SURFACE_ALPHA_RANGE = 0.6f..1.4f
    }
}
