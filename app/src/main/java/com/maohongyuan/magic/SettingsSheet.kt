package com.maohongyuan.magic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.core.data.ColorScheme
import com.maohongyuan.magic.core.data.GlassQuality
import com.maohongyuan.magic.core.data.MagicSettings
import com.maohongyuan.magic.designsystem.glass.GlassLevel
import com.maohongyuan.magic.designsystem.glass.MagicBadge
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicDivider
import com.maohongyuan.magic.designsystem.glass.MagicGlassSliderRow
import com.maohongyuan.magic.designsystem.glass.MagicGlassSurface
import com.maohongyuan.magic.designsystem.glass.MagicLabel
import androidx.compose.ui.text.input.KeyboardType
import com.maohongyuan.magic.designsystem.glass.MagicTextField
import com.maohongyuan.magic.designsystem.glass.MagicListRow
import com.maohongyuan.magic.designsystem.glass.MagicSegmentedControl
import com.maohongyuan.magic.designsystem.glass.MagicSolidButton
import com.maohongyuan.magic.designsystem.glass.LocalContentBackdrop
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.MagicSemanticMark
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

/**
 * 设置面板。
 *
 * 所有项**即时生效并持久化**（DataStore），没有「保存」按钮 ——
 * 设置类面板加确认按钮是移动端的反模式，用户改完就想立刻看到效果。
 */
