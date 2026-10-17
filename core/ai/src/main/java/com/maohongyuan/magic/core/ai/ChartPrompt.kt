package com.maohongyuan.magic.core.ai

import com.maohongyuan.magic.core.calendar.BaZiChart
import com.maohongyuan.magic.core.hexagram.LiuYaoChart
import com.maohongyuan.magic.core.hexagram.MeihuaChart
import com.maohongyuan.magic.core.interpret.InterpretationRequest
import com.maohongyuan.magic.core.model.YaoPosition

/**
 * 把卦盘渲染成给大模型的提示词。
 *
 * ── 为什么要自己渲染，而不是让模型去「算」 ──
 * 排盘（纳甲、六亲、世应、旬空、旺衰…）由本地的确定性算法完成，**结果一定是准的**。
 * 模型的价值在于**串联与表达**，不在于重算一遍 —— 让它自己排盘只会引入错误。
 *
 * 所以提示词里直接给**已经算好的完整卦盘**，只要求模型做推断与论述。
 * 这也是为什么这个应用的 Tier2 是「解释器」而不是「排盘器」。
 */
object ChartPrompt {

    /**
     * 系统提示词。
     *
     * 三条硬约束，缺一不可：
     * 1. **只依据给定卦盘** —— 不许自己重排，不许编造没给的爻
     * 2. **输出严格 JSON** —— 便于程序解析；不给 JSON 会得到五花八门的格式
     * 3. **不下绝对断言** —— 术数是文化研究，不能给医疗/法律/投资建议
     */
    const val SYSTEM_PROMPT: String = """你是一位精通中国传统术数的解卦师，熟稔《增删卜易》《卜筮正宗》的六爻断法、梅花易数的体用生克、以及子平八字的格局取用。

【最重要的前提】
卦盘已经由本地确定性算法排好，**纳甲、六亲、世应、六神、旬空、伏神、旺衰、变卦全部是准确的**。
你**绝对不要**自己重新排盘，也不要质疑或修改给定数据。你的任务是**基于这份卦盘做推断与论述**。

【断卦方法】
- 六爻：先定用神（按所问事类取六亲），看用神旺衰、有无生扶克制、是否旬空、有无伏神，再看动爻与变爻的作用、世应关系、六神辅助。逐层推导，不要跳步。
- 梅花：以体卦为主、用卦为事，看体用生克；再看互卦（中间过程）、变卦（最终结果）。
- 八字：只做客观描述与常规分析（五行分布、十神结构、大运走势）。**不要**断言寿夭、灾祸、婚姻成败等敏感结论。

【输出要求】
只输出一个 JSON 对象，不要 Markdown 代码块，不要任何额外文字。结构如下：
{
  "verdict": "吉" | "凶" | "平" | "吉中带阻",
  "headline": "一句话结论，30 字以内，要具体，不要空泛",
  "yongShen": "用神说明（仅六爻需要，其它填空字符串）",
  "sections": [
    {"title": "段落标题，4-8 字", "body": "论述内容，80-200 字", "tone": "POSITIVE" | "NEGATIVE" | "WARNING" | "NEUTRAL"}
  ]
}

sections 给 4-6 段，覆盖：用神/核心判断、旺衰或结构、动变或走势、应期或时机、建议。
语气务实平实，不用「必将」「注定」这类绝对词。"""

    /** 把请求渲染成用户消息 */
    fun userMessage(request: InterpretationRequest): String = buildString {
        appendLine("【所问】")
        appendLine(request.question.ifBlank { "（未填写，请按综合运势论断）" })
        appendLine()

        request.liuYao?.let { append(liuYao(it)) }
        request.meihua?.let { append(meihua(it, request)) }
        request.bazi?.let { append(bazi(it)) }

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
}
