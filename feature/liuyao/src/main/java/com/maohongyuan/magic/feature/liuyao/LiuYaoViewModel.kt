package com.maohongyuan.magic.feature.liuyao

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.maohongyuan.magic.core.calendar.CalendarService
import com.maohongyuan.magic.core.calendar.DayInfo
import com.maohongyuan.magic.core.hexagram.CoinThrow
import com.maohongyuan.magic.core.hexagram.Divination
import com.maohongyuan.magic.core.hexagram.DivinationMethod
import com.maohongyuan.magic.core.hexagram.DivinationResult
import com.maohongyuan.magic.core.hexagram.LiuYaoChart
import com.maohongyuan.magic.core.hexagram.LiuYaoDivination
import com.maohongyuan.magic.core.interpret.EngineTier
import com.maohongyuan.magic.core.interpret.Interpretation
import com.maohongyuan.magic.core.interpret.InterpretationRequest
import com.maohongyuan.magic.core.interpret.InterpreterChain
import com.maohongyuan.magic.core.interpret.LiuYaoRuleEngine
import com.maohongyuan.magic.core.model.Hexagram
import kotlinx.coroutines.flow.firstOrNull
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * 六爻页面状态。
 */
data class LiuYaoUiState(
    val question: String = "",
    val method: DivinationMethod = DivinationMethod.COIN,
    /** 当前日的日课 —— 日辰与月建由它提供 */
    val day: DayInfo? = null,
    /** 起卦结果 */
    val result: DivinationResult? = null,
    /** 装卦结果 */
    val chart: LiuYaoChart? = null,
    /** 解卦结果 */
    val interpretation: Interpretation? = null,
    val interpreting: Boolean = false,
    /** 数字起卦的两个输入 */
    val numberA: String = "",
    val numberB: String = "",
    /** 手动起卦：自初爻至上爻 */
    val manualLines: List<Boolean> = List(6) { true },
    val manualMoving: Set<Int> = emptySet(),
    /** 铜钱投掷记录，用于逐次展示 */
    val throws: List<CoinThrow> = emptyList(),
    /** 起卦中（摇卦动画用） */
    val casting: Boolean = false,
) {
    val canCast: Boolean
        get() = when (method) {
            DivinationMethod.NUMBER -> numberA.toIntOrNull() != null && numberB.toIntOrNull() != null
            DivinationMethod.MANUAL -> true
            else -> true
        }

    /** 当前使用的解卦引擎档位 */
    val tier: EngineTier get() = interpretation?.tier ?: EngineTier.RULE
}

/**
 * 六爻页面状态管理。
 *
 * 起卦 → 装卦 → 解卦 三步串起来：
 * 1. [Divination] 产出本卦与动爻
 * 2. [LiuYaoDivination] 按日辰月建装卦
 * 3. [InterpreterChain] 走降级链解卦（当前只有 Tier1 可用）
 */
class LiuYaoViewModel : ViewModel() {

    var state by mutableStateOf(LiuYaoUiState())
        private set

    /** 解卦引擎降级链。Tier2/3 尚未接入，但链接口已就位。 */
    private val chain = InterpreterChain(listOf(LiuYaoRuleEngine()))

    init {
        refreshDay()
    }

    fun refreshDay() {
        state = state.copy(day = CalendarService.today())
    }

    fun setQuestion(value: String) {
        state = state.copy(question = value)
    }

    fun selectMethod(value: DivinationMethod) {
        state = state.copy(method = value, interpretation = null)
    }

    fun setNumberA(value: String) {
        state = state.copy(numberA = value.filter { it.isDigit() }.take(6))
    }

    fun setNumberB(value: String) {
        state = state.copy(numberB = value.filter { it.isDigit() }.take(6))
    }

    /** 手动起卦：切换某一爻的阴阳 */
    fun toggleManualLine(index: Int) {
        val lines = state.manualLines.toMutableList()
        lines[index] = !lines[index]
        state = state.copy(manualLines = lines)
    }

    /** 手动起卦：切换某一爻是否动 */
    fun toggleManualMoving(index: Int) {
        val moving = state.manualMoving.toMutableSet()
        if (!moving.add(index)) moving.remove(index)
        state = state.copy(manualMoving = moving)
    }

    // ── 起卦 ──────────────────────────────────────────────────

    /** 铜钱摇卦 —— 一次摇出六爻。 */
    fun castByCoin() {
        val result = Divination.byCoinRandom()
        state = state.copy(
            throws = result.throws,
            result = result,
        )
        buildChart(result)
    }

    /** 时间起卦 —— 用当下时刻。 */
    fun castByTime() {
        val day = CalendarService.today()
        val hourBranch = hourBranchOf(day.solar.hour)
        val result = Divination.byTime(
            yearBranch = day.ganZhi.year?.branch ?: com.maohongyuan.magic.core.model.EarthlyBranch.ZI,
            lunarMonth = day.lunar.month,
            lunarDay = day.lunar.day,
            hourBranch = hourBranch,
        )
        state = state.copy(result = result, throws = emptyList())
        buildChart(result)
    }

    /** 数字起卦 */
    fun castByNumber() {
        val a = state.numberA.toIntOrNull() ?: return
        val b = state.numberB.toIntOrNull() ?: return
        val result = Divination.byNumber(a, b)
        state = state.copy(result = result, throws = emptyList())
        buildChart(result)
    }

    /** 手动起卦 */
    fun castByManual() {
        val result = Divination.byManual(state.manualLines, state.manualMoving)
        state = state.copy(result = result, throws = emptyList())
        buildChart(result)
    }

    /** 清空重来 */
    fun reset() {
        state = state.copy(
            result = null,
            chart = null,
            interpretation = null,
            throws = emptyList(),
        )
    }

    /** 示例卦 —— 供用户先看看界面长什么样 */
    fun loadDemo() {
        val demo = Hexagram.byName("天风姤") ?: return
        val result = Divination.byManual(demo.lines, setOf(3))
        state = state.copy(
            question = state.question.ifBlank { "今年事业运势如何？" },
            result = result,
            throws = emptyList(),
        )
        buildChart(result)
    }

    // ── 装卦与解卦 ────────────────────────────────────────────

    private fun buildChart(result: DivinationResult) {
        val day = state.day ?: CalendarService.today()
        val dayGanZhi = day.ganZhi.day
        val monthBranch = day.monthBranch
        if (dayGanZhi == null || monthBranch == null) {
            // 历法数据缺失 —— 不应发生，但要有兜底，不能让页面崩
            state = state.copy(chart = null)
            return
        }

        val chart = LiuYaoDivination.chart(
            hexagram = result.hexagram,
            dayGanZhi = dayGanZhi,
            monthBranch = monthBranch,
            movingIndexes = result.movingIndexes,
            question = state.question,
        )
        state = state.copy(chart = chart, interpretation = null)
        interpret(chart)
    }

    private fun interpret(chart: LiuYaoChart) {
        state = state.copy(interpreting = true)
        viewModelScope.launch {
            val result = runCatching {
                chain.interpret(
                    InterpretationRequest(
                        question = state.question.ifBlank { "综合" },
                        liuYao = chart,
                    )
                )
            }.getOrNull()
            state = state.copy(interpretation = result, interpreting = false)
        }
    }

    private fun hourBranchOf(hour: Int): com.maohongyuan.magic.core.model.EarthlyBranch {
        // 子时 23:00–01:00 ⇒ (hour + 1) / 2 % 12
        val index = ((hour + 1) / 2) % 12
        return com.maohongyuan.magic.core.model.EarthlyBranch.entries[index]
    }
}
