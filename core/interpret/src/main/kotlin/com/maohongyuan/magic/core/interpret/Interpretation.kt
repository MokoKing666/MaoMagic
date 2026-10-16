package com.maohongyuan.magic.core.interpret

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
    val liuYao: com.maohongyuan.magic.core.hexagram.LiuYaoChart? = null,
    val meihua: com.maohongyuan.magic.core.hexagram.MeihuaChart? = null,
    val bazi: com.maohongyuan.magic.core.calendar.BaZiChart? = null,
    /**
     * 月建地支。
     *
     * 六爻盘内已含月建，无需传；**梅花必须传**，否则无法判断体卦旺衰，
     * 引擎会跳过旺衰部分并在结果里说明。
     */
    val monthBranch: com.maohongyuan.magic.core.model.EarthlyBranch? = null,
) {
    init {
        require(listOfNotNull(liuYao, meihua, bazi).size == 1) {
            "一次只能解一个盘，实际收到 ${listOfNotNull(liuYao, meihua, bazi).size} 个"
        }
    }
}
