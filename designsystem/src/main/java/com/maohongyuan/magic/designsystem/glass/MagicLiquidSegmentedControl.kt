package com.maohongyuan.magic.designsystem.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.MagicType
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * 流体玻璃分段控件 —— **玻璃版**。
 *
 * 结合 [LiquidBottomTabs] 的胶囊形玻璃外壳与 [MagicGlassSlider] 的可拖拽滑块：
 * - 外壳用 [MagicGlassSurface] + [Capsule] 折射页面背景
 * - 滑块可拖拽，松手后按弹簧物理吸附到最近分段
 * - 支持点击轨道任意位置直接跳转
 *
 * ⚠️ **禁止在 [GlassCard] / [MagicGlassSurface] 内部使用。**
 *    这是 No Glass on Glass 原则：卡片已经是玻璃，内部再叠玻璃会导致层级塌陷。
 *    在玻璃卡片内部请改用实色的 [MagicSegmentedControl]。
 *
 * @param options 选项文本
 * @param selectedIndex 当前选中索引
 * @param onSelect 选中回调。拖拽松手或点击时触发
 */
@Composable
fun MagicLiquidSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    if (options.isEmpty()) return

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        val totalPx = with(density) { maxWidth.toPx() }
        val count = options.size
        val itemWidthPx = totalPx / count

        val offsetX = remember { Animatable(0f) }
        var placed by remember { mutableStateOf(false) }
        var isDragging by remember { mutableStateOf(false) }

        LaunchedEffect(selectedIndex, itemWidthPx) {
            if (isDragging) return@LaunchedEffect
            val target = selectedIndex * itemWidthPx
            if (!placed) {
                offsetX.snapTo(target)
                placed = true
            } else {
                offsetX.animateTo(
                    targetValue = target,
                    animationSpec = spring(
                        dampingRatio = 0.80f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
        }

        // ── 外壳：玻璃胶囊（层 1，可见）──
        MagicGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = Capsule(),
            level = GlassLevel.Thick,
            shadow = false,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(4.dp),
            ) {
                // ── 滑块（层 2，实色 accent，避免 Glass on Glass）──
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                offsetX.value
                                    .roundToInt()
                                    .coerceIn(0, (totalPx - itemWidthPx).toInt()),
                                0,
                            )
                        }
                        .fillMaxHeight()
                        .fillMaxWidth(1f / count),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                // 按压时轻微放大（iOS 分段控件滑块行为）
                                val press = if (isDragging) 0.15f else 0f
                                scaleX = 1f - press * 0.05f
                                scaleY = 1f - press * 0.05f
                            }
                            .background(
                                color = colors.accent.copy(alpha = 0.12f),
                                shape = Capsule(),
                            ),
                    )
                }

                // ── 文字层（层 3，在滑块之上）──
                Row(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, label ->
                        val selected = index == selectedIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                fontSize = MagicType.footnote.sp,
                                color = if (selected) colors.accent else colors.secondaryText,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }

                // ── 透明交互层（最顶层，捕获点击与拖拽）──
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(itemWidthPx, count) {
                            detectTapGestures { offset ->
                                val index = (offset.x / itemWidthPx)
                                    .toInt()
                                    .coerceIn(0, count - 1)
                                onSelect(index)
                            }
                        }
                        .pointerInput(itemWidthPx, count, totalPx) {
                            detectHorizontalDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    isDragging = false
                                    val index = (offsetX.value / itemWidthPx + 0.5f)
                                        .toInt()
                                        .coerceIn(0, count - 1)
                                    onSelect(index)
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val raw = offsetX.value + dragAmount
                                    scope.launch {
                                        offsetX.snapTo(
                                            raw.coerceIn(0f, totalPx - itemWidthPx),
                                        )
                                    }
                                },
                            )
                        },
                )
            }
        }
    }
}
