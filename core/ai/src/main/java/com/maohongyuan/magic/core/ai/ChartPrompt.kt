package com.maohongyuan.magic.core.ai

import com.maohongyuan.magic.core.calendar.BaZiChart
import com.maohongyuan.magic.core.hexagram.LiuYaoChart
import com.maohongyuan.magic.core.hexagram.MeihuaChart
import com.maohongyuan.magic.core.interpret.InterpretationRequest
import com.maohongyuan.magic.core.model.DivinationSystem
import com.maohongyuan.magic.core.model.YaoPosition
import com.maohongyuan.magic.core.tarot.TarotChart
import com.maohongyuan.magic.core.xiaoliuren.XiaoLiuRenChart

/**
 * 把卦盘渲染成给大模型的提示词。
 *
 * ── 为什么要自己渲染，而不是让模型去「算」 ──
 * 排盘（纳甲、六亲、世应、旬空、旺衰…）由本地的确定性算法完成，**结果一定是准的**。
 * 模型的价值在于**串联与表达**，不在于重算一遍 —— 让它自己排盘只会引入错误。
 *
 * 所以提示词里直接给**已经算好的完整卦盘**，只要求模型做推断与论述。
 * 这也是为什么这个应用的 Tier2 是「解释器」而不是「排盘器」。
 *
 * ── 为什么人设必须按系统分派 ──
 * 早期版本只有一份写死的六爻人设（「你熟稔《增删卜易》《卜筮正宗》的六爻断法」），
 * 塔罗、小六壬这些**没有结构化排盘**的系统走 `else` 分支只拿到一句
 * 「暂无结构化排盘，请依据常识作答」。模型拿着六爻人设、手里又没有卦盘，
 * 只能凭空编 —— 实测塔罗解读里出现了「世爻应爻」，用户一眼就看出是错的。
 *
 * 现在按 [DivinationSystem] 分派人设：六爻人设只在盘**确实是六爻**时才给，
 * 其余系统各自有自己的人设与术语白名单，并明确禁止挪用六爻术语。
 */
object ChartPrompt {

    /**
     * 系统提示词。人设 + 输出契约。
     *
     * 必须是函数而不是常量 —— 人设随盘的系统变化，输出契约里的
     * `yongShen` 字段也只对六爻有意义。
     */
    fun systemPrompt(request: InterpretationRequest): String = buildString {
        appendLine(persona(request.chart.system))
        appendLine()
        append(outputContract(includeYongShen = request.chart.system == DivinationSystem.LIU_YAO))
    }

    // ── 人设 ──────────────────────────────────────────────

    private fun persona(system: DivinationSystem): String {
        // 六爻人设自成一类：它**可以**用世应、纳甲这些词。
        if (system == DivinationSystem.LIU_YAO) return LIU_YAO_PERSONA

        val body = when (system) {
            DivinationSystem.MEI_HUA -> MEI_HUA_BODY
            DivinationSystem.BA_ZI -> BA_ZI_BODY
            DivinationSystem.TAROT -> TAROT_BODY
            DivinationSystem.XIAO_LIU_REN -> XIAO_LIU_REN_BODY
            // 紫微 / 大六壬 / 奇门 / 人格 / 日运 / 月运：目前没有结构化排盘。
            // 给「只依据所问」的通用人设，而不是套六爻。
            else -> genericBody(system)
        }
        return body + "\n\n" + termBan(system)
    }

    /**
     * 术语禁令 —— 每个非六爻系统都必须有。
     *
     * 名单按系统定制，不能一刀切：
     * - 梅花有「动爻」「变卦」，禁的是六爻的世应与纳甲
     * - 小六壬自己就有「六神」（青龙/朱雀…），不能禁
     * - 八字禁「卦」这一整套
     */
    private fun termBan(system: DivinationSystem): String {
        val banned = when (system) {
            DivinationSystem.TAROT -> listOf(
                "世爻", "应爻", "用神", "忌神", "纳甲", "六亲", "六神",
                "旬空", "伏神", "动爻", "变爻", "旺衰", "日辰", "月建", "天干地支",
            )
            DivinationSystem.BA_ZI -> listOf(
                "世爻", "应爻", "用神", "忌神", "纳甲", "六亲", "六神",
                "旬空", "伏神", "动爻", "变爻", "旺衰",
            )
            DivinationSystem.XIAO_LIU_REN -> listOf(
                "世爻", "应爻", "用神", "忌神", "纳甲", "六亲",
                "旬空", "伏神", "动爻", "变爻", "旺衰",
            )
            DivinationSystem.MEI_HUA -> listOf(
                "世爻", "应爻", "用神", "忌神", "纳甲", "六亲", "六神", "旬空", "伏神", "变爻",
            )
            else -> listOf(
                "世爻", "应爻", "用神", "忌神", "纳甲", "六亲", "六神", "旬空", "伏神", "动爻", "变爻",
            )
        }
        return buildString {
            appendLine("【硬约束 · 术语】")
            appendLine("这是「${system.label}」，不是六爻。严禁出现下列其他体系的术语：")
            appendLine(banned.joinToString("、"))
            appendLine("盘面里没有的信息不要编。")
        }.trimEnd()
    }

