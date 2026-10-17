package com.maohongyuan.magic.feature.meihua

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.maohongyuan.magic.core.calendar.CalendarService
import com.maohongyuan.magic.core.hexagram.MeihuaChart
import com.maohongyuan.magic.core.hexagram.MeihuaDivination
import com.maohongyuan.magic.core.interpret.Interpretation
import com.maohongyuan.magic.core.interpret.InterpretationRequest
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.maohongyuan.magic.core.ai.CloudAiConfig
import com.maohongyuan.magic.core.ai.CloudAiInterpreter
import com.maohongyuan.magic.core.data.SecretStore
import com.maohongyuan.magic.core.data.SettingsStore
import com.maohongyuan.magic.core.interpret.InterpreterChain
import com.maohongyuan.magic.core.interpret.MeihuaRuleEngine
import com.maohongyuan.magic.core.interpret.Tone
import com.maohongyuan.magic.core.interpret.Verdict
import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.Hexagram
import com.maohongyuan.magic.core.model.Trigram
import com.maohongyuan.magic.core.model.WuXingRelation
import com.maohongyuan.magic.designsystem.glass.GlassCard
import com.maohongyuan.magic.designsystem.glass.GlassLevel
import com.maohongyuan.magic.designsystem.glass.MagicBadge
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicDivider
import com.maohongyuan.magic.designsystem.glass.MagicInfoRow
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

private enum class MeihuaMethod(val label: String) { TIME("时间"), NUMBER("数字"), DEMO("示例") }

/**
 * 梅花易数。
 *
 * 梅花只看体用、互卦、变卦三件事，比六爻简洁，因此规则引擎的结论也相当稳定。
 */
