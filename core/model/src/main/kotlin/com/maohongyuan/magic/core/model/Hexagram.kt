package com.maohongyuan.magic.core.model

/**
 * 六十四卦。
 *
 * ── 编码 ──
 * `code = (upper.bits shl 3) or lower.bits`，取值 0..63。
 * 因为 [Trigram.bits] 的 bit0 是初爻，所以 code 的 **bit0 就是初爻**，
 * `code ushr 3` 是上卦、`code and 0b111` 是下卦。
 *
 * 例：天风姤 = 上乾(0b111) 下巽(0b110) ⇒ `0b111110` = 62。
 *
 * ── 为什么名字必须查表 ──
 * 卦名是**约定**（「天风姤」这个名字推不出来），属于数据；
 * 而**宫位、世应、纳甲、六亲**全部是规则，必须算法生成 —— 见 `core:hexagram`。
 * 本项目严格区分这两者：本文件只有名字与序卦传序号，不含任何术数规则。
 *
 * ── 序卦传序号 ──
 * Unicode 卦符区 `U+4DC0..U+4DFF` 是按**序卦传**排序的，不是二进制序。
 * 所以 `symbol` 必须由 [kingWen] 换算，不能直接用 code。
 */
class Hexagram private constructor(
    /** 二进制编码 0..63，bit0 = 初爻 */
    val code: Int,
    val name: String,
    /** 序卦传序号 1..64 */
    val kingWen: Int,
) {
    /** 上卦（外卦），即第四、五、六爻 */
    val upper: Trigram get() = Trigram.fromBits(code ushr 3)

    /** 下卦（内卦），即初、二、三爻 */
    val lower: Trigram get() = Trigram.fromBits(code and 0b111)

    /** 六爻阴阳，**自下而上**（初爻在 index 0） */
    val lines: List<Boolean> get() = (0 until 6).map { (code shr it) and 1 == 1 }

    /** 由上卦下卦拼出的完整卦名，如「天风姤」 */
    val fullName: String get() = name

    /** Unicode 卦符（U+4DC0 起，按序卦传排序） */
    val symbol: String get() = (0x4DC0 + kingWen - 1).toChar().toString()

    /**
     * 错卦（旁通）—— 六爻阴阳全变。
     */
    val opposite: Hexagram get() = of(code xor 0b111111)

    /**
     * 综卦（反卦）—— 六爻上下颠倒。
     */
    val reversed: Hexagram get() = of(
        (0 until 6).fold(0) { acc, i ->
            val bit = (code shr (5 - i)) and 1
            acc or (bit shl i)
        }
    )

    /**
     * 互卦 —— 取二三四爻为下卦、三四五爻为上卦。
     */
    val nuclear: Hexagram get() {
        val l = lines
        val lowerBits = (if (l[1]) 1 else 0) or (if (l[2]) 2 else 0) or (if (l[3]) 4 else 0)
        val upperBits = (if (l[2]) 1 else 0) or (if (l[3]) 2 else 0) or (if (l[4]) 4 else 0)
        return of((upperBits shl 3) or lowerBits)
    }

    /**
     * 变卦 —— 指定爻位（0 = 初爻）阴阳互变。
     *
     * 六爻装卦中「动爻」即由此产生；多爻动时依次调用。
     */
    fun changing(vararg yaoIndexes: Int): Hexagram =
        of(yaoIndexes.fold(code) { acc, i -> acc xor (1 shl i) })

    override fun toString(): String = "$name($symbol)"

    override fun equals(other: Any?): Boolean = other is Hexagram && other.code == code
    override fun hashCode(): Int = code

    companion object {
        const val COUNT = 64

        /** 按二进制编码 0..63 排列的卦名。生成规则：`code = (上卦.bits shl 3) or 下卦.bits`。 */
        private val NAMES = arrayOf(
            // 上卦 坤(0b000)
            "坤为地", "地雷复", "地水师", "地泽临", "地山谦", "地火明夷", "地风升", "地天泰",
            // 上卦 震(0b001)
            "雷地豫", "震为雷", "雷水解", "雷泽归妹", "雷山小过", "雷火丰", "雷风恒", "雷天大壮",
            // 上卦 坎(0b010)
            "水地比", "水雷屯", "坎为水", "水泽节", "水山蹇", "水火既济", "水风井", "水天需",
            // 上卦 兑(0b011)
            "泽地萃", "泽雷随", "泽水困", "兑为泽", "泽山咸", "泽火革", "泽风大过", "泽天夬",
            // 上卦 艮(0b100)
            "山地剥", "山雷颐", "山水蒙", "山泽损", "艮为山", "山火贲", "山风蛊", "山天大畜",
            // 上卦 离(0b101)
            "火地晋", "火雷噬嗑", "火水未济", "火泽睽", "火山旅", "离为火", "火风鼎", "火天大有",
            // 上卦 巽(0b110)
            "风地观", "风雷益", "风水涣", "风泽中孚", "风山渐", "风火家人", "巽为风", "风天小畜",
            // 上卦 乾(0b111)
            "天地否", "天雷无妄", "天水讼", "天泽履", "天山遁", "天火同人", "天风姤", "乾为天",
        )

        /** code → 序卦传序号。Unicode 卦符区按序卦传排序，故必须经此换算。 */
        private val KING_WEN = intArrayOf(
            2, 24, 7, 19, 15, 36, 46, 11,
            16, 51, 40, 54, 62, 55, 32, 34,
            8, 3, 29, 60, 39, 63, 48, 5,
            45, 17, 47, 58, 31, 49, 28, 43,
            23, 27, 4, 41, 52, 22, 18, 26,
            35, 21, 64, 38, 56, 30, 50, 14,
            20, 42, 59, 61, 53, 37, 57, 9,
            12, 25, 6, 10, 33, 13, 44, 1,
        )

        private val ALL: List<Hexagram> = (0 until COUNT).map { code ->
            Hexagram(code, NAMES[code], KING_WEN[code])
        }

        init {
            // 自检：数据表必须完整且自洽
            check(NAMES.size == COUNT) { "卦名表长度应为 $COUNT，实际 ${NAMES.size}" }
            check(KING_WEN.size == COUNT) { "序卦传表长度应为 $COUNT" }
            check(KING_WEN.toSet().size == COUNT) { "序卦传序号必须恰好是 1..64 的一个排列" }
            check(KING_WEN.all { it in 1..COUNT })
        }

        fun of(code: Int): Hexagram {
            require(code in 0 until COUNT) { "卦码必须在 0..63，实际 $code" }
            return ALL[code]
        }

        fun of(upper: Trigram, lower: Trigram): Hexagram =
            of((upper.bits shl 3) or lower.bits)

        /** 由自下而上的六爻阴阳取卦。 */
        fun ofLines(bottomToTop: List<Boolean>): Hexagram {
            require(bottomToTop.size == 6) { "需要恰好 6 爻" }
            return of(bottomToTop.foldIndexed(0) { i, acc, yang ->
                if (yang) acc or (1 shl i) else acc
            })
        }

        fun byName(name: String): Hexagram? = ALL.firstOrNull { it.name == name }

        fun byKingWen(index: Int): Hexagram? = ALL.firstOrNull { it.kingWen == index }

        fun all(): List<Hexagram> = ALL
    }
}
