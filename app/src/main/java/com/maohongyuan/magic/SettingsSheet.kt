package com.maohongyuan.magic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
import com.maohongyuan.magic.designsystem.glass.GlassLevel
import com.maohongyuan.magic.designsystem.glass.GlassMode
import com.maohongyuan.magic.designsystem.glass.LocalContentBackdrop
import com.maohongyuan.magic.designsystem.glass.MagicBadge
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicDivider
import com.maohongyuan.magic.designsystem.glass.MagicGlassSurface
import com.maohongyuan.magic.designsystem.glass.MagicLabel
import com.maohongyuan.magic.designsystem.glass.MagicListRow
import com.maohongyuan.magic.designsystem.glass.MagicSegmentedControl
import com.maohongyuan.magic.designsystem.glass.MagicSolidButton
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

/**
 * 设置弹窗。
 *
 * ⚠️ UI 阶段：仅「玻璃质感」可切换（用于验证降级效果），其余项为占位。
 */
@Composable
fun SettingsSheet(
    glassMode: GlassMode,
    onGlassModeChange: (GlassMode) -> Unit,
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
                    .height(520.dp)
                    .padding(MagicSpacing.cardPadding),
            ) {
                Text(
                    text = "设置",
                    fontSize = MagicType.title2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryText,
                )
                Spacer(Modifier.height(MagicSpacing.md))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MagicSpacing.section),
                ) {
                    // ── 外观 ──
                    Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)) {
                        MagicLabel("外观 · 玻璃质感")
                        MagicSegmentedControl(
                            options = listOf("完整", "简化", "关闭"),
                            selectedIndex = when (glassMode) {
                                GlassMode.FULL -> 0
                                GlassMode.REDUCED -> 1
                                GlassMode.OFF -> 2
                            },
                            onSelect = {
                                onGlassModeChange(
                                    when (it) {
                                        0 -> GlassMode.FULL
                                        1 -> GlassMode.REDUCED
                                        else -> GlassMode.OFF
                                    }
                                )
                            },
                        )
                        MagicCaption(
                            when (glassMode) {
                                GlassMode.FULL -> "完整折射与模糊。观感最好，性能开销最高。"
                                GlassMode.REDUCED -> "保留模糊、关闭折射。适合中低端机或省电场景。"
                                GlassMode.OFF -> "不使用玻璃效果，改为高不透明实色。可读性最好。"
                            }
                        )
                    }

                    MagicDivider()

                    // ── 解卦引擎 ──
                    Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)) {
                        MagicLabel("解卦引擎")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "基础模式（离线可用）",
                                fontSize = MagicType.subheadline.sp,
                                color = colors.secondaryText,
                                modifier = Modifier.weight(1f),
                            )
                            MagicBadge("当前", colors.accent)
                        }
                        MagicCaption("智能模式（云端 AI）与本地模型需要额外配置，将在后续版本开放。")
                    }

                    MagicDivider()

                    // ── 配色约定 ──
                    Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)) {
                        MagicLabel("吉凶配色")
                        MagicSegmentedControl(
                            options = listOf("中式", "西式"),
                            selectedIndex = 0,
                            onSelect = {},
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✓",
                                fontSize = MagicType.subheadline.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.auspicious,
                            )
                            Spacer(Modifier.width(MagicSpacing.xs))
                            Text(
                                text = "吉",
                                fontSize = MagicType.footnote.sp,
                                color = colors.auspicious,
                                modifier = Modifier.width(40.dp),
                            )
                            Text(
                                text = "✕",
                                fontSize = MagicType.subheadline.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.inauspicious,
                            )
                            Spacer(Modifier.width(MagicSpacing.xs))
                            Text(
                                text = "凶",
                                fontSize = MagicType.footnote.sp,
                                color = colors.inauspicious,
                            )
                        }
                        MagicCaption("中式约定：吉为红、凶为绿，与西方习惯相反。字形与颜色同时生效。")
                    }

                    MagicDivider()

                    // ── 关于 ──
                    Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.xs)) {
                        MagicLabel("关于")
                        MagicListRow(
                            title = "开源许可",
                            subtitle = "字体 · 库 · 协议",
                            onClick = onOpenLicenses,
                        )
                        MagicListRow(title = "隐私政策", subtitle = "不收集任何个人信息")
                        MagicListRow(title = "版本", subtitle = "0.1.0", trailing = null)
                    }

                    MagicCaption("术数内容仅供传统文化研究与娱乐，不构成任何决策建议。")
                }

                Spacer(Modifier.height(MagicSpacing.md))

                MagicSolidButton(
                    onClick = onDismiss,
                    selected = true,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = MagicSpacing.md),
                ) {
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
