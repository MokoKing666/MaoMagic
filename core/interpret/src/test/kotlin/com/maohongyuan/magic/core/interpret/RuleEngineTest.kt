package com.maohongyuan.magic.core.interpret

import com.maohongyuan.magic.core.calendar.BaZiService
import com.maohongyuan.magic.core.calendar.Gender
import com.maohongyuan.magic.core.hexagram.Divination
import com.maohongyuan.magic.core.hexagram.LiuYaoDivination
import com.maohongyuan.magic.core.hexagram.MeihuaDivination
import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.GanZhi
import com.maohongyuan.magic.core.model.Hexagram
import com.maohongyuan.magic.core.model.LiuQin
import com.maohongyuan.magic.core.model.Trigram
import com.maohongyuan.magic.core.model.WuXingRelation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionCategoryTest {

    @Test
    fun `关键词命中事类`() {
        assertEquals(QuestionCategory.CAREER, QuestionCategory.detect("今年事业运势如何"))
        assertEquals(QuestionCategory.CAREER, QuestionCategory.detect("这次考试能过吗"))
        assertEquals(QuestionCategory.WEALTH, QuestionCategory.detect("这笔生意能不能成"))
        assertEquals(QuestionCategory.MARRIAGE, QuestionCategory.detect("我和他还有姻缘吗"))
        assertEquals(QuestionCategory.HEALTH, QuestionCategory.detect("家人病情何时好转"))
        assertEquals(QuestionCategory.CHILDREN, QuestionCategory.detect("什么时候能怀孕"))
        assertEquals(QuestionCategory.TRAVEL, QuestionCategory.detect("下月搬家顺利吗"))
        assertEquals(QuestionCategory.FAMILY, QuestionCategory.detect("这个合同能签吗"))
        assertEquals(QuestionCategory.PEER, QuestionCategory.detect("和合伙人能否长久"))
    }

    @Test
    fun `无关键词时归入综合`() {
        assertEquals(QuestionCategory.GENERAL, QuestionCategory.detect("我想问问"))
        assertEquals(QuestionCategory.GENERAL, QuestionCategory.detect(""))
    }

    @Test
    fun `用神映射符合传统`() {
        assertEquals(LiuQin.GUAN_GUI, QuestionCategory.CAREER.yongShen)
        assertEquals(LiuQin.QI_CAI, QuestionCategory.WEALTH.yongShen)
        assertEquals(LiuQin.FU_MU, QuestionCategory.FAMILY.yongShen)
        assertEquals(LiuQin.ZI_SUN, QuestionCategory.CHILDREN.yongShen)
        // 综合类以世爻为用，故无固定六亲
        assertEquals(null, QuestionCategory.GENERAL.yongShen)
    }
}

class MeihuaChartTest {

    @Test
    fun `动爻在上卦则上卦为用下卦为体`() {
        // 泽火革 九四动（index 3，上卦）
        val chart = MeihuaDivination.chart(Hexagram.byName("泽火革")!!, 3)
        assertEquals(Trigram.DUI, chart.yongTrigram)   // 上卦兑为用
        assertEquals(Trigram.LI, chart.tiTrigram)      // 下卦离为体
        assertEquals(com.maohongyuan.magic.core.model.WuXing.HUO, chart.tiWuXing)
        assertEquals(com.maohongyuan.magic.core.model.WuXing.JIN, chart.yongWuXing)
        // 火克金 ⇒ 体克用 ⇒ 吉
        assertEquals(WuXingRelation.I_OVERCOME, chart.relation)
        assertEquals("体克用", chart.relationLabel)
        assertTrue(chart.favorable)
    }

    @Test
    fun `动爻在下卦则下卦为用上卦为体`() {
        // 泽火革 初九动（index 0，下卦）
        val chart = MeihuaDivination.chart(Hexagram.byName("泽火革")!!, 0)
        assertEquals(Trigram.LI, chart.yongTrigram)    // 下卦离为用
        assertEquals(Trigram.DUI, chart.tiTrigram)     // 上卦兑为体
        // 火克金 ⇒ 用克体 ⇒ 凶
        assertEquals(WuXingRelation.IT_OVERCOMES, chart.relation)
        assertEquals("用克体", chart.relationLabel)
        assertTrue(!chart.favorable)
    }

