package com.maohongyuan.magic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.platform.LocalContext
import com.maohongyuan.magic.board.BoardScreen
import com.maohongyuan.magic.core.data.DivinationRecordStore
import com.maohongyuan.magic.core.data.GlassQuality
import com.maohongyuan.magic.core.data.MagicSettings
import com.maohongyuan.magic.core.data.SecretStore
import com.maohongyuan.magic.core.data.SettingsStore
import com.maohongyuan.magic.designsystem.glass.GlassMode
import com.maohongyuan.magic.designsystem.glass.GlassTuning
import com.maohongyuan.magic.designsystem.glass.LiquidTab
import com.maohongyuan.magic.designsystem.glass.LocalGlassTuning
import com.maohongyuan.magic.designsystem.glass.MagicScaffold
import com.maohongyuan.magic.designsystem.theme.MagicTheme
import com.maohongyuan.magic.feature.almanac.AlmanacScreen
import com.maohongyuan.magic.feature.bazi.BaziScreen
import com.maohongyuan.magic.feature.liuyao.LiuyaoScreen
import com.maohongyuan.magic.feature.meihua.MeihuaScreen
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
            MagicTheme {
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
 */
@Composable
private fun MagicApp() {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val recordStore = remember { DivinationRecordStore(context) }
    val secretStore = remember { SecretStore(context) }

    var settings by remember { mutableStateOf(MagicSettings.DEFAULT) }
    LaunchedEffect(Unit) {
        settingsStore.settings.collect { settings = it }
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
        ) { scrollState ->
            when (board) {
                AppTab.SETTINGS -> {
                    if (showLicenses) {
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
                            scrollState = scrollState,
                        )
                    }
                }

                else -> {
                    if (openSystem == null) {
                        BoardScreen(
                            board = board,
                            scrollState = scrollState,
                            onOpen = { openSystems[tab] = it },
                            onUnavailable = { /* 见 SystemId.available 注释 */ },
                        )
                    } else {
                        // 钻进取顶层：板块页很短，留着上次的滚动位置会让人以为页面是空的
                        LaunchedEffect(openSystem) { scrollState.scrollTo(0) }
                        FeatureHost(
                            system = openSystem,
                            scrollState = scrollState,
                            // 功能页之间互相跳转：黄历 → 日运 这类动线要用。
                            // 目标若在别的板块，连 tab 一起切过去。
                            onNavigateTo = { target ->
                                tab = AppTab.entries.indexOf(target.board)
                                openSystems[target.board.ordinal] = target
                            },
                        )
                    }
                }
            }
        }
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
