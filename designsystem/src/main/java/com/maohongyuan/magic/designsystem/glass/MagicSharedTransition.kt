package com.maohongyuan.magic.designsystem.glass

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maohongyuan.magic.designsystem.theme.MagicSmoothCornerShape

/**
 * 共享元素（Container Transform）转场的公共设施。
 *
 * ── 设计目标 ──
 * 让「点卡片 → 卡片连续变形成整页 → 返回时原路缩回」成为**一套**运动，
 * 而不是「缩放 + 位移 + 淡入淡出」三个独立动画拼出来的近似效果。
 *
 * ── 为什么容器与内容分开处理 ──
 * - 容器（卡片外壳 / 页面根）用 [SharedTransitionScope.sharedBounds]：
 *   它负责「矩形边界」的连续变化（位置 + 宽高同时插值，不是以中心等比缩放）。
 * - 内容用 [SharedTransitionScope.sharedElement]：只有**真实存在对应关系**的
 *   视觉元素（这里是页面标题 ↔ 卡片标题）才配共享标识。
 * - 卡片的图标 / 副标题在目标页没有对应物，因此**不伪造**共享关系，
 *   跟随所在容器一起淡入淡出。
 *
 * ── 与流体玻璃的协同 ──
 * 共享元素在转场期间被搬到 [SharedTransitionScope] 的 overlay 里绘制，
 * 但 overlay 仍然位于 [MagicBackdrop] 的 `layerBackdrop` 子树内，
 * 所以玻璃表面继续通过 [LocalBackdrop]（屏幕锚定环境折射源）采样背景 ——
 * 折射会跟着容器一起移动，而不是「卡片玻璃消失、页面再画一块新的」。
 *
 * ⚠️ 这里**不**新增任何 `layerBackdrop`。转场只叠加 `drawBackdrop`（读），
 *    绝不写 —— 写会与录制形成渲染死循环，最终 SIGSEGV。
 */

/** 共享元素 key 工厂。带业务 ID，避免列表里多个条目互相错配。 */
object MagicTransitionKeys {

    /** 容器（卡片外壳 ↔ 页面根）的共享边界 key */
    fun container(id: String): String = "magic:container:$id"

    /** 标题（卡片标题 ↔ 页面大标题）的共享元素 key */
    fun title(id: String): String = "magic:title:$id"
}

/**
 * 转场时间轴与几何参数。
 *
 * 所有数值集中在这里，是为了让「边界运动」与「圆角变化」共用**同一套**时钟，
 * 而不是各自建一条容易跑偏的动画。
 */
object MagicTransitionSpec {

    /**
     * 边界弹簧。
     *
     * 用弹簧而不是 tween：iOS 的容器变形是「先快后稳」的物理收尾，
     * 线性缓动会在终点前显得发飘。
     * `dampingRatio = 0.86` 不会过冲 —— 容器边界过冲会在圆角处露出硬边。
     */
    val Bounds: BoundsTransform = BoundsTransform { _, _ -> boundsSpring() }

    /**
     * 进度弹簧 —— 与 [Bounds] **同参数**。
     *
     * 圆角、玻璃面板透明度都从这条进度派生，因此形状变化与边界运动天然同步，
     * 不存在「容器到位了圆角还在动」的错位。
     */
    val Progress: FiniteAnimationSpec<Float> = progressSpring()

    /**
     * 转场总时长。
     *
     * 必须与 [Bounds] 的弹簧**大致同步**：`StiffnessMediumLow(400) + 0.86 阻尼`
     * 的收敛时间约 230ms，这里取 320ms 留一点收尾余量。
     * 早期写成 420ms 时，容器在 230ms 就已经铺满，内容却还在慢慢淡入 ——
     * 观感变成「先跳成一整页，再慢慢亮起来」，而不是「卡片长成页面」。
     */
    const val DurationMillis: Int = 320

