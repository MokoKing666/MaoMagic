package com.maohongyuan.magic.designsystem.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.Shadow
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.MagicSpacing

/**
 * 玻璃层级。
 *
 * **全局只允许两层，且不得相邻嵌套**（No Glass on Glass）：
 * - [CONTENT]  内容卡片。内部控件必须用实心填充，不得再用玻璃。
 * - [OVERLAY]  悬浮控件（Tab 栏、弹窗、浮动按钮）。
 *
 * 违反这条规则会让「什么浮在什么之上」变得无法辨认 —— 层级塌陷，
 * 同时每多一层玻璃就多一次全屏纹理拷贝 + shader 采样。
 */
enum class GlassLayer { CONTENT, OVERLAY }

/**
 * 流体玻璃表面 —— 全局玻璃化的基本单元。
 *
 * 默认使用**环境折射源**（[LocalBackdrop]），因此可以安全地嵌套在任意深度的
 * 内容树里，包括 `LazyColumn` 的 item 内部。
 *
 * 若要真实折射页面内容（**仅限覆盖层**），传入 `backdrop = LocalContentBackdrop.current`。
 *
 * ⚠️ 调用顺序强制：`color filter ⇒ blur ⇒ lens`。本实现内部已按此顺序书写，
 *    修改时不要打乱。
 *
 * ⚠️ 卡片边界（内描边 + 顶边高光）不是装饰。缺了它们，卡片与背景之间没有
 *    分界，层级会完全塌陷 —— 把界面转成灰度后就看不出哪层在哪层之上。
 *
 * @param shape 必须是 [CornerBasedShape] 才能启用 lens 折射（[lens] 的硬性前提）。
 *              传入其它 Shape 时自动跳过 lens，仅保留 blur，不会崩溃。
 * @param shadow 是否投影。**覆盖层（Tab 栏、导航栏）必须传 false**：
 *               iOS 26/27 靠材质 + 内描边 + 顶边高光分层，不靠投影。
 *               投影会让玻璃读成「浮起来的塑料卡片」，是「不像 iOS」的主要来源之一。
 */
