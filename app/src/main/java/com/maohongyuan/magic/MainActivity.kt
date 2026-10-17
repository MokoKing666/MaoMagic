package com.maohongyuan.magic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.maohongyuan.magic.core.data.DivinationRecordStore
import com.maohongyuan.magic.core.data.GlassQuality
import com.maohongyuan.magic.core.data.MagicSettings
import com.maohongyuan.magic.core.data.SecretStore
import com.maohongyuan.magic.core.data.SettingsStore
import com.maohongyuan.magic.designsystem.glass.GlassTuning
import com.maohongyuan.magic.designsystem.glass.LocalGlassTuning
import kotlinx.coroutines.launch
import com.maohongyuan.magic.designsystem.glass.GlassMode
import com.maohongyuan.magic.designsystem.glass.LiquidTab
import com.maohongyuan.magic.designsystem.glass.MagicScaffold
import com.maohongyuan.magic.designsystem.glass.MagicTabIcon
import com.maohongyuan.magic.designsystem.theme.MagicTheme
import com.maohongyuan.magic.feature.almanac.AlmanacScreen
import com.maohongyuan.magic.feature.bazi.BaziScreen
import com.maohongyuan.magic.feature.liuyao.LiuyaoScreen
import com.maohongyuan.magic.feature.meihua.MeihuaScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MagicTheme { MagicApp() }
        }
    }
}

/** Tab 用简称。页面大标题（全称）由各页面自己的 MagicPageHeader 承担。 */
private val Tabs = listOf(
    LiquidTab(label = "六爻", icon = MagicTabIcon.SIX_YAO),
    LiquidTab(label = "八字", icon = MagicTabIcon.FOUR_PILLARS),
    LiquidTab(label = "梅花", icon = MagicTabIcon.PLUM_FLOWER),
    LiquidTab(label = "黄历", icon = MagicTabIcon.ALMANAC),
)

@Composable
private fun MagicApp() {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val recordStore = remember { DivinationRecordStore(context) }
    val secretStore = remember { SecretStore(context) }

    // API Key 单独管理：它不走 MagicSettings（那个会明文序列化落盘），
    // 而是存在 Android Keystore 加密的 SecretStore 里。
    var cloudApiKey by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        cloudApiKey = secretStore.get(SecretStore.KEY_CLOUD_API_KEY)
    }

    // 设置：先给默认值，读到持久化值后自动替换 —— 避免启动时白屏或闪烁
    var settings by remember { mutableStateOf(MagicSettings.DEFAULT) }
    LaunchedEffect(Unit) {
        settingsStore.settings.collect { settings = it }
    }

    var historyCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        recordStore.records.collect { historyCount = it.size }
    }

    var tab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // 设置里的档位 → 设计系统的 GlassMode
    val glassMode = when (settings.glassQuality) {
        GlassQuality.FULL -> GlassMode.FULL
        GlassQuality.REDUCED -> GlassMode.REDUCED
        GlassQuality.OFF -> GlassMode.OFF
    }

    // 把玻璃调校下发给整个设计系统
    CompositionLocalProvider(
        LocalGlassTuning provides GlassTuning(
            refractionScale = settings.refractionScale,
            surfaceAlphaScale = settings.surfaceAlphaScale,
        )
    ) {
        MagicScaffold(
            tabs = Tabs,
            selectedTab = tab,
            onSelectTab = { tab = it },
            onOpenSettings = { showSettings = true },
            glassMode = glassMode,
            floatingOverlay = {
                if (showSettings) {
                    SettingsSheet(
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
                        onDismiss = { showSettings = false },
                    )
                }
                if (showLicenses) {
                    LicenseScreen(onDismiss = { showLicenses = false })
                }
            },
        ) { scrollState ->
            when (tab) {
                0 -> LiuyaoScreen(scrollState)
                1 -> BaziScreen(scrollState)
                2 -> MeihuaScreen(scrollState)
                else -> AlmanacScreen(scrollState)
            }
        }
    }
}
