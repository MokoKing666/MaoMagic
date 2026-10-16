package com.maohongyuan.magic.designsystem.glass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 玻璃渲染的可调参数。
 *
 * 与 [GlassMode]（档位开关）正交：档位决定「用哪几层效果」，
 * 调校决定「效果有多强」。用户在设置页拖动滑块改的就是这里。
 *
 * ⚠️ 两个倍率都是**围绕设计标定值 1.0 缩放**，不是绝对值。
 *    设计标定的数值（表面 0.62 / 折射量夹到短边 1/4）是实测出来的可读性底线，
 *    倍率只是让用户在这个底线附近微调，不改变标定本身。
 */
@Immutable
data class GlassTuning(
    /**
     * 折射强度倍率。
     *
     * 上限刻意压到 1.6：再高会让 `chromaticAberration` 的采样跨度跨出元素边界，
     * 把周围明暗差读成色差，整片发灰蓝（这个坑在 MagicGlassSurface 里记过）。
     */
    val refractionScale: Float = 1f,

    /**
     * 表面不透明度倍率。
     *
     * 这是可读性的直接控制杆：调低会更通透，但正文对比度会掉。
     * 低于 0.8 时设置页应给出可读性提示。
     */
    val surfaceAlphaScale: Float = 1f,
)

val LocalGlassTuning = staticCompositionLocalOf { GlassTuning() }
