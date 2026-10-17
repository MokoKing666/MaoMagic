package com.maohongyuan.magic.feature.liuyao

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.maohongyuan.magic.core.calendar.CalendarService
import com.maohongyuan.magic.core.ai.CloudAiConfig
import com.maohongyuan.magic.core.ai.CloudAiInterpreter
import com.maohongyuan.magic.core.data.SecretStore
import com.maohongyuan.magic.core.data.SettingsStore
import com.maohongyuan.magic.core.data.DivinationRecord
import com.maohongyuan.magic.core.data.DivinationRecordStore
import com.maohongyuan.magic.core.calendar.DayInfo
import com.maohongyuan.magic.core.hexagram.CoinThrow
import com.maohongyuan.magic.core.hexagram.HexagramTable
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
    /**
     * 占卜者性别。
     *
     * 只影响婚姻/感情类的用神取法 —— 男占看妻财，女占看官鬼。
     * 其他事类不受影响，所以默认男即可，不必强制用户选。
     */
    val gender: com.maohongyuan.magic.core.calendar.Gender =
        com.maohongyuan.magic.core.calendar.Gender.MALE,
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
    /**
     * 正在查看的历史卦例 id。非 null 表示盘面来自历史记录，
     * 此时**不再落库**，否则每看一次就多存一条。
     */
    val viewingRecordId: String? = null,
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
    /** 历史卦例，按时间倒序 */
    val history: List<DivinationRecord> = emptyList(),
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
class LiuYaoViewModel(application: Application) : AndroidViewModel(application) {

    private val recordStore = DivinationRecordStore(application)
    private val settingsStore = SettingsStore(application)
    private val secretStore = SecretStore(application)

    var state by mutableStateOf(LiuYaoUiState())
        private set

    /**
     * 云端配置的**实时快照**。
     *
     * 用可变字段 + lambda 提供给链，而不是构造时读一次 ——
     * 用户在设置里填完 Key 应当**立刻生效**，不必重启应用。
     */
    private var cloudConfig = CloudAiConfig.DEFAULT

    /**
     * 解卦引擎降级链：智能模式 → 基础模式。
     *
     * [CloudAiConfig] 默认关闭，所以默认状态下 [CloudAiInterpreter.isAvailable]
     * 恒为 false，链直接落到 Tier1 —— **默认零网络**。
     */
    private val chain = InterpreterChain(
        listOf(
            CloudAiInterpreter { cloudConfig },
            LiuYaoRuleEngine(),
        )
    )

    init {
        refreshDay()
        viewModelScope.launch {
            settingsStore.settings.collect { s ->
                cloudConfig = CloudAiConfig(
                    enabled = s.cloudAiEnabled,
                    baseUrl = s.cloudAiBaseUrl,
                    apiKey = secretStore.get(SecretStore.KEY_CLOUD_API_KEY),
                    model = s.cloudAiModel,
                    temperature = s.cloudAiTemperature,
                )
            }
        }
        viewModelScope.launch {
            recordStore.records.collect { list ->
                state = state.copy(history = list)
            }
        }
    }

    // ── 历史 ──────────────────────────────────────────────────

