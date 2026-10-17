package com.maohongyuan.magic.feature.personality

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.core.personality.Dimension
import com.maohongyuan.magic.core.personality.PersonalityChart
import com.maohongyuan.magic.core.personality.PersonalityData
import com.maohongyuan.magic.core.personality.PersonalityScoring
import com.maohongyuan.magic.core.personality.Question
import com.maohongyuan.magic.designsystem.glass.GlassCard
import com.maohongyuan.magic.designsystem.glass.GlassLevel
import com.maohongyuan.magic.designsystem.glass.MagicBadge
import com.maohongyuan.magic.designsystem.glass.MagicCaption
import com.maohongyuan.magic.designsystem.glass.MagicDivider
import com.maohongyuan.magic.designsystem.glass.MagicMetricBar
import com.maohongyuan.magic.designsystem.glass.MagicPageContent
import com.maohongyuan.magic.designsystem.glass.MagicPageHeader
import com.maohongyuan.magic.designsystem.glass.MagicSectionTitle
import com.maohongyuan.magic.designsystem.glass.MagicSolidButton
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

/** 每页题数。60 题分 10 页，一屏 6 题不至于压垮人。 */
private const val PAGE_SIZE = 6

/**
 * 十六型人格。
 *
 * ⚠️ 不是 MBTI®（The Myers-Briggs Company 注册商标），也不是 NERIS（16Personalities 专有）。
 * 题目与类型描述均为自编，依荣格类型理论。
 */
@Composable
fun PersonalityScreen(scrollState: ScrollState) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current

    val answers = remember { mutableStateMapOf<Int, Int>() }
    var page by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<PersonalityChart?>(null) }

    val questions = PersonalityData.questions
    val totalPages = (questions.size + PAGE_SIZE - 1) / PAGE_SIZE

    fun submit() {
        result = PersonalityScoring.score(answers.toMap())
    }

    MagicPageContent(scrollState = scrollState) {
        MagicPageHeader(title = "十六型人格", subtitle = "荣格类型 · 自编 60 题")

        result?.let { r ->
            ResultCard(r) {
                result = null
                answers.clear()
                page = 0
            }
            Disclaimer()
            return@MagicPageContent
        }

        // ── 进度 ──
        GlassCard(level = GlassLevel.Thin) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "第 ${page + 1} / $totalPages 页",
                    fontSize = MagicType.footnote.sp,
                    color = colors.secondaryText,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "已答 ${answers.size} / ${questions.size}",
                    fontSize = MagicType.footnote.sp,
                    color = colors.tertiaryText,
                )
            }
            Spacer(Modifier.height(MagicSpacing.sm))
            // 进度条自己画，不用 MagicMetricBar ——
            // 它自带百分比读数，会和上面的「已答 n/60」重复；
            // 而把计数塞进它的 label 又会被挤成一字一行（label 栏宽度不够）。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(shapes.pill)
                    .background(colors.divider),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(answers.size.toFloat() / questions.size)
                        .fillMaxHeight()
                        .clip(shapes.pill)
                        .background(colors.accent),
                )
            }
        }

        // ── 本页题目 ──
        GlassCard {
            val start = page * PAGE_SIZE
            val slice = questions.drop(start).take(PAGE_SIZE)
            slice.forEachIndexed { index, q ->
                if (index > 0) {
                    Spacer(Modifier.height(MagicSpacing.cardPadding))
                    MagicDivider()
                    Spacer(Modifier.height(MagicSpacing.cardPadding))
                }
                QuestionRow(
                    index = start + index + 1,
                    question = q,
                    selected = answers[q.id],
                    onSelect = { answers[q.id] = it },
                )
            }
        }

        // ── 翻页 ──
        Row(horizontalArrangement = Arrangement.spacedBy(MagicSpacing.row)) {
            if (page > 0) {
                MagicSolidButton(
                    onClick = { page-- },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("上一页", fontSize = MagicType.body.sp, color = colors.secondaryText)
                }
            }
            val isLast = page == totalPages - 1
            MagicSolidButton(
                onClick = {
                    if (isLast) submit() else page++
                },
                selected = true,
                enabled = !isLast || PersonalityScoring.isComplete(answers.toMap()),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (isLast) "看结果" else "下一页",
                    fontSize = MagicType.body.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent,
                )
            }
        }

        if (page == totalPages - 1 && !PersonalityScoring.isComplete(answers.toMap())) {
            MagicCaption("还有题目没答完，答完才能看结果。可回到上一页补答。")
        }

        Disclaimer()
    }
}

