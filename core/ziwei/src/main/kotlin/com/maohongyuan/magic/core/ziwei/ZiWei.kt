package com.maohongyuan.magic.core.ziwei

import com.maohongyuan.magic.core.calendar.BaZiService
import com.maohongyuan.magic.core.calendar.CalendarService
import com.maohongyuan.magic.core.calendar.Gender
import com.maohongyuan.magic.core.model.DivinationChart
import com.maohongyuan.magic.core.model.DivinationSystem
import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.GanZhi
import com.maohongyuan.magic.core.model.HeavenlyStem

/** 五行局。数值就是局数，起紫微时要用。 */
enum class WuXingJu(val ju: Int, val label: String) {
    SHUI(2, "水二局"),
    MU(3, "木三局"),
    JIN(4, "金四局"),
    TU(5, "土五局"),
    HUO(6, "火六局"),
}

/** 十二宫的宫名，顺序为「命宫起逆排」 */
enum class PalaceName(val label: String) {
    MING("命宫"), XIONG_DI("兄弟"), FU_QI("夫妻"), ZI_NV("子女"),
    CAI_BO("财帛"), JI_E("疾厄"), QIAN_YI("迁移"), JIAO_YOU("交友"),
    GUAN_LU("官禄"), TIAN_ZHAI("田宅"), FU_DE("福德"), FU_MU("父母"),
}

/** 一颗星落在一宫 */
data class StarPlacement(
    val name: String,
    /** 四化：禄 / 权 / 科 / 忌；无则为 null */
    val hua: String? = null,
)

/** 一个宫位 */
data class Palace(
    val index: Int,
    val branch: EarthlyBranch,
    val name: PalaceName,
    val stars: List<StarPlacement> = emptyList(),
) {
    val starNames: List<String> get() = stars.map { it.name }
}

/** 紫微斗数盘 */
data class ZiWeiChart(
    val solarDate: String,
    val lunarMonth: Int,
    val lunarDay: Int,
    val hourBranch: EarthlyBranch,
    val yearGan: HeavenlyStem,
    val wuXingJu: WuXingJu,
    /** 命宫所在的地支 */
    val mingPalaceBranch: EarthlyBranch,
    /** 身宫所在的地支 */
    val shenPalaceBranch: EarthlyBranch,
    /** 紫微星所在地支 */
    val ziWeiBranch: EarthlyBranch,
    /** 十二宫，按地支子丑寅…卯的顺序排（便于画盘），每宫带宫名 */
    val palaces: List<Palace>,
) : DivinationChart {

    override val system: DivinationSystem = DivinationSystem.ZI_WEI

    fun palaceOf(name: PalaceName): Palace? = palaces.firstOrNull { it.name == name }

    /** 主星总数 —— 自检用 */
    val mainStarCount: Int get() = palaces.sumOf { it.stars.size }
}

/**
 * 紫微斗数排盘。
 *
 * ── 关于「标准派」──
 * 安星诀各家略有出入（尤其辅星）。本实现采用**最通行的一套**：
 * 命宫由寅起正月顺数、再自该宫起子时逆数；紫微由五行局与农历日定；
 * 紫微系逆布、天府系顺布。
 *
 * 只做 **14 主星**（紫微、天机、太阳、武曲、天同、廉贞 / 天府、太阴、贪狼、
 * 巨门、天相、天梁、七杀、破军）与主星四化。辅星、杂曜、大限流年未做。
 */
object ZiWeiService {

    /** 寅宫在十二支里的序号（子=0） */
    private const val YIN = 2

    /**
     * 三十组纳音的五行，按六十甲子「每两个一组」的顺序。
     * 索引 = 干支序号 / 2。用于由命宫干支推五行局。
     */
    private const val NA_YIN_WU_XING = "金火木土金火水土金木水土火木水金火木土金火水土金木水土火木水"

