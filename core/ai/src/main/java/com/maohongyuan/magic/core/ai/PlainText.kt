package com.maohongyuan.magic.core.ai

/**
 * 把模型输出的 Markdown 压成纯文本。
 *
 * ── 为什么要做 ──
 * 小模型（尤其 Gemma 系列）默认倾向输出 Markdown：`**加粗**`、`## 标题`、`- 列表`。
 * 我们的解读是**整段渲染进一个 Text 组件**，不做 Markdown 解析 ——
 * 结果用户看到一堆 `**` 和 `##`，像是给开发者看的东西。
 *
 * 两种解法：
 * 1. 提示词里禁止 Markdown —— 有效但不保险，模型经常不听话
 * 2. **在客户端兜底清洗** —— 无论模型输出什么，用户看到的都是干净文本
 *
 * 这里两者都做：提示词先说，清洗再兜底。
 * 只做提示词不够可靠，只做清洗又会让模型白费 token 写 Markdown。
 */
internal object PlainText {

    /** 把 Markdown 标记去掉，保留语义与换行 */
    fun sanitize(raw: String): String {
        var s = raw

        // 代码块围栏 ```xxx ... ``` —— 直接去掉围栏行，内容保留
        s = s.replace(Regex("(?m)^\\s*```[a-zA-Z]*\\s*$"), "")

        // 标题：### 标题 → 标题
        s = s.replace(Regex("(?m)^\\s{0,3}#{1,6}\\s*"), "")

        // 列表符号：- / * / + → 保留内容，前面换成「· 」
        s = s.replace(Regex("(?m)^\\s{0,3}[-*+]\\s+"), "· ")

        // 有序列表 1. 2. → 保留
        s = s.replace(Regex("(?m)^\\s{0,3}\\d+[.)]\\s+"), "· ")

        // 加粗 / 斜体 / 行内代码
        s = s.replace("**", "").replace("__", "")
        s = s.replace(Regex("`([^`]*)`"), "$1")
        s = s.replace(Regex("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)"), "$1")

        // 引用 > 前缀
        s = s.replace(Regex("(?m)^\\s{0,3}>\\s?"), "")

        // 分隔线 --- / *** / ___
        s = s.replace(Regex("(?m)^\\s{0,3}([-*_])\\1{2,}\\s*$"), "")

        // 链接 [文字](url) → 文字
        s = s.replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")

        // 连续空行压成一个
        s = s.replace(Regex("\\n{3,}"), "\n\n")

        return s.trim()
    }
}