    private val LIU_YAO_PERSONA = """你是一位精通六爻断法的解卦师，熟稔《增删卜易》《卜筮正宗》。

【最重要的前提】
卦盘已经由本地确定性算法排好，**纳甲、六亲、世应、六神、旬空、伏神、旺衰、变卦全部是准确的**。
你**绝对不要**自己重新排盘，也不要质疑或修改给定数据。你的任务是**基于这份卦盘做推断与论述**。

【断卦方法】
先定用神（按所问事类取六亲），看用神旺衰、有无生扶克制、是否旬空、有无伏神，
再看动爻与变爻的作用、世应关系、六神辅助。逐层推导，不要跳步。""".trimIndent()

    private val MEI_HUA_BODY = """你是一位精通梅花易数的解卦师。

【最重要的前提】
卦盘已由本地算法排好：本卦、互卦、变卦、动爻、体卦、用卦及其五行都是准确的。
**不要**自己重排，也不要质疑或修改给定数据。

【断卦方法】
以体卦为主、用卦为事，看体用生克定主断；再看互卦（中间过程）、变卦（最终结果），
结合体用旺衰判断力量对比。梅花重象与理，不重爻位细节。""".trimIndent()

    private val BA_ZI_BODY = """你是一位精通子平八字的命理师。

【最重要的前提】
四柱、藏干、十神、纳音、地势、大运、五行分布全部由本地算法排定，准确无误。
**不要**重新排盘，也不要质疑或修改给定数据。

【分析方法】
先看日主强弱与五行分布，再看十神结构与格局取用，最后结合大运走势谈阶段性的倾向。
只做客观描述与常规分析，**不要**断言寿夭、灾祸、婚姻成败等敏感结论。""".trimIndent()

    private val TAROT_BODY = """你是一位塔罗牌解读师，按象征与心理的路径解读牌面。

【最重要的前提】
抽到的牌、所在位置、正逆位都是**真实抽出的结果**，不要替换或补充其他牌。

【解读方法】
逐张解读：牌义要结合**它所在的位置**（例如「过去」位的牌与「结果」位的牌，含义不同）
以及正逆位来谈，再把这些牌串成一条连贯的叙述，回应提问者所问的事。
塔罗讲的是「牌面呈现了什么」，不做宿命式断言。""".trimIndent()

    private val XIAO_LIU_REN_BODY = """你是一位精通小六壬的占卜师。

【最重要的前提】
三宫（月宫 / 日宫 / 时宫）的落点与吉凶已由本地算法算定，准确无误。
**不要**重新起课，也不要质疑或修改给定数据。

【断法】
传统上「月宫」看事情的起因与环境，「日宫」看过程与当下，「时宫」看结果与应期，
以时宫为主断，三宫合参。六宫的宫义与吉凶倾向已给出，围绕它们谈。""".trimIndent()

    private fun genericBody(system: DivinationSystem): String =
        """你是一位传统术数的解读师，正在为「${system.label}」做解读。

【最重要的前提】
本次**没有**结构化排盘数据，你手上只有提问者所问的事。
因此：**只依据所问与「${system.label}」这一体系本身的常识作答**，
不要虚构盘面，不要编造任何具体的爻位、宫位、星曜或数值。""".trimIndent()

