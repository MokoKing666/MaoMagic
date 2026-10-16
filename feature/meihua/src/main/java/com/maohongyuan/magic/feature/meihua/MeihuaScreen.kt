package com.maohongyuan.magic.feature.meihua

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.maohongyuan.magic.designsystem.glass.MagicInfoRow
import com.maohongyuan.magic.designsystem.glass.MagicPageContent
import com.maohongyuan.magic.designsystem.glass.MagicPageHeader
import com.maohongyuan.magic.designsystem.glass.MagicPlaceholderNote
import com.maohongyuan.magic.designsystem.glass.MagicSectionTitle
import com.maohongyuan.magic.designsystem.glass.MagicSegmentedControl
import com.maohongyuan.magic.designsystem.glass.MagicSolidButton
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.LocalMagicWuXing
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

/**
 * 梅花易数。
 *
 * ⚠️ UI 阶段：全部为静态示例数据，交互未接线。
 *
 * 示例卦象（已按规则核对）：
 * - 本卦 泽火革（上兑☱ 下离☲），九四动
 * - 互卦 天风姤（互下取爻二三四 = 巽☴，互上取爻三四五 = 乾☰）
 * - 变卦 水火既济（九四阳变阴 ⇒ 上卦兑 110 翻初位 = 坎 010）
 * - 体用：动爻在上卦 ⇒ 上卦为用、下卦为体。体离火、用兑金 ⇒ 体克用 ⇒ 吉
 */
@Composable
fun MeihuaScreen(scrollState: androidx.compose.foundation.ScrollState) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(
            title = "梅花易数",
            subtitle = "体用 · 互卦 · 变卦",
        )

        MagicPlaceholderNote("本页为界面预览，数据为静态示例，尚未接入起卦计算")

        // ── 起卦方式 ──
        GlassCard {
            MagicSectionTitle("起卦方式")
            Spacer(Modifier.height(MagicSpacing.md))
            MagicSegmentedControl(
                options = listOf("时间", "数字", "报字"),
                selectedIndex = 0,
                onSelect = {},
            )
            Spacer(Modifier.height(MagicSpacing.cardPadding))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.accent.copy(alpha = 0.06f), shapes.controlInCard)
                    .padding(MagicSpacing.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "✿", fontSize = MagicType.display.sp, color = colors.accent)
                Spacer(Modifier.height(MagicSpacing.row))
                Text(
                    text = "2026-10-16 15:30",
                    fontSize = MagicType.subheadline.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.secondaryText,
                )
                Spacer(Modifier.height(MagicSpacing.xs))
                Text(
                    text = "年支序 + 月 + 日 定上卦，再加时支序 定下卦，总数除 6 取动爻",
                    fontSize = MagicType.caption.sp,
                    color = colors.tertiaryText,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(MagicSpacing.cardPadding))
                MagicSolidButton(
                    onClick = {},
                    selected = true,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "起卦",
                        fontSize = MagicType.body.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent,
                    )
                }
            }
        }

        // ── 三卦 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("卦象")
            Spacer(Modifier.height(MagicSpacing.cardPadding))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MagicSpacing.md),
            ) {
                HexagramColumn(
                    label = "本卦",
                    name = "泽火革",
                    symbol = "䷰",
                    // 自下而上：初阳、二阴、三阳、四阳、五阳、上阴
                    yaoBottomToTop = listOf(true, false, true, true, true, false),
                    movingIndex = 3,
                    modifier = Modifier.weight(1f),
                )
                HexagramColumn(
                    label = "互卦",
                    name = "天风姤",
                    symbol = "䷫",
                    yaoBottomToTop = listOf(false, true, true, true, true, true),
                    modifier = Modifier.weight(1f),
                )
                HexagramColumn(
                    label = "变卦",
                    name = "水火既济",
                    symbol = "䷾",
                    yaoBottomToTop = listOf(true, false, true, false, true, false),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(MagicSpacing.cardPadding))
            MagicCaption("标注 ● 者为动爻。变卦由动爻阴阳互变得出。")
        }

        // ── 体用 ──
        GlassCard {
            MagicSectionTitle("体用分析")
            Spacer(Modifier.height(MagicSpacing.md))

            Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.md)) {
                TiYongChip(
                    label = "体",
                    gua = "离 ☲",
                    element = "火",
                    color = LocalMagicWuXing.current.fire,
                    modifier = Modifier.weight(1f),
                )
                TiYongChip(
                    label = "用",
                    gua = "兑 ☱",
                    element = "金",
                    color = LocalMagicWuXing.current.metal,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(MagicSpacing.cardPadding))
            MagicDivider()
            Spacer(Modifier.height(MagicSpacing.cardPadding))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "✓",
                    fontSize = MagicType.headline.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.auspicious,
                )
                Spacer(Modifier.width(MagicSpacing.row))
                Column {
                    Text(
                        text = "体克用 · 吉",
                        fontSize = MagicType.body.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.auspicious,
                    )
                    Text(
                        text = "火克金，体卦克制用卦。事可为，但需主动付出、过程费力。",
                        fontSize = MagicType.footnote.sp,
                        color = colors.secondaryText,
                    )
                }
            }

            Spacer(Modifier.height(MagicSpacing.cardPadding))
            Column {
                MagicInfoRow("动爻", "九四（上卦）")
                MagicInfoRow("体用判定", "动爻在上卦 ⇒ 上卦为用、下卦为体")
                MagicInfoRow("生克", "体离火 克 用兑金", valueColor = colors.auspicious)
                MagicInfoRow("互卦", "天风姤 · 主事情中间状态")
            }
        }

        // ── 五行生克参考 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("五行生克 · 吉凶倾向")
            Spacer(Modifier.height(MagicSpacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)) {
                RelationRow("用生体", "用卦生体卦", true)
                RelationRow("体用比和", "同五行", true)
                RelationRow("体克用", "体卦克用卦", true)
                RelationRow("体生用", "体卦生用卦（耗泄）", false)
                RelationRow("用克体", "用卦克体卦", false)
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
 * 单个卦的竖向展示：六条爻线（自上而下）+ 卦名。
 *
 * @param yaoBottomToTop 自下而上（初爻 → 上爻）
 * @param movingIndex    动爻的**自下而上**下标（0 = 初爻），null 表示无动爻
 */
@Composable
private fun HexagramColumn(
    label: String,
    name: String,
    symbol: String,
    yaoBottomToTop: List<Boolean>,
    modifier: Modifier = Modifier,
    movingIndex: Int? = null,
) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current

    Column(
        modifier = modifier
            .background(
                color = if (colors.isLight) Color.White.copy(alpha = 0.55f)
                else Color.Black.copy(alpha = 0.32f),
                shape = shapes.controlInCard,
            )
            .padding(MagicSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = MagicType.caption.sp,
            color = colors.tertiaryText,
        )
        Spacer(Modifier.height(MagicSpacing.row))

        // 自上而下绘制：上爻 → 初爻
        yaoBottomToTop.reversed().forEachIndexed { displayIndex, yang ->
            val originalIndex = yaoBottomToTop.lastIndex - displayIndex
            val moving = originalIndex == movingIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                YaoBar(
                    yang = yang,
                    color = if (moving) colors.accent else colors.primaryText,
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (moving) "●" else " ",
                    fontSize = MagicType.caption.sp,
                    color = colors.accent,
                )
            }
            if (displayIndex != 5) Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(MagicSpacing.md))
        Text(text = symbol, fontSize = MagicType.displaySmall.sp, color = colors.primaryText)
        Spacer(Modifier.height(2.dp))
        Text(
            text = name,
            fontSize = MagicType.footnote.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.primaryText,
            textAlign = TextAlign.Center,
        )
    }
}