    fun deleteRecord(id: String) {
        viewModelScope.launch { recordStore.delete(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { recordStore.clear() }
    }

    fun refreshDay() {
        state = state.copy(day = CalendarService.today())
    }

    fun setQuestion(value: String) {
        state = state.copy(question = value)
    }

    /**
     * 设置占卜者性别。
     *
     * 只影响婚姻/感情类的用神取法 —— 男占看妻财，女占看官鬼。
     * 改性别后若已有解卦结果，要重新解一次，否则界面上的结论还是按旧性别算的。
     */
    fun setGender(value: com.maohongyuan.magic.core.calendar.Gender) {
        if (state.gender == value) return
        state = state.copy(gender = value)
        state.chart?.let { interpret(it) }
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

    /**
     * 摇**一爻**（三枚铜钱一次）。
     *
     * 铜钱起卦讲究的是「一爻一摇、六摇成卦」—— 一次摇出六爻虽然结果一样，
     * 但把过程压缩成一个按钮，仪式感就没了。所以拆成六次。
     *
     * 背面记 2、正面（字）记 3，三枚相加：6 老阴、7 少阳、8 少阴、9 老阳。
     * 摇满六爻才建盘。
     */
    fun throwOneCoin() {
        if (state.throws.size >= 6) return
        val one = CoinThrow(List(3) { if (kotlin.random.Random.nextBoolean()) 3 else 2 })
        val next = state.throws + one
        if (next.size >= 6) {
            val result = runCatching { Divination.byCoin(next) }.getOrNull()
            state = state.copy(throws = next, result = result)
            if (result != null) buildChart(result)
        } else {
            state = state.copy(throws = next)
        }
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
            // 用「最近已过的节气」，不是 day.jieQi —— 后者只有节气当天才有值
            jieQi = CalendarService.recentJieQi(day.solar.year, day.solar.month, day.solar.day),
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
                        chart = chart,
                        gender = state.gender,
                    )
                )
            }.getOrNull()
            state = state.copy(interpretation = result, interpreting = false)
            saveRecord(chart, result)
        }
    }

    /**
     * 落库。
     *
     * **只存卦象与日辰，不存解读全文** —— Tier1 是纯规则引擎，
     * 同样输入必得同样输出，解读可完全复现；存全文既浪费空间，
     * 又会在规则演进后与实际结果不一致。
     */
    private fun saveRecord(chart: LiuYaoChart, interpretation: Interpretation?) {
        // 查看历史卦例时不落库 —— 否则每点开一次就多存一条重复记录
        if (state.viewingRecordId != null) return
        val now = System.currentTimeMillis()
        val record = DivinationRecord(
            id = now.toString(),
            createdAt = now,
            method = state.result?.method?.label ?: "",
            question = state.question,
            hexagramCode = chart.hexagram.code,
            movingIndexes = chart.movingIndexes.sorted(),
            dayGanZhiIndex = chart.dayGanZhi.index,
            monthBranchOrdinal = chart.monthBranch.ordinal,
            verdict = interpretation?.verdict?.label,
            headline = interpretation?.headline,
        )
        viewModelScope.launch { recordStore.add(record) }
    }

    /**
     * 打开一条历史卦例。
     *
     * ⚠️ **必须用记录自己的时间还原日课，绝不能用今天。**
     * 六爻断卦靠日辰、月建定旺衰；用今日日辰重算旧卦，结论会变，那是误导。
     *
     * 记录里虽然存了 `dayGanZhiIndex` / `monthBranchOrdinal`，
     * 但 `createdAt` 能直接还原出当天日期，走历法重取更完整
     * （月建、旬空、节气都对得上）。
     */
    fun openRecord(record: DivinationRecord) {
        val date = java.time.Instant.ofEpochMilli(record.createdAt)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        val day = runCatching {
            CalendarService.of(date.year, date.monthValue, date.dayOfMonth, 12)
        }.getOrNull() ?: return
        val hexagram = runCatching { HexagramTable.of(record.hexagramCode).hexagram }.getOrNull() ?: return

        val result = DivinationResult(
            hexagram = hexagram,
            movingIndexes = record.movingIndexes.toSet(),
            method = DivinationMethod.COIN,
        )
        state = state.copy(
            question = record.question,
            day = day,
            result = result,
            viewingRecordId = record.id,
        )
        buildChart(result)
    }

    /** 批量导入卦例（已在界面层按 id 去重） */
    fun importRecords(list: List<DivinationRecord>) {
        if (list.isEmpty()) return
        viewModelScope.launch { list.forEach { recordStore.add(it) } }
    }

    /** 从历史卦例退回当前起卦（清掉历史标记，恢复今天的日课） */
    fun exitRecord() {
        state = state.copy(
            viewingRecordId = null,
            day = CalendarService.today(),
            result = null,
            chart = null,
            interpretation = null,
        )
    }

    private fun hourBranchOf(hour: Int): com.maohongyuan.magic.core.model.EarthlyBranch {
        // 子时 23:00–01:00 ⇒ (hour + 1) / 2 % 12
        val index = ((hour + 1) / 2) % 12
        return com.maohongyuan.magic.core.model.EarthlyBranch.entries[index]
    }
}
