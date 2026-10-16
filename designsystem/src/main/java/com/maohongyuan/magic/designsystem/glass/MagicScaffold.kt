package com.maohongyuan.magic.designsystem.glass

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
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

    /** 顶部渐隐遮罩高度。内容滚过时淡出，而不是硬切在状态栏下。 */
    val TopScrimHeight: Dp = 88.dp

    /** 右上角设置按钮的直径。44dp 是单手拇指可点的最小舒适尺寸 */
    val SettingsButtonSize: Dp = 44.dp

    /** 底栏上方的滚动边缘渐变高度 */
    val ScrollEdgeHeight: Dp = 152.dp
}

/**
 * 「打开设置」的注入点。
 *
 * 设置入口是**右上角的玻璃圆钮**（见下方 overlay），全局固定、始终可见。
 * 页面末尾由 [MagicPageContent] 自动追加一个入口，底栏图标长按也能打开 ——
 * 两条路径，保证可发现性。
 */
val LocalOpenSettings = staticCompositionLocalOf<() -> Unit> {
    error("MagicScaffold 未提供 onOpenSettings")
}

/**
 * 应用外壳。
 *
 * 负责建立流体玻璃的三层结构，并统一处理：
 * - 顶部安全区（`WindowInsets.statusBars`）
 * - 底部安全区（`WindowInsets.navigationBars`，在 [MagicPageContent] 里叠加）
 * - 键盘（`WindowInsets.ime`：视口收缩 + 自动隐藏底栏）
 * - 顶部 / 底部的滚动边缘渐隐
 * - 每个 Tab 各自记住滚动位置（iOS 行为，不是每次切回顶部）
 *
 * ⚠️ **本外壳没有顶部导航栏。** 这是刻意的：
 * 顶部只保留一层无文字、无按钮的纯渐变遮罩。
 * 页面身份由页面内的 [MagicPageHeader] 大标题承担；
 * 另有底栏长按作为快捷方式（发现性差，只作补充）。
 *
 * 页面内容请用 [MagicPageContent] 承载，它已内置一致的 insets 与内边距。
 */
@Composable
fun MagicScaffold(
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
    val scrollStates = remember(tabs.size) { List(tabs.size) { ScrollState(0) } }
    val safeIndex = selectedTab.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
    val scrollState = scrollStates.getOrElse(safeIndex) { ScrollState(0) }

    // 键盘弹出时底栏让位：否则底栏会悬在键盘上面，或与键盘重叠
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    // 包成 State 供底栏读取 —— 让传下去的 lambda 实例保持稳定，见下方注释
    val selectedTabState = rememberUpdatedState(selectedTab)

    CompositionLocalProvider(LocalOpenSettings provides onOpenSettings) {
        MagicBackdrop(
            modifier = modifier,
            glassMode = glassMode,
            content = { content(scrollState) },
            overlay = {
                // ── 顶部滚动边缘效果 ──
                // iOS 26 的滚动边缘效果本质是**渐进模糊**（贴边最糊、向内递减），
                // 而不是单纯压一层渐变 —— 后者只是「变暗」，不是「失焦」。
                // 这里用 MagicProgressiveBlur，再叠一层很淡的 scrim 保证状态栏图标可读。
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(MagicScaffoldDefaults.TopScrimHeight)
                ) {
                    MagicProgressiveBlur(
                        height = MagicScaffoldDefaults.TopScrimHeight,
                        maxBlur = 14.dp,
                        edge = BlurEdge.TOP,
                        backdrop = LocalContentBackdrop.current ?: LocalBackdrop.current,
                        scrimColor = colors.backdropBase,
                        scrimMaxAlpha = 0.58f,
                    )
                }

                // ── 设置入口：右上角 ──
                //
                // 位置分析（原先的两个入口都不合格）：
                //   · 页面末尾：长页面要滚到底才看得到，位置还随内容长度浮动，无法形成肌肉记忆
                //   · 长按 Tab：**零可发现性**，没有任何视觉提示，用户根本不会试
                //
                // 右上角是 iOS 放 trailing 导航按钮的惯例位置：固定不动、始终可见、单手可达。
                // 用一个玻璃圆钮而不是重开一条导航栏 —— 保持「顶部无栏」的既定设计。
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(end = MagicSpacing.screenMargin, top = MagicSpacing.xs),
                ) {
                    MagicGlassSurface(
                        modifier = Modifier
                            .size(MagicScaffoldDefaults.SettingsButtonSize)
                            .clip(LocalMagicShapes.current.pill)
                            .clickable(onClick = onOpenSettings),
                        shape = LocalMagicShapes.current.pill,
                        level = GlassLevel.Thin,
                        shadow = false,
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            MagicTuneIcon(
                                tint = colors.primaryText,
                                modifier = Modifier.size(
                                    MagicScaffoldDefaults.SettingsButtonSize * 0.44f
                                ),
                            )
                        }
                    }
                }

                // ── 滚动边缘效果（底部）──
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
                        // ⚠️ 必须传**实例稳定**的 lambda。
                        //
                        // 直接写 `{ selectedTab }` 会捕获 Int **值**：值一变，Compose 就得
                        // 重建 lambda，dock 内部的 LaunchedEffect(selectedTabIndex) 随之重启。
                        // 更早的版本还因此触发过 `remember(key)` 重置、把气泡的驱动链断掉
                        // （表现为「点 Tab 页面会切、气泡不动」）。
                        //
                        // 用 rememberUpdatedState 把值包成 State，lambda 捕获的是**稳定的 State 对象**，
                        // 实例在组件生命周期内恒定。
                        selectedTabIndex = { selectedTabState.value },
                        onTabSelected = onSelectTab,
                        // 覆盖层 → 内容折射源：气泡能真实折射页面内容
                        backdrop = LocalContentBackdrop.current ?: LocalBackdrop.current,
                        tabsCount = tabs.size,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(
                                horizontal = MagicSpacing.screenMargin,
                                vertical = MagicScaffoldDefaults.TabBarMargin,
                            ),
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            LiquidBottomTab(
                                onClick = { onSelectTab(index) },
                                // 长按任一 Tab 打开设置（快捷方式；主入口是右上角按钮）
                                onLongClick = onOpenSettings,
                            ) {
                                // ⚠️ 必须给中性色。选中态的蓝色由着色层折射产生，
                                // 这里给 accent 会叠成两种不同的蓝。
                                MagicTabIconView(
                                    icon = tab.icon,
                                    tint = LocalMagicColors.current.primaryText,
                                    selected = index == selectedTab,
                                    modifier = Modifier.size(22.dp),
                                )
                                Text(
                                    text = tab.label,
                                    fontSize = MagicType.footnote.sp,
                                    color = LocalMagicColors.current.primaryText,
                                )
                            }
                        }
                    }
                }

                floatingOverlay()
            },
        )
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
    topPadding: Dp = MagicSpacing.sectionLarge,
    bottomPadding: Dp = MagicScaffoldDefaults.ContentBottomPadding,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(MagicSpacing.cardGap),
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    // 键盘弹起时底栏已隐藏，不再需要为它留出空间
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
    ) {
        content()
    }
}

