package com.maohongyuan.magic.core.qimen

import com.maohongyuan.magic.core.calendar.CalendarService
import com.maohongyuan.magic.core.model.DivinationChart
import com.maohongyuan.magic.core.model.DivinationSystem
import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.GanZhi
import com.maohongyuan.magic.core.model.HeavenlyStem

/** 阳遁 / 阴遁 */
enum class Dun(val label: String) { YANG("阳遁"), YIN("阴遁") }

/** 三元 */
enum class Yuan(val label: String) { SHANG("上元"), ZHONG("中元"), XIA("下元") }

/**
 * 九宫。数值即洛书数，用于布仪时的顺逆序。
 *
 * 阳遁顺行 1→2→3→…→9，阴遁逆行 9→8→…→1。
 */
val JIU_GONG = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)

/** 九星，按宫序 1..9 */
val JIU_XING = listOf("天蓬", "天芮", "天冲", "天辅", "天禽", "天心", "天柱", "天任", "天英")

/** 八门。中五宫无门，用 null 占位保持宫序对齐。 */
val BA_MEN: List<String?> = listOf("休门", "死门", "伤门", "杜门", null, "开门", "惊门", "生门", "景门")

/** 八神，按起宫顺逆布 */
val BA_SHEN = listOf("值符", "螣蛇", "太阴", "六合", "白虎", "玄武", "九地", "九天")

/** 三奇六仪在九宫的排布顺序 */
val SAN_QI_LIU_YI = listOf("戊", "己", "庚", "辛", "壬", "癸", "丁", "丙", "乙")

/** 一个宫位的内容 */
data class QiMenPalace(
    val gong: Int,
    /** 地盘干（三奇六仪之一） */
    val diPan: String,
    /** 天盘干 */
    val tianPan: String,
    /** 九星 */
    val xing: String,
    /** 八门（中宫为 null） */
    val men: String?,
    /** 八神 */
    val shen: String,
)

/** 奇门盘 */
data class QiMenChart(
    val solarDate: String,
    val hourGanZhi: String,
    val jieQi: String,
    val yuan: Yuan,
    val dun: Dun,
    /** 局数 1..9 */
    val ju: Int,
    /** 旬首（甲子、甲戌…） */
    val xunShou: String,
    val palaces: List<QiMenPalace>,
) : DivinationChart {

    override val system: DivinationSystem = DivinationSystem.QI_MEN

    fun palaceOf(gong: Int): QiMenPalace? = palaces.firstOrNull { it.gong == gong }
}

/**
 * 时家奇门（转盘）。
 *
 * ── 定局表来源 ──
 * 由项目方提供的《奇门遁甲三元定局表》，24 节气 × 三元 = 72 个局数。
 * **不凭记忆编写** —— 局数错一位，整盘九宫全错且不会报错。
 */
object QiMenService {

    /**
     * 二十四节气三元定局表：`节气 -> [上元, 中元, 下元]`，附遁。
     * 冬至到芒种阳遁，夏至到大雪阴遁。
     */
    private val JU_TABLE: Map<String, Pair<Dun, List<Int>>> = mapOf(
        "冬至" to (Dun.YANG to listOf(1, 7, 4)),
        "小寒" to (Dun.YANG to listOf(2, 8, 5)),
        "大寒" to (Dun.YANG to listOf(3, 9, 6)),
        "立春" to (Dun.YANG to listOf(8, 5, 2)),
        "雨水" to (Dun.YANG to listOf(9, 6, 3)),
        "惊蛰" to (Dun.YANG to listOf(1, 7, 4)),
        "春分" to (Dun.YANG to listOf(3, 9, 6)),
        "清明" to (Dun.YANG to listOf(4, 1, 7)),
        "谷雨" to (Dun.YANG to listOf(5, 2, 8)),
        "立夏" to (Dun.YANG to listOf(4, 1, 7)),
        "小满" to (Dun.YANG to listOf(5, 2, 8)),
        "芒种" to (Dun.YANG to listOf(6, 3, 9)),
        "夏至" to (Dun.YIN to listOf(9, 3, 6)),
        "小暑" to (Dun.YIN to listOf(8, 2, 5)),
        "大暑" to (Dun.YIN to listOf(7, 1, 4)),
        "立秋" to (Dun.YIN to listOf(2, 5, 8)),
        "处暑" to (Dun.YIN to listOf(1, 4, 7)),
        "白露" to (Dun.YIN to listOf(9, 3, 6)),
        "秋分" to (Dun.YIN to listOf(7, 1, 4)),
        "寒露" to (Dun.YIN to listOf(6, 9, 3)),
        "霜降" to (Dun.YIN to listOf(5, 8, 2)),
        "立冬" to (Dun.YIN to listOf(6, 9, 3)),
        "小雪" to (Dun.YIN to listOf(5, 8, 2)),
        "大雪" to (Dun.YIN to listOf(4, 7, 1)),
    )

