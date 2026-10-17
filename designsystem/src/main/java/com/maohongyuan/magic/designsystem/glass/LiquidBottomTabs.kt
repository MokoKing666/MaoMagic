package com.maohongyuan.magic.designsystem.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.maohongyuan.magic.designsystem.glass.internal.DampedDragAnimation
import com.maohongyuan.magic.designsystem.glass.internal.InteractiveHighlight
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

@Immutable
data class LiquidTab(
    /** Tab 简称，与页面大标题（全称）区分：Tab 用「梅花」，页面标题用「梅花易数」 */
    val label: String,
    val icon: MagicTabIcon,
)

/**
 * 流体玻璃底部导航栏 —— **官方实现移植版**。
 *
 * 照搬自 AndroidLiquidGlass（Apache-2.0，作者 Kyant0）：
 * https://github.com/Kyant0/AndroidLiquidGlass （默认分支 `kmp`，不是 main）
 *
 * ── 关键认知：「滑动」不是「点了 Tab 后指示器滑过去」──
 * 而是**用手指直接拖那个气泡**，松手后按速度吸附到最近的 Tab。
 * 拖动过程中还有**速度驱动的横向拉伸** —— 这才是「流体」感的真正来源：
 * 不是位移本身，是「速度越大、拉伸越明显」。
 *
 * ── 三层结构（不要改）──
 * ```
 * 层 1  64dp 玻璃壳（可见）                     —— 中性色图标
 * 层 2  56dp tint(accent) 着色层（alpha 0，不可见但被 layerBackdrop 录制）
 * 层 3  56dp 气泡（可拖拽）                     —— 折射「页面内容 + 层 2」
 * ```
 * 「选中图标变蓝」的实现方式是：把整条 Tab 行染成 accent 色放到层 2，
 * 用 `alpha(0f)` 藏起来，再由层 3 的气泡折射出来。
 * **不是**给图标单独上色 —— 所以调用方给图标/文字必须是中性色。
 *
 * ── 本移植相对官方只改了 4 处 ──
 * 1. 主题判断：`isSystemInDarkTheme()` → `LocalMagicColors.current.isLight`
 * 2. 强调色：写死的 `Color(0xFF0088FF)` → `LocalMagicColors.current.accent`
 * 3. 容器色：写死的 `Color(0xFFFAFAFA / 0xFF121212)` → `GlassLevel.Thick` 的表面不透明度配白
 * 4. `backdrop` 由调用方传入（本项目传内容折射源）
 *
 * 其余（5 组 spring 参数、`(maxWidth - 8dp) / tabsCount`、两层 Row 结构、
 * velocity 拉伸公式、pressProgress 驱动的 5 个效果）**一律原样保留**。
 *
 * ⚠️ 本组件内部使用了 `rememberLayerBackdrop`（层 2 的 `layerBackdrop`），
 *    因此**绝不能被放在另一棵 `layerBackdrop` 子树内**，否则渲染死循环 → SIGSEGV。
 *    本项目由 `MagicScaffold` 放在 `MagicBackdrop` 的 `overlay` 插槽（与内容层平级）。
 */
@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val magicColors = LocalMagicColors.current
    val isLightTheme = magicColors.isLight
    val accentColor = magicColors.accent

    // 容器色照抄 Kyant0 LiquidBottomTabs：
    //   亮色 Color(0xFFFAFAFA).copy(0.4f)   近白
    //   深色 Color(0xFF121212).copy(0.4f)   近黑
    //
    // ⚠️ 原来这里写的是 Color.White.copy(alpha) —— **两种主题都是白**。
    // 深色下底栏被提亮成灰雾，和 MagicGlassSurface 是同一个错误，当时漏改了。
    // 深色玻璃该往更深走，不是往更白走。
    val containerColor = (if (isLightTheme) Color(0xFFFAFAFA) else Color(0xFF121212))
        .copy(
            alpha = if (isLightTheme) {
                GlassLevel.Thick.surfaceAlphaLight
            } else {
                GlassLevel.Thick.surfaceAlphaDark
            }
        )

    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        // 这个 -8dp 对应外壳的 padding(4dp)，官方标定值，勿改
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()

        // ⚠️ 这里**绝对不能**写成 `remember(selectedTabIndex) { ... }`。
        //
        // 官方原版用的是 `remember(selectedTabIndex)`，但它的调用方传的是**稳定 lambda**
        // （捕获 State 对象，实例不随值变化）。本项目调用方传的是 `{ selectedTab }` ——
        // 捕获的是 **Int 值**，值一变 Compose 就得造新 lambda，于是 key 变化、
        // `remember` 重新初始化，**生成一个新的 MutableIntState**。
        //
        // 后果：下面 effectB 里的 `snapshotFlow { currentIndex }` 绑定的是**旧 state 对象**，
        // 它此后永不变化 → 气泡再也不动。而页面走的是另一条路径（外部 tab 状态直接变），
        // 所以表现为「点 Tab 页面会切，但气泡不动」。
        //
        // 去掉 key 后 state 对象在组件生命周期内恒定，snapshotFlow 才能持续观测到变化。
        // 外部选中变化由 effectA 负责同步，本来也不需要这个 key。
        var currentIndex by remember {
            mutableIntStateOf(selectedTabIndex())
        }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                },
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    currentIndex = index
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f,
                    )
                },
            )
        }

        // ── 层 1：64dp 玻璃壳（可见，中性色内容）──
        Row(
            Modifier
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) },
                )
                .then(interactiveHighlight.modifier)
                .height(64f.dp)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )

        // ── 层 2：56dp accent 着色层（alpha 0，不可见；被 tabsBackdrop 录制供气泡折射）──
        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
            }
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(24f.dp.toPx() * progress, 24f.dp.toPx() * progress)
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                    .then(interactiveHighlight.modifier)
                    .height(56f.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }

        // ── 层 3：可拖拽的气泡 ──
        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    // 同时折射「页面内容」与「层 2 的着色层」
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true,
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(alpha = progress)
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(radius = 8f.dp * progress, alpha = progress)
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        // 速度驱动的横向拉伸 —— 流体感的核心
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            if (isLightTheme) Color.Black.copy(0.1f)
                            else Color.White.copy(0.1f),
                            alpha = 1f - progress,
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    },
                )
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount),
        )
    }
}
