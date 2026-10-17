package com.maohongyuan.magic.core.xiaoliuren

import com.maohongyuan.magic.core.model.DivinationChart
import com.maohongyuan.magic.core.model.DivinationSystem
import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.resource.JsonResource
import kotlinx.serialization.Serializable

/**
 * 小六壬的六宫。
 *
 * 顺序是**固定的**，且必须与 [XiaoLiuRenDivination] 的取模运算一致：
 * 大安(0) → 留连(1) → 速喜(2) → 赤口(3) → 小吉(4) → 空亡(5) → 回到大安。
 *
 * 断语放在 JSON 资源里（`resources/xiaoliuren/palace.json`）——
 * 这类文本会不断修订，硬编码进 Kotlin 每次改都要重编译整个模块。
 */
@Serializable
data class SixPalace(
    val id: String,
    val name: String,
    val wuXing: String,
    val liuShen: String,
    /** 吉 / 凶 / 大凶 */
    val verdict: String,
    val direction: String,
    val color: String,
    /** 宫义：这一宫本身是什么意思 */
    val meaning: String,
    /** 断法建议：具体该怎么办 */
    val advice: String,
)

/**
 * 六宫数据。
 *
 * `init` 里做完整性自检 —— 六宫少了任何一个，取模都会静默指到错的位置，
 * 属于「不报错但全错」的那类问题，必须在加载时就炸出来。
 */
object SixPalaceData {

    val palaces: List<SixPalace> by lazy {
        JsonResource.load<PalaceFile>("/xiaoliuren/palace.json").palaces
    }

    init {
        // 触发加载并校验。注意 palaces 是 lazy，这里显式访问一次
        check(palaces.size == COUNT) { "小六壬必须恰好 $COUNT 宫，实际 ${palaces.size}" }
        check(palaces.map { it.id }.toSet().size == COUNT) { "六宫 id 有重复" }
        check(palaces.map { it.name }.toSet().size == COUNT) { "六宫名称有重复" }
    }

    /** 按序号取宫（0..5），超出范围自动取模 */
    fun of(index: Int): SixPalace = palaces[((index % COUNT) + COUNT) % COUNT]

    /** 按名取宫（如「大安」） */
    fun byName(name: String): SixPalace? = palaces.firstOrNull { it.name == name }

    const val COUNT = 6

    @Serializable
    private data class PalaceFile(val palaces: List<SixPalace> = emptyList())
}

/** 起课方式 */
enum class XiaoLiuRenMethod(val label: String) {
    TIME("时间起课"),
    NUMBER("报数起课"),
}

/**
 * 小六壬课盘。
 *
 * 三宫 = 三个落点。传统上「月宫」看事情的起因/环境，
 * 「日宫」看过程/当下，「时宫」看结果/应期。
 */
data class XiaoLiuRenChart(
    val method: XiaoLiuRenMethod,
    /** 第一落点（时间起课=月宫，报数起课=第一个数） */
    val first: SixPalace,
    /** 第二落点（日宫 / 第二个数） */
    val second: SixPalace,
    /** 第三落点（时宫 / 第三个数）—— 断结果主要看它 */
    val third: SixPalace,
    /** 起课用的原始数值，便于展示与复现 */
    val rawFirst: Int,
    val rawSecond: Int,
    val rawThird: Int,
    val question: String,
) : DivinationChart {

    override val system: DivinationSystem = DivinationSystem.XIAO_LIU_REN

    /** 三宫合参的吉凶倾向：看第三宫（结果）为主，前两宫为辅 */
    val overallVerdict: String get() = third.verdict
}

/**
 * 起课算法。
 *
 * ── 核心规则：三次起数必须「接着上一个落宫」继续数 ──
 * ```
 * 自大安起正月，顺数至月 → 落宫 A
 * 自 A 宫起初一，顺数至日 → 落宫 B     ← 起点是 A，不是大安
 * 自 B 宫起子时，顺数至时 → 落宫 C     ← 起点是 B，不是大安
 * ```
 * 写成「各自从大安起数」结果会全错，而且**不会报错** —— 这是本算法唯一的坑。
 *
 * 取模公式（0-based）：
 * - 月宫 A = (月 - 1) mod 6
 * - 日宫 B = (A + 日 - 1) mod 6
 * - 时宫 C = (B + 时支序) mod 6
 */
object XiaoLiuRenDivination {

    /**
     * 时间起课。
     *
     * @param lunarMonth 农历月（1..12），闰月按本月算
     * @param lunarDay 农历日（1..30）
     * @param hourBranch 时辰地支（子=0 … 亥=11）
     */
    fun byTime(
        lunarMonth: Int,
        lunarDay: Int,
        hourBranch: EarthlyBranch,
        question: String = "",
    ): XiaoLiuRenChart {
        require(lunarMonth in 1..12) { "农历月必须在 1..12，实际 $lunarMonth" }
        require(lunarDay in 1..30) { "农历日必须在 1..30，实际 $lunarDay" }
        return build(
            method = XiaoLiuRenMethod.TIME,
            a = lunarMonth, b = lunarDay, c = hourBranch.ordinal + 1,
            question = question,
        )
    }

    /**
     * 报数起课。
     *
     * 三个数替代「月/日/时」的角色，**数法完全相同**（也是接着上一个落宫数）。
     * 传统上报数常取 1..12 之类的小数，这里不限范围，取模即可。
     */
    fun byNumbers(
        first: Int,
        second: Int,
        third: Int,
        question: String = "",
    ): XiaoLiuRenChart {
        require(first > 0 && second > 0 && third > 0) { "报数必须为正整数" }
        return build(XiaoLiuRenMethod.NUMBER, first, second, third, question)
    }

    /**
     * 唯一的取模入口 —— 时间与报数共用，保证两条路径的算法**不可能分叉**。
     * 三个入参都是 1-based 的「第几个」。
     */
    private fun build(
        method: XiaoLiuRenMethod,
        a: Int,
        b: Int,
        c: Int,
        question: String,
    ): XiaoLiuRenChart {
        val n = SixPalaceData.COUNT
        val i = (a - 1).mod(n)              // 自大安起数
        val j = (i + b - 1).mod(n)          // 自 i 宫起数
        val k = (j + c - 1).mod(n)          // 自 j 宫起数
        return XiaoLiuRenChart(
            method = method,
            first = SixPalaceData.of(i),
            second = SixPalaceData.of(j),
            third = SixPalaceData.of(k),
            rawFirst = a,
            rawSecond = b,
            rawThird = c,
            question = question,
        )
    }
}