    @Test
    fun `互卦与变卦自动计算`() {
        val chart = MeihuaDivination.chart(Hexagram.byName("泽火革")!!, 3)
        assertEquals("天风姤", chart.nuclear.name)
        assertEquals("水火既济", chart.changed.name)
        assertEquals("九四", chart.movingTitle)
    }

    @Test
    fun `体用关系与吉凶对应表`() {
        // 用生体 吉、比和 吉、体克用 吉、体生用 凶、用克体 凶
        fun favorable(relation: WuXingRelation) = when (relation) {
            WuXingRelation.IT_GENERATES, WuXingRelation.SAME, WuXingRelation.I_OVERCOME -> true
            WuXingRelation.I_GENERATE, WuXingRelation.IT_OVERCOMES -> false
        }
        assertTrue(favorable(WuXingRelation.IT_GENERATES))
        assertTrue(favorable(WuXingRelation.SAME))
        assertTrue(favorable(WuXingRelation.I_OVERCOME))
        assertTrue(!favorable(WuXingRelation.I_GENERATE))
        assertTrue(!favorable(WuXingRelation.IT_OVERCOMES))
    }
}

class RuleEngineTest {

    private val jiaZi = GanZhi(0)

    private fun liuYaoChart(name: String, moving: Set<Int> = emptySet()) =
        LiuYaoDivination.chart(
            hexagram = Hexagram.byName(name)!!,
            dayGanZhi = jiaZi,
            monthBranch = EarthlyBranch.XU,
            movingIndexes = moving,
        )

    @Test
    fun `六爻引擎产出完整结果`() = runBlocking {
        val engine = LiuYaoRuleEngine()
        val result = engine.interpret(
            InterpretationRequest(
                question = "今年事业运势如何",
                chart = liuYaoChart("天风姤", setOf(3)),
            )
        ).firstOrNull()

        assertNotNull(result)
        assertEquals(EngineTier.RULE, result!!.tier)
        assertTrue(result.headline.isNotBlank())
        assertTrue("应有分段解读", result.sections.size >= 4)
        // 用神应为官鬼（事业）
        assertTrue(result.yongShenNote?.contains("官鬼") == true)
        // 每段都要有标题与正文
        result.sections.forEach {
            assertTrue(it.title.isNotBlank())
            assertTrue(it.body.isNotBlank())
        }
    }

    @Test
    fun `六爻引擎对静卦也能出结论`() = runBlocking {
        val result = LiuYaoRuleEngine().interpret(
            InterpretationRequest("随便问问", chart = liuYaoChart("乾为天"))
        ).firstOrNull()
        assertNotNull(result)
        assertTrue(result!!.sections.any { it.body.contains("静卦") })
    }

    @Test
    fun `六爻引擎用神不上卦时取伏神`() = runBlocking {
        // 天风姤缺妻财，问财运应取伏神
        val result = LiuYaoRuleEngine().interpret(
            InterpretationRequest("今年财运如何", chart = liuYaoChart("天风姤"))
        ).firstOrNull()!!
        assertTrue(result.sections.any { it.body.contains("伏神") })
    }

    @Test
    fun `六爻引擎收到梅花请求时返回空`() = runBlocking {
        val meihua = MeihuaDivination.byNumber(1, 2)
        val result = LiuYaoRuleEngine().interpret(
            InterpretationRequest("测试", chart = meihua)
        ).firstOrNull()
        assertEquals(null, result)
    }

    @Test
    fun `梅花引擎产出完整结果`() = runBlocking {
        val result = MeihuaRuleEngine().interpret(
            InterpretationRequest(
                question = "此事可行否",
                chart = MeihuaDivination.chart(Hexagram.byName("泽火革")!!, 3),
                monthBranch = EarthlyBranch.XU,
            )
        ).firstOrNull()!!

        assertEquals(EngineTier.RULE, result.tier)
        assertTrue(result.sections.any { it.title == "体用" })
        assertTrue(result.sections.any { it.title == "互卦" })
        // 九四在上卦 ⇒ 上卦兑为用、下卦离为体
        assertEquals("体离 · 用兑", result.yongShenNote)
    }

