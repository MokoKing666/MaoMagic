package com.maohongyuan.magic.core.tarot

import com.maohongyuan.magic.core.model.DivinationChart
import com.maohongyuan.magic.core.model.DivinationSystem
import com.maohongyuan.magic.core.resource.JsonResource
import kotlinx.serialization.Serializable
import kotlin.random.Random

/** 大阿卡纳 / 小阿卡纳 */
enum class Arcana { MAJOR, MINOR }

/** 小阿卡纳四花色。花色对应元素，是牌义的基础。 */
enum class Suit(val label: String, val element: String) {
    WANDS("权杖", "火"),
    CUPS("圣杯", "水"),
    SWORDS("宝剑", "风"),
    PENTACLES("星币", "土"),
}

/**
 * 一张牌。
 *
 * 牌义数据译自 A. E. Waite《The Pictorial Key to the Tarot》(1911)，
 * 该著作已进入公有领域。**不取自任何现代出版物** —— 那些书受版权保护，
 * 其具体文字不可复制，连「换几个词照搬结构」也属实质性相似。
 */
@Serializable
data class TarotCard(
    val id: Int,
    val name: String,
    val arcana: String,
    val rank: String,
    val suit: String? = null,
    val keywords: List<String> = emptyList(),
    val upright: String,
    val reversed: String,
) {
    val isMajor: Boolean get() = arcana == "MAJOR"
    val suitEnum: Suit? get() = suit?.let { s -> Suit.entries.firstOrNull { it.name == s } }
}

/**
 * 牌库。
 *
 * `init` 里做完整性自检：78 张、id 唯一、四花色各 14 张、大阿卡纳 22 张。
 * 少一张或多一张，抽牌概率就错了，且**不会报错** —— 必须在加载时炸出来。
 */
object TarotDeck {

    val cards: List<TarotCard> by lazy {
        val major = JsonResource.load<CardFile>("/tarot/major.json").cards
        val minor = JsonResource.load<CardFile>("/tarot/minor.json").cards
        major + minor
    }

    init {
        val all = cards
        check(all.size == TOTAL) { "牌库必须 $TOTAL 张，实际 ${all.size}" }
        check(all.map { it.id }.toSet().size == TOTAL) { "牌 id 有重复或缺失" }
        check(all.count { it.isMajor } == 22) { "大阿卡纳必须 22 张，实际 ${all.count { it.isMajor }}" }
        check(all.count { !it.isMajor } == 56) { "小阿卡纳必须 56 张" }
        Suit.entries.forEach { suit ->
            val n = all.count { it.suitEnum == suit }
            check(n == 14) { "${suit.label}必须 14 张，实际 $n" }
        }
        check(all.all { it.upright.isNotBlank() && it.reversed.isNotBlank() }) {
            "有牌缺少正位或逆位牌义"
        }
    }

    const val TOTAL = 78

    @Serializable
    private data class CardFile(val cards: List<TarotCard> = emptyList())
}

/** 牌阵。位置名会直接显示在界面上，必须是有意义的。 */
enum class TarotSpread(val label: String, val positions: List<String>) {
    SINGLE("单张", listOf("整体")),
    THREE("三张", listOf("过去", "现在", "未来")),
    CELTIC_CROSS(
        "凯尔特十字",
        listOf(
            "现状", "阻碍", "根基", "过去",
            "目标", "近期", "自身", "环境",
            "希望与恐惧", "结果",
        ),
    ),
    ;

    val size: Int get() = positions.size
}

/** 抽到的一张牌（含正逆位与所在位置） */
data class DrawnCard(
    val card: TarotCard,
    val reversed: Boolean,
    val positionName: String,
) {
    /** 当前朝向的牌义 */
    val meaning: String get() = if (reversed) card.reversed else card.upright
    val orientationLabel: String get() = if (reversed) "逆位" else "正位"
}

/** 塔罗盘 */
data class TarotChart(
    val spread: TarotSpread,
    val drawn: List<DrawnCard>,
    val question: String,
) : DivinationChart {

    override val system: DivinationSystem = DivinationSystem.TAROT

    /** 逆位占比 —— 用来判断整体基调 */
    val reversedCount: Int get() = drawn.count { it.reversed }
}

/**
 * 抽牌。
 *
 * ── Random 必须可注入 ──
 * 不给随机源就没法测（同一次抽牌要能复现），用户也无法重放同一副牌。
 * 默认值只是给生产用的，测试一律传固定种子。
 *
 * ── 不重复 ──
 * 洗牌后取前 n 张，天然不会抽到重复的牌。若用「每次随机取一张」则可能重复。
 */
object TarotDivination {

    fun draw(
        spread: TarotSpread,
        question: String = "",
        random: Random = Random.Default,
    ): TarotChart {
        val picked = TarotDeck.cards.shuffled(random).take(spread.size)
        return TarotChart(
            spread = spread,
            drawn = picked.mapIndexed { index, card ->
                DrawnCard(
                    card = card,
                    // 正逆位各 50%
                    reversed = random.nextBoolean(),
                    positionName = spread.positions[index],
                )
            },
            question = question,
        )
    }
}
