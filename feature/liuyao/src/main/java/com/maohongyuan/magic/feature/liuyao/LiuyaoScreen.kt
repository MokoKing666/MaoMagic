package com.maohongyuan.magic.feature.liuyao

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.maohongyuan.magic.designsystem.glass.MagicHeaderCell
import com.maohongyuan.magic.designsystem.glass.MagicInfoRow
import com.maohongyuan.magic.designsystem.glass.MagicLabel
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
 * 六爻起卦解卦。
 *
 * ⚠️ UI 阶段：全部为静态示例数据，交互未接线。
 * 卦盘示例为「天风姤 → 泽风大过」，九四动。
 */
@Composable
fun LiuyaoScreen(scrollState: androidx.compose.foundation.ScrollState) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(
            title = "六爻起卦",
            subtitle = "铜钱 · 时间 · 数字 · 手动",
        )

        MagicPlaceholderNote("本页为界面预览，数据为静态示例，尚未接入装卦算法")

        // ── 所问事项 ──
        GlassCard {
            MagicSectionTitle("所问何事")
            Spacer(Modifier.height(MagicSpacing.row))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (colors.isLight) Color.White.copy(alpha = 0.72f)
                        else Color.Black.copy(alpha = 0.40f),
                        shape = shapes.controlInCard,
                    )
                    .padding(MagicSpacing.md),
            ) {
                Text(
                    text = "例：今年事业运势如何？",
                    fontSize = MagicType.subheadline.sp,
                    color = colors.tertiaryText,
                )
            }
            Spacer(Modifier.height(MagicSpacing.md))
            MagicCaption("所问越具体，断卦越有针对性。一事一卦。")
        }

        // ── 起卦方式 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("起卦方式")
            Spacer(Modifier.height(MagicSpacing.md))
            MagicSegmentedControl(
                options = listOf("铜钱", "时间", "数字", "手动"),
                selectedIndex = 0,
                onSelect = {},
            )
            Spacer(Modifier.height(MagicSpacing.cardPadding))

            // 当前方式的操作区（占位）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.accent.copy(alpha = 0.06f), shapes.controlInCard)
                    .padding(MagicSpacing.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "☯",
                    fontSize = MagicType.display.sp,
                    color = colors.accent,
                )
                Spacer(Modifier.height(MagicSpacing.row))
                Text(
                    text = "三枚铜钱，摇六次成卦",
                    fontSize = MagicType.subheadline.sp,
                    color = colors.secondaryText,
                )
                Spacer(Modifier.height(MagicSpacing.xs))
                Text(
                    text = "正面记 3、反面记 2；6 老阴、7 少阳、8 少阴、9 老阳",
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
                        text = "摇卦",
                        fontSize = MagicType.body.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent,
                    )
                }
            }
        }

        // ── 卦盘 ──
        HexagramCard()

        // ── 课盘信息 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("课盘")
            Spacer(Modifier.height(MagicSpacing.md))
            Column {
                MagicInfoRow("月建", "戊戌月 · 土旺")
                MagicInfoRow("日辰", "丁卯日 · 木")
                MagicInfoRow("旬空", "戌、亥")
                MagicInfoRow("用神", "官鬼（问事业）", valueColor = colors.accent)
                MagicInfoRow("世应", "世初应四 · 世克应")
            }
        }

        // ── 互卦 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("互卦")
            Spacer(Modifier.height(MagicSpacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "䷀", fontSize = MagicType.display.sp, color = colors.primaryText)
                Spacer(Modifier.width(MagicSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "乾为天",
                        fontSize = MagicType.body.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryText,
                    )
                    Text(
                        text = "互下取爻二三四 · 互上取爻三四五",
                        fontSize = MagicType.footnote.sp,
                        color = colors.tertiaryText,
                    )
                }
            }
            Spacer(Modifier.height(MagicSpacing.md))
            MagicCaption("互卦主事情发展过程中的中间状态。")
        }

        // ── 断卦提示 ──
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MagicSectionTitle("断卦", modifier = Modifier.weight(1f))
                MagicBadge("基础模式 · 离线", colors.accent)
            }
            Spacer(Modifier.height(MagicSpacing.md))
            MagicDivider()
            Spacer(Modifier.height(MagicSpacing.md))

            Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)) {
                HintItem(
                    title = "用神",
                    body = "问事业以官鬼为用神。官鬼壬午火在四爻，发动化出丁酉金，回头克用神。",
                )
                HintItem(
                    title = "旺衰",
                    body = "用神午火生于戌月，火气渐退；日辰卯木生火，稍有帮扶，但被月建所泄。",
                )
                HintItem(
                    title = "世应",
                    body = "世爻辛丑土在初爻，用神午火生世爻 —— 事来就我，但用神自身受克，须防中途生变。",
                )
                HintItem(
                    title = "六神",
                    body = "官鬼临腾蛇，主事情反复、心中不安。玄武临父母，留意文书疏漏。",
                )
            }

            Spacer(Modifier.height(MagicSpacing.cardPadding))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.auspicious.copy(alpha = 0.10f), shapes.controlInCard)
                    .padding(MagicSpacing.md),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✓",
                            fontSize = MagicType.subheadline.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.auspicious,
                        )
                        Spacer(Modifier.width(MagicSpacing.xs))
                        Text(
                            text = "综合倾向：吉中带阻",
                            fontSize = MagicType.subheadline.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.auspicious,
                        )
                    }
                    Spacer(Modifier.height(MagicSpacing.xs))
                    Text(
                        text = "事可成，但过程反复，宜稳不宜急。建议先稳住现有局面，待入冬火退之后再图推进。",
                        fontSize = MagicType.footnote.sp,
                        color = colors.secondaryText,
                    )
                }
            }

            Spacer(Modifier.height(MagicSpacing.md))
            MagicCaption("本解读由本地规则引擎生成，仅供传统文化研究与娱乐参考。")
        }

        Text(
            text = "术数内容仅供传统文化研究与娱乐，不构成任何决策建议。",
            fontSize = MagicType.legalMin.sp,
            color = colors.tertiaryText,
            modifier = Modifier.padding(vertical = MagicSpacing.row),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 卦盘
// ─────────────────────────────────────────────────────────────

/** 一爻的数据。UI 阶段为静态示例。 */
private data class YaoRowData(
    val position: String,
    val liuShen: String,
    val liuQin: String,
    val ganZhi: String,
    val yang: Boolean,
    val moving: Boolean,
    val shiYing: String?,
    val bianYang: Boolean,
    val bianGanZhi: String,
    val bianLiuQin: String,
    /** 伏神（挂在本爻之下） */
    val fuShen: String? = null,
)

private val DemoYao = listOf(
    YaoRowData("上九", "玄武", "父母", "壬戌土", true, false, null, true, "丁未土", "妻财"),
    YaoRowData("九五", "白虎", "兄弟", "壬申金", true, false, null, true, "丁酉金", "官鬼"),
    YaoRowData("九四", "腾蛇", "官鬼", "壬午火", true, true, "应", false, "丁亥水", "父母"),
    YaoRowData("九三", "勾陈", "兄弟", "辛酉金", true, false, null, true, "辛酉金", "官鬼"),
    YaoRowData("九二", "朱雀", "子孙", "辛亥水", true, false, null, true, "辛亥水", "父母", fuShen = "妻财 甲寅"),
    YaoRowData("初六", "青龙", "父母", "辛丑土", false, false, "世", false, "辛丑土", "妻财"),
)

@Composable
private fun HexagramCard() {
    val colors = LocalMagicColors.current

    GlassCard(level = GlassLevel.Thin) {
        // ── 卦名 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "䷫",
                fontSize = MagicType.display.sp,
                color = colors.primaryText,
            )
            Spacer(Modifier.width(MagicSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "天风姤",
                    fontSize = MagicType.title2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryText,
                )
                Text(
                    text = "乾宫 · 一世 · 世初应四",
                    fontSize = MagicType.footnote.sp,
                    color = colors.tertiaryText,
                )
            }
        }

        Spacer(Modifier.height(MagicSpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
            MagicBadge("九四动", colors.accent)
            MagicBadge("旬空 戌亥", colors.warning)
        }

        Spacer(Modifier.height(MagicSpacing.cardPadding))
        MagicDivider()
        Spacer(Modifier.height(MagicSpacing.md))

        // ── 表头 ──
        Row(modifier = Modifier.fillMaxWidth()) {
            MagicHeaderCell("六神", weight = 0.85f)
            MagicHeaderCell("六亲", weight = 0.9f)
            MagicHeaderCell("世应", weight = 0.55f)
            MagicHeaderCell("本卦", weight = 1.5f)
            MagicHeaderCell("变卦", weight = 1.5f)
        }
        Spacer(Modifier.height(MagicSpacing.xs))
        MagicDivider()
        Spacer(Modifier.height(MagicSpacing.xs))

        // ── 六爻（自上而下：上爻 → 初爻）──
        DemoYao.forEachIndexed { index, yao ->
            YaoTableRow(yao)
            if (index != DemoYao.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.divider)
                )
            }
        }

        Spacer(Modifier.height(MagicSpacing.md))
        MagicCaption("变卦：泽风大过 · 震宫游魂。变卦六亲以变卦自身宫位五行重算。")
    }
}