    fun chart(
        year: Int, month: Int, day: Int, hour: Int,
        minute: Int = 0,
        gender: Gender = Gender.MALE,
    ): ZiWeiChart {
        val bazi = BaZiService.chart(year, month, day, hour, minute, gender)
        val info = CalendarService.of(year, month, day, hour)
        val lunarMonth = info.lunar.month
        val lunarDay = info.lunar.day
        val yearGan = bazi.pillars[0].gan
        val hourBranch = bazi.pillars[3].zhi

        // ── 命宫 / 身宫 ──
        // 自寅起正月，顺数至生月 → 得月宫；再自月宫起子时，
        // 逆数至生时 → 命宫；顺数 → 身宫。
        val monthPalace = (YIN + (lunarMonth - 1)).mod(12)
        val mingIdx = (monthPalace - hourBranch.ordinal).mod(12)
        val shenIdx = (monthPalace + hourBranch.ordinal).mod(12)

        // ── 五行局：命宫的纳音五行决定 ──
        val wuXingJu = wuXingJuOf(yearGan, mingIdx)

        // ── 起紫微 ──
        val ziWeiIdx = ziWeiIndex(wuXingJu.ju, lunarDay)

        // ── 安十四主星 ──
        val stars = mutableMapOf<Int, MutableList<StarPlacement>>()
        fun place(index: Int, name: String) {
            stars.getOrPut(index.mod(12)) { mutableListOf() } += StarPlacement(name)
        }

        // 紫微系（逆布）
        place(ziWeiIdx, "紫微")
        place(ziWeiIdx - 1, "天机")
        place(ziWeiIdx - 3, "太阳")
        place(ziWeiIdx - 4, "武曲")
        place(ziWeiIdx - 5, "天同")
        place(ziWeiIdx - 8, "廉贞")

        // 天府系（顺布）。天府与紫微关于寅申轴对称。
        val tianFuIdx = (2 * YIN - ziWeiIdx).mod(12)
        place(tianFuIdx, "天府")
        place(tianFuIdx + 1, "太阴")
        place(tianFuIdx + 2, "贪狼")
        place(tianFuIdx + 3, "巨门")
        place(tianFuIdx + 4, "天相")
        place(tianFuIdx + 5, "天梁")
        place(tianFuIdx + 6, "七杀")
        place(tianFuIdx + 10, "破军")

        // ── 四化（生年天干）──
        val huaMap = SI_HUA[yearGan] ?: emptyMap()
        val withHua = stars.mapValues { (_, list) ->
            list.map { sp -> sp.copy(hua = huaMap[sp.name]) }
        }

        // ── 十二宫：命宫起逆排 ──
        val palaces = (0 until 12).map { branchIdx ->
            val offset = (branchIdx - mingIdx).mod(12)
            // 命宫 offset=0；逆排意味着地支递增方向对应宫序递减
            val nameIdx = (12 - offset).mod(12)
            Palace(
                index = branchIdx,
                branch = EarthlyBranch.entries[branchIdx],
                name = PalaceName.entries[nameIdx],
                stars = (withHua[branchIdx] ?: emptyList()).sortedBy { MAIN_STAR_ORDER.indexOf(it.name) },
            )
        }

        return ZiWeiChart(
            solarDate = info.solar.ymd,
            lunarMonth = lunarMonth,
            lunarDay = lunarDay,
            hourBranch = hourBranch,
            yearGan = yearGan,
            wuXingJu = wuXingJu,
            mingPalaceBranch = EarthlyBranch.entries[mingIdx],
            shenPalaceBranch = EarthlyBranch.entries[shenIdx],
            ziWeiBranch = EarthlyBranch.entries[ziWeiIdx.mod(12)],
            palaces = palaces,
        )
    }

    /**
     * 起紫微。
     *
     * ```
     * 商 q = ceil(日 / 局数)
     * 余 r = q * 局数 - 日
     * 紫微 = 寅 + (q - 1) ± r      （r 为偶取 +，为奇取 -）
     * ```
     * 自检：水二局初一 → 丑、初二 → 寅、初三 → 寅、初四 → 卯，与传世星表一致。
     */
    fun ziWeiIndex(ju: Int, lunarDay: Int): Int {
        require(ju in 2..6) { "局数必须在 2..6" }
        require(lunarDay in 1..30) { "农历日必须在 1..30" }
        val q = (lunarDay + ju - 1) / ju
        val r = q * ju - lunarDay
        val offset = if (r % 2 == 0) r else -r
        return (YIN + (q - 1) + offset).mod(12)
    }

