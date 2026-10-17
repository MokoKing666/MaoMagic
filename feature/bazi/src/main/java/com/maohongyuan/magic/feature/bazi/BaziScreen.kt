package com.maohongyuan.magic.feature.bazi

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import com.maohongyuan.magic.core.calendar.LiuNianService
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.core.calendar.BaZiChart
import com.maohongyuan.magic.core.calendar.BaZiService
import com.maohongyuan.magic.core.calendar.Gender
import com.maohongyuan.magic.core.interpret.BaZiRuleEngine
import com.maohongyuan.magic.core.interpret.Interpretation
import com.maohongyuan.magic.core.interpret.InterpretationRequest
import com.maohongyuan.magic.core.interpret.Tone
import com.maohongyuan.magic.core.interpret.Verdict
import com.maohongyuan.magic.core.model.WuXing
import com.maohongyuan.magic.designsystem.glass.GlassCard
import com.maohongyuan.magic.designsystem.glass.GlassLevel
import com.maohongyuan.magic.designsystem.glass.MagicBadge
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicCell
import com.maohongyuan.magic.designsystem.glass.MagicDivider
import com.maohongyuan.magic.designsystem.glass.MagicHeaderCell
import com.maohongyuan.magic.designsystem.glass.MagicInfoRow
import com.maohongyuan.magic.designsystem.glass.MagicMetricBar
import com.maohongyuan.magic.designsystem.glass.MagicPageContent
import com.maohongyuan.magic.designsystem.glass.MagicPageHeader
import com.maohongyuan.magic.designsystem.glass.MagicSectionTitle
import com.maohongyuan.magic.designsystem.glass.MagicSegmentedControl
import com.maohongyuan.magic.designsystem.glass.MagicSolidButton
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.LocalMagicWuXing
import com.maohongyuan.magic.designsystem.theme.MagicSemanticMark
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * 八字命盘。
 *
 * 数据来自 [BaZiService]（lunar 库封装，离线计算）。
 */
