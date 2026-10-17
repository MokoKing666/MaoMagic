package com.maohongyuan.magic.core.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 农历入口测试。
 *
 * 八字页允许按农历填生辰，换算在这一层做。这里只验证一件事：
 * **农历 → 公历 → 农历 必须能还原**。还原不了说明换算错了，
 * 而错了不会报错，只会让整个命盘偏移。
 */
class LunarInputTest {

    @Test
    fun `农历转公历再转回来能还原`() {
        // 覆盖多个年份与月份，含月大月小、跨年边界
        val samples = listOf(
            Triple(1990, 5, 23),
            Triple(1990, 12, 30),
            Triple(2000, 1, 1),
            Triple(2024, 2, 29),   // 闰年二月
            Triple(2026, 9, 7),
            Triple(1985, 3, 20),
        )
        samples.forEach { (y, m, d) ->
            val info = CalendarService.ofLunar(y, m, d, 12)
            assertEquals("农历 $y-$m-$d 换算后年份不对", y, info.lunar.year)
            assertEquals("农历 $y-$m-$d 换算后月份不对", m, info.lunar.month)
            assertEquals("农历 $y-$m-$d 换算后日不对", d, info.lunar.day)
        }
    }

    @Test
    fun `换算出的公历日期合法`() {
        for (y in listOf(1990, 2000, 2026)) {
            for (m in 1..12) {
                val info = CalendarService.ofLunar(y, m, 15, 12)
                assertTrue("公历年越界", info.solar.year in 1899..2101)
                assertTrue("公历月越界", info.solar.month in 1..12)
                assertTrue("公历日越界", info.solar.day in 1..31)
            }
        }
    }

    @Test
    fun `农历与公历口径互不干扰`() {
        // 同一天：按公历取 与 按农历取（换算后）应得到同一个干支
        val bySolar = CalendarService.of(2026, 10, 17, 12)
        val lunarMonth = bySolar.lunar.month
        val lunarDay = bySolar.lunar.day
        val byLunar = CalendarService.ofLunar(bySolar.lunar.year, lunarMonth, lunarDay, 12)
        assertEquals("两种口径的日柱应一致", bySolar.ganZhi.day, byLunar.ganZhi.day)
        assertEquals("两种口径的公历日期应一致", bySolar.solar.ymd, byLunar.solar.ymd)
    }
}
