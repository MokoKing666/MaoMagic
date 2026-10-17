package com.maohongyuan.magic.feature.liuyao

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maohongyuan.magic.core.data.DivinationRecord
import com.maohongyuan.magic.core.hexagram.CoinThrow
import com.maohongyuan.magic.core.hexagram.DivinationMethod
import com.maohongyuan.magic.core.hexagram.LiuYaoChart
import com.maohongyuan.magic.core.hexagram.LiuYaoLine
import com.maohongyuan.magic.core.hexagram.YaoRelation
import com.maohongyuan.magic.core.interpret.EngineTier
import com.maohongyuan.magic.core.interpret.Interpretation
import com.maohongyuan.magic.core.interpret.Tone
import com.maohongyuan.magic.core.interpret.Verdict
import com.maohongyuan.magic.core.model.Hexagram
import com.maohongyuan.magic.core.model.YaoPosition
import com.maohongyuan.magic.designsystem.glass.GlassCard
import com.maohongyuan.magic.designsystem.glass.GlassLevel
import com.maohongyuan.magic.designsystem.glass.MagicBadge
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicDivider
import com.maohongyuan.magic.designsystem.glass.MagicHeaderCell
import com.maohongyuan.magic.designsystem.glass.MagicInfoRow
import com.maohongyuan.magic.designsystem.glass.MagicPageContent
import com.maohongyuan.magic.designsystem.glass.MagicPageHeader
import com.maohongyuan.magic.designsystem.glass.MagicSectionTitle
import com.maohongyuan.magic.designsystem.glass.MagicSegmentedControl
import com.maohongyuan.magic.designsystem.glass.MagicSolidButton
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.MagicSemanticMark
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

private val Methods = DivinationMethod.entries

/**
 * 六爻起卦解卦。
 *
 * 数据流：起卦 → 装卦 → 解卦（Tier1 规则引擎，走降级链）。
 */
