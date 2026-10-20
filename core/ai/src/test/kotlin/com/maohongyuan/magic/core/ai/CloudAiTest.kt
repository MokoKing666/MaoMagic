package com.maohongyuan.magic.core.ai

import com.maohongyuan.magic.core.calendar.BaZiService
import com.maohongyuan.magic.core.calendar.Gender
import com.maohongyuan.magic.core.hexagram.Divination
import com.maohongyuan.magic.core.hexagram.LiuYaoDivination
import com.maohongyuan.magic.core.hexagram.MeihuaDivination
import com.maohongyuan.magic.core.interpret.InterpretationRequest
import com.maohongyuan.magic.core.model.EarthlyBranch
import com.maohongyuan.magic.core.model.GanZhi
import com.maohongyuan.magic.core.model.Hexagram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier2 的纯逻辑测试。
 *
 * 网络部分不做单测（需要真实端点），但**配置判定与提示词构造**是纯函数，
 * 且正是最容易出错的地方 —— 提示词里少写一个字段，模型就会瞎猜。
 */
class CloudAiTest {

    // ── 配置 ──────────────────────────────────────────────

    @Test
    fun `默认配置不完整且不会联网`() {
        val c = CloudAiConfig.DEFAULT
        assertFalse("默认必须是关闭的", c.enabled)
        assertFalse(c.isComplete)
        assertEquals("", c.baseUrl)
        assertEquals("", c.apiKey)
    }

    @Test
    fun `四项缺一不可`() {
        val full = CloudAiConfig(
            enabled = true,
            baseUrl = "https://api.example.com/v1",
            apiKey = "sk-x",
            model = "gpt-4o-mini",
        )
        assertTrue(full.isComplete)

        assertFalse(full.copy(enabled = false).isComplete)
        assertFalse(full.copy(baseUrl = "").isComplete)
        assertFalse(full.copy(baseUrl = "   ").isComplete)
        assertFalse(full.copy(apiKey = "").isComplete)
        assertFalse(full.copy(model = "").isComplete)
    }

    @Test
    fun `端点拼接容忍结尾斜杠`() {
        fun url(base: String) = CloudAiConfig(baseUrl = base).chatCompletionsUrl
        assertEquals("https://a.com/v1/chat/completions", url("https://a.com/v1"))
        assertEquals("https://a.com/v1/chat/completions", url("https://a.com/v1/"))
        assertEquals("https://a.com/v1/chat/completions", url("https://a.com/v1///"))
    }

    // ── 提示词：六爻 ──────────────────────────────────────

    @Test
    fun `六爻提示词包含完整卦盘`() {
        val chart = LiuYaoDivination.chart(
            hexagram = Hexagram.of(39),          // 风地观
            dayGanZhi = GanZhi(59),              // 癸亥
            monthBranch = EarthlyBranch.XU,
            movingIndexes = setOf(2),
            question = "这次面试能过吗",
        )
        val text = ChartPrompt.userMessage(InterpretationRequest("这次面试能过吗", chart = chart))

        // 所问必须原样带上 —— 用神取错，整卦就断偏了
        assertTrue(text.contains("这次面试能过吗"))
        // 卦名与宫位
        assertTrue(text.contains(chart.hexagram.name))
        assertTrue(text.contains(chart.palace.palaceLabel))
        // 月建日辰旬空
        assertTrue(text.contains("戌月"))
        assertTrue(text.contains("癸亥日"))
        assertTrue(text.contains(chart.xunKong.first.label))
        // 六爻逐爻
        assertTrue(text.contains("初爻"))
        assertTrue(text.contains("上爻"))
        // 世应标记
        assertTrue(text.contains("[世]"))
        assertTrue(text.contains("[应]"))
        // 动爻与变爻
        assertTrue(text.contains("·动"))
        assertTrue(text.contains("变爻："))
        // 六亲六神纳甲
        assertTrue(text.contains(chart.shiLine.liuQin.label))
        assertTrue(text.contains(chart.shiLine.liuShen.label))
        assertTrue(text.contains(chart.shiLine.naJia.label))
    }

    @Test
    fun `六爻静卦不出现变爻字样`() {
        val chart = LiuYaoDivination.chart(
            hexagram = Hexagram.of(1),
            dayGanZhi = GanZhi(0),
            monthBranch = EarthlyBranch.ZI,
            movingIndexes = emptySet(),
            question = "静卦测试",
        )
        val text = ChartPrompt.userMessage(InterpretationRequest("静卦测试", chart = chart))
        assertFalse("无动爻时不应出现变爻", text.contains("变爻："))
        assertFalse(text.contains("·动"))
    }

    // ── 提示词：梅花 ──────────────────────────────────────

    @Test
    fun `梅花提示词包含三卦与体用`() {
        val chart = MeihuaDivination.chart(Hexagram.of(39), 2)
        val text = ChartPrompt.userMessage(
            InterpretationRequest("梅花测试", chart = chart, monthBranch = EarthlyBranch.XU)
        )
        assertTrue(text.contains(chart.original.name))
        assertTrue(text.contains(chart.nuclear.name))
        assertTrue(text.contains(chart.changed.name))
        assertTrue(text.contains("体卦："))
        assertTrue(text.contains("用卦："))
        assertTrue(text.contains(chart.tiTrigram.label))
        assertTrue(text.contains(chart.yongTrigram.label))
        // 梅花必须带月建，否则判不了旺衰
        assertTrue(text.contains("戌月"))
    }

    // ── 提示词：八字 ──────────────────────────────────────

    @Test
    fun `八字提示词包含四柱与十神`() {
        val chart = BaZiService.chart(1990, 6, 15, 12, gender = Gender.MALE)
        val text = ChartPrompt.userMessage(InterpretationRequest("看命盘", chart = chart))

        assertTrue(text.contains("年柱"))
        assertTrue(text.contains("月柱"))
        assertTrue(text.contains("日柱"))
        assertTrue(text.contains("时柱"))
        assertTrue(text.contains("日主：${chart.dayMaster}"))
        assertTrue(text.contains("五行分布"))
        assertTrue(text.contains("大运"))
        // 十神与藏干
        assertTrue(text.contains(chart.day.shiShenGan))
    }

    // ── 提示词：通用约束 ──────────────────────────────────

    @Test
    fun `所问为空时有兜底且不崩`() {
        val chart = MeihuaDivination.chart(Hexagram.of(1), 0)
        val text = ChartPrompt.userMessage(InterpretationRequest("", chart = chart))
        assertTrue(text.contains("未填写"))
        assertTrue(text.isNotBlank())
    }

    @Test
    fun `系统提示词写明了不许重排与必须输出 JSON`() {
        // 人设按盘的系统分派，这里用六爻盘取一份提示词
        val chart = MeihuaDivination.chart(Hexagram.of(1), 0)
        val sys = ChartPrompt.systemPrompt(InterpretationRequest("问", chart = chart))
        // 这三条是防止模型自由发挥的关键约束，缺了就会得到乱七八糟的输出
        assertTrue("必须禁止模型自行重排", sys.contains("重排") && sys.contains("不要"))
        assertTrue("必须要求 JSON 输出", sys.contains("JSON"))
        assertTrue("必须包含免责边界", sys.contains("不要") && sys.contains("断言"))
    }
}
