package com.maohongyuan.magic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.maohongyuan.magic.board.BoardScreen
import com.maohongyuan.magic.core.interpret.InferencePrefs
import com.maohongyuan.magic.core.data.DivinationRecordStore
import com.maohongyuan.magic.core.data.GlassQuality
import com.maohongyuan.magic.core.data.MagicSettings
import com.maohongyuan.magic.core.data.SecretStore
import com.maohongyuan.magic.core.data.SettingsStore
import com.maohongyuan.magic.designsystem.glass.GlassMode
import com.maohongyuan.magic.designsystem.glass.GlassTuning
import com.maohongyuan.magic.designsystem.glass.LiquidTab
import com.maohongyuan.magic.designsystem.glass.LocalBackButtonVisible
import com.maohongyuan.magic.designsystem.glass.LocalGlassTuning
import com.maohongyuan.magic.designsystem.glass.LocalMagicTransitionScope
import com.maohongyuan.magic.designsystem.glass.LocalPageTitleSharedKey
import com.maohongyuan.magic.designsystem.glass.MagicContainerTransformSurface
import com.maohongyuan.magic.designsystem.glass.MagicScaffold
import com.maohongyuan.magic.designsystem.glass.MagicTransitionScope
import com.maohongyuan.magic.designsystem.glass.MagicTransitionSpec
import com.maohongyuan.magic.designsystem.glass.magicTransitionProgress
import com.maohongyuan.magic.designsystem.glass.pageBounds
import com.maohongyuan.magic.designsystem.theme.MagicTheme
import com.maohongyuan.magic.feature.almanac.AlmanacScreen
import com.maohongyuan.magic.feature.bazi.BaziScreen
import com.maohongyuan.magic.feature.liuyao.LiuyaoScreen
import com.maohongyuan.magic.feature.meihua.MeihuaScreen
import com.maohongyuan.magic.core.fortune.FortunePeriod
import com.maohongyuan.magic.feature.fortune.FortuneScreen
import com.maohongyuan.magic.feature.personality.PersonalityScreen
import com.maohongyuan.magic.feature.daliuren.DaLiuRenScreen
import com.maohongyuan.magic.feature.qimen.QiMenScreen
import com.maohongyuan.magic.feature.ziwei.ZiWeiScreen
import com.maohongyuan.magic.feature.tarot.TarotScreen
import com.maohongyuan.magic.feature.xiaoliuren.XiaoLiuRenScreen
import com.maohongyuan.magic.navigation.AppTab
import com.maohongyuan.magic.navigation.SystemId
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── 资源基建探针 ──
        // 验证纯 JVM 模块（java-library）的 src/main/resources 经 AGP 打进 APK 后，
        // 运行期还能不能用 getResourceAsStream 读到。
        // 本仓库此前从未用过资源文件，这条属于「业界通行说法」而非既成事实，
        // 因此用一次启动探针把它变成可验证的事实。读不到就说明塔罗/人格的资源方案要换。
        runCatching {
            val ok = com.maohongyuan.magic.core.resource.JsonResource.exists("/probe.json")
            android.util.Log.i("MagicResourceProbe", "probe.json readable = $ok")
        }.onFailure {
            android.util.Log.e("MagicResourceProbe", "探针异常", it)
        }

        enableEdgeToEdge()
        setContent {
            // 主题必须在 MagicTheme 之外读取 —— 它决定 MagicTheme 的入参。
            // SettingsStore 内部用 preferencesDataStore 委托，同进程重复创建
            // 会共享同一个 DataStore 实例，所以这里再 new 一次是安全的。
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val themeStore = androidx.compose.runtime.remember { SettingsStore(ctx) }
            var themeMode by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(com.maohongyuan.magic.core.data.ThemeMode.SYSTEM)
            }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                themeStore.settings.collect { themeMode = it.themeMode }
            }
            val dark = when (themeMode) {
                com.maohongyuan.magic.core.data.ThemeMode.SYSTEM ->
                    androidx.compose.foundation.isSystemInDarkTheme()
                com.maohongyuan.magic.core.data.ThemeMode.LIGHT -> false
                com.maohongyuan.magic.core.data.ThemeMode.DARK -> true
            }
            MagicTheme(darkTheme = dark) {
                MagicApp()
            }
        }
    }
}

