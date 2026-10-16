package com.maohongyuan.magic.core.hexagram

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.GanZhi
import com.maohongyuan.magic.core.model.Hexagram
import com.maohongyuan.magic.core.model.LiuQin
import com.maohongyuan.magic.core.model.LiuShen
import com.maohongyuan.magic.core.model.Trigram
import com.maohongyuan.magic.core.model.WuXing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 六爻装卦。
 *
 * 基准案例：**天风姤，甲子日，戌月**（旬空戌亥）。
 *
 * 传统排盘结果（逐项手算核对）：
 * ```
 * 玄武  父母 壬戌土  ┃ 空       上九
 * 白虎  兄弟 壬申金  ┃          九五
 * 腾蛇  官鬼 壬午火  ┃ 应       九四
 * 勾陈  兄弟 辛酉金  ┃          九三
 * 朱雀  子孙 辛亥水  ┃ 空  妻财甲寅木(伏)   九二
 * 青龙  父母 辛丑土  ┃ 世       初六
 * ```
 */
class LiuYaoChartTest {

    /** 甲子日：旬空戌亥，六神自青龙起于初爻 */
    private val jiaZi = GanZhi(0)

    private fun chartOf(name: String, moving: Set<Int> = emptySet()): LiuYaoChart =
        LiuYaoDivination.chart(
            hexagram = Hexagram.byName(name)!!,
            dayGanZhi = jiaZi,
            monthBranch = EarthlyBranch.XU,
            movingIndexes = moving,
        )

    @Test
    fun `天风姤的宫位与世应`() {
        val chart = chartOf("天风姤")
        assertEquals(Trigram.QIAN, chart.palace.palace)
        assertEquals(Generation.YI_SHI, chart.palace.generation)
        assertEquals("乾宫 · 一世 · 世初应四", chart.palace.summary)

        // 世在初爻（index 0），应在四爻（index 3）
        assertEquals(0, chart.shiLine.index)
        assertEquals(3, chart.yingLine.index)
        assertEquals(YaoRelation.SHI, chart.lines[0].relation)
        assertEquals(YaoRelation.YING, chart.lines[3].relation)
        // 其余四爻无标记
        assertEquals(4, chart.lines.count { it.relation == YaoRelation.NONE })
    }

    @Test
    fun `天风姤的六亲与纳甲`() {
        val chart = chartOf("天风姤")
        // 天风姤爻题：初六、九二、九三、九四、九五、上九 ⇒ 阴阳为 阴 阳 阳 阳 阳 阳
        val expected = listOf(
            Triple("辛丑", LiuQin.FU_MU, false),
            Triple("辛亥", LiuQin.ZI_SUN, true),
            Triple("辛酉", LiuQin.XIONG_DI, true),
            Triple("壬午", LiuQin.GUAN_GUI, true),
            Triple("壬申", LiuQin.XIONG_DI, true),
            Triple("壬戌", LiuQin.FU_MU, true),
        )
        chart.lines.forEachIndexed { i, line ->
            val (naJia, qin, yang) = expected[i]
            assertEquals("第 ${i + 1} 爻纳甲", naJia, line.naJia.label)
            assertEquals("第 ${i + 1} 爻六亲", qin, line.liuQin)
            assertEquals("第 ${i + 1} 爻阴阳", yang, line.yang)
        }
    }

    @Test
    fun `天风姤的六神自初爻青龙起`() {
        val chart = chartOf("天风姤")
        assertEquals(
            listOf(
                LiuShen.QING_LONG, LiuShen.ZHU_QUE, LiuShen.GOU_CHEN,
                LiuShen.TENG_SHE, LiuShen.BAI_HU, LiuShen.XUAN_WU,
            ),
            chart.lines.map { it.liuShen },
        )
    }

    @Test
    fun `甲子日旬空戌亥`() {
        val chart = chartOf("天风姤")
        assertEquals("戌亥", chart.xunKong.label)
        // 二爻辛亥、上爻壬戌落空
        assertTrue("二爻辛亥应落空", chart.lines[1].kong)
        assertTrue("上爻壬戌应落空", chart.lines[5].kong)
        assertEquals(listOf(1, 5), chart.lines.filter { it.kong }.map { it.index })
    }