@Composable
private fun QuestionRow(
    index: Int,
    question: Question,
    selected: Int?,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$index. 更接近哪一边？",
            fontSize = MagicType.caption.sp,
            color = colors.tertiaryText,
        )
        Spacer(Modifier.height(MagicSpacing.xs))

        // ── 为什么是上下布局而不是左右 ──
        // 左右布局下，5 个圆点要占掉约 180dp，剩下的宽度分给两段中文描述
        // 每段只有 70dp 左右 —— 中文 13sp 一个字约 13dp，等于每行只能放 5 个字，
        // 实测被压成「聚会里主 / 动找陌生 / 人聊天」这种竖排，极难读。
        // 改成描述各占整行、圆点居中，中文再长也不会挤。
        Text(
            text = question.l,
            fontSize = MagicType.footnote.sp,
            color = if (selected != null && selected <= 2) colors.primaryText else colors.secondaryText,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(MagicSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 五点：1 完全偏上（左）… 5 完全偏下（右）
            repeat(5) { i ->
                val choice = i + 1
                val on = selected == choice
                // 统一尺寸 + 描边：原实现把中间那个做小 4dp，排出来参差不齐；
                // 且全实心灰没有任何可点暗示，用户不知道能点。
                Box(
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .size(38.dp)
                        .clip(shapes.pill)
                        .background(if (on) colors.accent else Color.Transparent)
                        .border(
                            width = if (on) 0.dp else 1.dp,
                            color = if (on) Color.Transparent else colors.divider,
                            shape = shapes.pill,
                        )
                        .clickable { onSelect(choice) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$choice",
                        fontSize = MagicType.caption.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        color = if (on) colors.backdropBase else colors.tertiaryText,
                    )
                }
            }
        }

        Spacer(Modifier.height(MagicSpacing.sm))
        Text(
            text = question.r,
            fontSize = MagicType.footnote.sp,
            color = if (selected != null && selected >= 4) colors.primaryText else colors.secondaryText,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun poleName(letter: Char): String = when (letter) {
    'E' -> "外向"; 'I' -> "内向"
    'S' -> "实感"; 'N' -> "直觉"
    'T' -> "思考"; 'F' -> "情感"
    'J' -> "判断"; 'P' -> "知觉"
    else -> letter.toString()
}

@Composable
private fun ResultCard(chart: PersonalityChart, onRetake: () -> Unit) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current
    val type = chart.type

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = chart.code,
                fontSize = MagicType.largeTitle.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primaryText,
            )
            Spacer(Modifier.height(0.dp))
            Column(modifier = Modifier.weight(1f).padding(start = MagicSpacing.md)) {
                Text(
                    text = type?.label ?: "",
                    fontSize = MagicType.title3.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent,
                )
                if (chart.hasTie) {
                    MagicBadge("有维度居中", colors.warning)
                }
            }
        }

        Spacer(Modifier.height(MagicSpacing.cardPadding))
        MagicDivider()
        Spacer(Modifier.height(MagicSpacing.md))

        // ── 四维：两极分布条 ──
        //
        // 原实现用 MagicMetricBar(ratio = percent/100) 画「0 → 强度」的单向条，
        // 语义是错的：percent 是「偏离中点的幅度」，不是「这一极占多少」。
        // 而且 label 里塞「外向 E (20%)」这种长文本会被窄栏挤成竖排。
        //
        // 改成两极分布：第一极占比 = 50 + percent/2，第二极 = 50 − percent/2，
        // 条从左填充第一极占比。这样「60 : 40」一眼能读出倾向与幅度。
        chart.scores.forEach { s ->
            val firstPole = s.dimension.first
            val secondPole = s.dimension.second
            val firstShare = 50 + s.percent / 2
            val secondShare = 100 - firstShare

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${poleName(firstPole)} $firstPole",
                    fontSize = MagicType.caption.sp,
                    fontWeight = if (s.raw >= 0) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (s.raw >= 0) colors.accent else colors.tertiaryText,
                )
                Text(
                    text = "$firstShare : $secondShare",
                    fontSize = MagicType.caption.sp,
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${poleName(secondPole)} $secondPole",
                    fontSize = MagicType.caption.sp,
                    fontWeight = if (s.raw < 0) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (s.raw < 0) colors.accent else colors.tertiaryText,
                )
            }
            Spacer(Modifier.height(MagicSpacing.xs))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(shapes.pill)
                    .background(colors.divider),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(firstShare / 100f)
                        .fillMaxHeight()
                        .clip(shapes.pill)
                        .background(colors.accent),
                )
            }
            Spacer(Modifier.height(MagicSpacing.md))
        }

        Spacer(Modifier.height(MagicSpacing.md))
        MagicCaption("条形长度表示倾向强度，不是「好」或「差」。")
    }

    type?.let { t ->
        GlassCard(level = GlassLevel.Thin) {
            MagicSectionTitle("概述")
            Spacer(Modifier.height(MagicSpacing.xs))
            Text(t.summary, fontSize = MagicType.footnote.sp, color = colors.secondaryText)

            Spacer(Modifier.height(MagicSpacing.md))
            MagicDivider()
            Spacer(Modifier.height(MagicSpacing.md))

            MagicSectionTitle("常见优势")
            Spacer(Modifier.height(MagicSpacing.xs))
            t.strengths.forEach {
                Text("· $it", fontSize = MagicType.footnote.sp, color = colors.auspicious)
                Spacer(Modifier.height(2.dp))
            }

            Spacer(Modifier.height(MagicSpacing.md))
            MagicSectionTitle("值得留意")
            Spacer(Modifier.height(MagicSpacing.xs))
            t.cautions.forEach {
                Text("· $it", fontSize = MagicType.footnote.sp, color = colors.warning)
                Spacer(Modifier.height(2.dp))
            }

            if (t.fit.isNotBlank()) {
                Spacer(Modifier.height(MagicSpacing.md))
                MagicSectionTitle("较契合的方向")
                Spacer(Modifier.height(MagicSpacing.xs))
                Text(t.fit, fontSize = MagicType.footnote.sp, color = colors.secondaryText)
            }
        }
    }

    // 局限性 —— 这不是免责套话，是这套工具本身的真实问题
    GlassCard(level = GlassLevel.Thin) {
        MagicSectionTitle("关于这个结果")
        Spacer(Modifier.height(MagicSpacing.xs))
        Text(
            text = "四维类型把连续的倾向切成两类，落在中间的人结果本就不稳定。" +
                "有研究显示相当比例的人隔几周重测会得到不同类型 —— 这不是测错了，" +
                "而是这套框架本身的边界。\n\n" +
                "把它当作自我观察的一个角度，而不是关于你的定论。",
            fontSize = MagicType.footnote.sp,
            color = colors.secondaryText,
        )
        Spacer(Modifier.height(MagicSpacing.md))
        MagicSolidButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) {
            Text("重新测", fontSize = MagicType.body.sp, color = colors.secondaryText)
        }
    }
}

@Composable
private fun Disclaimer() {
    val colors = LocalMagicColors.current
    Text(
        text = "依荣格类型理论自编，非 MBTI®，非 NERIS。人格类型是倾向而非标签。",
        fontSize = MagicType.legalMin.sp,
        color = colors.tertiaryText,
        modifier = Modifier.padding(vertical = MagicSpacing.row),
    )
}
