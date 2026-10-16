package com.maohongyuan.magic.designsystem.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * 连续曲率圆角 —— iOS 26/27 风格的圆角构造。
 *
 * ── 为什么不能直接用 `RoundedCornerShape` ──
 * `RoundedCornerShape` 生成的是**正圆弧**：一段 90° 圆 + 两条直线，
 * 在「弧 ↔ 直线」接点处曲率从 `1/r` 直接跳到 `0`。
 * 实测苹果 iOS 26 的圆角不是正圆弧，也不是标准 squircle：
 *
 * ```
 * SHAPE          4586
 * RADIUS         1500
 * EXTENSION       793       ← 圆弧之外还有一段「扩展过渡段」
 * ARC RATIO     5 : 9       ← 圆弧只占角部总长的 5/9
 * ARC CURVATURE  1500 : 1397.68
 * CONTINUITY     不是 G1（曲率梳法线不连续），也不是 G2（曲率不连续）
 * ```
 * 来源：nevoit 对 iOS 26 圆角的数据挖掘（经知乎整理）。
 *
 * 本实现用**三次贝塞尔**逼近这一族曲线：每个角一段三次贝塞尔，
 * 端点落在两条直边上、手柄沿直边方向延伸，因此与直边切线连续。
 * 手柄长度由 [smoothing] 控制，越大角部越饱满、越接近连续曲率：
 *
 * ```
 * smoothing = 0.00  →  手柄 0.5523·r  →  相对圆弧径向偏差 0.0%   （正圆弧）
 * smoothing = 0.35  →  手柄 0.6040·r  →  径向偏差 2.6%          （默认，iOS 观感）
 * smoothing = 1.00  →  手柄 0.7000·r  →  径向偏差 7.8%          （大半径下明显）
 * ```
 * 手柄上限刻意压在 0.70 而不是 1.0：0.82 时偏差已达 14%，圆角会读成「方形」，
 * 不再是 iOS 的观感。这是实测算出来的，不是拍的。
 *
 * ── 诚实的边界 ──
 * 这不是苹果原始曲线的逐点复刻（苹果未公开参数，第三方挖掘只给出比例，
 * 无法唯一还原曲线）。它解决的是**族**的问题：让圆角离开「正圆弧」这一族。
 *
 * 在 28–36dp 这个尺度上，2.6% 的偏差约合 **2.5px @3.5x**，
 * 属于「看得出更顺，但说不出哪里不一样」的量级。
 * 真正会拉开差距的场合是：半径很大时（弹窗、平板分栏）、做圆角形变动画时、
 * 以及内外层需要同心排布时。
 *
 * 必须继承 [CornerBasedShape] —— 这是 `lens()` 折射的硬性前提，
 * 见 [com.maohongyuan.magic.designsystem.glass.MagicGlassSurface]。
 * 注意：本版本 Compose 中 `CornerBasedShape.createOutline(size, layoutDirection, density)`
 * 是 **final**，可覆写的是带四个已解析半径的重载。
 */
@Immutable
class MagicSmoothCornerShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    /** 0 = 正圆弧，1 = 最饱满。见类注释。 */
    val smoothing: Float = DefaultSmoothing,
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    /** 四角同半径。 */
    constructor(all: CornerSize, smoothing: Float = DefaultSmoothing) :
        this(all, all, all, all, smoothing)

    /** 四角同半径（Dp 便捷写法）。 */
    constructor(all: Dp, smoothing: Float = DefaultSmoothing) :
        this(CornerSize(all), smoothing)

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        // 基类已完成 toPx 与合法化钳制，这里拿到的就是最终像素半径。
        val path = Path()
        path.buildSmoothRoundRect(
            width = size.width,
            height = size.height,
            topLeft = topStart,
            topRight = topEnd,
            bottomRight = bottomEnd,
            bottomLeft = bottomStart,
            smoothing = smoothing,
        )
        return Outline.Generic(path)
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = MagicSmoothCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
        smoothing = smoothing,
    )

    override fun toString(): String = "MagicSmoothCornerShape(smoothing=$smoothing)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MagicSmoothCornerShape) return false
        return topStart == other.topStart &&
            topEnd == other.topEnd &&
            bottomEnd == other.bottomEnd &&
            bottomStart == other.bottomStart &&
            smoothing == other.smoothing
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        result = 31 * result + smoothing.hashCode()
        return result
    }

    companion object {
        /**
         * 默认平滑度。手柄 0.6040·r，相对正圆弧的径向偏差 2.6% ——
         * 在 28dp 半径上约合 2.5px @3.5x，观感更顺但不会读成方形。
         */
        const val DefaultSmoothing: Float = 0.35f
    }
}

/** 正圆弧的三次贝塞尔手柄系数（4/3·(√2−1) 的经典近似）。 */
private const val CircleHandle: Float = 0.5522848f

/**
 * 手柄系数上限。刻意取 0.70 而非 1.0：
 * 0.70 → 径向偏差 7.8%（大半径下明显但不失真）
 * 0.82 → 径向偏差 14.2%（圆角开始读成方形，已不是 iOS 观感）
 */
private const val MaxHandle: Float = 0.70f

/**
 * 顺时针构建一条「连续曲率圆角矩形」路径。
 *
 * 每个角一段三次贝塞尔：端点落在两条直边上，手柄沿直边方向延伸。
 * `handle = CircleHandle + (MaxHandle − CircleHandle) · smoothing`
 * ⇒ smoothing = 0 时手柄 0.5523r，与正圆弧几乎重合（径向偏差 0.03%）。
 */
private fun Path.buildSmoothRoundRect(
    width: Float,
    height: Float,
    topLeft: Float,
    topRight: Float,
    bottomRight: Float,
    bottomLeft: Float,
    smoothing: Float,
) {
    val s = smoothing.coerceIn(0f, 1f)
    val h = CircleHandle + (MaxHandle - CircleHandle) * s

    moveTo(0f, height - bottomLeft)

    // 左边 → 左上角
    lineTo(0f, topLeft)
    if (topLeft > 0f) {
        cubicTo(
            0f, topLeft - topLeft * h,
            topLeft - topLeft * h, 0f,
            topLeft, 0f,
        )
    }

    // 上边 → 右上角
    lineTo(width - topRight, 0f)
    if (topRight > 0f) {
        cubicTo(
            width - topRight + topRight * h, 0f,
            width, topRight - topRight * h,
            width, topRight,
        )
    }

    // 右边 → 右下角
    lineTo(width, height - bottomRight)
    if (bottomRight > 0f) {
        cubicTo(
            width, height - bottomRight + bottomRight * h,
            width - bottomRight + bottomRight * h, height,
            width - bottomRight, height,
        )
    }

    // 下边 → 左下角
    lineTo(bottomLeft, height)
    if (bottomLeft > 0f) {
        cubicTo(
            bottomLeft - bottomLeft * h, height,
            0f, height - bottomLeft + bottomLeft * h,
            0f, height - bottomLeft,
        )
    }

    close()
}
