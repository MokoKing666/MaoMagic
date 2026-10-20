package com.maohongyuan.magic.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 端上模型的清单约束。
 *
 * 上一版这里测的是「骁龙 8 Elite 用 NPU 版、天玑用 GPU 版」的选型树。
 * 换成 Qwen3.5-0.8B 后只有一个文件，选型树不存在了 ——
 * 剩下的风险是**清单本身写错**（文件名、大小、哈希、下载地址），
 * 那种错不会报错，只会让用户下 920 MB 然后校验失败。
 */
class ModelSelectionTest {

    private val only = LocalModel.QWEN35_08B

    @Test
    fun `任何机型都只推同一个模型`() {
        listOf(
            DeviceProfile("SM8750", "QTI"),
            DeviceProfile("MT6991", "MediaTek"),
            DeviceProfile("", ""),
        ).forEach { device ->
            val list = LocalModel.candidatesFor(device)
            assertEquals("候选应只有一项", 1, list.size)
            assertEquals(only, list.first())
        }
    }

    @Test
    fun `手动指定变体也只会拿到它自己`() {
        assertEquals(listOf(only), LocalModel.candidatesFor(DeviceProfile("", ""), only.name))
        // 名字写错时退回默认，而不是抛异常
        assertEquals(listOf(only), LocalModel.candidatesFor(DeviceProfile("", ""), "GPU"))
    }

    @Test
    fun `文件是文本版而不是 VL 版`() {
        // VL 版带 ViT，1.30 GB；本项目只用文本
        assertEquals("Qwen3.5-0.8B_int8.litertlm", only.fileName)
        assertTrue("不该出现 -VL", !only.fileName.contains("VL"))
    }

    @Test
    fun `大小与哈希已登记`() {
        assertEquals(964_566_992L, only.sizeBytes)
        assertNotNull("缺少 sha256，下载校验会退化成只比大小", only.sha256)
        assertEquals(64, only.sha256!!.length)
    }

    @Test
    fun `下载地址指向 Qwen3_5 仓库且带文件名`() {
        val url = only.downloadUrl
        assertTrue(url, url.contains("litert-community/Qwen3.5-0.8B"))
        assertTrue(url, url.endsWith("FilePath=${only.fileName}"))
    }

    @Test
    fun `摘要里带上模型名`() {
        assertTrue(DeviceProfile("SM8750", "QTI").summary.contains(only.label))
    }
}
