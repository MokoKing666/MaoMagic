package com.maohongyuan.magic.core.calendar

import com.maohongyuan.magic.core.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 流年换算的测试。
 *
 * 这一块是**纯算术**，算错会直接给出错误的年份干支 —— 属于「静默出错」，
 * 用户看不出问题但结论全错。所以边界与基准点必须钉死。
 */
class LiuNianServiceTest {

    // ── 年 → 干支 ──────────────────────────────────────────

    @Test
    fun `基准点 1984 是甲子年`() {
        val gz = LiuNianService.yearGanZhi(1984)
        assertEquals("甲", gz.stem.label)
        assertEquals("子", gz.branch.label)
        assertEquals(0, gz.index)
    }

    @Test
    fun `已知年份换算正确`() {
        // 2026 丙午（对应参考盘面）
        assertEquals("丙午", LiuNianService.yearGanZhi(2026).toString())
        // 2025 乙巳
        assertEquals("乙巳", LiuNianService.yearGanZhi(2025).toString())
        // 2000 庚辰
        assertEquals("庚辰", LiuNianService.yearGanZhi(2000).toString())
        // 1949 己丑
        assertEquals("己丑", LiuNianService.yearGanZhi(1949).toString())
    }

    @Test
    fun `六十年一轮回`() {
        val a = LiuNianService.yearGanZhi(1984)
        val b = LiuNianService.yearGanZhi(2044)
        assertEquals(a.index, b.index)
        // 且连续两年必须相邻，不能跳
        val n1 = LiuNianService.yearGanZhi(2026)
        val n2 = LiuNianService.yearGanZhi(2027)
        assertEquals((n1.index + 1) % 60, n2.index)
    }

    // ── 十神（以甲日主为全表基准）─────────────────────────

    @Test
    fun `甲日主的十神全表`() {
        val jia = HeavenlyStem.JIA
        fun s(stem: HeavenlyStem) = LiuNianService.shiShen(jia, stem)

        assertEquals("比肩", s(HeavenlyStem.JIA))      // 甲 同木同阳
        assertEquals("劫财", s(HeavenlyStem.YI))       // 乙 同木异阴
        assertEquals("食神", s(HeavenlyStem.BING))     // 丙 木生火 同阳
        assertEquals("伤官", s(HeavenlyStem.DING))     // 丁 木生火 异阴
        assertEquals("偏财", s(HeavenlyStem.WU))       // 戊 木克土 同阳
        assertEquals("正财", s(HeavenlyStem.JI))       // 己 木克土 异阴
        assertEquals("七杀", s(HeavenlyStem.GENG))     // 庚 金克木 同阳
        assertEquals("正官", s(HeavenlyStem.XIN))      // 辛 金克木 异阴
        assertEquals("偏印", s(HeavenlyStem.REN))      // 壬 水生木 同阳
        assertEquals("正印", s(HeavenlyStem.GUI))      // 癸 水生木 异阴
    }

    @Test
    fun `日主换一个也要对`() {
        // 庚日主：庚见甲应为偏财（庚阳金克甲阳木，同阳）
        assertEquals("偏财", LiuNianService.shiShen(HeavenlyStem.GENG, HeavenlyStem.JIA))
        // 庚见乙为正财（异阴）
        assertEquals("正财", LiuNianService.shiShen(HeavenlyStem.GENG, HeavenlyStem.YI))
        // 庚见丙为七杀（丙火克庚金，同阳）
        assertEquals("七杀", LiuNianService.shiShen(HeavenlyStem.GENG, HeavenlyStem.BING))
    }

    // ── 大运 → 流年 ────────────────────────────────────────

    @Test
    fun `一步大运给出十个流年且年龄年份连续`() {
        val chart = BaZiService.chart(1990, 6, 15, 12, gender = Gender.MALE)
        val period = chart.daYun.first()
        val years = LiuNianService.ofDaYun(chart, period)

        assertEquals("大运一步管十年", 10, years.size)

        years.forEachIndexed { i, ln ->
            assertEquals(period.startYear + i, ln.year)
            assertEquals(period.startAge + i, ln.age)
            // 干支必须逐年相邻
            if (i > 0) {
                assertEquals((years[i - 1].ganZhi.index + 1) % 60, ln.ganZhi.index)
            }
        }

        // 起止年份必须落在该大运区间内
        assertEquals(period.startYear, years.first().year)
        assertEquals(period.endYear, years.last().year)
    }

    @Test
    fun `流年十神与日主一致`() {
        val chart = BaZiService.chart(1985, 3, 20, 8, gender = Gender.FEMALE)
        val years = LiuNianService.ofDaYun(chart, chart.daYun[2])
        years.forEach { ln ->
            assertEquals(
                LiuNianService.shiShen(chart.dayMaster, ln.ganZhi.stem),
                ln.shiShen,
            )
            assertTrue(ln.shiShen.isNotBlank())
        }
    }

    @Test
    fun `大运区间异常时返回空而不是崩`() {
        val chart = BaZiService.chart(1990, 6, 15, 12, gender = Gender.MALE)
        val broken = DaYunPeriod(
            index = 99, startAge = 100, endAge = 90,
            startYear = 2100, endYear = 2090,
            ganZhi = "甲子", xunKong = "戌亥",
        )
        assertTrue(LiuNianService.ofDaYun(chart, broken).isEmpty())
    }
}
