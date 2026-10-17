package com.maohongyuan.magic.navigation

import com.maohongyuan.magic.designsystem.glass.MagicTabIcon

/**
 * 底部导航的四个板块。
 *
 * ── 为什么从「一个系统一个 tab」改成「板块」 ──
 * 系统要扩到 12 个，底栏是**均分单行胶囊**（[com.maohongyuan.magic.designsystem.glass.LiquidBottomTabs]
 * 按 `(maxWidth - 8dp) / tabsCount` 均分），12 个每个只剩约 32dp，图标 22dp 会挤爆。
 *
 * 而 `LiquidBottomTabs` 的气泡拖拽与速度拉伸逻辑有大量踩坑注释
 * （`remember(selectedTabIndex)` 陷阱、嵌套 `layerBackdrop` 会 SIGSEGV），
 * **不动它**，改为只改调用层：底栏固定 4 个板块，板块页内用卡片列出系统。
 */
enum class AppTab(val label: String, val icon: MagicTabIcon) {
    TRADITIONAL("传统", MagicTabIcon.BOARD_TRADITIONAL),
    MODERN("现代", MagicTabIcon.BOARD_MODERN),
    DAILY("日常", MagicTabIcon.BOARD_DAILY),
    SETTINGS("设置", MagicTabIcon.SETTINGS),
    ;

    /** 板块页的分组标题；null = 该板块不分组 */
    val groups: List<String>? get() = when (this) {
        TRADITIONAL -> listOf("命理", "占卜")
        else -> null
    }
}

/**
 * 全部术数系统的**唯一注册表**。
 *
 * 新增系统只改这里 + [systemScreen] 的分派 —— 底栏、板块页、标题都从这里取，
 * 不会再出现「加了一处漏了另一处」。
 */
enum class SystemId(
    val title: String,
    val subtitle: String,
    val icon: MagicTabIcon,
    val board: AppTab,
    /** 板块页里的分组归属；null = 不分组 */
    val group: String? = null,
) {
    // ── 传统 · 命理 ──
    BAZI("八字", "四柱命盘", MagicTabIcon.SYS_BAZI, AppTab.TRADITIONAL, "命理"),
    ZIWEI("紫微斗数", "十二宫星曜", MagicTabIcon.SYS_ZIWEI, AppTab.TRADITIONAL, "命理"),

    // ── 传统 · 占卜 ──
    LIUYAO("六爻", "铜钱起卦", MagicTabIcon.SYS_LIUYAO, AppTab.TRADITIONAL, "占卜"),
    MEIHUA("梅花易数", "体用互变", MagicTabIcon.SYS_MEIHUA, AppTab.TRADITIONAL, "占卜"),
    XIAOLIUREN("小六壬", "六宫速断", MagicTabIcon.SYS_XIAOLIUREN, AppTab.TRADITIONAL, "占卜"),
    DALIUREN("大六壬", "四课三传", MagicTabIcon.SYS_DALIUREN, AppTab.TRADITIONAL, "占卜"),
    QIMEN("奇门遁甲", "九宫八门", MagicTabIcon.SYS_QIMEN, AppTab.TRADITIONAL, "占卜"),

    // ── 现代 ──
    TAROT("塔罗", "抽牌解牌", MagicTabIcon.SYS_TAROT, AppTab.MODERN),
    // ⚠️ 不叫 MBTI —— 那是 The Myers-Briggs Company 的注册商标。
    MBTI("十六型人格", "荣格类型", MagicTabIcon.SYS_MBTI, AppTab.MODERN),

    // ── 日常 ──
    ALMANAC("黄历", "今日宜忌", MagicTabIcon.SYS_ALMANAC, AppTab.DAILY),
    DAILY_FORTUNE("日运", "流日十神", MagicTabIcon.SYS_DAILY, AppTab.DAILY),
    MONTHLY_FORTUNE("月运", "流月十神", MagicTabIcon.SYS_MONTHLY, AppTab.DAILY),
    ;

    /**
     * 是否已经能用。
     *
     * 未实现的系统在板块页仍然显示卡片（让用户看到全貌），但点击时给出提示，
     * 而不是进到一个空白页 —— 空白页会让人以为应用坏了。
     */
    val available: Boolean get() = when (this) {
        BAZI, LIUYAO, MEIHUA, ALMANAC, XIAOLIUREN, TAROT, MBTI, ZIWEI, DALIUREN,
        DAILY_FORTUNE, MONTHLY_FORTUNE -> true
        else -> false
    }

    companion object {
        /** 按板块分组取出系统；[group] 为 null 时返回该板块全部 */
        fun of(board: AppTab, group: String? = null): List<SystemId> =
            entries.filter { it.board == board && (group == null || it.group == group) }
    }
}
