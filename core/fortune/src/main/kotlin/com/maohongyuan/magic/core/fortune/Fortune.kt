package com.maohongyuan.magic.core.fortune

import com.maohongyuan.magic.core.calendar.BaZiService
import com.maohongyuan.magic.core.calendar.CalendarService
import com.maohongyuan.magic.core.calendar.Gender
import com.maohongyuan.magic.core.calendar.LiuNianService
import com.maohongyuan.magic.core.model.DivinationChart
import com.maohongyuan.magic.core.model.DivinationSystem
import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.GanZhi
import com.maohongyuan.magic.core.model.HeavenlyStem
import kotlinx.serialization.Serializable

/**
 * 命主档案。
 *
 * 放在本模块而不是 core:data —— 后者是 Android 库（DataStore/Keystore），
 * 而纯 JVM 的 fortune 也要用它。Store 在 core:data，数据类在这里。
 */
@Serializable
data class BirthProfile(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int = 0,
    val gender: String = "MALE",
) {
    val genderEnum: Gender get() = if (gender == "FEMALE") Gender.FEMALE else Gender.MALE
}

/** 日运 / 月运 */
enum class FortunePeriod(val label: String) {
    DAILY("日运"),
    MONTHLY("月运"),
}

/** 四个维度 */
enum class FortuneDimension(val label: String) {
    CAREER("事业"),
    WEALTH("财运"),
    LOVE("感情"),
    HEALTH("健康"),
}

/** 某维度的评分 */
data class FortuneScore(
    val dimension: FortuneDimension,
    /** 0..100，50 为平 */
    val score: Int,
    /** 一句话说明为什么是这个分 */
    val reason: String,
) {
    val level: String
        get() = when {
            score >= 70 -> "顺"
            score >= 55 -> "偏顺"
            score >= 45 -> "平"
            score >= 30 -> "偏滞"
            else -> "滞"
        }
}

/** 日运/月运盘 */
data class FortuneChart(
    val period: FortunePeriod,
    /** 展示用的时间标签，如「2026-10-17」或「2026年10月」 */
    val dateLabel: String,
    val dayMaster: HeavenlyStem,
    val targetGanZhi: GanZhi,
    val shiShen: String,
    val scores: List<FortuneScore>,
    /** 流日/流月地支与命局四支的冲合害 */
    val relations: List<String>,
) : DivinationChart {

    override val system: DivinationSystem
        get() = if (period == FortunePeriod.DAILY) {
            DivinationSystem.DAILY_FORTUNE
        } else {
            DivinationSystem.MONTHLY_FORTUNE
        }
}

/**
 * 日运 / 月运的推算。
 *
 * ── 数据来源 ──
 * 流日/流月干支直接用 `lunar` 库算好的结果（`CalendarService.of(...).ganZhi`），
 * **不手写五虎遁/日柱递推** —— 那是历法层已经解决的问题。
 *
 * ── 十神 → 四维的映射是启发式的 ──
 * 这不是传统定论，是把「十神对日主意味着什么」映射到现代人关心的四个方面的
 * 一种工程化处理。**所以 UI 上必须说明它只是参考，不是断语。**
 */
object FortuneService {

    fun daily(profile: BirthProfile, year: Int, month: Int, day: Int): FortuneChart {
        val chart = BaZiService.chart(
            year = profile.year, month = profile.month, day = profile.day,
            hour = profile.hour, minute = profile.minute, gender = profile.genderEnum,
        )
        val today = CalendarService.of(year, month, day, profile.hour)
        val gz = today.ganZhi.day ?: error("取不到流日干支")
        return build(
            period = FortunePeriod.DAILY,
            label = today.solar.ymd,
            chart = chart,
            target = gz,
            targetBranch = gz.branch,
        )
    }

    fun monthly(profile: BirthProfile, year: Int, month: Int): FortuneChart {
        val chart = BaZiService.chart(
            year = profile.year, month = profile.month, day = profile.day,
            hour = profile.hour, minute = profile.minute, gender = profile.genderEnum,
        )
        // 取该月中旬的月柱 —— 月柱在整月内不变，取哪一天都一样
        val mid = CalendarService.of(year, month, 15, profile.hour)
        val gz = mid.ganZhi.month ?: error("取不到流月干支")
        return build(
            period = FortunePeriod.MONTHLY,
            label = "${year}年${month}月",
            chart = chart,
            target = gz,
            targetBranch = gz.branch,
        )
    }