    /**
     * 页面内容在容器内的显现。
     *
     * 前 70ms 保持全透明 —— 这段时间容器还在卡片尺寸附近，
     * 立刻铺满内容会让「卡片被页面盖住」，而不是「卡片变成页面」。
     * 之后 170ms 内淡入完毕，与边界收敛基本同时结束。
     */
    val PageContentEnter: EnterTransition = fadeIn(
        animationSpec = keyframes {
            durationMillis = DurationMillis
            0f at 0
            0f at 70
            1f at 240
        }
    )

    /** 页面内容离开：快速让位给收缩的容器 */
    val PageContentExit: ExitTransition = fadeOut(
        animationSpec = keyframes {
            durationMillis = DurationMillis
            1f at 0
            0f at 80
        }
    )

    /** 卡片内容在容器内的显现（返回时用）：比页面稍晚，让容器先到位 */
    val CardContentEnter: EnterTransition = fadeIn(
        animationSpec = keyframes {
            durationMillis = DurationMillis
            0f at 0
            0f at 70
            1f at 210
        }
    )

    /** 卡片内容离开：必须快，否则会和玻璃面板的淡入叠成重影 */
    val CardContentExit: ExitTransition = fadeOut(
        animationSpec = keyframes {
            durationMillis = DurationMillis
            1f at 0
            0f at 80
        }
    )

    /**
     * 页面根（AnimatedContent 层）的进出。
     *
     * 注意这两个的**时长**决定了旧页面在组合树里活多久 ——
     * 若短于边界弹簧，来源卡片会在动画途中被销毁，转场直接断掉。
     * 所以用 keyframes 把 alpha 早早拉满/拉没，但把 `durationMillis`
     * 拉到 [DurationMillis]，让内容一直活到弹簧收尾。
     */
    val PageEnter: EnterTransition = fadeIn(
        animationSpec = keyframes {
            durationMillis = DurationMillis
            0f at 0
            1f at 200
        }
    )

    val PageExit: ExitTransition = fadeOut(
        animationSpec = keyframes {
            durationMillis = DurationMillis
            1f at 0
            0f at 90
        }
    )

    /** 标题交叉淡入 —— 两端字号不同，靠交叉把「换字号」藏掉 */
    val TitleEnter: EnterTransition = fadeIn(
        animationSpec = keyframes {
            durationMillis = DurationMillis
            0f at 0
            1f at 130
        }
    )

    val TitleExit: ExitTransition = fadeOut(
        animationSpec = keyframes {
            durationMillis = DurationMillis
            1f at 0
            0f at 130
        }
    )

    /**
     * 来源卡片宽度占页面宽度的比例。
     *
     * 板块页是 2 列网格：`(屏宽 − 2×16dp 边距 − 12dp 列间距) / 2`。
     * 360dp 屏 → 0.439；412dp 屏 → 0.447；800dp 平板 → 0.472。
     * 取 0.45 覆盖手机与平板，误差 < 2.5%。
     *
     * 它只影响**转场途中**圆角的换算精度（见 [morphRadius]），
     * 不影响首尾两帧 —— 首尾由卡片自身圆角与页面圆角精确决定。
     */
    const val SourceWidthRatio: Float = 0.45f

    /** 卡片圆角（与 `MagicShapes.card` 的 28dp 一致） */
    val CardCorner: Dp = 28.dp

    /**
     * 容器在「子坐标系」里应当使用的圆角。
     *
     * 共享边界用的是 `scaleToBounds`：子内容按**最终尺寸**布局，再整体缩放。
     * 于是子坐标系里的圆角 r 在屏幕上看起来是 `r × s`（s = 当前缩放比）。
     * 要在屏幕上呈现 `28dp × (1 − p)` 的连续收角，子坐标系里就必须写
     * `r = 28dp × (1 − p) / s`，其中 `s` 从 [SourceWidthRatio] 线性走到 1。
     *
     * 不做这个换算的后果：容器放大过程中，玻璃面板的圆角比裁剪区域更圆，
     * 四个角会露出底图的月牙形缺口。
     */
    fun morphRadius(progress: Float): Dp {
        val p = progress.coerceIn(0f, 1f)
        val scale = SourceWidthRatio + (1f - SourceWidthRatio) * p
        return CardCorner * ((1f - p) / scale)
    }

