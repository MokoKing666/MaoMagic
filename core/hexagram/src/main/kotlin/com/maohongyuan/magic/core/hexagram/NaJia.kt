package com.maohongyuan.magic.core.hexagram

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.HeavenlyStem
import com.maohongyuan.magic.core.model.Trigram
import com.maohongyuan.magic.core.model.WuXing

/**
 * 一爻的纳甲（干支）。
 *
 * 六亲、旺衰、空亡全部以**地支**为准，所以 [wuXing] 取的是 [branch] 的五行，
 * 不是天干的。这一点容易写错 —— 天干在纳甲里只用于显示与部分神煞。
 */
data class NaJiaLine(val stem: HeavenlyStem, val branch: EarthlyBranch) {
    val wuXing: WuXing get() = branch.wuXing
    val label: String get() = "${stem.label}${branch.label}"

    override fun toString(): String = label
}

/**
 * 八卦纳甲表。
 *
 * 口诀：**乾纳甲壬，坤纳乙癸，震纳庚，巽纳辛，坎纳戊，离纳己，艮纳丙，兑纳丁**。
 * 其中乾坤两卦内外卦天干不同（乾内甲外壬、坤内乙外癸），其余六卦内外同干。
 *
 * 这是**传统规定**，不可推导，故用数据表。
 * 表本身由 [NaJia.of] 按口诀构造，并带自检 —— 见同文件的 init 校验。
 */
