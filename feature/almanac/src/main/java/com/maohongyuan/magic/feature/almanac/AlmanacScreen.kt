package com.maohongyuan.magic.feature.almanac

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.maohongyuan.magic.designsystem.glass.DateGranularity
import com.maohongyuan.magic.designsystem.glass.GlassCard
import com.maohongyuan.magic.designsystem.glass.GlassLevel
import com.maohongyuan.magic.designsystem.glass.MagicDateBar
import com.maohongyuan.magic.designsystem.glass.MagicListRow
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.core.calendar.CalendarService
import com.maohongyuan.magic.core.calendar.DayInfo
import com.maohongyuan.magic.core.calendar.HourInfo
import com.maohongyuan.magic.designsystem.glass.MagicBadge
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicDivider
import com.maohongyuan.magic.designsystem.glass.MagicInfoRow
import com.maohongyuan.magic.designsystem.glass.MagicPageContent
import com.maohongyuan.magic.designsystem.glass.MagicPageHeader
import com.maohongyuan.magic.designsystem.glass.MagicSectionTitle
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.MagicSemanticMark
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

/**
 * 今日黄历。
 *
 * 数据来自 [CalendarService]（历法内核，离线计算，无网络依赖）。
 */
@Composable
fun AlmanacScreen(
    scrollState: ScrollState,
    onOpenDailyFortune: (() -> Unit)? = null,
) {
    val colors = LocalMagicColors.current

    // 黄历不再只看今天 —— 任意日期都能查。
    // 时柱取「当前时刻的 hour」：日子本身没有时柱这个概念，换成任何固定值都是武断的；
    // 保持随当前时刻最不误导，页面上另有 caption 指向下方的「时辰吉凶」。
    var date by remember { mutableStateOf(LocalDate.now()) }
    val day: DayInfo = remember(date) {
        CalendarService.of(date.year, date.monthValue, date.dayOfMonth, LocalTime.now().hour)
    }
    val hours: List<HourInfo> = remember(day) {
        CalendarService.hoursOf(day.solar.year, day.solar.month, day.solar.day)
    }

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(
            title = "黄历",
            subtitle = "${day.solar.ymd} · 农历${day.lunar.monthLabel}${day.lunar.dayLabel}",
        )

        // 日期条：前后翻 + 点开输入任意日期
        MagicDateBar(
            date = date,
            onDateChange = { date = it },
            granularity = DateGranularity.DAY,
        )

        // ── 日课概要 ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = day.lunar.dayLabel,
                        fontSize = MagicType.display.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText,
                    )
                    Spacer(Modifier.height(MagicSpacing.xs))
                    Text(
                        text = "${day.lunar.yearGanZhi}年 · " +
                            "${day.ganZhi.month?.stem?.label ?: ""}${day.ganZhi.month?.branch?.label ?: ""}月 · " +
                            "${day.ganZhi.day?.stem?.label ?: ""}${day.ganZhi.day?.branch?.label ?: ""}日",
                        fontSize = MagicType.subheadline.sp,
                        color = colors.secondaryText,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    MagicBadge(day.solar.weekLabel, colors.accent)
                    Spacer(Modifier.height(MagicSpacing.row))
                    Text(
                        text = "${day.lunar.zodiac}年",
                        fontSize = MagicType.footnote.sp,
                        color = colors.tertiaryText,
                    )
                }
            }

            if (day.jieQi != null) {
                Spacer(Modifier.height(MagicSpacing.md))
                MagicBadge("节气 · ${day.jieQi}", colors.auspicious)
            }
            if (day.festivals.isNotEmpty()) {
                Spacer(Modifier.height(MagicSpacing.row))
                Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.xs)) {
                    day.festivals.forEach { MagicBadge(it, colors.warning) }
                }
            }

            MagicDivider()

            Column {
                MagicInfoRow("年柱", "${day.ganZhi.year} · ${day.ganZhi.yearNaYin}")
                MagicInfoRow("月柱", "${day.ganZhi.month} · ${day.ganZhi.monthNaYin}")
                MagicInfoRow("日柱", "${day.ganZhi.day} · ${day.ganZhi.dayNaYin}")
                MagicInfoRow("时柱", "${day.ganZhi.hour} · ${day.ganZhi.timeNaYin} · 随当前时辰")
                MagicInfoRow("建除", "${day.almanac.zhiXing}日")
                MagicInfoRow("黄道", "${day.almanac.tianShen}（${day.almanac.tianShenType}）")
                MagicInfoRow("星宿", "${day.almanac.xiu}宿 · ${day.almanac.xiuLuck}")
            }
        }

        // ── 宜 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("宜")
            Spacer(Modifier.height(MagicSpacing.md))
            MarkedList(
                mark = MagicSemanticMark.AUSPICIOUS,
                color = colors.auspicious,
                items = day.almanac.yi,
            )
        }

        // ── 忌 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("忌")
            Spacer(Modifier.height(MagicSpacing.md))
            MarkedList(
                mark = MagicSemanticMark.INAUSPICIOUS,
                color = colors.inauspicious,
                items = day.almanac.ji,
            )
        }

        // ── 冲煞与吉神方位 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("冲煞 · 吉神方位")
            Spacer(Modifier.height(MagicSpacing.md))
            Column {
                MagicInfoRow(
                    label = "冲煞",
                    value = "${day.almanac.clashDescription}煞${day.almanac.sha}",
                    valueColor = colors.warning,
                )
                day.almanac.positions.forEach { p ->
                    MagicInfoRow(p.name, "${p.direction} · ${p.description}")
                }
            }
        }

        // ── 时辰吉凶 ──
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("时辰吉凶")
            Spacer(Modifier.height(MagicSpacing.md))
            HourGrid(hours)
            Spacer(Modifier.height(MagicSpacing.md))
            MagicCaption(
                "吉时：" + hours.filter { it.auspicious }
                    .joinToString("、") { it.branch.label } + "。" +
                    "凶时宜静不宜动。"
            )
        }

        // ── 吉神 / 凶煞 ──
        if (day.almanac.jiShen.isNotEmpty() || day.almanac.xiongSha.isNotEmpty()) {
            GlassCard(level = GlassLevel.Thin) {
                MagicSectionTitle("神煞")
                Spacer(Modifier.height(MagicSpacing.md))
                if (day.almanac.jiShen.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = MagicSemanticMark.AUSPICIOUS.glyph,
                            fontSize = MagicType.subheadline.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.auspicious,
                            modifier = Modifier.width(24.dp),
                        )
                        Text(
                            text = day.almanac.jiShen.joinToString("、"),
                            fontSize = MagicType.footnote.sp,
                            color = colors.secondaryText,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(MagicSpacing.row))
                }
                if (day.almanac.xiongSha.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = MagicSemanticMark.INAUSPICIOUS.glyph,
                            fontSize = MagicType.subheadline.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.inauspicious,
                            modifier = Modifier.width(24.dp),
                        )
                        Text(
                            text = day.almanac.xiongSha.joinToString("、"),
                            fontSize = MagicType.footnote.sp,
                            color = colors.secondaryText,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // ── 彭祖百忌 ──
        if (day.almanac.pengZuGan.isNotBlank() || day.almanac.pengZuZhi.isNotBlank()) {
            GlassCard(level = GlassLevel.Thin) {
                MagicSectionTitle("彭祖百忌")
                Spacer(Modifier.height(MagicSpacing.md))
                Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                    if (day.almanac.pengZuGan.isNotBlank()) {
                        Text(
                            text = day.almanac.pengZuGan,
                            fontSize = MagicType.subheadline.sp,
                            color = colors.secondaryText,
                        )
                    }
                    if (day.almanac.pengZuZhi.isNotBlank()) {
                        Text(
                            text = day.almanac.pengZuZhi,
                            fontSize = MagicType.subheadline.sp,
                            color = colors.secondaryText,
                        )
                    }
                }
            }
        }

        // ── 动线：去日运 ──
        //
        // 黄历回答「这个日子本身宜不宜做某事」，日运回答「这个日子对你意味着什么」。
        // 两者是两层，不是替代关系 —— 所以这里给一条明确的下一步，而不是把日运塞进来。
        GlassCard(level = GlassLevel.Thin) {
            MagicListRow(
                title = "查看我的今日运势",
                subtitle = "按你的日主与今日干支推十神",
                trailing = "›",
                onClick = onOpenDailyFortune,
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
            text = if (items.isEmpty()) "诸事不宜" else items.joinToString("、"),
            fontSize = MagicType.subheadline.sp,
            fontWeight = FontWeight.Medium,
            color = colors.primaryText,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 十二时辰吉凶网格 */
@Composable
private fun HourGrid(hours: List<HourInfo>) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current

    Column(verticalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
        hours.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
                row.forEach { hour ->
                    val tint = if (hour.auspicious) colors.auspicious else colors.inauspicious
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(tint.copy(alpha = 0.10f), shapes.controlInCard)
                            .padding(vertical = MagicSpacing.row),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = hour.branch.label,
                            fontSize = MagicType.body.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primaryText,
                        )
                        Text(
                            text = hour.rangeLabel,
                            fontSize = MagicType.caption.sp,
                            color = colors.tertiaryText,
                        )
                        Text(
                            text = if (hour.auspicious) "吉" else "凶",
                            fontSize = MagicType.caption.sp,
                            fontWeight = FontWeight.Bold,
                            color = tint,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = hour.tianShen,
                            fontSize = MagicType.caption.sp,
                            color = colors.tertiaryText,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
