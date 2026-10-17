package com.maohongyuan.magic.core.personality

import com.maohongyuan.magic.core.interpret.DivinationInterpreter
import com.maohongyuan.magic.core.interpret.EngineTier
import com.maohongyuan.magic.core.interpret.Interpretation
import com.maohongyuan.magic.core.interpret.InterpretationRequest
import com.maohongyuan.magic.core.interpret.InterpretationSection
import com.maohongyuan.magic.core.interpret.Tone
import com.maohongyuan.magic.core.interpret.Verdict
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 十六型人格的 Tier1 引擎。
 *
 * ── 这不是占卜，不能判吉凶 ──
 * 人格类型没有好坏。所以 [Verdict] 固定 NEUTRAL，
 * 用段落的 [Tone] 表达「优势 / 注意」，**绝不套用红绿吉凶配色**。
 * 套上去语义就错了：一个「凶」的人格类型是没有意义的说法。
 */
class PersonalityRuleEngine : DivinationInterpreter {

    override val tier: EngineTier = EngineTier.RULE

    override suspend fun isAvailable(): Boolean = true

    override fun interpret(request: InterpretationRequest): Flow<Interpretation> {
        val chart = request.chart as? PersonalityChart ?: return flowOf()
        val type = chart.type ?: return flowOf()
        return flowOf(analyze(chart, type))
    }

    private fun analyze(chart: PersonalityChart, type: PersonalityType): Interpretation {
        val sections = mutableListOf<InterpretationSection>()

        sections += InterpretationSection(
            title = "四维倾向",
            body = chart.scores.joinToString("\n") { s ->
                val pole = if (s.raw >= 0) s.dimension.first else s.dimension.second
                val desc = poleLabel(pole)
                "${s.dimension.label}：偏 $pole（$desc）${s.percent}%"
            },
            tone = Tone.NEUTRAL,
        )

        sections += InterpretationSection(
            title = "概述",
            body = type.summary,
            tone = Tone.NEUTRAL,
        )

        sections += InterpretationSection(
            title = "常见优势",
            body = type.strengths.joinToString("\n") { "· $it" },
            tone = Tone.POSITIVE,
        )

        sections += InterpretationSection(
            title = "值得留意",
            body = type.cautions.joinToString("\n") { "· $it" },
            tone = Tone.WARNING,
        )

        if (type.fit.isNotBlank()) {
            sections += InterpretationSection(
                title = "较契合的方向",
                body = type.fit,
                tone = Tone.NEUTRAL,
            )
        }

        // ── 局限性说明：这一条不是免责套话，是这套工具本身的真实问题 ──
        sections += InterpretationSection(
            title = "关于这个结果",
            body = buildString {
                append("四维类型把连续的倾向切成两类，落在中间的人结果本就不稳定。")
                append("有研究显示相当比例的人隔几周重测会得到不同类型 —— 这不是测错了，")
                append("而是这套框架本身的边界。")
                if (chart.hasTie) {
                    append("\n\n本次有维度恰好居中（平局按第一极计），该维度尤其不宜当真。")
                }
                append("\n\n把它当作自我观察的一个角度，而不是关于你的定论。")
            },
            tone = Tone.NEUTRAL,
        )

        return Interpretation(
            verdict = Verdict.NEUTRAL,
            headline = "${type.code} · ${type.label}",
            sections = sections,
            tier = tier,
        )
    }

    private fun poleLabel(letter: Char): String = when (letter) {
        'E' -> "外向"
        'I' -> "内向"
        'S' -> "实感"
        'N' -> "直觉"
        'T' -> "思考"
        'F' -> "情感"
        'J' -> "判断"
        'P' -> "知觉"
        else -> letter.toString()
    }
}