    /** 三元由符头地支定：子午卯酉上元、寅申巳亥中元、辰戌丑未下元 */
    private val SHANG_ZHI = setOf(EarthlyBranch.ZI, EarthlyBranch.WU, EarthlyBranch.MAO, EarthlyBranch.YOU)
    private val ZHONG_ZHI = setOf(EarthlyBranch.YIN, EarthlyBranch.SHEN, EarthlyBranch.SI, EarthlyBranch.HAI)

    /** 旬首表：六甲旬对应的仪（地盘干） */
    private val XUN_SHOU = mapOf(
        "甲子" to "戊", "甲戌" to "己", "甲申" to "庚",
        "甲午" to "辛", "甲辰" to "壬", "甲寅" to "癸",
    )

    fun chart(year: Int, month: Int, day: Int, hour: Int): QiMenChart {
        val info = CalendarService.of(year, month, day, hour)
        val hourGz = info.ganZhi.hour ?: error("取不到时柱")
        val dayGz = info.ganZhi.day ?: error("取不到日柱")

        val jieQi = currentJieQi(year, month, day)
        val yuan = yuanOf(year, month, day)
        val (dun, jus) = JU_TABLE.getValue(jieQi)
        val ju = jus[yuan.ordinal]

        // ── 地盘：局数宫起戊，阳遁顺布、阴遁逆布 ──
        val diPanMap = mutableMapOf<Int, String>()
        val startIdx = JIU_GONG.indexOf(ju)
        SAN_QI_LIU_YI.forEachIndexed { k, gan ->
            val gong = if (dun == Dun.YANG) {
                JIU_GONG[(startIdx + k) % 9]
            } else {
                JIU_GONG[(startIdx - k + 9 * 2) % 9]
            }
            diPanMap[gong] = gan
        }

        // ── 旬首与值符值使 ──
        val xunShou = xunShouOf(hourGz)
        val xunYi = XUN_SHOU.getValue(xunShou)
        // 旬首（仪）在地盘上的宫 → 该宫的星为值符，门为值使
        val xunGong = diPanMap.entries.first { it.value == xunYi }.key
        val zhiFuXing = JIU_XING[xunGong - 1]
        val zhiShiMen = BA_MEN[xunGong - 1]

        // ── 天盘：值符随时干 ──
        // 时干在地盘上的宫，就是值符星落宫
        val shiGanGong = diPanMap.entries.firstOrNull { it.value == hourGz.stem.label }?.key
            ?: xunGong   // 时干为戊己庚辛壬癸时必在盘上；若为甲乙丙丁，甲隐于旬首
        // 天盘干：值符星所在宫的天盘干 = 时干
        val tianPanMap = mutableMapOf<Int, String>()
        // 天盘九星随值符整体旋转：原 xunGong 的星移到 shiGanGong
        val starMap = mutableMapOf<Int, String>()
        JIU_XING.forEachIndexed { i, xing ->
            val fromGong = i + 1
            val offset = (fromGong - xunGong)
            val toGong = if (dun == Dun.YANG) {
                ((shiGanGong - 1 + offset + 9 * 2) % 9) + 1
            } else {
                ((shiGanGong - 1 - offset + 9 * 2) % 9) + 1
            }
            starMap[toGong] = xing
        }
        // 天盘干：地盘干随星一起转
        JIU_XING.forEachIndexed { i, _ ->
            val fromGong = i + 1
            val to = starMap.entries.first { it.value == JIU_XING[i] }.key
            tianPanMap[to] = diPanMap.getValue(fromGong)
        }

        // ── 八门：值使随时支 ──
        val hourZhiIdx = hourGz.branch.ordinal
        // 时支所在宫（子1丑8寅3卯4辰4巳9午9未2申7酉6戌6亥8）—— 用通用九宫映射
        val zhiToGong = listOf(1, 8, 3, 4, 4, 9, 9, 2, 7, 6, 6, 8)
        val targetGong = zhiToGong[hourZhiIdx]
        val menMap = mutableMapOf<Int, String?>()
        val menList = BA_MEN.filterNotNull()
        val zhiShiIdx = menList.indexOf(zhiShiMen)
        menList.forEachIndexed { k, men ->
            val offset = (k - zhiShiIdx + 8 * 2) % 8
            // 八门按八宫（去中宫）顺逆布
            val baGong = listOf(1, 8, 3, 4, 9, 2, 7, 6)
            val fromIdx = baGong.indexOf(xunGong).let { if (it < 0) 0 else it }
            val toIdx = baGong.indexOf(targetGong).let { if (it < 0) 0 else it }
            val idx = if (dun == Dun.YANG) {
                (toIdx + offset) % 8
            } else {
                (toIdx - offset + 16) % 8
            }
            menMap[baGong[idx]] = men
        }

        // ── 八神：值符起于值符星落宫，阳顺阴逆 ──
        val shenMap = mutableMapOf<Int, String>()
        val baGongOrder = listOf(1, 8, 3, 4, 9, 2, 7, 6)
        val startBa = baGongOrder.indexOf(shiGanGong).let { if (it < 0) 0 else it }
        BA_SHEN.forEachIndexed { k, shen ->
            val idx = if (dun == Dun.YANG) {
                (startBa + k) % 8
            } else {
                (startBa - k + 16) % 8
            }
            shenMap[baGongOrder[idx]] = shen
        }

        val palaces = JIU_GONG.map { gong ->
            QiMenPalace(
                gong = gong,
                diPan = diPanMap.getValue(gong),
                tianPan = tianPanMap[gong] ?: diPanMap.getValue(gong),
                xing = starMap[gong] ?: JIU_XING[gong - 1],
                men = menMap[gong],
                shen = shenMap[gong] ?: "—",
            )
        }

        return QiMenChart(
            solarDate = info.solar.ymd,
            hourGanZhi = hourGz.toString(),
            jieQi = jieQi,
            yuan = yuan,
            dun = dun,
            ju = ju,
            xunShou = xunShou,
            palaces = palaces,
        )
    }

