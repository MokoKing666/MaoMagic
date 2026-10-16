package com.maohongyuan.magic.core.model

/**
 * 五行。
 *
 * ⚠️ **枚举顺序不可改** —— 相生相克关系是用序号算术推导的，
 * 不是手写查表。手写 5×5 的关系表必然出错，算术则天然自洽。
 *
 * ```
 * 相生：木 → 火 → 土 → 金 → 水 → 木   （序号 +1 mod 5）
 * 相克：木 → 土 → 水 → 火 → 金 → 木   （序号 +2 mod 5）
 * ```
 *
 * 两条关系在 mod 5 下分别是步长 1 与步长 2，因此只要顺序正确，
 * 20 条关系全部自动成立，且 `A.generates.generatedBy == A` 这类往返恒等式必定成立。
 * 见 `WuXingTest` 的穷尽验证。
 */
enum class WuXing(val label: String) {
    MU("木"),
    HUO("火"),
    TU("土"),
    JIN("金"),
    SHUI("水"),
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

    /** 我生者（我泄气于它） */
    val generates: WuXing get() = entries[(ordinal + 1) % 5]

    /** 生我者（它生我，为我的印） */
    val generatedBy: WuXing get() = entries[(ordinal + 4) % 5]

    /** 我克者（我克它为财） */
    val overcomes: WuXing get() = entries[(ordinal + 2) % 5]

    /** 克我者（它克我为官鬼） */
    val overcomeBy: WuXing get() = entries[(ordinal + 3) % 5]

    companion object {
        fun ofLabel(label: String): WuXing? = entries.firstOrNull { it.label == label }
    }
}

/** 阴阳 */
enum class YinYang(val label: String) {
    YANG("阳"),
    YIN("阴"),
    ;

    /** ⚠️ 必须返回中文标签，否则枚举名（YANG/YIN）会漏到界面上。 */
    override fun toString(): String = label

    val opposite: YinYang get() = if (this == YANG) YIN else YANG
}

/**
 * 五行对另一个五行的关系 —— 六亲与体用生克都建立在这上面。
 */
enum class WuXingRelation(val label: String) {
    /** 同我 */
    SAME("比和"),

    /** 我生它 */
    I_GENERATE("我生"),

    /** 它生我 */
    IT_GENERATES("生我"),

    /** 我克它 */
    I_OVERCOME("我克"),

    /** 它克我 */
    IT_OVERCOMES("克我"),
    ;

    /** ⚠️ 必须返回中文标签，否则枚举名（SAME/I_GENERATE…）会漏到界面上。 */
    override fun toString(): String = label

    companion object {
        /** 以 [self] 为我，判断 [other] 与我的关系。 */
        fun between(self: WuXing, other: WuXing): WuXingRelation = when {
            self == other -> SAME
            self.generates == other -> I_GENERATE
            self.generatedBy == other -> IT_GENERATES
            self.overcomes == other -> I_OVERCOME
            self.overcomeBy == other -> IT_OVERCOMES
            else -> error("五行关系不可达：$self / $other")
        }
    }
}
