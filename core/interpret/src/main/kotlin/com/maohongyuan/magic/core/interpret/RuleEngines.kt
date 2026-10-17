package com.maohongyuan.magic.core.interpret

import com.maohongyuan.magic.core.calendar.BaZiChart
import com.maohongyuan.magic.core.hexagram.MeihuaChart
import com.maohongyuan.magic.core.hexagram.WangShuai
import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.Trigram
import com.maohongyuan.magic.core.model.WuXing
import com.maohongyuan.magic.core.model.WuXingRelation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Tier1 梅花易数规则引擎。
 *
 * 梅花只看三件事：**体用生克、互卦、变卦**。比六爻简洁得多，
 * 因此规则引擎能做到相当可靠。
 *
 * 断法：
 * 1. 体用生克 —— 主断，权重最大
 * 2. 体卦旺衰 —— 由月建定
 * 3. 用卦旺衰 —— 用旺则体受制
 * 4. 变卦 —— 结果
 * 5. 互卦 —— 中间过程
 */
class MeihuaRuleEngine : DivinationInterpreter {

    override val tier: EngineTier = EngineTier.RULE

    override suspend fun isAvailable(): Boolean = true

    override fun interpret(request: InterpretationRequest): Flow<Interpretation> {
        val chart = request.chart as? MeihuaChart ?: return flowOf()
        return flowOf(analyze(chart, request.monthBranch))
    }

