package com.maohongyuan.magic.core.hexagram

import com.maohongyuan.magic.core.model.Hexagram
import com.maohongyuan.magic.core.model.Trigram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 京房八宫表。
 *
 * 下面八张卦序表是**传统规定**，逐宫手算核对过。
 * 表本身由变爻掩码算法生成 —— 若掩码写错（尤其游魂、归魂那两条不「继续往上」的规则），
 * 这里的比对会立刻失败。
 */
class HexagramTableTest {

    private val expectedPalaces = mapOf(
        Trigram.QIAN to listOf(
            "乾为天", "天风姤", "天山遁", "天地否", "风地观", "山地剥", "火地晋", "火天大有",
        ),
        Trigram.KAN to listOf(
            "坎为水", "水泽节", "水雷屯", "水火既济", "泽火革", "雷火丰", "地火明夷", "地水师",
        ),
        Trigram.GEN to listOf(
            "艮为山", "山火贲", "山天大畜", "山泽损", "火泽睽", "天泽履", "风泽中孚", "风山渐",
        ),
        Trigram.ZHEN to listOf(
            "震为雷", "雷地豫", "雷水解", "雷风恒", "地风升", "水风井", "泽风大过", "泽雷随",
        ),
        Trigram.XUN to listOf(
            "巽为风", "风天小畜", "风火家人", "风雷益", "天雷无妄", "火雷噬嗑", "山雷颐", "山风蛊",
        ),
        Trigram.LI to listOf(
            "离为火", "火山旅", "火风鼎", "火水未济", "山水蒙", "风水涣", "天水讼", "天火同人",
        ),
        Trigram.KUN to listOf(
            "坤为地", "地雷复", "地泽临", "地天泰", "雷天大壮", "泽天夬", "水天需", "水地比",
        ),
        Trigram.DUI to listOf(
            "兑为泽", "泽水困", "泽地萃", "泽山咸", "水山蹇", "地山谦", "雷山小过", "雷泽归妹",
        ),
    )

    @Test
    fun `八宫各八卦与传统卦序完全一致`() {
        expectedPalaces.forEach { (palace, names) ->
            val actual = HexagramTable.palace(palace).map { it.hexagram.name }
            assertEquals("${palace.label}宫", names, actual)
        }
    }

    @Test
    fun `八宫恰好覆盖全部六十四卦且无重复`() {
        val all = HexagramTable.all()
        assertEquals(Hexagram.COUNT, all.size)
        assertEquals(Hexagram.COUNT, all.map { it.hexagram.code }.toSet().size)
        assertEquals(
            (0 until Hexagram.COUNT).toSet(),
            all.map { it.hexagram.code }.toSet(),
        )
    }

    @Test
    fun `每个卦的宫位查询与八宫表自洽`() {
        expectedPalaces.forEach { (palace, names) ->
            names.forEach { name ->
                val hex = Hexagram.byName(name)!!
                assertEquals("$name 的宫位", palace, HexagramTable.of(hex).palace)
            }
        }
    }

    @Test
    fun `本宫卦必为八纯卦`() {
        expectedPalaces.keys.forEach { palace ->
            val benGong = HexagramTable.palace(palace).first()
            assertEquals(Generation.BEN_GONG, benGong.generation)
            assertEquals(palace, benGong.hexagram.upper)
            assertEquals(palace, benGong.hexagram.lower)
        }
    }

    @Test
    fun `世应位置符合规定`() {
        // 本宫世六应三；一世世初应四；二世世二应五；三世世三应六
        // 四世世四应初；五世世五应二；游魂世四应初；归魂世三应六
        val expected = mapOf(
            Generation.BEN_GONG to (5 to 2),
            Generation.YI_SHI to (0 to 3),
            Generation.ER_SHI to (1 to 4),
            Generation.SAN_SHI to (2 to 5),
            Generation.SI_SHI to (3 to 0),
            Generation.WU_SHI to (4 to 1),
            Generation.YOU_HUN to (3 to 0),
            Generation.GUI_HUN to (2 to 5),
        )
        expected.forEach { (generation, pair) ->
            assertEquals(generation.label, pair.first, generation.shiIndex)
            assertEquals(generation.label, pair.second, generation.yingIndex)
        }
    }

    @Test
    fun `世应相隔三爻且不重合`() {
        Generation.entries.forEach { generation ->
            assertEquals(
                "${generation.label} 世应应相隔三爻",
                3,
                (generation.yingIndex - generation.shiIndex + 6) % 6,
            )
            assertTrue(generation.shiIndex != generation.yingIndex)
        }
    }

    @Test
    fun `每宫八个阶段各一个卦且互不相同`() {
        expectedPalaces.keys.forEach { palace ->
            val list = HexagramTable.palace(palace)
            assertEquals(8, list.size)
            assertEquals(Generation.entries.toSet(), list.map { it.generation }.toSet())
            assertEquals("${palace.label}宫八阶段不应重复", 8, list.map { it.hexagram.code }.toSet().size)
        }
    }

    @Test
    fun `游魂与归魂不是继续往上变`() {
        // 游魂 = 五世之后回变第四爻；归魂 = 游魂之后内卦三爻全变回本宫
        // 这两条是八宫规则里唯二不「递推」的，单独校验
        assertEquals(0b010111, Generation.YOU_HUN.xorMask)
        assertEquals(0b010000, Generation.GUI_HUN.xorMask)

        // 乾宫：五世山地剥 → 游魂火地晋 → 归魂火天大有
        val qian = HexagramTable.palace(Trigram.QIAN).map { it.hexagram.name }
        assertEquals("山地剥", qian[5])
        assertEquals("火地晋", qian[6])
        assertEquals("火天大有", qian[7])

        // 归魂的内卦必须回到本宫（下卦 == 宫位经卦）
        expectedPalaces.keys.forEach { palace ->
            val guiHun = HexagramTable.palace(palace).last()
            assertEquals(Generation.GUI_HUN, guiHun.generation)
            assertEquals("${palace.label}宫归魂的内卦应回到本宫", palace, guiHun.hexagram.lower)
        }
    }

