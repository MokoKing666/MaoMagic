package com.maohongyuan.magic.core.calendar

import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar

/**
 * 历法内核统一入口。
 *
 * 职责边界：**lunar 库完全封装在本模块内部**，对外只暴露本项目自己的领域类型
 * （[DayInfo] / [BaZiChart]）。这样将来若要替换历法实现，改动只在本模块内。
 *
 * 数据来源：[cn.6tail:lunar](https://github.com/6tail/lunar-java)（MIT）。
 */
object CalendarService {

    /** 今日（含当前时刻）。 */
    fun today(): DayInfo = of(Solar.fromDate(java.util.Date()))

    /**
     * 按公历取日课信息。
     *
     * @param hour 24 小时制。时柱由它决定，取默认值会让时柱不准。
     */
    fun of(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
    ): DayInfo = of(Solar.fromYmdHms(year, month, day, hour, minute, 0))

    fun of(solar: Solar): DayInfo = fromLunar(solar.lunar, solar)

    /**
     * 按**农历**取某一天的信息。
     *
     * 八字页允许用户按农历填生辰，但排盘内核一律吃公历 ——
     * 换算只在这一层做，上层不必知道。
     *
     * [lunarMonth] 传负数表示闰月（lunar 库约定：闰五月 = -5）。
     * 闰月是真实存在的输入（有人就生在闰五月），不能只接受 1..12。
     *
     * @throws IllegalArgumentException 该农历日期不存在（如闰月填错、月大月小不符）
     */
    fun ofLunar(
        lunarYear: Int,
        lunarMonth: Int,
        lunarDay: Int,
        hour: Int,
        minute: Int = 0,
    ): DayInfo {
        val solar = Lunar.fromYmd(lunarYear, lunarMonth, lunarDay).solar
        return of(Solar.fromYmdHms(solar.year, solar.month, solar.day, hour, minute, 0))
    }

    private fun fromLunar(lunar: Lunar, solar: Solar): DayInfo {
        val rawMonth = lunar.month

        val solarInfo = SolarInfo(
            year = solar.year,
            month = solar.month,
            day = solar.day,
            hour = solar.hour,
            minute = solar.minute,
            weekLabel = "星期${solar.weekInChinese}",
            ymd = solar.toYmd(),
        )

        val lunarInfo = LunarInfo(
            year = lunar.year,
            month = kotlin.math.abs(rawMonth),
            day = lunar.day,
            // lunar-java 用负数表示闰月
            isLeapMonth = rawMonth < 0,
            monthLabel = "${if (rawMonth < 0) "闰" else ""}${lunar.monthInChinese}月",
            dayLabel = lunar.dayInChinese,
            yearGanZhi = lunar.yearInGanZhi,
            zodiac = lunar.yearShengXiao,
        )

        val ganZhiInfo = GanZhiInfo(
            year = parseGanZhi(lunar.yearInGanZhi),
            month = parseGanZhi(lunar.monthInGanZhi),
            day = parseGanZhi(lunar.dayInGanZhi),
            hour = parseGanZhi(lunar.timeInGanZhi),
            yearNaYin = lunar.yearNaYin,
            monthNaYin = lunar.monthNaYin,
            dayNaYin = lunar.dayNaYin,
            timeNaYin = lunar.timeNaYin,
        )

        val almanac = AlmanacInfo(
            yi = lunar.dayYi,
            ji = lunar.dayJi,
            clashDescription = lunar.dayChongDesc,
            clashZodiac = lunar.dayChongShengXiao,
            sha = lunar.daySha,
            jiShen = lunar.dayJiShen,
            xiongSha = lunar.dayXiongSha,
            zhiXing = lunar.zhiXing,
            tianShen = lunar.dayTianShen,
            tianShenType = lunar.dayTianShenType,
            tianShenLuck = lunar.dayTianShenLuck,
            xiu = lunar.xiu,
            xiuLuck = lunar.xiuLuck,
            pengZuGan = lunar.pengZuGan,
            pengZuZhi = lunar.pengZuZhi,
            positions = listOf(
                PositionInfo("喜神", lunar.dayPositionXi, lunar.dayPositionXiDesc),
                PositionInfo("福神", lunar.dayPositionFu, lunar.dayPositionFuDesc),
                PositionInfo("财神", lunar.dayPositionCai, lunar.dayPositionCaiDesc),
                PositionInfo("阳贵神", lunar.dayPositionYangGui, lunar.dayPositionYangGuiDesc),
                PositionInfo("阴贵神", lunar.dayPositionYinGui, lunar.dayPositionYinGuiDesc),
            ),
        )

        return DayInfo(
            solar = solarInfo,
            lunar = lunarInfo,
            ganZhi = ganZhiInfo,
            almanac = almanac,
            jieQi = lunar.jieQi.ifBlank { null },
            festivals = (lunar.festivals + lunar.otherFestivals).distinct(),
        )
    }

