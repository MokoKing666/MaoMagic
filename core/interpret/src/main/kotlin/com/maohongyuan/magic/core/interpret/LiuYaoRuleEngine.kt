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
                "婚姻", "感情", "情感", "爱情", "对象", "恋爱", "姻缘", "复合", "分手",
                "配偶", "结婚", "脱单", "喜欢", "表白", "相亲", "暧昧", "暗恋",
                "前任", "复合", "和好", "吵架", "冷战", "老公", "老婆", "男友", "女友",
                "男朋友", "女朋友", "伴侣", "夫妻", "婚姻",
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
                "财务", "工资", "薪资", "赚", "亏", "回本", "分红", "存钱", "理财",
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
        return flowOf(analyze(request.question, chart, request.gender))
    }

    private fun analyze(
        question: String,
        chart: LiuYaoChart,
        gender: com.maohongyuan.magic.core.calendar.Gender,
    ): Interpretation {
        val category = QuestionCategory.detect(question)
        // 婚姻/感情：男占看妻财，女占看官鬼。其余事类不分性别。
        val yongShen = if (category == QuestionCategory.MARRIAGE) {
            if (gender == com.maohongyuan.magic.core.calendar.Gender.MALE) {
                LiuQin.QI_CAI
            } else {
                LiuQin.GUAN_GUI
            }
        } else {
            category.yongShen
        }
        // 每个因子带权重：正数助成、负数阻成、零为中性补充。
        // 分开存是为了输出时能分成「有利 / 不利」两栏 —— 之前全塞一个分号串里，
        // 用户读到的是一堆并列的术语，看不出哪条在帮忙、哪条在拖后腿。
        val factors = mutableListOf<Factor>()
        var score = 0

        fun add(delta: Int, text: String, tone: Tone) {
            score += delta
            factors += Factor(delta, text, tone)
        }

        // ── 1. 定用神 ──
        val yongLine: LiuYaoLine?
        val yongLabel: String
        val yongWuXing: com.maohongyuan.magic.core.model.WuXing
        var viaFuShen = false

        if (yongShen == null) {
            // 综合类以世爻为用神
            yongLine = chart.shiLine
            yongLabel = "世爻"
            yongWuXing = yongLine.naJia.wuXing
        } else {
            val direct = chart.linesOf(yongShen)
            if (direct.isNotEmpty()) {
                yongLine = direct.first()
                yongLabel = yongShen.label
                yongWuXing = yongLine.naJia.wuXing
            } else {
                // 用神不上卦 —— 取伏神
                val fu = chart.fuShenList.firstOrNull { it.liuQin == yongShen }
                yongLine = fu?.let { chart.lines[it.underIndex] }
                yongLabel = yongShen.label
                yongWuXing = fu?.naJia?.wuXing ?: com.maohongyuan.magic.core.model.WuXing.TU
                viaFuShen = fu != null
            }
        }

        val yongTitle = when {
            viaFuShen -> "伏神${yongLabel}"
            else -> "${yongLine?.title ?: "—"}（$yongLabel）"
        }

        factors += Factor(
            0,
            "所问事类判为「${category.label}」，用神取 $yongLabel" +
                if (viaFuShen) "（不上卦，取伏神）" else "（${yongLine?.naJia?.label ?: ""}）",
            Tone.NEUTRAL,
        )

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
            factors += Factor(0, "卦中无动爻，为静卦 —— 事情维持现状，无甚变数", Tone.NEUTRAL)
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
            Verdict.AUSPICIOUS -> "事可为 —— 用神有力，阻碍不多"
            Verdict.INAUSPICIOUS -> "此事难成 —— 用神受制，宜缓不宜急"
            Verdict.NEUTRAL -> "吉凶参半 —— 成与不成，多半看你怎么做"
            Verdict.MIXED ->
                if (score > 0) "能成，但要费些力气 —— 有助力也有牵绊"
                else "眼下不顺，但有转机 —— 别急着放弃"
        }

        val helpful = factors.filter { it.weight > 0 }
        val harmful = factors.filter { it.weight < 0 }
        val neutral = factors.filter { it.weight == 0 }

        // 白话结论：把卦理翻成「这件事上你该注意什么」
        val advice = ADVICE[category] ?: ADVICE.getValue(QuestionCategory.GENERAL)
        // 结论按维度说 —— 用户问的是一件事，但想知道的是「各方面怎么样」
        val loveQin = if (gender == com.maohongyuan.magic.core.calendar.Gender.MALE) {
            LiuQin.QI_CAI
        } else {
            LiuQin.GUAN_GUI
        }
        val dimensions = DIMENSIONS.map { (liuQin, label, hint) ->
            val qin = if (label == "感情") loveQin else liuQin
            label to describeDimension(chart, qin, hint)
        }

        val conclusion = buildString {
            append(headline)
            append("。")
            append(
                when (verdict) {
                    Verdict.AUSPICIOUS -> "眼下是顺的，该推进就推进。"
                    Verdict.INAUSPICIOUS -> "眼下阻力大于助力，先别硬推 —— 缓一缓、换个时机再说。"
                    Verdict.NEUTRAL -> "没有明显推力也没有明显阻力，结果取决于你的做法。"
                    Verdict.MIXED -> "有助力也有牵绊，关键是把牵绊那条先处理掉。"
                }
            )
            if (verdict != Verdict.AUSPICIOUS) append(advice)
            // 应期提示：旬空与静卦最能说明「什么时候」
            if (yongLine != null && yongLine.kong) {
                append("用神落旬空（${chart.xunKong.label}），事多虚而不实；要等出空之后才见分晓。")
            }
            if (chart.lines.none { it.moving }) {
                append("卦中无动爻，事态维持现状，短期内不会有大的变化。")
            }
        }

        val sections = buildList {
            add(
                InterpretationSection(
                    title = "用神",
                    body = buildString {
                        append("你问的是「${category.label}」这类事，按六爻的取法，看 $yongLabel。")
                        append("\n")
                        append(
                            if (viaFuShen) {
                                "$yongLabel 没有上卦，只能从伏神里找 —— 事不明朗，需待时机。"
                            } else {
                                "$yongLabel 落在${yongLine?.title ?: "—"}（${yongLine?.naJia?.label ?: ""}）。"
                            }
                        )
                        append("\n")
                        append("六神临${yongLine?.liuShen?.label ?: "—"}：")
                        append(LIU_SHEN_HINT[yongLine?.liuShen] ?: "无特别取象。")
                    },
                    tone = Tone.NEUTRAL,
                )
            )
            add(
                InterpretationSection(
                    title = "旺衰",
                    body = "$yongLabel 属${yongWuXing}，生于${chart.monthBranch.label}月，处「${wangShuai.label}」。" +
                        when (wangShuai) {
                            WangShuai.WANG -> "月令正旺，它自己就有底气 —— 这是最有力的一档。"
                            WangShuai.XIANG -> "得月令所生，气是够的，虽不如当令那么强。"
                            WangShuai.XIU -> "月令不生不克，气平 —— 不算强，但也没被压。"
                            WangShuai.QIU -> "月令克它，气偏弱 —— 想成事得靠别处补。"
                            WangShuai.SI -> "月令克得最狠，气衰 —— 单靠它自己撑不起来。"
                        },
                    tone = when (wangShuai) {
                        WangShuai.WANG, WangShuai.XIANG -> Tone.POSITIVE
                        WangShuai.XIU -> Tone.NEUTRAL
                        else -> Tone.NEGATIVE
                    },
                )
            )
            if (helpful.isNotEmpty()) {
                add(
                    InterpretationSection(
                        title = "有利的一面",
                        body = helpful.joinToString("\n") { "· ${it.text}" },
                        tone = Tone.POSITIVE,
                    )
                )
            }
            if (harmful.isNotEmpty()) {
                add(
                    InterpretationSection(
                        title = "不利的一面",
                        body = harmful.joinToString("\n") { "· ${it.text}" },
                        tone = Tone.NEGATIVE,
                    )
                )
            }
            if (helpful.isEmpty() && harmful.isEmpty()) {
                add(
                    InterpretationSection(
                        title = "卦象",
                        body = "这一卦没有明显的生扶，也没有明显的克伤 —— 事态平稳，看不出强信号。",
                        tone = Tone.NEUTRAL,
                    )
                )
            }
            add(
                InterpretationSection(
                    title = "世应",
                    body = "世爻${shi.title}（${shi.liuQin.label}${shi.naJia.label}）代表你自己，" +
                        "应爻${chart.yingLine.title}（${chart.yingLine.liuQin.label}${chart.yingLine.naJia.label}）代表对方或外部。" +
                        "${chart.palace.summary}。",
                    tone = Tone.NEUTRAL,
                )
            )
            if (neutral.isNotEmpty()) {
                add(
                    InterpretationSection(
                        title = "补充",
                        body = neutral.joinToString("\n") { "· ${it.text}" },
                        tone = Tone.NEUTRAL,
                    )
                )
            }
            add(
                InterpretationSection(
                    title = "各方面",
                    body = dimensions.joinToString("\n") { (label, text) -> "【$label】$text" },
                    tone = Tone.NEUTRAL,
                )
            )
            add(
                InterpretationSection(
                    title = "结论",
                    body = conclusion,
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

    /**
     * 分维度解读。
     *
     * 用户问的是一件事，但真正想知道的是「各方面怎么样」——
     * 所以除了针对所问事类的用神分析，再按六亲给五个维度各一句话。
     *
     * ⚠️ 六爻古法讲「一事一占」，多问则卦不应。这里的分维度只是
     * **从同一卦里读出各方面的气象**，不是另起五卦，也不该被当成五次占卜的结论。
     */
    private fun describeDimension(chart: LiuYaoChart, liuQin: LiuQin, hint: List<String>): String {
        val lines = chart.linesOf(liuQin)
        if (lines.isEmpty()) return "卦中不见，此事暂无着落。"
        val line = lines.first()
        val ws = WangShuai.of(line.naJia.wuXing, chart.monthBranch)
        val kongNote = if (line.kong) "，又落旬空" else ""
        val movingNote = if (line.moving) "，且在动" else ""
        return "在${line.title}（${line.naJia.label}），${ws.label}$kongNote$movingNote —— " +
            when (ws) {
                WangShuai.WANG, WangShuai.XIANG -> hint[0]
                WangShuai.XIU -> hint[1]
                else -> hint[2]
            }
    }

    /** 一个断卦因子。weight > 0 助成、< 0 阻成、= 0 中性 */
    private data class Factor(val weight: Int, val text: String, val tone: Tone)

    private companion object {
        /**
         * 各类事的行动建议。
         *
         * 只给「该怎么做」，不给「会不会成」—— 会不会成由卦决定，
         * 怎么做是用户能控制的。混在一起说，用户就分不清哪些能改、哪些不能改。
         */
        val ADVICE: Map<QuestionCategory, String> = mapOf(
            QuestionCategory.CAREER to "事业上宜按部就班推进，别急于求成；先把手上已有的做扎实。",
            QuestionCategory.WEALTH to "财务上宜守成，不宜追高或加杠杆；先看清再动手。",
            QuestionCategory.MARRIAGE to "感情上宜坦诚沟通，少翻旧账；有误会就当面说清。",
            QuestionCategory.HEALTH to "身体上宜静养、及时就医，别拖别硬扛。",
            QuestionCategory.FAMILY to "家事上宜早作准备、留好凭证；文书合同尤其要看细。",
            QuestionCategory.CHILDREN to "对子女或下属宜耐心引导，强压反而起反作用。",
            QuestionCategory.PEER to "与人合作宜守分寸，口头承诺要落到字面。",
            QuestionCategory.TRAVEL to "出行搬迁宜留退路，能推迟就推迟。",
            QuestionCategory.GENERAL to "宜先观察形势再动手，不必急于表态。",
        )

        /**
         * 分维度表：六亲 → 维度名 → 三档断语（有力 / 平平 / 无力）。
         *
         * 断语只描述「气象」，不承诺结果 —— 与行动建议同理，
         * 说「眼下有气」比说「一定会成」诚实。
         */
        val DIMENSIONS: List<Triple<LiuQin, String, List<String>>> = listOf(
            Triple(
                LiuQin.QI_CAI, "财运",
                listOf("财有气，进项可期。", "财气平平，够用但难有大进。", "财气弱，宜守不宜投。"),
            ),
            Triple(
                LiuQin.GUAN_GUI, "事业",
                listOf("事业有气，利于推动。", "事业平稳，暂无大变动。", "事业受制，宜守成待时。"),
            ),
            Triple(
                LiuQin.QI_CAI, "感情",
                listOf("感情有气，彼此有意。", "感情平缓，需要主动经营。", "感情偏弱，易生疏离。"),
            ),
            Triple(
                LiuQin.ZI_SUN, "身心",
                listOf("子孙有气，身心舒泰。", "身心平常，注意作息。", "子孙无力，宜多休养。"),
            ),
            Triple(
                LiuQin.FU_MU, "文书·长辈",
                listOf("文书顺遂，长辈得力。", "文书平平，按流程走即可。", "文书易生波折，细看条款。"),
            ),
        )

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
