package com.maohongyuan.magic.core.interpret

/**
 * 解卦的「说话风格」。
 *
 * ── 为什么单独抽出来 ──
 * 早期风格是写死在 [ChartPrompt] 的输出契约里的（曾仕强那一套）。
 * 现在要能切换，就必须把它从「契约」里拆出来，只替换【怎么说话】那一段 ——
 * 直说、不用术语、建议能落地、JSON 结构这些**硬要求对所有风格都成立**，
 * 不能因为换风格就丢掉。
 *
 * ── 为什么只影响 AI 两档 ──
 * 规则引擎（Tier1）的文案是各系统自己写死的，不受这里影响。
 * 所以没配 Key、也没下载模型时，换风格看不出变化 —— 设置页有说明。
 *
 * ── 放在 interpret 而不是 data ──
 * 依赖方向是 `ai → interpret`、`data → model`，两边都能看到 interpret。
 * 持久化只存 [name] 字符串，避免将来改枚举名炸掉旧数据。
 */
enum class InterpretationStyle(val label: String, val description: String) {

    /** 默认。讲了一辈子《易经》的老先生，当面讲话。 */
    ZENG_SHIQIANG(
        "曾仕强",
        "像讲了一辈子《易经》的老先生当面跟你讲话：家常话、打比方、有话直说、最后给一句叮嘱。",
    ),

    /**
     * 老子视角。取自 chmxn/laozi-perspective（MIT）的思维框架，按端上小模型重写。
     *
     * ⚠️ 只搬了「怎么看」和「怎么说」，没搬它的 Agentic 联网研究流程 ——
     * 端上模型没有网络，那一整套 WebSearch 步骤在这个应用里是死代码。
     */
    LAOZI(
        "老子视角",
        "用《道德经》的镜片看盘：反者道之动、无为、柔弱胜刚强、知足知止。短句、对偶、留白。",
    ),
    ;

    companion object {
        fun of(name: String?): InterpretationStyle =
            entries.firstOrNull { it.name == name } ?: ZENG_SHIQIANG
    }
}
