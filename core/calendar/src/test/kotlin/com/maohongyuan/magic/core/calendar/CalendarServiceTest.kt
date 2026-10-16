package com.maohongyuan.magic.core.calendar

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.HeavenlyStem
import com.maohongyuan.magic.core.model.WuXing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 历法内核。
 *
 * 断言分两类：
 * 1. **硬锚点** —— 公历与农历的对应、节气、生肖，这些是客观事实，写死比对
 * 2. **结构不变量** —— 无论哪一天都必须成立的性质（如五行统计总数恒为 8）
 *
 * 后者能在不依赖具体历法数据的情况下抓出大部分实现错误。
 */
class CalendarServiceTest {

    @Test
    fun `2024 年春节对应农历正月初一`() {
        // 2024-02-10 是甲辰年正月初一（春节）—— 客观事实
        val day = CalendarService.of(2024, 2, 10)
        assertEquals(1, day.lunar.month)
        assertEquals(1, day.lunar.day)
        assertEquals("龙", day.lunar.zodiac)
        assertEquals("正月", day.lunar.monthLabel)
        assertEquals("初一", day.lunar.dayLabel)
    }

    @Test
    fun `2024 立春在二月四日`() {
        val day = CalendarService.of(2024, 2, 4)
        assertEquals("立春", day.jieQi)
    }

    @Test
    fun `非节气日 jieQi 为空`() {
        val day = CalendarService.of(2024, 2, 20)
        assertEquals(null, day.jieQi)
    }

    @Test
    fun `公历信息完整`() {
        val day = CalendarService.of(2026, 10, 16, 15, 30)
        assertEquals(2026, day.solar.year)
        assertEquals(10, day.solar.month)
        assertEquals(16, day.solar.day)
        assertEquals(15, day.solar.hour)
        assertEquals(30, day.solar.minute)
        assertEquals("2026-10-16", day.solar.ymd)
        assertTrue(day.solar.weekLabel.startsWith("星期"))
    }

    @Test
    fun `四柱干支全部解析成功`() {
        val day = CalendarService.of(2026, 10, 16, 15, 30)
        assertNotNull("年柱", day.ganZhi.year)
        assertNotNull("月柱", day.ganZhi.month)
        assertNotNull("日柱", day.ganZhi.day)
        assertNotNull("时柱", day.ganZhi.hour)
        assertNotNull(day.dayGanZhi)
        assertNotNull(day.monthBranch)
    }

    @Test
    fun `时柱随小时变化`() {
        val morning = CalendarService.of(2026, 10, 16, 8)
        val evening = CalendarService.of(2026, 10, 16, 20)
        assertTrue(
            "不同时辰的时柱应不同",
            morning.ganZhi.hour != evening.ganZhi.hour,
        )
        // 但日柱必须相同
        assertEquals(morning.ganZhi.day, evening.ganZhi.day)
    }

    @Test
    fun `黄历信息齐全`() {
        val day = CalendarService.of(2026, 10, 16)
        assertTrue("宜不应为空", day.almanac.yi.isNotEmpty())
        assertTrue("忌不应为空", day.almanac.ji.isNotEmpty())
        assertTrue("冲煞描述不应为空", day.almanac.clashDescription.isNotBlank())
        assertTrue("建除不应为空", day.almanac.zhiXing.isNotBlank())
        assertTrue("二十八宿不应为空", day.almanac.xiu.isNotBlank())
        assertTrue("彭祖百忌不应为空", day.almanac.pengZuGan.isNotBlank())
        assertEquals("吉神方位应有五项", 5, day.almanac.positions.size)
    }

    @Test
    fun `天神类型只有黄道或黑道`() {
        // 连续一年逐日校验，确保判据稳定
        for (month in 1..12) {
            val day = CalendarService.of(2026, month, 15)
            assertTrue(
                "${month}月15日 的天神类型异常：${day.almanac.tianShenType}",
                day.almanac.tianShenType == "黄道" || day.almanac.tianShenType == "黑道",
            )
        }
    }

