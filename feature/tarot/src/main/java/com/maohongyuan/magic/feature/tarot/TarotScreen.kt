package com.maohongyuan.magic.feature.tarot

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.core.interpret.Interpretation
import com.maohongyuan.magic.core.interpret.InterpretationRequest
import com.maohongyuan.magic.core.interpret.Tone
import com.maohongyuan.magic.core.tarot.DrawnCard
import com.maohongyuan.magic.core.tarot.TarotChart
import com.maohongyuan.magic.core.tarot.TarotDivination
import com.maohongyuan.magic.core.tarot.TarotRuleEngine
import com.maohongyuan.magic.core.tarot.TarotSpread
import com.maohongyuan.magic.designsystem.glass.GlassCard
import com.maohongyuan.magic.designsystem.glass.GlassLevel
import com.maohongyuan.magic.designsystem.glass.MagicBadge
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicDivider
import com.maohongyuan.magic.designsystem.glass.MagicPageContent
import com.maohongyuan.magic.designsystem.glass.MagicPageHeader
import com.maohongyuan.magic.designsystem.glass.MagicSectionTitle
import com.maohongyuan.magic.designsystem.glass.MagicSegmentedControl
import com.maohongyuan.magic.designsystem.glass.MagicSolidButton
import com.maohongyuan.magic.designsystem.glass.MagicTextField
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * 塔罗。
 *
 * 牌义数据译自 A. E. Waite《The Pictorial Key to the Tarot》(1911)，
 * 该著作已进入公有领域。不取自任何现代出版物。
 */
@Composable
fun TarotScreen(scrollState: ScrollState) {
    val colors = LocalMagicColors.current
    val scope = rememberCoroutineScope()

    var spread by remember { mutableStateOf(TarotSpread.THREE) }
    var question by remember { mutableStateOf("") }
    var chart by remember { mutableStateOf<TarotChart?>(null) }
    var interpretation by remember { mutableStateOf<Interpretation?>(null) }

    fun draw() {
        val c = TarotDivination.draw(spread, question)
        chart = c
        interpretation = null
        scope.launch {
            interpretation = TarotRuleEngine()
                .interpret(InterpretationRequest(question = question, chart = c))
                .firstOrNull()
        }
    }

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(title = "塔罗", subtitle = "韦特体系 · 78 张")

        // ── 抽牌 ──
        GlassCard {
            MagicSectionTitle("牌阵")
            Spacer(Modifier.height(MagicSpacing.md))

            MagicSegmentedControl(
                options = TarotSpread.entries.map { it.label },
                selectedIndex = TarotSpread.entries.indexOf(spread),
                onSelect = { spread = TarotSpread.entries[it] },
            )
            Spacer(Modifier.height(MagicSpacing.xs))
            MagicCaption("${spread.size} 张：" + spread.positions.joinToString(" / "))

            Spacer(Modifier.height(MagicSpacing.md))
            MagicTextField(
                value = question,
                onValueChange = { question = it },
                placeholder = "所问何事（可留空）",
            )

            Spacer(Modifier.height(MagicSpacing.cardPadding))
            MagicSolidButton(onClick = ::draw, selected = true, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "抽牌",
                    fontSize = MagicType.body.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent,
                )
            }
        }

        // ── 牌面 ──
        chart?.let { c ->
            GlassCard(level = GlassLevel.Thin) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MagicSectionTitle("牌面", modifier = Modifier.weight(1f))
                    MagicBadge("${c.drawn.size} 张 · 逆位 ${c.reversedCount}", colors.accent)
                }
                Spacer(Modifier.height(MagicSpacing.md))

                c.drawn.forEachIndexed { index, d ->
                    if (index > 0) {
                        Spacer(Modifier.height(MagicSpacing.md))
                        MagicDivider()
                        Spacer(Modifier.height(MagicSpacing.md))
                    }
                    CardBlock(d)
                }

                Spacer(Modifier.height(MagicSpacing.md))
                MagicCaption(
                    "牌义译自 A. E. Waite《The Pictorial Key to the Tarot》(1911)，已进入公有领域。"
                )
            }
        }

        // ── 解读 ──
        interpretation?.let { it ->
            GlassCard(level = GlassLevel.Thin) {
                MagicSectionTitle("解读")
                Spacer(Modifier.height(MagicSpacing.xs))
                Text(
                    text = it.headline,
                    fontSize = MagicType.subheadline.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryText,
                )
                Spacer(Modifier.height(MagicSpacing.md))

                // 逐张牌已经在「牌面」里给过了，这里只显示整体性的段落
                val overview = it.sections.filterNot { s ->
                    chart?.drawn?.any { d -> s.title.contains(d.positionName) } == true
                }
                overview.forEachIndexed { index, section ->
                    if (index > 0) {
                        Spacer(Modifier.height(MagicSpacing.md))
                        MagicDivider()
                        Spacer(Modifier.height(MagicSpacing.md))
                    }
                    Text(
                        text = section.title,
                        fontSize = MagicType.footnote.sp,
                        fontWeight = FontWeight.Medium,
                        color = toneColor(section.tone, colors),
                    )
                    Spacer(Modifier.height(MagicSpacing.xs))
                    Text(
                        text = section.body,
                        fontSize = MagicType.footnote.sp,
                        color = colors.secondaryText,
                    )
                }
            }
        }

        Text(
            text = "塔罗用于自我观照，不构成任何决策建议。",
            fontSize = MagicType.legalMin.sp,
            color = colors.tertiaryText,
            modifier = Modifier.padding(vertical = MagicSpacing.row),
        )
    }
}

@Composable
private fun CardBlock(d: DrawnCard) {
    val colors = LocalMagicColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = d.positionName,
                fontSize = MagicType.caption.sp,
                color = colors.tertiaryText,
                modifier = Modifier.weight(1f),
            )
            // 正逆位用字形 + 颜色双通道，不只靠颜色
            Text(
                text = if (d.reversed) "▽ 逆位" else "△ 正位",
                fontSize = MagicType.caption.sp,
                fontWeight = FontWeight.Medium,
                color = if (d.reversed) colors.warning else colors.auspicious,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = d.card.name,
            fontSize = MagicType.title3.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primaryText,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = d.card.keywords.joinToString(" · "),
            fontSize = MagicType.caption.sp,
            color = colors.accent,
        )
        Spacer(Modifier.height(MagicSpacing.xs))
        Text(
            text = d.meaning,
            fontSize = MagicType.footnote.sp,
            color = colors.secondaryText,
        )
    }
}

private fun toneColor(
    tone: Tone,
    colors: com.maohongyuan.magic.designsystem.theme.MagicColors,
) = when (tone) {
    Tone.POSITIVE -> colors.auspicious
    Tone.NEGATIVE -> colors.inauspicious
    Tone.WARNING -> colors.warning
    Tone.NEUTRAL -> colors.tertiaryText
}
