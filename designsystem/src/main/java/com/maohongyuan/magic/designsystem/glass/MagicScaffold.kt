package com.maohongyuan.magic.designsystem.glass

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

object MagicScaffoldDefaults {
    /** 底栏外壳高度：内容 48dp + 上下内边距 8dp × 2。必须与 LiquidBottomTabs 保持一致。 */
    val TabBarHeight: Dp = 64.dp

    /** 底栏距屏幕左右 / 底部的悬浮外边距 */
    val TabBarMargin: Dp = MagicSpacing.row

    /**
     * 内容区底部必须留出的空间 = 底栏 + 悬浮外边距 + 16dp 余量。
     *
     * 注意：**不含** `navigationBars`。导航条高度是设备相关的（手势条 ~24dp、
     * 三键导航 ~48dp），必须由 [MagicPageContent] 在运行时叠加，
     * 不能用一个大数字「大概够用」地蒙过去 —— 三键导航下会直接遮挡内容。
     */
    val ContentBottomPadding: Dp = TabBarHeight + TabBarMargin + 16.dp

    /** 大标题折叠行程。滚动超过这个距离，inline 导航栏完全显现。 */
    val HeaderHeight: Dp = 96.dp

    /** 底栏上方的滚动边缘渐变高度 */
    val ScrollEdgeHeight: Dp = 152.dp
}

/**
 * 应用外壳。
 *
 * 负责建立流体玻璃的三层结构，并统一处理：
 * - 顶部安全区（`WindowInsets.statusBars`）
 * - 底部安全区（`WindowInsets.navigationBars`，在 [MagicPageContent] 里叠加）
 * - 键盘（`WindowInsets.ime`：视口收缩 + 自动隐藏底栏）
 * - 大标题 → inline title 的**连续**折叠（不是阈值开关）
 * - 底栏上方的滚动边缘渐隐
 * - 每个 Tab 各自记住滚动位置（iOS 行为，不是每次切回顶部）
 *
 * 页面内容请用 [MagicPageContent] 承载，它已内置一致的 insets 与内边距。
 */
@Composable
fun MagicScaffold(
    title: String,
    tabs: List<LiquidTab>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    glassMode: GlassMode = GlassMode.FULL,
    floatingOverlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable (ScrollState) -> Unit,
) {
    val colors = LocalMagicColors.current
    val density = LocalDensity.current

    // 每个 Tab 一个 ScrollState —— 切回来时停在上次的位置。
    // 早期实现是 LaunchedEffect(selectedTab) { scrollTo(0) }：既是瞬移无动画，
    // 又和 iOS 行为相反（iOS 的 Tab 会记住各自的滚动位置）。
    val scrollStates = remember(tabs.size) { List(tabs.size) { ScrollState(0) } }
    val safeIndex = selectedTab.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
    val scrollState = scrollStates.getOrElse(safeIndex) { ScrollState(0) }

    // 键盘弹出时底栏让位：否则底栏会悬在键盘上面，或与键盘重叠
    val imeBottom = WindowInsets.ime.getBottom(density)
    val imeVisible = imeBottom > 0

    MagicBackdrop(
        modifier = modifier,
        glassMode = glassMode,
        content = { content(scrollState) },
        overlay = {
            MagicNavBar(
                title = title,
                scrollOffset = { scrollState.value },
                onOpenSettings = onOpenSettings,
                modifier = Modifier.align(Alignment.TopCenter),
            )

            // ── 滚动边缘效果 ──
            // 内容靠近底栏时渐进淡出，而不是以全对比度从底栏下方穿出来。
            // 底栏随后绘制，因此这层只会影响底栏周围与下方的内容。
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(MagicScaffoldDefaults.ScrollEdgeHeight)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.backdropBase.copy(alpha = 0.94f),
                            )
                        )
                    )
            )

            if (!imeVisible) {
                LiquidBottomTabs(
                    tabs = tabs,
                    selectedIndex = selectedTab,
                    onSelect = onSelectTab,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(
                            horizontal = MagicSpacing.screenMargin,
                            vertical = MagicScaffoldDefaults.TabBarMargin,
                        ),
                )
            }

            floatingOverlay()
        },
    )
}

/**
 * 折叠式顶部导航栏。
 *
 * iOS 的 inline 导航栏是**整宽**的：材质从屏幕顶端一直铺到标题下方，
 * 底部收一条发丝线。早期实现做成左右各内缩 16dp 的悬浮圆角条（`largeCard`），
 * 那是「浮动工具条」，不是导航栏。
 *
 * 折叠是**连续**的：由滚动偏移直接驱动 alpha 与位移，
 * 而不是 `scroll > 96px` 这样的布尔开关（那会在阈值处生硬地闪一下）。
 * 状态读取放在 `graphicsLayer` 的 lambda 里，因此滚动时不会触发重组，只重绘。
 */
@Composable
fun MagicNavBar(
    title: String,
    scrollOffset: () -> Int,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { MagicScaffoldDefaults.HeaderHeight.toPx() }

    MagicGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val fraction = (scrollOffset() / thresholdPx).coerceIn(0f, 1f)
                alpha = fraction
                translationY = (1f - fraction) * -size.height
            },
        shape = RectangleShape,
        level = GlassLevel.Thick,
        // 覆盖层 → 使用内容折射源，真实折射身后的页面内容
        backdrop = LocalContentBackdrop.current,
        // 覆盖层不投影：iOS 靠材质 + 发丝线分层，投影会让它读成「浮起来的塑料条」
        shadow = false,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // 玻璃铺到屏幕顶端，内容避开状态栏
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(
                        horizontal = MagicSpacing.cardPadding,
                        vertical = MagicSpacing.row,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    fontSize = MagicType.headline.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "设置",
                    fontSize = MagicType.footnote.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.accent,
                    modifier = Modifier.clickable(onClick = onOpenSettings),
                )
            }
            // 底部发丝线 —— inline 导航栏的收口
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.divider)
            )
        }
    }
}

/**
 * 标准页面内容容器。
 *
 * 内置：
 * - `statusBars` 安全区（必须放在 `verticalScroll` **外侧**，
 *   否则内边距会跟着内容滚走，内容就顶到状态栏下面去了）
 * - `navigationBars` 安全区 —— 在**运行时**叠加，不写死
 * - 键盘（`ime`）：视口收缩，且键盘弹起时不再为底栏预留空间
 * - 屏幕边距 16dp
 * - 底部 [MagicScaffoldDefaults.ContentBottomPadding]
 */
@Composable
fun MagicPageContent(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = MagicSpacing.screenMargin,
    topPadding: Dp = MagicSpacing.section,
    bottomPadding: Dp = MagicScaffoldDefaults.ContentBottomPadding,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(MagicSpacing.cardGap),
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    // 键盘弹起时底栏已隐藏，不再需要为它留出 88dp
    val effectiveBottom = if (imeVisible) {
        MagicSpacing.screenMargin
    } else {
        bottomPadding + navBarBottom
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            // 放在 verticalScroll 外侧：键盘弹起时收缩的是视口，而不是把内容顶上去
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = horizontalPadding)
            .padding(top = topPadding, bottom = effectiveBottom),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
