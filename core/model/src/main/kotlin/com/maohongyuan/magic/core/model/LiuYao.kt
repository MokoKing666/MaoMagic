package com.maohongyuan.magic.core.model

/**
 * 六亲。
 *
 * 由「爻的五行」与「卦宫五行」的关系**推导**，不是查表：
 *
 * | 关系 | 六亲 |
 * |---|---|
 * | 生我 | 父母 |
 * | 同我 | 兄弟 |
 * | 克我 | 官鬼 |
 * | 我克 | 妻财 |
 * | 我生 | 子孙 |
 */
enum class LiuQin(val label: String) {
    FU_MU("父母"),
    XIONG_DI("兄弟"),
    GUAN_GUI("官鬼"),
    QI_CAI("妻财"),
    ZI_SUN("子孙"),
    ;

    companion object {
        /**
         * 以 [palaceWuXing]（卦宫五行）为我，推导 [yaoWuXing] 的六亲。
         */
        fun of(palaceWuXing: WuXing, yaoWuXing: WuXing): LiuQin =
            when (WuXingRelation.between(palaceWuXing, yaoWuXing)) {
                WuXingRelation.IT_GENERATES -> FU_MU
                WuXingRelation.SAME -> XIONG_DI
                WuXingRelation.IT_OVERCOMES -> GUAN_GUI
                WuXingRelation.I_OVERCOME -> QI_CAI
                WuXingRelation.I_GENERATE -> ZI_SUN
            }
    }
}

/**
 * 六神（六兽）。
 *
 * 起法：按**日干**决定初爻配哪个神，然后自初爻向上依次循环。
 *
 * | 日干 | 初爻 |
 * |---|---|
 * | 甲 乙 | 青龙 |
 * | 丙 丁 | 朱雀 |
 * | 戊 | 勾陈 |
 * | 己 | 腾蛇 |
 * | 庚 辛 | 白虎 |
 * | 壬 癸 | 玄武 |
 *
 * 注意戊、己**各占一个**（不像其它天干两两一组），所以起始序号不能用 `ordinal / 2`
 * 推导，必须查表。
 */
enum class LiuShen(val label: String) {
    QING_LONG("青龙"),
    ZHU_QUE("朱雀"),
    GOU_CHEN("勾陈"),
    TENG_SHE("腾蛇"),
    BAI_HU("白虎"),
    XUAN_WU("玄武"),
    ;

    companion object {
        /** 日干 → 初爻所配六神的下标。 */
        private val START_BY_STEM = intArrayOf(
            /* 甲 */ 0, /* 乙 */ 0,
            /* 丙 */ 1, /* 丁 */ 1,
            /* 戊 */ 2,
            /* 己 */ 3,
            /* 庚 */ 4, /* 辛 */ 4,
            /* 壬 */ 5, /* 癸 */ 5,
        )

        /**
         * 取第 [yaoIndex] 爻（0 = 初爻）所配六神。
         */
        fun at(dayStem: HeavenlyStem, yaoIndex: Int): LiuShen {
            require(yaoIndex in 0..5) { "爻位必须在 0..5，实际 $yaoIndex" }
            return entries[(START_BY_STEM[dayStem.ordinal] + yaoIndex) % 6]
        }

        /** 自初爻到上爻的六个六神。 */
        fun sequence(dayStem: HeavenlyStem): List<LiuShen> =
            (0..5).map { at(dayStem, it) }
    }
}

/**
 * 爻位。
 *
 * `index` 自下而上：0 = 初爻，5 = 上爻。
 * 爻题（如「九四」「初六」）由**爻位 + 阴阳**共同决定，不是固定字符串。
 */
enum class YaoPosition(val index: Int, val ordinalLabel: String) {
    CHU(0, "初"),
    ER(1, "二"),
    SAN(2, "三"),
    SI(3, "四"),
    WU(4, "五"),
    SHANG(5, "上"),
    ;

    /**
     * 爻题。阳爻称「九」，阴爻称「六」。
     *
     * 格式规则：
     * - 初爻、上爻把「初 / 上」放前面：初九、上六
     * - 中间四爻把数字放前面：九二、六三、九四、六五
     */
    fun title(yang: Boolean): String {
        val number = if (yang) "九" else "六"
        return when (this) {
            CHU -> "$ordinalLabel$number"
            SHANG -> "$ordinalLabel$number"
            else -> "$number$ordinalLabel"
        }
    }

    companion object {
        fun of(index: Int): YaoPosition = entries[index]
    }
}
