package com.maohongyuan.magic.core.interpret

import com.maohongyuan.magic.core.hexagram.LiuYaoChart
import com.maohongyuan.magic.core.hexagram.LiuYaoLine
import com.maohongyuan.magic.core.hexagram.WangShuai
import com.maohongyuan.magic.core.model.LiuQin
import com.maohongyuan.magic.core.model.WuXingRelation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 所问事类 —— 决定用神取哪个六亲。
 *
 * 用神取错，整卦皆错。所以这里是断卦的第一步，也是最重要的一步。
 */
enum class QuestionCategory(val label: String, val yongShen: LiuQin?) {
    CAREER("事业", LiuQin.GUAN_GUI),
    WEALTH("财运", LiuQin.QI_CAI),
    MARRIAGE("婚姻", LiuQin.QI_CAI),
    HEALTH("疾病", LiuQin.GUAN_GUI),
    FAMILY("长辈·文书", LiuQin.FU_MU),
    CHILDREN("子女", LiuQin.ZI_SUN),
    PEER("朋友·竞争", LiuQin.XIONG_DI),
    TRAVEL("出行", LiuQin.FU_MU),

    /** 无法归类时以**世爻**为用神 */
    GENERAL("综合", null),
    ;

    companion object {
        /**
         * 由问题文本推测事类。
         *
         * 命中顺序即优先级：越具体的关键词越靠前，
         * 例如「考试」属事业而非文书，「房产」属家宅而非财运。
         */
        private val KEYWORDS: List<Pair<QuestionCategory, List<String>>> = listOf(
            CAREER to listOf(
                "事业", "工作", "升职", "求职", "面试", "考试", "功名", "前程",
                "职位", "官运", "调动", "晋升", "创业", "考公", "考研",
            ),
            HEALTH to listOf(
                "疾病", "健康", "身体", "手术", "痊愈", "生病", "病情", "康复", "医",
            ),
            MARRIAGE to listOf(
                "婚姻", "感情", "对象", "恋爱", "姻缘", "复合", "分手", "配偶", "结婚", "脱单",
            ),
            CHILDREN to listOf(
                "子女", "孩子", "怀孕", "生育", "下属", "学生", "子嗣",
            ),
            TRAVEL to listOf(
                "出行", "搬家", "旅行", "迁移", "出国", "远行", "迁居",
            ),
            FAMILY to listOf(
                "父母", "长辈", "家宅", "房子", "房产", "文书", "合同", "证件", "签约",
            ),
            PEER to listOf(
                "朋友", "兄弟", "竞争", "合伙", "对手", "同事",
            ),
            WEALTH to listOf(
                "财", "钱", "生意", "投资", "买卖", "收入", "盈利", "亏损", "债", "股",
            ),
        )

        fun detect(question: String): QuestionCategory =
            KEYWORDS.firstOrNull { (_, words) -> words.any { question.contains(it) } }?.first
                ?: GENERAL
    }
}

/**
 * Tier1 六爻规则引擎。
 *
 * **纯规则、零网络、零模型**，是整条降级链的终点，因此必须永远可用。
 *
 * 断卦七步：
 * 1. 定用神（由事类决定取哪个六亲）
 * 2. 看用神旺衰（月建）
 * 3. 看日辰生克冲合
 * 4. 看动爻对用神的生克
 * 5. 看变爻回头生克
 * 6. 看用神与世爻的关系
 * 7. 看旬空
 *
 * 打分而非「命中即断」：单一征象不足以定吉凶，必须合参。
 * 分值映射到 [Verdict]，并在 [Interpretation.sections] 里逐条给出依据，
 * 用户可以看到「为什么是这个结论」。
 */
class LiuYaoRuleEngine : DivinationInterpreter {

    override val tier: EngineTier = EngineTier.RULE

    override suspend fun isAvailable(): Boolean = true

    override fun interpret(request: InterpretationRequest): Flow<Interpretation> {
        val chart = request.chart as? LiuYaoChart ?: return flowOf()
        return flowOf(analyze(request.question, chart))
    }

