package com.maohongyuan.magic.core.calendar

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.HeavenlyStem
import com.tyme.lunar.LunarDay
import com.tyme.sixtycycle.SixtyCycle
import com.tyme.solar.SolarDay

/**
 * 历法内核统一入口。
 *
 * 职责边界：**tyme 库完全封装在本模块内部**，对外只暴露本项目自己的领域类型
 * （[DayInfo] / [BaZiChart]）。这样将来若要替换历法实现，改动只在本模块内。
 *
 * 数据来源：[cn.6tail:tyme4kt](https://github.com/6tail/tyme4kt)（MIT）。
 *
 * ── 从 lunar 迁移过来时补了什么 ──
 * tyme 缺的四组数据自己算，见 [AlmanacSupplement] 与 [BaZiSupplement]：
 * 冲 / 煞 / 彭祖 / 五神方位 / 宿吉凶 / 农历月名日名 / 十神 / 藏干 / 地势。
 * 每一项都有对拍测试（拿 lunar 当 oracle，730 天零差异）。
 */
object CalendarService {

    /** 今日（含当前时刻）。 */
    fun today(): DayInfo {
        val now = java.time.LocalDateTime.now()
        return of(now.year, now.monthValue, now.dayOfMonth, now.hour, now.minute)
    }

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
    ): DayInfo = build(SolarDay.fromYmd(year, month, day), hour, minute)

    /**
     * 最近一个**已过**的节气名。
     *
     * tyme 的 `SolarDay.getTerm()` 直接给出「当前所处节气」，
     * 非节气日也返回（如 10-17 → 寒露）。所以这里不用再往回扫 16 天。
     */
    fun recentJieQi(year: Int, month: Int, day: Int): String? =
        SolarDay.fromYmd(year, month, day).getTerm()?.getName()

    /**
     * 按**农历**取某一天的信息。
     *
     * 八字页允许用户按农历填生辰，但排盘内核一律吃公历 ——
     * 换算只在这一层做，上层不必知道。
     *
     * [lunarMonth] 传负数表示闰月（沿用 lunar 库约定：闰五月 = -5）。
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
        val day = LunarDay.fromYmd(lunarYear, lunarMonth, lunarDay)
            ?: throw IllegalArgumentException("农历日期不存在：$lunarYear 年 $lunarMonth 月 $lunarDay 日")
        val solar = day.getSolarDay()
        return of(solar.year, solar.month, solar.day, hour, minute)
    }

    private fun build(solarDay: SolarDay, hour: Int, minute: Int): DayInfo {
        val y = solarDay.year
        val m = solarDay.month
        val d = solarDay.day

        val lunarDay: LunarDay = solarDay.getLunarDay()
        val lunarMonth = lunarDay.getLunarMonth()
        val lunarYear = lunarMonth.getLunarYear()

        val cycleDay = solarDay.getSixtyCycleDay()
        val dayCycle: SixtyCycle = cycleDay.getSixtyCycle()
        val dayStem = HeavenlyStem.entries[dayCycle.getHeavenStem().getIndex()]
        val dayBranch = EarthlyBranch.entries[dayCycle.getEarthBranch().getIndex()]

        // 时辰干支：十二时辰自子起，index = ((hour + 1) / 2) % 12
        val hourIndex = ((hour + 1) / 2) % 12
        val hourCycle = cycleDay.getHours()[hourIndex].getSixtyCycle()

        val solarInfo = SolarInfo(
            year = y,
            month = m,
            day = d,
            hour = hour,
            minute = minute,
            weekLabel = "星期" + solarDay.getWeek().getName(),
            ymd = "%04d-%02d-%02d".format(y, m, d),
        )

        val rawMonth = lunarMonth.getMonthWithLeap()
        val lunarInfo = LunarInfo(
            year = lunarYear.year,
            month = kotlin.math.abs(rawMonth),
            day = lunarDay.day,
            isLeapMonth = rawMonth < 0,
            monthLabel = "${if (rawMonth < 0) "闰" else ""}${AlmanacSupplement.monthName(kotlin.math.abs(rawMonth))}月",
            dayLabel = AlmanacSupplement.dayName(lunarDay.day),
            yearGanZhi = lunarYear.getSixtyCycle().toString(),
            zodiac = AlmanacSupplement.zodiac(EarthlyBranch.entries[lunarYear.getSixtyCycle().getEarthBranch().getIndex()]),
        )

        val ganZhiInfo = GanZhiInfo(
            year = toGanZhi(lunarYear.getSixtyCycle()),
            month = toGanZhi(cycleDay.getMonth()),
            day = toGanZhi(dayCycle),
            hour = toGanZhi(hourCycle),
            yearNaYin = lunarYear.getSixtyCycle().getSound().getName(),
            monthNaYin = cycleDay.getMonth().getSound().getName(),
            dayNaYin = dayCycle.getSound().getName(),
            timeNaYin = hourCycle.getSound().getName(),
        )

        // 吉凶神：tyme 的 getGods() 是「吉神在前、凶煞在后」，
        // 但分界点没暴露 —— 按每个 God 自己的吉凶属性拆
        val gods = lunarDay.getGods()
        val jiShen = gods.filter { it.getLuck().getName() == "吉" }.map { it.getName() }
        val xiongSha = gods.filter { it.getLuck().getName() == "凶" }.map { it.getName() }

        val clash = AlmanacSupplement.clash(toSixtyIndex(dayCycle))
        val (pengZuGan, pengZuZhi) = AlmanacSupplement.pengZu(dayStem, dayBranch)
        val xiuName = lunarDay.getTwentyEightStar().getName()
        val ecliptic = lunarDay.getTwelveStar().getEcliptic().getName()

        val almanac = AlmanacInfo(
            // 宜忌为空时补「无」—— 黄历传统写法，也是 UI 期望的（空列表会渲染成一片空白）
            yi = lunarDay.getRecommends().map { it.getName() }.ifEmpty { listOf("无") },
            ji = lunarDay.getAvoids().map { it.getName() }.ifEmpty { listOf("无") },
            clashDescription = clash.description,
            clashZodiac = clash.zodiac,
            sha = AlmanacSupplement.sha(dayBranch),
            jiShen = jiShen,
            xiongSha = xiongSha,
            zhiXing = lunarDay.getDuty().getName(),
            tianShen = lunarDay.getTwelveStar().getName(),
            tianShenType = ecliptic,
            tianShenLuck = if (ecliptic == "黄道") "吉" else "凶",
            xiu = xiuName,
            xiuLuck = AlmanacSupplement.xiuLuck(xiuName),
            pengZuGan = pengZuGan,
            pengZuZhi = pengZuZhi,
            positions = AlmanacSupplement.positions(dayStem),
        )

        return DayInfo(
            solar = solarInfo,
            lunar = lunarInfo,
            ganZhi = ganZhiInfo,
            almanac = almanac,
            // 语义修正：字段名叫 jieQi，但界面要的是「当前所处节气」。
            // tyme 的 getTerm() 正是这个语义，非节气日也有值。
            jieQi = solarDay.getTerm()?.getName(),
            festivals = listOfNotNull(lunarDay.getFestival()?.getName()).filter { it.isNotBlank() },
        )
    }

    private fun toGanZhi(cycle: SixtyCycle): com.maohongyuan.magic.core.model.GanZhi? {
        val stem = HeavenlyStem.entries.getOrNull(cycle.getHeavenStem().getIndex()) ?: return null
        val branch = EarthlyBranch.entries.getOrNull(cycle.getEarthBranch().getIndex()) ?: return null
        return com.maohongyuan.magic.core.model.GanZhi.of(stem, branch)
    }

    /** 六十甲子序号 */
    private fun toSixtyIndex(cycle: SixtyCycle): Int {
        val s = cycle.getHeavenStem().getIndex()
        val b = cycle.getEarthBranch().getIndex()
        for (i in 0 until 60) if (i % 10 == s && i % 12 == b) return i
        return 0
    }

    /**
     * 十二时辰的吉凶概览。
     *
     * ⚠️ 用的是**时家**黄黑道，不是 `LunarDay.getTwelveStar()`（那是**日家**天神）。
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
        val solarDay = SolarDay.fromYmd(year, month, day)
        val cycleDay = solarDay.getSixtyCycleDay()
        val dayBranch = EarthlyBranch.entries[cycleDay.getSixtyCycle().getEarthBranch().getIndex()]
        val hourCycles = cycleDay.getHours()

        return EarthlyBranch.entries.mapIndexed { i, branch ->
            val shen = HourTianShen.of(dayBranch, branch)
            HourInfo(
                branch = branch,
                ganZhi = toGanZhi(hourCycles[i].getSixtyCycle()),
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
        /** 青龙所临的时辰地支序号。`(8 + 2 × 日支序号) mod 12`。 */
        fun qingLongHourIndex(dayBranch: EarthlyBranch): Int = (8 + 2 * dayBranch.ordinal) % 12

        /** 取某日某时辰所临的时家天神。 */
        fun of(dayBranch: EarthlyBranch, hourBranch: EarthlyBranch): HourTianShen {
            val start = qingLongHourIndex(dayBranch)
            val offset = (hourBranch.ordinal - start + 12) % 12
            return entries[offset]
        }
    }
}

/** 单个时辰的吉凶 */
data class HourInfo(
    val branch: EarthlyBranch,
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