@Composable
fun SettingsSheet(
    settings: MagicSettings,
    onChange: (MagicSettings) -> Unit,
    /** API Key 单独走 Keystore，不进 MagicSettings（后者会明文落盘） */
    cloudApiKey: String,
    onCloudApiKeyChange: (String) -> Unit,
    historyCount: Int,
    onClearHistory: () -> Unit,
    onOpenLicenses: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current

    // 遮罩：模态态必须让下层明确退后
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f))
            .clickable(onClick = onDismiss),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(MagicSpacing.screenMargin)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.BottomCenter,
    ) {
        MagicGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.sheet,
            level = GlassLevel.Thick,
            // 弹窗压在页面之上 ⇒ 用内容折射源，模糊身后的页面内容
            backdrop = LocalContentBackdrop.current,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp)
                    .padding(MagicSpacing.cardPadding),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "设置",
                        fontSize = MagicType.title2.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText,
                        modifier = Modifier.weight(1f),
                    )
                    MagicBadge("v${BuildConfig.VERSION_NAME}", colors.accent)
                }
                Spacer(Modifier.height(MagicSpacing.md))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MagicSpacing.section),
                ) {
                    // ── 外观 ──
                    Section("外观") {
                        MagicLabel("玻璃质感")
                        Spacer(Modifier.height(MagicSpacing.row))
                        MagicSegmentedControl(
                            options = GlassQuality.entries.map { it.label },
                            selectedIndex = GlassQuality.entries.indexOf(settings.glassQuality),
                            onSelect = { onChange(settings.copy(glassQuality = GlassQuality.entries[it])) },
                        )
                        Spacer(Modifier.height(MagicSpacing.xs))
                        MagicCaption(settings.glassQuality.description)

                        if (settings.glassQuality != GlassQuality.OFF) {
                            Spacer(Modifier.height(MagicSpacing.cardPadding))
                            MagicGlassSliderRow(
                                label = "折射强度",
                                valueText = "%.2f×".format(settings.refractionScale),
                                value = settings.refractionScale,
                                onValueChange = { onChange(settings.copy(refractionScale = it)) },
                                valueRange = MagicSettings.REFRACTION_RANGE,
                                caption = "1.00× 为设计标定值。调高更「液体」，但过高会让色散把周围明暗差读成色差。",
                            )

                            Spacer(Modifier.height(MagicSpacing.cardPadding))
                            MagicGlassSliderRow(
                                label = "表面通透度",
                                valueText = "%.2f×".format(settings.surfaceAlphaScale),
                                value = settings.surfaceAlphaScale,
                                onValueChange = { onChange(settings.copy(surfaceAlphaScale = it)) },
                                valueRange = MagicSettings.SURFACE_ALPHA_RANGE,
                                caption = "调低更通透。表面不透明度是正文可读性的底线，低于 0.85× 时文字对比度会明显下降。",
                            )
                            if (settings.surfaceAlphaScale < 0.85f) {
                                Spacer(Modifier.height(MagicSpacing.xs))
                                Text(
                                    text = "${MagicSemanticMark.WARNING.glyph} 当前通透度较低，正文可读性可能不足",
                                    fontSize = MagicType.caption.sp,
                                    color = colors.warning,
                                )
                            }
                        }
                    }

                    // ── 配色 ──
                    Section("配色") {
                        MagicLabel("吉凶配色")
                        Spacer(Modifier.height(MagicSpacing.row))
                        MagicSegmentedControl(
                            options = ColorScheme.entries.map { it.label },
                            selectedIndex = ColorScheme.entries.indexOf(settings.colorScheme),
                            onSelect = { onChange(settings.copy(colorScheme = ColorScheme.entries[it])) },
                        )
                        Spacer(Modifier.height(MagicSpacing.xs))
                        MagicCaption(
                            if (settings.colorScheme == ColorScheme.CHINESE) {
                                "中式约定：吉为红、凶为绿。与股市涨跌配色一致。"
                            } else {
                                "西式约定：吉为绿、凶为红。"
                            }
                        )
                        Spacer(Modifier.height(MagicSpacing.row))
                        MagicCaption("无论选哪种，吉凶都同时用颜色与字形（✓ / ✕）双通道表达，不单独依赖颜色。")
                    }

                    // ── 解卦引擎 ──
                    Section("解卦引擎") {
                        MagicLabel("模式")
                        Spacer(Modifier.height(MagicSpacing.row))
                        MagicSegmentedControl(
                            options = listOf("基础模式", "智能模式"),
                            selectedIndex = if (settings.cloudAiEnabled) 1 else 0,
                            onSelect = { onChange(settings.copy(cloudAiEnabled = it == 1)) },
                        )
                        Spacer(Modifier.height(MagicSpacing.xs))
                        MagicCaption(
                            if (settings.cloudAiEnabled) {
                                "智能模式：把排好的卦盘发给你指定的 AI 服务，由它做论述。"
                            } else {
                                "基础模式：本地规则引擎，离线可用、结果可复现。默认且推荐。"
                            }
                        )

                        if (settings.cloudAiEnabled) {
                            Spacer(Modifier.height(MagicSpacing.cardPadding))
                            MagicLabel("服务地址")
                            Spacer(Modifier.height(MagicSpacing.sm))
                            MagicTextField(
                                value = settings.cloudAiBaseUrl,
                                onValueChange = { onChange(settings.copy(cloudAiBaseUrl = it)) },
                                placeholder = "https://api.example.com/v1",
                                keyboardType = KeyboardType.Uri,
                            )
                            Spacer(Modifier.height(MagicSpacing.xs))
                            MagicCaption("OpenAI 兼容接口的根地址，不要带 /chat/completions。")

                            Spacer(Modifier.height(MagicSpacing.cardPadding))
                            MagicLabel("API Key")
                            Spacer(Modifier.height(MagicSpacing.sm))
                            MagicTextField(
                                value = cloudApiKey,
                                onValueChange = onCloudApiKeyChange,
                                placeholder = "sk-...",
                                isPassword = true,
                            )
                            Spacer(Modifier.height(MagicSpacing.xs))
                            MagicCaption("只存在本机，用 Android Keystore 加密，不会上传到任何地方。")

                            Spacer(Modifier.height(MagicSpacing.cardPadding))
                            MagicLabel("模型名")
                            Spacer(Modifier.height(MagicSpacing.sm))
                            MagicTextField(
                                value = settings.cloudAiModel,
                                onValueChange = { onChange(settings.copy(cloudAiModel = it)) },
                                placeholder = "gpt-4o-mini",
                            )

                            Spacer(Modifier.height(MagicSpacing.cardPadding))
                            MagicGlassSliderRow(
                                label = "温度",
                                valueText = "%.2f".format(settings.cloudAiTemperature),
                                value = settings.cloudAiTemperature,
                                onValueChange = { onChange(settings.copy(cloudAiTemperature = it)) },
                                valueRange = 0f..1f,
                                caption = "越低越稳定。解卦建议 0.2-0.4：同一卦象两次问不应得到完全不同的结论。",
                            )

                            Spacer(Modifier.height(MagicSpacing.cardPadding))
                            val ready = settings.cloudAiBaseUrl.isNotBlank() &&
                                settings.cloudAiModel.isNotBlank() &&
                                cloudApiKey.isNotBlank()
                            Text(
                                text = if (ready) {
                                    MagicSemanticMark.AUSPICIOUS.glyph +
                                        " 配置完整，起卦时将优先使用云端 AI"
                                } else {
                                    MagicSemanticMark.WARNING.glyph +
                                        " 配置不完整，仍会使用基础模式"
                                },
                                fontSize = MagicType.caption.sp,
                                color = if (ready) colors.auspicious else colors.warning,
                            )
                            Spacer(Modifier.height(MagicSpacing.sm))
                            MagicCaption(
                                "开启后应用会联网：请求发往上面填的地址，内容为卦盘与你所问的问题。" +
                                    "关闭此项即恢复完全离线。"
                            )
                        }
                    }

                    // ── 数据 ──
                    Section("数据") {
                        MagicLabel("自动保存卦例")
                        Spacer(Modifier.height(MagicSpacing.row))
                        MagicSegmentedControl(
                            options = listOf("开", "关"),
                            selectedIndex = if (settings.autoSaveHistory) 0 else 1,
                            onSelect = { onChange(settings.copy(autoSaveHistory = it == 0)) },
                        )
                        Spacer(Modifier.height(MagicSpacing.xs))
                        MagicCaption("开启后每次起卦自动存入本机历史，不联网、不上传。")

                        Spacer(Modifier.height(MagicSpacing.cardPadding))
                        MagicListRow(
                            title = "清空卦例历史",
                            subtitle = "当前 $historyCount 条",
                            trailing = null,
                            onClick = onClearHistory,
                        )
                    }

                    // ── 关于 ──
                    Section("关于") {
                        MagicListRow(
                            title = "开源许可",
                            subtitle = "字体 · 库 · 协议",
                            onClick = onOpenLicenses,
                        )
                        MagicDivider()
                        MagicListRow(
                            title = "版本",
                            subtitle = "v${BuildConfig.VERSION_NAME}",
                            trailing = null,
                        )
                        MagicDivider()
                        MagicCaption(
                            "术数内容仅供传统文化研究与娱乐，不构成任何决策建议。\n" +
                                "本应用不收集任何个人信息，所有数据仅存于本机。"
                        )
                    }

                    Spacer(Modifier.height(MagicSpacing.xs))
                    MagicSolidButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "完成",
                            fontSize = MagicType.body.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        com.maohongyuan.magic.designsystem.glass.MagicSectionTitle(title)
        Spacer(Modifier.height(MagicSpacing.md))
        content()
    }
}