@Composable
fun BaziScreen(scrollState: ScrollState) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    val wuXing = LocalMagicWuXing.current
    val scope = rememberCoroutineScope()

    var year by remember { mutableStateOf("1990") }
    var month by remember { mutableStateOf("6") }
    var day by remember { mutableStateOf("15") }
    var hour by remember { mutableStateOf("12") }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var chart by remember { mutableStateOf<BaZiChart?>(null) }
    var interpretation by remember { mutableStateOf<Interpretation?>(null) }
    // 当前展开流年的大运。默认第 1 步（第一步大运），起盘即有内容可看
    var selectedDaYun by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }

    fun cast() {
        val y = year.toIntOrNull()
        val m = month.toIntOrNull()
        val d = day.toIntOrNull()
        val h = hour.toIntOrNull()
        if (y == null || m == null || d == null || h == null ||
            m !in 1..12 || d !in 1..31 || h !in 0..23 || y !in 1900..2100
        ) {
            error = "请检查生辰：年 1900–2100，月 1–12，日 1–31，时 0–23"
            chart = null
            interpretation = null
            return
        }
        error = null
        val c = runCatching { BaZiService.chart(y, m, d, h, gender = gender) }.getOrNull()
        chart = c
        interpretation = null
        if (c != null) {
            scope.launch {
                interpretation = BaZiRuleEngine().interpret(
                    InterpretationRequest("看命盘", chart = c)
                ).firstOrNull()
            }
        }
    }

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(title = "八字命盘", subtitle = "四柱 · 十神 · 大运流年")

        // ── 生辰 ──
        GlassCard {
            MagicSectionTitle("生辰")
            Spacer(Modifier.height(MagicSpacing.xs))
            // 输入口径必须写明 —— 公历/农历填错会整盘皆错，这是最容易踩的坑
            MagicCaption("请填【公历（阳历）】出生时间，不是农历。下方会自动换算成农历与干支。")
            Spacer(Modifier.height(MagicSpacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                FieldBox("公历年", year, { year = it }, Modifier.weight(1.4f))
                FieldBox("月", month, { month = it }, Modifier.weight(1f))
                FieldBox("日", day, { day = it }, Modifier.weight(1f))
                FieldBox("时", hour, { hour = it }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(MagicSpacing.md))
            MagicSegmentedControl(
                options = Gender.entries.map { it.label },
                selectedIndex = Gender.entries.indexOf(gender),
                onSelect = { gender = Gender.entries[it] },
            )
            if (error != null) {
                Spacer(Modifier.height(MagicSpacing.md))
                Text(
                    text = "${MagicSemanticMark.WARNING.glyph} $error",
                    fontSize = MagicType.footnote.sp,
                    color = colors.warning,
                )
            }
            Spacer(Modifier.height(MagicSpacing.cardPadding))
            MagicSolidButton(onClick = ::cast, selected = true, modifier = Modifier.fillMaxWidth()) {
                Text("排盘", fontSize = MagicType.body.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
            }
            Spacer(Modifier.height(MagicSpacing.md))
            MagicCaption("时柱由出生时辰决定，请务必填对。性别影响大运顺逆。")
        }

        chart?.let { c ->
            // ── 四柱 ──
            GlassCard(level = GlassLevel.Thin) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MagicSectionTitle("四柱", modifier = Modifier.weight(1f))
                    MagicBadge(c.eightCharLabel, colors.accent)
                }
                Spacer(Modifier.height(MagicSpacing.xs))
                // 回显换算结果：用户一眼就能确认公历填得对不对
                MagicCaption(
                    "公历 ${c.solar.ymd} ${c.solar.hour}时 · " +
                        "农历 ${c.lunar.yearGanZhi}年${c.lunar.monthLabel}${c.lunar.dayLabel}" +
                        if (c.lunar.isLeapMonth) "（闰月）" else ""
                )
                Spacer(Modifier.height(MagicSpacing.md))

                Row(modifier = Modifier.fillMaxWidth()) {
                    MagicHeaderCell("", weight = 0.72f)
                    c.pillars.forEach { MagicHeaderCell(it.label, weight = 1f, alignment = TextAlign.Center) }
                }
                Spacer(Modifier.height(MagicSpacing.xs))
                MagicDivider()
                Spacer(Modifier.height(MagicSpacing.xs))

                PillarRow("天干", c.pillars.map { it.gan.label }, emphasized = true)
                PillarRow("地支", c.pillars.map { it.zhi.label }, emphasized = true)
                PillarRow("藏干", c.pillars.map { p -> p.hideGan.joinToString("") { it.label } })
                PillarRow("十神", c.pillars.map { it.shiShenGan })
                PillarRow("纳音", c.pillars.map { it.naYin })
                PillarRow("地势", c.pillars.map { it.diShi })

                Spacer(Modifier.height(MagicSpacing.md))
                MagicCaption("日柱为命主本身，已用强调色标出。")
            }

            // ── 五行旺衰 ──
            GlassCard(level = GlassLevel.Thin) {
                MagicSectionTitle("五行分布")
                Spacer(Modifier.height(MagicSpacing.md))
                Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)) {
                    c.wuXingRanked.forEach { (wx, n) ->
                        MagicMetricBar(
                            label = wx.label,
                            ratio = if (c.maxWuXingCount == 0) 0f else n.toFloat() / c.maxWuXingCount,
                            color = when (wx) {
                                WuXing.MU -> wuXing.wood
                                WuXing.HUO -> wuXing.fire
                                WuXing.TU -> wuXing.earth
                                WuXing.JIN -> wuXing.metal
                                WuXing.SHUI -> wuXing.water
                            },
                        )
                    }
                }
                Spacer(Modifier.height(MagicSpacing.md))
                MagicDivider()
                Spacer(Modifier.height(MagicSpacing.md))
                val missing = c.wuXingCount.filter { it.value == 0 }.keys
                Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                    MagicBadge("日主 ${c.dayMaster.label}（${c.dayMaster.wuXing}）", colors.accent)
                    if (missing.isNotEmpty()) {
                        MagicBadge("缺 ${missing.joinToString("、") { it.label }}", colors.warning)
                    }
                }
                Spacer(Modifier.height(MagicSpacing.md))
                MagicCaption("统计口径为四柱 4 干 + 4 支，不含藏干。")
            }

            // ── 大运 · 流年 ──
            //
            // 大运是可点的：点中某一步，下方展开它管的十个流年。
            // 大运只给「十年一格的走势」，流年才回答「今年具体怎样」——
            // 两者必须一起看，缺了流年等于只说了个大概。
            GlassCard(level = GlassLevel.Thin) {
                MagicSectionTitle("大运 · 流年")
                Spacer(Modifier.height(MagicSpacing.xs))
                MagicCaption(
                    "${if (c.forward) "顺行" else "逆行"} · ${c.startAge} 岁起运　" +
                        "点大运看流年"
                )
                Spacer(Modifier.height(MagicSpacing.md))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row),
                ) {
                    c.daYun.forEach { d ->
                        val active = d.index == selectedDaYun
                        Column(
                            modifier = Modifier
                                .width(84.dp)
                                .background(
                                    if (active) colors.accent.copy(alpha = 0.12f) else Color.Transparent,
                                    shapes.controlInCard,
                                )
                                .clickable { selectedDaYun = d.index }
                                .padding(vertical = MagicSpacing.md),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "${d.startAge}岁",
                                fontSize = MagicType.caption.sp,
                                color = if (active) colors.accent else colors.tertiaryText,
                            )
                            Text(
                                text = d.ganZhi,
                                fontSize = MagicType.body.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) colors.accent else colors.primaryText,
                            )
                            Text("${d.startYear}", fontSize = MagicType.caption.sp, color = colors.tertiaryText)
                            Text(
                                text = "空${d.xunKong}",
                                fontSize = MagicType.caption.sp,
                                color = colors.tertiaryText,
                            )
                        }
                    }
                }

                // ── 选中大运的十个流年 ──
                val activePeriod = c.daYun.firstOrNull { it.index == selectedDaYun }
                if (activePeriod != null) {
                    val years = LiuNianService.ofDaYun(c, activePeriod)

                    Spacer(Modifier.height(MagicSpacing.cardPadding))
                    MagicDivider()
                    Spacer(Modifier.height(MagicSpacing.md))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${activePeriod.ganZhi}运",
                            fontSize = MagicType.subheadline.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent,
                        )
                        Spacer(Modifier.width(MagicSpacing.sm))
                        Text(
                            text = "${activePeriod.startAge}–${activePeriod.endAge} 岁　" +
                                "${activePeriod.startYear}–${activePeriod.endYear}",
                            fontSize = MagicType.caption.sp,
                            color = colors.tertiaryText,
                        )
                    }
                    Spacer(Modifier.height(MagicSpacing.md))

                    // 五个一行的网格：十年刚好两行，不用横向滚动
                    years.chunked(5).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MagicSpacing.sm),
                        ) {
                            row.forEach { ln ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            colors.accent.copy(alpha = 0.06f),
                                            shapes.controlInCard,
                                        )
                                        .padding(vertical = MagicSpacing.sm),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = "${ln.year}",
                                        fontSize = MagicType.caption.sp,
                                        color = colors.tertiaryText,
                                    )
                                    Text(
                                        text = ln.ganZhi.toString(),
                                        fontSize = MagicType.subheadline.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.primaryText,
                                    )
                                    Text(
                                        text = ln.shiShen,
                                        fontSize = MagicType.caption.sp,
                                        color = colors.secondaryText,
                                    )
                                    Text(
                                        text = "${ln.age}岁",
                                        fontSize = MagicType.caption.sp,
                                        color = colors.tertiaryText,
                                    )
                                }
                            }
                            // 补空格，保证最后一行也是五等分对齐
                            repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(MagicSpacing.sm))
                    }

                    MagicCaption("流年干支按「公元 4 年为甲子」推得；十神相对日主 ${c.dayMaster.label} 判定。")
                }
            }

            // ── 宫位 ──
            GlassCard(level = GlassLevel.Thin) {
                MagicSectionTitle("胎元 · 命宫 · 身宫")
                Spacer(Modifier.height(MagicSpacing.md))
                Column {
                    MagicInfoRow("胎元", "${c.taiYuan} · ${c.taiYuanNaYin}")
                    MagicInfoRow("命宫", "${c.mingGong} · ${c.mingGongNaYin}")
                    MagicInfoRow("身宫", "${c.shenGong} · ${c.shenGongNaYin}")
                }
            }
        }

        interpretation?.let { InterpretationCard(it) }

        Text(
            text = "术数内容仅供传统文化研究与娱乐，不构成任何决策建议。",
            fontSize = MagicType.legalMin.sp,
            color = colors.tertiaryText,
            modifier = Modifier.padding(vertical = MagicSpacing.row),
        )
    }
}

