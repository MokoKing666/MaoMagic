package com.maohongyuan.magic.core.resource

import kotlinx.serialization.json.Json

/**
 * 从 `src/main/resources` 读取 JSON。
 *
 * ── 路径规则 ──
 * 用**以 `/` 开头的绝对路径**，如 `"/tarot/cards.json"`。
 * 不要用相对路径 —— 相对路径是相对调用方所在包，换个调用点就找不到。
 *
 * ── 为什么失败就抛异常，而不是返回 null ──
 * 资源缺失属于**打包/开发期错误**，静默返回空数据会让界面显示一片空白，
 * 反而更难排查。这里抛出带路径的明确异常，由各引擎在边界处 `runCatching` 兜住
 * → 返回空 Flow → 降级链继续。**用户看到的永远不是崩溃。**
 *
 * ── Android 可读性 ──
 * 纯 JVM 模块的 `src/main/resources` 会被 AGP 打进 APK 的 Java 资源区，
 * 运行期经 `PathClassLoader` 可读。这一点在本项目**已用真机验证过**
 * （见 `:core:resource` 的探针与 `XiaoLiuRenTest`）。
 */
object JsonResource {

    // inline 的 load() 需要访问它，故标 @PublishedApi（仍是 internal，不进公开 API）
    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        // 资源里写默认值时不要求每个字段都出现，容忍后续加字段
        encodeDefaults = true
    }

    /**
     * 读取并反序列化。
     *
     * @param path 以 `/` 开头的资源绝对路径
     * @throws IllegalStateException 资源不存在或解析失败
     */
    inline fun <reified T> load(path: String): T {
        val text = readText(path)
        return json.decodeFromString(text)
    }

    /** 只读原始文本（调试与探针用） */
    @PublishedApi
    internal fun readText(path: String): String {
        require(path.startsWith("/")) { "资源路径必须以 / 开头，实际：$path" }
        val stream = JsonResource::class.java.getResourceAsStream(path)
            ?: error("资源缺失：$path（检查是否在 src/main/resources 下，且已被打进产物）")
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    /** 探针：返回资源是否存在，不抛异常。用于启动自检 */
    fun exists(path: String): Boolean =
        path.startsWith("/") && JsonResource::class.java.getResourceAsStream(path) != null
}
