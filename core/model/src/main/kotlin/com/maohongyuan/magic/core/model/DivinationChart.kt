package com.maohongyuan.magic.core.model

/**
 * 术数系统标识。
 *
 * 每个系统一个常量 —— 用于 UI 展示与解卦提示词的分派。
 */
enum class DivinationSystem(val label: String) {
    LIU_YAO("六爻"),
    MEI_HUA("梅花易数"),
    BA_ZI("八字"),
    XIAO_LIU_REN("小六壬"),
    TAROT("塔罗"),
    MBTI("十六型"),
    DAILY_FORTUNE("日运"),
    MONTHLY_FORTUNE("月运"),
    ZI_WEI("紫微斗数"),
    DA_LIU_REN("大六壬"),
    QI_MEN("奇门遁甲"),
    ;

    /** ⚠️ 必须返回中文标签，否则枚举名会漏到界面上。 */
    override fun toString(): String = label
}

/**
 * 排盘结果的统一标记接口。
 *
 * ── 为什么是普通 interface 而不是 sealed ──
 * `sealed` 要求所有实现与接口**同模块同包**，而各盘分散在不同模块：
 * `LiuYaoChart`/`MeihuaChart` 在 `core:hexagram`、`BaZiChart` 在 `core:calendar`、
 * 紫微在 `core:ziwei`……跨模块无法 sealed。因此用普通接口，
 * 分派处（如 `ChartPrompt`）的 `when` 必须带 `else` 分支。
 *
 * ── 为什么放在 core:model ──
 * 这是零依赖的纯 JVM 模块，所有系统模块都已（直接或间接）依赖它。
 * 放在 `core:interpret` 会让后者反向依赖所有系统模块，变成上帝模块。
 *
 * ── 约束 ──
 * 实现必须是**不可变数据类**，且**禁止出现任何 android.* 导入** ——
 * 这条链上的所有模块都是纯 JVM，单测要能脱离模拟器跑。
 */
interface DivinationChart {
    val system: DivinationSystem
}
