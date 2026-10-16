package com.maohongyuan.magic.core.model

/**
 * 八卦（经卦）。
 *
 * `bits` 用 **(初, 中, 上)** 三位二进制表示 —— **bit0 是最下面的初爻**，
 * 与「爻自下而上」的传统一致。这一点很容易写反，务必对照下面的校验：
 *
 * ```
 * 乾 ☰ 111   初阳 中阳 上阳
 * 兑 ☱ 110   初阳 中阳 上阴
 * 离 ☲ 101   初阳 中阴 上阳
 * 震 ☳ 100   初阳 中阴 上阴
 * 巽 ☴ 011   初阴 中阳 上阳
 * 坎 ☵ 010   初阴 中阳 上阴
 * 艮 ☶ 001   初阴 中阴 上阳
 * 坤 ☷ 000   初阴 中阴 上阴
 * ```
 *
 * 枚举顺序按**先天八卦**（乾一兑二离三震四巽五坎六艮七坤八），
 * 但请注意**不要**用 ordinal 做算术 —— 六十四卦的编码用的是 [bits]。
 */
enum class Trigram(
    val label: String,
    val symbol: String,
    val bits: Int,
    val wuXing: WuXing,
    /** 后天八卦方位 */
    val direction: String,
    /** 说卦传的家人卦象 */
    val family: String,
) {
    QIAN("乾", "☰", 0b111, WuXing.JIN, "西北", "父"),
    DUI("兑", "☱", 0b011, WuXing.JIN, "西", "少女"),
    LI("离", "☲", 0b101, WuXing.HUO, "南", "中女"),
    ZHEN("震", "☳", 0b001, WuXing.MU, "东", "长男"),
    XUN("巽", "☴", 0b110, WuXing.MU, "东南", "长女"),
    KAN("坎", "☵", 0b010, WuXing.SHUI, "北", "中男"),
    GEN("艮", "☶", 0b100, WuXing.TU, "东北", "少男"),
    KUN("坤", "☷", 0b000, WuXing.TU, "西南", "母"),
    ;

    /** ⚠️ 必须返回中文标签，否则枚举名（QIAN/DUI…）会漏到界面上。 */
    override fun toString(): String = label

    /** 初爻是否为阳 */
    val bottomYang: Boolean get() = (bits and 0b001) != 0

    /** 中爻是否为阳 */
    val middleYang: Boolean get() = (bits and 0b010) != 0

    /** 上爻是否为阳 */
    val topYang: Boolean get() = (bits and 0b100) != 0

    /** 由下而上的三爻阴阳 */
    val lines: List<Boolean> get() = listOf(bottomYang, middleYang, topYang)

    /**
     * 错卦（旁通）—— 三爻阴阳全变。
     * 乾↔坤、兑↔艮、离↔坎、震↔巽。
     */
    val opposite: Trigram get() = fromBits(bits xor 0b111)

    /**
     * 综卦（反卦）—— 三爻上下颠倒。
     * 震↔艮、巽↔兑；乾、坤、离、坎三爻对称，综卦即自身。
     */
    val reversed: Trigram get() = fromBits(
        (if (bottomYang) 0b100 else 0) or
            (if (middleYang) 0b010 else 0) or
            (if (topYang) 0b001 else 0)
    )

    companion object {
        /** 由三位二进制（bit0 = 初爻）取卦。 */
        fun fromBits(bits: Int): Trigram =
            entries.first { it.bits == bits }

        fun ofLabel(label: String): Trigram? = entries.firstOrNull { it.label == label }

        /**
         * 由「初、中、上」三爻阴阳取卦。
         */
        fun ofLines(bottom: Boolean, middle: Boolean, top: Boolean): Trigram =
            fromBits(
                (if (bottom) 0b001 else 0) or
                    (if (middle) 0b010 else 0) or
                    (if (top) 0b100 else 0)
            )
    }
}
