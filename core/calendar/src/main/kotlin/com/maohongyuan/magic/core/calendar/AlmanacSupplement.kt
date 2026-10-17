package com.maohongyuan.magic.core.calendar

import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.HeavenlyStem

/**
 * 黄历里 lunar 有、tyme 没有的四组数据。
 *
 * 迁移到 tyme 后这四项要自己算。好在都是**确定性小表**，
 * 而且可以拿 lunar 当 oracle 逐日对拍验证（见 `AlmanacSupplementParityTest`）。
 *
 * 全部按「日干支」查，与时辰、月建无关。
 */
object AlmanacSupplement {

    private val ZODIAC = listOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")

    /**
     * 日冲。
     *
     * 冲 = 60 甲子序号 **减 6**（等价于「支进六位、干退六位即进四位」）。
     *
     * 验证：乙亥(11) → 己巳(5) ✓、丙子(12) → 庚午(6) ✓、甲子(0) → 戊午(54) ✓。
     *
     * 注意干支要按六十甲子整体位移，不能分别算干和支 ——
     * 单独算会得到「辛巳」这种不在六十甲子里对应位置的组合。
     */
    fun clash(dayGanZhiIndex: Int): ClashInfo {
        val idx = (dayGanZhiIndex - 6).mod(60)
        val stem = HeavenlyStem.entries[idx % 10].label
        val branch = EarthlyBranch.entries[idx % 12].label
        val zodiac = ZODIAC[idx % 12]
        return ClashInfo(description = "($stem$branch)$zodiac", zodiac = zodiac)
    }

    /**
     * 日煞方位。
     *
     * 按日支循环：子→南、丑→东、寅→北、卯→西，此后每四位一轮。
     * 即 `[南, 东, 北, 西][日支序号 % 4]`。
     */
    fun sha(dayBranch: EarthlyBranch): String =
        listOf("南", "东", "北", "西")[dayBranch.ordinal % 4]

    /** 彭祖百忌 · 日干 */
    private val PENG_ZU_STEM = listOf(
        "甲不开仓财物耗散",
        "乙不栽植千株不长",
        "丙不修灶必见灾殃",
        "丁不剃头头必生疮",
        "戊不受田田主不祥",
        "己不破券二比并亡",
        "庚不经络织机虚张",
        "辛不合酱主人不尝",
        "壬不泱水更难提防",
        "癸不词讼理弱敌强",
    )

    /** 彭祖百忌 · 日支 */
    private val PENG_ZU_BRANCH = listOf(
        "子不问卜自惹祸殃",
        "丑不冠带主不还乡",
        "寅不祭祀神鬼不尝",
        "卯不穿井水泉不香",
        "辰不哭泣必主重丧",
        "巳不远行财物伏藏",
        "午不苫盖屋主更张",
        "未不服药毒气入肠",
        "申不安床鬼祟入房",
        "酉不会客醉坐颠狂",
        "戌不吃犬作怪上床",
        "亥不嫁娶不利新郎",
    )

    fun pengZu(dayStem: HeavenlyStem, dayBranch: EarthlyBranch): Pair<String, String> =
        PENG_ZU_STEM[dayStem.ordinal] to PENG_ZU_BRANCH[dayBranch.ordinal]