    @Test
    fun `天风姤缺妻财故有伏神`() {
        val chart = chartOf("天风姤")
        // 本卦六亲：父母 子孙 兄弟 官鬼 兄弟 父母 —— 缺妻财
        assertEquals(listOf(LiuQin.QI_CAI), chart.missingLiuQin)

        val fu = chart.fuShenList
        assertEquals(1, fu.size)
        assertEquals(LiuQin.QI_CAI, fu[0].liuQin)
        assertEquals("甲寅", fu[0].naJia.label)
        // 乾为天的二爻为甲寅木，故伏于二爻之下
        assertEquals(1, fu[0].underIndex)
        assertEquals("妻财甲寅", fu[0].label)
        assertEquals(fu[0], chart.lines[1].fuShen)
        // 其余爻无伏神
        assertNull(chart.lines[0].fuShen)
        assertNull(chart.lines[5].fuShen)
    }

    @Test
    fun `乾为天六亲齐全故无伏神`() {
        val chart = chartOf("乾为天")
        assertTrue(chart.missingLiuQin.isEmpty())
        assertTrue(chart.fuShenList.isEmpty())
        // 乾为天六亲：子孙 妻财 父母 官鬼 兄弟 父母
        assertEquals(
            listOf(
                LiuQin.ZI_SUN, LiuQin.QI_CAI, LiuQin.FU_MU,
                LiuQin.GUAN_GUI, LiuQin.XIONG_DI, LiuQin.FU_MU,
            ),
            chart.lines.map { it.liuQin },
        )
    }

    // ── 变卦 ──────────────────────────────────────────────────

    @Test
    fun `静卦无变卦`() {
        val chart = chartOf("天风姤")
        assertNull(chart.changed)
        assertTrue(chart.movingIndexes.isEmpty())
        assertTrue(chart.lines.all { it.bian == null })
    }

    @Test
    fun `天风姤上九动变泽风大过`() {
        val chart = chartOf("天风姤", moving = setOf(5))
        assertEquals("泽风大过", chart.changed?.name)
        assertEquals(listOf(5), chart.movingIndexes)

        val top = chart.lines[5]
        assertTrue(top.moving)
        // 变爻：大过上爻 = 上兑外盘第三爻 = 丁未土
        assertEquals("丁未", top.bian?.naJia?.label)
        // 大过属震宫（木），木克土 ⇒ 妻财
        assertEquals(Trigram.ZHEN, top.bian?.palace)
        assertEquals(LiuQin.QI_CAI, top.bian?.liuQin)

        // 非动爻没有变爻
        assertTrue(chart.lines.dropLast(1).all { it.bian == null })
    }

    @Test
    fun `变爻六亲按变卦自身宫位重算`() {
        // 天风姤九四动 ⇒ 变卦巽为风（巽宫木）
        // 巽为风四爻 = 辛未土；木克土 ⇒ 妻财
        // 若错误地沿用本卦宫位（乾宫金），未土生金 ⇒ 父母 —— 两者不同，正是这条测试要抓的
        val chart = chartOf("天风姤", moving = setOf(3))
        assertEquals("巽为风", chart.changed?.name)

        val si = chart.lines[3]
        assertEquals("辛未", si.bian?.naJia?.label)
        assertEquals(Trigram.XUN, si.bian?.palace)
        assertEquals("变爻六亲应按变卦宫位重算", LiuQin.QI_CAI, si.bian?.liuQin)
        // 本卦宫位推出的结果确实不同，证明重算不是多余
        assertEquals(LiuQin.FU_MU, LiuQin.of(WuXing.JIN, WuXing.TU))
    }

    @Test
    fun `多爻动`() {
        val chart = chartOf("天风姤", moving = setOf(0, 3))
        assertEquals(listOf(0, 3), chart.movingIndexes)
        // 初爻动 + 四爻动 ⇒ 下巽变乾、上乾变巽 ⇒ 上巽下乾 = 风天小畜
        assertEquals("风天小畜", chart.changed?.name)
        assertNotNull(chart.lines[0].bian)
        assertNotNull(chart.lines[3].bian)
    }

    // ── 旺衰 ──────────────────────────────────────────────────

    @Test
    fun `月建旺衰`() {
        // 戌月（土令）：土旺、金相、火休、木囚、水死
        assertEquals(WangShuai.WANG, WangShuai.of(WuXing.TU, EarthlyBranch.XU))
        assertEquals(WangShuai.XIANG, WangShuai.of(WuXing.JIN, EarthlyBranch.XU))
        assertEquals(WangShuai.XIU, WangShuai.of(WuXing.HUO, EarthlyBranch.XU))
        assertEquals(WangShuai.QIU, WangShuai.of(WuXing.MU, EarthlyBranch.XU))
        assertEquals(WangShuai.SI, WangShuai.of(WuXing.SHUI, EarthlyBranch.XU))
    }

