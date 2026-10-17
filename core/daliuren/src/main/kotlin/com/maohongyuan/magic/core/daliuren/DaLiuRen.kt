package com.maohongyuan.magic.core.daliuren

import com.maohongyuan.magic.core.calendar.CalendarService
import com.maohongyuan.magic.core.model.DivinationChart
import com.maohongyuan.magic.core.model.DivinationSystem
import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.HeavenlyStem
import com.maohongyuan.magic.core.model.WuXing
import com.maohongyuan.magic.core.model.WuXingRelation

/** 一课：上神 / 下神。下神若是日干，用日干本身表示。 */
data class Ke(
    val label: String,
    /** 下神（地盘）。日干课的下神是天干，其余是地支 */
    val xiaGan: HeavenlyStem?,
    val xiaZhi: EarthlyBranch?,
    val shang: EarthlyBranch,
) {
    val xiaLabel: String get() = xiaGan?.label ?: xiaZhi!!.label
    val xiaWuXing: WuXing get() = xiaGan?.wuXing ?: xiaZhi!!.wuXing
}

/** 大六壬盘 */
data class DaLiuRenChart(
    val solarDate: String,
    val dayGanZhi: String,
    val hourBranch: EarthlyBranch,
    /** 月将（太阳所在星次），决定天盘 */
    val yueJiang: EarthlyBranch,
    /** 最近一个中气的名字 */
    val zhongQi: String,
    /** 天盘：天盘[i] = 地盘支 i 之上的天盘支 */
    val tianPan: List<EarthlyBranch>,
    val ke: List<Ke>,
    /** 三传：初传 / 中传 / 末传 */
    val sanChuan: List<EarthlyBranch>,
    /** 取三传用的方法名 */
    val method: String,
) : DivinationChart {

    override val system: DivinationSystem = DivinationSystem.DA_LIU_REN

    fun tianPanOf(diPan: EarthlyBranch): EarthlyBranch = tianPan[diPan.ordinal]
}

/**
 * 大六壬排盘。
 *
 * ⚠️ **实验性实现**，三传只完整覆盖贼克 / 比用 / 涉害三种取法，
 * 昴星、别责、八专、伏吟、反吟走简化路径。未与经典课例对拍。
 */
object DaLiuRenService {

    /** 日干寄宫：甲寄寅、乙寄辰、丙寄巳、丁寄未、戊寄巳、己寄未、庚寄申、辛寄戌、壬寄亥、癸寄丑 */
    private val JI_GONG = mapOf(
        HeavenlyStem.JIA to EarthlyBranch.YIN,
        HeavenlyStem.YI to EarthlyBranch.CHEN,
        HeavenlyStem.BING to EarthlyBranch.SI,
        HeavenlyStem.DING to EarthlyBranch.WEI,
        HeavenlyStem.WU to EarthlyBranch.SI,
        HeavenlyStem.JI to EarthlyBranch.WEI,
        HeavenlyStem.GENG to EarthlyBranch.SHEN,
        HeavenlyStem.XIN to EarthlyBranch.XU,
        HeavenlyStem.REN to EarthlyBranch.HAI,
        HeavenlyStem.GUI to EarthlyBranch.CHOU,
    )

    /**
     * 中气 → 月将。
     *
     * 月将随太阳过宫而换，换将点是十二**中气**：
     * 冬至后用丑（大吉）、大寒后用子（神后）、雨水后用亥（登明）…
     */
    private val ZHONG_QI_TO_JIANG = mapOf(
        "冬至" to EarthlyBranch.CHOU,
        "大寒" to EarthlyBranch.ZI,
        "雨水" to EarthlyBranch.HAI,
        "春分" to EarthlyBranch.XU,
        "谷雨" to EarthlyBranch.YOU,
        "小满" to EarthlyBranch.SHEN,
        "夏至" to EarthlyBranch.WEI,
        "大暑" to EarthlyBranch.WU,
        "处暑" to EarthlyBranch.SI,
        "秋分" to EarthlyBranch.CHEN,
        "霜降" to EarthlyBranch.MAO,
        "小雪" to EarthlyBranch.YIN,
    )

