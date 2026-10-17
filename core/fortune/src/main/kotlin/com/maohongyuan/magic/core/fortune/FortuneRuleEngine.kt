package com.maohongyuan.magic.core.fortune

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
 * 日运 / 月运的 Tier1 引擎。
 *
 * 四维评分是**启发式映射**（十神 → 事业/财运/感情/健康），
 * 不是传统定论。所以段落里要把「为什么」写出来，让用户能判断合不合理，
 * 而不是只给一个分数。
 */
class FortuneRuleEngine : DivinationInterpreter {

    override val tier: EngineTier = EngineTier.RULE

    override suspend fun isAvailable(): Boolean = true

    override fun interpret(request: InterpretationRequest): Flow<Interpretation> {
        val chart = request.chart as? FortuneChart ?: return flowOf()
        return flowOf(analyze(chart))
    }

    private fun analyze(chart: FortuneChart): Interpretation {
        val sections = mutableListOf<InterpretationSection>()

        sections += InterpretationSection(
            title = "本${if (chart.period == FortunePeriod.DAILY) "日" else "月"}干支",
            body = "${chart.targetGanZhi}，相对日主 ${chart.dayMaster.label} 为「${chart.shiShen}」。",
            tone = Tone.NEUTRAL,
        )

        chart.scores.forEach { s ->
            sections += InterpretationSection(
                title = "${s.dimension.label} · ${s.level}（${s.score}）",
                body = s.reason,
                tone = when {
                    s.score >= 70 -> Tone.POSITIVE
                    s.score <= 30 -> Tone.WARNING
                    else -> Tone.NEUTRAL
                },
            )
        }

        if (chart.relations.isNotEmpty()) {
            sections += InterpretationSection(
                title = "与命局的互动",
                body = chart.relations.joinToString("\n") { "· 流${if (chart.period == FortunePeriod.DAILY) "日" else "月"}支$it" },
                tone = Tone.NEUTRAL,
            )
        }

        // 方法说明 —— 这一条必须写，因为四维评分是工程化处理，不是传统断法
        sections += InterpretationSection(
            title = "这套评分是怎么来的",
            body = "先取流${if (chart.period == FortunePeriod.DAILY) "日" else "月"}干支相对你日主的十神，" +
                "再按十神对各维度的影响加减分（基准 50 分），最后叠加流${if (chart.period == FortunePeriod.DAILY) "日" else "月"}支" +
                "与命局四支的冲、合、害。\n\n" +
                "这套映射是便于参考的工程化处理，不是传统断法里的定论。" +
                "分数高不等于万事大吉，低也不等于诸事不宜 —— 它只说明这个时段的倾向。",
            tone = Tone.NEUTRAL,
        )

        val avg = chart.scores.map { it.score }.average().toInt()
        return Interpretation(
            verdict = when {
                avg >= 65 -> Verdict.AUSPICIOUS
                avg <= 35 -> Verdict.INAUSPICIOUS
                chart.scores.any { it.score >= 70 } && chart.scores.any { it.score <= 30 } -> Verdict.MIXED
                else -> Verdict.NEUTRAL
            },
            headline = "${chart.dateLabel} · ${chart.shiShen}当值",
            sections = sections,
            tier = tier,
        )
    }
}
