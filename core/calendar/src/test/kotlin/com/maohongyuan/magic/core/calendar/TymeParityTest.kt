package com.maohongyuan.magic.core.calendar

import com.nlf.calendar.Solar
import com.tyme.solar.SolarDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * lunar ↔ tyme4kt 对拍。
 *
 * 换库的前提是**结果完全一致**。历法错一天，所有卦的旺衰判断全偏，
 * 而且不会报错 —— 所以必须先证明等价，再改实现。
 *
 * 逐日跑两年（730 天），比对：日干支、节气。
 */
class TymeParityTest {

    private val start = LocalDate.of(2026, 1, 1)
    private val end = LocalDate.of(2027, 12, 31)

    private fun days(): Sequence<LocalDate> = generateSequence(start) { d ->
        if (d.isBefore(end)) d.plusDays(1) else null
    }

    @Test
    fun `日干支两年逐日一致`() {
        var n = 0
        var firstDiff: String? = null
        days().forEach { d ->
            val lunar = Solar.fromYmd(d.year, d.monthValue, d.dayOfMonth).lunar
            val tyme = SolarDay.fromYmd(d.year, d.monthValue, d.dayOfMonth)
                .getSixtyCycleDay().getSixtyCycle().toString()
            val expected = lunar.dayInGanZhi
            if (expected != tyme && firstDiff == null) {
                firstDiff = "$d  lunar=$expected  tyme=$tyme"
            }
            n++
        }
        // 防「空跑」：循环没进的话 assertEquals(null, null) 照样通过
        assertTrue("只比了 $n 天，循环可能没执行", n >= 700)
        assertEquals("共比 $n 天，首处差异：$firstDiff", null, firstDiff)
    }

    @Test
    fun `节气日一致`() {
        var checked = 0
        var firstDiff: String? = null
        days().forEach { d ->
            val lunarJq = Solar.fromYmd(d.year, d.monthValue, d.dayOfMonth).lunar.jieQi
            if (lunarJq.isNullOrBlank()) return@forEach
            val tymeJq = SolarDay.fromYmd(d.year, d.monthValue, d.dayOfMonth).getTerm()?.toString()
            checked++
            if (lunarJq != tymeJq && firstDiff == null) {
                firstDiff = "$d  lunar=$lunarJq  tyme=$tymeJq"
            }
        }
        // 两年应有 48 个节气，留出浮动余量
        assertTrue("只比了 $checked 个节气日，异常", checked in 44..52)
        assertEquals("共比 $checked 个节气日，首处差异：$firstDiff", null, firstDiff)
    }

    @Test
    fun `tyme 的 getTerm 在非节气日也给出当前节气`() {
        // 这条是「换库唯一实际收益」的证据：lunar 的 jieQi 在非节气日为 null，
        // tyme 的 getTerm 直接给当前所处节气，可省掉 recentJieQi 的 16 天回溯。
        val d = LocalDate.of(2026, 10, 17)
        val lunarJq = Solar.fromYmd(d.year, d.monthValue, d.dayOfMonth).lunar.jieQi
        val tymeJq = SolarDay.fromYmd(d.year, d.monthValue, d.dayOfMonth).getTerm()?.toString()
        assertEquals("lunar 在非节气日应为空", null, lunarJq?.ifBlank { null })
        assertEquals("寒露", tymeJq)
    }

    @Test
    fun `黄历字段两年逐日一致`() {
        var n = 0
        val diffs = LinkedHashMap<String, String>()

        // lunar 在宜忌/吉神为空时返回占位符「无」，tyme 返回空列表 ——
        // 语义一致，比对前把占位符去掉
        fun norm(list: List<String>) = list.filter { it.isNotBlank() && it != "无" }.joinToString(" ")

        fun check(field: String, d: LocalDate, expected: String, actual: String) {
            if (expected != actual && field !in diffs) diffs[field] = "$d  lunar=$expected  tyme=$actual"
        }

        days().forEach { d ->
            val l = Solar.fromYmdHms(d.year, d.monthValue, d.dayOfMonth, 12, 0, 0).lunar
            val t = SolarDay.fromYmd(d.year, d.monthValue, d.dayOfMonth).getLunarDay()

            check("宜", d, norm(l.dayYi), t.getRecommends().joinToString(" ") { it.getName() })
            check("忌", d, norm(l.dayJi), t.getAvoids().joinToString(" ") { it.getName() })
            check("值星", d, l.zhiXing, t.getDuty().toString())
            check("宿", d, l.xiu, t.getTwentyEightStar().toString())
            check("黄黑道", d, l.dayTianShen, t.getTwelveStar().toString())
            // 吉神 + 凶煞 = tyme 的 Gods（探针已确认顺序：吉神在前、凶煞在后）
            check("吉凶神", d,
                norm(l.dayJiShen + l.dayXiongSha),
                t.getGods().joinToString(" ") { it.getName() })
            n++
        }
        assertTrue("只比了 $n 天", n >= 700)
        assertTrue("有字段不一致：$diffs", diffs.isEmpty())
    }
}