@Composable
fun LiuyaoScreen(scrollState: ScrollState) {
    val colors = LocalMagicColors.current
    val vm: LiuYaoViewModel = viewModel()
    val state = vm.state

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(
            title = "六爻起卦",
            subtitle = "铜钱 · 时间 · 数字 · 手动",
        )

        GlassCard {
            MagicSectionTitle("所问何事")
            Spacer(Modifier.height(MagicSpacing.md))
            QuestionInput(value = state.question, onValueChange = vm::setQuestion)
            Spacer(Modifier.height(MagicSpacing.md))
            MagicCaption("所问越具体，断卦越有针对性。一事一卦。")
        }

        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("起卦方式")
            Spacer(Modifier.height(MagicSpacing.md))
            MagicSegmentedControl(
                options = Methods.map { it.label },
                selectedIndex = Methods.indexOf(state.method),
                onSelect = { vm.selectMethod(Methods[it]) },
            )
            Spacer(Modifier.height(MagicSpacing.cardPadding))

            when (state.method) {
                DivinationMethod.COIN -> CoinPanel(state.throws, vm::castByCoin)
                DivinationMethod.TIME -> TimePanel(state, vm::castByTime)
                DivinationMethod.NUMBER -> NumberPanel(
                    a = state.numberA,
                    b = state.numberB,
                    onA = vm::setNumberA,
                    onB = vm::setNumberB,
                    onCast = vm::castByNumber,
                    enabled = state.canCast,
                )
                DivinationMethod.MANUAL -> ManualPanel(
                    lines = state.manualLines,
                    moving = state.manualMoving,
                    onToggleLine = vm::toggleManualLine,
                    onToggleMoving = vm::toggleManualMoving,
                    onCast = vm::castByManual,
                )
            }

            Spacer(Modifier.height(MagicSpacing.md))
            if (state.result != null) {
                MagicSolidButton(onClick = vm::reset, modifier = Modifier.fillMaxWidth()) {
                    Text("重新起卦", fontSize = MagicType.footnote.sp, color = colors.accent)
                }
            } else {
                MagicSolidButton(onClick = vm::loadDemo, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "载入示例卦（天风姤 · 九四动）",
                        fontSize = MagicType.footnote.sp,
                        color = colors.secondaryText,
                    )
                }
            }
        }

        state.chart?.let { chart ->
            HexagramCard(chart)
            CourseCard(chart)
        }

        state.interpretation?.let { InterpretationCard(it) }

        if (state.history.isNotEmpty()) {
            HistoryCard(
                records = state.history,
                onDelete = vm::deleteRecord,
                onClear = vm::clearHistory,
            )
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
// 输入
// ─────────────────────────────────────────────────────────────

@Composable
private fun inputBackground(): Color {
    val colors = LocalMagicColors.current
    return if (colors.isLight) Color.White.copy(alpha = 0.72f) else Color.Black.copy(alpha = 0.40f)
}

@Composable
private fun QuestionInput(value: String, onValueChange: (String) -> Unit) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(inputBackground(), shapes.controlInCard)
            .padding(MagicSpacing.md),
    ) {
        if (value.isEmpty()) {
            Text(
                text = "例：今年事业运势如何？",
                fontSize = MagicType.subheadline.sp,
                color = colors.tertiaryText,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = MagicType.subheadline.sp, color = colors.primaryText),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NumberInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    Column(modifier = modifier) {
        Text(text = label, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
        Spacer(Modifier.height(MagicSpacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(inputBackground(), shapes.controlInCard)
                .padding(MagicSpacing.md),
        ) {
            if (value.isEmpty()) {
                Text("0", fontSize = MagicType.subheadline.sp, color = colors.tertiaryText)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = MagicType.subheadline.sp, color = colors.primaryText),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 四种起卦操作区
// ─────────────────────────────────────────────────────────────

@Composable
private fun castPanel(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.accent.copy(alpha = 0.06f), shapes.controlInCard)
            .padding(MagicSpacing.cardPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun CoinPanel(throws: List<CoinThrow>, onCast: () -> Unit) {
    val colors = LocalMagicColors.current
    castPanel {
        Text(text = "☯", fontSize = MagicType.display.sp, color = colors.accent)
        Spacer(Modifier.height(MagicSpacing.row))
        Text("三枚铜钱，摇六次成卦", fontSize = MagicType.subheadline.sp, color = colors.secondaryText)
        Spacer(Modifier.height(MagicSpacing.xs))
        Text(
            text = "正面（字）记 3、反面（背）记 2；6 老阴、7 少阳、8 少阴、9 老阳",
            fontSize = MagicType.caption.sp,
            color = colors.tertiaryText,
        )

        if (throws.isNotEmpty()) {
            Spacer(Modifier.height(MagicSpacing.cardPadding))
            throws.reversed().forEachIndexed { displayIndex, t ->
                val index = throws.lastIndex - displayIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = YaoPosition.of(index).ordinalLabel,
                        fontSize = MagicType.caption.sp,
                        color = colors.tertiaryText,
                        modifier = Modifier.width(24.dp),
                    )
                    Text(
                        text = t.coins.joinToString(" "),
                        fontSize = MagicType.caption.sp,
                        color = colors.secondaryText,
                        modifier = Modifier.width(72.dp),
                    )
                    Text(
                        text = "${t.sum} ${t.label}",
                        fontSize = MagicType.caption.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (t.moving) colors.accent else colors.secondaryText,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(MagicSpacing.cardPadding))
        MagicSolidButton(onClick = onCast, selected = true, modifier = Modifier.fillMaxWidth()) {
            Text("摇卦", fontSize = MagicType.body.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
        }
    }
}

@Composable
private fun TimePanel(state: LiuYaoUiState, onCast: () -> Unit) {
    val colors = LocalMagicColors.current
    castPanel {
        Text(text = "◷", fontSize = MagicType.display.sp, color = colors.accent)
        Spacer(Modifier.height(MagicSpacing.row))
        Text(
            text = state.day?.let {
                "${it.solar.ymd} " + "%02d:%02d".format(it.solar.hour, it.solar.minute)
            } ?: "读取时间中…",
            fontSize = MagicType.subheadline.sp,
            fontWeight = FontWeight.Medium,
            color = colors.secondaryText,
        )
        Spacer(Modifier.height(MagicSpacing.xs))
        Text(
            text = state.day?.let {
                "农历${it.lunar.monthLabel}${it.lunar.dayLabel} · ${it.lunar.yearGanZhi}年"
            } ?: "",
            fontSize = MagicType.caption.sp,
            color = colors.tertiaryText,
        )
        Spacer(Modifier.height(MagicSpacing.row))
        Text(
            text = "年支序 + 月 + 日 定上卦，再加时支序 定下卦，总数除 6 取动爻",
            fontSize = MagicType.caption.sp,
            color = colors.tertiaryText,
        )
        Spacer(Modifier.height(MagicSpacing.cardPadding))
        MagicSolidButton(onClick = onCast, selected = true, modifier = Modifier.fillMaxWidth()) {
            Text("起卦", fontSize = MagicType.body.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
        }
    }
}

@Composable
private fun NumberPanel(
    a: String,
    b: String,
    onA: (String) -> Unit,
    onB: (String) -> Unit,
    onCast: () -> Unit,
    enabled: Boolean,
) {
    val colors = LocalMagicColors.current
    castPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
            NumberInput("数一（定上卦）", a, onA, Modifier.weight(1f))
            NumberInput("数二（定下卦）", b, onB, Modifier.weight(1f))
        }
        Spacer(Modifier.height(MagicSpacing.md))
        Text(
            text = "上卦 = 数一 ÷ 8 取余，下卦 = 数二 ÷ 8 取余，动爻 = 两数之和 ÷ 6 取余",
            fontSize = MagicType.caption.sp,
            color = colors.tertiaryText,
        )
        Spacer(Modifier.height(MagicSpacing.cardPadding))
        MagicSolidButton(
            onClick = onCast,
            enabled = enabled,
            selected = true,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("起卦", fontSize = MagicType.body.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
        }
    }
}

@Composable
private fun ManualPanel(
    lines: List<Boolean>,
    moving: Set<Int>,
    onToggleLine: (Int) -> Unit,
    onToggleMoving: (Int) -> Unit,
    onCast: () -> Unit,
) {
    val colors = LocalMagicColors.current
    castPanel {
        Text(
            text = "点爻线切换阴阳，点「动」标记动爻",
            fontSize = MagicType.caption.sp,
            color = colors.tertiaryText,
        )
        Spacer(Modifier.height(MagicSpacing.md))

        (5 downTo 0).forEach { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleLine(index) }
                    .padding(vertical = MagicSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = YaoPosition.of(index).ordinalLabel,
                    fontSize = MagicType.footnote.sp,
                    color = colors.tertiaryText,
                    modifier = Modifier.width(24.dp),
                )
                YaoLine(
                    yang = lines[index],
                    color = colors.primaryText,
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                )
                Spacer(Modifier.width(MagicSpacing.md))
                Box(
                    modifier = Modifier
                        .clickable { onToggleMoving(index) }
                        .padding(horizontal = MagicSpacing.xs, vertical = 2.dp),
                ) {
                    Text(
                        text = "动",
                        fontSize = MagicType.caption.sp,
                        fontWeight = if (index in moving) FontWeight.Bold else FontWeight.Normal,
                        color = if (index in moving) colors.accent else colors.tertiaryText,
                    )
                }
            }
        }

        Spacer(Modifier.height(MagicSpacing.cardPadding))
        MagicSolidButton(onClick = onCast, selected = true, modifier = Modifier.fillMaxWidth()) {
            Text("成卦", fontSize = MagicType.body.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 卦盘
// ─────────────────────────────────────────────────────────────

@Composable
private fun HexagramCard(chart: LiuYaoChart) {
    val colors = LocalMagicColors.current

    GlassCard(level = GlassLevel.Thin) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = chart.hexagram.symbol, fontSize = MagicType.display.sp, color = colors.primaryText)
            Spacer(Modifier.width(MagicSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chart.hexagram.name,
                    fontSize = MagicType.title2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryText,
                )
                Text(text = chart.palace.summary, fontSize = MagicType.footnote.sp, color = colors.tertiaryText)
            }
        }

        if (chart.movingIndexes.isNotEmpty()) {
            Spacer(Modifier.height(MagicSpacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                MagicBadge(
                    chart.movingIndexes.joinToString("、") { chart.lines[it].title } + " 动",
                    colors.accent,
                )
            }
        }

        Spacer(Modifier.height(MagicSpacing.cardPadding))
        MagicDivider()
        Spacer(Modifier.height(MagicSpacing.md))

        // ── 顶部信息表 ──
        //
        // 参照通行六爻排盘：起卦的基本条件要先摆出来，
        // 断卦时月建、日辰、旬空是判断旺衰生克的基准，不能埋在正文里。
        //
        // 缺的两项（节气、神煞）当前 LiuYaoChart 里没有字段，未做。
        InfoRow("事项", chart.question.ifBlank { "未填写" })
        InfoRow("月建", "${chart.monthBranch.label}月")
        InfoRow("日辰", "${chart.dayGanZhi}　旬空 ${chart.xunKong.first.label}${chart.xunKong.second.label}")
        InfoRow("卦式", chart.palace.summary)
        Spacer(Modifier.height(MagicSpacing.cardPadding))
        MagicDivider()
        Spacer(Modifier.height(MagicSpacing.md))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // ⚠️ 这里**不能**写 MagicHeaderCell("六神", weight = 0f) ——
            // Compose 的 Modifier.weight 要求 weight > 0，传 0f 会在组合时抛
            // IllegalArgumentException，表现就是「卦盘一出现就闪退」。
            // 定宽列直接用 Text + width，不要走 weight。
            Text(
                text = "六神",
                fontSize = MagicType.caption.sp,
                color = colors.tertiaryText,
                modifier = Modifier.width(YaoWidth.SHEN),
            )
            MagicHeaderCell("本卦", weight = 1f)
            Spacer(Modifier.width(YaoWidth.MARK))
            Spacer(Modifier.width(YaoWidth.CHANGE))
            Spacer(Modifier.width(YaoWidth.MARK))
            MagicHeaderCell("变卦", weight = 1f)
        }
        Spacer(Modifier.height(MagicSpacing.xs))
        MagicDivider()
        Spacer(Modifier.height(MagicSpacing.xs))

        chart.lines.reversed().forEachIndexed { displayIndex, line ->
            YaoTableRow(line)
            if (displayIndex != chart.lines.lastIndex) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
            }
        }

        Spacer(Modifier.height(MagicSpacing.md))
        MagicCaption("世应、六亲、六神、旬空均由算法推出；伏神取本宫首卦同爻位。")
    }
}

/** 信息表的一行：左侧标签定宽，右侧内容可换行。 */
@Composable
private fun InfoRow(label: String, value: String) {
    val colors = LocalMagicColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = label,
            fontSize = MagicType.footnote.sp,
            color = colors.tertiaryText,
            modifier = Modifier.width(56.dp),
        )
        Text(
            text = value,
            fontSize = MagicType.footnote.sp,
            color = colors.primaryText,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 卦盘表的列定义 —— 表头与数据行共用同一份，避免改一处漏一处。 */
private object YaoCols {
    const val SHEN = 0.85f
    const val QIN = 0.9f
    const val SHI = 0.55f
    const val GUA = 1.4f
    const val BIAN = 1.4f
    val GAP = MagicSpacing.md
}

// 单元格固定三槽：没有内容也占位，保证六行爻线落在同一组水平线上
private val YaoLineSlot = 22.dp
private val YaoTextSlot = 22.dp
private val YaoSubSlot = 20.dp

@Composable
private fun YaoTableRow(line: LiuYaoLine) {
    val colors = LocalMagicColors.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MagicSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── 六神 ──
        Text(
            text = line.liuShen.label,
            fontSize = MagicType.footnote.sp,
            color = if (line.moving) colors.accent else colors.secondaryText,
            modifier = Modifier.width(YaoWidth.SHEN),
        )

        // ── 本卦侧：六亲 + 地支 + 五行，换行放干支与世应 ──
        //
        // 参照通行排盘的「官午火 丙 · 世」：六亲定是谁、地支定哪一爻、
        // 五行定生克，三者必须同一视觉单元，否则断卦要来回对照爻位。
        // 世应压在这一格末尾，不单独占列 —— 独立列会让整行被切得很碎。
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.liuQin.label + line.naJia.branch.label + line.naJia.wuXing.label,
                fontSize = MagicType.footnote.sp,
                fontWeight = FontWeight.Medium,
                color = if (line.kong) colors.tertiaryText else colors.secondaryText,
                maxLines = 1,
            )
            Text(
                text = buildString {
                    append(line.naJia.label)
                    when (line.relation) {
                        YaoRelation.SHI -> append(" · 世")
                        YaoRelation.YING -> append(" · 应")
                        YaoRelation.NONE -> Unit
                    }
                },
                fontSize = MagicType.caption.sp,
                fontWeight = if (line.relation == YaoRelation.NONE) FontWeight.Normal else FontWeight.SemiBold,
                color = if (line.relation == YaoRelation.NONE) colors.tertiaryText else colors.accent,
                maxLines = 1,
            )
            line.fuShen?.let { fs ->
                Text(
                    text = fs.label,
                    fontSize = MagicType.caption.sp,
                    color = colors.inauspicious,
                    maxLines = 1,
                )
            }
        }

        // ── 本卦爻符 ──
        YaoMark(
            yang = line.yang,
            tint = if (line.moving) colors.accent else colors.primaryText,
            modifier = Modifier.width(YaoWidth.MARK),
        )

        // ── 动爻标记：老阳 ○ / 老阴 × ──
        Text(
            text = if (line.moving) (if (line.yang) "○" else "×") else "",
            fontSize = MagicType.caption.sp,
            color = colors.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(YaoWidth.CHANGE),
        )

        // ── 变卦爻符：只有动爻才变，静爻与本站相同 ──
        YaoMark(
            yang = line.bian?.let { !line.yang } ?: line.yang,
            tint = if (line.bian != null) colors.secondaryText else colors.divider,
            modifier = Modifier.width(YaoWidth.MARK),
        )

        // ── 变卦侧：变出的六亲 + 地支 + 五行 ──
        //
        // 只有动爻有内容；静爻留空占位，保证六行爻线对齐。
        Column(modifier = Modifier.weight(1f)) {
            line.bian?.let { b ->
                Text(
                    text = b.liuQin.label + b.naJia.branch.label + b.naJia.wuXing.label,
                    fontSize = MagicType.footnote.sp,
                    color = colors.secondaryText,
                    maxLines = 1,
                )
                Text(
                    text = b.naJia.label,
                    fontSize = MagicType.caption.sp,
                    color = colors.tertiaryText,
                    maxLines = 1,
                )
            } ?: run {
                // 静爻留空占位，保证六行爻符对齐
                Text(text = " ", fontSize = MagicType.footnote.sp)
                Text(text = " ", fontSize = MagicType.caption.sp)
            }
        }
    }
}

/** 卦盘的固定列宽。文本列用 weight，这几列必须定宽，否则六行爻符对不齐。 */
private object YaoWidth {
    val SHEN = 34.dp
    val MARK = 40.dp
    val CHANGE = 18.dp
}

/** 爻符：阳爻一整条，阴爻断开两段 */
@Composable
private fun YaoMark(yang: Boolean, tint: Color, modifier: Modifier = Modifier) {
    YaoLine(yang = yang, color = tint, modifier = modifier.height(6.dp))
}

/** 爻线：阳爻一整条，阴爻断开两段 */
@Composable
private fun YaoLine(yang: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val h = size.height
        val radius = CornerRadius(h / 2f)
        if (yang) {
            drawRoundRect(color, Offset.Zero, Size(size.width, h), radius)
        } else {
            val gap = size.width * 0.26f
            val segW = (size.width - gap) / 2f
            drawRoundRect(color, Offset.Zero, Size(segW, h), radius)
            drawRoundRect(color, Offset(size.width - segW, 0f), Size(segW, h), radius)
        }
    }
}

/** 课盘 —— 月建、日辰、旬空 */
@Composable
private fun CourseCard(chart: LiuYaoChart) {
    val colors = LocalMagicColors.current
    GlassCard(level = GlassLevel.Thin) {
        MagicSectionTitle("课盘")
        Spacer(Modifier.height(MagicSpacing.md))
        Column {
            MagicInfoRow("月建", "${chart.monthBranch.label}月 · ${chart.monthBranch.wuXing}旺")
            MagicInfoRow("日辰", "${chart.dayGanZhi}日 · ${chart.dayGanZhi.branch.wuXing}")
            MagicInfoRow("旬空", chart.xunKong.label)
            MagicInfoRow(
                "世应",
                "世${chart.shiLine.position.ordinalLabel}应${chart.yingLine.position.ordinalLabel}",
            )
            if (chart.fuShenList.isNotEmpty()) {
                MagicInfoRow(
                    label = "伏神",
                    value = chart.fuShenList.joinToString("、") { it.label },
                    valueColor = colors.inauspicious,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 解卦
// ─────────────────────────────────────────────────────────────

@Composable
private fun InterpretationCard(interpretation: Interpretation) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current

    val verdictColor = when (interpretation.verdict) {
        Verdict.AUSPICIOUS -> colors.auspicious
        Verdict.INAUSPICIOUS -> colors.inauspicious
        Verdict.NEUTRAL -> colors.secondaryText
        Verdict.MIXED -> colors.warning
    }
    val mark = when (interpretation.verdict) {
        Verdict.AUSPICIOUS -> MagicSemanticMark.AUSPICIOUS
        Verdict.INAUSPICIOUS -> MagicSemanticMark.INAUSPICIOUS
        Verdict.MIXED -> MagicSemanticMark.WARNING
        Verdict.NEUTRAL -> null
    }

    GlassCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MagicSectionTitle("断卦", modifier = Modifier.weight(1f))
            MagicBadge(interpretation.tier.label, colors.accent)
        }
        Spacer(Modifier.height(MagicSpacing.md))
        MagicDivider()
        Spacer(Modifier.height(MagicSpacing.md))

        Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)) {
            interpretation.sections.forEach { section ->
                Column {
                    Text(
                        text = section.title,
                        fontSize = MagicType.footnote.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (section.tone) {
                            Tone.POSITIVE -> colors.auspicious
                            Tone.NEGATIVE -> colors.inauspicious
                            Tone.WARNING -> colors.warning
                            Tone.NEUTRAL -> colors.accent
                        },
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = section.body,
                        fontSize = MagicType.footnote.sp,
                        color = colors.secondaryText,
                    )
                }
            }
        }

        // 综合结论 —— 颜色 + 字形双通道
        Spacer(Modifier.height(MagicSpacing.cardPadding))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(verdictColor.copy(alpha = 0.10f), shapes.controlInCard)
                .padding(MagicSpacing.md),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mark != null) {
                        Text(
                            text = mark.glyph,
                            fontSize = MagicType.subheadline.sp,
                            fontWeight = FontWeight.Bold,
                            color = verdictColor,
                        )
                        Spacer(Modifier.width(MagicSpacing.xs))
                    }
                    Text(
                        text = "${mark?.label ?: "平"} · ${interpretation.headline}",
                        fontSize = MagicType.subheadline.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = verdictColor,
                    )
                }
                if (interpretation.yongShenNote != null) {
                    Spacer(Modifier.height(MagicSpacing.xs))
                    Text(
                        text = "用神：${interpretation.yongShenNote}",
                        fontSize = MagicType.caption.sp,
                        color = colors.tertiaryText,
                    )
                }
            }
        }

        Spacer(Modifier.height(MagicSpacing.md))
        MagicCaption(
            "本解读由「${interpretation.tier.label}」生成（${interpretation.tier.description}）。" +
                if (interpretation.tier == EngineTier.RULE) "结果完全基于卦象规则推导，可复现。" else ""
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 历史卦例
// ─────────────────────────────────────────────────────────────

/**
 * 最近卦例。
 *
 * 只展示，**不做「载入重算」** —— 记录里的日辰是当时的，用今日日辰重装会得出不同结论，
 * 那反而是误导。要看当时的断语，记录里已存了吉凶与结论。
 */
@Composable
private fun HistoryCard(
    records: List<DivinationRecord>,
    onDelete: (String) -> Unit,
    onClear: () -> Unit,
) {
    val colors = LocalMagicColors.current
    val shown = records.take(20)
    // 待确认删除的记录 id。null 表示没有待确认项。
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    GlassCard(level = GlassLevel.Thin) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MagicSectionTitle("最近卦例", modifier = Modifier.weight(1f))
            Text(
                text = "清空",
                fontSize = MagicType.footnote.sp,
                color = colors.tertiaryText,
                modifier = Modifier
                    .clickable(onClick = onClear)
                    .padding(horizontal = MagicSpacing.xs, vertical = 2.dp),
            )
        }
        Spacer(Modifier.height(MagicSpacing.md))

        shown.forEachIndexed { index, record ->
            val hex = Hexagram.of(record.hexagramCode)
            val verdictColor = when (record.verdict) {
                Verdict.AUSPICIOUS.label -> colors.auspicious
                Verdict.INAUSPICIOUS.label -> colors.inauspicious
                Verdict.MIXED.label -> colors.warning
                else -> colors.secondaryText
            }
            val mark = when (record.verdict) {
                Verdict.AUSPICIOUS.label -> MagicSemanticMark.AUSPICIOUS
                Verdict.INAUSPICIOUS.label -> MagicSemanticMark.INAUSPICIOUS
                Verdict.MIXED.label -> MagicSemanticMark.WARNING
                else -> null
            }

            // ⚠️ 原来这里是 `.clickable { onDelete(record.id) }` —— **单击即永久删除**，
            // 没有二次确认，也没有「查看」入口。卦例是用户攒下来的东西，
            // 一次误触就没了（本轮开发中就因此误删过一条）。
            //
            // 改成两段式：单击选中 → 行内出现「删除 / 取消」，再点删除才真删。
            // 代价是多一次点击，换的是不会误删。
            val pending = pendingDeleteId == record.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pendingDeleteId = if (pending) null else record.id }
                    .padding(vertical = MagicSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = hex.symbol, fontSize = MagicType.title2.sp, color = colors.primaryText)
                Spacer(Modifier.width(MagicSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = hex.name,
                            fontSize = MagicType.subheadline.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.primaryText,
                        )
                        if (record.movingIndexes.isNotEmpty()) {
                            Spacer(Modifier.width(MagicSpacing.xs))
                            Text(
                                text = record.movingIndexes.joinToString("") {
                                    YaoPosition.of(it).ordinalLabel
                                } + "动",
                                fontSize = MagicType.caption.sp,
                                color = colors.accent,
                            )
                        }
                    }
                    Text(
                        text = record.question.ifBlank { "（未填写所问）" },
                        fontSize = MagicType.caption.sp,
                        color = colors.tertiaryText,
                        maxLines = 1,
                    )
                    Text(
                        text = "${formatRecordTime(record.createdAt)} · ${record.method}",
                        fontSize = MagicType.caption.sp,
                        color = colors.tertiaryText,
                    )
                }
                if (mark != null) {
                    Text(
                        text = mark.glyph,
                        fontSize = MagicType.footnote.sp,
                        fontWeight = FontWeight.Bold,
                        color = verdictColor,
                    )
                    Spacer(Modifier.width(MagicSpacing.xs))
                    Text(text = mark.label, fontSize = MagicType.footnote.sp, color = verdictColor)
                }
            }
            if (pending) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = MagicSpacing.md),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "取消",
                        fontSize = MagicType.footnote.sp,
                        color = colors.tertiaryText,
                        modifier = Modifier
                            .clickable { pendingDeleteId = null }
                            .padding(horizontal = MagicSpacing.md, vertical = MagicSpacing.sm),
                    )
                    Text(
                        text = "删除",
                        fontSize = MagicType.footnote.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.inauspicious,
                        modifier = Modifier
                            .clickable {
                                onDelete(record.id)
                                pendingDeleteId = null
                            }
                            .padding(horizontal = MagicSpacing.md, vertical = MagicSpacing.sm),
                    )
                }
            }
            if (index != shown.lastIndex) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
            }
        }

        Spacer(Modifier.height(MagicSpacing.md))
        MagicCaption("点按条目选择，再确认删除。记录仅存于本机，不上传任何数据。")
    }
}

private val historyTimeFormat: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")

private fun formatRecordTime(epochMillis: Long): String =
    java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(historyTimeFormat)
