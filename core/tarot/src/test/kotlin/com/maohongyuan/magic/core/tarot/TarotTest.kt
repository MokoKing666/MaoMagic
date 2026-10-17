package com.maohongyuan.magic.core.tarot

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 塔罗的牌库与抽牌测试。
 *
 * 抽牌必须可复现 —— 同一个种子两次抽牌结果要完全一致，
 * 否则测试无法断言、用户也无法重放。
 */
class TarotTest {

    // ── 牌库完整性 ──────────────────────────────────────────

    @Test
    fun `牌库恰好 78 张且 id 不重复`() {
        assertEquals(78, TarotDeck.cards.size)
        assertEquals(78, TarotDeck.cards.map { it.id }.toSet().size)
    }

    @Test
    fun `大阿卡纳 22 张 小阿卡纳 56 张`() {
        assertEquals(22, TarotDeck.cards.count { it.isMajor })
        assertEquals(56, TarotDeck.cards.count { !it.isMajor })
    }

    @Test
    fun `四花色各 14 张`() {
        Suit.entries.forEach { suit ->
            assertEquals(
                "${suit.label}应为 14 张",
                14,
                TarotDeck.cards.count { it.suitEnum == suit },
            )
        }
    }

    @Test
    fun `每张牌都有正位与逆位牌义`() {
        TarotDeck.cards.forEach {
            assertTrue("${it.name} 缺正位", it.upright.isNotBlank())
            assertTrue("${it.name} 缺逆位", it.reversed.isNotBlank())
            assertTrue("${it.name} 缺关键词", it.keywords.isNotEmpty())
        }
    }

    @Test
    fun `大阿卡纳没有花色 小阿卡纳必有花色`() {
        TarotDeck.cards.forEach {
            if (it.isMajor) {
                assertEquals("${it.name} 不该有花色", null, it.suit)
            } else {
                assertTrue("${it.name} 缺花色", it.suit != null)
            }
        }
    }

    // ── 抽牌 ────────────────────────────────────────────────

    @Test
    fun `同一种子抽牌结果完全一致`() {
        val a = TarotDivination.draw(TarotSpread.THREE, random = Random(42))
        val b = TarotDivination.draw(TarotSpread.THREE, random = Random(42))
        assertEquals(
            a.drawn.map { it.card.id to it.reversed },
            b.drawn.map { it.card.id to it.reversed },
        )
    }

    @Test
    fun `不同种子结果不同`() {
        val a = TarotDivination.draw(TarotSpread.THREE, random = Random(1))
        val b = TarotDivination.draw(TarotSpread.THREE, random = Random(2))
        assertTrue(
            "不同种子应抽出不同的牌",
            a.drawn.map { it.card.id } != b.drawn.map { it.card.id },
        )
    }

    @Test
    fun `同一次抽牌不会抽到重复的牌`() {
        repeat(50) { seed ->
            val c = TarotDivination.draw(TarotSpread.CELTIC_CROSS, random = Random(seed))
            assertEquals(
                "seed=$seed 抽到了重复的牌",
                c.drawn.size,
                c.drawn.map { it.card.id }.toSet().size,
            )
        }
    }

    @Test
    fun `牌阵张数与位置名匹配`() {
        TarotSpread.entries.forEach { spread ->
            val c = TarotDivination.draw(spread, random = Random(7))
            assertEquals(spread.size, c.drawn.size)
            assertEquals(spread.positions, c.drawn.map { it.positionName })
        }
    }

    @Test
    fun `正逆位都会出现`() {
        // 统计大量抽牌，正位与逆位都应存在（各 50% 左右）
        var rev = 0
        var up = 0
        repeat(200) { seed ->
            TarotDivination.draw(TarotSpread.CELTIC_CROSS, random = Random(seed)).drawn.forEach {
                if (it.reversed) rev++ else up++
            }
        }
        assertTrue("逆位应出现过", rev > 0)
        assertTrue("正位应出现过", up > 0)
        // 比例应大致均衡（2000 张牌，允许较宽区间）
        val ratio = rev.toDouble() / (rev + up)
        assertTrue("逆位比例 $ratio 偏离 50% 太多", ratio in 0.35..0.65)
    }

    @Test
    fun `逆位时取逆位牌义`() {
        val c = TarotDivination.draw(TarotSpread.SINGLE, random = Random(3))
        val d = c.drawn.first()
        assertEquals(if (d.reversed) d.card.reversed else d.card.upright, d.meaning)
    }

    // ── 引擎 ────────────────────────────────────────────────

    @Test
    fun `引擎产出逐张解读与整体基调`() {
        val chart = TarotDivination.draw(TarotSpread.THREE, "问工作", Random(11))
        val result = runBlocking {
            TarotRuleEngine().interpret(
                com.maohongyuan.magic.core.interpret.InterpretationRequest("问工作", chart)
            ).firstOrNull()
        } ?: error("引擎未产出结果")

        assertEquals("应有 3 张牌 + 基调 + 花色，共至少 4 段", true, result.sections.size >= 4)
        assertTrue("应含整体基调", result.sections.any { it.title == "整体基调" })
        chart.drawn.forEach { d ->
            assertTrue(
                "应含 ${d.positionName} 的解读",
                result.sections.any { it.title.contains(d.positionName) },
            )
        }
    }

    @Test
    fun `引擎对非塔罗的盘返回空流`() {
        val other = com.maohongyuan.magic.core.interpret.InterpretationRequest(
            question = "测试",
            chart = object : com.maohongyuan.magic.core.model.DivinationChart {
                override val system = com.maohongyuan.magic.core.model.DivinationSystem.BA_ZI
            },
        )
        val r = runBlocking { TarotRuleEngine().interpret(other).firstOrNull() }
        assertEquals(null, r)
    }
}