    /**
     * 输出契约。
     *
     * ⚠️ 这里**只**要求 JSON。早期版本在末尾又写了一段「只用纯文本、不要 Markdown」，
     * 与前面的 JSON 要求直接冲突，模型时而给 JSON、时而给散文 ——
     * 散文会被解析器判为失败并降级，等于白花一次调用。
     * 「不要 Markdown」这条改成了对**字符串内容**的约束，写在 JSON 契约内部。
     */
    private fun outputContract(includeYongShen: Boolean): String = buildString {
        appendLine("【输出要求】")
        appendLine("只输出一个 JSON 对象，不要 Markdown 代码块，不要任何额外文字。结构如下：")
        appendLine("{")
        appendLine("""  "verdict": "吉" | "凶" | "平" | "吉中带阻",""")
        appendLine("""  "headline": "一句话结论，30 字以内，要具体，不要空泛",""")
        if (includeYongShen) {
            appendLine("""  "yongShen": "用神说明",""")
        }
        appendLine("""  "sections": [""")
        appendLine(
            """    {"title": "段落标题，4-8 字", "body": "论述内容，80-200 字", """ +
                """"tone": "POSITIVE" | "NEGATIVE" | "WARNING" | "NEUTRAL"}"""
        )
        appendLine("  ]")
        appendLine("}")
        appendLine()
        appendLine("sections 给 4-6 段，覆盖：核心判断、依据、走势或时机、建议。")
        appendLine("字符串内部只用纯文本：不要 # 标题、不要 ** 加粗、不要反引号。要分点时用「·」开头。")
        appendLine("语气务实平实，不用「必将」「注定」这类绝对词。")
        appendLine()
        appendLine("【边界 · 对所有系统生效】")
        appendLine("术数内容仅供传统文化研究与娱乐。不要给出医疗、法律、投资等专业建议，也不要下绝对断言。")
        appendLine()
        appendLine("【硬约束 · 最重要】")
        appendLine("上面给出的盘面数据全部由本地确定性算法算出：")
        appendLine("不得改动、不得重新推算、不得用你自己的五行生克去推翻。")
        appendLine("你的任务只有一件 —— 把这些事实讲成人话，说清哪些有利、哪些不利、对他问的这件事意味着什么。")
        appendLine("盘面里没有的信息就不要编，宁可不说。")
    }

    // ── 用户消息 ──────────────────────────────────────────

    /** 把请求渲染成用户消息 */
    fun userMessage(request: InterpretationRequest): String = buildString {
        appendLine("【所问】")
        appendLine(request.question.ifBlank { "（未填写，请按综合运势论断）" })
        appendLine()

        // 按盘的类型分派。用 `when` + `else` 而不是 sealed ——
        // 各盘分散在不同模块，跨模块无法 sealed（见 DivinationChart 注释）。
        // 新增系统时在这里加一个分支即可；未加的系统会走 else 只输出系统名，
        // 并且人设那边已经明确禁用六爻术语，模型不会凭空编世应。
        when (val chart = request.chart) {
            is LiuYaoChart -> append(liuYao(chart))
            is MeihuaChart -> append(meihua(chart, request))
            is BaZiChart -> append(bazi(chart))
            is TarotChart -> append(tarot(chart))
            is XiaoLiuRenChart -> append(xiaoLiuRen(chart))
            else -> {
                appendLine("【排盘】${chart.system.label}")
                appendLine("（该系统暂未提供结构化排盘。不要虚构盘面，只依据所问作答。）")
                appendLine()
            }
        }

        appendLine()
        append("请按系统要求输出 JSON。")
    }

    // ── 六爻 ──────────────────────────────────────────────

    private fun liuYao(chart: LiuYaoChart): String = buildString {
        appendLine("【六爻卦盘】")
        appendLine("本卦：${chart.hexagram.name}（${chart.palace.palaceLabel}·${chart.palace.generation.label}）")
        chart.changed?.let { appendLine("变卦：${it.name}") }
        appendLine("月建：${chart.monthBranch}月　日辰：${chart.dayGanZhi}日")
        appendLine("旬空：${chart.xunKong.first}${chart.xunKong.second}")
        appendLine()

        appendLine("六爻自初爻至上爻：")
        chart.lines.forEach { line ->
            val mark = buildString {
                append(if (line.yang) "阳" else "阴")
                if (line.moving) append("·动")
                if (line.kong) append("·旬空")
            }
            val rel = line.relation.label?.let { " [$it]" } ?: ""
            appendLine(
                "  ${YaoPosition.of(line.index).ordinalLabel}爻 $mark　" +
                    "${line.liuShen}　${line.liuQin}${line.naJia.label}（${line.naJia.wuXing}）　" +
                    "${line.wangShuai.label}$rel"
            )
            line.fuShen?.let { appendLine("      伏神：${it.label}") }
            line.bian?.let {
                appendLine("      变爻：${it.naJia.label}（${it.naJia.wuXing}）${it.liuQin}，变卦属${it.palace}宫")
            }
        }

        if (chart.missingLiuQin.isNotEmpty()) {
            appendLine()
            appendLine("本卦缺失六亲（以伏神论）：${chart.missingLiuQin.joinToString("、") { it.label }}")
        }
        appendLine()
    }