    private fun analyze(chart: MeihuaChart, monthBranch: EarthlyBranch?): Interpretation {
        val notes = mutableListOf<Pair<String, Tone>>()
        var score = 0

        fun add(delta: Int, text: String, tone: Tone) {
            score += delta
            notes += text to tone
        }

        // ── 1. 体用生克（主断）──
        when (chart.relation) {
            WuXingRelation.IT_GENERATES -> {
                add(3, "用生体 —— 事来就我，不劳而获", Tone.POSITIVE)
            }
            WuXingRelation.SAME -> {
                add(2, "体用比和 —— 彼此同气，事顺", Tone.POSITIVE)
            }
            WuXingRelation.I_OVERCOME -> {
                add(2, "体克用 —— 事在掌握，但需主动费力", Tone.POSITIVE)
            }
            WuXingRelation.I_GENERATE -> {
                add(-2, "体生用 —— 我去就事，耗泄自身", Tone.NEGATIVE)
            }
            WuXingRelation.IT_OVERCOMES -> {
                add(-3, "用克体 —— 事来伤我，受制于人", Tone.NEGATIVE)
            }
        }

        // ── 2 & 3. 旺衰（需月建，缺失则跳过并说明）──
        if (monthBranch != null) {
            val tiWangShuai = WangShuai.of(chart.tiWuXing, monthBranch)
            when (tiWangShuai) {
                WangShuai.WANG -> add(2, "体卦${chart.tiWuXing}当令而旺，自身有力", Tone.POSITIVE)
                WangShuai.XIANG -> add(1, "体卦${chart.tiWuXing}得月生而相", Tone.POSITIVE)
                WangShuai.XIU -> add(0, "体卦${chart.tiWuXing}休于月建", Tone.NEUTRAL)
                WangShuai.QIU -> add(-1, "体卦${chart.tiWuXing}囚于月建，自身偏弱", Tone.WARNING)
                WangShuai.SI -> add(-2, "体卦${chart.tiWuXing}死于月建，自身无力", Tone.NEGATIVE)
            }

            val yongWangShuai = WangShuai.of(chart.yongWuXing, monthBranch)
            when (yongWangShuai) {
                WangShuai.WANG -> add(-1, "用卦${chart.yongWuXing}当令而旺，来势较盛", Tone.WARNING)
                WangShuai.SI, WangShuai.QIU -> add(1, "用卦${chart.yongWuXing}衰于月建，其势已弱", Tone.POSITIVE)
                else -> Unit
            }
        } else {
            notes += "未提供月建，本次不做旺衰判断（结论仅据体用与变卦）" to Tone.WARNING
        }

        // ── 4. 变卦（结果）──
        val changedTrigram: Trigram =
            if (chart.movingIndex < 3) chart.changed.lower else chart.changed.upper
        when (WuXingRelation.between(changedTrigram.wuXing, chart.tiWuXing)) {
            WuXingRelation.I_GENERATE ->
                add(2, "变卦${changedTrigram.label}（${changedTrigram.wuXing}）生体，结局向吉", Tone.POSITIVE)
            WuXingRelation.I_OVERCOME ->
                add(-2, "变卦${changedTrigram.label}（${changedTrigram.wuXing}）克体，结局不利", Tone.NEGATIVE)
            WuXingRelation.SAME ->
                add(1, "变卦与体同气，结局平稳", Tone.POSITIVE)
            WuXingRelation.IT_GENERATES ->
                add(-1, "体生变卦，结局耗力", Tone.WARNING)
            WuXingRelation.IT_OVERCOMES ->
                add(1, "体克变卦，结局可制", Tone.POSITIVE)
        }

        val verdict = when {
            score >= 5 -> Verdict.AUSPICIOUS
            score <= -5 -> Verdict.INAUSPICIOUS
            score == 0 -> Verdict.NEUTRAL
            else -> Verdict.MIXED
        }

        val headline = when (verdict) {
            Verdict.AUSPICIOUS -> "${chart.relationLabel}，体气得力，事可成"
            Verdict.INAUSPICIOUS -> "${chart.relationLabel}，体气受制，事难谐"
            Verdict.NEUTRAL -> "体用相抵，吉凶在人"
            Verdict.MIXED -> if (score > 0) "吉中带阻，可为但需用心" else "凶中有救，尚可挽回"
        }

        val sections = buildList {
            add(
                InterpretationSection(
                    title = "体用",
                    body = "动爻${chart.movingTitle}在${if (chart.movingIndex < 3) "下卦" else "上卦"}，" +
                        "故以 ${chart.yongTrigram.label}（${chart.yongWuXing}）为用、" +
                        "${chart.tiTrigram.label}（${chart.tiWuXing}）为体。" +
                        "体用关系为「${chart.relationLabel}」。",
                    tone = if (chart.favorable) Tone.POSITIVE else Tone.NEGATIVE,
                )
            )
            add(
                InterpretationSection(
                    title = "卦象",
                    body = "本卦 ${chart.original.name}${chart.original.symbol}" +
                        " · 互卦 ${chart.nuclear.name}${chart.nuclear.symbol}" +
                        " · 变卦 ${chart.changed.name}${chart.changed.symbol}",
                    tone = Tone.NEUTRAL,
                )
            )
            add(
                InterpretationSection(
                    title = "互卦",
                    body = "互卦为 ${chart.nuclear.name}，主事情发展过程中的中间状态 —— " +
                        "${describeNuclear(chart)}",
                    tone = Tone.NEUTRAL,
                )
            )
            add(
                InterpretationSection(
                    title = "综合",
                    body = notes.joinToString("；") + "。",
                    tone = when (verdict) {
                        Verdict.AUSPICIOUS -> Tone.POSITIVE
                        Verdict.INAUSPICIOUS -> Tone.NEGATIVE
                        Verdict.NEUTRAL -> Tone.NEUTRAL
                        Verdict.MIXED -> Tone.WARNING
                    },
                )
            )
        }

        return Interpretation(
            verdict = verdict,
            headline = headline,
            sections = sections,
            tier = EngineTier.RULE,
            yongShenNote = "体${chart.tiTrigram.label} · 用${chart.yongTrigram.label}",
        )
    }

    /** 互卦的取象说明。互卦上卦与体卦的生克，反映中间过程是否顺遂。 */
    private fun describeNuclear(chart: MeihuaChart): String {
        // 以体为我，看互卦上卦与体的关系
        val relation = WuXingRelation.between(chart.tiWuXing, chart.nuclear.upper.wuXing)
        return when (relation) {
            WuXingRelation.IT_GENERATES ->
                "互卦上卦生体，中间过程较为顺遂"
            WuXingRelation.SAME ->
                "互卦上卦与体同气，中间过程平顺"
            WuXingRelation.I_OVERCOME ->
                "互卦上卦为体所克，中间过程虽有阻力但可克服"
            WuXingRelation.I_GENERATE ->
                "互卦上卦泄体之气，中间过程较为耗费"
            WuXingRelation.IT_OVERCOMES ->
                "互卦上卦克体，中间过程多阻，须防中途生变"
        }
    }
}

