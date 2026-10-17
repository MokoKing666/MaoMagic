package com.maohongyuan.magic.core.calendar

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.HeavenlyStem
import com.nlf.calendar.Solar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 自研的十神/藏干/地势 与 lunar 对拍。
 *
 * 八字里这三样错一格，整张盘的性格判断就偏了，而且不会报错。
 * 所以逐柱逐日验证。
 */
class BaZiSupplementParityTest {

    private fun days(): Sequence<LocalDate> = generateSequence(LocalDate.of(2026, 1, 1)) { d ->
        if (d.isBefore(LocalDate.of(2027, 12, 31))) d.plusDays(1) else null
    }

    @Test
    fun `四柱十神 藏干 地势 两年逐日一致`() {
        var n = 0
        val diffs = LinkedHashMap<String, String>()

        fun check(field: String, d: LocalDate, expected: String, actual: String) {
            if (expected != actual && field !in diffs) diffs[field] = "$d  lunar=$expected  自研=$actual"
        }

        days().forEach { d ->
            val ec = Solar.fromYmdHms(d.year, d.monthValue, d.dayOfMonth, 12, 0, 0).lunar.eightChar
            val dayStem = HeavenlyStem.ofLabel(ec.dayGan)!!

            // 十神：年/月/日/时 四柱的天干
            listOf(
                Triple("年干十神", ec.yearGan, ec.yearShiShenGan),
                Triple("月干十神", ec.monthGan, ec.monthShiShenGan),
                // 日干自身的十神不参与 lunar 比对：lunar 标「日主」，我们算出来是「比肩」。
                // 「日主」是显示层惯例（日主是坐标原点），不该塞进 tenGod 的语义里。
                Triple("日干十神", ec.dayGan, "比肩"),
                Triple("时干十神", ec.timeGan, ec.timeShiShenGan),
            ).forEach { (name, gan, expected) ->
                val stem = HeavenlyStem.ofLabel(gan) ?: return@forEach
                check(name, d, expected, BaZiSupplement.tenGod(dayStem, stem))
            }

            // 藏干：年/月/日/时 四柱的地支
            listOf(
                "年藏干" to (ec.yearHideGan to ec.yearZhi),
                "月藏干" to (ec.monthHideGan to ec.monthZhi),
                "日藏干" to (ec.dayHideGan to ec.dayZhi),
                "时藏干" to (ec.timeHideGan to ec.timeZhi),
            ).forEach { (name, pair) ->
                val (expected, zhi) = pair
                val branch = EarthlyBranch.ofLabel(zhi) ?: return@forEach
                check(name, d, expected.joinToString(","), BaZiSupplement.hideStems(branch).joinToString(",") { it.label })
            }

            // 地势：四柱地支
            listOf(
                "年地势" to (ec.yearDiShi to ec.yearZhi),
                "月地势" to (ec.monthDiShi to ec.monthZhi),
                "日地势" to (ec.dayDiShi to ec.dayZhi),
                "时地势" to (ec.timeDiShi to ec.timeZhi),
            ).forEach { (name, pair) ->
                val (expected, zhi) = pair
                val branch = EarthlyBranch.ofLabel(zhi) ?: return@forEach
                check(name, d, expected, BaZiSupplement.terrain(dayStem, branch))
            }
            n++
        }
        // 日主自比必为比肩 —— 单独断言，不依赖 lunar 的显示惯例
        assertEquals("比肩", BaZiSupplement.tenGod(HeavenlyStem.JIA, HeavenlyStem.JIA))
        assertEquals("劫财", BaZiSupplement.tenGod(HeavenlyStem.JIA, HeavenlyStem.YI))
        assertTrue("只比了 $n 天", n >= 700)
        assertTrue("有字段不一致：$diffs", diffs.isEmpty())
    }
}