@Composable
fun MagicGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = LocalMagicShapes.current.card,
    level: GlassLevel = GlassLevel.Regular,
    backdrop: Backdrop? = null,
    shadow: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalMagicColors.current
    val mode = LocalGlassMode.current
    val resolvedBackdrop = backdrop ?: LocalBackdrop.current
    val effective = level.degradeTo(mode)

    val surfaceAlpha = if (colors.isLight) level.surfaceAlphaLight else level.surfaceAlphaDark

    // ⚠️ 表面色**一律用白**，深色主题下也绝不能用黑。
    //
    // 这里踩过一个坑：早期实现写成 `if (isLight) White else Black`，
    // 结果深色模式下「黑 0.60」叠在暗背景上，算出来的卡片比背景**更暗** ——
    // 卡片读作「在页面上挖了个洞」，而不是「抬起的面板」。
    // iOS 深色模式的抬升表面是**更亮**的（材质把表面朝光源方向提）。
    //
    // 亮色：白 0.62 —— 遮住背景，保证正文 ≥ 4.5:1。
    // 深色：白 0.07 —— 只做轻微抬升；可读性由 backdrop 自身不透明（首行 drawRect）保证，
    //       不靠表面色去遮。两者机制不同，数值不可互换。
    val surfaceColor = Color.White.copy(alpha = surfaceAlpha)

    // ── OFF：完全放弃 backdrop，改用实色表面，性能最省 ──
    if (mode == GlassMode.OFF) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(
                    // 亮色近不透明白（遮住背景）；深色低透明白（抬升页面底色）
                    Color.White.copy(alpha = if (colors.isLight) 0.94f else 0.10f)
                ),
            content = content,
        )
        return
    }

    // lens 只支持 CornerBasedShape；非圆角形状静默跳过折射
    val cornerShape = shape as? CornerBasedShape

    Box(
        modifier = modifier.drawBackdrop(
            backdrop = resolvedBackdrop,
            shape = { shape },
            effects = {
                // 顺序：color filter ⇒ blur ⇒ lens
                if (effective.blur > 0.dp) {
                    vibrancy()
                    blur(effective.blur.toPx())
                }
                if (cornerShape != null &&
                    effective.lensHeight > 0.dp &&
                    effective.lensAmount > 0.dp
                ) {
                    // lens 的 height 必须落在 [0, 最大圆角半径] 内，否则圆角处会出现不连续。
                    // Compose 未公开 minCornerRadius，故按库自身的方式从四角尺寸求得。
                    val density = this
                    val maxRadius = listOf(
                        cornerShape.topStart,
                        cornerShape.topEnd,
                        cornerShape.bottomEnd,
                        cornerShape.bottomStart,
                    ).maxOf { it.toPx(size, density) }
                        .coerceAtMost(size.minDimension / 2f)

                    val height = effective.lensHeight.toPx().coerceAtMost(maxRadius)

                    // ⚠️ 折射量必须夹到**元素短边的 1/4**，不能只夹到 minDimension。
                    //
                    // 这里踩过一个很隐蔽的坑：`refractionAmount` 原先是
                    // `.coerceAtMost(size.minDimension)` —— 对底部 Tab 栏来说，
                    // minDimension 就是栏高（48dp / 168px），于是折射量被放到**整个栏高**。
                    //
                    // 后果不是"折射太强"，而是**整条栏发灰蓝**：
                    // 开了 `chromaticAberration` 后，shader 会在 ±dispersedCoord 处
                    // 分别采样 R / G / B（见库内 RoundedRectRefractionWithDispersion shader）。
                    // 采样跨度正比于折射量，168px 的跨度已经跨出栏外 ——
                    // 于是「栏上方亮、栏下方暗」这个明暗梯度被当成色差读了出来，
                    // 整条栏被染成冷色。
                    //
                    // 亮色主题下看不出来，因为 surfaceAlphaLight = 0.62 的白遮罩盖住了；
                    // 深色主题 surfaceAlphaDark = 0.11 几乎不遮，问题才暴露。
                    //
                    // 实测（深色，底栏中心）：夹之前 rgb(82,91,94) R−B=−12（冷），
                    // 夹到 1/4 之后 rgb(72,72,64) R−B=+8（暖），与完全关掉 lens 的结果一致。
                    val amount = effective.lensAmount.toPx().coerceAtMost(size.minDimension * 0.25f)
                    if (height > 0f && amount > 0f) {
                        lens(
                            refractionHeight = height,
                            refractionAmount = amount,
                            depthEffect = true,
                            chromaticAberration = true,
                        )
                    }
                }
            },
            // 自己画描边与高光，不用库默认的 highlight，保证与设计令牌一致
            highlight = null,
            shadow = {
                // 内容卡片才投影，且要轻 —— 它的职责只是「把卡片从背景上托起来」，
                // 不是制造悬浮感。原值 radius 24 / offset 8 会让卡片读成塑料板。
                if (shadow) {
                    Shadow(
                        radius = 14.dp,
                        offset = DpOffset(0.dp, 5.dp),
                        color = colors.glassShadow,
                    )
                } else {
                    null
                }
            },
            onDrawSurface = {
                // 1. 可读性层 —— 表面不透明度是可读性的底线，不是审美选择
                drawRect(surfaceColor)

                // 2. 内描边由 Modifier.border 负责（见下方 modifier 链），此处只画顶边高光。
                // 3. 顶边高光 1dp —— 模拟受光面，跟随圆角内缩
                val inset = (cornerShape?.topStart?.toPx(size, this) ?: 0f)
                    .coerceAtMost(size.width / 2f)
                if (size.width > inset * 2f) {
                    drawLine(
                        color = colors.glassHighlight,
                        start = Offset(inset, 0.5f.dp.toPx()),
                        end = Offset(size.width - inset, 0.5f.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            },
        )
            // 内描边 1dp —— 定义卡片边界。缺了它，卡片与背景之间没有分界，
            // 把界面转成灰度后就分辨不出哪层在哪层之上了。
            .border(width = 1.dp, color = colors.glassStroke, shape = shape),
        content = content,
    )
}

/**
 * 玻璃卡片（L1 内容层）。
 *
 * 默认内边距 16dp，配合 `card` 圆角 28dp，满足同心公式：
 * `r_child = 28 − 16 = 12` ⇒ 内部控件应使用 `shapes.controlInCard`。
 *
 * ⚠️ 本卡片内部**禁止**再放 [MagicGlassSurface]。需要分段控件用
 *    [MagicSegmentedControl]，需要按钮用 [MagicSolidButton]。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.Regular,
    contentPadding: PaddingValues = PaddingValues(MagicSpacing.cardPadding),
    backdrop: Backdrop? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MagicGlassSurface(
        modifier = modifier,
        shape = LocalMagicShapes.current.card,
        level = level,
        backdrop = backdrop,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content,
        )
    }
}

/**
 * 实心按钮（**非玻璃**）。
 *
 * 用于卡片内部 —— 卡片已经是玻璃，内部再叠玻璃就违反 No Glass on Glass。
 *
 * 按压反馈用**轻微缩放**（iOS 语汇），不是 Material ripple。
 */
@Composable
fun MagicSolidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = MagicSpacing.cardPadding,
        vertical = MagicSpacing.row,
    ),
    content: @Composable RowScope.() -> Unit,
) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val background = when {
        // 禁用态必须有可见的底。
        // 原值用 colors.divider（12% 黑），画在浅色卡片上几乎看不出按钮存在，
        // 禁用态形同虚设 —— 用户会以为是渲染错误。
        !enabled -> colors.primaryText.copy(alpha = 0.06f)
        selected -> colors.accent.copy(alpha = 0.12f)
        else -> Color.Transparent
    }

    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = Spring.StiffnessMedium),
        label = "solidButtonPress",
    )

    Box(
        modifier = modifier
            .scale(pressScale)
            .clip(shapes.pill)
            .background(background)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                // iOS 不用 Material ripple
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * 分段控件（实心，**非玻璃**）。
 *
 * 之所以刻意做成实心：它是 L1 卡片**内部**的控件，
 * 若也用玻璃就会形成「卡片玻璃 → 控件玻璃 → Tab 栏玻璃」三层嵌套，
 * 层级无法辨认，性能也白白浪费。
 *
 * ── 滑块只有一个 ──
 * 和底栏指示器同一个道理：每个分段各自 `animateColorAsState` 自己的底色，
 * 高亮只会原地淡入淡出。这里改成单个滑块跨分段滑动。
 *
 * ── 同心圆角 ──
 * 外层 [shape]（12dp）减去内边距 [MagicSpacing.segmentedInset]（2dp）
 * ⇒ 滑块必须是 10dp，即 [thumbShape]。
 * 早期实现内外层都用 12dp，圆心不重合，接缝处出现可见的厚度跳变。
 *
 * @param shape 外层容器圆角，卡片内使用时传 `shapes.controlInCard`
 * @param thumbShape 滑块圆角，必须满足 `r_thumb = r_outer − segmentedInset`
 */
@Composable
fun MagicSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = LocalMagicShapes.current.controlInCard,
    thumbShape: CornerBasedShape = LocalMagicShapes.current.controlInCardThumb,
) {
    val colors = LocalMagicColors.current
    val density = LocalDensity.current
    val inset = MagicSpacing.segmentedInset
    val insetPx = with(density) { inset.toPx() }

    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    val itemWidthPx = if (options.isEmpty() || rowSize.width <= 0) {
        0f
    } else {
        (rowSize.width - insetPx * (options.size - 1)) / options.size
    }

    val thumbX = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(selectedIndex, itemWidthPx) {
        if (itemWidthPx <= 0f) return@LaunchedEffect
        val target = selectedIndex * (itemWidthPx + insetPx)
        if (!placed) {
            thumbX.snapTo(target)
            placed = true
        } else {
            thumbX.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.80f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (colors.isLight) Color.White.copy(alpha = 0.72f)
                else Color.Black.copy(alpha = 0.45f)
            )
            .padding(inset),
    ) {
        // ① 唯一的那一个滑块（先画，在 Row 之下）
        if (itemWidthPx > 0f && rowSize.height > 0) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbX.value.roundToInt(), 0) }
                    .width(with(density) { itemWidthPx.toDp() })
                    .height(with(density) { rowSize.height.toDp() })
                    .clip(thumbShape)
                    .background(colors.accent.copy(alpha = 0.12f)),
            )
        }

        // ② 分段只负责文字
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { rowSize = it },
            horizontalArrangement = Arrangement.spacedBy(inset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(thumbShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) },
                        )
                        .padding(vertical = MagicSpacing.row),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) colors.accent else colors.secondaryText,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
