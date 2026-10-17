package com.maohongyuan.magic.core.interpret

import com.maohongyuan.magic.core.model.DivinationChart

/**
 * 解卦引擎档位。
 *
 * 三档构成**降级链**：`ON_DEVICE → CLOUD_AI → RULE`。
 * 任何一档不可用都会自动降级，**最终必定落到 [RULE]** —— 它零网络、零模型依赖，
 * 因此永远可用。这是「离线可用」这条产品承诺的技术保证。
 */
enum class EngineTier(val label: String, val description: String) {
    /** Tier 3：端上本地模型（LiteRT-LM），需用户自行下载模型 */
    ON_DEVICE("本地模型", "端上模型 · 需下载"),

    /** Tier 2：云端 OpenAI 兼容 API，需用户自填 BaseURL 与 Key */
    CLOUD_AI("智能模式", "云端 AI · 需联网"),

    /** Tier 1：本地规则引擎，离线兜底 */
    RULE("基础模式", "本地规则引擎 · 离线可用"),
    ;

    /** 降级链顺序：越靠前越优先 */
    companion object {
        val chain: List<EngineTier> = listOf(ON_DEVICE, CLOUD_AI, RULE)
    }
}

/** 吉凶倾向 */
enum class Verdict(val label: String) {
    AUSPICIOUS("吉"),
    INAUSPICIOUS("凶"),
    NEUTRAL("平"),
    MIXED("吉中带阻"),
}

/** 段落的语气，供 UI 上色（走语义色 + 字形双通道，不只靠颜色） */
enum class Tone {
    POSITIVE,
    NEGATIVE,
    WARNING,
    NEUTRAL,
}

/** 一段解读 */
data class InterpretationSection(
    val title: String,
    val body: String,
    val tone: Tone = Tone.NEUTRAL,
)

/**
 * 解读结果。
 */
data class Interpretation(
    val verdict: Verdict,
    /** 一句话结论 */
    val headline: String,
    val sections: List<InterpretationSection>,
    /** 产出本结果的引擎档位 —— 必须如实上报，UI 要展示给用户 */
    val tier: EngineTier,
    /** 可选的用神说明（六爻） */
    val yongShenNote: String? = null,
) {
    companion object {
        /** 免责声明，所有结果都必须带上 */
        const val DISCLAIMER: String =
            "术数内容仅供传统文化研究与娱乐，不构成任何决策建议。"
    }
}

/**
 * 解读请求。
 *
 * 三个盘互斥：一次只解一个。
 */
data class InterpretationRequest(
    /** 所问何事。**越具体，断卦越有针对性** */
    val question: String,
    /**
     * 要解读的盘。**唯一**，不再三选一。
     *
     * 早期用 `liuYao?/meihua?/bazi?` 三个可空字段加 `require(恰好 1 个)`，
     * 每新增一个术数系统都要改这个约束 —— 扩到 12 个系统时不可维护。
     * 现改为统一的 [DivinationChart] 标记接口，各引擎自行 `as?` 判断能否处理。
     */
    val chart: DivinationChart,
    /**
     * 占卜者性别。
     *
     * 六爻断婚姻/感情，**男占以妻财为用神，女占以官鬼为用神** ——
     * 取错则整卦的用神都错。其他事类不受影响。
     */
    val gender: com.maohongyuan.magic.core.calendar.Gender =
        com.maohongyuan.magic.core.calendar.Gender.MALE,
    /**
     * **算法已算定的事实**（规则引擎的输出）。
     *
     * AI 引擎拿到它之后，**只负责把这些事实说成人话，不得重新推算卦理**。
     *
     * 为什么必须这么做：用神取谁、旺衰如何、是否旬空、哪爻动 ——
     * 这些是确定性计算，规则引擎不会错。让 AI 从原始盘面自己推，它会给
     * 「用神旺相」这种听起来对、实际是「囚」的结论，而且编得很有说服力。
     *
     * 为 null 表示调用方没提供（例如直接调 AI 引擎做测试），
     * 此时 AI 只能看到盘面 —— 输出质量会下降，但不该崩。
     */
    val facts: Interpretation? = null,
    /**
     * 月建地支。
     *
     * 六爻盘内已含月建，无需传；**梅花必须传**，否则无法判断体卦旺衰，
     * 引擎会跳过旺衰部分并在结果里说明。
     */
    val monthBranch: com.maohongyuan.magic.core.model.EarthlyBranch? = null,
)
