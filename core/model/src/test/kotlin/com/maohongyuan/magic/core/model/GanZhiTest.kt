package com.maohongyuan.magic.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GanZhiTest {

    @Test
    fun `天干的五行与阴阳`() {
        val wuXing = listOf(
            HeavenlyStem.JIA to WuXing.MU, HeavenlyStem.YI to WuXing.MU,
            HeavenlyStem.BING to WuXing.HUO, HeavenlyStem.DING to WuXing.HUO,
            HeavenlyStem.WU to WuXing.TU, HeavenlyStem.JI to WuXing.TU,
            HeavenlyStem.GENG to WuXing.JIN, HeavenlyStem.XIN to WuXing.JIN,
            HeavenlyStem.REN to WuXing.SHUI, HeavenlyStem.GUI to WuXing.SHUI,
        )
        wuXing.forEach { (stem, expected) -> assertEquals(stem.label, expected, stem.wuXing) }

        val yang = listOf(
            HeavenlyStem.JIA, HeavenlyStem.BING, HeavenlyStem.WU,
            HeavenlyStem.GENG, HeavenlyStem.REN,
        )
        HeavenlyStem.entries.forEach {
            assertEquals(it.label, it in yang, it.yinYang == YinYang.YANG)
        }
    }

    @Test
    fun `地支的五行`() {
        val expected = mapOf(
            EarthlyBranch.ZI to WuXing.SHUI, EarthlyBranch.CHOU to WuXing.TU,
            EarthlyBranch.YIN to WuXing.MU, EarthlyBranch.MAO to WuXing.MU,
            EarthlyBranch.CHEN to WuXing.TU, EarthlyBranch.SI to WuXing.HUO,
            EarthlyBranch.WU to WuXing.HUO, EarthlyBranch.WEI to WuXing.TU,
            EarthlyBranch.SHEN to WuXing.JIN, EarthlyBranch.YOU to WuXing.JIN,
            EarthlyBranch.XU to WuXing.TU, EarthlyBranch.HAI to WuXing.SHUI,
        )
        expected.forEach { (branch, wuXing) -> assertEquals(branch.label, wuXing, branch.wuXing) }
    }

    @Test
    fun `地支的阴阳为偶数位阳`() {
        val yang = listOf(
            EarthlyBranch.ZI, EarthlyBranch.YIN, EarthlyBranch.CHEN,
            EarthlyBranch.WU, EarthlyBranch.SHEN, EarthlyBranch.XU,
        )
        EarthlyBranch.entries.forEach {
            assertEquals(it.label, it in yang, it.yinYang == YinYang.YANG)
        }
    }

    @Test
    fun `六冲与传统六组一致`() {
        val pairs = listOf(
            EarthlyBranch.ZI to EarthlyBranch.WU,
            EarthlyBranch.CHOU to EarthlyBranch.WEI,
            EarthlyBranch.YIN to EarthlyBranch.SHEN,
            EarthlyBranch.MAO to EarthlyBranch.YOU,
            EarthlyBranch.CHEN to EarthlyBranch.XU,
            EarthlyBranch.SI to EarthlyBranch.HAI,
        )
        pairs.forEach { (a, b) ->
            assertEquals("$a 应冲 $b", b, a.clash)
            assertEquals("冲应为对称关系", a, b.clash)
        }
        // 冲不含自身
        EarthlyBranch.entries.forEach { assertTrue("$it 不应自冲", it.clash != it) }
    }

    @Test
    fun `六合与传统六组一致`() {
        val pairs = listOf(
            EarthlyBranch.ZI to EarthlyBranch.CHOU,
            EarthlyBranch.YIN to EarthlyBranch.HAI,
            EarthlyBranch.MAO to EarthlyBranch.XU,
            EarthlyBranch.CHEN to EarthlyBranch.YOU,
            EarthlyBranch.SI to EarthlyBranch.SHEN,
            EarthlyBranch.WU to EarthlyBranch.WEI,
        )
        pairs.forEach { (a, b) ->
            assertEquals("$a 应合 $b", b, a.combine)
            assertEquals("合应为对称关系", a, b.combine)
        }
        EarthlyBranch.entries.forEach { assertTrue("$it 不应自合", it.combine != it) }
    }

    @Test
    fun `三合局与传统四局一致`() {
        val trines = listOf(
            setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN), // 水局
            setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI), // 木局
            setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU),   // 火局
            setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU), // 金局
        )
        trines.forEach { trine ->
            trine.forEach { branch ->
                assertEquals("$branch 的三合局", trine, branch.trinePartners.toSet())
            }
        }
    }

    @Test
    fun `地支藏干与传世本一致`() {
        val expected = mapOf(
            EarthlyBranch.ZI to listOf(HeavenlyStem.GUI),
            EarthlyBranch.CHOU to listOf(HeavenlyStem.JI, HeavenlyStem.GUI, HeavenlyStem.XIN),
            EarthlyBranch.YIN to listOf(HeavenlyStem.JIA, HeavenlyStem.BING, HeavenlyStem.WU),
            EarthlyBranch.MAO to listOf(HeavenlyStem.YI),
            EarthlyBranch.CHEN to listOf(HeavenlyStem.WU, HeavenlyStem.YI, HeavenlyStem.GUI),
            EarthlyBranch.SI to listOf(HeavenlyStem.BING, HeavenlyStem.WU, HeavenlyStem.GENG),
            EarthlyBranch.WU to listOf(HeavenlyStem.DING, HeavenlyStem.JI),
            EarthlyBranch.WEI to listOf(HeavenlyStem.JI, HeavenlyStem.DING, HeavenlyStem.YI),
            EarthlyBranch.SHEN to listOf(HeavenlyStem.GENG, HeavenlyStem.REN, HeavenlyStem.WU),
            EarthlyBranch.YOU to listOf(HeavenlyStem.XIN),
            EarthlyBranch.XU to listOf(HeavenlyStem.WU, HeavenlyStem.XIN, HeavenlyStem.DING),
            EarthlyBranch.HAI to listOf(HeavenlyStem.REN, HeavenlyStem.JIA),
        )
        expected.forEach { (branch, stems) ->
            assertEquals(branch.label, stems, branch.hiddenStems)
        }
        // 四正之支（子卯午酉）只藏本气
        assertEquals(1, EarthlyBranch.ZI.hiddenStems.size)
        assertEquals(1, EarthlyBranch.MAO.hiddenStems.size)
        assertEquals(1, EarthlyBranch.YOU.hiddenStems.size)
    }

    @Test
    fun `地支时辰起点`() {
        assertEquals(23, EarthlyBranch.ZI.hourStart)
        assertEquals(1, EarthlyBranch.CHOU.hourStart)
        assertEquals(3, EarthlyBranch.YIN.hourStart)
        assertEquals(5, EarthlyBranch.MAO.hourStart)
        assertEquals(7, EarthlyBranch.CHEN.hourStart)
        assertEquals(9, EarthlyBranch.SI.hourStart)
        assertEquals(11, EarthlyBranch.WU.hourStart)
        assertEquals(13, EarthlyBranch.WEI.hourStart)
        assertEquals(15, EarthlyBranch.SHEN.hourStart)
        assertEquals(17, EarthlyBranch.YOU.hourStart)
        assertEquals(19, EarthlyBranch.XU.hourStart)
        assertEquals(21, EarthlyBranch.HAI.hourStart)
    }

    @Test
    fun `生肖顺序`() {
        val expected = "鼠牛虎兔龙蛇马羊猴鸡狗猪"
        assertEquals(expected, EarthlyBranch.entries.joinToString("") { it.zodiac })
    }

    // ── 六十甲子 ──────────────────────────────────────────────

    @Test
    fun `六十甲子的首尾与步长`() {
        assertEquals("甲子", GanZhi(0).toString())
        assertEquals("乙丑", GanZhi(1).toString())
        assertEquals("癸亥", GanZhi(59).toString())
        // 每 10 步天干归甲，每 12 步地支归子
        assertEquals(HeavenlyStem.JIA, GanZhi(10).stem)
        assertEquals(HeavenlyStem.JIA, GanZhi(20).stem)
        assertEquals(EarthlyBranch.ZI, GanZhi(12).branch)
    }

    @Test
    fun `六十甲子全部满足阳干配阳支阴干配阴支`() {
        for (i in 0 until GanZhi.CYCLE) {
            val gz = GanZhi(i)
            assertEquals(
                "第 $i 位 $gz 的干支阴阳应一致",
                gz.stem.yinYang,
                gz.branch.yinYang,
            )
        }
    }

    @Test
    fun `非法干支组合应被拒绝`() {
        // 甲为阳干、丑为阴支，阴阳不配
        assertNull(GanZhi.of(HeavenlyStem.JIA, EarthlyBranch.CHOU))
        // 乙为阴干、子为阳支
        assertNull(GanZhi.of(HeavenlyStem.YI, EarthlyBranch.ZI))
        // 合法组合
        assertEquals(0, GanZhi.of(HeavenlyStem.JIA, EarthlyBranch.ZI)?.index)
        assertEquals(59, GanZhi.of(HeavenlyStem.GUI, EarthlyBranch.HAI)?.index)
    }

    @Test
    fun `六十甲子恰好遍历所有合法干支组合`() {
        val legal = HeavenlyStem.entries.flatMap { s ->
            EarthlyBranch.entries.mapNotNull { b -> GanZhi.of(s, b)?.index }
        }
        assertEquals(GanZhi.CYCLE, legal.size)
        assertEquals((0 until GanZhi.CYCLE).toSet(), legal.toSet())
    }

    // ── 六神 ──────────────────────────────────────────────────

    @Test
    fun `六神起法符合日干规定`() {
        // 甲乙日：初爻青龙
        assertEquals(LiuShen.QING_LONG, LiuShen.at(HeavenlyStem.JIA, 0))
        assertEquals(LiuShen.QING_LONG, LiuShen.at(HeavenlyStem.YI, 0))
        // 丙丁日：初爻朱雀
        assertEquals(LiuShen.ZHU_QUE, LiuShen.at(HeavenlyStem.BING, 0))
        assertEquals(LiuShen.ZHU_QUE, LiuShen.at(HeavenlyStem.DING, 0))
        // 戊日：初爻勾陈（单独一个，不与丁共享）
        assertEquals(LiuShen.GOU_CHEN, LiuShen.at(HeavenlyStem.WU, 0))
        // 己日：初爻腾蛇（单独一个）
        assertEquals(LiuShen.TENG_SHE, LiuShen.at(HeavenlyStem.JI, 0))
        // 庚辛日：初爻白虎
        assertEquals(LiuShen.BAI_HU, LiuShen.at(HeavenlyStem.GENG, 0))
        assertEquals(LiuShen.BAI_HU, LiuShen.at(HeavenlyStem.XIN, 0))
        // 壬癸日：初爻玄武
        assertEquals(LiuShen.XUAN_WU, LiuShen.at(HeavenlyStem.REN, 0))
        assertEquals(LiuShen.XUAN_WU, LiuShen.at(HeavenlyStem.GUI, 0))
    }

    @Test
    fun `六神自初爻至上爻依次循环`() {
        val seq = LiuShen.sequence(HeavenlyStem.JIA)
        assertEquals(6, seq.size)
        assertEquals(LiuShen.QING_LONG, seq[0])
        assertEquals(LiuShen.ZHU_QUE, seq[1])
        assertEquals(LiuShen.GOU_CHEN, seq[2])
        assertEquals(LiuShen.TENG_SHE, seq[3])
        assertEquals(LiuShen.BAI_HU, seq[4])
        assertEquals(LiuShen.XUAN_WU, seq[5])
        // 任意日干下六神都是六个不同的值
        HeavenlyStem.entries.forEach { stem ->
            assertEquals("$stem 日", 6, LiuShen.sequence(stem).toSet().size)
        }
    }

    @Test
    fun `戊日与己日的六神起点相差一位`() {
        // 这是最容易写错的地方：戊、己各占一个起点，不是两两一组
        val wu = LiuShen.sequence(HeavenlyStem.WU)
        val ji = LiuShen.sequence(HeavenlyStem.JI)
        assertEquals(LiuShen.GOU_CHEN, wu[0])
        assertEquals(LiuShen.TENG_SHE, ji[0])
        assertEquals("己日初爻应是戊日初爻的下一个", wu[1], ji[0])
    }
}