/**
 * 应用根组合。
 *
 * ── 导航结构（两层）──
 * ```
 * 底栏 4 个板块 ──► 板块页（卡片网格）──► 功能页
 *                                      └─► 返回钮回到板块页
 * ```
 * 每个板块**各自记住**当前打开的系统：在「传统」里进了六爻，
 * 切到「日常」再切回来，仍然停在六爻 —— 这是 iOS 的 tab 语义。
 *
 * ── 为什么不引入 navigation-compose ──
 * 钻取只有两层，手写状态机更贴合项目「无 DI、全手工装配」的既有风格；
 * 且 `NavHost` 会与 [MagicScaffold] 的「每 tab 一个 ScrollState」
 * 和「唯一根 backdrop」两个机制打架。
 *
 * ── 容器变形转场（v1.6）──
 * 状态机不变，只在**渲染层**外面套一层 `SharedTransitionLayout + AnimatedContent`：
 * 状态切换时新旧两页同时在组合树里，`sharedBounds` 才有「起点 / 终点」可插值。
 * 卡片与页面的配对 key 由 [MagicPage] 派生，普通 tab 切换没有对应 key，
 * 自然走回退的淡入淡出。
 */
@Composable
private fun MagicApp() {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val recordStore = remember { DivinationRecordStore(context) }
    val secretStore = remember { SecretStore(context) }

    var settings by remember { mutableStateOf(MagicSettings.DEFAULT) }
    LaunchedEffect(Unit) {
        settingsStore.settings.collect { s ->
            settings = s
            // 把偏好同步到全局缓存 —— 降级链与端上后端都从那里读。
            // 放这里而不是设置页里，是为了冷启动时就生效。
            InferencePrefs.update(
                s.engineTier,
                s.inferenceBackend,
                s.modelVariant,
                s.interpretStyle,
            )
        }
    }

    // API Key 单独管理：它不走 MagicSettings（那个会明文序列化落盘），
    // 而是存在 Android Keystore 加密的 SecretStore 里。
    var cloudApiKey by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        cloudApiKey = secretStore.get(SecretStore.KEY_CLOUD_API_KEY)
    }

    var historyCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        recordStore.records.collect { historyCount = it.size }
    }

    var tab by remember { mutableIntStateOf(0) }

    // 每个板块各自记住打开的系统；null = 停留在板块首页
    val openSystems = remember { mutableStateListOf<SystemId?>(null, null, null, null) }
    var showLicenses by remember { mutableStateOf(false) }

    val board = AppTab.entries[tab]
    val openSystem = openSystems[tab]

    // 返回语义：先退子页（许可），再退功能页，都退完了才交给系统（退出应用）
    val onBack: (() -> Unit)? = when {
        board == AppTab.SETTINGS && showLicenses -> ({ showLicenses = false })
        openSystem != null -> ({ openSystems[tab] = null })
        else -> null
    }

    BackHandler(enabled = onBack != null) { onBack?.invoke() }

    val scope = rememberCoroutineScope()

    val glassMode = when (settings.glassQuality) {
        GlassQuality.FULL -> GlassMode.FULL
        GlassQuality.REDUCED -> GlassMode.REDUCED
        GlassQuality.OFF -> GlassMode.OFF
    }

    // 功能页有自己的滚动位置。
    //
    // 以前板块页与功能页共用 MagicScaffold 下发的那个 ScrollState，
    // 于是「进功能页先 scrollTo(0)」会把板块页也归零 ——
    // 返回时列表跳回顶部，卡片位置和用户的浏览位置一起丢了。
    // 分开之后，返回必然落在原卡片上，转场起点才对得上。
    val featureScrolls = remember { HashMap<SystemId, ScrollState>() }

    val page: MagicPage = when {
        board == AppTab.SETTINGS && showLicenses -> MagicPage.Settings(licenses = true)
        board == AppTab.SETTINGS -> MagicPage.Settings(licenses = false)
        openSystem != null -> MagicPage.Feature(openSystem)
        else -> MagicPage.Board(board)
    }

    CompositionLocalProvider(
        LocalGlassTuning provides GlassTuning(
            refractionScale = settings.refractionScale,
            surfaceAlphaScale = settings.surfaceAlphaScale,
        )
    ) {
        MagicScaffold(
            tabs = AppTab.entries.map { LiquidTab(it.label, it.icon) },
            selectedTab = tab,
            onSelectTab = { tab = it },
            onBack = onBack,
            glassMode = glassMode,
        ) { boardScrollState ->
            // ⚠️ SharedTransitionLayout 必须落在 layerBackdrop 子树**内部**。
            //
            // 共享元素在转场期间被搬到本作用域的 overlay 里绘制；只要 overlay
            // 还在内容层录制范围内，玻璃表面就仍能通过 LocalBackdrop 采样到背景，
            // 折射跟着容器一起走。搬到外面就会「动画期间玻璃失效、落位后突然出现」。
            SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = page,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = { magicPageTransition(initialState, targetState) },
                    label = "magic-page",
                ) { target ->
                    val progress = magicTransitionProgress()
                    val transitionScope = remember {
                        MagicTransitionScope(
                            shared = this@SharedTransitionLayout,
                            visibility = this,
                            progressState = progress,
                        )
                    }

                    CompositionLocalProvider(
                        LocalMagicTransitionScope provides transitionScope,
                        // 只有功能页的大标题参与共享；板块页 / 设置页没有配对的卡片标题。
                        LocalPageTitleSharedKey provides
                            (target as? MagicPage.Feature)?.system?.name,
                        // ⚠️ 必须**按页面**下发，不能沿用 MagicScaffold 那个全局值。
                        //
                        // 返回钮是浮层，MagicPageContent 会读这个值来决定顶部内边距。
                        // 若两页共用同一个值：钻取时它立刻变 true，正在淡出的板块页
                        // 顶部内边距也跟着涨 44dp —— 实测退场过程中整页会往下跳一下。
                        LocalBackButtonVisible provides (
                            target is MagicPage.Feature ||
                                (target is MagicPage.Settings && target.licenses)
                            ),
                    ) {
                        when (target) {
                            is MagicPage.Board -> BoardScreen(
                                board = target.tab,
                                scrollState = boardScrollState,
                                onOpen = { openSystems[target.tab.ordinal] = it },
                                onUnavailable = { /* 见 SystemId.available 注释 */ },
                            )

                            is MagicPage.Feature -> transitionScope.FeaturePage(
                                system = target.system,
                                scrollState = featureScrolls.getOrPut(target.system) {
                                    ScrollState(0)
                                },
                                onNavigateTo = { next ->
                                    tab = AppTab.entries.indexOf(next.board)
                                    openSystems[next.board.ordinal] = next
                                },
                            )

                            is MagicPage.Settings -> if (target.licenses) {
                                LicenseScreen(onDismiss = { showLicenses = false })
                            } else {
                                SettingsScreen(
                                    settings = settings,
                                    onChange = { next ->
                                        settings = next
                                        scope.launch { settingsStore.update { next } }
                                    },
                                    cloudApiKey = cloudApiKey,
                                    onCloudApiKeyChange = { key ->
                                        cloudApiKey = key
                                        secretStore.put(SecretStore.KEY_CLOUD_API_KEY, key)
                                    },
                                    historyCount = historyCount,
                                    onClearHistory = { scope.launch { recordStore.clear() } },
                                    onOpenLicenses = { showLicenses = true },
                                    scrollState = boardScrollState,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 页面路由。
 *
 * 只描述「哪一页」，不持有任何 UI 状态 —— 这样它才能作为
 * [AnimatedContent] 的 targetState 参与相等性比较，决定是否触发转场。
 */
private sealed interface MagicPage {

    /** 板块首页 */
    data class Board(val tab: AppTab) : MagicPage

    /** 功能页（钻取目标） */
    data class Feature(val system: SystemId) : MagicPage

    /** 设置页 / 许可页 */
    data class Settings(val licenses: Boolean) : MagicPage
}

/**
 * 页面级转场规格。
 *
 * 只有「板块页 ↔ 功能页」这一对使用容器变形；其余（tab 切换、设置页）
 * 一律走普通淡入淡出 —— 没有来源卡片的导航硬套卡片缩放只会显得莫名其妙。
 *
 * ⚠️ 两个分支的 enter/exit **时长**都必须是 [MagicTransitionSpec.DurationMillis]：
 * `AnimatedContent` 靠退出动画跑完来销毁旧页面，提前销毁会连带取消共享元素转场。
 */
private fun magicPageTransition(
    initial: MagicPage,
    target: MagicPage,
): ContentTransform {
    val isContainerMorph = (initial is MagicPage.Board && target is MagicPage.Feature) ||
        (initial is MagicPage.Feature && target is MagicPage.Board)

    return if (isContainerMorph) {
        ContentTransform(
            targetContentEnter = MagicTransitionSpec.PageEnter,
            initialContentExit = MagicTransitionSpec.PageExit,
            sizeTransform = SizeTransform(clip = false),
        )
    } else {
        ContentTransform(
            targetContentEnter = fadeIn(tween(durationMillis = 240, easing = FastOutSlowInEasing)),
            initialContentExit = fadeOut(tween(durationMillis = 160)),
            sizeTransform = SizeTransform(clip = false),
        )
    }
}

/**
 * 功能页外壳 —— 容器变形的**目标边界**。
 *
 * 结构自下而上：
 * 1. 共享边界（`pageBounds`）：从被点卡片的矩形连续长到整页
 * 2. 玻璃面板：卡片材质的延续，随进度收角并淡出
 * 3. 页面本体：跟随容器缩放，前 110ms 保持透明
 */
@Composable
private fun MagicTransitionScope.FeaturePage(
    system: SystemId,
    scrollState: ScrollState,
    onNavigateTo: (SystemId) -> Unit,
) {
    MagicContainerTransformSurface(
        progress = { progress },
        modifier = pageBounds(system.name, Modifier.fillMaxSize()),
    ) {
        FeatureHost(
            system = system,
            scrollState = scrollState,
            onNavigateTo = onNavigateTo,
        )
    }
}

/**
 * 功能页分派。
 *
 * 未实现的系统不会走到这里 —— [BoardScreen] 已经拦掉（见 [SystemId.available]）。
 * 因此这里的 `else` 只会在「已标记可用但忘了接线」时触发，属于开发期错误，
 * 用一句明确文案兜住而不是崩溃。
 */
@Composable
private fun FeatureHost(
    system: SystemId,
    scrollState: ScrollState,
    onNavigateTo: (SystemId) -> Unit,
) {
    when (system) {
        SystemId.LIUYAO -> LiuyaoScreen(scrollState)
        SystemId.BAZI -> BaziScreen(scrollState)
        SystemId.MEIHUA -> MeihuaScreen(scrollState)
        SystemId.XIAOLIUREN -> XiaoLiuRenScreen(scrollState)
        SystemId.TAROT -> TarotScreen(scrollState)
        SystemId.MBTI -> PersonalityScreen(scrollState)
        SystemId.ZIWEI -> ZiWeiScreen(scrollState)
        SystemId.DALIUREN -> DaLiuRenScreen(scrollState)
        SystemId.QIMEN -> QiMenScreen(scrollState)
        SystemId.DAILY_FORTUNE -> FortuneScreen(FortunePeriod.DAILY, scrollState)
        SystemId.MONTHLY_FORTUNE -> FortuneScreen(FortunePeriod.MONTHLY, scrollState)
        SystemId.ALMANAC -> AlmanacScreen(
            scrollState = scrollState,
            // 日运尚未实现时不显示这条动线 —— 宁可没有入口，
            // 也不给一个点进去是空白页的死按钮。
            onOpenDailyFortune = if (SystemId.DAILY_FORTUNE.available) {
                { onNavigateTo(SystemId.DAILY_FORTUNE) }
            } else {
                null
            },
        )
        else -> Text("「${system.title}」尚未接入页面（开发期错误）")
    }
}
