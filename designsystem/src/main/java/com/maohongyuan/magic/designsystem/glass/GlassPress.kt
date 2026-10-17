package com.maohongyuan.magic.designsystem.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
 * catalog/utils/InteractiveHighlight.kt   高光跟随
 * catalog/components/LiquidButton.kt      形变公式
 * ```
 * **参数一个没改** —— 手感就是这些数字，改了就变味。
 *
 * ── 为什么不是「内容缩放」 ──
 * 常见做法是按下时把整个 Composable `scale(0.96)`，那是「按钮变小」，
 * 不是 iOS 那种「玻璃被按下去」。
 *
 * 这里只形变**玻璃层**（`drawBackdrop` 的 `layerBlock`），内容纹丝不动：
 * - 整体轻微**放大**（不是缩小）
 * - 位移用 `tanh` 软饱和，拖不远
 * - 各向异性缩放 —— 朝拖动方向的轴拉长、垂直轴压扁，这才是「水滴」
 */
@Stable
class GlassPressState(
    private val scope: CoroutineScope,
) {
    /** 按压进度 0..1，带回弹 */
    private val progress = Animatable(0f, 0.001f)
    internal val offsetAnim = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)
    private var downAt = Offset.Zero

    private val progressSpec = spring(0.5f, 300f, 0.001f)
    private val offsetSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

    val pressProgress: Float get() = progress.value

    /** 当前手指相对按下点的偏移 */
    internal val dragOffset: Offset get() = offsetAnim.value - downAt

    /**
     * 玻璃层形变。传给 `drawBackdrop(layerBlock = state.layerBlock())`。
     *
     * 注意：`size` 是 `GraphicsLayerScope` 的尺寸，必须在这里取 ——
     * 提前算会拿到 0，缩放比例就全错了。
     */
    fun layerBlock(): GraphicsLayerScope.() -> Unit = {
        val p = progress.value
        if (p > 0.001f) {
            val w = size.width
            val h = size.height
            if (w > 0f && h > 0f) {
                // 整体轻微放大：4dp 相对高度
                val base = lerp(1f, 1f + 4f.dp.toPx() / h, p)

                val d = dragOffset
                val maxOffset = size.minDimension
                val initialDerivative = 0.05f
                translationX = maxOffset * tanh(initialDerivative * d.x / maxOffset)
                translationY = maxOffset * tanh(initialDerivative * d.y / maxOffset)

                // 各向异性：朝拖动方向拉伸，垂直方向压缩
                val maxDragScale = 4f.dp.toPx() / h
                val angle = atan2(d.y, d.x)
                scaleX = base + maxDragScale * abs(cos(angle) * d.x / size.maxDimension) *
                    (w / h).fastCoerceAtMost(1f)
                scaleY = base + maxDragScale * abs(sin(angle) * d.y / size.maxDimension) *
                    (h / w).fastCoerceAtMost(1f)
            }
        }
    }

    /**
     * 高光。传给 `drawBackdrop(highlight = { state.highlight() })`。
     *
     * 按压时变亮、变宽 —— 视觉上是「玻璃被压出一个光斑」。
     */
    fun highlight(): Highlight {
        val p = progress.value
        return Highlight(
            width = 1.dp,
            blurRadius = androidx.compose.ui.unit.lerp(0.dp, 12.dp, p),
            alpha = 0.12f * p,
            style = HighlightStyle.Default,
        )
    }

    /**
     * 手势 + 点击。
     *
     * 用 `pointerInput` 自己跟踪按下/抬起，而不是 `clickable` 的
     * `interactionSource` —— 后者拿不到**手指位置**，高光就跟不上手。
     */
    fun modifier(onClick: () -> Unit): Modifier = Modifier
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    downAt = down.position
                    scope.launch {
                        launch { progress.animateTo(1f, progressSpec) }
                        launch { offsetAnim.snapTo(down.position) }
                    }
                    // 拖动时让玻璃跟着手走
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            val up = change.position
                            val dist = (up - downAt).getDistance()
                            // 抬起时先回弹，再触发点击
                            scope.launch {
                                launch { progress.animateTo(0f, progressSpec) }
                                launch { offsetAnim.animateTo(downAt, offsetSpec) }
                            }
                            if (!moved || dist < viewConfiguration.touchSlop * 2) onClick()
                            break
                        }
                        moved = moved || (change.position - downAt).getDistance() > viewConfiguration.touchSlop
                        scope.launch { offsetAnim.snapTo(change.position) }
                        change.consume()
                    }
                }
            }
        }

}


/**
 * 内容玻璃用的按压形变。
 *
 * 九宫格卡片是「内容玻璃」，本身没有 backdrop，改不了 layerBlock。
 * 这里直接对内容做 graphicsLayer 形变，公式与 layerBlock 一致
 * （tanh 位移 + 各向异性），视觉上仍是「被按下去」。
 */
fun GlassPressState.contentModifier(onClick: () -> Unit): Modifier = modifier(onClick).then(
    Modifier.graphicsLayer {
        val p = pressProgress
        if (p > 0.001f) {
            val w = size.width
            val h = size.height
            if (w > 0f && h > 0f) {
                val base = lerp(1f, 1f - 3f.dp.toPx() / h, p)
                val d = dragOffset
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
).then(
    Modifier.drawWithContent {
        drawContent()
        val p = pressProgress
        if (p > 0.001f) {
            val c = offsetAnim.value
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.18f * p), Color.Transparent),
                    center = c,
                    radius = size.minDimension * 1.5f,
                ),
                radius = size.minDimension * 1.5f,
                center = c,
                blendMode = BlendMode.Plus,
            )
        }
    },
)

@Composable
fun rememberGlassPress(): GlassPressState {
    val scope = rememberCoroutineScope()
    return remember(scope) { GlassPressState(scope) }
}
