package com.maohongyuan.magic.core.data

import kotlinx.serialization.Serializable

/**
 * 一条卦例记录。
 *
 * **只存必要字段，不存解读全文** —— 解读可由卦象与日辰完全复现
 * （Tier1 是纯规则引擎，同样输入必得同样输出），存全文既浪费空间，
 * 又会在规则演进后与实际结果不一致。
 */
@Serializable
data class DivinationRecord(
    /** 记录 id。用创建时间戳的字符串形式，天然有序且无需额外生成器。 */
    val id: String,
    val createdAt: Long,
    /** 起卦方式标签（铜钱 / 时间 / 数字 / 手动） */
    val method: String,
    val question: String,
    /** 本卦编码 0..63 */
    val hexagramCode: Int,
    /** 动爻位置，0 = 初爻 */
    val movingIndexes: List<Int>,
    /** 日辰干支序号 0..59 */
    val dayGanZhiIndex: Int,
    /** 月建地支序号 0..11 */
    val monthBranchOrdinal: Int,
    /** 吉凶结论（可空 —— 旧记录可能没有） */
    val verdict: String? = null,
    /** 一句话结论 */
    val headline: String? = null,
) {
    companion object {
        /** 最多保留的记录数。超出后按时间淘汰最旧的。 */
        const val MAX_RECORDS = 200
    }
}
