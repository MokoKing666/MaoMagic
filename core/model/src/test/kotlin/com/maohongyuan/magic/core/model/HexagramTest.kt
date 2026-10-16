package com.maohongyuan.magic.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HexagramTest {

    // ── 八卦 ──────────────────────────────────────────────────

    /**
     * 这一条是整个模型的地基。
     *
     * `Trigram.bits` 的 bit0 必须是最下面的初爻。写反了不会崩、也不会报错，
     * 只会让**所有卦象上下颠倒** —— 所以必须用卦符本身作参照逐卦核对。
     */
    @Test
    fun `八卦的爻象与卦符一致`() {
        val expected = mapOf(
            Trigram.QIAN to listOf(true, true, true),     // ☰ 三阳
            Trigram.DUI to listOf(true, true, false),     // ☱ 上缺
            Trigram.LI to listOf(true, false, true),      // ☲ 中虚
            Trigram.ZHEN to listOf(true, false, false),   // ☳ 下实
            Trigram.XUN to listOf(false, true, true),     // ☴ 下断
            Trigram.KAN to listOf(false, true, false),    // ☵ 中满
            Trigram.GEN to listOf(false, false, true),    // ☶ 上实
            Trigram.KUN to listOf(false, false, false),   // ☷ 三阴
        )
        expected.forEach { (trigram, lines) ->
            assertEquals("${trigram.label}${trigram.symbol} 自下而上的三爻", lines, trigram.lines)
            assertEquals(trigram.label, lines[0], trigram.bottomYang)
            assertEquals(trigram.label, lines[1], trigram.middleYang)
            assertEquals(trigram.label, lines[2], trigram.topYang)
        }
    }

    @Test
    fun `八卦的二进制编码与爻象互推`() {
        Trigram.entries.forEach { trigram ->
            val rebuilt = (if (trigram.bottomYang) 1 else 0) or
                (if (trigram.middleYang) 2 else 0) or
                (if (trigram.topYang) 4 else 0)
            assertEquals(trigram.label, rebuilt, trigram.bits)
            assertEquals(trigram, Trigram.fromBits(trigram.bits))
            assertEquals(
                trigram,
                Trigram.ofLines(trigram.bottomYang, trigram.middleYang, trigram.topYang),
            )
        }
        // 八个编码必须互不相同
        assertEquals(8, Trigram.entries.map { it.bits }.toSet().size)
    }

    @Test
    fun `错卦与综卦`() {
        // 错卦（旁通）：三爻全变
        assertEquals(Trigram.KUN, Trigram.QIAN.opposite)
        assertEquals(Trigram.GEN, Trigram.DUI.opposite)
        assertEquals(Trigram.KAN, Trigram.LI.opposite)
        assertEquals(Trigram.XUN, Trigram.ZHEN.opposite)
        Trigram.entries.forEach { assertEquals(it, it.opposite.opposite) }

        // 综卦（反卦）：上下颠倒。乾、坤、离、坎三爻对称，综卦即自身
        listOf(Trigram.QIAN, Trigram.KUN, Trigram.LI, Trigram.KAN).forEach {
            assertEquals("${it.label} 综卦应为自身", it, it.reversed)
        }
        assertEquals(Trigram.GEN, Trigram.ZHEN.reversed)
        assertEquals(Trigram.DUI, Trigram.XUN.reversed)
        Trigram.entries.forEach { assertEquals(it, it.reversed.reversed) }
    }

    @Test
    fun `八卦五行与家人卦象`() {
        assertEquals(WuXing.JIN, Trigram.QIAN.wuXing)
        assertEquals(WuXing.JIN, Trigram.DUI.wuXing)
        assertEquals(WuXing.HUO, Trigram.LI.wuXing)
        assertEquals(WuXing.MU, Trigram.ZHEN.wuXing)
        assertEquals(WuXing.MU, Trigram.XUN.wuXing)
        assertEquals(WuXing.SHUI, Trigram.KAN.wuXing)
        assertEquals(WuXing.TU, Trigram.GEN.wuXing)
        assertEquals(WuXing.TU, Trigram.KUN.wuXing)

        assertEquals("父", Trigram.QIAN.family)
        assertEquals("母", Trigram.KUN.family)
        assertEquals("长男", Trigram.ZHEN.family)
        assertEquals("长女", Trigram.XUN.family)
        assertEquals("中男", Trigram.KAN.family)
        assertEquals("中女", Trigram.LI.family)
        assertEquals("少男", Trigram.GEN.family)
        assertEquals("少女", Trigram.DUI.family)
    }

    // ── 六十四卦 ──────────────────────────────────────────────

    @Test
    fun `六十四卦的编码与名字一一对应且无重复`() {
        val all = Hexagram.all()
        assertEquals(Hexagram.COUNT, all.size)
        assertEquals(Hexagram.COUNT, all.map { it.code }.toSet().size)
        assertEquals(Hexagram.COUNT, all.map { it.name }.toSet().size)
        assertEquals(Hexagram.COUNT, all.map { it.kingWen }.toSet().size)
        assertEquals((0 until Hexagram.COUNT).toSet(), all.map { it.code }.toSet())
    }

    @Test
    fun `序卦传序号恰好是 1 到 64 的排列`() {
        val kingWen = Hexagram.all().map { it.kingWen }.sorted()
        assertEquals((1..Hexagram.COUNT).toList(), kingWen)
    }

    @Test
    fun `已知卦象的编码正确`() {
        assertEquals("乾为天", Hexagram.of(63).name)
        assertEquals("坤为地", Hexagram.of(0).name)
        // 上乾(0b111) 下巽(0b110) → 0b111110 = 62
        assertEquals("天风姤", Hexagram.of(62).name)
        assertEquals("天风姤", Hexagram.of(Trigram.QIAN, Trigram.XUN).name)
        // 上兑(0b011) 下离(0b101) → 0b011101 = 29
        assertEquals("泽火革", Hexagram.of(29).name)
        // 上坎(0b010) 下离(0b101) → 0b010101 = 21
        assertEquals("水火既济", Hexagram.of(21).name)
        // 上坤 下坎 → 地水师
        assertEquals("地水师", Hexagram.of(Trigram.KUN, Trigram.KAN).name)
    }

    @Test
    fun `上下卦与六爻编码自洽`() {
        Hexagram.all().forEach { hex ->
            val l = hex.lines
            // 下卦 = 初二三位
            assertEquals(
                "${hex.name} 的下卦",
                hex.lower,
                Trigram.ofLines(l[0], l[1], l[2]),
            )
            // 上卦 = 四五上位
            assertEquals(
                "${hex.name} 的上卦",
                hex.upper,
                Trigram.ofLines(l[3], l[4], l[5]),
            )
            // code 的 bit0..bit5 依次是初..上
            l.forEachIndexed { i, yang ->
                assertEquals("${hex.name} 第 ${i + 1} 爻", yang, (hex.code shr i) and 1 == 1)
            }
        }
    }

    @Test
    fun `泽火革的六爻为阳阴阳阳阳阴`() {
        // 下离 = 初阳 二阴 三阳；上兑 = 四阳 五阳 上阴
        val ge = Hexagram.byName("泽火革")!!
        assertEquals(listOf(true, false, true, true, true, false), ge.lines)
        assertEquals(Trigram.LI, ge.lower)
        assertEquals(Trigram.DUI, ge.upper)
        assertEquals(29, ge.code)
    }

    @Test
    fun `互卦取二三四爻与三四五爻`() {
        // 泽火革 爻象 阳阴阳阳阳阴
        // 互下 = 爻2,3,4 = 阴阳阳 → 巽(0b110)
        // 互上 = 爻3,4,5 = 阳阳阳 → 乾(0b111)
        // ⇒ 天风姤
        val ge = Hexagram.byName("泽火革")!!
        assertEquals("天风姤", ge.nuclear.name)
        assertEquals(Trigram.XUN, ge.nuclear.lower)
        assertEquals(Trigram.QIAN, ge.nuclear.upper)
    }

    @Test
    fun `变卦由指定爻位阴阳互变`() {
        // 泽火革 九四（index 3）动：上卦兑(0b011) 的初位翻转 → 0b010 = 坎
        // ⇒ 上坎下离 = 水火既济
        val ge = Hexagram.byName("泽火革")!!
        assertEquals("水火既济", ge.changing(3).name)

        // 变两次回到原卦
        assertEquals(ge, ge.changing(3).changing(3))
        // 六爻全变即错卦
        assertEquals(ge.opposite, ge.changing(0, 1, 2, 3, 4, 5))
    }

    @Test
    fun `错卦是六爻全变且为对合运算`() {
        Hexagram.all().forEach { hex ->
            assertEquals("${hex.name} 的错卦", hex.code xor 0b111111, hex.opposite.code)
            assertEquals(hex, hex.opposite.opposite)
            assertEquals(hex.upper.opposite, hex.opposite.upper)
            assertEquals(hex.lower.opposite, hex.opposite.lower)
        }
    }

    @Test
    fun `综卦是上下颠倒且为对合运算`() {
        Hexagram.all().forEach { hex ->
            assertEquals(hex, hex.reversed.reversed)
            // 综卦的六爻是原卦六爻的倒序
            assertEquals(hex.lines.reversed(), hex.reversed.lines)
        }
        // 三爻对称的卦（如乾为天、坤为地）综卦即自身
        assertEquals(Hexagram.byName("乾为天"), Hexagram.byName("乾为天")!!.reversed)
        assertEquals(Hexagram.byName("坤为地"), Hexagram.byName("坤为地")!!.reversed)
    }

    @Test
    fun `卦符来自序卦传序号而非编码`() {
        // 乾为天是序卦传第 1 卦 ⇒ U+4DC0
        assertEquals("\u4DC0", Hexagram.byName("乾为天")!!.symbol)
        // 坤为地是第 2 卦 ⇒ U+4DC1
        assertEquals("\u4DC1", Hexagram.byName("坤为地")!!.symbol)
        // 火水未济是第 64 卦 ⇒ U+4DFF
        assertEquals("\u4DFF", Hexagram.byName("火水未济")!!.symbol)
        // 泽火革是第 49 卦 ⇒ U+4DF0
        assertEquals("\u4DF0", Hexagram.byName("泽火革")!!.symbol)
    }

    @Test
    fun `按名字与序卦传查询`() {
        assertNotNull(Hexagram.byName("天风姤"))
        assertEquals(1, Hexagram.byKingWen(1)!!.kingWen)
        assertEquals(64, Hexagram.byKingWen(64)!!.kingWen)
        assertEquals("乾为天", Hexagram.byKingWen(1)!!.name)
        assertEquals("火水未济", Hexagram.byKingWen(64)!!.name)
        assertTrue(Hexagram.byName("不存在的卦") == null)
    }

    @Test
    fun `由六爻阴阳取卦`() {
        // 乾为天：六爻皆阳
        assertEquals("乾为天", Hexagram.ofLines(List(6) { true }).name)
        // 坤为地：六爻皆阴
        assertEquals("坤为地", Hexagram.ofLines(List(6) { false }).name)
        // 泽火革
        assertEquals(
            "泽火革",
            Hexagram.ofLines(listOf(true, false, true, true, true, false)).name,
        )
    }

    // ── 爻题 ──────────────────────────────────────────────────

    @Test
    fun `爻题按阴阳与爻位组合`() {
        assertEquals("初九", YaoPosition.CHU.title(yang = true))
        assertEquals("初六", YaoPosition.CHU.title(yang = false))
        assertEquals("九二", YaoPosition.ER.title(yang = true))
        assertEquals("六二", YaoPosition.ER.title(yang = false))
        assertEquals("六三", YaoPosition.SAN.title(yang = false))
        assertEquals("九四", YaoPosition.SI.title(yang = true))
        assertEquals("六五", YaoPosition.WU.title(yang = false))
        assertEquals("上九", YaoPosition.SHANG.title(yang = true))
        assertEquals("上六", YaoPosition.SHANG.title(yang = false))
    }

    @Test
    fun `爻题的生成与泽火革的实际爻题一致`() {
        // 泽火革：初九、六二、九三、九四、九五、上六
        val expected = listOf("初九", "六二", "九三", "九四", "九五", "上六")
        val ge = Hexagram.byName("泽火革")!!
        val actual = ge.lines.mapIndexed { i, yang -> YaoPosition.of(i).title(yang) }
        assertEquals(expected, actual)
    }
}
