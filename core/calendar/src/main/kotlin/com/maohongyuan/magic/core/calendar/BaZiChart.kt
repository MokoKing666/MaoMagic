package com.maohongyuan.magic.core.calendar

import com.maohongyuan.magic.core.model.DivinationChart
import com.maohongyuan.magic.core.model.DivinationSystem
import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.GanZhi
import com.maohongyuan.magic.core.model.HeavenlyStem
import com.maohongyuan.magic.core.model.WuXing
import com.tyme.eightchar.ChildLimit
import com.tyme.eightchar.DecadeFortune
import com.tyme.eightchar.EightChar
import com.tyme.sixtycycle.SixtyCycle
import com.tyme.solar.SolarTime

enum class Gender(val label: String) {
    MALE("男"),
    FEMALE("女"),
}

/**
 * 一柱（年 / 月 / 日 / 时）。
 */
data class Pillar(
    /** 年柱 / 月柱 / 日柱 / 时柱 */
    val label: String,
    val gan: HeavenlyStem,
    val zhi: EarthlyBranch,
    /** 藏干（人元），顺序为本气、中气、余气 */
    val hideGan: List<HeavenlyStem>,
    val naYin: String,
    /** 天干十神。日柱本身为「日主」 */
    val shiShenGan: String,
    /** 地支各藏干的十神 */
    val shiShenZhi: List<String>,
    /** 地势（长生十二宫），以日干为主 */
    val diShi: String,
) {
    val ganZhi: GanZhi get() = GanZhi.of(gan, zhi) ?: GanZhi(0)

    val label2: String get() = "${gan.label}${zhi.label}"
}

/**
 * 一步大运。
 */
data class DaYunPeriod(
    val index: Int,
    val startAge: Int,
    val endAge: Int,
    val startYear: Int,
    val endYear: Int,
    val ganZhi: String,
    val xunKong: String,
) {
    val label: String get() = "$startAge–$endAge 岁"
}

/**
 * 八字命盘。
 */
data class BaZiChart(
    val solar: SolarInfo,
    val lunar: LunarInfo,
    val gender: Gender,
    val year: Pillar,
    val month: Pillar,
    val day: Pillar,
    val hour: Pillar,
    /** 日主（日干）—— 命盘的核心 */
    val dayMaster: HeavenlyStem,
    /** 四柱八字中五行的出现次数（仅计 4 干 + 4 支，不含藏干） */
    val wuXingCount: Map<WuXing, Int>,
    val taiYuan: String,
    val taiYuanNaYin: String,
    val mingGong: String,
    val mingGongNaYin: String,
    val shenGong: String,
    val shenGongNaYin: String,
    /** 起运年龄 */
    val startAge: Int,
    /** 大运顺行 / 逆行 */
    val forward: Boolean,
    val daYun: List<DaYunPeriod>,
) : DivinationChart {
    override val system: DivinationSystem = DivinationSystem.BA_ZI

    val pillars: List<Pillar> get() = listOf(year, month, day, hour)

    /** 命盘四柱，如「庚午 壬午 辛酉 壬辰」 */
    val eightCharLabel: String get() = pillars.joinToString(" ") { it.label2 }

    /** 五行分布的最大值，供图表归一化 */
    val maxWuXingCount: Int get() = wuXingCount.values.maxOrNull() ?: 0

    /** 相对日主的五行强弱排序（出现次数从多到少） */
    val wuXingRanked: List<Pair<WuXing, Int>>
        get() = WuXing.entries.map { it to (wuXingCount[it] ?: 0) }
            .sortedByDescending { it.second }
}

/**
 * 八字排盘。
 *
 * 同样把 tyme 库封装在内部。
 *
 * 十神 / 藏干 / 地势 tyme 没有，由 [BaZiSupplement] 自研
 * （已拿 lunar 当 oracle 对拍 730 天零差异）。
 */
object BaZiService {

