package com.maohongyuan.magic.core.hexagram

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.HeavenlyStem

/**
 * 六爻神煞。
 *
 * ── 取法依据 ──
 * 见 `docs/神煞取法.md`。这里只实现**可信度最高**的那一批：
 * 驿马、桃花、华盖、将星、劫煞、亡神 —— 六个都由「日支 → 三合局 → 十二长生」推出，
 * 各家取法一致，不存在异文。
 *
 * 另有禄神（日干查）与阳干羊刃，取法也通行；阴干羊刃有两说，故不做。
 *
 * ── 为什么按十二长生推，而不是写死 24 个格子 ──
 * 写死表格看着直观，但**抄错一格没有任何测试能发现** ——
 * 排出来只是少一个或多一个神煞，不会报错。
 * 按长生位推，逻辑只有一处，错了测试立刻抓到。
 *
 * 长生位对应（三合局）：
 * ```
 * 申子辰（水）→ 长生在申
 * 寅午戌（火）→ 长生在寅
 * 巳酉丑（金）→ 长生在巳
 * 亥卯未（木）→ 长生在亥
 * ```
 * 各神煞取自十二长生的固定位：
 * ```
 * 桃花 = 沐浴（+1）   临官 = 禄（+3）   将星 = 帝旺（+4）
 * 驿马 = 病（+6）     华盖 = 墓（+8）   劫煞 = 绝（+9）   亡神 = 临官（+3）
 * ```
 * 注意 **亡神与禄同位**（临官），这是古法如此，不是笔误。
 */
enum class ShenSha(val label: String) {
    YI_MA("驿马"),
    TAO_HUA("桃花"),
    HUA_GAI("华盖"),
    JIANG_XING("将星"),
    JIE_SHA("劫煞"),
    WANG_SHEN("亡神"),
    LU_SHEN("禄神"),
    YANG_REN("羊刃"),
    ;

    companion object {

        /** 三合局 → 长生地支 */
        private val CHANG_SHENG: Map<Set<EarthlyBranch>, EarthlyBranch> = mapOf(
            setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to EarthlyBranch.SHEN,
            setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to EarthlyBranch.YIN,
            setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to EarthlyBranch.SI,
            setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to EarthlyBranch.HAI,
        )

        /** 十二长生位序（自长生起） */
        private const val MU_YU = 1      // 沐浴
        private const val LIN_GUAN = 3   // 临官
        private const val DI_WANG = 4    // 帝旺
        private const val BING = 6       // 病
        private const val MU = 8         // 墓
        private const val JUE = 9        // 绝

        /** 日干 → 禄支。丙戊同禄、丁己同禄，各家一致。 */
        private val LU: Map<HeavenlyStem, EarthlyBranch> = mapOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN,
            HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.BING to EarthlyBranch.SI,
            HeavenlyStem.DING to EarthlyBranch.WU,
            HeavenlyStem.WU to EarthlyBranch.SI,
            HeavenlyStem.JI to EarthlyBranch.WU,
            HeavenlyStem.GENG to EarthlyBranch.SHEN,
            HeavenlyStem.XIN to EarthlyBranch.YOU,
            HeavenlyStem.REN to EarthlyBranch.HAI,
            HeavenlyStem.GUI to EarthlyBranch.ZI,
        )

        /**
         * 阳干 → 羊刃（禄前一位）。
         *
         * **只做阳干**：阴干羊刃有「无刃」与「禄前一位」两说，
         * 取其一都可能与用户所学不符，索性留空。
         */
        private val YANG_REN: Map<HeavenlyStem, EarthlyBranch> = mapOf(
            HeavenlyStem.JIA to EarthlyBranch.MAO,
            HeavenlyStem.BING to EarthlyBranch.WU,
            HeavenlyStem.WU to EarthlyBranch.WU,
            HeavenlyStem.GENG to EarthlyBranch.YOU,
            HeavenlyStem.REN to EarthlyBranch.ZI,
        )

        /**
         * 按日辰推出当日全部神煞。
         *
         * @param dayStem  日干（禄神、羊刃用）
         * @param dayBranch 日支（其余六个用）
         */
        fun of(dayStem: HeavenlyStem, dayBranch: EarthlyBranch): List<ShenShaHit> {
            val group = CHANG_SHENG.entries.firstOrNull { dayBranch in it.key }?.value
                ?: return emptyList()   // 理论上不可能，地支必属某一局
            val hits = ArrayList<ShenShaHit>(8)

            fun at(shenSha: ShenSha, offset: Int) {
                hits += ShenShaHit(shenSha, group.plus(offset))
            }

            at(ShenSha.TAO_HUA, MU_YU)
            at(ShenSha.JIANG_XING, DI_WANG)
            at(ShenSha.YI_MA, BING)
            at(ShenSha.HUA_GAI, MU)
            at(ShenSha.JIE_SHA, JUE)
            at(ShenSha.WANG_SHEN, LIN_GUAN)

            LU[dayStem]?.let { hits += ShenShaHit(ShenSha.LU_SHEN, it) }
            YANG_REN[dayStem]?.let { hits += ShenShaHit(ShenSha.YANG_REN, it) }

            return hits
        }
    }
}

/** 一个神煞落在哪一支 */
data class ShenShaHit(val shenSha: ShenSha, val branch: EarthlyBranch)

/** 地支 + 偏移，按十二支循环 */
private fun EarthlyBranch.plus(offset: Int): EarthlyBranch {
    val all = EarthlyBranch.entries
    return all[(ordinal + offset).mod(all.size)]
}
