package com.maohongyuan.magic.core.calendar

import com.maohongyuan.magic.core.model.GanZhi
import com.maohongyuan.magic.core.model.HeavenlyStem
import com.maohongyuan.magic.core.model.WuXingRelation

/**
 * 流年 —— 大运之下的逐年干支与十神。
 *
 * ── 为什么必须自己算 ──
 * `lunar` 库只给出**四柱**的干支与十神，不给「任意某一年」的。
 * 而流年就是「某一步大运覆盖的那 10 个年份」各自的干支，属可推导的算术，
 * 不需要也不该查表。
 *
 * @param year 公历年
 * @param age 虚岁年龄（该年命主几岁）
 * @param ganZhi 该年干支
 * @param shiShen 该年天干相对日主的十神
 */
data class LiuNian(
    val year: Int,
    val age: Int,
    val ganZhi: GanZhi,
    val shiShen: String,
)

object LiuNianService {

    /**
     * 公历年 → 干支。
     *
     * 基准：**公元 4 年为甲子年**（这是干支纪年的通行换算起点）。
     * 于是 `index = (year - 4) mod 60`，0 = 甲子。
     *
     * 自检：1984 → (1984-4) mod 60 = 0 → 甲子 ✓（1984 确为甲子年）
     *      2026 → 2022 mod 60 = 42 → 丙午 ✓
     */
    fun yearGanZhi(year: Int): GanZhi = GanZhi(((year - 4) % 60 + 60) % 60)

    /**
     * 十神 —— [target] 相对日主 [dayMaster] 的关系。
     *
     * 五行定大类（比劫 / 食伤 / 财 / 官杀 / 印），
     * 阴阳是否相同定细分（同则偏，异则正；但比劫相反：同则比肩、异则劫财）。
     */
    fun shiShen(dayMaster: HeavenlyStem, target: HeavenlyStem): String {
        val same = dayMaster.yinYang == target.yinYang
        return when (WuXingRelation.between(dayMaster.wuXing, target.wuXing)) {
            WuXingRelation.SAME -> if (same) "比肩" else "劫财"
            WuXingRelation.I_GENERATE -> if (same) "食神" else "伤官"
            WuXingRelation.I_OVERCOME -> if (same) "偏财" else "正财"
            WuXingRelation.IT_OVERCOMES -> if (same) "七杀" else "正官"
            WuXingRelation.IT_GENERATES -> if (same) "偏印" else "正印"
        }
    }

    /**
     * 列出某一步大运覆盖的全部流年。
     *
     * 大运起于 [DaYunPeriod.startYear]、止于 [endYear]（含），逐年给出干支与十神。
     */
    fun ofDaYun(chart: BaZiChart, period: DaYunPeriod): List<LiuNian> {
        if (period.endYear < period.startYear) return emptyList()
        return (period.startYear..period.endYear).map { year ->
            val gz = yearGanZhi(year)
            LiuNian(
                year = year,
                age = period.startAge + (year - period.startYear),
                ganZhi = gz,
                shiShen = shiShen(chart.dayMaster, gz.stem),
            )
        }
    }
}