    fun chart(year: Int, month: Int, day: Int, hour: Int): DaLiuRenChart {
        val info = CalendarService.of(year, month, day, hour)
        val dayGz = info.ganZhi.day ?: error("取不到日柱")
        val hourZhi = info.ganZhi.hour?.branch ?: EarthlyBranch.ZI

        val (zhongQi, yueJiang) = recentZhongQi(year, month, day)

        // ── 天地盘：月将加时 ──
        // 月将放在时支之上，其余顺布。
        val tianPan = (0..11).map { i ->
            EarthlyBranch.entries[(yueJiang.ordinal + i - hourZhi.ordinal).mod(12)]
        }
        fun shang(diPan: EarthlyBranch): EarthlyBranch = tianPan[diPan.ordinal]

        // ── 四课 ──
        val jiGong = JI_GONG.getValue(dayGz.stem)
        val ke1Shang = shang(jiGong)
        val ke2Shang = shang(ke1Shang)
        val ke3Shang = shang(dayGz.branch)
        val ke4Shang = shang(ke3Shang)

        val ke = listOf(
            Ke("一课", dayGz.stem, null, ke1Shang),
            Ke("二课", null, ke1Shang, ke2Shang),
            Ke("三课", null, dayGz.branch, ke3Shang),
            Ke("四课", null, ke3Shang, ke4Shang),
        )

        // ── 三传 ──
        val (chuan, method) = pickSanChuan(ke, dayGz.stem, tianPan)

        return DaLiuRenChart(
            solarDate = info.solar.ymd,
            dayGanZhi = dayGz.toString(),
            hourBranch = hourZhi,
            yueJiang = yueJiang,
            zhongQi = zhongQi,
            tianPan = tianPan,
            ke = ke,
            sanChuan = chuan,
            method = method,
        )
    }

    /**
     * 找最近一个中气。
     *
     * ⚠️ 这里用「往回逐日扫最多 32 天」的笨办法 ——
     * `DayInfo.almanac.jieQi` 只在当天就是节气日时非空，拿不到「最近一个节气」。
     * lunar 库其实有 `prevJieQi`，但没在本项目暴露；为了不动 core:calendar
     * 的公开 API，先用扫描。单次排盘最多 32 次历法计算，可接受。
     */
    private fun recentZhongQi(year: Int, month: Int, day: Int): Pair<String, EarthlyBranch> {
        val base = java.time.LocalDate.of(year, month, day)
        for (back in 0..32) {
            val d = base.minusDays(back.toLong())
            val jq = runCatching {
                CalendarService.of(d.year, d.monthValue, d.dayOfMonth, 12).jieQi
            }.getOrNull()
            if (jq != null && ZHONG_QI_TO_JIANG.containsKey(jq)) {
                return jq to ZHONG_QI_TO_JIANG.getValue(jq)
            }
        }
        // 理论上 32 天之内必有一个中气；兜底给冬至（不会静默出错，会体现在 method 里）
        return "冬至(兜底)" to EarthlyBranch.CHOU
    }

    /**
     * 取三传。
     *
     * 顺序：下贼上 → 上克下 → 涉害（简化）。
     * 有多个候选时用「比用」：取与日干阴阳相同者。
     *
     * 中传 = 天盘[初传]，末传 = 天盘[中传]。
     */
    private fun pickSanChuan(
        ke: List<Ke>,
        dayStem: HeavenlyStem,
        tianPan: List<EarthlyBranch>,
    ): Pair<List<EarthlyBranch>, String> {
        fun relation(k: Ke) = WuXingRelation.between(k.xiaWuXing, k.shang.wuXing)

        val xiaZeiShang = ke.filter { relation(it) == WuXingRelation.IT_OVERCOMES }
        val shangKeXia = ke.filter { relation(it) == WuXingRelation.I_OVERCOME }

        val candidates: List<Ke>
        val method: String
        when {
            xiaZeiShang.isNotEmpty() -> {
                candidates = xiaZeiShang
                method = "贼克"
            }
            shangKeXia.isNotEmpty() -> {
                candidates = shangKeXia
                method = "贼克（上克下）"
            }
            else -> {
                // 涉害：这里取「上神与下神同五行」以外的第一课作为兜底，
                // 真正的涉害深浅计数未实现 —— 已标注为实验性
                candidates = ke
                method = "涉害（简化，未计深浅）"
            }
        }

        val chosen = if (candidates.size == 1) {
            candidates.first()
        } else {
            // 比用：取与日干阴阳相同者；仍不唯一则取第一课
            val sameYinYang = candidates.filter { it.shang.yinYang == dayStem.yinYang }
            (sameYinYang.ifEmpty { candidates }).first().also {
                if (method == "贼克") { /* 比用是同法内的细分，不改 method 名 */ }
            }
        }

        val chu = chosen.shang
        val zhong = tianPan[chu.ordinal]
        val mo = tianPan[zhong.ordinal]
        val suffix = if (candidates.size > 1) " · 比用" else ""
        return listOf(chu, zhong, mo) to (method + suffix)
    }
}