@Composable
fun MeihuaScreen(scrollState: ScrollState) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current

    var method by remember { mutableStateOf(MeihuaMethod.TIME) }
    var numberA by remember { mutableStateOf("") }
    var numberB by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var chart by remember { mutableStateOf<MeihuaChart?>(null) }
    var interpretation by remember { mutableStateOf<Interpretation?>(null) }
    var monthBranch by remember { mutableStateOf<EarthlyBranch?>(null) }

    val scope = rememberCoroutineScope()

    // ── 解卦降级链：智能模式 → 基础模式 ──
    //
    // 云端配置默认关闭，所以默认状态下 CloudAiInterpreter.isAvailable 恒为 false，
    // 链直接落到 Tier1 —— 默认零网络。用户填好 Key 后无需重启即生效。
    val context = LocalContext.current
    var cloudConfig by remember { mutableStateOf(CloudAiConfig.DEFAULT) }
    LaunchedEffect(Unit) {
        val settingsStore = SettingsStore(context)
        val secretStore = SecretStore(context)
        settingsStore.settings.collect { st ->
            cloudConfig = CloudAiConfig(
                enabled = st.cloudAiEnabled,
                baseUrl = st.cloudAiBaseUrl,
                apiKey = secretStore.get(SecretStore.KEY_CLOUD_API_KEY),
                model = st.cloudAiModel,
                temperature = st.cloudAiTemperature,
            )
        }
    }
    // rememberUpdatedState 让 lambda 拿到**最新**配置，同时保持实例稳定
    val configRef = rememberUpdatedState(cloudConfig)
    val chain = remember {
        InterpreterChain(
            listOf(
                CloudAiInterpreter { configRef.value },
                MeihuaRuleEngine(),
            )
        )
    }

    fun interpret(chartValue: MeihuaChart) {
        val day = CalendarService.today()
        monthBranch = day.monthBranch
        interpretation = null
        scope.launch {
            // InterpreterChain.interpret 直接返回结果（内部已做降级与 firstOrNull），
            // 不再是 Flow —— 这里不能再接 .firstOrNull()
            interpretation = chain.interpret(
                InterpretationRequest(
                    question = question.ifBlank { "综合" },
                    meihua = chartValue,
                    monthBranch = day.monthBranch,
                )
            )
        }
    }

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(title = "梅花易数", subtitle = "体用 · 互卦 · 变卦")

        // ── 所问何事 ──
        GlassCard {
            MagicSectionTitle("所问何事")
            Spacer(Modifier.height(MagicSpacing.md))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (colors.isLight) Color.White.copy(alpha = 0.72f)
                        else Color.Black.copy(alpha = 0.40f),
                        shapes.controlInCard,
                    )
                    .padding(MagicSpacing.md),
            ) {
                if (question.isEmpty()) {
                    Text("例：此事可行否？", fontSize = MagicType.subheadline.sp, color = colors.tertiaryText)
                }
                BasicTextField(
                    value = question,
                    onValueChange = { question = it },
                    textStyle = TextStyle(fontSize = MagicType.subheadline.sp, color = colors.primaryText),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ── 起卦 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("起卦方式")
            Spacer(Modifier.height(MagicSpacing.md))
            MagicSegmentedControl(
                options = MeihuaMethod.entries.map { it.label },
                selectedIndex = MeihuaMethod.entries.indexOf(method),
                onSelect = { method = MeihuaMethod.entries[it] },
            )
            Spacer(Modifier.height(MagicSpacing.cardPadding))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.accent.copy(alpha = 0.06f), shapes.controlInCard)
                    .padding(MagicSpacing.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("✿", fontSize = MagicType.display.sp, color = colors.accent)
                Spacer(Modifier.height(MagicSpacing.row))

                when (method) {
                    MeihuaMethod.TIME -> {
                        val day = remember { CalendarService.today() }
                        Text(
                            text = "${day.solar.ymd} · 农历${day.lunar.monthLabel}${day.lunar.dayLabel}",
                            fontSize = MagicType.subheadline.sp,
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
                            onClick = {
                                val c = MeihuaDivination.byTime(
                                    yearBranch = day.ganZhi.year?.branch ?: EarthlyBranch.ZI,
                                    lunarMonth = day.lunar.month,
                                    lunarDay = day.lunar.day,
                                    hourBranch = hourBranchOf(day.solar.hour),
                                )
                                chart = c
                                interpret(c)
                            },
                            selected = true,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("起卦", fontSize = MagicType.body.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                        }
                    }

                    MeihuaMethod.NUMBER -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                            NumberBox("数一（上卦）", numberA, { numberA = it.filter { c -> c.isDigit() }.take(6) }, Modifier.weight(1f))
                            NumberBox("数二（下卦）", numberB, { numberB = it.filter { c -> c.isDigit() }.take(6) }, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(MagicSpacing.md))
                        Text(
                            text = "上卦 = 数一 ÷ 8 取余，下卦 = 数二 ÷ 8 取余，动爻 = 两数之和 ÷ 6 取余",
                            fontSize = MagicType.caption.sp,
                            color = colors.tertiaryText,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(MagicSpacing.cardPadding))
                        MagicSolidButton(
                            onClick = {
                                val a = numberA.toIntOrNull() ?: return@MagicSolidButton
                                val b = numberB.toIntOrNull() ?: return@MagicSolidButton
                                val c = MeihuaDivination.byNumber(a, b)
                                chart = c
                                interpret(c)
                            },
                            enabled = numberA.toIntOrNull() != null && numberB.toIntOrNull() != null,
                            selected = true,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("起卦", fontSize = MagicType.body.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                        }
                    }

                    MeihuaMethod.DEMO -> {
                        Text(
                            text = "泽火革 · 九四动",
                            fontSize = MagicType.subheadline.sp,
                            color = colors.secondaryText,
                        )
                        Spacer(Modifier.height(MagicSpacing.xs))
                        Text(
                            text = "载入一个已知卦例，便于先看界面",
                            fontSize = MagicType.caption.sp,
                            color = colors.tertiaryText,
                        )
                        Spacer(Modifier.height(MagicSpacing.cardPadding))
                        MagicSolidButton(
                            onClick = {
                                val c = MeihuaDivination.chart(Hexagram.byName("泽火革")!!, 3)
                                chart = c
                                interpret(c)
                            },
                            selected = true,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("载入", fontSize = MagicType.body.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                        }
                    }
                }
            }
        }

        // ── 三卦 ──
        chart?.let { c ->
            GlassCard(level = GlassLevel.Thin) {
                MagicSectionTitle("卦象")
                Spacer(Modifier.height(MagicSpacing.cardPadding))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MagicSpacing.md),
                ) {
                    HexagramColumn("本卦", c.original, c.movingIndex, Modifier.weight(1f))
                    HexagramColumn("互卦", c.nuclear, null, Modifier.weight(1f))
                    HexagramColumn("变卦", c.changed, null, Modifier.weight(1f))
                }
                Spacer(Modifier.height(MagicSpacing.cardPadding))
                MagicCaption("标注 ● 者为动爻。变卦由动爻阴阳互变得出，互卦取爻二三四与三四五。")
            }

            // ── 体用 ──
            GlassCard {
                MagicSectionTitle("体用分析")
                Spacer(Modifier.height(MagicSpacing.md))

                Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.md)) {
                    TiYongChip("体", c.tiTrigram, Modifier.weight(1f))
                    TiYongChip("用", c.yongTrigram, Modifier.weight(1f))
                }

                Spacer(Modifier.height(MagicSpacing.cardPadding))
                MagicDivider()
                Spacer(Modifier.height(MagicSpacing.cardPadding))

                val tint = if (c.favorable) colors.auspicious else colors.inauspicious
                val mark = if (c.favorable) MagicSemanticMark.AUSPICIOUS else MagicSemanticMark.INAUSPICIOUS
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mark.glyph,
                        fontSize = MagicType.headline.sp,
                        fontWeight = FontWeight.Bold,
                        color = tint,
                    )
                    Spacer(Modifier.width(MagicSpacing.row))
                    Column {
                        Text(
                            text = "${c.relationLabel} · ${mark.label}",
                            fontSize = MagicType.body.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tint,
                        )
                        Text(
                            text = relationHint(c.relation),
                            fontSize = MagicType.footnote.sp,
                            color = colors.secondaryText,
                        )
                    }
                }

                Spacer(Modifier.height(MagicSpacing.cardPadding))
                Column {
                    MagicInfoRow("动爻", "${c.movingTitle}（${if (c.movingIndex < 3) "下卦" else "上卦"}）")
                    MagicInfoRow("体用判定", "动爻所在卦为用，另一卦为体")
                    MagicInfoRow("生克", "体${c.tiWuXing} ${relationVerb(c.relation)} 用${c.yongWuXing}", valueColor = tint)
                    if (monthBranch != null) {
                        MagicInfoRow("月建", "${monthBranch!!.label}月")
                    }
                }
            }
        }

        // ── 解卦 ──
        interpretation?.let { InterpretationCard(it) }

        Text(
            text = "术数内容仅供传统文化研究与娱乐，不构成任何决策建议。",
            fontSize = MagicType.legalMin.sp,
            color = colors.tertiaryText,
            modifier = Modifier.padding(vertical = MagicSpacing.row),
        )
    }
}