    // ── 梅花 ──────────────────────────────────────────────

    private fun meihua(chart: MeihuaChart, request: InterpretationRequest): String = buildString {
        appendLine("【梅花易数卦盘】")
        appendLine("本卦：${chart.original.name}（上${chart.original.upper}下${chart.original.lower}）")
        appendLine("互卦：${chart.nuclear.name}")
        appendLine("变卦：${chart.changed.name}")
        appendLine("动爻：${YaoPosition.of(chart.movingIndex).ordinalLabel}爻")
        appendLine(
            "体卦：${chart.tiTrigram}（${chart.tiTrigram.wuXing}）　" +
                "用卦：${chart.yongTrigram}（${chart.yongTrigram.wuXing}）"
        )
        request.monthBranch?.let { appendLine("月建：${it}月") }
        appendLine()
    }

    // ── 八字 ──────────────────────────────────────────────

    private fun bazi(chart: BaZiChart): String = buildString {
        appendLine("【八字命盘】")
        appendLine("性别：${chart.gender.label}　日主：${chart.dayMaster}（${chart.dayMaster.wuXing}）")
        appendLine()
        appendLine("四柱：")
        listOf(chart.year, chart.month, chart.day, chart.hour).forEach { p ->
            appendLine(
                "  ${p.label}：${p.gan}${p.zhi}　藏干 ${p.hideGan.joinToString("") { it.label }}　" +
                    "十神 ${p.shiShenGan}　纳音 ${p.naYin}　地势 ${p.diShi}"
            )
        }
        appendLine()
        appendLine("五行分布：" + chart.wuXingCount.entries.joinToString("　") { "${it.key}${it.value}" })
        appendLine("胎元：${chart.taiYuan}　命宫：${chart.mingGong}　身宫：${chart.shenGong}")
        appendLine("起运：${chart.startAge} 岁，大运${if (chart.forward) "顺行" else "逆行"}")
        appendLine("大运：" + chart.daYun.take(8).joinToString("　") { "${it.startAge}岁${it.ganZhi}" })
        appendLine()
    }

    // ── 塔罗 ──────────────────────────────────────────────

    /**
     * 塔罗牌阵。
     *
     * 逐张给出**位置名 + 牌名 + 正逆位 + 该朝向的牌义**。
     * 位置名不能省：同一张牌在「过去」位与「结果」位，解读完全不同。
     */
    private fun tarot(chart: TarotChart): String = buildString {
        appendLine("【塔罗牌阵】${chart.spread.label}（${chart.spread.positions.joinToString(" / ")}）")
        appendLine("共 ${chart.drawn.size} 张，其中逆位 ${chart.reversedCount} 张")
        appendLine()
        chart.drawn.forEachIndexed { index, d ->
            appendLine("${index + 1}. ${d.positionName} · ${d.card.name}（${d.orientationLabel}）")
            appendLine("   牌义：${d.meaning}")
        }
        appendLine()
    }

    // ── 小六壬 ────────────────────────────────────────────

    private fun xiaoLiuRen(chart: XiaoLiuRenChart): String = buildString {
        appendLine("【小六壬课盘】${chart.method.label}")
        appendLine("起课数：${chart.rawFirst} / ${chart.rawSecond} / ${chart.rawThird}")
        appendLine()
        listOf(
            "月宫（起因 / 环境）" to chart.first,
            "日宫（过程 / 当下）" to chart.second,
            "时宫（结果 / 应期）" to chart.third,
        ).forEach { (label, palace) ->
            appendLine("$label：${palace.name}（${palace.wuXing}·${palace.liuShen}）　${palace.verdict}")
            appendLine("   宫义：${palace.meaning}")
            appendLine("   断法：${palace.advice}")
        }
        appendLine()
        appendLine("三宫落点：${chart.first.name} → ${chart.second.name} → ${chart.third.name}")
        appendLine("主断看时宫：${chart.third.name}（${chart.overallVerdict}）")
        appendLine()
    }
}