data class NaJia(
    /** 内卦三爻：初、二、三 */
    val inner: List<NaJiaLine>,
    /** 外卦三爻：四、五、上 */
    val outer: List<NaJiaLine>,
) {
    init {
        require(inner.size == 3) { "内卦应有 3 爻，实际 ${inner.size}" }
        require(outer.size == 3) { "外卦应有 3 爻，实际 ${outer.size}" }
    }

    /** 取第 [index] 爻（0 = 初爻）的纳甲。 */
    fun line(index: Int): NaJiaLine {
        require(index in 0..5) { "爻位必须在 0..5，实际 $index" }
        return if (index < 3) inner[index] else outer[index - 3]
    }

    companion object {
        private fun line(stem: HeavenlyStem, branch: EarthlyBranch) = NaJiaLine(stem, branch)

        /**
         * 纳甲表。构造时按口诀展开，不手写 48 条记录。
         *
         * 每卦给「内卦天干、内卦三支」与「外卦天干、外卦三支」。
         */
        private val TABLE: Map<Trigram, NaJia> = buildMap {
            // 乾：内甲子寅辰，外壬午申戌
            put(
                Trigram.QIAN,
                NaJia(
                    inner = listOf(
                        line(HeavenlyStem.JIA, EarthlyBranch.ZI),
                        line(HeavenlyStem.JIA, EarthlyBranch.YIN),
                        line(HeavenlyStem.JIA, EarthlyBranch.CHEN),
                    ),
                    outer = listOf(
                        line(HeavenlyStem.REN, EarthlyBranch.WU),
                        line(HeavenlyStem.REN, EarthlyBranch.SHEN),
                        line(HeavenlyStem.REN, EarthlyBranch.XU),
                    ),
                ),
            )
            // 兑：内外同干丁，内巳卯丑，外亥酉未
            put(
                Trigram.DUI,
                NaJia(
                    inner = listOf(
                        line(HeavenlyStem.DING, EarthlyBranch.SI),
                        line(HeavenlyStem.DING, EarthlyBranch.MAO),
                        line(HeavenlyStem.DING, EarthlyBranch.CHOU),
                    ),
                    outer = listOf(
                        line(HeavenlyStem.DING, EarthlyBranch.HAI),
                        line(HeavenlyStem.DING, EarthlyBranch.YOU),
                        line(HeavenlyStem.DING, EarthlyBranch.WEI),
                    ),
                ),
            )
            // 离：内外同干己，内卯丑亥，外酉未巳
            put(
                Trigram.LI,
                NaJia(
                    inner = listOf(
                        line(HeavenlyStem.JI, EarthlyBranch.MAO),
                        line(HeavenlyStem.JI, EarthlyBranch.CHOU),
                        line(HeavenlyStem.JI, EarthlyBranch.HAI),
                    ),
                    outer = listOf(
                        line(HeavenlyStem.JI, EarthlyBranch.YOU),
                        line(HeavenlyStem.JI, EarthlyBranch.WEI),
                        line(HeavenlyStem.JI, EarthlyBranch.SI),
                    ),
                ),
            )
            // 震：内外同干庚，内子寅辰，外午申戌
            put(
                Trigram.ZHEN,
                NaJia(
                    inner = listOf(
                        line(HeavenlyStem.GENG, EarthlyBranch.ZI),
                        line(HeavenlyStem.GENG, EarthlyBranch.YIN),
                        line(HeavenlyStem.GENG, EarthlyBranch.CHEN),
                    ),
                    outer = listOf(
                        line(HeavenlyStem.GENG, EarthlyBranch.WU),
                        line(HeavenlyStem.GENG, EarthlyBranch.SHEN),
                        line(HeavenlyStem.GENG, EarthlyBranch.XU),
                    ),
                ),
            )
            // 巽：内外同干辛，内丑亥酉，外未巳卯
            put(
                Trigram.XUN,
                NaJia(
                    inner = listOf(
                        line(HeavenlyStem.XIN, EarthlyBranch.CHOU),
                        line(HeavenlyStem.XIN, EarthlyBranch.HAI),
                        line(HeavenlyStem.XIN, EarthlyBranch.YOU),
                    ),
                    outer = listOf(
                        line(HeavenlyStem.XIN, EarthlyBranch.WEI),
                        line(HeavenlyStem.XIN, EarthlyBranch.SI),
                        line(HeavenlyStem.XIN, EarthlyBranch.MAO),
                    ),
                ),
            )
            // 坎：内外同干戊，内寅辰午，外申戌子
            put(
                Trigram.KAN,
                NaJia(
                    inner = listOf(
                        line(HeavenlyStem.WU, EarthlyBranch.YIN),
                        line(HeavenlyStem.WU, EarthlyBranch.CHEN),
                        line(HeavenlyStem.WU, EarthlyBranch.WU),
                    ),
                    outer = listOf(
                        line(HeavenlyStem.WU, EarthlyBranch.SHEN),
                        line(HeavenlyStem.WU, EarthlyBranch.XU),
                        line(HeavenlyStem.WU, EarthlyBranch.ZI),
                    ),
                ),
            )
            // 艮：内外同干丙，内辰午申，外戌子寅
            put(
                Trigram.GEN,
                NaJia(
                    inner = listOf(
                        line(HeavenlyStem.BING, EarthlyBranch.CHEN),
                        line(HeavenlyStem.BING, EarthlyBranch.WU),
                        line(HeavenlyStem.BING, EarthlyBranch.SHEN),
                    ),
                    outer = listOf(
                        line(HeavenlyStem.BING, EarthlyBranch.XU),
                        line(HeavenlyStem.BING, EarthlyBranch.ZI),
                        line(HeavenlyStem.BING, EarthlyBranch.YIN),
                    ),
                ),
            )
            // 坤：内乙未巳卯，外癸丑亥酉
            put(
                Trigram.KUN,
                NaJia(
                    inner = listOf(
                        line(HeavenlyStem.YI, EarthlyBranch.WEI),
                        line(HeavenlyStem.YI, EarthlyBranch.SI),
                        line(HeavenlyStem.YI, EarthlyBranch.MAO),
                    ),
                    outer = listOf(
                        line(HeavenlyStem.GUI, EarthlyBranch.CHOU),
                        line(HeavenlyStem.GUI, EarthlyBranch.HAI),
                        line(HeavenlyStem.GUI, EarthlyBranch.YOU),
                    ),
                ),
            )

            check(size == Trigram.entries.size) {
                "纳甲表应覆盖全部 ${Trigram.entries.size} 卦，实际 $size"
            }
        }

        fun of(trigram: Trigram): NaJia =
            TABLE[trigram] ?: error("纳甲表缺少 ${trigram.label}")
    }
}
