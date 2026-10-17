package com.maohongyuan.magic.core.xiaoliuren

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
 * 小六壬的 Tier1 规则引擎。
 *
 * 纯规则、无随机、无网络 —— 同样的三宫落点永远得到同样的结论，可复现。
 *
 * ── 为什么引擎放在本模块而不是 core:interpret ──
 * `core:interpret` 若依赖每个系统模块，就会变成「上帝模块」；
 * 各系统自带引擎、只依赖 `core:interpret` 的接口，方向才是单向的。
 */
class XiaoLiuRenRuleEngine : DivinationInterpreter {

    override val tier: EngineTier = EngineTier.RULE

    override suspend fun isAvailable(): Boolean = true

    override fun interpret(request: InterpretationRequest): Flow<Interpretation> {
        // 取不到盘就返回空流，让 InterpreterChain 继续降级 —— 绝不抛异常
        val chart = request.chart as? XiaoLiuRenChart ?: return flowOf()
        return flowOf(analyze(chart))
    }

    private fun analyze(chart: XiaoLiuRenChart): Interpretation {
        val third = chart.third
        val sections = mutableListOf<InterpretationSection>()

        // ── 三宫 ──
        sections += InterpretationSection(
            title = "三宫落点",
            body = buildString {
                append("${stageLabel(chart, 0)}落「${chart.first.name}」，")
                append("${stageLabel(chart, 1)}落「${chart.second.name}」，")
                append("${stageLabel(chart, 2)}落「${chart.third.name}」。")
                append("\n三宫属${chart.first.wuXing}、${chart.second.wuXing}、${chart.third.wuXing}。")
            },
            tone = Tone.NEUTRAL,
        )

        // ── 结果宫（断事以第三宫为主）──
        sections += InterpretationSection(
            title = "结果 · ${third.name}",
            body = "${third.meaning}\n${third.advice}",
            tone = toneOf(third.verdict),
        )

        // ── 过程宫 ──
        sections += InterpretationSection(
            title = "过程 · ${chart.second.name}",
            body = chart.second.meaning,
            tone = toneOf(chart.second.verdict),
        )

        // ── 起因宫 ──
        sections += InterpretationSection(
            title = "起因 · ${chart.first.name}",
            body = chart.first.meaning,
            tone = toneOf(chart.first.verdict),
        )

        // ── 三宫是否一致 ──
        val verdicts = listOf(chart.first, chart.second, chart.third).map { it.verdict }
        val consistent = verdicts.toSet().size == 1
        if (!consistent) {
            sections += InterpretationSection(
                title = "三宫合参",
                body = "三宫吉凶不一（${verdicts.joinToString("、")}），" +
                    "事情会有反复，不宜只凭一时的顺逆下判断。" +
                    "以结果宫「${third.name}」为主，过程宫的「${chart.second.name}」说明阻力的来源。",
                tone = Tone.WARNING,
            )
        } else {
            sections += InterpretationSection(
                title = "三宫合参",
                body = "三宫同为「${verdicts.first()}」，事情前后一致，走势明确。",
                tone = if (third.verdict == "吉") Tone.POSITIVE else Tone.NEGATIVE,
            )
        }

        // ── 方位与颜色（传统用法，供参考）──
        sections += InterpretationSection(
            title = "方位 · 颜色",
            body = "结果宫在${third.direction}，宜用${third.color}色。",
            tone = Tone.NEUTRAL,
        )

        return Interpretation(
            verdict = overallVerdict(verdicts),
            headline = buildHeadline(chart),
            sections = sections,
            tier = tier,
        )
    }

    private fun buildHeadline(chart: XiaoLiuRenChart): String =
        "结果落「${chart.third.name}」（${chart.third.verdict}）"

    /** 三宫吉凶 → 总判 */
    private fun overallVerdict(verdicts: List<String>): Verdict {
        val allGood = verdicts.all { it == "吉" }
        val allBad = verdicts.all { it != "吉" }
        return when {
            allGood -> Verdict.AUSPICIOUS
            allBad -> Verdict.INAUSPICIOUS
            else -> Verdict.MIXED
        }
    }

    private fun toneOf(verdict: String): Tone = when (verdict) {
        "吉" -> Tone.POSITIVE
        "大凶" -> Tone.NEGATIVE
        "凶" -> Tone.WARNING
        else -> Tone.NEUTRAL
    }

    /** 时间起课说「月/日/时」，报数起课说「一/二/三」 */
    private fun stageLabel(chart: XiaoLiuRenChart, index: Int): String = when (chart.method) {
        XiaoLiuRenMethod.TIME -> listOf("月宫", "日宫", "时宫")[index]
        XiaoLiuRenMethod.NUMBER -> listOf("第一数", "第二数", "第三数")[index]
    }
}
