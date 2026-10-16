package com.maohongyuan.magic.core.hexagram

import com.maohongyuan.magic.core.model.Hexagram
import com.maohongyuan.magic.core.model.Trigram
import com.maohongyuan.magic.core.model.WuXing
import com.maohongyuan.magic.core.model.WuXingRelation

/**
 * 梅花易数排盘。
 *
 * 梅花只看三件事：**体用、互卦、变卦**。
 *
 * ── 体用判定 ──
 * **动爻所在的那一卦为「用」，另一卦为「体」**。
 * - 动爻在下卦（初、二、三）⇒ 下卦为用、上卦为体
 * - 动爻在上卦（四、五、上）⇒ 上卦为用、下卦为体
 *
 * ── 吉凶 ──
 * 以体为我、用为事：
 *
 * | 关系 | 断 |
 * |---|---|
 * | 用生体 | 吉（事来就我） |
 * | 体用比和 | 吉 |
 * | 体克用 | 吉（但需费力） |
 * | 体生用 | 凶（耗泄） |
 * | 用克体 | 凶 |
 */
data class MeihuaChart(
    /** 本卦 */
    val original: Hexagram,
    /** 互卦 —— 取二三四爻为下卦、三四五爻为上卦，主事情中间状态 */
    val nuclear: Hexagram,
    /** 变卦 —— 动爻阴阳互变，主结果 */
    val changed: Hexagram,
    /** 动爻位置，0 = 初爻 */
    val movingIndex: Int,
) {
    /** 体卦（不动的那一卦） */
    val tiTrigram: Trigram
        get() = if (movingIndex < 3) original.upper else original.lower

    /** 用卦（动爻所在的那一卦） */
    val yongTrigram: Trigram
        get() = if (movingIndex < 3) original.lower else original.upper

    val tiWuXing: WuXing get() = tiTrigram.wuXing
    val yongWuXing: WuXing get() = yongTrigram.wuXing

    /** 以体为我，用与我的关系 */
    val relation: WuXingRelation get() = WuXingRelation.between(tiWuXing, yongWuXing)

    /** 体用关系是否偏吉 */
    val favorable: Boolean
        get() = when (relation) {
            WuXingRelation.IT_GENERATES, WuXingRelation.SAME, WuXingRelation.I_OVERCOME -> true
            WuXingRelation.I_GENERATE, WuXingRelation.IT_OVERCOMES -> false
        }

    /** 体用断语 */
    val relationLabel: String
        get() = when (relation) {
            WuXingRelation.IT_GENERATES -> "用生体"
            WuXingRelation.SAME -> "体用比和"
            WuXingRelation.I_OVERCOME -> "体克用"
            WuXingRelation.I_GENERATE -> "体生用"
            WuXingRelation.IT_OVERCOMES -> "用克体"
        }

    /** 动爻爻题，如「九四」 */
    val movingTitle: String
        get() = com.maohongyuan.magic.core.model.YaoPosition.of(movingIndex)
            .title(original.lines[movingIndex])
}

/**
 * 梅花易数起卦。
 */
object MeihuaDivination {

    /**
     * 由「本卦 + 动爻」推出完整的梅花盘（互卦与变卦自动计算）。
     */
    fun chart(original: Hexagram, movingIndex: Int): MeihuaChart {
        require(movingIndex in 0..5) { "动爻位置必须在 0..5，实际 $movingIndex" }
        return MeihuaChart(
            original = original,
            nuclear = original.nuclear,
            changed = original.changing(movingIndex),
            movingIndex = movingIndex,
        )
    }

    /** 由 [DivinationResult] 直接构造。多动爻时取第一个动爻（梅花一卦一动）。 */
    fun chart(result: DivinationResult): MeihuaChart {
        val moving = result.movingIndexes.minOrNull() ?: 0
        return chart(result.hexagram, moving)
    }

    /** 时间起卦并直接成盘。 */
    fun byTime(
        yearBranch: com.maohongyuan.magic.core.model.EarthlyBranch,
        lunarMonth: Int,
        lunarDay: Int,
        hourBranch: com.maohongyuan.magic.core.model.EarthlyBranch,
    ): MeihuaChart = chart(
        Divination.byTime(yearBranch, lunarMonth, lunarDay, hourBranch),
    )

    /** 数字起卦并直接成盘。 */
    fun byNumber(first: Int, second: Int): MeihuaChart =
        chart(Divination.byNumber(first, second))
}
