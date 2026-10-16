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
    var tab by remember { mutableIntStateOf(0) }
    var glassMode by remember { mutableStateOf(GlassMode.FULL) }
    var showSettings by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    MagicScaffold(
        tabs = Tabs,
        selectedTab = tab,
        onSelectTab = { tab = it },
        onOpenSettings = { showSettings = true },
        glassMode = glassMode,
        floatingOverlay = {
            if (showSettings) {
                SettingsSheet(
                    glassMode = glassMode,
                    onGlassModeChange = { glassMode = it },
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
