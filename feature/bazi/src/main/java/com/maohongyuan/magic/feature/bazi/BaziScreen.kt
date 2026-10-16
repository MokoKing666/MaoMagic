package com.maohongyuan.magic.feature.bazi

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicDivider
import com.maohongyuan.magic.designsystem.glass.MagicHeaderCell
import com.maohongyuan.magic.designsystem.glass.MagicCell
import com.maohongyuan.magic.designsystem.glass.MagicLabel
import com.maohongyuan.magic.designsystem.glass.MagicMetricBar
import com.maohongyuan.magic.designsystem.glass.MagicPageContent
import com.maohongyuan.magic.designsystem.glass.MagicPageHeader
import com.maohongyuan.magic.designsystem.glass.MagicPlaceholderNote
import com.maohongyuan.magic.designsystem.glass.MagicSectionTitle
import com.maohongyuan.magic.designsystem.glass.MagicSolidButton
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.LocalMagicWuXing
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

/**
 * 八字排盘。
 *
 * ⚠️ UI 阶段：全部为静态示例数据，交互未接线。
 */
@Composable
fun BaziScreen(scrollState: androidx.compose.foundation.ScrollState) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(
            title = "八字命盘",
            subtitle = "四柱 · 十神 · 大运流年",
        )

        MagicPlaceholderNote("本页为界面预览，数据为静态示例，尚未接入排盘计算")

        // ── 生辰输入 ──
        GlassCard {
            MagicSectionTitle("生辰")
            Spacer(Modifier.height(MagicSpacing.row))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row),
            ) {
                FakeInput("1990-06-15", Modifier.weight(1.4f))
                FakeInput("08:30", Modifier.weight(1f))
            }
            Spacer(Modifier.height(MagicSpacing.row))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row),
            ) {
                FakeInput("男", Modifier.weight(1f))
                FakeInput("北京", Modifier.weight(2.4f))
            }
            Spacer(Modifier.height(MagicSpacing.cardPadding))
            MagicSolidButton(
                onClick = {},
                selected = true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "排盘",
                    fontSize = MagicType.body.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent,
                )
            }
        }

        // ── 四柱 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("四柱")
            Spacer(Modifier.height(MagicSpacing.md))

            Row(modifier = Modifier.fillMaxWidth()) {
                MagicHeaderCell("", weight = 0.72f)
                MagicHeaderCell("年柱", weight = 1f, alignment = TextAlign.Center)
                MagicHeaderCell("月柱", weight = 1f, alignment = TextAlign.Center)
                MagicHeaderCell("日柱", weight = 1f, alignment = TextAlign.Center)
                MagicHeaderCell("时柱", weight = 1f, alignment = TextAlign.Center)
            }
            Spacer(Modifier.height(MagicSpacing.xs))
            MagicDivider()
            Spacer(Modifier.height(MagicSpacing.xs))

            PillarRow("天干", listOf("庚", "壬", "辛", "壬"), emphasized = true)
            PillarRow("地支", listOf("午", "午", "酉", "辰"), emphasized = true)
            PillarRow("藏干", listOf("丁己", "丁己", "辛", "戊乙癸"))
            PillarRow("十神", listOf("劫财", "伤官", "日主", "伤官"))
            PillarRow("纳音", listOf("路旁土", "杨柳木", "石榴木", "长流水"))
            PillarRow("神煞", listOf("将星", "文昌", "禄神", "华盖"))

            Spacer(Modifier.height(MagicSpacing.md))
            MagicCaption("日主辛金，生于午月，火旺金弱。")
        }

        // ── 五行旺衰 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("五行旺衰")
            Spacer(Modifier.height(MagicSpacing.md))

            val wuXing = LocalMagicWuXing.current
            Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)) {
                MagicMetricBar("木", 0.12f, wuXing.wood)
                MagicMetricBar("火", 0.34f, wuXing.fire)
                MagicMetricBar("土", 0.18f, wuXing.earth)
                MagicMetricBar("金", 0.24f, wuXing.metal)
                MagicMetricBar("水", 0.12f, wuXing.water)
            }

            Spacer(Modifier.height(MagicSpacing.md))
            MagicDivider()
            Spacer(Modifier.height(MagicSpacing.md))

            Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                MagicBadge("喜用 金 水", colors.accent)
                MagicBadge("忌神 火 土", colors.warning)
            }
        }

        // ── 十神统计 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("十神分布")
            Spacer(Modifier.height(MagicSpacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                TenGodRow("比肩", "劫财", 2)
                TenGodRow("食神", "伤官", 3)
                TenGodRow("偏财", "正财", 0)
                TenGodRow("七杀", "正官", 1)
                TenGodRow("偏印", "正印", 2)
            }
        }

        // ── 大运 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("大运")
            Spacer(Modifier.height(MagicSpacing.xs))
            MagicCaption("6 岁起运 · 顺行")
            Spacer(Modifier.height(MagicSpacing.md))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row),
            ) {
                listOf(
                    Triple("6", "乙未", "正财"),
                    Triple("16", "丙申", "七杀"),
                    Triple("26", "丁酉", "正官"),
                    Triple("36", "戊戌", "偏印"),
                    Triple("46", "己亥", "正印"),
                    Triple("56", "庚子", "比肩"),
                ).forEachIndexed { index, (age, ganZhi, tenGod) ->
                    val active = index == 2
                    Column(
                        modifier = Modifier
                            .width(84.dp)
                            .background(
                                color = if (active) colors.accent.copy(alpha = 0.12f) else Color.Transparent,
                                shape = shapes.controlInCard,
                            )
                            .padding(vertical = MagicSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${age}岁",
                            fontSize = MagicType.caption.sp,
                            color = colors.tertiaryText,
                        )
                        Text(
                            text = ganZhi,
                            fontSize = MagicType.body.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) colors.accent else colors.primaryText,
                        )
                        Text(
                            text = tenGod,
                            fontSize = MagicType.caption.sp,
                            color = if (active) colors.accent else colors.tertiaryText,
                        )
                    }
                }
            }
        }

        // ── 神煞 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("神煞")
            Spacer(Modifier.height(MagicSpacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                ShenShaRow("天乙贵人", "日柱", colors.auspicious)
                ShenShaRow("文昌贵人", "月柱", colors.auspicious)
                ShenShaRow("羊刃", "年柱", colors.warning)
                ShenShaRow("孤辰", "时柱", colors.inauspicious)
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

/** 输入框外观占位（不可输入） */
@Composable
private fun FakeInput(hint: String, modifier: Modifier = Modifier) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    Box(
        modifier = modifier
            .background(
                color = if (colors.isLight) Color.White.copy(alpha = 0.72f)
                else Color.Black.copy(alpha = 0.40f),
                shape = shapes.controlInCard,
            )
            .padding(horizontal = MagicSpacing.md, vertical = MagicSpacing.md),
    ) {
        Text(
            text = hint,
            fontSize = MagicType.subheadline.sp,
            color = colors.secondaryText,
        )
    }
}

/** 四柱表的一行 */
@Composable
private fun PillarRow(
    label: String,
    values: List<String>,
    emphasized: Boolean = false,
) {
    val colors = LocalMagicColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MagicSpacing.row),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MagicCell(text = label, weight = 0.72f, color = colors.tertiaryText)
        values.forEachIndexed { index, value ->
            MagicCell(
                text = value,
                weight = 1f,
                emphasized = emphasized,
                // 日柱是命主本身，用强调色标出
                color = if (index == 2 && emphasized) colors.accent else null,
                alignment = TextAlign.Center,
            )
        }
    }
}