    /**
     * 变形中玻璃面板的不透明度。
     *
     * 0 → 0.12 快速淡入：与卡片内容淡出交叉，遮住中间那一瞬的空档；
     * 0.55 之后淡出：把表面让给页面自身的内容，终点处完全消失，
     * 这样页面静止时不会多出一层本不该存在的全屏玻璃。
     */
    fun panelAlpha(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return when {
            p < 0.12f -> p / 0.12f
            p < 0.55f -> 1f
            else -> 1f - ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
        }
    }

    private fun boundsSpring(): FiniteAnimationSpec<Rect> =
        spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)

    private fun progressSpring(): FiniteAnimationSpec<Float> =
        spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
}

/**
 * 转场作用域 —— 把「共享边界作用域 + 当前页面的可见性进度」打包下发。
 *
 * 页面与卡片只认这个对象，不直接依赖 `AnimatedContent` 或导航实现，
 * 因此换导航方案时转场代码不用跟着改。
 */
class MagicTransitionScope(
    val shared: SharedTransitionScope,
    val visibility: AnimatedVisibilityScope,
    private val progressState: State<Float>,
) {
    /** 0 = 仍在来源卡片上；1 = 已完全变成目标页面 */
    val progress: Float get() = progressState.value
}

/**
 * 当前是否处在可用的共享元素转场作用域内。
 *
 * 为 `null` 时（例如设置页、许可页、预览）所有共享修饰符退化为无操作，
 * 页面照常显示 —— 这是「没有匹配共享元素时平稳降级」的落点。
 */
val LocalMagicTransitionScope = staticCompositionLocalOf<MagicTransitionScope?> { null }

/**
 * 页面标题的共享 key。
 *
 * 由路由层下发，页面本身不必知道自己在跟哪张卡片配对 ——
 * 于是 12 个 feature 页面都不用改签名。
 */
val LocalPageTitleSharedKey = staticCompositionLocalOf<String?> { null }

/**
 * 来源卡片一侧的共享边界。
 *
 * @param id 稳定的业务 ID（这里用系统枚举名）
 */
@Composable
fun MagicTransitionScope.cardBounds(
    id: String,
    modifier: Modifier = Modifier,
): Modifier = containerBounds(
    key = MagicTransitionKeys.container(id),
    modifier = modifier,
    enter = MagicTransitionSpec.CardContentEnter,
    exit = MagicTransitionSpec.CardContentExit,
)

/** 目标页面一侧的共享边界。key 必须与 [cardBounds] 完全一致才会配对。 */
@Composable
fun MagicTransitionScope.pageBounds(
    id: String,
    modifier: Modifier = Modifier,
): Modifier = containerBounds(
    key = MagicTransitionKeys.container(id),
    modifier = modifier,
    enter = MagicTransitionSpec.PageContentEnter,
    exit = MagicTransitionSpec.PageContentExit,
)

@Composable
private fun MagicTransitionScope.containerBounds(
    key: String,
    modifier: Modifier,
    enter: EnterTransition,
    exit: ExitTransition,
): Modifier = with(shared) {
    modifier.sharedBounds(
        sharedContentState = rememberSharedContentState(key),
        animatedVisibilityScope = visibility,
        enter = enter,
        exit = exit,
        boundsTransform = MagicTransitionSpec.Bounds,
        // FillWidth + TopStart：等比缩放（不拉伸文字），左上角对齐。
        // 容器从卡片长到整页时，页面顶部始终贴着容器顶边 —— 这正是 iOS 的观感。
        // 用 FillBounds 会把文字横向拉扁；用 RemeasureToBounds 会让整页在
        // 每一帧按新宽度重新排版，既跳字又昂贵。
        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopStart,
        ),
    )
}