@Composable
private fun YaoTableRow(yao: YaoRowData) {
    val colors = LocalMagicColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MagicSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 六神
        Text(
            text = yao.liuShen,
            fontSize = MagicType.footnote.sp,
            color = colors.tertiaryText,
            modifier = Modifier.weight(0.85f),
        )

        // 六亲
        Text(
            text = yao.liuQin,
            fontSize = MagicType.footnote.sp,
            fontWeight = FontWeight.Medium,
            color = colors.secondaryText,
            modifier = Modifier.weight(0.9f),
        )

        // 世应
        Box(modifier = Modifier.weight(0.55f)) {
            if (yao.shiYing != null) {
                MagicBadge(
                    text = yao.shiYing,
                    color = if (yao.shiYing == "世") colors.accent else colors.tertiaryText,
                )
            }
        }

        // 本卦
        YaoCell(
            yang = yao.yang,
            ganZhi = yao.ganZhi,
            tint = if (yao.moving) colors.accent else colors.primaryText,
            sub = yao.fuShen,
            subColor = colors.inauspicious,
            modifier = Modifier.weight(1.5f),
        )

        // 变卦
        YaoCell(
            yang = yao.bianYang,
            ganZhi = yao.bianGanZhi,
            tint = colors.secondaryText,
            sub = yao.bianLiuQin,
            subColor = colors.tertiaryText,
            modifier = Modifier.weight(1.5f),
        )
    }
}