/** 十神一行：一对（偏 / 正）+ 数量点阵 */
@Composable
private fun TenGodRow(left: String, right: String, count: Int) {
    val colors = LocalMagicColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = left,
            fontSize = MagicType.footnote.sp,
            color = colors.secondaryText,
            modifier = Modifier.width(56.dp),
        )
        Text(
            text = right,
            fontSize = MagicType.footnote.sp,
            color = colors.secondaryText,
            modifier = Modifier.width(56.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.xs)) {
            repeat(count) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(6.dp)
                        .background(colors.accent, LocalMagicShapes.current.pill),
                )
            }
        }
        if (count == 0) {
            Text(
                text = "—",
                fontSize = MagicType.footnote.sp,
                color = colors.tertiaryText,
            )
        }
    }
}

/** 神煞一行 */
@Composable
private fun ShenShaRow(name: String, position: String, color: Color) {
    val colors = LocalMagicColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .background(color, LocalMagicShapes.current.pill),
        )
        Spacer(Modifier.width(MagicSpacing.md))
        Text(
            text = name,
            fontSize = MagicType.subheadline.sp,
            fontWeight = FontWeight.Medium,
            color = colors.primaryText,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = position,
            fontSize = MagicType.footnote.sp,
            color = colors.tertiaryText,
        )
    }
}