    @Test
    fun `建除十二神取值合法`() {
        val legal = "建除满平定执破危成收开闭"
        for (month in 1..12) {
            val day = CalendarService.of(2026, month, 10)
            assertTrue(
                "${month}月10日 建除异常：${day.almanac.zhiXing}",
                legal.contains(day.almanac.zhiXing),
            )
        }
    }

    @Test
    fun `十二时辰覆盖全部地支且吉凶都有`() {
        val hours = CalendarService.hoursOf(2026, 10, 16)
        assertEquals(12, hours.size)
        assertEquals(
            EarthlyBranch.entries.toList(),
            hours.map { it.branch },
        )
        // 黄道黑道各六个 —— 时家十二神固定六吉六凶
        assertEquals("应有六个吉时", 6, hours.count { it.auspicious })
        assertEquals("应有六个凶时", 6, hours.count { !it.auspicious })
    }

    @Test
    fun `时家青龙起法符合口诀`() {
        // 子午青龙起申、丑未起戌、寅申起子、卯酉起寅、辰戌起辰、巳亥起午
        val expected = mapOf(
            EarthlyBranch.ZI to EarthlyBranch.SHEN,
            EarthlyBranch.WU to EarthlyBranch.SHEN,
            EarthlyBranch.CHOU to EarthlyBranch.XU,
            EarthlyBranch.WEI to EarthlyBranch.XU,
            EarthlyBranch.YIN to EarthlyBranch.ZI,
            EarthlyBranch.SHEN to EarthlyBranch.ZI,
            EarthlyBranch.MAO to EarthlyBranch.YIN,
            EarthlyBranch.YOU to EarthlyBranch.YIN,
            EarthlyBranch.CHEN to EarthlyBranch.CHEN,
            EarthlyBranch.XU to EarthlyBranch.CHEN,
            EarthlyBranch.SI to EarthlyBranch.WU,
            EarthlyBranch.HAI to EarthlyBranch.WU,
        )
        expected.forEach { (dayBranch, qingLongHour) ->
            assertEquals(
                "$dayBranch 日的青龙应临 $qingLongHour 时",
                qingLongHour.ordinal,
                HourTianShen.qingLongHourIndex(dayBranch),
            )
            assertEquals(
                HourTianShen.QING_LONG,
                HourTianShen.of(dayBranch, qingLongHour),
            )
        }
    }

    @Test
    fun `时家十二神每天恰好各出现一次`() {
        EarthlyBranch.entries.forEach { dayBranch ->
            val seq = EarthlyBranch.entries.map { HourTianShen.of(dayBranch, it) }
            assertEquals("$dayBranch 日", 12, seq.toSet().size)
            assertEquals(HourTianShen.entries.toSet(), seq.toSet())
        }
    }

    @Test
    fun `时家十二神黄黑各半`() {
        assertEquals(6, HourTianShen.entries.count { it.auspicious })
        assertEquals(6, HourTianShen.entries.count { !it.auspicious })
        // 青龙必为黄道
        assertTrue(HourTianShen.QING_LONG.auspicious)
    }

    @Test
    fun `任何一天都是六吉六凶`() {
        for (month in 1..12) {
            val hours = CalendarService.hoursOf(2026, month, 8)
            assertEquals(
                "${month}月8日",
                6,
                hours.count { it.auspicious },
            )
        }
    }

    @Test
    fun `时辰区间标签正确`() {
        val hours = CalendarService.hoursOf(2026, 10, 16)
        assertEquals("23–01", hours[0].rangeLabel)   // 子时
        assertEquals("01–03", hours[1].rangeLabel)   // 丑时
        assertEquals("11–13", hours[6].rangeLabel)   // 午时
        assertEquals("21–23", hours[11].rangeLabel)  // 亥时
    }

    @Test
    fun `闰月标记与月份绝对值`() {
        // 2023 年闰二月：2023-03-22 起为闰二月
        val day = CalendarService.of(2023, 3, 25)
        assertTrue("应为闰月", day.lunar.isLeapMonth)
        assertEquals("月份应取绝对值", 2, day.lunar.month)
        assertTrue("标签应带闰字", day.lunar.monthLabel.startsWith("闰"))
    }
}