    private fun analyze(question: String, chart: LiuYaoChart): Interpretation {
        val category = QuestionCategory.detect(question)
        val notes = mutableListOf<Pair<String, Tone>>()
        var score = 0

        fun add(delta: Int, text: String, tone: Tone) {
            score += delta
            notes += text to tone
        }

        // ── 1. 定用神 ──
        val yongLine: LiuYaoLine?
        val yongLabel: String
        val yongWuXing: com.maohongyuan.magic.core.model.WuXing
        var viaFuShen = false

        if (category.yongShen == null) {
            // 综合类以世爻为用神
            yongLine = chart.shiLine
            yongLabel = "世爻"
            yongWuXing = yongLine.naJia.wuXing
        } else {
            val direct = chart.linesOf(category.yongShen)
            if (direct.isNotEmpty()) {
                yongLine = direct.first()
                yongLabel = category.yongShen.label
                yongWuXing = yongLine.naJia.wuXing
            } else {
                // 用神不上卦 —— 取伏神
                val fu = chart.fuShenList.firstOrNull { it.liuQin == category.yongShen }
                yongLine = fu?.let { chart.lines[it.underIndex] }
                yongLabel = category.yongShen.label
                yongWuXing = fu?.naJia?.wuXing ?: com.maohongyuan.magic.core.model.WuXing.TU
                viaFuShen = fu != null
            }
        }

        val yongTitle = when {
            viaFuShen -> "伏神${yongLabel}"
            else -> "${yongLine?.title ?: "—"}（$yongLabel）"
        }

        notes += (
            "所问事类判为「${category.label}」，用神取 $yongLabel" +
                if (viaFuShen) "（不上卦，取伏神）" else "（${yongLine?.naJia?.label ?: ""}）"
            ) to Tone.NEUTRAL

        if (viaFuShen) {
            add(-1, "用神伏藏不现，主事情隐晦难显，需待日月生扶方可出头", Tone.WARNING)
        }

        // ── 2. 用神旺衰（月建） ──
        val wangShuai = WangShuai.of(yongWuXing, chart.monthBranch)
        when (wangShuai) {
            WangShuai.WANG -> add(2, "用神${yongWuXing}当令而旺，气盛有力", Tone.POSITIVE)
            WangShuai.XIANG -> add(1, "用神${yongWuXing}得月建所生而相，有气", Tone.POSITIVE)
            WangShuai.XIU -> add(0, "用神${yongWuXing}休于月建，气平", Tone.NEUTRAL)
            WangShuai.QIU -> add(-1, "用神${yongWuXing}囚于月建，气弱", Tone.WARNING)
            WangShuai.SI -> add(-2, "用神${yongWuXing}死于月建，气衰无力", Tone.NEGATIVE)
        }

        // ── 3. 日辰生克冲 ──
        val dayBranch = chart.dayGanZhi.branch
        when (WuXingRelation.between(dayBranch.wuXing, yongWuXing)) {
            WuXingRelation.I_GENERATE ->
                add(1, "日辰${dayBranch.label}（${dayBranch.wuXing}）生用神，得日辰之助", Tone.POSITIVE)
            WuXingRelation.I_OVERCOME ->
                add(-1, "日辰${dayBranch.label}（${dayBranch.wuXing}）克用神，受日辰之制", Tone.NEGATIVE)
            WuXingRelation.SAME ->
                add(1, "用神与日辰同气（${dayBranch.wuXing}），得比助", Tone.POSITIVE)
            WuXingRelation.IT_GENERATES ->
                add(-1, "用神生日辰，气泄于外", Tone.WARNING)
            WuXingRelation.IT_OVERCOMES ->
                add(1, "用神克日辰，虽出力但能制之", Tone.POSITIVE)
        }

        if (yongLine != null && dayBranch == yongLine.naJia.branch.clash) {
            add(-1, "日辰${dayBranch.label}冲用神${yongLine.naJia.branch.label}，主事有变动反复", Tone.WARNING)
        }
        if (yongLine != null && dayBranch == yongLine.naJia.branch.combine) {
            add(1, "日辰${dayBranch.label}合住用神，主事情黏着难速成", Tone.WARNING)
        }

        // ── 4 & 5. 动爻与变爻 ──
        val moving = chart.lines.filter { it.moving }
        if (moving.isEmpty()) {
            notes += "卦中无动爻，为静卦 —— 事情维持现状，无甚变数" to Tone.NEUTRAL
        } else {
            moving.forEach { line ->
                when (WuXingRelation.between(line.naJia.wuXing, yongWuXing)) {
                    WuXingRelation.I_GENERATE ->
                        add(1, "${line.title}（${line.liuQin.label}${line.naJia.label}）动而生用神", Tone.POSITIVE)
                    WuXingRelation.I_OVERCOME ->
                        add(-1, "${line.title}（${line.liuQin.label}${line.naJia.label}）动而克用神", Tone.NEGATIVE)
                    else -> Unit
                }
                // 变爻回头生克
                line.bian?.let { bian ->
                    when (WuXingRelation.between(bian.naJia.wuXing, yongWuXing)) {
                        WuXingRelation.I_GENERATE ->
                            add(1, "${line.title} 变出 ${bian.liuQin.label}${bian.naJia.label}，回头生用神", Tone.POSITIVE)
                        WuXingRelation.I_OVERCOME ->
                            add(-1, "${line.title} 变出 ${bian.liuQin.label}${bian.naJia.label}，回头克用神", Tone.NEGATIVE)
                        else -> Unit
                    }
                }
            }
        }

        // ── 6. 用神与世爻 ──
        val shi = chart.shiLine
        if (yongLine != null && yongLine.index != shi.index) {
            when (WuXingRelation.between(yongWuXing, shi.naJia.wuXing)) {
                WuXingRelation.I_GENERATE ->
                    add(2, "用神生世爻 —— 事来就我，主得利", Tone.POSITIVE)
                WuXingRelation.I_OVERCOME ->
                    add(-2, "用神克世爻 —— 事来伤我，主受制", Tone.NEGATIVE)
                WuXingRelation.IT_GENERATES ->
                    add(-1, "世爻生用神 —— 我去就事，主耗力", Tone.WARNING)
                WuXingRelation.IT_OVERCOMES ->
                    add(1, "世爻克用神 —— 事在我掌握之中", Tone.POSITIVE)
                WuXingRelation.SAME -> add(0, "世爻与用神同气，彼此相应", Tone.NEUTRAL)
            }
        }

        // ── 7. 旬空 ──
        if (yongLine != null && yongLine.kong) {
            add(-2, "用神落旬空（${chart.xunKong.label}）—— 事多虚而不实，或须待出空", Tone.NEGATIVE)
        }
        if (shi.kong) {
            add(-1, "世爻落旬空 —— 自身立场不稳，宜守不宜进", Tone.WARNING)
        }

        // ── 综合 ──
        val verdict = when {
            score >= 4 -> Verdict.AUSPICIOUS
            score <= -4 -> Verdict.INAUSPICIOUS
            score == 0 -> Verdict.NEUTRAL
            else -> Verdict.MIXED
        }

        val headline = when (verdict) {
            Verdict.AUSPICIOUS -> "用神有力，事可成，宜进"
            Verdict.INAUSPICIOUS -> "用神受制，事难成，宜守"
            Verdict.NEUTRAL -> "吉凶相抵，事在人为"
            Verdict.MIXED -> if (score > 0) "吉中带阻，可成但需费力" else "凶中有救，尚可转圜"
        }

        val sections = buildList {
            add(
                InterpretationSection(
                    title = "用神",
                    body = "所问「${category.label}」，以 $yongLabel 为用神，取 $yongTitle。",
                    tone = Tone.NEUTRAL,
                )
            )
            add(
                InterpretationSection(
                    title = "旺衰",
                    body = "用神属${yongWuXing}，生于${chart.monthBranch.label}月，处「${wangShuai.label}」。",
                    tone = when (wangShuai) {
                        WangShuai.WANG, WangShuai.XIANG -> Tone.POSITIVE
                        WangShuai.XIU -> Tone.NEUTRAL
                        else -> Tone.NEGATIVE
                    },
                )
            )
            add(
                InterpretationSection(
                    title = "世应",
                    body = "世爻${shi.title}（${shi.liuQin.label}${shi.naJia.label}），" +
                        "应爻${chart.yingLine.title}（${chart.yingLine.liuQin.label}${chart.yingLine.naJia.label}）。" +
                        "${chart.palace.summary}。",
                    tone = Tone.NEUTRAL,
                )
            )
            add(
                InterpretationSection(
                    title = "六神",
                    body = "用神临${yongLine?.liuShen?.label ?: "—"}。" + LIU_SHEN_HINT[yongLine?.liuShen],
                    tone = Tone.NEUTRAL,
                )
            )
            add(
                InterpretationSection(
                    title = "综合",
                    body = notes.joinToString("；") { it.first } + "。",
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
            yongShenNote = "$yongLabel · ${chart.palace.summary}",
        )
    }

    private companion object {
        /** 六神断语的常见取象，用于给结果补一句辅助说明。 */
        val LIU_SHEN_HINT = mapOf(
            com.maohongyuan.magic.core.model.LiuShen.QING_LONG to "青龙主喜庆、正财、正面之事。",
            com.maohongyuan.magic.core.model.LiuShen.ZHU_QUE to "朱雀主口舌、文书、信息往来。",
            com.maohongyuan.magic.core.model.LiuShen.GOU_CHEN to "勾陈主田土、迟滞、牵连。",
            com.maohongyuan.magic.core.model.LiuShen.TENG_SHE to "腾蛇主虚惊、缠绕、反覆。",
            com.maohongyuan.magic.core.model.LiuShen.BAI_HU to "白虎主凶险、伤病、刚猛之事。",
            com.maohongyuan.magic.core.model.LiuShen.XUAN_WU to "玄武主暗昧、盗失、隐情。",
        )
    }
}