    @Test
    fun `春季旺衰符合传统`() {
        // 春为木令：木旺 火相 水休 金囚 土死
        val chun = EarthlyBranch.YIN
        assertEquals(WangShuai.WANG, WangShuai.of(WuXing.MU, chun))
        assertEquals(WangShuai.XIANG, WangShuai.of(WuXing.HUO, chun))
        assertEquals(WangShuai.XIU, WangShuai.of(WuXing.SHUI, chun))
        assertEquals(WangShuai.QIU, WangShuai.of(WuXing.JIN, chun))
        assertEquals(WangShuai.SI, WangShuai.of(WuXing.TU, chun))
    }

    @Test
    fun `任何月令下五种旺衰各出现一次`() {
        EarthlyBranch.entries.forEach { month ->
            val states = WuXing.entries.map { WangShuai.of(it, month) }
            assertEquals("$month 月", 5, states.toSet().size)
        }
    }

    // ── 爻题 ──────────────────────────────────────────────────

    @Test
    fun `天风姤的六爻爻题`() {
        val chart = chartOf("天风姤")
        assertEquals(
            listOf("初六", "九二", "九三", "九四", "九五", "上九"),
            chart.lines.map { it.title },
        )
    }
}

class DivinationTest {

    // ── 铜钱 ──────────────────────────────────────────────────

    @Test
    fun `三枚铜钱的四种结果与阴阳动静`() {
        val laoYin = CoinThrow(listOf(2, 2, 2))    // 6
        assertEquals(6, laoYin.sum)
        assertEquals("老阴", laoYin.label)
        assertTrue("老阴为阴", !laoYin.yang)
        assertTrue("老阴为动", laoYin.moving)

        val shaoYang = CoinThrow(listOf(2, 2, 3))  // 7
        assertEquals(7, shaoYang.sum)
        assertEquals("少阳", shaoYang.label)
        assertTrue("少阳为阳", shaoYang.yang)
        assertTrue("少阳为静", !shaoYang.moving)

        val shaoYin = CoinThrow(listOf(2, 3, 3))   // 8
        assertEquals(8, shaoYin.sum)
        assertEquals("少阴", shaoYin.label)
        assertTrue("少阴为阴", !shaoYin.yang)
        assertTrue("少阴为静", !shaoYin.moving)

        val laoYang = CoinThrow(listOf(3, 3, 3))   // 9
        assertEquals(9, laoYang.sum)
        assertEquals("老阳", laoYang.label)
        assertTrue("老阳为阳", laoYang.yang)
        assertTrue("老阳为动", laoYang.moving)
    }

    @Test
    fun `六爻皆为老阳则得乾为天且六爻全动`() {
        val result = Divination.byCoin(List(6) { CoinThrow(listOf(3, 3, 3)) })
        assertEquals("乾为天", result.hexagram.name)
        assertEquals((0..5).toSet(), result.movingIndexes)
        assertEquals("坤为地", result.changed?.name)
    }

    @Test
    fun `六爻皆为少阴则得坤为地且无动爻`() {
        val result = Divination.byCoin(List(6) { CoinThrow(listOf(2, 3, 3)) })
        assertEquals("坤为地", result.hexagram.name)
        assertTrue(result.movingIndexes.isEmpty())
        assertNull(result.changed)
    }

    @Test
    fun `铜钱法概率分布符合三枚各半的必然结果`() {
        // 老阴 1/8、少阳 3/8、少阴 3/8、老阳 1/8
        val n = 80_000
        val c = mutableMapOf(6 to 0, 7 to 0, 8 to 0, 9 to 0)
        val random = kotlin.random.Random(20261016)
        repeat(n) {
            val sum = CoinThrow.random(random).sum
            c[sum] = c.getValue(sum) + 1
        }
        val p6 = c.getValue(6).toDouble() / n
        val p7 = c.getValue(7).toDouble() / n
        val p8 = c.getValue(8).toDouble() / n
        val p9 = c.getValue(9).toDouble() / n

        assertEquals(0.125, p6, 0.01)
        assertEquals(0.375, p7, 0.01)
        assertEquals(0.375, p8, 0.01)
        assertEquals(0.125, p9, 0.01)
        // 动静各半
        assertEquals(0.25, p6 + p9, 0.01)
    }

