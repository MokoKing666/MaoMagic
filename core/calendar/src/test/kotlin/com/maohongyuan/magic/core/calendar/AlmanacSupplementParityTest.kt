package com.maohongyuan.magic.core.calendar

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.HeavenlyStem
import com.nlf.calendar.Solar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 自研的冲/煞/彭祖/五神 与 lunar 对拍。
 *
 * 这四项 tyme 没有，迁移后要自己算。拿 lunar 当 oracle 逐日验证 ——
 * 全绿才能删 lunar，否则就是「自己写了个看起来对的东西」。
 */
class AlmanacSupplementParityTest {

    private fun days(): Sequence<LocalDate> = generateSequence(LocalDate.of(2026, 1, 1)) { d ->
        if (d.isBefore(LocalDate.of(2027, 12, 31))) d.plusDays(1) else null
    }

    /** 由干、支求六十甲子序号 */
    private fun sixtyIndex(stem: Int, branch: Int): Int {
        for (i in 0 until 60) if (i % 10 == stem && i % 12 == branch) return i
        error("干支组合不存在：$stem/$branch")
    }

    @Test
    fun `冲 煞 彭祖 五神 两年逐日一致`() {
        var n = 0
        val diffs = LinkedHashMap<String, String>()

        fun check(field: String, d: LocalDate, expected: String, actual: String) {
            if (expected != actual && field !in diffs) diffs[field] = "$d  lunar=$expected  自研=$actual"
        }

        days().forEach { d ->
            val l = Solar.fromYmdHms(d.year, d.monthValue, d.dayOfMonth, 12, 0, 0).lunar
            val stem = HeavenlyStem.ofLabel(l.dayGan) ?: error("干解析失败 ${l.dayGan}")
            val branch = EarthlyBranch.ofLabel(l.dayZhi) ?: error("支解析失败 ${l.dayZhi}")
            val idx = sixtyIndex(stem.ordinal, branch.ordinal)

            val clash = AlmanacSupplement.clash(idx)
            check("冲描述", d, l.dayChongDesc, clash.description)
            check("冲生肖", d, l.dayChongShengXiao, clash.zodiac)
            check("煞", d, l.daySha, AlmanacSupplement.sha(branch))

            val (pzGan, pzZhi) = AlmanacSupplement.pengZu(stem, branch)
            check("彭祖干", d, l.pengZuGan, pzGan)
            check("彭祖支", d, l.pengZuZhi, pzZhi)

            val pos = AlmanacSupplement.positions(stem)
            val lunarPos = listOf(
                l.dayPositionXi to l.dayPositionXiDesc,
                l.dayPositionFu to l.dayPositionFuDesc,
                l.dayPositionCai to l.dayPositionCaiDesc,
                l.dayPositionYangGui to l.dayPositionYangGuiDesc,
                l.dayPositionYinGui to l.dayPositionYinGuiDesc,
            )
            pos.forEachIndexed { i, p ->
                check(p.name, d, "${lunarPos[i].first}/${lunarPos[i].second}", "${p.direction}/${p.description}")
            }
            check("宿吉凶", d, l.xiuLuck, AlmanacSupplement.xiuLuck(l.xiu))
            check("农历月名", d, l.monthInChinese, AlmanacSupplement.monthName(kotlin.math.abs(l.month)))
            check("农历日名", d, l.dayInChinese, AlmanacSupplement.dayName(l.day))
            n++
        }
        assertTrue("只比了 $n 天", n >= 700)
        assertTrue("有字段不一致：$diffs", diffs.isEmpty())
    }
}
