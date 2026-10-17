package com.maohongyuan.magic.designsystem.glass

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * 玻璃按压张力。
 *
 * ── 抄自哪里 ──
 * `Kyant0/AndroidLiquidGlass`（默认分支 `kmp`）：
 * ```
 * utils/InteractiveHighlight.kt   跟手高光
 * components/LiquidButton.kt      形变公式
 * ```
 * **参数一个没改。**
 *
 * ── 只形变玻璃层，内容不动 ──
 * 常见做法是按下时把整个 Composable `scale(0.96)` —— 那是「按钮变小」，
 * 不是 iOS 那种「玻璃被按下去」。
 *
 * 这里改的是 `drawBackdrop` 的 `layerBlock`：
 * - 整体轻微**放大**（不是缩小）
 * - 位移用 `tanh` 软饱和
 * - 各向异性缩放 —— 朝拖动方向拉长、垂直方向压扁，这才是「水滴」
 *
 * 高光交给 [InteractiveHighlight]：手指按住哪儿，哪儿就亮。
 */
@Stable
class GlassPressState(
    private val scope: CoroutineScope,
) {
    private val highlight = InteractiveHighlight(scope)

    /** 按压进度 0..1，带回弹 */
    val pressProgress: Float get() = highlight.pressProgress

    /**
     * 玻璃层形变。传给 `drawBackdrop(layerBlock = press.layerBlock())`。
     *
     * `size` 必须在 `GraphicsLayerScope` 里取 —— 提前算会拿到 0，比例全错。
     */
    fun layerBlock(): GraphicsLayerScope.() -> Unit = {
        val p = highlight.pressProgress
        if (p > 0.001f) {
            val w = size.width
            val h = size.height
            if (w > 0f && h > 0f) {
                val base = lerp(1f, 1f + 4f.dp.toPx() / h, p)

                val d = highlight.offset
                val maxOffset = size.minDimension
                val initialDerivative = 0.05f
                translationX = maxOffset * tanh(initialDerivative * d.x / maxOffset)
                translationY = maxOffset * tanh(initialDerivative * d.y / maxOffset)

                val maxDragScale = 4f.dp.toPx() / h
                val angle = atan2(d.y, d.x)
                scaleX = base + maxDragScale * abs(cos(angle) * d.x / size.maxDimension) *
                    (w / h).fastCoerceAtMost(1f)
                scaleY = base + maxDragScale * abs(sin(angle) * d.y / size.maxDimension) *
                    (h / w).fastCoerceAtMost(1f)
            }
        }
    }

    /** 跟手高光，画在内容之上 */
    val highlightModifier: Modifier get() = highlight.modifier

    /** 按下 / 拖动 / 抬起跟踪 */
    val gestureModifier: Modifier get() = highlight.gestureModifier

    /**
     * 点击 + 按压跟踪。
     *
     * `indication = null`：按压视觉由 [InteractiveHighlight] 提供，
     * 再叠一层系统 ripple 就重了。
     */
    fun modifier(onClick: () -> Unit): Modifier =
        Modifier
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
            .then(highlight.gestureModifier)
            .then(highlight.modifier)

    /**
     * 内容玻璃（没有自己 backdrop 的元素）用的等效形变。
     *
     * 公式与 [layerBlock] 一致，只是作用对象从「玻璃层」换成「整个内容」。
     * 九宫格卡片、列表行这类 [GlassCard] 用它。
     */
    fun contentModifier(onClick: () -> Unit): Modifier = modifier(onClick).then(
        Modifier.graphicsLayer {
            val p = highlight.pressProgress
            if (p > 0.001f) {
                val w = size.width
                val h = size.height
                if (w > 0f && h > 0f) {
                    val base = lerp(1f, 1f - 3f.dp.toPx() / h, p)
                    val d = highlight.offset
                    val maxOffset = size.minDimension
                    translationX = maxOffset * tanh(0.05f * d.x / maxOffset)
                    translationY = maxOffset * tanh(0.05f * d.y / maxOffset)
                    val maxDragScale = 3f.dp.toPx() / h
                    val angle = atan2(d.y, d.x)
                    scaleX = base + maxDragScale * abs(cos(angle) * d.x / size.maxDimension) *
                        (w / h).fastCoerceAtMost(1f)
                    scaleY = base + maxDragScale * abs(sin(angle) * d.y / size.maxDimension) *
                        (h / w).fastCoerceAtMost(1f)
                }
            }
        },
    )
}

@Composable
fun rememberGlassPress(): GlassPressState {
    val scope = rememberCoroutineScope()
    return remember(scope) { GlassPressState(scope) }
}