    // ── 时间 ──────────────────────────────────────────────────

    @Test
    fun `时间起卦的公式`() {
        // 年支 子(1) + 农历月 1 + 日 1 = 3 ⇒ 上卦 = 3 mod 8 = 3 = 离
        // 再加 时支 子(1) ⇒ 4 ⇒ 下卦 = 4 = 震
        // 动爻 = 4 mod 6 = 4 ⇒ index 3（四爻）
        val result = Divination.byTime(
            yearBranch = EarthlyBranch.ZI,
            lunarMonth = 1,
            lunarDay = 1,
            hourBranch = EarthlyBranch.ZI,
        )
        assertEquals("火雷噬嗑", result.hexagram.name)
        assertEquals(setOf(3), result.movingIndexes)
        assertEquals(DivinationMethod.TIME, result.method)
    }

    @Test
    fun `时间起卦余零取坤与上爻`() {
        // 构造 total 为 8 的倍数：年支 子(1) + 月 7 + 日 8 = 16 ⇒ 上卦 16 mod 8 = 0 ⇒ 坤
        // 加 时支 子(1) ⇒ 17 ⇒ 下卦 17 mod 8 = 1 ⇒ 乾
        // 动爻 = 17 mod 6 = 5 ⇒ index 4（五爻）
        val result = Divination.byTime(
            yearBranch = EarthlyBranch.ZI,
            lunarMonth = 7,
            lunarDay = 8,
            hourBranch = EarthlyBranch.ZI,
        )
        assertEquals(Trigram.KUN, result.hexagram.upper)
        assertEquals(Trigram.QIAN, result.hexagram.lower)
        assertEquals(setOf(4), result.movingIndexes)
    }

    @Test
    fun `时间起卦动爻落在六爻之内`() {
        for (m in 1..12) {
            for (d in 1..30) {
                val r = Divination.byTime(EarthlyBranch.ZI, m, d, EarthlyBranch.ZI)
                assertTrue("$m 月 $d 日", r.movingIndexes.single() in 0..5)
            }
        }
    }

    // ── 数字 ──────────────────────────────────────────────────

    @Test
    fun `数字起卦的公式`() {
        // 1 ⇒ 乾，2 ⇒ 兑 ⇒ 天泽履；(1+2) mod 6 = 3 ⇒ index 2
        val result = Divination.byNumber(1, 2)
        assertEquals("天泽履", result.hexagram.name)
        assertEquals(setOf(2), result.movingIndexes)
        assertEquals(DivinationMethod.NUMBER, result.method)
    }

    @Test
    fun `数字为八的倍数时取坤`() {
        val result = Divination.byNumber(8, 16)
        assertEquals(Trigram.KUN, result.hexagram.upper)
        assertEquals(Trigram.KUN, result.hexagram.lower)
        assertEquals("坤为地", result.hexagram.name)
    }

    @Test
    fun `数字为六的倍数时动爻取上爻`() {
        // 1 + 5 = 6 ⇒ 6 mod 6 = 0 ⇒ 上爻（index 5）
        val result = Divination.byNumber(1, 5)
        assertEquals(setOf(5), result.movingIndexes)
    }

    // ── 手动 ──────────────────────────────────────────────────

    @Test
    fun `手动起卦`() {
        val result = Divination.byManual(
            bottomToTop = listOf(true, false, true, true, true, false),
            movingIndexes = setOf(3),
        )
        assertEquals("泽火革", result.hexagram.name)
        assertEquals("水火既济", result.changed?.name)
        assertEquals(DivinationMethod.MANUAL, result.method)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `手动起卦爻数不对应报错`() {
        Divination.byManual(listOf(true, false), setOf(0))
    }

    // ── 先天八卦数 ────────────────────────────────────────────

    @Test
    fun `先天八卦数顺序`() {
        assertEquals(1, Divination.xianTianNumber(Trigram.QIAN))
        assertEquals(2, Divination.xianTianNumber(Trigram.DUI))
        assertEquals(3, Divination.xianTianNumber(Trigram.LI))
        assertEquals(4, Divination.xianTianNumber(Trigram.ZHEN))
        assertEquals(5, Divination.xianTianNumber(Trigram.XUN))
        assertEquals(6, Divination.xianTianNumber(Trigram.KAN))
        assertEquals(7, Divination.xianTianNumber(Trigram.GEN))
        assertEquals(8, Divination.xianTianNumber(Trigram.KUN))
    }
}
