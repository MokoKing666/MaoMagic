package com.maohongyuan.magic.core.hexagram

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.Hexagram
import com.maohongyuan.magic.core.model.Trigram
import kotlin.random.Random

/** 起卦方式 */
enum class DivinationMethod(val label: String) {
    COIN("铜钱"),
    TIME("时间"),
    NUMBER("数字"),
    MANUAL("手动"),
}

/**
 * 单次投掷三枚铜钱的结果。
 *
 * 正面（字）记 **3**，反面（背）记 **2**。三枚之和即爻数：
 *
 * | 和 | 名称 | 阴阳 | 是否动 |
 * |---|---|---|---|
 * | 6 | 老阴 | 阴 | 动（变阳） |
 * | 7 | 少阳 | 阳 | 静 |
 * | 8 | 少阴 | 阴 | 静 |
 * | 9 | 老阳 | 阳 | 动（变阴） |
 *
 * 概率由三枚独立硬币自然给出：老阴 1/8、少阳 3/8、少阴 3/8、老阳 1/8。
 * 这不是调参调出来的，是「三枚各 1/2」的必然结果。
 */
data class CoinThrow(val coins: List<Int>) {
    init {
        require(coins.size == 3) { "需三枚铜钱，实际 ${coins.size}" }
        require(coins.all { it == 2 || it == 3 }) { "每枚只能是 2（背）或 3（字）" }
    }

    val sum: Int get() = coins.sum()

    val yang: Boolean get() = sum == 7 || sum == 9

    val moving: Boolean get() = sum == 6 || sum == 9

    /** 老阴 / 少阳 / 少阴 / 老阳 */
    val label: String
        get() = when (sum) {
            6 -> "老阴"
            7 -> "少阳"
            8 -> "少阴"
            else -> "老阳"
        }

    companion object {
        /** 随机投掷一次。 */
        fun random(random: Random = Random): CoinThrow =
            CoinThrow(List(3) { if (random.nextBoolean()) 3 else 2 })
    }
}

/** 起卦结果 */
data class DivinationResult(
    val hexagram: Hexagram,
    /** 动爻位置，0 = 初爻 */
    val movingIndexes: Set<Int>,
    val method: DivinationMethod,
    /** 铜钱法的原始投掷记录（自初爻至上爻），其它方式为空 */
    val throws: List<CoinThrow> = emptyList(),
) {
    val changed: Hexagram?
        get() = if (movingIndexes.isEmpty()) {
            null
        } else {
            hexagram.changing(*movingIndexes.toIntArray())
        }
}

/**
 * 起卦。
 *
 * 四种方式殊途同归：都产出「本卦 + 动爻集合」。
 */
object Divination {

    // ── 一、铜钱 ──────────────────────────────────────────────

    /**
     * 由六次投掷起卦。[throws] 自**初爻**排到**上爻**。
     */
    fun byCoin(throws: List<CoinThrow>): DivinationResult {
        require(throws.size == 6) { "需投掷六次，实际 ${throws.size}" }
        val hexagram = Hexagram.ofLines(throws.map { it.yang })
        val moving = throws.mapIndexedNotNull { i, t -> if (t.moving) i else null }.toSet()
        return DivinationResult(hexagram, moving, DivinationMethod.COIN, throws)
    }

    /** 现场随机摇六次。 */
    fun byCoinRandom(random: Random = Random): DivinationResult =
        byCoin(List(6) { CoinThrow.random(random) })

    // ── 二、时间（梅花易数法）────────────────────────────────

    /**
     * 时间起卦。
     *
     * 采用梅花易数的通行公式（**先天八卦数**：乾1 兑2 离3 震4 巽5 坎6 艮7 坤8）：
     *
     * ```
     * 上卦 = (年支数 + 月数 + 日数)               mod 8   余 0 取 8
     * 下卦 = (年支数 + 月数 + 日数 + 时支数)       mod 8   余 0 取 8
     * 动爻 = (年支数 + 月数 + 日数 + 时支数)       mod 6   余 0 取 6
     * ```
     *
     * 年支数、时支数均以 子 = 1 起算；月、日取**农历**月日。
     *
     * @param yearBranch 年支
     * @param lunarMonth 农历月 1..12
     * @param lunarDay   农历日 1..30
     * @param hourBranch 时支
     */
    fun byTime(
        yearBranch: EarthlyBranch,
        lunarMonth: Int,
        lunarDay: Int,
        hourBranch: EarthlyBranch,
    ): DivinationResult {
        require(lunarMonth in 1..12) { "农历月应在 1..12，实际 $lunarMonth" }
        require(lunarDay in 1..30) { "农历日应在 1..30，实际 $lunarDay" }

        val yearNum = yearBranch.ordinal + 1      // 子 = 1
        val hourNum = hourBranch.ordinal + 1
        val base = yearNum + lunarMonth + lunarDay
        val total = base + hourNum

        val upper = trigramOf(base)
        val lower = trigramOf(total)
        val moving = if (total % 6 == 0) 5 else total % 6 - 1

        return DivinationResult(
            hexagram = Hexagram.of(upper, lower),
            movingIndexes = setOf(moving),
            method = DivinationMethod.TIME,
        )
    }

    // ── 三、数字 ──────────────────────────────────────────────

    /**
     * 数字起卦。
     *
     * ```
     * 上卦 = 数一 mod 8   余 0 取 8
     * 下卦 = 数二 mod 8   余 0 取 8
     * 动爻 = (数一 + 数二) mod 6   余 0 取 6
     * ```
     */
    fun byNumber(first: Int, second: Int): DivinationResult {
        val upper = trigramOf(first)
        val lower = trigramOf(second)
        val sum = first + second
        val moving = if (sum % 6 == 0) 5 else sum % 6 - 1

        return DivinationResult(
            hexagram = Hexagram.of(upper, lower),
            movingIndexes = setOf(moving),
            method = DivinationMethod.NUMBER,
        )
    }

    // ── 四、手动 ──────────────────────────────────────────────

    /**
     * 手动指定六爻与动爻。
     *
     * @param bottomToTop 自初爻至上爻的六爻阴阳
     */
    fun byManual(bottomToTop: List<Boolean>, movingIndexes: Set<Int>): DivinationResult {
        require(bottomToTop.size == 6) { "需六爻" }
        require(movingIndexes.all { it in 0..5 }) { "动爻位置必须在 0..5" }
        return DivinationResult(
            hexagram = Hexagram.ofLines(bottomToTop),
            movingIndexes = movingIndexes,
            method = DivinationMethod.MANUAL,
        )
    }

    // ── 工具 ──────────────────────────────────────────────────

    /**
     * 先天八卦数 → 经卦。余 0 取坤（数 8）。
     *
     * [Trigram.entries] 的顺序正是先天次序（乾兑离震巽坎艮坤 = 1..8），
     * 所以直接按下标取即可，不需要另立映射表。
     */
    private fun trigramOf(value: Int): Trigram {
        val n = ((value % 8) + 8) % 8
        return if (n == 0) Trigram.KUN else Trigram.entries[n - 1]
    }

    /** 先天八卦数（1..8），供展示。 */
    fun xianTianNumber(trigram: Trigram): Int = trigram.ordinal + 1
}
