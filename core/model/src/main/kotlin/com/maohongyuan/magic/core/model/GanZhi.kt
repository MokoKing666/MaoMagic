package com.maohongyuan.magic.core.model

/**
 * 天干。
 *
 * 五行与阴阳都是**算术推导**，不是手写表：
 * - 五行 = `WuXing.entries[ordinal / 2]`（甲乙木、丙丁火、戊己土、庚辛金、壬癸水）
 * - 阴阳 = 偶数位为阳
 */
enum class HeavenlyStem(val label: String) {
    JIA("甲"), YI("乙"),
    BING("丙"), DING("丁"),
    WU("戊"), JI("己"),
    GENG("庚"), XIN("辛"),
    REN("壬"), GUI("癸"),
    ;

    /**
     * ⚠️ 必须重写 `toString()` 返回中文标签。
     *
     * 默认实现返回**枚举常量名**（`TU` / `SHUI` / `MU`…），
     * 一旦被写进字符串插值就会直接漏到界面上 —— 实测出现过
     * 「戊戌月 · TU旺」这种明显错误的展示。
     * 这些枚举在 UI 里到处被插值，统一在这里兜住，比在每个调用点加 `.label` 可靠。
     */
    override fun toString(): String = label

    val wuXing: WuXing get() = WuXing.entries[ordinal / 2]

    val yinYang: YinYang get() = if (ordinal % 2 == 0) YinYang.YANG else YinYang.YIN

    companion object {
        fun ofLabel(label: String): HeavenlyStem? = entries.firstOrNull { it.label == label }
    }
}

/**
 * 地支。
 *
 * - 阴阳 = 偶数位为阳（子寅辰午申戌为阳，丑卯巳未酉亥为阴）
 * - 五行**无算术规律**，用查表（这是传统规定，不是可推导的）
 * - 冲 / 合 / 三合**都是算术**，见各属性注释
 */
enum class EarthlyBranch(val label: String, val zodiac: String, val wuXing: WuXing) {
    ZI("子", "鼠", WuXing.SHUI),
    CHOU("丑", "牛", WuXing.TU),
    YIN("寅", "虎", WuXing.MU),
    MAO("卯", "兔", WuXing.MU),
    CHEN("辰", "龙", WuXing.TU),
    SI("巳", "蛇", WuXing.HUO),
    WU("午", "马", WuXing.HUO),
    WEI("未", "羊", WuXing.TU),
    SHEN("申", "猴", WuXing.JIN),
    YOU("酉", "鸡", WuXing.JIN),
    XU("戌", "狗", WuXing.TU),
    HAI("亥", "猪", WuXing.SHUI),
    ;

    /** ⚠️ 必须返回中文标签，否则枚举名（ZI/CHOU…）会漏到界面上。 */
    override fun toString(): String = label

    val yinYang: YinYang get() = if (ordinal % 2 == 0) YinYang.YANG else YinYang.YIN

    /**
     * 六冲。相隔六位，纯算术：`(ordinal + 6) % 12`。
     * 子午、丑未、寅申、卯酉、辰戌、巳亥。
     */
    val clash: EarthlyBranch get() = entries[(ordinal + 6) % 12]

    /**
     * 六合。`(1 − ordinal) mod 12`，对全部 12 支成立。
     * 子丑、寅亥、卯戌、辰酉、巳申、午未。
     */
    val combine: EarthlyBranch get() = entries[((1 - ordinal) % 12 + 12) % 12]

    /**
     * 六害（穿）。
     *
     * 无独立算术规律，但可由「合」与「冲」推出：**与我的六合者相冲的那个，就是害我的**。
     * 自检：子→未、丑→午、寅→巳、卯→辰、申→亥、酉→戌，与传世口诀一致。
     */
    val harm: EarthlyBranch get() = combine.clash

    /**
     * 三合局的另外两支（本支 → 下一个 → 再下一个，步长 4）。
     * 申子辰（水）、亥卯未（木）、寅午戌（火）、巳酉丑（金）。
     */
    val trinePartners: List<EarthlyBranch>
        get() = listOf(this, entries[(ordinal + 4) % 12], entries[(ordinal + 8) % 12])

    /**
     * 所属时辰的起始小时（24 小时制）。
     * 子时 23:00–01:00，丑时 01:00–03:00 …… 亥时 21:00–23:00。
     */
    val hourStart: Int get() = (ordinal * 2 + 23) % 24

    /**
     * 藏干（人元）。顺序为「本气、中气、余气」。
     *
     * 八字取用神与十神时以本气为主，中余气为辅。
     */
    val hiddenStems: List<HeavenlyStem>
        get() = when (this) {
            ZI -> listOf(HeavenlyStem.GUI)
            CHOU -> listOf(HeavenlyStem.JI, HeavenlyStem.GUI, HeavenlyStem.XIN)
            YIN -> listOf(HeavenlyStem.JIA, HeavenlyStem.BING, HeavenlyStem.WU)
            MAO -> listOf(HeavenlyStem.YI)
            CHEN -> listOf(HeavenlyStem.WU, HeavenlyStem.YI, HeavenlyStem.GUI)
            SI -> listOf(HeavenlyStem.BING, HeavenlyStem.WU, HeavenlyStem.GENG)
            WU -> listOf(HeavenlyStem.DING, HeavenlyStem.JI)
            WEI -> listOf(HeavenlyStem.JI, HeavenlyStem.DING, HeavenlyStem.YI)
            SHEN -> listOf(HeavenlyStem.GENG, HeavenlyStem.REN, HeavenlyStem.WU)
            YOU -> listOf(HeavenlyStem.XIN)
            XU -> listOf(HeavenlyStem.WU, HeavenlyStem.XIN, HeavenlyStem.DING)
            HAI -> listOf(HeavenlyStem.REN, HeavenlyStem.JIA)
        }

    companion object {
        fun ofLabel(label: String): EarthlyBranch? = entries.firstOrNull { it.label == label }
    }
}

/**
 * 干支组合 —— 六十甲子。
 *
 * `stem` 与 `branch` 的序号按 `ordinal % 10` / `ordinal % 12` 联动，
 * 因此天然满足「阳干配阳支、阴干配阴支」，不会出现甲丑这种非法组合。
 */
@JvmInline
value class GanZhi(val index: Int) {
    init {
        require(index in 0..59) { "干支序号必须在 0..59，实际 $index" }
    }

    val stem: HeavenlyStem get() = HeavenlyStem.entries[index % 10]
    val branch: EarthlyBranch get() = EarthlyBranch.entries[index % 12]

    override fun toString(): String = "${stem.label}${branch.label}"

    companion object {
        /** 以「甲子」为 0 的六十甲子总数 */
        const val CYCLE = 60

        fun of(stem: HeavenlyStem, branch: EarthlyBranch): GanZhi? {
            // 干支必须同阴阳，且同余于 60 的步长 2
            for (i in 0 until CYCLE) {
                if (HeavenlyStem.entries[i % 10] == stem && EarthlyBranch.entries[i % 12] == branch) {
                    return GanZhi(i)
                }
            }
            return null
        }
    }
}
