package com.maohongyuan.magic.core.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 最近节气的测试。
 *
 * `DayInfo.jieQi` 只有节气当天才有值，盘面要的是「现在处于哪个节气」。
 * 这个换算错了不会报错，只会让盘面上少一行或显示错一个节气。
 */
class RecentJieQiTest {

    @Test
    fun `非节气日能取到最近已过的节气`() {
        // 2026 寒露约 10/8，霜降约 10/23 → 10/17 应得「寒露」
        assertEquals("寒露", CalendarService.recentJieQi(2026, 10, 17))
        // 10/9 刚过寒露
        assertEquals("寒露", CalendarService.recentJieQi(2026, 10, 9))
        // 10/24 已过霜降
        assertEquals("霜降", CalendarService.recentJieQi(2026, 10, 24))
    }

    @Test
    fun `节气当天返回节气本身`() {
        // 10/8 是寒露当天
        assertEquals("寒露", CalendarService.recentJieQi(2026, 10, 8))
    }

    @Test
    fun `全年每一天都能取到节气`() {
        var d = java.time.LocalDate.of(2026, 1, 1)
        val end = java.time.LocalDate.of(2026, 12, 31)
        while (!d.isAfter(end)) {
            val jq = CalendarService.recentJieQi(d.year, d.monthValue, d.dayOfMonth)
            assertNotNull("$d 取不到节气", jq)
            d = d.plusDays(1)
        }
    }
}