    /**
     * 五神方位：喜神、福神、财神、阳贵、阴贵。
     *
     * 按日干查。每项是「卦位 / 方位」两段，如「艮 / 东北」。
     * 数据源为 lunar 实测输出（60 甲子日全覆盖，10 个干各出现 6 次，结果稳定）。
     */
    private val POSITIONS: Map<HeavenlyStem, List<Pair<String, String>>> = mapOf(
        HeavenlyStem.JIA to listOf(
            "艮" to "东北", "坎" to "正北", "艮" to "东北", "坤" to "西南", "艮" to "东北"),
        HeavenlyStem.YI to listOf(
            "乾" to "西北", "坤" to "西南", "艮" to "东北", "坤" to "西南", "坎" to "正北"),
        HeavenlyStem.BING to listOf(
            "坤" to "西南", "乾" to "西北", "坤" to "西南", "兑" to "正西", "乾" to "西北"),
        HeavenlyStem.DING to listOf(
            "离" to "正南", "巽" to "东南", "坤" to "西南", "乾" to "西北", "兑" to "正西"),
        HeavenlyStem.WU to listOf(
            "巽" to "东南", "艮" to "东北", "坎" to "正北", "艮" to "东北", "坤" to "西南"),
        HeavenlyStem.JI to listOf(
            "艮" to "东北", "坎" to "正北", "坎" to "正北", "坎" to "正北", "坤" to "西南"),
        HeavenlyStem.GENG to listOf(
            "乾" to "西北", "坤" to "西南", "震" to "正东", "离" to "正南", "艮" to "东北"),
        HeavenlyStem.XIN to listOf(
            "坤" to "西南", "乾" to "西北", "震" to "正东", "艮" to "东北", "离" to "正南"),
        HeavenlyStem.REN to listOf(
            "离" to "正南", "巽" to "东南", "离" to "正南", "震" to "正东", "巽" to "东南"),
        HeavenlyStem.GUI to listOf(
            "巽" to "东南", "艮" to "东北", "离" to "正南", "巽" to "东南", "震" to "正东"),
    )

    private val POSITION_NAMES = listOf("喜神", "福神", "财神", "阳贵神", "阴贵神")

    fun positions(dayStem: HeavenlyStem): List<PositionInfo> =
        POSITIONS.getValue(dayStem).mapIndexed { i, (gua, dir) ->
            PositionInfo(POSITION_NAMES[i], gua, dir)
        }
    /**
     * 二十八宿吉凶。
     *
     * tyme 只给宿名，不给吉凶 —— 这张表 lunar 有，我们照抄（28 项，逐日对拍验证）。
     *
     * 二十八宿按圆序排列：角亢氐房心尾箕斗牛女虚危室壁奎娄胃昴毕觜参井鬼柳星张翼轸。
     */
    private val XIU_LUCK: Map<String, String> = mapOf(
        "角" to "吉", "亢" to "凶", "氐" to "凶", "房" to "吉",
        "心" to "凶", "尾" to "吉", "箕" to "吉", "斗" to "吉",
        "牛" to "凶", "女" to "凶", "虚" to "凶", "危" to "凶",
        "室" to "吉", "壁" to "吉", "奎" to "凶", "娄" to "吉",
        "胃" to "吉", "昴" to "凶", "毕" to "吉", "觜" to "凶",
        "参" to "吉", "井" to "吉", "鬼" to "凶", "柳" to "凶",
        "星" to "凶", "张" to "吉", "翼" to "凶", "轸" to "吉",
    )

    fun xiuLuck(xiuName: String): String = XIU_LUCK[xiuName] ?: ""

    /** 生肖。按地支取，**不要用 tyme 的 getTwenty()** —— 那是三元九运，不是生肖。 */
    fun zodiac(branch: EarthlyBranch): String = ZODIAC[branch.ordinal]

    /** 农历月名。1..12 → 正/二/…/十/冬/腊。闰月由调用方加「闰」字。 */
    private val MONTH_NAMES = listOf(
        "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊",
    )

    fun monthName(month: Int): String = MONTH_NAMES.getOrElse(month - 1) { "$month" }

    /** 农历日名。初一…初十、十一…十九、二十、廿一…廿九、三十。 */
    private val DAY_NAMES = listOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十",
    )

    fun dayName(day: Int): String = DAY_NAMES.getOrElse(day - 1) { "$day" }
}

/** 日冲：描述串（如「(己巳)蛇」）与生肖（如「蛇」） */
data class ClashInfo(val description: String, val zodiac: String)