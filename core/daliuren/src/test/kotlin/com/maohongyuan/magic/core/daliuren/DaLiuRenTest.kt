package com.maohongyuan.magic.core.daliuren

import com.maohongyuan.magic.core.model.EarthlyBranch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 大六壬测试。
 *
 * ⚠️ 三传的取法**没有权威课例对拍**，所以这里只断言结构不变量：
 * 天地盘必须是一个双射、天盘[时支] 必须等于月将、三传必须自洽。
 * 这些不变量一旦破了，盘一定是错的；但它们成立**不代表三传取得对**。
 */
class DaLiuRenTest {

    private fun chartOf(y: Int, m: Int, d: Int, h: Int) = DaLiuRenService.chart(y, m, d, h)

    // ── 月将 ────────────────────────────────────────────────

    @Test
    fun `任何日期都能取到月将`() {
        // 中气扫描应保证 32 天内必有一个中气
        for (m in 1..12) {
            for (d in listOf(1, 10, 20, 28)) {
                val c = chartOf(2026, m, d, 12)
                assertTrue("${m}月${d}日 月将异常", c.yueJiang.ordinal in 0..11)
                assertTrue("${m}月${d}日 中气为空", c.zhongQi.isNotBlank())
                assertTrue("${m}月${d}日 落到兜底了", !c.zhongQi.contains("兜底"))
            }
        }
    }

    @Test
    fun `月将随中气变化`() {
        // 冬至后（1月初）应为丑将；夏至后（7月初）应为未将
        assertEquals(EarthlyBranch.CHOU, chartOf(2026, 1, 5, 12).yueJiang)
        assertEquals(EarthlyBranch.WEI, chartOf(2026, 7, 5, 12).yueJiang)
        // 春分后（4月初）戌将
        assertEquals(EarthlyBranch.XU, chartOf(2026, 4, 5, 12).yueJiang)
    }

    // ── 天地盘 ──────────────────────────────────────────────

    @Test
    fun `天盘是十二支的双射`() {
        for (h in 0..23 step 3) {
            val c = chartOf(2026, 6, 15, h)
            assertEquals("天盘长度不对", 12, c.tianPan.size)
            assertEquals("天盘有重复，不是双射", 12, c.tianPan.toSet().size)
            assertEquals(EarthlyBranch.entries.toSet(), c.tianPan.toSet())
        }
    }

    @Test
    fun `天盘在时支上等于月将`() {
        // 月将加时：月将正好压在时支之上
        for (h in 0..23 step 2) {
            val c = chartOf(2026, 6, 15, h)
            assertEquals(
                "时支 ${c.hourBranch.label} 之上的天盘应等于月将 ${c.yueJiang.label}",
                c.yueJiang,
                c.tianPan[c.hourBranch.ordinal],
            )
        }
    }

    // ── 四课 ────────────────────────────────────────────────

    @Test
    fun `四课齐备且上神合法`() {
        val c = chartOf(2026, 6, 15, 12)
        assertEquals(4, c.ke.size)
        assertEquals(listOf("一课", "二课", "三课", "四课"), c.ke.map { it.label })
        c.ke.forEach {
            assertTrue("${it.label} 上神越界", it.shang.ordinal in 0..11)
            assertTrue("${it.label} 下神为空", it.xiaGan != null || it.xiaZhi != null)
        }
        // 一课下神是日干，其余是地支
        assertTrue("一课下神应为日干", c.ke[0].xiaGan != null)
        assertTrue("二课下神应为地支", c.ke[1].xiaGan == null)
    }

    @Test
    fun `二课下神等于一课上神`() {
        val c = chartOf(2026, 6, 15, 12)
        assertEquals(c.ke[0].shang, c.ke[1].xiaZhi)
        assertEquals(c.ke[2].shang, c.ke[3].xiaZhi)
    }

    // ── 三传 ────────────────────────────────────────────────

    @Test
    fun `三传齐备且自洽`() {
        for (m in 1..12) {
            for (h in listOf(0, 6, 12, 18)) {
                val c = chartOf(2026, m, 15, h)
                assertEquals("三传应为三支", 3, c.sanChuan.size)
                c.sanChuan.forEach { assertTrue("三传越界", it.ordinal in 0..11) }
                // 中传 = 天盘[初传]，末传 = 天盘[中传]
                assertEquals("中传不等于初传的上神", c.tianPanOf(c.sanChuan[0]), c.sanChuan[1])
                assertEquals("末传不等于中传的上神", c.tianPanOf(c.sanChuan[1]), c.sanChuan[2])
                assertTrue("取法名不应为空", c.method.isNotBlank())
            }
        }
    }

    @Test
    fun `初传必来自四课的上神`() {
        for (m in 1..12) {
            val c = chartOf(2026, m, 15, 12)
            assertTrue(
                "初传 ${c.sanChuan[0].label} 不在四课上神里",
                c.ke.any { it.shang == c.sanChuan[0] },
            )
        }
    }

    @Test
    fun `同一天反复排盘结果一致`() {
        val a = chartOf(2026, 6, 15, 12)
        val b = chartOf(2026, 6, 15, 12)
        assertEquals(a.yueJiang, b.yueJiang)
        assertEquals(a.tianPan, b.tianPan)
        assertEquals(a.sanChuan, b.sanChuan)
        assertEquals(a.method, b.method)
    }
}
