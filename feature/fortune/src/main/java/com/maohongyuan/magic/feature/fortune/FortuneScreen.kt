package com.maohongyuan.magic.feature.fortune

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.core.data.BirthProfileStore
import com.maohongyuan.magic.core.fortune.BirthProfile
import com.maohongyuan.magic.core.fortune.FortuneChart
import com.maohongyuan.magic.core.fortune.FortunePeriod
import com.maohongyuan.magic.core.fortune.FortuneService
import com.maohongyuan.magic.designsystem.glass.DateGranularity
import com.maohongyuan.magic.designsystem.glass.GlassCard
import com.maohongyuan.magic.designsystem.glass.GlassLevel
import com.maohongyuan.magic.designsystem.glass.MagicBadge
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicDateBar
import com.maohongyuan.magic.designsystem.glass.MagicDivider
import com.maohongyuan.magic.designsystem.glass.MagicEmptyState
import com.maohongyuan.magic.designsystem.glass.MagicMetricBar
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
import java.time.LocalDate

/**
 * 日运 / 月运。
 *
 * ── 与黄历的分工 ──
 * 黄历回答「这个日子本身宜不宜做某事」，所有人看到的一样；
 * 日运回答「这个日子对你意味着什么」，依赖你的日主。
 * 所以这里**不列宜忌**（那是黄历的职责），也不重复罗列四柱，
 * 只讲「你的日主 vs 今日干支」的关系与四维倾向。
 */
@Composable
fun FortuneScreen(period: FortunePeriod, scrollState: ScrollState) {
    val colors = LocalMagicColors.current
    val context = LocalContext.current
    val store = remember { BirthProfileStore(context) }

    var profile by remember { mutableStateOf<BirthProfile?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var chart by remember { mutableStateOf<FortuneChart?>(null) }

    LaunchedEffect(Unit) {
        profile = store.profile.firstOrNull()
        loaded = true
    }

    // 档案或日期一变就重算
    LaunchedEffect(profile, date) {
        val p = profile
        chart = if (p == null) {
            null
        } else {
            runCatching {
                if (period == FortunePeriod.DAILY) {
                    FortuneService.daily(p, date.year, date.monthValue, date.dayOfMonth)
                } else {
                    FortuneService.monthly(p, date.year, date.monthValue)
                }
            }.getOrNull()
        }
    }

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(
            title = period.label,
            subtitle = "按你的日主推流${if (period == FortunePeriod.DAILY) "日" else "月"}十神",
        )

        if (!loaded) return@MagicPageContent

        val p = profile
        if (p == null) {
            // ── 空态：就地引导填写，不要求先去设置页 ──
            GlassCard {
                MagicEmptyState(
                    glyph = "生",
                    title = "先填一下出生时间",
                    description = "日运/月运要算你的日主，没有生辰就算不出来。只存本机，不上传。",
                )
            }
            ProfileForm(initial = null) { saved ->
                profile = saved
            }
            Disclaimer()
            return@MagicPageContent
        }

        // ── 档案摘要 ──
        GlassCard(level = GlassLevel.Thin) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MagicSectionTitle("命主", modifier = Modifier.weight(1f))
                MagicBadge("${p.year}-${p.month}-${p.day} ${p.hour}时", colors.accent)
            }
            Spacer(Modifier.height(MagicSpacing.xs))
            MagicCaption("日主 ${chart?.dayMaster?.label ?: "—"} · 公历生辰")
            Spacer(Modifier.height(MagicSpacing.md))
            MagicDateBar(
                date = date,
                onDateChange = { date = it },
                granularity = if (period == FortunePeriod.DAILY) {
                    DateGranularity.DAY
                } else {
                    DateGranularity.MONTH
                },
            )
        }

        val c = chart
        if (c == null) {
            GlassCard(level = GlassLevel.Thin) {
                MagicCaption("这个日期算不出来，请换一个试试。")
            }
            return@MagicPageContent
        }

        // ── 干支关系 ──
        GlassCard {
            MagicSectionTitle("本${if (period == FortunePeriod.DAILY) "日" else "月"}")
            Spacer(Modifier.height(MagicSpacing.xs))
            Text(
                text = c.dateLabel,
                fontSize = MagicType.title3.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primaryText,
            )
            Spacer(Modifier.height(MagicSpacing.sm))
            Text(
                text = "流${if (period == FortunePeriod.DAILY) "日" else "月"} ${c.targetGanZhi}　" +
                    "日主 ${c.dayMaster.label}　→　${c.shiShen}",
                fontSize = MagicType.footnote.sp,
                color = colors.accent,
            )
        }

        // ── 四维 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("四维倾向")
            Spacer(Modifier.height(MagicSpacing.xs))
            MagicCaption("基准 50 分，按十神与冲合害加减。是参考，不是断语。")
            Spacer(Modifier.height(MagicSpacing.md))

            c.scores.forEachIndexed { i, s ->
                if (i > 0) Spacer(Modifier.height(MagicSpacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = s.dimension.label,
                        fontSize = MagicType.footnote.sp,
                        color = colors.secondaryText,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${s.level} ${s.score}",
                        fontSize = MagicType.footnote.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            s.score >= 70 -> colors.auspicious
                            s.score <= 30 -> colors.inauspicious
                            else -> colors.tertiaryText
                        },
                    )
                }
                Spacer(Modifier.height(MagicSpacing.xs))
                MagicMetricBar(
                    label = "",
                    ratio = s.score / 100f,
                    color = if (s.score >= 70) colors.auspicious
                    else if (s.score <= 30) colors.inauspicious
                    else colors.accent,
                )
                Spacer(Modifier.height(MagicSpacing.xs))
                Text(s.reason, fontSize = MagicType.caption.sp, color = colors.tertiaryText)
            }
        }

        // ── 冲合害 ──
        if (c.relations.isNotEmpty()) {
            GlassCard(level = GlassLevel.Thin) {
                MagicSectionTitle("与命局的互动")
                Spacer(Modifier.height(MagicSpacing.md))
                c.relations.forEach {
                    Text("· 流${if (period == FortunePeriod.DAILY) "日" else "月"}支$it",
                        fontSize = MagicType.footnote.sp, color = colors.secondaryText)
                    Spacer(Modifier.height(MagicSpacing.xs))
                }
            }
        }

        MagicCaption(
            "四维评分是把十神影响映射到现代关注面上的一种工程化处理，不是传统断法里的定论。" +
                "分数高不等于万事大吉，低也不等于诸事不宜。"
        )

        Spacer(Modifier.height(MagicSpacing.md))
        ProfileForm(initial = p) { saved -> profile = saved }

        Disclaimer()
    }
}

