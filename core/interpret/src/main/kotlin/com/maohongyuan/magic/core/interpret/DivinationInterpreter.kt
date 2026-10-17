package com.maohongyuan.magic.core.interpret

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

/**
 * 解卦引擎。
 *
 * 实现必须是**纯函数式的**：同样输入产生同样输出（Tier1 保证确定性；
 * Tier2/3 允许随机性，但不得修改输入）。
 */
interface DivinationInterpreter {

    val tier: EngineTier

    /**
     * 当前是否可用。
     *
     * Tier1 永远返回 true —— 这是降级链的终点。
     * Tier2 需要检查是否已配置 BaseURL/Key 且网络可达。
     * Tier3 需要检查模型是否已下载且运行时可用。
     */
    suspend fun isAvailable(): Boolean

    /**
     * 解读。
     *
     * 返回 [Flow] 是为了让 Tier2/3 能流式输出；Tier1 一次性 emit 一个结果。
     * 若本引擎无法处理该请求（例如六爻引擎收到梅花请求），应 emit 空 Flow。
     */
    fun interpret(request: InterpretationRequest): Flow<Interpretation>
}

/**
 * 降级链。
 *
 * 默认按 [EngineTier.chain] 自动尝试并降级。
 * 用户明确选择档位时严格使用该档，失败就返回错误，不静默切档。
 *
 * AUTO 可以静默降级；手动选择不能降级 —— 否则界面显示的模式与用户选择不一致。
 */
class InterpreterChain(
    private val interpreters: List<DivinationInterpreter>,
) {
    init {
        require(interpreters.isNotEmpty()) { "降级链不能为空" }
    }

    /**
     * 选出当前可用的引擎。按 [EngineTier.chain] 的优先级排序。
     */
    suspend fun resolve(): DivinationInterpreter {
        val ordered = interpreters.sortedBy { EngineTier.chain.indexOf(it.tier) }
        for (engine in ordered) {
            if (runCatching { engine.isAvailable() }.getOrDefault(false)) {
                return engine
            }
        }
        // 理论上不可达：Tier1 永远可用。保底再找一次，仍找不到则抛错以暴露配置问题。
        return ordered.lastOrNull { it.tier == EngineTier.RULE }
            ?: error("降级链缺少 Tier1 规则引擎 —— 这是配置错误，Tier1 必须始终存在")
    }

    /**
     * 依次尝试，返回第一个真正产出结果的引擎。
     *
     * 若某引擎声明可用但在执行时失败（如网络中断），会继续降级到下一个 ——
     * 「可用」只是入场券，执行仍可能失败。
     */
    suspend fun interpret(
        request: InterpretationRequest,
        /** 用户指定的档位。默认取全局偏好，null 则自动降级 */
        preferred: EngineTier? = InferencePrefs.preferredTier,
    ): Interpretation {
        val byPriority = interpreters.sortedBy { EngineTier.chain.indexOf(it.tier) }
        // 规则引擎**总是先算一遍**。
        // 它零依赖、瞬时完成，算出的用神/旺衰/旬空是确定性的。
        // 把这份结果作为 facts 交给 AI；AI 只做「说人话」，不再自己推卦理。
        val ruleEngine = byPriority.firstOrNull { it.tier == EngineTier.RULE }
        val facts = ruleEngine?.let { engine ->
            runCatching { engine.interpret(request).firstOrNull() }.getOrNull()
        }

        // 用户明确选档时，**不允许静默切到另一档**。
        // 否则用户选了「本地模型」，GPU/NPU 失败后界面却显示「基础模式」，
        // 既掩盖真实错误，也违背用户选择。让调用方拿到错误并明确展示。
        if (preferred != null) {
            val selected = byPriority.firstOrNull { it.tier == preferred }
                ?: error("当前没有注册「${preferred.label}」引擎")
            if (!runCatching { selected.isAvailable() }.getOrDefault(false)) {
                error("所选「${preferred.label}」不可用。请检查模型是否已下载、配置是否完整。")
            }
            val effective = if (selected.tier == EngineTier.RULE || facts == null) {
                request
            } else {
                request.copy(facts = facts)
            }
            val result = try {
                selected.interpret(effective).firstOrNull()
            } catch (t: Throwable) {
                throw IllegalStateException(
                    "所选「${preferred.label}」调用失败：${t.message ?: t.javaClass.simpleName}",
                    t,
                )
            }
            return result ?: error("所选「${preferred.label}」没有返回结果；未切换到其它模式。")
        }

        for (engine in byPriority) {
            if (!runCatching { engine.isAvailable() }.getOrDefault(false)) continue
            // 规则引擎自己就是算的那一方，不需要再喂一遍
            val effective = if (engine.tier == EngineTier.RULE || facts == null) {
                request
            } else {
                request.copy(facts = facts)
            }
            val result = runCatching {
                engine.interpret(effective).firstOrNull()
            }.getOrNull()
            if (result != null) return result
        }
        error("所有引擎均未能产出结果 —— Tier1 不应失败")
    }
}
