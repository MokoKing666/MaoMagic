package com.maohongyuan.magic.core.ai

import com.maohongyuan.magic.core.interpret.InterpretationRequest
import com.maohongyuan.magic.core.model.DivinationChart
import com.maohongyuan.magic.core.model.DivinationSystem
import com.maohongyuan.magic.core.tarot.TarotDivination
import com.maohongyuan.magic.core.tarot.TarotSpread
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 提示词必须按系统分派 —— 这是被真实用户抓到的 bug 的回归测试。
 *
 * 现场：塔罗解读里出现了「世爻应爻」。
 * 原因：`SYSTEM_PROMPT` 是写死的六爻人设，塔罗走 `else` 分支只拿到一句
 * 「暂无结构化排盘」，模型拿着六爻人设、没有卦盘，只能凭空编。
 *
 * 这里锁死三件事：
 * 1. 非六爻系统的**人设正文**里不得出现六爻术语（禁令清单本身除外）
 * 2. 塔罗的用户消息必须**真的带上牌面**，而不是「暂无结构化排盘」
 * 3. 六爻自己的提示词仍保留世应等术语 —— 别矫枉过正把六爻也洗掉
 */
class ChartPromptTest {

    /** 只为测人设分派的最小假盘 —— 不参与渲染 */
    private data class FakeChart(override val system: DivinationSystem) : DivinationChart

    private fun prompt(system: DivinationSystem): String =
        ChartPrompt.systemPrompt(InterpretationRequest("问一件事", chart = FakeChart(system)))

    /**
     * 只取**人设正文**：砍掉【硬约束 · 术语】（它本身要列出被禁的词），
     * 也砍掉后面共用的输出契约（术语对照表里同样会出现这些词）。
     */
    private fun body(system: DivinationSystem): String =
        prompt(system)
            .substringBefore("【怎么说话")
            .replace(Regex("【硬约束 · 术语】[\\s\\S]*"), "")

    @Test
    fun `塔罗人设正文不含任何六爻术语`() {
        val b = body(DivinationSystem.TAROT)
        assertTrue("塔罗人设缺失", b.contains("塔罗"))
        listOf("世爻", "应爻", "纳甲", "六亲", "旬空", "伏神", "变爻", "用神").forEach {
            assertFalse("塔罗人设正文里出现了 $it", b.contains(it))
        }
    }

    @Test
    fun `塔罗提示词带有术语禁令`() {
        assertTrue(prompt(DivinationSystem.TAROT).contains("【硬约束 · 术语】"))
        assertTrue(prompt(DivinationSystem.TAROT).contains("世爻"))
    }

    @Test
    fun `小六壬人设正文不含六爻核心术语`() {
        val b = body(DivinationSystem.XIAO_LIU_REN)
        assertTrue(b.contains("小六壬"))
        listOf("世爻", "应爻", "纳甲", "用神", "旬空", "伏神").forEach {
            assertFalse("小六壬人设正文里出现了 $it", b.contains(it))
        }
    }

    @Test
    fun `无结构化排盘的系统给出通用人设且禁用六爻术语`() {
        val b = body(DivinationSystem.ZI_WEI)
        assertTrue(b.contains("紫微斗数"))
        assertFalse(b.contains("世爻"))
        assertFalse(b.contains("纳甲"))
    }

    @Test
    fun `六爻提示词保留世应等术语`() {
        val p = prompt(DivinationSystem.LIU_YAO)
        listOf("世应", "纳甲", "六亲", "旬空", "伏神", "用神").forEach {
            assertTrue("六爻人设丢了 $it", p.contains(it))
        }
    }

    @Test
    fun `所有系统都要求输出 JSON 并带边界声明`() {
        DivinationSystem.entries.forEach { s ->
            val p = prompt(s)
            assertTrue("$s 没要求 JSON", p.contains("JSON"))
            assertTrue("$s 缺边界声明", p.contains("仅供传统文化研究"))
        }
    }

    @Test
    fun `任何系统都不再索要用神字段 —— 输出不许出现术语`() {
        DivinationSystem.entries.forEach { s ->
            assertFalse("$s 仍在要求 yongShen", prompt(s).contains("yongShen"))
        }
    }

    @Test
    fun `所有系统都必须直说，并列出禁止的推脱话`() {
        DivinationSystem.entries.forEach { s ->
            val p = prompt(s)
            assertTrue("$s 缺「必须直说」", p.contains("必须直说"))
            listOf("卦里没显示", "因人而异", "仅供参考").forEach {
                assertTrue("$s 缺禁用话术 $it", p.contains(it))
            }
        }
    }

    @Test
    fun `所有系统都带术语对照表`() {
        DivinationSystem.entries.forEach { s ->
            val p = prompt(s)
            assertTrue("$s 缺术语对照", p.contains("世爻→你自己这一边"))
            assertTrue("$s 缺应爻对照", p.contains("应爻→对方那一边"))
        }
    }

    @Test
    fun `所有系统都要求建议可落地`() {
        DivinationSystem.entries.forEach { s ->
            val p = prompt(s)
            assertTrue("$s 缺落地要求", p.contains("那我明天做什么"))
        }
    }

    @Test
    fun `塔罗用户消息带上牌面而非暂无排盘`() {
        val chart = TarotDivination.draw(TarotSpread.THREE, question = "换工作", random = Random(7))
        val msg = ChartPrompt.userMessage(InterpretationRequest("换工作", chart = chart))

        assertTrue("应包含牌阵名", msg.contains(TarotSpread.THREE.label))
        assertFalse("不应再落到「暂无结构化排盘」", msg.contains("暂未提供结构化排盘"))
        chart.drawn.forEach { d ->
            assertTrue("缺位置名 ${d.positionName}", msg.contains(d.positionName))
            assertTrue("缺牌名 ${d.card.name}", msg.contains(d.card.name))
        }
    }

    @Test
    fun `未覆盖的系统仍给出系统名且不虚构盘面`() {
        val msg = ChartPrompt.userMessage(
            InterpretationRequest("问", chart = FakeChart(DivinationSystem.QI_MEN))
        )
        assertTrue(msg.contains(DivinationSystem.QI_MEN.label))
        assertTrue(msg.contains("不要虚构盘面"))
    }
}