/**
 * 单爻单元格：爻线图形 + 干支，可选副行（伏神 / 变卦六亲）。
 *
 * 爻线用 Canvas 绘制而非 Unicode 字符 —— 字符在不同字体下宽度与粗细都不一致，
 * 无法保证多行纵向对齐。
 */
@Composable
private fun YaoCell(
    yang: Boolean,
    ganZhi: String,
    tint: Color,
    sub: String?,
    subColor: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        YaoLine(
            yang = yang,
            color = tint,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        )
        Text(
            text = ganZhi,
            fontSize = MagicType.caption.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
        )
        if (sub != null) {
            Text(
                text = sub,
                // 字号下限 12sp —— 低于此值在玻璃上不可读
                fontSize = MagicType.caption.sp,
                color = subColor,
            )
        }
    }
}

/** 爻线：阳爻一整条，阴爻断开两段 */
@Composable
private fun YaoLine(
    yang: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val h = size.height
        val radius = CornerRadius(h / 2f)
        if (yang) {
            drawRoundRect(
                color = color,
                topLeft = Offset.Zero,
                size = Size(size.width, h),
                cornerRadius = radius,
            )
        } else {
            val gap = size.width * 0.26f
            val segW = (size.width - gap) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset.Zero,
                size = Size(segW, h),
                cornerRadius = radius,
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width - segW, 0f),
                size = Size(segW, h),
                cornerRadius = radius,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 断卦条目
// ─────────────────────────────────────────────────────────────

@Composable
private fun HintItem(title: String, body: String) {
    val colors = LocalMagicColors.current
    Column {
        Text(
            text = title,
            fontSize = MagicType.footnote.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.accent,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = body,
            fontSize = MagicType.footnote.sp,
            color = colors.secondaryText,
        )
    }
}