    /**
     * 五行局：由命宫的干支纳音决定。
     *
     * 命宫的天干要用**五虎遁**从生年天干推：
     * 甲己之年丙作首、乙庚之岁戊为头、丙辛必定寻庚起、丁壬壬位顺行流、戊癸甲寅好追求。
     */
    fun wuXingJuOf(yearGan: HeavenlyStem, mingIdx: Int): WuXingJu {
        // 寅宫的天干
        val yinGanOrdinal = when (yearGan.ordinal % 5) {
            0 -> 2   // 甲己 → 丙
            1 -> 4   // 乙庚 → 戊
            2 -> 6   // 丙辛 → 庚
            3 -> 8   // 丁壬 → 壬
            else -> 0 // 戊癸 → 甲
        }
        val ganAtMing = HeavenlyStem.entries[(yinGanOrdinal + (mingIdx - YIN)).mod(10)]
        val ganZhi = GanZhi.of(ganAtMing, EarthlyBranch.entries[mingIdx])
            ?: error("命宫干支不成立：${ganAtMing.label}${EarthlyBranch.entries[mingIdx].label}")
        val naYin = NA_YIN_WU_XING[ganZhi.index / 2]
        return when (naYin) {
            '水' -> WuXingJu.SHUI
            '木' -> WuXingJu.MU
            '金' -> WuXingJu.JIN
            '土' -> WuXingJu.TU
            '火' -> WuXingJu.HUO
            else -> error("未知纳音五行：$naYin")
        }
    }

    /** 十四主星在界面上的显示顺序（紫微系在前） */
    val MAIN_STAR_ORDER = listOf(
        "紫微", "天机", "太阳", "武曲", "天同", "廉贞",
        "天府", "太阴", "贪狼", "巨门", "天相", "天梁", "七杀", "破军",
    )

    /**
     * 生年四化。
     *
     * 注意：戊、己、辛、壬 四干各有一化落在**辅星**（右弼、文曲、文昌、左辅），
     * 本版未安辅星，因此那四化不会出现在盘上 —— 这是已知的缺口，不是错漏。
     */
    private val SI_HUA: Map<HeavenlyStem, Map<String, String>> = mapOf(
        HeavenlyStem.JIA to mapOf("廉贞" to "禄", "破军" to "权", "武曲" to "科", "太阳" to "忌"),
        HeavenlyStem.YI to mapOf("天机" to "禄", "天梁" to "权", "紫微" to "科", "太阴" to "忌"),
        HeavenlyStem.BING to mapOf("天同" to "禄", "天机" to "权", "文昌" to "科", "廉贞" to "忌"),
        HeavenlyStem.DING to mapOf("太阴" to "禄", "天同" to "权", "天机" to "科", "巨门" to "忌"),
        HeavenlyStem.WU to mapOf("贪狼" to "禄", "太阴" to "权", "右弼" to "科", "天机" to "忌"),
        HeavenlyStem.JI to mapOf("武曲" to "禄", "贪狼" to "权", "天梁" to "科", "文曲" to "忌"),
        HeavenlyStem.GENG to mapOf("太阳" to "禄", "武曲" to "权", "太阴" to "科", "天同" to "忌"),
        HeavenlyStem.XIN to mapOf("巨门" to "禄", "太阳" to "权", "文曲" to "科", "文昌" to "忌"),
        HeavenlyStem.REN to mapOf("天梁" to "禄", "紫微" to "权", "左辅" to "科", "武曲" to "忌"),
        HeavenlyStem.GUI to mapOf("破军" to "禄", "巨门" to "权", "太阴" to "科", "贪狼" to "忌"),
    )
}