/** 填写/修改命主档案 */
@Composable
private fun ProfileForm(initial: BirthProfile?, onSave: (BirthProfile) -> Unit) {
    val colors = LocalMagicColors.current
    val context = LocalContext.current
    val store = remember { BirthProfileStore(context) }
    val scope = rememberCoroutineScope()

    var y by remember { mutableStateOf(initial?.year?.toString() ?: "") }
    var m by remember { mutableStateOf(initial?.month?.toString() ?: "") }
    var d by remember { mutableStateOf(initial?.day?.toString() ?: "") }
    var h by remember { mutableStateOf(initial?.hour?.toString() ?: "") }
    var gender by remember { mutableStateOf(initial?.gender ?: "MALE") }
    var error by remember { mutableStateOf<String?>(null) }

    GlassCard(level = GlassLevel.Thin) {
        MagicSectionTitle(if (initial == null) "填写生辰" else "修改生辰")
        Spacer(Modifier.height(MagicSpacing.xs))
        MagicCaption("请填【公历（阳历）】。只存本机。")

        Spacer(Modifier.height(MagicSpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
            NumField("年", y, { y = it }, Modifier.weight(1.4f))
            NumField("月", m, { m = it }, Modifier.weight(1f))
            NumField("日", d, { d = it }, Modifier.weight(1f))
            NumField("时", h, { h = it }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(MagicSpacing.md))
        MagicSegmentedControl(
            options = listOf("男", "女"),
            selectedIndex = if (gender == "FEMALE") 1 else 0,
            onSelect = { gender = if (it == 1) "FEMALE" else "MALE" },
        )

        if (error != null) {
            Spacer(Modifier.height(MagicSpacing.sm))
            Text("⚠ $error", fontSize = MagicType.caption.sp, color = colors.warning)
        }

        Spacer(Modifier.height(MagicSpacing.cardPadding))
        MagicSolidButton(
            onClick = {
                val yy = y.toIntOrNull()
                val mm = m.toIntOrNull()
                val dd = d.toIntOrNull()
                val hh = h.toIntOrNull()
                if (yy == null || mm == null || dd == null || hh == null ||
                    yy !in 1900..2100 || mm !in 1..12 || dd !in 1..31 || hh !in 0..23
                ) {
                    error = "请检查：年 1900–2100，月 1–12，日 1–31，时 0–23"
                    return@MagicSolidButton
                }
                val p = BirthProfile(yy, mm, dd, hh, 0, gender)
                error = null
                scope.launch { store.save(p) }
                onSave(p)
            },
            selected = true,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (initial == null) "保存并开始" else "保存",
                fontSize = MagicType.body.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.accent,
            )
        }
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
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(4)) },
            placeholder = label,
        )
    }
}

@Composable
private fun Disclaimer() {
    val colors = LocalMagicColors.current
    Text(
        text = "术数内容仅供传统文化研究与娱乐，不构成任何决策建议。",
        fontSize = MagicType.legalMin.sp,
        color = colors.tertiaryText,
        modifier = Modifier.padding(vertical = MagicSpacing.row),
    )
}
