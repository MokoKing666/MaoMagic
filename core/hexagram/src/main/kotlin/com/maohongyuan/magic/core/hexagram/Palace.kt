package com.maohongyuan.magic.core.hexagram

import com.maohongyuan.magic.core.model.Hexagram
import com.maohongyuan.magic.core.model.Trigram
import com.maohongyuan.magic.core.model.WuXing
import com.maohongyuan.magic.core.model.YaoPosition

/**
 * 京房八宫的卦变阶段。
 *
 * 每一宫的八个卦，由**本宫首卦（八纯卦）**按固定规则逐爻变出。
 * 规则用**累积异或掩码**表达最简洁 —— 掩码相对本宫首卦，
 * 每多一世就多翻一位：
 *
 * ```
 * 本宫  000000        不变
 * 一世  000001        初爻变
 * 二世  000011        初、二变
 * 三世  000111        初、二、三变
 * 四世  001111        初~四变
 * 五世  011111        初~五变
 * 游魂  010111        五世之后**再变第四爻**（不是继续往上变）
 * 归魂  010000        游魂之后**内卦三爻全变回本宫**
 * ```
 *
 * ⚠️ 游魂与归魂是这套规则里唯二不「继续往上」的，写成递推会错。
 *    掩码是手算验证过的：乾宫八纯卦依次得到
 *    乾为天 → 天风姤 → 天山遁 → 天地否 → 风地观 → 山地剥 → 火地晋 → 火天大有。
 *
 * 世应位置：应爻恒为世爻 + 3（隔三爻）。
 */
enum class Generation(
    val label: String,
    /** 世爻位置，0 = 初爻 */
    val shiIndex: Int,
    /** 相对本宫首卦的异或掩码 */
    val xorMask: Int,
) {
    BEN_GONG("本宫", 5, 0b000000),
    YI_SHI("一世", 0, 0b000001),
    ER_SHI("二世", 1, 0b000011),
    SAN_SHI("三世", 2, 0b000111),
    SI_SHI("四世", 3, 0b001111),
    WU_SHI("五世", 4, 0b011111),
    YOU_HUN("游魂", 3, 0b010111),
    GUI_HUN("归魂", 2, 0b010000),
    ;

    /** 应爻位置。世应相隔三位。 */
    val yingIndex: Int get() = (shiIndex + 3) % 6
}

/**
 * 一个卦在八宫体系中的位置与身份。
 */
data class PalaceHexagram(
    val hexagram: Hexagram,
    /** 所属宫（以八纯卦的经卦表示） */
    val palace: Trigram,
    val generation: Generation,
) {
    /** 宫位名称，如「乾宫」 */
    val palaceLabel: String get() = "${palace.label}宫"

    /** 宫位五行 —— 六亲的「我」 */
    val palaceWuXing: WuXing get() = palace.wuXing

    val shiPosition: YaoPosition get() = YaoPosition.of(generation.shiIndex)
    val yingPosition: YaoPosition get() = YaoPosition.of(generation.yingIndex)

    /** 是否世爻在第 [yaoIndex] 爻（0 = 初爻） */
    fun isShi(yaoIndex: Int): Boolean = yaoIndex == generation.shiIndex

    fun isYing(yaoIndex: Int): Boolean = yaoIndex == generation.yingIndex

    /** 如「乾宫 · 一世 · 世初应四」 */
    val summary: String
        get() = "$palaceLabel · ${generation.label} · " +
            "世${shiPosition.ordinalLabel}应${yingPosition.ordinalLabel}"
}

/**
 * 六十四卦表 —— **完全由京房八宫变爻规则算法生成，不硬编码任何一条记录**。
 *
 * 手写 64 条宫位/世应记录必然出错；算法生成则天然自洽，
 * 且 [build] 里的自检会保证「恰好覆盖 64 卦、无重复、无遗漏」。
 */
object HexagramTable {

    /** 八宫的显示顺序（传统次序：乾坎艮震巽离坤兑）。 */
    val PALACE_ORDER: List<Trigram> = listOf(
        Trigram.QIAN, Trigram.KAN, Trigram.GEN, Trigram.ZHEN,
        Trigram.XUN, Trigram.LI, Trigram.KUN, Trigram.DUI,
    )

    private val all: List<PalaceHexagram> = build()

    private val byCode: Array<PalaceHexagram> = Array(Hexagram.COUNT) { code ->
        all.first { it.hexagram.code == code }
    }

    private fun build(): List<PalaceHexagram> {
        val result = ArrayList<PalaceHexagram>(Hexagram.COUNT)

        PALACE_ORDER.forEach { palace ->
            // 本宫首卦 = 上下同为该经卦的八纯卦
            val baseCode = Hexagram.of(palace, palace).code
            Generation.entries.forEach { generation ->
                val code = baseCode xor generation.xorMask
                result += PalaceHexagram(Hexagram.of(code), palace, generation)
            }
        }

        // ── 自检 ──
        check(result.size == Hexagram.COUNT) {
            "八宫共应生成 ${Hexagram.COUNT} 卦，实际 ${result.size}"
        }
        val codes = result.map { it.hexagram.code }
        check(codes.toSet().size == Hexagram.COUNT) {
            "八宫生成的卦码有重复：${codes.groupBy { it }.filter { it.value.size > 1 }.keys}"
        }
        check(codes.toSet() == (0 until Hexagram.COUNT).toSet()) {
            "八宫未覆盖全部卦码"
        }
        // 每宫恰好八卦
        PALACE_ORDER.forEach { palace ->
            val count = result.count { it.palace == palace }
            check(count == Generation.entries.size) {
                "${palace.label}宫应有 ${Generation.entries.size} 卦，实际 $count"
            }
        }
        // 本宫卦必须是八纯卦（上下卦相同）
        PALACE_ORDER.forEach { palace ->
            val benGong = result.first { it.palace == palace && it.generation == Generation.BEN_GONG }
            check(benGong.hexagram.upper == palace && benGong.hexagram.lower == palace) {
                "${palace.label}宫的本宫卦应为八纯卦，实际为 ${benGong.hexagram.name}"
            }
        }
        // 世应不可重合
        Generation.entries.forEach {
            check(it.shiIndex != it.yingIndex) { "${it.label} 的世应位置重合" }
        }

        return result
    }

    fun of(hexagram: Hexagram): PalaceHexagram = byCode[hexagram.code]

    fun of(code: Int): PalaceHexagram = byCode[code]

    /** 按传统次序返回全部 64 卦。 */
    fun all(): List<PalaceHexagram> = all

    /** 返回某一宫的八卦（本宫 → 归魂）。 */
    fun palace(palace: Trigram): List<PalaceHexagram> =
        Generation.entries.map { generation ->
            val baseCode = Hexagram.of(palace, palace).code
            of(baseCode xor generation.xorMask)
        }

    /** 按宫位五行分组，供「某五行有哪些卦」这类查询。 */
    fun byWuXing(): Map<WuXing, List<PalaceHexagram>> =
        all.groupBy { it.palaceWuXing }
}