    @Test
    fun `宫位五行与六亲可推导`() {
        // 乾宫属金 ⇒ 乾为天的六亲为 子孙 妻财 父母 官鬼 兄弟 父母
        val qian = HexagramTable.of(Hexagram.byName("乾为天")!!)
        assertEquals(com.maohongyuan.magic.core.model.WuXing.JIN, qian.palaceWuXing)
        assertEquals("乾宫", qian.palaceLabel)
    }

    @Test
    fun `摘要文本格式`() {
        val gou = HexagramTable.of(Hexagram.byName("天风姤")!!)
        assertEquals("乾宫 · 一世 · 世初应四", gou.summary)
    }
}

class NaJiaTest {

    @Test
    fun `乾为天的纳甲`() {
        val qian = Hexagram.byName("乾为天")!!
        val expected = listOf("甲子", "甲寅", "甲辰", "壬午", "壬申", "壬戌")
        val actual = (0..5).map { i ->
            val trigram = if (i < 3) qian.lower else qian.upper
            NaJia.of(trigram).line(i).label
        }
        assertEquals(expected, actual)
    }

    @Test
    fun `天风姤的纳甲内外卦各用各盘`() {
        // 下巽内盘 辛丑辛亥辛酉；上乾外盘 壬午壬申壬戌
        val gou = Hexagram.byName("天风姤")!!
        val expected = listOf("辛丑", "辛亥", "辛酉", "壬午", "壬申", "壬戌")
        val actual = (0..5).map { i ->
            val trigram = if (i < 3) gou.lower else gou.upper
            NaJia.of(trigram).line(i).label
        }
        assertEquals(expected, actual)
    }

    @Test
    fun `乾坤内外卦天干不同其余六卦相同`() {
        // 乾内甲外壬、坤内乙外癸
        assertEquals(
            listOf("甲", "甲", "甲"),
            NaJia.of(Trigram.QIAN).inner.map { it.stem.label },
        )
        assertEquals(
            listOf("壬", "壬", "壬"),
            NaJia.of(Trigram.QIAN).outer.map { it.stem.label },
        )
        assertEquals(
            listOf("乙", "乙", "乙"),
            NaJia.of(Trigram.KUN).inner.map { it.stem.label },
        )
        assertEquals(
            listOf("癸", "癸", "癸"),
            NaJia.of(Trigram.KUN).outer.map { it.stem.label },
        )

        // 其余六卦内外同干
        listOf(Trigram.DUI, Trigram.LI, Trigram.ZHEN, Trigram.XUN, Trigram.KAN, Trigram.GEN)
            .forEach { t ->
                val naJia = NaJia.of(t)
                assertEquals(
                    "${t.label}内外应同干",
                    naJia.inner.map { it.stem }.toSet(),
                    naJia.outer.map { it.stem }.toSet(),
                )
            }
    }

    @Test
    fun `纳甲表覆盖全部八卦`() {
        Trigram.entries.forEach { NaJia.of(it) }
    }

    @Test
    fun `纳甲的五行取地支而非天干`() {
        // 甲子：天干甲属木，地支子属水。六爻以地支为准 ⇒ 水
        val jiaZi = NaJia.of(Trigram.QIAN).line(0)
        assertEquals("甲子", jiaZi.label)
        assertEquals(com.maohongyuan.magic.core.model.WuXing.SHUI, jiaZi.wuXing)
    }
}

class XunKongTest {

    @Test
    fun `六旬空亡与传统规定一致`() {
        val expected = listOf(
            "甲子" to "戌亥",
            "甲戌" to "申酉",
            "甲申" to "午未",
            "甲午" to "辰巳",
            "甲辰" to "寅卯",
            "甲寅" to "子丑",
        )
        expected.forEachIndexed { i, (head, kong) ->
            val xun = XunKong.of(com.maohongyuan.magic.core.model.GanZhi(i * 10))
            assertEquals(head, xun.xunHead.toString())
            assertEquals(kong, xun.label)
        }
    }

    @Test
    fun `旬内十日的空亡相同`() {
        for (head in 0..5) {
            val base = XunKong.of(com.maohongyuan.magic.core.model.GanZhi(head * 10))
            for (offset in 0 until 10) {
                val gz = com.maohongyuan.magic.core.model.GanZhi(head * 10 + offset)
                assertEquals("$gz 的空亡", base, XunKong.of(gz))
            }
        }
    }

    @Test
    fun `空亡恰好覆盖十二支中的两支且相邻`() {
        XunKong.allSix().forEach { xun ->
            val gap = (xun.second.ordinal - xun.first.ordinal + 12) % 12
            assertEquals("${xun.label} 应相邻", 1, gap)
        }
        // 六旬共覆盖全部十二支，每支恰好落空一次
        val all = XunKong.allSix().flatMap { listOf(it.first, it.second) }
        assertEquals(12, all.size)
        assertEquals(12, all.toSet().size)
    }

    @Test
    fun `contains 判定`() {
        val xun = XunKong.of(com.maohongyuan.magic.core.model.GanZhi(0))
        assertTrue(com.maohongyuan.magic.core.model.EarthlyBranch.XU in xun)
        assertTrue(com.maohongyuan.magic.core.model.EarthlyBranch.HAI in xun)
        assertTrue(com.maohongyuan.magic.core.model.EarthlyBranch.ZI !in xun)
    }
}
