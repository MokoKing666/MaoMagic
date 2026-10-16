package com.maohongyuan.magic.designsystem.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Immutable
data class LiquidTab(
    /** Tab 简称，与页面大标题（全称）区分：Tab 用「梅花」，页面标题用「梅花易数」 */
    val label: String,
    val icon: MagicTabIcon,
)

/** 选中指示器高度。48dp 使其胶囊半径恰好等于「外壳半径 − 内边距」，满足同心条件。 */
private val IndicatorHeight = 48.dp

/** 外壳内边距。同心公式的 padding 项。 */
private val BarPadding = 8.dp

/** 指示器在滑动途中最大横向拉伸比例。这是「流体」感的来源。 */
private const val StretchPeak = 1.10f

/**
 * 玻璃底部导航栏。
 *
 * 使用**内容折射源**（[LocalContentBackdrop]），因此页面内容滚动经过它下方时
 * 会被真实折射 —— 这是「流体」感的来源。
 *
 * 必须放在 [MagicBackdrop] 的 `overlay` 插槽里（与内容层平级），
 * 否则会与被 `layerBackdrop` 标记的子树形成引用环并崩溃。
 *
 * ── 几何约束 ──
 * 选中指示器不是「随便一个圆角块」。它与外壳必须**同心**：
 * ```
 * r_indicator = r_bar − BarPadding
 *             = 32dp − 8dp = 24dp
 * ```
 * 指示器高度取 48dp ⇒ 胶囊半径 = 48/2 = 24dp ⇒ 与上式**恰好相等**。
 * 外壳用 `RoundedCornerShape(percent = 50)`，因此无论高度怎么变都保持 h/2，
 * 这条等式不会因为系统字号放大而失效。
 *
 * ── 指示器只有**一个** ──
 * 早期实现让每个 item 各自 `animateColorAsState` 自己的背景色，结果是高亮
 * **原地淡入淡出**，从不位移 —— 看起来「没有动画」。现在改为一个共享指示器，
 * 用 [Animatable] 驱动它的 x 偏移跨 item 滑动，并在途中做一次横向拉伸回弹。
 *
 * ── 关于指示器的材质 ──
 * 指示器**不**使用独立的 `drawBackdrop`。它靠「半透明白 + 1dp 内描边」表达，
 * 视觉上读作底栏玻璃的一部分。这样既拿到 iOS 那种「材质内选中态」的观感，
 * 又不引入玻璃叠玻璃（每多一层就是一次全屏纹理拷贝 + shader 采样）。
 */
@Composable
fun LiquidBottomTabs(
    tabs: List<LiquidTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalMagicShapes.current
    val colors = LocalMagicColors.current
    val contentBackdrop = LocalContentBackdrop.current
    val density = LocalDensity.current

    val gapPx = with(density) { MagicSpacing.row.toPx() }

    // 等宽 item，指示器的位置可以直接算出来，不需要给每个 item 记坐标。
    var rowWidthPx by remember { mutableIntStateOf(0) }
    val itemWidthPx = if (tabs.isEmpty() || rowWidthPx <= 0) {
        0f
    } else {
        (rowWidthPx - gapPx * (tabs.size - 1)) / tabs.size
    }
    val itemWidthDp = with(density) { itemWidthPx.toDp() }

    // 位移与形变分开驱动：位移用 spring，宽度用一次「拉伸 → 回弹」
    val indicatorX = remember { Animatable(0f) }
    val indicatorStretch = remember { Animatable(1f) }
    var placed by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIndex, itemWidthPx) {
        if (itemWidthPx <= 0f) return@LaunchedEffect
        val target = selectedIndex * (itemWidthPx + gapPx)

        // 首帧直接落位，避免刚进入页面时指示器从最左侧滑过来
        if (!placed) {
            indicatorX.snapTo(target)
            placed = true
            return@LaunchedEffect
        }

        launch {
            indicatorStretch.snapTo(1f)
            indicatorStretch.animateTo(
                targetValue = StretchPeak,
                animationSpec = tween(durationMillis = 130, easing = FastOutSlowInEasing),
            )
            indicatorStretch.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.50f,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }

        indicatorX.animateTo(
            targetValue = target,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    // 同心圆角：外壳胶囊半径 32dp − 内边距 8dp = 24dp = 指示器高度的一半
    val indicatorShape = remember { RoundedCornerShape(IndicatorHeight / 2) }

    MagicGlassSurface(
        // 不写死高度：让内容决定，避免不同字号/系统缩放下标签被裁切
        modifier = modifier.fillMaxWidth(),
        shape = shapes.tabBar,
        level = GlassLevel.Thick,
        backdrop = contentBackdrop,
        // 覆盖层不投影：iOS 靠材质 + 内描边分层
        shadow = false,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 拉伸中的指示器不能被外壳圆角切掉
                .clip(shapes.tabBar)
                .padding(BarPadding)
                .onSizeChanged { rowWidthPx = it.width },
        ) {
            // ① 唯一的那一个指示器
            if (itemWidthPx > 0f) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorX.value.roundToInt(), 0) }
                        .width(itemWidthDp * indicatorStretch.value)
                        .height(IndicatorHeight)
                        .clip(indicatorShape)
                        .background(Color.White.copy(alpha = 0.30f))
                        .border(1.dp, colors.glassStroke, indicatorShape),
                )
            }

            // ② item 只负责内容，不再各自画背景
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    LiquidTabItem(
                        tab = tab,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidTabItem(
    tab: LiquidTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> colors.accent
            pressed -> colors.primaryText
            else -> colors.secondaryText
        },
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "tabColor",
    )

    // iOS 的按压反馈是「轻微下沉」，不是 Material ripple。
    // 早期实现用了 indication = null 且没有 pressed 态 —— 按下去毫无响应。
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )

    Box(
        modifier = modifier
            .height(IndicatorHeight)
            .clip(RoundedCornerShape(IndicatorHeight / 2))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.scale(pressScale),
        ) {
            // 选中态填充、未选中描边 —— 不只靠颜色区分（色觉障碍友好）
            MagicTabIconView(
                icon = tab.icon,
                tint = contentColor,
                selected = selected,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = tab.label,
                fontSize = MagicType.footnote.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
            )
        }
    }
}