    private fun build(
        period: FortunePeriod,
        label: String,
        chart: com.maohongyuan.magic.core.calendar.BaZiChart,
        target: GanZhi,
        targetBranch: EarthlyBranch,
    ): FortuneChart {
        val dayMaster = chart.dayMaster
        val shiShen = LiuNianService.shiShen(dayMaster, target.stem)

        // 基础分 50，按十神加减
        val deltas = mutableMapOf(
            FortuneDimension.CAREER to 0,
            FortuneDimension.WEALTH to 0,
            FortuneDimension.LOVE to 0,
            FortuneDimension.HEALTH to 0,
        )
        val reasons = mutableMapOf<FortuneDimension, String>()

        fun add(d: FortuneDimension, delta: Int, why: String) {
            deltas[d] = (deltas[d] ?: 0) + delta
            reasons[d] = if (reasons[d].isNullOrBlank()) why else "${reasons[d]}；$why"
        }

        when (shiShen) {
            "比肩", "劫财" -> {
                add(FortuneDimension.HEALTH, 6, "$shiShen 帮身，精力足")
                add(FortuneDimension.WEALTH, -8, "$shiShen 争财，易破耗")
                add(FortuneDimension.CAREER, -3, "$shiShen 同侪相争，助力与阻力并存")
            }
            "食神", "伤官" -> {
                add(FortuneDimension.LOVE, 8, "$shiShen 主表达，人际顺畅")
                add(FortuneDimension.CAREER, -4, "$shiShen 泄气，宜出力不宜争位")
                add(FortuneDimension.HEALTH, 3, "$shiShen 泄秀，心情舒畅")
            }
            "偏财", "正财" -> {
                add(FortuneDimension.WEALTH, 12, "$shiShen 当值，财路显")
                add(FortuneDimension.CAREER, 4, "$shiShen 务实，事有着落")
                add(FortuneDimension.HEALTH, -3, "$shiShen 耗身，宜节劳")
            }
            "七杀", "正官" -> {
                add(FortuneDimension.CAREER, 12, "$shiShen 当值，事权在握")
                add(FortuneDimension.HEALTH, -6, "$shiShen 克身，压力偏大")
                add(FortuneDimension.LOVE, -2, "$shiShen 严整，情感上偏紧")
            }
            "偏印", "正印" -> {
                add(FortuneDimension.HEALTH, 8, "$shiShen 生身，宜调养")
                add(FortuneDimension.CAREER, 5, "$shiShen 主学，宜进修")
                add(FortuneDimension.WEALTH, -5, "$shiShen 印重财轻，进财慢")
            }
        }

        // ── 流日/流月支与命局四支的冲合害 ──
        val relations = mutableListOf<String>()
        chart.pillars.forEach { p ->
            when {
                p.zhi == targetBranch.clash -> {
                    relations += "与${p.label}（${p.zhi.label}）相冲"
                    add(FortuneDimension.HEALTH, -5, "冲${p.label}")
                    add(FortuneDimension.LOVE, -4, "冲${p.label}")
                }
                p.zhi == targetBranch.combine -> {
                    relations += "与${p.label}（${p.zhi.label}）相合"
                    add(FortuneDimension.LOVE, 5, "合${p.label}")
                    add(FortuneDimension.CAREER, 3, "合${p.label}")
                }
                p.zhi == targetBranch.harm -> {
                    relations += "与${p.label}（${p.zhi.label}）相害"
                    add(FortuneDimension.HEALTH, -3, "害${p.label}")
                }
            }
        }

        val scores = FortuneDimension.entries.map { d ->
            FortuneScore(
                dimension = d,
                score = (50 + (deltas[d] ?: 0)).coerceIn(5, 95),
                reason = reasons[d].orEmpty().ifBlank { "无明显生克，平顺" },
            )
        }

        return FortuneChart(
            period = period,
            dateLabel = label,
            dayMaster = dayMaster,
            targetGanZhi = target,
            shiShen = shiShen,
            scores = scores,
            relations = relations,
        )
    }
}
