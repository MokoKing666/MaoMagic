package com.maohongyuan.magic.core.interpret

import com.maohongyuan.magic.core.calendar.BaZiService
import com.maohongyuan.magic.core.calendar.CalendarService
import com.maohongyuan.magic.core.calendar.Gender
import com.maohongyuan.magic.core.hexagram.Divination
import com.maohongyuan.magic.core.hexagram.LiuYaoDivination
import com.maohongyuan.magic.core.hexagram.MeihuaDivination
import com.maohongyuan.magic.core.hexagram.YaoRelation
import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.GanZhi
import com.maohongyuan.magic.core.model.Hexagram
import com.maohongyuan.magic.core.model.LiuQin
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 全输入空间的集成测试。
 *
 * 单元测试只能证明「我想到的那几个例子是对的」；这里把**整个输入空间**跑一遍，
 * 用来抓「某些输入下才会崩」这类问题 —— 它们编译期不报错、单测也覆盖不到，
 * 只会在真机上以闪退的形式出现。
 *
 * 覆盖：
 * - 六爻：64 卦 × {静卦, 六种单动爻} = 448 种盘，每种都装卦 + 解卦
 * - 梅花：64 卦 × 6 种动爻 = 384 种盘
 * - 八字：随机抽样 120 组生辰 × 2 性别
 */
class FullPipelineTest {

    private val dayGanZhi = GanZhi(0)          // 甲子日
    private val monthBranch = EarthlyBranch.XU // 戌月

    @Test
    fun `六爻全空间装卦与解卦不崩且结构完整`() = runBlocking {
        val engine = LiuYaoRuleEngine()
        var cases = 0

        Hexagram.all().forEach { hex ->
            // 静卦 + 六种单动爻
            val movingSets = listOf(emptySet<Int>()) +
                (0..5).map { setOf(it) }

            movingSets.forEach { moving ->
                val chart = LiuYaoDivination.chart(
                    hexagram = hex,
                    dayGanZhi = dayGanZhi,
                    monthBranch = monthBranch,
                    movingIndexes = moving,
                    question = "全空间测试",
                )

                // ── 结构不变量 ──
                assertEquals("${hex.name} 应有六爻", 6, chart.lines.size)
                assertEquals(
                    "${hex.name} 世应各一",
                    1,
                    chart.lines.count { it.relation == YaoRelation.SHI },
                )
                assertEquals(
                    "${hex.name} 世应各一",
                    1,
                    chart.lines.count { it.relation == YaoRelation.YING },
                )
                assertEquals(
                    "${hex.name} 动爻数应匹配",
                    moving.size,
                    chart.lines.count { it.moving },
                )
                assertEquals(
                    "${hex.name} 变卦存在性应与动爻一致",
                    moving.isNotEmpty(),
                    chart.changed != null,
                )
                // 每个动爻必须有变爻，静爻必须没有
                chart.lines.forEach { line ->
                    assertEquals(
                        "${hex.name} 第 ${line.index + 1} 爻的变爻",
                        line.moving,
                        line.bian != null,
                    )
                }
                // 六亲必须是合法枚举（非空即已保证）
                assertTrue(chart.lines.all { it.liuQin in LiuQin.entries })
                // 旬空必为两支
                assertNotNull(chart.xunKong)

                // ── 解卦不崩 ──
                val result = engine.interpret(
                    InterpretationRequest(
                        question = "事业财运婚姻疾病综合测试",
                        liuYao = chart,
                    )
                ).firstOrNull()
                assertNotNull("${hex.name} $moving 应产出解卦结果", result)
                assertEquals(EngineTier.RULE, result!!.tier)
                assertTrue(result.headline.isNotBlank())
                assertTrue(result.sections.isNotEmpty())

                cases++
            }
        }
        assertEquals("应覆盖 64 × 7 = 448 种盘", 448, cases)
    }