/** 爻线：阳爻一整条，阴爻断开两段 */
@Composable
private fun YaoBar(
    yang: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val h = size.height
        val radius = CornerRadius(h / 2f)
        if (yang) {
            drawRoundRect(color, Offset.Zero, Size(size.width, h), radius)
        } else {
            val gap = size.width * 0.28f
            val segW = (size.width - gap) / 2f
            drawRoundRect(color, Offset.Zero, Size(segW, h), radius)
            drawRoundRect(color, Offset(size.width - segW, 0f), Size(segW, h), radius)
        }
    }
}

@Composable
private fun TiYongChip(
    label: String,
    gua: String,
    element: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), shapes.controlInCard)
            .padding(MagicSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = MagicType.caption.sp,
                color = colors.tertiaryText,
            )
            Text(
                text = gua,
                fontSize = MagicType.body.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primaryText,
            )
        }
        Text(
            text = element,
            fontSize = MagicType.title2.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun RelationRow(relation: String, description: String, auspicious: Boolean) {
    val colors = LocalMagicColors.current
    val tint = if (auspicious) colors.auspicious else colors.inauspicious
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (auspicious) "✓" else "✕",
            fontSize = MagicType.footnote.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
            modifier = Modifier.width(20.dp),
        )
        Text(
            text = relation,
            fontSize = MagicType.footnote.sp,
            fontWeight = FontWeight.Medium,
            color = colors.primaryText,
            modifier = Modifier.width(76.dp),
        )
        Text(
            text = description,
            fontSize = MagicType.footnote.sp,
            color = colors.tertiaryText,
            modifier = Modifier.weight(1f),
        )
    }
}
