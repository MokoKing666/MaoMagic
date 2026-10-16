package com.maohongyuan.magic.core.hexagram

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.GanZhi

/**
 * 旬空（空亡）。
 *
 * 六十甲子每十位为一旬，每旬有两位地支轮空。规则**完全算术化**：
 *
 * ```
 * 旬首 = 干支序号 − (干支序号 mod 10)
 * 空亡 = [ (旬首 + 10) mod 12 , (旬首 + 11) mod 12 ]
 * ```
 *
 * 校验（六旬全部对得上）：
 *
 * | 旬 | 旬首 | 空亡 |
 * |---|---|---|
 * | 甲子 | 0 | 戌、亥 |
 * | 甲戌 | 10 | 申、酉 |
 * | 甲申 | 20 | 午、未 |
 * | 甲午 | 30 | 辰、巳 |
 * | 甲辰 | 40 | 寅、卯 |
 * | 甲寅 | 50 | 子、丑 |
 */
data class XunKong(
    /** 旬首干支（必为甲某） */
    val xunHead: GanZhi,
    val first: EarthlyBranch,
    val second: EarthlyBranch,
) {
    operator fun contains(branch: EarthlyBranch): Boolean =
        branch == first || branch == second

    val label: String get() = "${first.label}${second.label}"

    override fun toString(): String = label

    companion object {
        fun of(ganZhi: GanZhi): XunKong {
            val head = ganZhi.index - (ganZhi.index % 10)
            return XunKong(
                xunHead = GanZhi(head),
                first = EarthlyBranch.entries[(head + 10) % 12],
                second = EarthlyBranch.entries[(head + 11) % 12],
            )
        }

        /** 六旬的空亡，供自检使用。 */
        internal fun allSix(): List<XunKong> =
            (0..5).map { of(GanZhi(it * 10)) }
    }
}