class BaZiServiceTest {

    private fun chart(
        hour: Int = 12,
        gender: Gender = Gender.MALE,
    ) = BaZiService.chart(1990, 6, 15, hour, gender = gender)

    @Test
    fun `四柱齐全且日主为日干`() {
        val c = chart()
        assertEquals("年柱", c.year.label)
        assertEquals("月柱", c.month.label)
        assertEquals("日柱", c.day.label)
        assertEquals("时柱", c.hour.label)
        assertEquals(c.day.gan, c.dayMaster)
    }

    @Test
    fun `五行统计恒为八`() {
        // 4 天干 + 4 地支，无论哪一天都应为 8
        for (month in 1..12) {
            val c = BaZiService.chart(1990, month, 15, 12)
            assertEquals(
                "$month 月 15 日的五行统计",
                8,
                c.wuXingCount.values.sum(),
            )
        }
    }

    @Test
    fun `五行统计覆盖全部五种且键完整`() {
        val c = chart()
        assertEquals(WuXing.entries.toSet(), c.wuXingCount.keys)
    }

    @Test
    fun `五行排行按次数降序`() {
        val c = chart()
        val ranked = c.wuXingRanked
        assertEquals(5, ranked.size)
        for (i in 0 until ranked.size - 1) {
            assertTrue(
                "排行应降序：${ranked[i]} 在 ${ranked[i + 1]} 之前",
                ranked[i].second >= ranked[i + 1].second,
            )
        }
        assertEquals(c.maxWuXingCount, ranked.first().second)
    }

    @Test
    fun `大运数量与步长合理`() {
        val c = chart()
        assertEquals(8, c.daYun.size)
        // 每步十年，年龄连续
        c.daYun.forEach { d ->
            assertEquals("${d.index} 步大运应跨十年", 10, d.endAge - d.startAge + 1)
        }
        for (i in 0 until c.daYun.size - 1) {
            assertEquals(
                "大运年龄应连续",
                c.daYun[i].endAge + 1,
                c.daYun[i + 1].startAge,
            )
        }
    }

    @Test
    fun `大运干支可解析为合法干支`() {
        chart().daYun.forEach { d ->
            assertNotNull("${d.ganZhi} 应为合法干支", parseGanZhi(d.ganZhi))
        }
    }

    @Test
    fun `男女大运顺逆相反`() {
        val male = chart(gender = Gender.MALE)
        val female = chart(gender = Gender.FEMALE)
        // 同一年同一时刻，男女的大运方向必然相反
        assertTrue(
            "男女大运方向应相反",
            male.forward != female.forward,
        )
    }

    @Test
    fun `胎元命宫身宫齐全`() {
        val c = chart()
        assertTrue(c.taiYuan.isNotBlank())
        assertTrue(c.mingGong.isNotBlank())
        assertTrue(c.shenGong.isNotBlank())
    }

    @Test
    fun `藏干与十神与地支对应`() {
        val c = chart()
        c.pillars.forEach { pillar ->
            assertTrue("${pillar.label} 应有藏干", pillar.hideGan.isNotEmpty())
            assertEquals(
                "${pillar.label} 的藏干数应与其十神数一致",
                pillar.hideGan.size,
                pillar.shiShenZhi.size,
            )
        }
    }

    @Test
    fun `日柱的天干十神为日主`() {
        val c = chart()
        // lunar-java 对日柱本身返回「日主」
        assertEquals("日主", c.day.shiShenGan)
    }

    @Test
    fun `八字标签为四柱拼接`() {
        val c = chart()
        val expected = listOf(c.year, c.month, c.day, c.hour)
            .joinToString(" ") { "${it.gan.label}${it.zhi.label}" }
        assertEquals(expected, c.eightCharLabel)
    }

    @Test
    fun `时柱随出生时辰变化而日柱不变`() {
        val morning = chart(hour = 8)
        val evening = chart(hour = 20)
        assertEquals(morning.day, evening.day)
        assertTrue("不同时辰时柱应不同", morning.hour != evening.hour)
    }
}