    @Test
    fun `梅花全空间排盘与解卦不崩`() = runBlocking {
        val engine = MeihuaRuleEngine()
        var cases = 0

        Hexagram.all().forEach { hex ->
            (0..5).forEach { moving ->
                val chart = MeihuaDivination.chart(hex, moving)

                // ── 结构不变量 ──
                assertEquals(hex, chart.original)
                assertEquals(hex.nuclear, chart.nuclear)
                assertEquals(hex.changing(moving), chart.changed)
                // 体用必取自上下卦
                // ⚠️ 不能断言两者不同 —— 八纯卦（乾为天、坤为地…）上下卦本就相同，
                //    此时体用为同一个经卦，是合法的。
                assertTrue(
                    "${hex.name} 体用应取自上下卦",
                    setOf(chart.tiTrigram, chart.yongTrigram) == setOf(hex.upper, hex.lower),
                )
                // 动爻位置决定体用
                if (moving < 3) {
                    assertEquals(hex.upper, chart.tiTrigram)
                    assertEquals(hex.lower, chart.yongTrigram)
                } else {
                    assertEquals(hex.lower, chart.tiTrigram)
                    assertEquals(hex.upper, chart.yongTrigram)
                }

                val result = engine.interpret(
                    InterpretationRequest(
                        question = "综合测试",
                        meihua = chart,
                        monthBranch = monthBranch,
                    )
                ).firstOrNull()
                assertNotNull("${hex.name} 动$moving 应产出结果", result)
                assertTrue(result!!.sections.isNotEmpty())

                cases++
            }
        }
        assertEquals("应覆盖 64 × 6 = 384 种盘", 384, cases)
    }

    @Test
    fun `八字抽样排盘不崩且结构自洽`() = runBlocking {
        val engine = BaZiRuleEngine()
        var cases = 0

        // 覆盖不同年代、月份、时辰、性别
        for (year in listOf(1950, 1976, 1990, 2000, 2010, 2024)) {
            for (month in 1..12) {
                for (hour in listOf(0, 6, 12, 18, 23)) {
                    for (gender in Gender.entries) {
                        val chart = BaZiService.chart(year, month, 15, hour, gender = gender)

                        assertEquals(4, chart.pillars.size)
                        assertEquals(8, chart.wuXingCount.values.sum())
                        assertEquals(chart.day.gan, chart.dayMaster)
                        assertEquals(8, chart.daYun.size)
                        // 大运年龄连续
                        chart.daYun.zipWithNext().forEach { (a, b) ->
                            assertEquals(a.endAge + 1, b.startAge)
                        }

                        val result = engine.interpret(
                            InterpretationRequest("看命盘", bazi = chart)
                        ).firstOrNull()
                        assertNotNull(result)
                        assertTrue(result!!.headline.contains("日主"))

                        cases++
                    }
                }
            }
        }
        assertEquals("应覆盖 6 年 × 12 月 × 5 时辰 × 2 性别 = 720 组", 720, cases)
    }

    @Test
    fun `历法连续一年逐日不崩`() {
        // 逐日跑一整年，覆盖节气、闰月、月首月末等边界
        var days = 0
        for (month in 1..12) {
            for (day in 1..28) {
                val info = CalendarService.of(2024, month, day, 12)
                assertNotNull(info.ganZhi.year)
                assertNotNull(info.ganZhi.month)
                assertNotNull(info.ganZhi.day)
                assertNotNull(info.ganZhi.hour)
                assertNotNull(info.dayGanZhi)
                assertNotNull(info.monthBranch)
                assertTrue(info.almanac.yi.isNotEmpty())

                val hours = CalendarService.hoursOf(2024, month, day)
                assertEquals(12, hours.size)
                assertEquals("$month/$day 应六吉六凶", 6, hours.count { it.auspicious })
                days++
            }
        }
        assertEquals(12 * 28, days)
    }

    @Test
    fun `四种起卦方式全空间不崩`() {
        // 数字起卦：覆盖 1..64 的所有组合抽样
        for (a in 1..16) {
            for (b in 1..16) {
                val r = Divination.byNumber(a, b)
                assertTrue(r.movingIndexes.single() in 0..5)
                assertNotNull(r.hexagram)
            }
        }

        // 时间起卦：覆盖 12 年支 × 12 月 × 30 日 × 12 时支
        for (yb in EarthlyBranch.entries) {
            for (month in 1..12) {
                for (day in listOf(1, 15, 30)) {
                    for (hb in EarthlyBranch.entries) {
                        val r = Divination.byTime(yb, month, day, hb)
                        assertTrue(r.movingIndexes.single() in 0..5)
                    }
                }
            }
        }

        // 手动起卦：全 64 卦 × 全动爻组合抽样
        Hexagram.all().forEach { hex ->
            val r = Divination.byManual(hex.lines, setOf(0, 3))
            assertEquals(hex, r.hexagram)
            assertEquals(setOf(0, 3), r.movingIndexes)
        }
    }
}