@Composable
private fun FieldBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    Column(modifier = modifier) {
        Text(label, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
        Spacer(Modifier.height(MagicSpacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (colors.isLight) Color.White.copy(alpha = 0.72f)
                    else Color.Black.copy(alpha = 0.40f),
                    shapes.controlInCard,
                )
                .padding(horizontal = MagicSpacing.md, vertical = MagicSpacing.md),
        ) {
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(4)) },
                singleLine = true,
                textStyle = TextStyle(fontSize = MagicType.subheadline.sp, color = colors.primaryText),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PillarRow(label: String, values: List<String>, emphasized: Boolean = false) {
    val colors = LocalMagicColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MagicSpacing.row),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MagicCell(text = label, weight = 0.72f, color = colors.tertiaryText)
        values.forEachIndexed { index, value ->
            MagicCell(
                text = value,
                weight = 1f,
                emphasized = emphasized,
                // 日柱（index 2）是命主本身，用强调色标出
                color = if (index == 2 && emphasized) colors.accent else null,
                alignment = TextAlign.Center,
            )
        }
    }
}

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

    GlassCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MagicSectionTitle("概述", modifier = Modifier.weight(1f))
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
                    Text(section.body, fontSize = MagicType.footnote.sp, color = colors.secondaryText)
                }
            }
        }

        Spacer(Modifier.height(MagicSpacing.cardPadding))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(verdictColor.copy(alpha = 0.10f), shapes.controlInCard)
                .padding(MagicSpacing.md),
        ) {
            Text(
                text = interpretation.headline,
                fontSize = MagicType.subheadline.sp,
                fontWeight = FontWeight.SemiBold,
                color = verdictColor,
            )
        }
    }
}