/**
 * 标题的共享边界。
 *
 * ⚠️ 这里用 `sharedBounds` 而**不是** `sharedElement`。
 *
 * `sharedElement` 的语义是「两端内容在相同约束下布局完全一致」，它按动画尺寸
 * 重新布局子内容、不做缩放。卡片标题是 17sp、页面大标题是 34sp —— 用
 * `sharedElement` 会把 34sp 的字塞进 17sp 的框里，实测直接被裁成「八」。
 *
 * `sharedBounds` + `scaleToBounds(FillWidth)` 才是对的做法：两端内容各自按
 * 目标尺寸布局，再等比缩放到动画边界，于是 17sp 与 34sp 在交叉淡入时视觉尺寸一致，
 * 读起来就是「同一个标题在长大」。
 *
 * `zIndexInOverlay = 1f`：容器（玻璃面板）默认在 0 层，标题必须在它之上，
 * 否则会出现「文字飞到一半被面板盖住」。
 */
@Composable
fun MagicTransitionScope.titleBounds(
    id: String,
    modifier: Modifier = Modifier,
): Modifier = with(shared) {
    modifier.sharedBounds(
        sharedContentState = rememberSharedContentState(MagicTransitionKeys.title(id)),
        animatedVisibilityScope = visibility,
        enter = MagicTransitionSpec.TitleEnter,
        exit = MagicTransitionSpec.TitleExit,
        boundsTransform = MagicTransitionSpec.Bounds,
        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.Center,
        ),
        zIndexInOverlay = 1f,
    )
}

/**
 * 变形中的玻璃容器。
 *
 * 结构（自下而上）：
 * 1. 玻璃面板 —— 卡片材质的延续。随进度收角、并在末段淡出。
 * 2. 页面内容 —— 由调用方传入，随共享边界一起缩放/裁剪。
 *
 * 两层共用**同一个**圆角形状对象，所以玻璃边缘、裁剪区、内容边界永远一致，
 * 不会出现「玻璃是圆的、内容被切成方的」。
 */
@Composable
fun MagicContainerTransformSurface(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val p = progress().coerceIn(0f, 1f)
    val shape = MagicSmoothCornerShape(MagicTransitionSpec.morphRadius(p))
    val panelAlpha = MagicTransitionSpec.panelAlpha(p)

    // 进度还在 0 时**不画页面内容**。
    //
    // 原因：目标页第一次组合到共享边界真正激活之间会隔一小段时间
    // （页面首帧要建十几块玻璃，实测 100~200ms）。这段时间里共享边界尚未生效，
    // 页面会以**整页尺寸**画出来 —— 观感就是「卡片还没变形，页面已经铺满屏幕」。
    // 此刻来源卡片仍在原位完整可见，所以把内容藏起来不会露出空白。
    val contentAlpha = if (p <= 0.001f) 0f else 1f

    Box(modifier = modifier.fillMaxSize().clip(shape)) {
        if (panelAlpha > 0.01f) {
            MagicGlassSurface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = panelAlpha },
                shape = shape,
                level = GlassLevel.Regular,
                // 转场中的容器不投影：投影会在缩放时被放大成一片灰雾
                shadow = false,
            ) {}
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentAlpha },
            content = content,
        )
    }
}

/**
 * 把「当前页面可见性」接成 0→1 的进度。
 *
 * 必须放在 `AnimatedContent` 的内容 lambda 里调用 —— 它读的是该条目自己的
 * [androidx.compose.animation.core.Transition]，进/出方向自动正确：
 * 进入的页面 0→1，离开的页面 1→0。
 */
@Composable
fun AnimatedVisibilityScope.magicTransitionProgress(): State<Float> =
    transition.animateFloat(
        transitionSpec = { MagicTransitionSpec.Progress },
        label = "magic-container-progress",
    ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