    /**
     * 排盘。
     *
     * @param hour 24 小时制。**时柱完全由它决定**，不传或传错会让时柱不准。
     * @param gender 影响大运的顺逆 —— 阳年男/阴年女顺行，反之逆行。
     */
    fun chart(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0,
        gender: Gender = Gender.MALE,
        daYunCount: Int = 8,
    ): BaZiChart {
        val time = SolarTime(year, month, day, hour, minute, 0)
        val solarDay = time.getSolarDay()
        val cycleDay = solarDay.getSixtyCycleDay()
        val ec = EightChar(
            cycleDay.getYear(),
            cycleDay.getMonth(),
            cycleDay.getSixtyCycle(),
            cycleDay.getHours()[((hour + 1) / 2) % 12].getSixtyCycle(),
        )

        val dayMaster = toStem(ec.getDay())
        val built = listOf(
            buildPillar("年柱", ec.getYear(), dayMaster),
            buildPillar("月柱", ec.getMonth(), dayMaster),
            buildPillar("日柱", ec.getDay(), dayMaster, isDayMaster = true),
            buildPillar("时柱", ec.getHour(), dayMaster),
        )

        // 五行统计：只计 4 干 + 4 支，不含藏干 —— 这是最通行的口径
        val counts = WuXing.entries.associateWith { 0 }.toMutableMap()
        built.forEach { pillar ->
            counts[pillar.gan.wuXing] = counts.getValue(pillar.gan.wuXing) + 1
            counts[pillar.zhi.wuXing] = counts.getValue(pillar.zhi.wuXing) + 1
        }

        val child = ChildLimit(time, if (gender == Gender.MALE) com.tyme.enums.Gender.MAN else com.tyme.enums.Gender.WOMAN)
        val daYun = (0 until daYunCount).mapNotNull { i ->
            runCatching { DecadeFortune.fromChildLimit(child, i) }.getOrNull()?.let { d ->
                DaYunPeriod(
                    index = i + 1,
                    startAge = d.getStartAge(),
                    endAge = d.getEndAge(),
                    startYear = d.getStartSixtyCycleYear().getYear(),
                    endYear = d.getEndSixtyCycleYear().getYear(),
                    ganZhi = d.getSixtyCycle().toString(),
                    xunKong = xunKong(d.getSixtyCycle()),
                )
            }
        }

        val lunarDay = solarDay.getLunarDay()
        val lunarMonth = lunarDay.getLunarMonth()
        val lunarYear = lunarMonth.getLunarYear()
        val rawMonth = lunarMonth.getMonthWithLeap()

        return BaZiChart(
            solar = SolarInfo(
                year = year,
                month = month,
                day = day,
                hour = hour,
                minute = minute,
                weekLabel = "星期" + solarDay.getWeek().getName(),
                ymd = "%04d-%02d-%02d".format(year, month, day),
            ),
            lunar = LunarInfo(
                year = lunarYear.year,
                month = kotlin.math.abs(rawMonth),
                day = lunarDay.day,
                isLeapMonth = rawMonth < 0,
                monthLabel = "${if (rawMonth < 0) "闰" else ""}${AlmanacSupplement.monthName(kotlin.math.abs(rawMonth))}月",
                dayLabel = AlmanacSupplement.dayName(lunarDay.day),
                yearGanZhi = lunarYear.getSixtyCycle().toString(),
                zodiac = AlmanacSupplement.zodiac(EarthlyBranch.entries[lunarYear.getSixtyCycle().getEarthBranch().getIndex()]),
            ),
            gender = gender,
            year = built[0],
            month = built[1],
            day = built[2],
            hour = built[3],
            dayMaster = dayMaster,
            wuXingCount = counts,
            taiYuan = ec.getFetalOrigin().toString(),
            taiYuanNaYin = ec.getFetalOrigin().getSound().getName(),
            mingGong = ec.getOwnSign().toString(),
            mingGongNaYin = ec.getOwnSign().getSound().getName(),
            shenGong = ec.getBodySign().toString(),
            shenGongNaYin = ec.getBodySign().getSound().getName(),
            startAge = child.getYearCount(),
            forward = child.isForward(),
            daYun = daYun,
        )
    }

    private fun toStem(cycle: SixtyCycle): HeavenlyStem =
        HeavenlyStem.entries[cycle.getHeavenStem().getIndex()]

    private fun toBranch(cycle: SixtyCycle): EarthlyBranch =
        EarthlyBranch.entries[cycle.getEarthBranch().getIndex()]

    private fun buildPillar(
        label: String,
        cycle: SixtyCycle,
        dayMaster: HeavenlyStem,
        isDayMaster: Boolean = false,
    ): Pillar {
        val gan = toStem(cycle)
        val zhi = toBranch(cycle)
        val hides = BaZiSupplement.hideStems(zhi)
        return Pillar(
            label = label,
            gan = gan,
            zhi = zhi,
            hideGan = hides,
            naYin = cycle.getSound().getName(),
            // 日干自身按惯例显示「日主」，不叫「比肩」—— 日主是坐标原点
            shiShenGan = if (isDayMaster) "日主" else BaZiSupplement.tenGod(dayMaster, gan),
            shiShenZhi = hides.map { BaZiSupplement.tenGod(dayMaster, it) },
            diShi = BaZiSupplement.terrain(dayMaster, zhi),
        )
    }

    /**
     * 旬空。
     *
     * 六旬各空两支：甲子旬空戌亥、甲戌旬空申酉、甲申旬空午未、
     * 甲午旬空辰巳、甲辰旬空寅卯、甲寅旬空子丑。
     */
    private fun xunKong(cycle: SixtyCycle): String {
        val s = cycle.getHeavenStem().getIndex()
        val b = cycle.getEarthBranch().getIndex()
        var idx = 0
        for (i in 0 until 60) if (i % 10 == s && i % 12 == b) { idx = i; break }
        // 六旬各空两支，首个空支 = (10 - 2 × 旬序) mod 12
        val xun = idx / 10
        val first = (10 - 2 * xun) % 12
        return EarthlyBranch.entries[first].label + EarthlyBranch.entries[(first + 1) % 12].label
    }
}