private fun hourBranchOf(hour: Int): EarthlyBranch =
    EarthlyBranch.entries[((hour + 1) / 2) % 12]

private fun relationHint(relation: WuXingRelation): String = when (relation) {
    WuXingRelation.IT_GENERATES -> "用卦生体卦，事来就我，主得利"
    WuXingRelation.SAME -> "体用同气，彼此相应，主顺遂"
    WuXingRelation.I_OVERCOME -> "体卦克用卦，事在掌握，但需主动费力"
    WuXingRelation.I_GENERATE -> "体卦生用卦，我去就事，主耗泄"
    WuXingRelation.IT_OVERCOMES -> "用卦克体卦，事来伤我，主受制"
}

private fun relationVerb(relation: WuXingRelation): String = when (relation) {
    WuXingRelation.IT_GENERATES -> "受"
    WuXingRelation.SAME -> "同于"
    WuXingRelation.I_OVERCOME -> "克"
    WuXingRelation.I_GENERATE -> "生"
    WuXingRelation.IT_OVERCOMES -> "受制于"
}

@Composable
private fun NumberBox(
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

/**
 * 单个卦的竖向展示：六条爻线（自上而下）+ 卦名。
 */
@Composable
private fun HexagramColumn(
    label: String,
    hexagram: Hexagram,
    movingIndex: Int?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current

    Column(
        modifier = modifier
            .background(
                if (colors.isLight) Color.White.copy(alpha = 0.55f)
                else Color.Black.copy(alpha = 0.32f),
                shapes.controlInCard,
            )
            .padding(MagicSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
        Spacer(Modifier.height(MagicSpacing.row))

        hexagram.lines.reversed().forEachIndexed { displayIndex, yang ->
            val originalIndex = hexagram.lines.lastIndex - displayIndex
            val moving = originalIndex == movingIndex
            Row(
                modifier = Modifier.fillMaxWidth().height(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                YaoBar(
                    yang = yang,
                    color = if (moving) colors.accent else colors.primaryText,
                    modifier = Modifier.weight(1f).height(5.dp),
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
        Text(text = hexagram.symbol, fontSize = MagicType.glyph.sp, color = colors.primaryText)
        Spacer(Modifier.height(2.dp))
        Text(
            text = hexagram.name,
            fontSize = MagicType.footnote.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.primaryText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun YaoBar(yang: Boolean, color: Color, modifier: Modifier = Modifier) {
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
private fun TiYongChip(label: String, trigram: Trigram, modifier: Modifier = Modifier) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    val wuXing = LocalMagicWuXing.current
    val tint = when (trigram.wuXing) {
        com.maohongyuan.magic.core.model.WuXing.MU -> wuXing.wood
        com.maohongyuan.magic.core.model.WuXing.HUO -> wuXing.fire
        com.maohongyuan.magic.core.model.WuXing.TU -> wuXing.earth
        com.maohongyuan.magic.core.model.WuXing.JIN -> wuXing.metal
        com.maohongyuan.magic.core.model.WuXing.SHUI -> wuXing.water
    }
    Row(
        modifier = modifier
            .background(tint.copy(alpha = 0.12f), shapes.controlInCard)
            .padding(MagicSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
            Text(
                text = "${trigram.label} ${trigram.symbol}",
                fontSize = MagicType.body.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primaryText,
            )
        }
        Text(
            text = trigram.wuXing.label,
            fontSize = MagicType.title2.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
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
        }

        Spacer(Modifier.height(MagicSpacing.md))
        MagicCaption("本解读由「${interpretation.tier.label}」生成（${interpretation.tier.description}）。")
    }
}
