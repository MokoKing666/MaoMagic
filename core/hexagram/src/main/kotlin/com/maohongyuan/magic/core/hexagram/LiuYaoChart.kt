package com.maohongyuan.magic.core.hexagram

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.GanZhi
import com.maohongyuan.magic.core.model.Hexagram
import com.maohongyuan.magic.core.model.LiuQin
import com.maohongyuan.magic.core.model.LiuShen
import com.maohongyuan.magic.core.model.Trigram
import com.maohongyuan.magic.core.model.WuXing
import com.maohongyuan.magic.core.model.WuXingRelation
import com.maohongyuan.magic.core.model.YaoPosition

/** 世应标记 */
enum class YaoRelation(val label: String?) {
    SHI("世"),
    YING("应"),
    NONE(null),
}

/**
 * 五行在月令下的旺衰。
 *
 * 以月建五行为「令」：
 *
 * | 关系 | 状态 |
 * |---|---|
 * | 与令同 | 旺 |
 * | 令生我 | 相 |
 * | 我生令 | 休 |
 * | 我克令 | 囚 |
 * | 令克我 | 死 |
 *
 * 例：春（木令）→ 木旺、火相、水休、金囚、土死。
 */
enum class WangShuai(val label: String) {
    WANG("旺"),
    XIANG("相"),
    XIU("休"),
    QIU("囚"),
    SI("死"),
    ;

    companion object {
        fun of(element: WuXing, monthBranch: EarthlyBranch): WangShuai {
            val month = monthBranch.wuXing
            return when {
                element == month -> WANG
                month.generates == element -> XIANG
                element.generates == month -> XIU
                element.overcomes == month -> QIU
                else -> SI
            }
        }

        /**
         * 生克关系的吉凶倾向（体用、世应之间）。
         *
         * 吉：用生体、比和、体克用；凶：体生用、用克体。
         */
        fun isFavorable(relation: WuXingRelation): Boolean = when (relation) {
            WuXingRelation.IT_GENERATES, WuXingRelation.SAME, WuXingRelation.I_OVERCOME -> true
            WuXingRelation.I_GENERATE, WuXingRelation.IT_OVERCOMES -> false
        }
    }
}

/**
 * 伏神。
 *
 * 当本卦六爻缺少某个六亲时，从**本宫首卦（八纯卦）**借来，
 * 挂在首卦中该六亲所在的**同一爻位**之下。
 *
 * 例：天风姤（乾宫）六亲为 父母/子孙/兄弟/官鬼/兄弟/父母，缺「妻财」；
 * 乾为天的二爻为甲寅木（妻财），故「妻财甲寅」伏于天风姤二爻之下。
 */
data class FuShen(
    val liuQin: LiuQin,
    val naJia: NaJiaLine,
    /** 伏于第几爻之下（0 = 初爻） */
    val underIndex: Int,
) {
    val label: String get() = "${liuQin.label}${naJia.label}"
}

/**
 * 变爻 —— 动爻变化后得到的那一爻。
 *
 * ⚠️ 变爻的六亲按**变卦自身的宫位五行**重算，不是沿用本卦的宫位。
 *    这是最容易写错的一处：天风姤（乾宫金）九四壬午火动，变出丁亥水；
 *    变卦泽风大过属震宫（木），水生于木 ⇒ 变爻六亲为「父母」，而非本卦宫位推出的结果。
 */
data class BianYao(
    val naJia: NaJiaLine,
    val liuQin: LiuQin,
    /** 变卦所属宫 */
    val palace: Trigram,
) {
    val label: String get() = "${liuQin.label}${naJia.label}"
}

/** 一爻的完整装卦信息。 */
data class LiuYaoLine(
    /** 0 = 初爻 */
    val index: Int,
    val yang: Boolean,
    val naJia: NaJiaLine,
    val liuQin: LiuQin,
    val liuShen: LiuShen,
    val relation: YaoRelation,
    val moving: Boolean,
    /** 是否旬空 */
    val kong: Boolean,
    val wangShuai: WangShuai,
    val fuShen: FuShen?,
    /** 仅动爻非空 */
    val bian: BianYao?,
) {
    val position: YaoPosition get() = YaoPosition.of(index)

    /** 爻题，如「九四」 */
    val title: String get() = position.title(yang)
}

/**
 * 六爻排盘结果。
 */
data class LiuYaoChart(
    val hexagram: Hexagram,
    val palace: PalaceHexagram,
    /** 自初爻至上爻 */
    val lines: List<LiuYaoLine>,
    /** 变卦。无动爻时为 null */
    val changed: Hexagram?,
    val monthBranch: EarthlyBranch,
    val dayGanZhi: GanZhi,
    val xunKong: XunKong,
    val question: String,
) {
    val movingIndexes: List<Int> get() = lines.filter { it.moving }.map { it.index }

    val shiLine: LiuYaoLine get() = lines.first { it.relation == YaoRelation.SHI }
    val yingLine: LiuYaoLine get() = lines.first { it.relation == YaoRelation.YING }

    /** 某六亲所在的爻（本卦范围内） */
    fun linesOf(liuQin: LiuQin): List<LiuYaoLine> = lines.filter { it.liuQin == liuQin }

    /** 本卦六爻中缺失的六亲（这些六亲会以伏神形式出现） */
    val missingLiuQin: List<LiuQin>
        get() = LiuQin.entries.filter { qin -> lines.none { it.liuQin == qin } }

    /** 所有伏神 */
    val fuShenList: List<FuShen> get() = lines.mapNotNull { it.fuShen }
}

