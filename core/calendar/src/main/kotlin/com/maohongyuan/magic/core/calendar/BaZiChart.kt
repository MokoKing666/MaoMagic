package com.maohongyuan.magic.core.calendar

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.GanZhi
import com.maohongyuan.magic.core.model.HeavenlyStem
import com.maohongyuan.magic.core.model.WuXing
import com.nlf.calendar.Solar

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
    /** 该步大运的天干十神（相对日主）。由调用方按需计算，此处只给干支。 */
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
) {
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
 * 同样把 lunar 库封装在内部。
 */
object BaZiService {

    /** 由 lunar 返回的字符串构造一柱。天干地支解析失败时退回甲子，保证不崩。 */
    private fun buildPillar(
        label: String,
        ganLabel: String,
        zhiLabel: String,
        hideGanRaw: List<String>,
        naYin: String,
        shiShenGan: String,
        shiShenZhi: List<String>,
        diShi: String,
    ): Pillar = Pillar(
        label = label,
        gan = HeavenlyStem.ofLabel(ganLabel) ?: HeavenlyStem.JIA,
        zhi = EarthlyBranch.ofLabel(zhiLabel) ?: EarthlyBranch.ZI,
        hideGan = hideGanRaw.mapNotNull { HeavenlyStem.ofLabel(it) },
        naYin = naYin,
        shiShenGan = shiShenGan,
        shiShenZhi = shiShenZhi,
        diShi = diShi,
    )

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
        val solar = Solar.fromYmdHms(year, month, day, hour, minute, 0)
        val lunar = solar.lunar
        val ec = lunar.eightChar

        val built = listOf(
            buildPillar(
                label = "年柱",
                ganLabel = ec.yearGan, zhiLabel = ec.yearZhi,
                hideGanRaw = ec.yearHideGan,
                naYin = ec.yearNaYin, shiShenGan = ec.yearShiShenGan,
                shiShenZhi = ec.yearShiShenZhi, diShi = ec.yearDiShi,
            ),
            buildPillar(
                label = "月柱",
                ganLabel = ec.monthGan, zhiLabel = ec.monthZhi,
                hideGanRaw = ec.monthHideGan,
                naYin = ec.monthNaYin, shiShenGan = ec.monthShiShenGan,
                shiShenZhi = ec.monthShiShenZhi, diShi = ec.monthDiShi,
            ),
            buildPillar(
                label = "日柱",
                ganLabel = ec.dayGan, zhiLabel = ec.dayZhi,
                hideGanRaw = ec.dayHideGan,
                naYin = ec.dayNaYin, shiShenGan = ec.dayShiShenGan,
                shiShenZhi = ec.dayShiShenZhi, diShi = ec.dayDiShi,
            ),
            buildPillar(
                label = "时柱",
                ganLabel = ec.timeGan, zhiLabel = ec.timeZhi,
                hideGanRaw = ec.timeHideGan,
                naYin = ec.timeNaYin, shiShenGan = ec.timeShiShenGan,
                shiShenZhi = ec.timeShiShenZhi, diShi = ec.timeDiShi,
            ),
        )

        val dayMaster = built[2].gan

        // 五行统计：只计 4 干 + 4 支，不含藏干 —— 这是最通行的口径
        val counts = WuXing.entries.associateWith { 0 }.toMutableMap()
        built.forEach { pillar ->
            counts[pillar.gan.wuXing] = counts.getValue(pillar.gan.wuXing) + 1
            counts[pillar.zhi.wuXing] = counts.getValue(pillar.zhi.wuXing) + 1
        }

        val yun = ec.getYun(if (gender == Gender.MALE) 1 else 0)
        val daYun = yun.daYun
            .drop(1) // 第 0 步是「起运前」，不展示
            .take(daYunCount)
            .map { d ->
                DaYunPeriod(
                    index = d.index,
                    startAge = d.startAge,
                    endAge = d.endAge,
                    startYear = d.startYear,
                    endYear = d.endYear,
                    ganZhi = d.ganZhi,
                    xunKong = d.xunKong,
                )
            }

        return BaZiChart(
            solar = SolarInfo(
                year = solar.year,
                month = solar.month,
                day = solar.day,
                hour = solar.hour,
                minute = solar.minute,
                weekLabel = "星期${solar.weekInChinese}",
                ymd = solar.toYmd(),
            ),
            lunar = LunarInfo(
                year = lunar.year,
                month = kotlin.math.abs(lunar.month),
                day = lunar.day,
                isLeapMonth = lunar.month < 0,
                monthLabel = "${if (lunar.month < 0) "闰" else ""}${lunar.monthInChinese}月",
                dayLabel = lunar.dayInChinese,
                yearGanZhi = lunar.yearInGanZhi,
                zodiac = lunar.yearShengXiao,
            ),
            gender = gender,
            year = built[0],
            month = built[1],
            day = built[2],
            hour = built[3],
            dayMaster = dayMaster,
            wuXingCount = counts,
            taiYuan = ec.taiYuan,
            taiYuanNaYin = ec.taiYuanNaYin,
            mingGong = ec.mingGong,
            mingGongNaYin = ec.mingGongNaYin,
            shenGong = ec.shenGong,
            shenGongNaYin = ec.shenGongNaYin,
            startAge = yun.startYear,
            forward = yun.isForward,
            daYun = daYun,
        )
    }
}