    @Test
    fun `梅花缺月建时跳过旺衰并说明`() = runBlocking {
        val result = MeihuaRuleEngine().interpret(
            InterpretationRequest(
                question = "测试",
                chart = MeihuaDivination.chart(Hexagram.byName("泽火革")!!, 3),
                monthBranch = null,
            )
        ).firstOrNull()!!
        assertTrue(result.sections.any { it.body.contains("未提供月建") })
    }

    @Test
    fun `八字引擎只做客观陈述不做格局推断`() = runBlocking {
        val chart = BaZiService.chart(1990, 6, 15, 12, gender = Gender.MALE)
        val result = BaZiRuleEngine().interpret(
            InterpretationRequest("看下命盘", chart = chart)
        ).firstOrNull()!!

        assertEquals(EngineTier.RULE, result.tier)
        assertTrue(result.headline.contains("日主"))
        // 必须明确声明不做用神取用
        assertTrue(result.sections.any { it.body.contains("不含用神取用") })
    }

    @Test
    fun `引擎收到处理不了的盘时返回空流而不是崩`() = runBlocking {
        // 这是改造后的关键契约：引擎用 `as?` 取盘，取不到就 emit 空流，
        // 由 InterpreterChain 继续降级。绝不能抛异常 ——
        // 否则一个引擎的不匹配会打断整条链。
        val meihua = MeihuaDivination.byNumber(1, 2)
        val liuYao = liuYaoChart("乾为天")

        assertNull(
            "六爻引擎不该处理梅花盘",
            LiuYaoRuleEngine().interpret(InterpretationRequest("测试", chart = meihua)).firstOrNull(),
        )
        assertNull(
            "梅花引擎不该处理六爻盘",
            MeihuaRuleEngine().interpret(InterpretationRequest("测试", chart = liuYao)).firstOrNull(),
        )
        assertNull(
            "八字引擎不该处理六爻盘",
            BaZiRuleEngine().interpret(InterpretationRequest("测试", chart = liuYao)).firstOrNull(),
        )
    }
}

class InterpreterChainTest {

    /** 永远不可用的假引擎 */
    private class UnavailableEngine(override val tier: EngineTier) : DivinationInterpreter {
        override suspend fun isAvailable(): Boolean = false
        override fun interpret(request: InterpretationRequest): Flow<Interpretation> = flowOf()
    }

    /** 声明可用但执行时失败的引擎 —— 用来验证「可用只是入场券」 */
    private class FailingEngine(override val tier: EngineTier) : DivinationInterpreter {
        override suspend fun isAvailable(): Boolean = true
        override fun interpret(request: InterpretationRequest): Flow<Interpretation> = flowOf()
    }

    @Test
    fun `降级链在高端引擎不可用时落到 Tier1`() = runBlocking {
        val chain = InterpreterChain(
            listOf(
                UnavailableEngine(EngineTier.ON_DEVICE),
                UnavailableEngine(EngineTier.CLOUD_AI),
                LiuYaoRuleEngine(),
            )
        )
        assertEquals(EngineTier.RULE, chain.resolve().tier)
    }

    @Test
    fun `可用但执行失败的引擎会被跳过`() = runBlocking {
        val chain = InterpreterChain(
            listOf(
                FailingEngine(EngineTier.CLOUD_AI),
                LiuYaoRuleEngine(),
            )
        )
        val result = chain.interpret(
            InterpretationRequest(
                "测试",
                chart = LiuYaoDivination.chart(
                    Hexagram.byName("乾为天")!!,
                    GanZhi(0),
                    EarthlyBranch.XU,
                ),
            )
        )
        // 云端引擎声明可用但没产出结果，应降级到 Tier1
        assertEquals(EngineTier.RULE, result.tier)
    }

    @Test
    fun `降级链顺序恒为 Tier3 Tier2 Tier1`() {
        assertEquals(
            listOf(EngineTier.ON_DEVICE, EngineTier.CLOUD_AI, EngineTier.RULE),
            EngineTier.chain,
        )
    }

    @Test
    fun `Tier1 永远可用`() = runBlocking {
        assertTrue(LiuYaoRuleEngine().isAvailable())
        assertTrue(MeihuaRuleEngine().isAvailable())
        assertTrue(BaZiRuleEngine().isAvailable())
    }
}
