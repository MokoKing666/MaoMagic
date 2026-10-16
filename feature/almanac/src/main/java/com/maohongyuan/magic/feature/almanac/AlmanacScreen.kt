package com.maohongyuan.magic.feature.almanac

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.designsystem.glass.GlassCard
import com.maohongyuan.magic.designsystem.glass.GlassLevel
import com.maohongyuan.magic.designsystem.glass.MagicBadge
import com.maohongyuan.magic.designsystem.glass.MagicCardBody
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicDivider
import com.maohongyuan.magic.designsystem.glass.MagicInfoRow
import com.maohongyuan.magic.designsystem.glass.MagicLabel
import com.maohongyuan.magic.designsystem.glass.MagicPageContent
import com.maohongyuan.magic.designsystem.glass.MagicPageHeader
import com.maohongyuan.magic.designsystem.glass.MagicPlaceholderNote
import com.maohongyuan.magic.designsystem.glass.MagicSectionTitle
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.MagicSemanticMark
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

/**
 * 黄历择日 + 每日运势。
 *
 * ⚠️ UI 阶段：全部为静态示例数据，交互未接线。
 */
@Composable
fun AlmanacScreen(scrollState: androidx.compose.foundation.ScrollState) {
    val colors = LocalMagicColors.current

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(
            title = "今日黄历",
            subtitle = "2026 年 10 月 16 日 · 农历九月初六",
        )

        MagicPlaceholderNote("本页为界面预览，数据为静态示例，尚未接入历法计算")

        // ── 日课概要 ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "初六",
                        fontSize = MagicType.display.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText,
                    )
                    Spacer(Modifier.height(MagicSpacing.xs))
                    Text(
                        text = "丙午年 · 戊戌月 · 丁卯日",
                        fontSize = MagicType.subheadline.sp,
                        color = colors.secondaryText,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    MagicBadge("星期五", colors.accent)
                    Spacer(Modifier.height(MagicSpacing.row))
                    Text(
                        text = "马年",
                        fontSize = MagicType.footnote.sp,
                        color = colors.tertiaryText,
                    )
                }
            }

            MagicDivider()

            MagicCardBody(spacing = 0.dp) {
                MagicInfoRow("纳音", "天河水")
                MagicInfoRow("星宿", "东方青龙 · 亢金龙（凶）")
                MagicInfoRow("建除", "危日")
                MagicInfoRow("黄道", "玉堂（黄道吉日）")
            }
        }

        // ── 宜 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("宜")
            MagicCardBody {
                MarkedList(
                    mark = MagicSemanticMark.AUSPICIOUS,
                    color = colors.auspicious,
                    items = listOf("祭祀", "出行", "签约", "纳财", "安床", "求医"),
                )
            }
        }

        // ── 忌 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("忌")
            MagicCardBody {
                MarkedList(
                    mark = MagicSemanticMark.INAUSPICIOUS,
                    color = colors.inauspicious,
                    items = listOf("动土", "嫁娶", "开市", "入宅"),
                )
            }
        }

        // ── 冲煞与吉神方位 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("冲煞 · 吉神方位")
            MagicCardBody(spacing = 0.dp) {
                MagicInfoRow(
                    label = "冲煞",
                    value = "冲兔（丁卯）煞东",
                    valueColor = colors.warning,
                )
                MagicInfoRow("喜神", "巽 · 东南")
                MagicInfoRow("福神", "兑 · 正西")
                MagicInfoRow("财神", "离 · 正南")
                MagicInfoRow("阳贵神", "巽 · 东南")
                MagicInfoRow("阴贵神", "震 · 正东")
                MagicInfoRow("胎神", "仓库门外正南")
            }
        }

        // ── 时辰吉凶 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("时辰吉凶")
            Spacer(Modifier.height(MagicSpacing.xs))
            HourGrid()
            Spacer(Modifier.height(MagicSpacing.sm))
            MagicCaption("吉时：子、寅、卯、午、申、酉。凶时宜静不宜动。")
        }

        // ── 彭祖百忌 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("彭祖百忌")
            MagicCardBody {
                Text(
                    text = "丁不剃头 头必生疮",
                    fontSize = MagicType.subheadline.sp,
                    color = colors.secondaryText,
                )
                Text(
                    text = "卯不穿井 水泉不香",
                    fontSize = MagicType.subheadline.sp,
                    color = colors.secondaryText,
                )
            }
        }

        Text(
            text = "术数内容仅供传统文化研究与娱乐，不构成任何决策建议。",
            fontSize = MagicType.legalMin.sp,
            color = colors.tertiaryText,
            modifier = Modifier.padding(vertical = MagicSpacing.row),
        )
    }
}

/**
 * 带字形冗余的条目列表。
 *
 * 颜色 + 字形双通道：色觉障碍用户与西式配色习惯用户都能正确理解语义。
 */
@Composable
private fun MarkedList(
    mark: MagicSemanticMark,
    color: Color,
    items: List<String>,
) {
    val colors = LocalMagicColors.current
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = mark.glyph,
            fontSize = MagicType.subheadline.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(24.dp),
        )
        Text(
            text = items.joinToString("、"),
            fontSize = MagicType.subheadline.sp,
            fontWeight = FontWeight.Medium,
            color = colors.primaryText,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 十二时辰吉凶网格 */
@Composable
private fun HourGrid() {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current

    // 支, 时段, 吉凶
    val hours = listOf(
        Triple("子", "23–01", true),
        Triple("丑", "01–03", false),
        Triple("寅", "03–05", true),
        Triple("卯", "05–07", true),
        Triple("辰", "07–09", false),
        Triple("巳", "09–11", false),
        Triple("午", "11–13", true),
        Triple("未", "13–15", false),
        Triple("申", "15–17", true),
        Triple("酉", "17–19", true),
        Triple("戌", "19–21", false),
        Triple("亥", "21–23", false),
    )

    Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
        hours.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                row.forEach { (branch, range, auspicious) ->
                    val tint = if (auspicious) colors.auspicious else colors.inauspicious
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(tint.copy(alpha = 0.10f), shapes.controlInCard)
                            .padding(vertical = MagicSpacing.row),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = branch,
                            fontSize = MagicType.body.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primaryText,
                        )
                        Text(
                            text = range,
                            fontSize = MagicType.caption.sp,
                            color = colors.tertiaryText,
                        )
                        Text(
                            text = if (auspicious) "吉" else "凶",
                            fontSize = MagicType.caption.sp,
                            fontWeight = FontWeight.Bold,
                            color = tint,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
