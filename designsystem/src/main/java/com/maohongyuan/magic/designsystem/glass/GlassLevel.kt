package com.maohongyuan.magic.designsystem.glass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 玻璃强度总开关。
 *
 * 全局玻璃化有真实的性能与可读性代价，因此必须给用户一个降级开关：
 * 低端机、晕动敏感、省电场景都能用。
 */
enum class GlassMode {
    /** 全效果：vibrancy + blur + lens 折射/色散 */
    FULL,

    /** 降级：只保留 vibrancy + blur，关闭 lens（省掉 RuntimeShader 采样） */
    REDUCED,

    /** 关闭：不使用 backdrop，改用高不透明度实色表面（可读性最好、性能最省） */
    OFF,
}

val LocalGlassMode = staticCompositionLocalOf { GlassMode.FULL }

/**
 * 玻璃档位。
 *
 * 三档对应不同用途，兼顾观感与可读性：
 * - [Thick]  底部 Tab 栏、大标题栏 —— 最厚，折射最明显
 * - [Regular] 卡片、弹窗、按钮 —— 默认
 * - [Thin]   列表行、密集信息区 —— 最薄，优先保证文字可读性
 *
 * ⚠️ lens 的 height 会被自动收敛到 `shape.minCornerRadius` 以内，
 *    amount 收敛到 `size.minDimension` 以内，避免圆角处出现不连续。
 */
@Immutable
data class GlassLevel(
    val blur: Dp,
    val lensHeight: Dp,
    val lensAmount: Dp,
    /** 亮色主题下的表面色不透明度（白叠在亮背景上 —— 遮住背景） */
    val surfaceAlphaLight: Float,
    /**
     * 暗色主题下的表面色不透明度（**白**叠在暗背景上 —— 抬升表面）。
     *
     * ⚠️ 与 [surfaceAlphaLight] 机制不同，数值不可互换：
     * 亮色靠「白遮住背景」保可读性，深色靠「backdrop 自身不透明」保可读性，
     * 白只负责把卡片从背景里抬起来。所以深色的值必须**很小**（0.07~0.12）。
     * 早期把深色也写成 0.60 并改用黑底，导致卡片比背景更暗 —— 层级整个反了。
     */
    val surfaceAlphaDark: Float,
) {
    companion object {
        /**
         * 底部 Tab 栏、大标题栏。
         *
         * 表面色 **0.62** —— 与 [Regular] 持平，不更低。
         *
         * 这里踩过一个坑：原值 0.55 比卡片的 0.62 更透，
         * 但 Tab 标签只有 13sp（比卡片正文 17sp 更小）。
         * 结果是「更小的字放在更透的表面上」—— 可读性风险方向正好反了。
         * 悬浮控件比内容卡片更透是**审美**诉求，不能凌驾于可读性之上。
         */
        val Thick = GlassLevel(
            // blur 8dp / lens 12·24 —— 照抄 Kyant0 LiquidBottomTabs 与 LiquidButton。
            // 原来 blur 24dp 是「三倍厚度」，玻璃被糊成磨砂，反而不像玻璃。
            // 照抄 Kyant0 LiquidBottomTabs：blur 8dp / lens 24·24
            blur = 8.dp,
            lensHeight = 24.dp,
            lensAmount = 24.dp,
            // 照抄 containerColor：近白 40% / 近黑 40%
            surfaceAlphaLight = 0.40f,
            surfaceAlphaDark = 0.40f,
        )

        /**
         * 卡片、弹窗、按钮（默认）。
         *
         * 表面色 0.62 —— 这是**可读性的底线**，不是审美选择。
         * 实测：表面色低于 0.55 时，正文（17sp）在浅色玻璃上的有效对比度会跌破 4.5:1。
         * blur 24 + 高饱和 是玻璃质感的来源，靠它保住「玻璃感」而不是靠降低不透明度。
         */
        val Regular = GlassLevel(
            // 介于 LiquidButton 与 LiquidBottomTabs 之间：blur 8dp / lens 12·24
            blur = 8.dp,
            lensHeight = 12.dp,
            lensAmount = 24.dp,
            surfaceAlphaLight = 0.40f,
            surfaceAlphaDark = 0.40f,
        )

        /**
         * 列表行、密集信息区 —— 无 lens。
         *
         * 表格化数据（爻位/干支/六亲/六神）必须像素级可读，不允许折射干扰。
         *
         * 深色下抬升与 [Regular] 持平（0.08）：**抬得越高，浅色文字的对比度越低**，
         * 所以「密集数据要更不透明」这条在深色下不能翻译成「抬得更高」。
         * 边界清晰度由内描边 + 顶边高光负责，不靠继续抬升。
         */
        val Thin = GlassLevel(
            blur = 12.dp,
            lensHeight = 0.dp,
            lensAmount = 0.dp,
            surfaceAlphaLight = 0.72f,
            surfaceAlphaDark = 0.08f,
        )
    }
}

/** 按 [GlassMode] 降级档位 */
internal fun GlassLevel.degradeTo(mode: GlassMode): GlassLevel = when (mode) {
    GlassMode.FULL -> this
    GlassMode.REDUCED -> copy(lensHeight = 0.dp, lensAmount = 0.dp)
    GlassMode.OFF -> copy(blur = 0.dp, lensHeight = 0.dp, lensAmount = 0.dp)
}