    /** 当前节气：往回逐日扫最多 16 天找最近节气（同大六壬的取舍） */
    private fun currentJieQi(year: Int, month: Int, day: Int): String {
        val base = java.time.LocalDate.of(year, month, day)
        for (back in 0..16) {
            val d = base.minusDays(back.toLong())
            val jq = runCatching {
                CalendarService.of(d.year, d.monthValue, d.dayOfMonth, 12).jieQi
            }.getOrNull()
            if (jq != null && JU_TABLE.containsKey(jq)) return jq
        }
        return "冬至"
    }

    /**
     * 三元：找当日或之前最近的**甲日或己日**（符头），由其地支定元。
     * 子午卯酉 → 上元；寅申巳亥 → 中元；辰戌丑未 → 下元。
     */
    private fun yuanOf(year: Int, month: Int, day: Int): Yuan {
        val base = java.time.LocalDate.of(year, month, day)
        for (back in 0..9) {
            val d = base.minusDays(back.toLong())
            val gz = runCatching {
                CalendarService.of(d.year, d.monthValue, d.dayOfMonth, 12).ganZhi.day
            }.getOrNull() ?: continue
            if (gz.stem == HeavenlyStem.JIA || gz.stem == HeavenlyStem.JI) {
                return when (gz.branch) {
                    in SHANG_ZHI -> Yuan.SHANG
                    in ZHONG_ZHI -> Yuan.ZHONG
                    else -> Yuan.XIA
                }
            }
        }
        return Yuan.SHANG
    }

    /** 旬首：由时干支推所在六甲旬 */
    private fun xunShouOf(gz: GanZhi): String {
        // 六十甲子中，旬首序号 = (干支序号 / 10) * 10
        val idx = gz.index
        val xunStart = (idx / 10) * 10
        val names = listOf("甲子", "甲戌", "甲申", "甲午", "甲辰", "甲寅")
        return names[xunStart / 10]
    }
}