/**
 * 六爻装卦。
 *
 * 这是全项目的技术核心。装卦六步：
 * 1. **定宫** —— 由 [HexagramTable] 按京房八宫变爻规则取得宫位与世应
 * 2. **纳甲** —— 内外卦各按 [NaJia] 配干支
 * 3. **六亲** —— 以宫位五行为「我」，由爻的地支五行推导
 * 4. **六神** —— 按日干起于初爻
 * 5. **旬空** —— 由日干支定
 * 6. **伏神 / 变爻** —— 缺六亲则借本宫首卦；有动爻则算变卦并重算六亲
 */
object LiuYaoDivination {

    /**
     * 装卦。
     *
     * @param movingIndexes 动爻位置（0 = 初爻）。空集合表示静卦。
     */
    fun chart(
        hexagram: Hexagram,
        dayGanZhi: GanZhi,
        monthBranch: EarthlyBranch,
        movingIndexes: Set<Int> = emptySet(),
        question: String = "",
    ): LiuYaoChart {
        require(movingIndexes.all { it in 0..5 }) { "动爻位置必须在 0..5" }

        val palace = HexagramTable.of(hexagram)
        val xunKong = XunKong.of(dayGanZhi)
        val liuShenSeq = LiuShen.sequence(dayGanZhi.stem)

        // 变卦：仅在有动爻时存在
        val changed = if (movingIndexes.isEmpty()) {
            null
        } else {
            hexagram.changing(*movingIndexes.toIntArray())
        }
        val changedPalace = changed?.let { HexagramTable.of(it) }

        val lines = (0..5).map { i ->
            val naJia = naJiaAt(hexagram, i)
            val moving = i in movingIndexes

            LiuYaoLine(
                index = i,
                yang = hexagram.lines[i],
                naJia = naJia,
                liuQin = LiuQin.of(palace.palaceWuXing, naJia.wuXing),
                liuShen = liuShenSeq[i],
                relation = when {
                    palace.isShi(i) -> YaoRelation.SHI
                    palace.isYing(i) -> YaoRelation.YING
                    else -> YaoRelation.NONE
                },
                moving = moving,
                kong = naJia.branch in xunKong,
                wangShuai = WangShuai.of(naJia.wuXing, monthBranch),
                fuShen = null, // 稍后统一填
                bian = if (moving && changed != null && changedPalace != null) {
                    val bianNaJia = naJiaAt(changed, i)
                    BianYao(
                        naJia = bianNaJia,
                        // 变爻六亲按变卦自身宫位重算
                        liuQin = LiuQin.of(changedPalace.palaceWuXing, bianNaJia.wuXing),
                        palace = changedPalace.palace,
                    )
                } else {
                    null
                },
            )
        }

        val withFuShen = attachFuShen(lines, palace)

        return LiuYaoChart(
            hexagram = hexagram,
            palace = palace,
            lines = withFuShen,
            changed = changed,
            monthBranch = monthBranch,
            dayGanZhi = dayGanZhi,
            xunKong = xunKong,
            question = question,
        )
    }

    /** 取某卦第 [index] 爻的纳甲。下卦用内盘，上卦用外盘。 */
    private fun naJiaAt(hexagram: Hexagram, index: Int): NaJiaLine {
        val trigram = if (index < 3) hexagram.lower else hexagram.upper
        return NaJia.of(trigram).line(index)
    }

    /**
     * 补齐伏神。
     *
     * 规则：本卦六爻若缺某六亲，则到**本宫首卦（八纯卦）**里找该六亲，
     * 挂在它所在的**同一爻位**之下。
     */
    private fun attachFuShen(
        lines: List<LiuYaoLine>,
        palace: PalaceHexagram,
    ): List<LiuYaoLine> {
        val present = lines.map { it.liuQin }.toSet()
        val missing = LiuQin.entries.filter { it !in present }
        if (missing.isEmpty()) return lines

        // 本宫首卦的六爻装卦（仅需六亲与纳甲）
        val benGong = Hexagram.of(palace.palace, palace.palace)
        val benGongLines = (0..5).map { i ->
            val naJia = naJiaAt(benGong, i)
            i to (naJia to LiuQin.of(palace.palaceWuXing, naJia.wuXing))
        }

        // 每个缺失的六亲，取首卦中**首次出现**的爻位
        val fuShenByIndex: Map<Int, FuShen> = missing.mapNotNull { qin ->
            val hit = benGongLines.firstOrNull { it.second.second == qin } ?: return@mapNotNull null
            val (idx, pair) = hit
            idx to FuShen(liuQin = qin, naJia = pair.first, underIndex = idx)
        }.toMap()

        return lines.map { line ->
            fuShenByIndex[line.index]?.let { line.copy(fuShen = it) } ?: line
        }
    }
}