/**
 * Tier1 八字规则引擎。
 *
 * ⚠️ **本引擎只做最基础的格局判断**，不做专业的用神取用与格局成败分析 ——
 * 那需要大量流派知识与个案判断，规则引擎给不出可靠结论。
 * 因此输出刻意保守，只陈述**客观事实**（日主、五行分布、偏枯与否），
 * 并明确提示用户这不构成命理推断。
 */
class BaZiRuleEngine : DivinationInterpreter {

    override val tier: EngineTier = EngineTier.RULE

    override suspend fun isAvailable(): Boolean = true

    override fun interpret(request: InterpretationRequest): Flow<Interpretation> {
        val chart = request.chart as? BaZiChart ?: return flowOf()
        return flowOf(analyze(chart))
    }

    private fun analyze(chart: BaZiChart): Interpretation {
        val dm = chart.dayMaster
        val dmWuXing = dm.wuXing

        // 同我 + 生我 为「帮身」，其余为「耗身」
        val support = chart.wuXingCount.getValue(dmWuXing) +
            chart.wuXingCount.getValue(dmWuXing.generatedBy)
        val drain = 8 - support

        val ranked = chart.wuXingRanked
        val strongest = ranked.first()
        val weakest = ranked.last()
        val missing = chart.wuXingCount.filter { it.value == 0 }.keys

        val verdict = when {
            support >= 6 -> Verdict.MIXED
            support <= 2 -> Verdict.MIXED
            else -> Verdict.NEUTRAL
        }

        val headline = when {
            support >= 6 -> "日主偏旺（帮身 $support / 8）"
            support <= 2 -> "日主偏弱（帮身 $support / 8）"
            else -> "日主中和（帮身 $support / 8）"
        }

        val sections = buildList {
            add(
                InterpretationSection(
                    title = "日主",
                    body = "日主 ${dm.label}（$dmWuXing，${dm.yinYang.label}）。" +
                        "四柱为 ${chart.eightCharLabel}。",
                    tone = Tone.NEUTRAL,
                )
            )
            add(
                InterpretationSection(
                    title = "五行分布",
                    body = chart.wuXingRanked.joinToString("、") { (wx, n) -> "$wx $n" } +
                        "。最多者为 ${strongest.first}（${strongest.second}），" +
                        "最少者为 ${weakest.first}（${weakest.second}）。" +
                        if (missing.isEmpty()) "" else "缺 ${missing.joinToString("、")}。",
                    tone = Tone.NEUTRAL,
                )
            )
            add(
                InterpretationSection(
                    title = "帮身与耗身",
                    body = "生我者（${dmWuXing.generatedBy}）与同我者（$dmWuXing）合计 $support 个，" +
                        "其余 $drain 个。",
                    tone = when {
                        support >= 6 || support <= 2 -> Tone.WARNING
                        else -> Tone.NEUTRAL
                    },
                )
            )
            add(
                InterpretationSection(
                    title = "大运",
                    body = "${if (chart.forward) "顺" else "逆"}行，" +
                        "${chart.startAge} 岁起运。首三步：" +
                        chart.daYun.take(3).joinToString("、") { "${it.startAge}岁 ${it.ganZhi}" } + "。",
                    tone = Tone.NEUTRAL,
                )
            )
            add(
                InterpretationSection(
                    title = "说明",
                    body = "以上仅为四柱的客观排布与五行统计，**不含用神取用与格局成败判断**。" +
                        "命理分析流派众多、个案差异极大，本引擎不做此类推断。",
                    tone = Tone.WARNING,
                )
            )
        }

        return Interpretation(
            verdict = verdict,
            headline = headline,
            sections = sections,
            tier = EngineTier.RULE,
            yongShenNote = "日主 ${dm.label}（$dmWuXing）",
        )
    }
}
