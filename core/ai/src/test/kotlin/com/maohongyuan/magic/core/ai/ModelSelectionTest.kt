package com.maohongyuan.magic.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 机型 → 模型 的选型测试。
 *
 * 这是整条链路最容易出错、后果最严重的一环：
 * 选错不会报错，只会让用户下 2.9 GB 然后发现跑不起来。
 */
class ModelSelectionTest {

    private fun pick(soc: String, vendor: String) =
        LocalModel.candidatesFor(DeviceProfile(soc, vendor)).first()

    @Test
    fun `骁龙 8 Elite 用 NPU 专用版`() {
        assertEquals(LocalModel.QUALCOMM_SM8750, pick("SM8750", "QTI"))
    }

    @Test
    fun `骁龙 8 Elite 的变体代号也认`() {
        assertEquals(LocalModel.QUALCOMM_SM8750, pick("SM8750P", "QTI"))
        assertEquals(LocalModel.QUALCOMM_SM8750, pick("sm8750-ab", "QTI"))
    }

    @Test
    fun `更新的骁龙不走 NPU 版 —— 没有对应模型文件`() {
        // 8 Elite Gen 5 / Gen 6 都是高通，但官方没有发布对应的 .litertlm。
        // NPU 模型不向前兼容，硬用只会初始化失败。
        assertEquals(LocalModel.GPU, pick("SM8850", "QTI"))
        assertEquals(LocalModel.GPU, pick("SM8950", "QTI"))
    }

    @Test
    fun `天玑走 GPU 版而不是通用版`() {
        // 「通用版」是 CPU 兜底，不是「普通手机版」。
        // 天玑的 Mali GPU 跑 GPU delegate 比 CPU 快得多。
        assertEquals(LocalModel.GPU, pick("MT6991", "MediaTek"))
    }

    @Test
    fun `SoC 读不到时安全退回 GPU`() {
        assertEquals(LocalModel.GPU, pick("", ""))
    }

    @Test
    fun `候选列表末尾一定有通用版兜底`() {
        val list = LocalModel.candidatesFor(DeviceProfile("MT6991", "MediaTek"))
        assertEquals(LocalModel.GENERIC, list.last())
        assertTrue(list.size >= 2)
    }

    @Test
    fun `只有骁龙 8E 的候选里才出现 NPU 版`() {
        val snap = LocalModel.candidatesFor(DeviceProfile("SM8750", "QTI"))
        val mtk = LocalModel.candidatesFor(DeviceProfile("MT6991", "MediaTek"))
        assertTrue(snap.contains(LocalModel.QUALCOMM_SM8750))
        assertFalse(mtk.contains(LocalModel.QUALCOMM_SM8750))
    }

    @Test
    fun `骁龙标记不因高通平台而误判`() {
        assertTrue(DeviceProfile("SM8850", "QTI").isQualcomm)
        assertFalse(DeviceProfile("SM8850", "QTI").isSnapdragon8Elite)
        assertTrue(DeviceProfile("SM8750", "QTI").isSnapdragon8Elite)
    }
}
