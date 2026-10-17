package com.maohongyuan.magic.feature.xiaoliuren

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.core.ai.InterpreterChains
import com.maohongyuan.magic.core.calendar.CalendarService
import com.maohongyuan.magic.core.interpret.Interpretation
import com.maohongyuan.magic.core.interpret.InterpretationRequest
import com.maohongyuan.magic.core.interpret.Tone
import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.xiaoliuren.SixPalace
import com.maohongyuan.magic.core.xiaoliuren.XiaoLiuRenChart
import com.maohongyuan.magic.core.xiaoliuren.XiaoLiuRenDivination
import com.maohongyuan.magic.core.xiaoliuren.XiaoLiuRenMethod
import com.maohongyuan.magic.core.xiaoliuren.XiaoLiuRenRuleEngine
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
 * 小六壬。
 *
 * 六宫循环，按「月 → 日 → 时」逐级顺数落宫，三宫合参。
 *
 * ── 为什么时间起课只能用「今天」──
 * 农历月日要由历法换算，用户手填农历容易填错。本版时间起课直接用今日农历，
 * 想看别的日子请用报数起课（三个数自由输入）。
 * 后续要支持选日期时，复用黄历页那套 MagicDateBar 即可。
 */
@Composable
fun XiaoLiuRenScreen(scrollState: ScrollState) {
    val colors = LocalMagicColors.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // 必须 remember —— 否则每次解卦都重建后端、重新加载 2 GB 模型
    val chain = remember { InterpreterChains.standard(context, XiaoLiuRenRuleEngine()) }

    val today = remember { CalendarService.today() }
    val todayBranch: EarthlyBranch = remember(today) {
        // 时柱的地支就是当前时辰
        today.ganZhi.hour?.branch ?: EarthlyBranch.ZI
    }

    var method by remember { mutableStateOf(XiaoLiuRenMethod.TIME) }
    var n1 by remember { mutableStateOf("") }
    var n2 by remember { mutableStateOf("") }
    var n3 by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var chart by remember { mutableStateOf<XiaoLiuRenChart?>(null) }
    var interpretation by remember { mutableStateOf<Interpretation?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun cast() {
        error = null
        val result = runCatching {
            when (method) {
                XiaoLiuRenMethod.TIME -> XiaoLiuRenDivination.byTime(
                    lunarMonth = today.lunar.month,
                    lunarDay = today.lunar.day,
                    hourBranch = todayBranch,
                    question = question,
                )
                XiaoLiuRenMethod.NUMBER -> XiaoLiuRenDivination.byNumbers(
                    first = n1.toIntOrNull() ?: error("第一个数请填正整数"),
                    second = n2.toIntOrNull() ?: error("第二个数请填正整数"),
                    third = n3.toIntOrNull() ?: error("第三个数请填正整数"),
                    question = question,
                )
            }
        }.getOrElse {
            error = it.message ?: "起课失败"
            return
        }
        chart = result
        interpretation = null
        error = null
        scope.launch {
            val outcome = runCatching {
                chain.interpret(InterpretationRequest(question = question, chart = result))
            }
            interpretation = outcome.getOrNull()
            error = outcome.exceptionOrNull()?.message
        }
    }


    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(title = "小六壬", subtitle = "六宫速断 · 月日时三级落宫")

        // ── 起课 ──
        GlassCard {
            MagicSectionTitle("起课")
            Spacer(Modifier.height(MagicSpacing.md))

            MagicSegmentedControl(
                options = XiaoLiuRenMethod.entries.map { it.label },
                selectedIndex = XiaoLiuRenMethod.entries.indexOf(method),
                onSelect = { method = XiaoLiuRenMethod.entries[it] },
            )

            Spacer(Modifier.height(MagicSpacing.md))

            when (method) {
                XiaoLiuRenMethod.TIME -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                        InfoChip("农历", "${today.lunar.monthLabel}${today.lunar.dayLabel}")
                        InfoChip("时辰", "${todayBranch.label}时")
                    }
                    Spacer(Modifier.height(MagicSpacing.sm))
                    MagicCaption("用今日的农历月日与当前时辰起课。想算别的日子请改用报数起课。")
                }

                XiaoLiuRenMethod.NUMBER -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                        NumField("第一个数", n1, { n1 = it }, Modifier.weight(1f))
                        NumField("第二个数", n2, { n2 = it }, Modifier.weight(1f))
                        NumField("第三个数", n3, { n3 = it }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(MagicSpacing.sm))
                    MagicCaption("三个数按「自上一落宫继续数」的规则逐级落宫，不是各自从大安起。")
                }
            }

            Spacer(Modifier.height(MagicSpacing.md))
            MagicTextField(
                value = question,
                onValueChange = { question = it },
                placeholder = "所问何事（可留空）",
            )

            if (error != null) {
                Spacer(Modifier.height(MagicSpacing.md))
                Text(
                    text = "⚠ $error",
                    fontSize = MagicType.footnote.sp,
                    color = colors.warning,
                )
            }

            Spacer(Modifier.height(MagicSpacing.cardPadding))
            MagicSolidButton(onClick = ::cast, selected = true, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "起课",
                    fontSize = MagicType.body.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent,
                )
            }
        }

        // ── 三宫 ──
        chart?.let { c ->
            GlassCard(level = GlassLevel.Thin) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MagicSectionTitle("三宫", modifier = Modifier.weight(1f))
                    MagicBadge(c.overallVerdict, colors.accent)
                }
                Spacer(Modifier.height(MagicSpacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row),
                ) {
                    PalaceCell(stageLabel(c, 0), c.first, Modifier.weight(1f))
                    PalaceCell(stageLabel(c, 1), c.second, Modifier.weight(1f))
                    PalaceCell(stageLabel(c, 2), c.third, Modifier.weight(1f))
                }

                Spacer(Modifier.height(MagicSpacing.md))
                MagicDivider()
                Spacer(Modifier.height(MagicSpacing.md))
                MagicCaption(
                    "结果看时宫（第三落点），过程看日宫，起因看月宫。" +
                        "三宫五行：${c.first.wuXing} · ${c.second.wuXing} · ${c.third.wuXing}"
                )
            }
        }

        // ── 断语 ──
        interpretation?.let { it ->
            GlassCard(level = GlassLevel.Thin) {
                MagicSectionTitle("断语")
                Spacer(Modifier.height(MagicSpacing.xs))
                Text(
                    text = it.headline,
                    fontSize = MagicType.subheadline.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryText,
                )
                Spacer(Modifier.height(MagicSpacing.md))

                it.sections.forEachIndexed { index, section ->
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
            text = "术数内容仅供传统文化研究与娱乐，不构成任何决策建议。",
            fontSize = MagicType.legalMin.sp,
            color = colors.tertiaryText,
            modifier = Modifier.padding(vertical = MagicSpacing.row),
        )
    }
}