    /**
     * 十二时辰的吉凶概览。
     *
     * ⚠️ 用的是**时家**黄黑道，不是 `Lunar.getDayTianShen()`（那是**日家**天神）。
     *    早期实现误用了日家天神，导致一天里 12 个时辰吉凶完全相同 —— 显然不对。
     *
     * 时家十二天神自「青龙」起循环，青龙所临的时辰由日支决定：
     *
     * ```
     * 青龙时辰地支序号 = (8 + 2 × 日支序号) mod 12
     * ```
     *
     * 展开即传统口诀「子午青龙起申、丑未起戌、寅申起子、卯酉起寅、辰戌起辰、巳亥起午」。
     * 十二神中黄道六、黑道六，因此每天恰好六个吉时、六个凶时。
     */
    fun hoursOf(year: Int, month: Int, day: Int): List<HourInfo> {
        val lunar = Solar.fromYmdHms(year, month, day, 12, 0, 0).lunar
        val dayBranch = com.maohongyuan.magic.core.model.EarthlyBranch.entries[
            lunar.dayZhi.let { zhi ->
                com.maohongyuan.magic.core.model.EarthlyBranch.ofLabel(zhi)?.ordinal ?: 0
            }
        ]

        return com.maohongyuan.magic.core.model.EarthlyBranch.entries.map { branch ->
            val hour = (branch.hourStart + 1) % 24
            val hourLunar = Solar.fromYmdHms(year, month, day, hour, 0, 0).lunar
            val shen = HourTianShen.of(dayBranch, branch)
            HourInfo(
                branch = branch,
                ganZhi = parseGanZhi(hourLunar.timeInGanZhi),
                tianShen = shen.label,
                tianShenType = if (shen.auspicious) "黄道" else "黑道",
                auspicious = shen.auspicious,
            )
        }
    }
}

/**
 * 时家十二天神（黄黑道）。
 *
 * 自青龙起循环，顺序固定。黄道六、黑道六。
 */
enum class HourTianShen(val label: String, val auspicious: Boolean) {
    QING_LONG("青龙", true),
    MING_TANG("明堂", true),
    TIAN_XING("天刑", false),
    ZHU_QUE("朱雀", false),
    JIN_KUI("金匮", true),
    TIAN_DE("天德", true),
    BAI_HU("白虎", false),
    YU_TANG("玉堂", true),
    TIAN_LAO("天牢", false),
    XUAN_WU("玄武", false),
    SI_MING("司命", true),
    GOU_CHEN("勾陈", false),
    ;

    companion object {
        /**
         * 青龙所临的时辰地支序号。`(8 + 2 × 日支序号) mod 12`。
         */
        fun qingLongHourIndex(dayBranch: com.maohongyuan.magic.core.model.EarthlyBranch): Int =
            (8 + 2 * dayBranch.ordinal) % 12

        /** 取某日某时辰所临的时家天神。 */
        fun of(
            dayBranch: com.maohongyuan.magic.core.model.EarthlyBranch,
            hourBranch: com.maohongyuan.magic.core.model.EarthlyBranch,
        ): HourTianShen {
            val start = qingLongHourIndex(dayBranch)
            val offset = (hourBranch.ordinal - start + 12) % 12
            return entries[offset]
        }
    }
}

/** 单个时辰的吉凶 */
data class HourInfo(
    val branch: com.maohongyuan.magic.core.model.EarthlyBranch,
    val ganZhi: com.maohongyuan.magic.core.model.GanZhi?,
    val tianShen: String,
    /** 黄道 / 黑道 */
    val tianShenType: String,
    val auspicious: Boolean,
) {
    /** 子时 23:00–01:00 */
    val rangeLabel: String
        get() = "%02d–%02d".format(branch.hourStart, (branch.hourStart + 2) % 24)
}
