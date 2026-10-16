package com.maohongyuan.magic.core.calendar

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.GanZhi
import com.maohongyuan.magic.core.model.HeavenlyStem

/**
 * 把 lunar 库返回的「甲子」这类字符串解析成强类型 [GanZhi]。
 *
 * 解析失败时返回 null 而不是抛异常 —— 历法库在某些边界日期可能返回空串，
 * 上层应能优雅降级，而不是让整个页面崩掉。
 */
internal fun parseGanZhi(text: String?): GanZhi? {
    if (text == null || text.length < 2) return null
    val stem = HeavenlyStem.ofLabel(text.substring(0, 1)) ?: return null
    val branch = EarthlyBranch.ofLabel(text.substring(1, 2)) ?: return null
    return GanZhi.of(stem, branch)
}

/** 解析 lunar 返回的五行串（如「金水」），逐字转成枚举。 */
internal fun parseWuXing(text: String?): List<com.maohongyuan.magic.core.model.WuXing> {
    if (text.isNullOrBlank()) return emptyList()
    return text.mapNotNull { ch ->
        com.maohongyuan.magic.core.model.WuXing.ofLabel(ch.toString())
    }
}

// ─────────────────────────────────────────────────────────────
// 日课信息
// ─────────────────────────────────────────────────────────────

/** 公历信息 */
data class SolarInfo(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    /** 星期五 */
    val weekLabel: String,
    /** 2026-10-16 */
    val ymd: String,
)

/** 农历信息 */
data class LunarInfo(
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean,
    /** 九月 */
    val monthLabel: String,
    /** 初六 */
    val dayLabel: String,
    /** 丙午 */
    val yearGanZhi: String,
    /** 马 */
    val zodiac: String,
)

/** 四柱干支与纳音 */
data class GanZhiInfo(
    val year: GanZhi?,
    val month: GanZhi?,
    val day: GanZhi?,
    val hour: GanZhi?,
    val yearNaYin: String,
    val monthNaYin: String,
    val dayNaYin: String,
    val timeNaYin: String,
)

/** 吉神方位 */
data class PositionInfo(val name: String, val direction: String, val description: String)

/** 黄历宜忌与神煞 */
data class AlmanacInfo(
    val yi: List<String>,
    val ji: List<String>,
    /** 冲兔(丁卯)煞东 */
    val clashDescription: String,
    val clashZodiac: String,
    val sha: String,
    val jiShen: List<String>,
    val xiongSha: List<String>,
    /** 建除十二值星：建/除/满/平/定/执/破/危/成/收/开/闭 */
    val zhiXing: String,
    /** 天神（黄黑道），如「玉堂」 */
    val tianShen: String,
    /** 黄道 / 黑道 */
    val tianShenType: String,
    /** 吉 / 凶 */
    val tianShenLuck: String,
    /** 二十八宿，如「亢」 */
    val xiu: String,
    val xiuLuck: String,
    val pengZuGan: String,
    val pengZuZhi: String,
    val positions: List<PositionInfo>,
)

/**
 * 一天的完整历法信息 —— 黄历页与六爻起卦共用的数据源。
 */
data class DayInfo(
    val solar: SolarInfo,
    val lunar: LunarInfo,
    val ganZhi: GanZhiInfo,
    val almanac: AlmanacInfo,
    /** 当日节气（非节气日为空） */
    val jieQi: String?,
    val festivals: List<String>,
) {
    /** 日辰干支 —— 六爻装卦的必需输入 */
    val dayGanZhi: GanZhi? get() = ganZhi.day

    /** 月建地支 —— 六爻旺衰的必需输入 */
    val monthBranch: EarthlyBranch? get() = ganZhi.month?.branch
}
