package com.maohongyuan.magic.core.resource

import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 资源基建的自检。
 *
 * ── 这组测试的意义 ──
 * 「纯 JVM 模块的 `src/main/resources` 能被读到」这条，在 JVM 单测里一定能过
 * （Gradle 会把 resources 放进 test runtime classpath）。
 *
 * 真正的不确定性在 **Android 运行期**：APK 打包后还能不能用
 * `getResourceAsStream` 读到 —— 那一条只能靠真机验证，见 `MainActivity` 里的启动探针。
 * 本测试只保证「加载器本身逻辑正确」。
 */
class JsonResourceTest {

    @Serializable
    private data class Probe(
        val schemaVersion: Int = 0,
        val note: String = "",
        val expected: String = "",
    )

    @Test
    fun `能读到并解析 JSON 资源`() {
        val probe = JsonResource.load<Probe>("/probe.json")
        assertEquals("resource-ok", probe.expected)
        assertEquals(1, probe.schemaVersion)
    }

    @Test
    fun `exists 对存在的资源返回 true`() {
        assertTrue(JsonResource.exists("/probe.json"))
    }

    @Test
    fun `exists 对不存在的资源返回 false 而不是抛异常`() {
        assertTrue(!JsonResource.exists("/nope/does-not-exist.json"))
    }

    @Test
    fun `路径必须以斜杠开头`() {
        val e = runCatching { JsonResource.readText("probe.json") }.exceptionOrNull()
        assertTrue("相对路径应当被拒绝", e is IllegalArgumentException)
    }

    @Test
    fun `资源缺失时抛的是带路径的明确异常`() {
        val e = runCatching { JsonResource.readText("/missing.json") }.exceptionOrNull()
        assertTrue(e is IllegalStateException)
        assertTrue("异常信息里应包含路径，方便排查", e?.message?.contains("/missing.json") == true)
    }
}