private fun stageLabel(chart: XiaoLiuRenChart, index: Int): String = when (chart.method) {
    XiaoLiuRenMethod.TIME -> listOf("月宫", "日宫", "时宫")[index]
    XiaoLiuRenMethod.NUMBER -> listOf("第一数", "第二数", "第三数")[index]
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

@Composable
private fun InfoChip(label: String, value: String) {
    val colors = LocalMagicColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
        Spacer(Modifier.height(0.dp))
        Text(
            "  $value",
            fontSize = MagicType.footnote.sp,
            fontWeight = FontWeight.Medium,
            color = colors.primaryText,
        )
    }
}

@Composable
private fun PalaceCell(stage: String, palace: SixPalace, modifier: Modifier = Modifier) {
    val colors = LocalMagicColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stage, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
        Spacer(Modifier.height(MagicSpacing.xs))
        Text(
            text = palace.name,
            fontSize = MagicType.title3.sp,
            fontWeight = FontWeight.Bold,
            color = if (palace.verdict == "吉") colors.auspicious else colors.inauspicious,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${palace.wuXing} · ${palace.liuShen}",
            fontSize = MagicType.caption.sp,
            color = colors.tertiaryText,
        )
    }
}

@Composable
private fun NumField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
        Spacer(Modifier.height(MagicSpacing.xs))
        MagicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(3)) },
            placeholder = "数",
        )
    }
}
