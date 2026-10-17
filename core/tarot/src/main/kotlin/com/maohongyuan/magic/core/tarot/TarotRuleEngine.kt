package com.maohongyuan.magic.core.tarot

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
 * 塔罗的 Tier1 规则引擎。
 *
 * 塔罗本身不判吉凶 —— 它讲的是「牌面呈现了什么」。所以 [Verdict] 用 NEUTRAL
 * 或 MIXED，靠段落的 tone 表达倾向，而不是硬套红绿吉凶配色。
 */
class TarotRuleEngine : DivinationInterpreter {

    override val tier: EngineTier = EngineTier.RULE

    override suspend fun isAvailable(): Boolean = true

    override fun interpret(request: InterpretationRequest): Flow<Interpretation> {
        val chart = request.chart as? TarotChart ?: return flowOf()
        return flowOf(analyze(chart))
    }

    private fun analyze(chart: TarotChart): Interpretation {
        val sections = mutableListOf<InterpretationSection>()

        // ── 逐张牌 ──
        chart.drawn.forEach { d ->
            sections += InterpretationSection(
                title = "${d.positionName} · ${d.card.name}（${d.orientationLabel}）",
                body = d.meaning,
                tone = if (d.reversed) Tone.WARNING else Tone.NEUTRAL,
            )
        }

        // ── 整体基调 ──
        val total = chart.drawn.size
        val rev = chart.reversedCount
        val revRatio = if (total == 0) 0.0 else rev.toDouble() / total
        val tone = when {
            revRatio == 0.0 -> Tone.POSITIVE
            revRatio >= 0.6 -> Tone.WARNING
            else -> Tone.NEUTRAL
        }
        sections += InterpretationSection(
            title = "整体基调",
            body = buildString {
                append("$total 张牌中 $rev 张逆位。")
                append(
                    when {
                        rev == 0 -> "全部正位，牌面呈现的是较顺畅的一面，能量流动没有明显阻碍。"
                        revRatio >= 0.6 -> "逆位占多数，提示当下有较多未展开或被压住的部分，" +
                            "事情可能卡在某处，宜先处理内在的阻力。"
                        else -> "正逆参半，事情有推有阻，属常态；不必过度解读单张逆位。"
                    }
                )
            },
            tone = tone,
        )

        // ── 大阿卡纳占比：重大议题的提示 ──
        val majorCount = chart.drawn.count { it.card.isMajor }
        if (majorCount > 0) {
            sections += InterpretationSection(
                title = "大阿卡纳",
                body = "抽到 $majorCount 张大阿卡纳。" +
                    if (majorCount * 2 >= total) {
                        "占比偏高，通常意味着这件事牵涉较深的、长期性的议题，不只是当下的琐事。"
                    } else {
                        "占比不高，这件事更多落在日常层面，宜就事论事。"
                    },
                tone = Tone.NEUTRAL,
            )
        }

        // ── 花色分布 ──
        val suitCounts = Suit.entries
            .mapNotNull { s -> chart.drawn.count { it.card.suitEnum == s }.takeIf { it > 0 }?.let { s to it } }
        if (suitCounts.isNotEmpty()) {
            sections += InterpretationSection(
                title = "花色分布",
                body = suitCounts.joinToString("、") { (s, n) -> "${s.label}(${s.element}) $n 张" } +
                    "。\n" + suitHint(suitCounts.maxByOrNull { it.second }?.first),
                tone = Tone.NEUTRAL,
            )
        }

        return Interpretation(
            verdict = if (revRatio >= 0.6) Verdict.MIXED else Verdict.NEUTRAL,
            headline = buildHeadline(chart),
            sections = sections,
            tier = tier,
        )
    }

    private fun buildHeadline(chart: TarotChart): String {
        val names = chart.drawn.take(3).joinToString("、") { it.card.name }
        return if (chart.drawn.size <= 3) names else "$names 等 ${chart.drawn.size} 张"
    }

    /** 出现最多的花色 → 这件事主要落在哪个层面 */
    private fun suitHint(suit: Suit?): String = when (suit) {
        Suit.WANDS -> "权杖偏多，重点在行动与意志：这件事更看你愿不愿意去做。"
        Suit.CUPS -> "圣杯偏多，重点在情感与关系：这件事的核心是感受，不是道理。"
        Suit.SWORDS -> "宝剑偏多，重点在思虑与言语：容易想太多，也容易因话说得不妥而起波折。"
        Suit.PENTACLES -> "星币偏多，重点在现实与物质：钱、身体、具体条件会是关键。"
        null -> "各花色分布均衡，没有特别偏重的层面。"
    }
}
