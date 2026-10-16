package com.maohongyuan.magic.designsystem.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur

/** 渐进模糊贴合哪条边 */
enum class BlurEdge { TOP, BOTTOM }

/**
 * 渐进模糊（Progressive Blur）—— 边缘强、向内递减的模糊。
 *
 * ── 说明：这不是 backdrop 库提供的效果 ──
 * 通读 `kyant.gitbook.io/backdrop` 的教程目录与 `api/backdrop-effects` 后确认：
 * 库**没有** progressive blur 这个 API（`blur()` 是均匀模糊）。
 * 这是 iOS 26/27 的滚动边缘效果，本文件自行实现。
 *
 * ── 实现思路 ──
 * Compose 的 `blur()` 半径是**空间均匀**的，做不出「越靠边越糊」。
 * 这里用**多层递减模糊 + 渐变遮罩**叠加来逼近：
 *
 * ```
 * 层 0（最底）：模糊最弱，渐变覆盖整条带子
 * 层 1        ：模糊中等，渐变只覆盖靠边的 2/3
 * 层 2（最上）：模糊最强，渐变只覆盖靠边的 1/3
 * ```
 *
 * 每层用 `BlendMode.DstIn` 按垂直渐变擦除，于是合成结果是
 * **贴边处最糊、向内逐渐清晰**，与 iOS 的滚动边缘效果一致。
 *
 * 层数越多越平滑，代价是每层一次离屏模糊。默认 3 层，
 * 在 88dp 这个尺度上已经看不出台阶。
 *
 * @param height 带子的高度（通常与顶/底遮罩区一致）
 * @param maxBlur 贴边处最大模糊半径
 * @param layers 叠加层数。≥ 2
 */
@Composable
fun MagicProgressiveBlur(
    height: Dp,
    modifier: Modifier = Modifier,
    maxBlur: Dp = 18.dp,
    edge: BlurEdge = BlurEdge.TOP,
    layers: Int = 3,
    backdrop: Backdrop? = null,
    /** 叠在最上层的渐变遮罩色（可选），用于进一步压暗或提亮边缘 */
    scrimColor: Color? = null,
    /** scrimColor 在贴边处的最大不透明度 */
    scrimMaxAlpha: Float = 0.0f,
) {
    val resolvedBackdrop = backdrop ?: LocalContentBackdrop.current ?: LocalBackdrop.current
    val n = layers.coerceAtLeast(2)

    Box(modifier = modifier.fillMaxWidth().height(height)) {
        for (i in 0 until n) {
            // i = 0 是最底层（模糊最弱、覆盖最广）；i = n-1 是最上层（最糊、只贴边）
            val strength = (i + 1).toFloat() / n
            val blurAmount = maxBlur * strength
            // 覆盖范围：最底层铺满，越往上层越短
            val extent = 1f - i.toFloat() / n

            val stops = if (edge == BlurEdge.TOP) {
                arrayOf(0f to Color.Black, extent to Color.Transparent)
            } else {
                arrayOf((1f - extent) to Color.Transparent, 1f to Color.Black)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    // 每层必须有独立离屏层，否则 DstIn 会擦到别的层
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(height)
                        .drawBackdrop(
                            backdrop = resolvedBackdrop,
                            shape = { RectangleShape },
                            effects = { blur(blurAmount.toPx()) },
                        ),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(height)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = stops,
                                    startY = 0f,
                                    endY = size.height,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        },
                )
            }
        }

        if (scrimColor != null && scrimMaxAlpha > 0f) {
            val stops = if (edge == BlurEdge.TOP) {
                arrayOf(0f to scrimColor.copy(alpha = scrimMaxAlpha), 1f to Color.Transparent)
            } else {
                arrayOf(0f to Color.Transparent, 1f to scrimColor.copy(alpha = scrimMaxAlpha))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = stops,
                                startY = 0f,
                                endY = size.height,
                            ),
                        )
                    },
            )
        }
    }
}
