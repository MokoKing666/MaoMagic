package com.maohongyuan.magic.core.calendar

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.HeavenlyStem

/**
 * 八字里 lunar 有、tyme 没有的三组数据：**十神、藏干、地势**。
 *
 * 迁移到 tyme 后 `BaZiChart` 要自己算这三样。好在都能推导，不用抄表：
 *
 * - **十神**：由「日干 vs 目标干」的五行生克 + 阴阳同异唯一确定
 * - **藏干**：12 个地支的本气/中气/余气，固定表
 * - **地势**：十二长生，由长生位 + 阳顺阴逆推
 *
 * 全部拿 lunar 当 oracle 逐柱对拍（见 `BaZiSupplementParityTest`）。
 */
object BaZiSupplement {

    /** 天干五行 */
    private val STEM_ELEMENT: Map<HeavenlyStem, WuXingKind> = mapOf(
        HeavenlyStem.JIA to WuXingKind.WOOD, HeavenlyStem.YI to WuXingKind.WOOD,
        HeavenlyStem.BING to WuXingKind.FIRE, HeavenlyStem.DING to WuXingKind.FIRE,
        HeavenlyStem.WU to WuXingKind.EARTH, HeavenlyStem.JI to WuXingKind.EARTH,
        HeavenlyStem.GENG to WuXingKind.METAL, HeavenlyStem.XIN to WuXingKind.METAL,
        HeavenlyStem.REN to WuXingKind.WATER, HeavenlyStem.GUI to WuXingKind.WATER,
    )

    /** 天干阴阳：甲丙戊庚壬为阳 */
    private fun isYang(stem: HeavenlyStem): Boolean = stem.ordinal % 2 == 0

    /** 五行相生：木→火→土→金→水→木 */
    private fun generates(a: WuXingKind, b: WuXingKind): Boolean = when (a) {
        WuXingKind.WOOD -> b == WuXingKind.FIRE
        WuXingKind.FIRE -> b == WuXingKind.EARTH
        WuXingKind.EARTH -> b == WuXingKind.METAL
        WuXingKind.METAL -> b == WuXingKind.WATER
        WuXingKind.WATER -> b == WuXingKind.WOOD
    }

    /**
     * 十神。
     *
     * 以日干为「我」，看目标干与我的关系：
     * ```
     * 同我   → 比肩(同阴阳) / 劫财(异)
     * 我生   → 食神(同)     / 伤官(异)
     * 我克   → 偏财(同)     / 正财(异)
     * 克我   → 七杀(同)     / 正官(异)
     * 生我   → 偏印(同)     / 正印(异)
     * ```
     * 同阴阳者偏、异阴阳者正 —— 比肩/七杀/偏印/偏财/食神是「同」，其余是「异」。
     */
    fun tenGod(dayStem: HeavenlyStem, target: HeavenlyStem): String {
        val me = STEM_ELEMENT.getValue(dayStem)
        val other = STEM_ELEMENT.getValue(target)
        val same = isYang(dayStem) == isYang(target)
        return when {
            me == other -> if (same) "比肩" else "劫财"
            generates(me, other) -> if (same) "食神" else "伤官"
            generates(other, me) -> if (same) "偏印" else "正印"
            controls(me, other) -> if (same) "偏财" else "正财"
            else -> if (same) "七杀" else "正官"
        }
    }

    /** 五行相克：木→土→水→火→金→木 */
    private fun controls(a: WuXingKind, b: WuXingKind): Boolean = when (a) {
        WuXingKind.WOOD -> b == WuXingKind.EARTH
        WuXingKind.EARTH -> b == WuXingKind.WATER
        WuXingKind.WATER -> b == WuXingKind.FIRE
        WuXingKind.FIRE -> b == WuXingKind.METAL
        WuXingKind.METAL -> b == WuXingKind.WOOD
    }

    /** 地支藏干（本气、中气、余气），自子起 */
    private val HIDE_STEMS: Map<EarthlyBranch, List<HeavenlyStem>> = mapOf(
        EarthlyBranch.ZI to listOf(HeavenlyStem.GUI),
        EarthlyBranch.CHOU to listOf(HeavenlyStem.JI, HeavenlyStem.GUI, HeavenlyStem.XIN),
        EarthlyBranch.YIN to listOf(HeavenlyStem.JIA, HeavenlyStem.BING, HeavenlyStem.WU),
        EarthlyBranch.MAO to listOf(HeavenlyStem.YI),
        EarthlyBranch.CHEN to listOf(HeavenlyStem.WU, HeavenlyStem.YI, HeavenlyStem.GUI),
        EarthlyBranch.SI to listOf(HeavenlyStem.BING, HeavenlyStem.GENG, HeavenlyStem.WU),
        EarthlyBranch.WU to listOf(HeavenlyStem.DING, HeavenlyStem.JI),
        EarthlyBranch.WEI to listOf(HeavenlyStem.JI, HeavenlyStem.DING, HeavenlyStem.YI),
        EarthlyBranch.SHEN to listOf(HeavenlyStem.GENG, HeavenlyStem.REN, HeavenlyStem.WU),
        EarthlyBranch.YOU to listOf(HeavenlyStem.XIN),
        EarthlyBranch.XU to listOf(HeavenlyStem.WU, HeavenlyStem.XIN, HeavenlyStem.DING),
        EarthlyBranch.HAI to listOf(HeavenlyStem.REN, HeavenlyStem.JIA),
    )

    fun hideStems(branch: EarthlyBranch): List<HeavenlyStem> = HIDE_STEMS.getValue(branch)

    /** 十天干的长生支：甲亥、乙午、丙寅、丁酉、戊寅、己酉、庚巳、辛子、壬申、癸卯 */
    private val CHANG_SHENG_BRANCH: Map<HeavenlyStem, EarthlyBranch> = mapOf(
        HeavenlyStem.JIA to EarthlyBranch.HAI,
        HeavenlyStem.YI to EarthlyBranch.WU,
        HeavenlyStem.BING to EarthlyBranch.YIN,
        HeavenlyStem.DING to EarthlyBranch.YOU,
        HeavenlyStem.WU to EarthlyBranch.YIN,
        HeavenlyStem.JI to EarthlyBranch.YOU,
        HeavenlyStem.GENG to EarthlyBranch.SI,
        HeavenlyStem.XIN to EarthlyBranch.ZI,
        HeavenlyStem.REN to EarthlyBranch.SHEN,
        HeavenlyStem.GUI to EarthlyBranch.MAO,
    )

    private val TERRAIN_NAMES = listOf(
        "长生", "沐浴", "冠带", "临官", "帝旺", "衰",
        "病", "死", "墓", "绝", "胎", "养",
    )

    /**
     * 地势（十二长生）。
     *
     * **阳干顺行、阴干逆行** —— 这是与「六爻神煞按三合局推」不同的地方，
     * 神煞一律顺推，八字地势要分阴阳。
     */
    fun terrain(dayStem: HeavenlyStem, branch: EarthlyBranch): String {
        val start = CHANG_SHENG_BRANCH.getValue(dayStem).ordinal
        val offset = if (isYang(dayStem)) {
            (branch.ordinal - start).mod(12)
        } else {
            (start - branch.ordinal).mod(12)
        }
        return TERRAIN_NAMES[offset]
    }
}

/** 五行（本模块内自用，避免与 core:model 的 WuXing 混淆） */
enum class WuXingKind { WOOD, FIRE, EARTH, METAL, WATER }
