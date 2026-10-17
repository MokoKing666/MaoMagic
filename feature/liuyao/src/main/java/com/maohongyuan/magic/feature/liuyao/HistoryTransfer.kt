package com.maohongyuan.magic.feature.liuyao

import com.maohongyuan.magic.core.data.DivinationRecord
import org.json.JSONArray
import org.json.JSONObject

/**
 * 卦例的导出与导入。
 *
 * ── 为什么要有这个 ──
 * 卦例只存本机（`Preferences DataStore`），**换手机就全丢**。
 * 术数用户会攒卦例、隔段时间回看，这个损失不可逆，所以必须有导出。
 *
 * ── 为什么手写 JSON 而不用 kotlinx.serialization ──
 * `DivinationRecord` 是走 Preferences 存 key-value 的，没有 `@Serializable`。
 * 为了导出去给它加注解、再引入序列化配置，成本高于手写这几十行。
 * 手写还有个好处：字段缺失时能逐项兜底，不会整批失败。
 */
object HistoryTransfer {

    /** 导出格式版本。将来字段变动时，靠它判断要不要迁移。 */
    private const val VERSION = 1

    fun toJson(records: List<DivinationRecord>): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("app", "maoxianhuayu")
        root.put("exportedAt", System.currentTimeMillis())
        val arr = JSONArray()
        records.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("createdAt", r.createdAt)
                    put("method", r.method)
                    put("question", r.question)
                    put("hexagramCode", r.hexagramCode)
                    put("movingIndexes", JSONArray(r.movingIndexes))
                    put("dayGanZhiIndex", r.dayGanZhiIndex)
                    put("monthBranchOrdinal", r.monthBranchOrdinal)
                    r.verdict?.let { put("verdict", it) }
                    r.headline?.let { put("headline", it) }
                }
            )
        }
        root.put("records", arr)
        return root.toString(2)
    }

    /**
     * 解析导入文件。
     *
     * **单条坏数据不该让整批失败** —— 跳过并计数，把坏了几条告诉用户。
     * 旧版本导出的文件可能缺字段，用默认值兜底。
     */
    fun fromJson(text: String): kotlin.Result<Parsed> {
        return runCatching {
            val root = JSONObject(text)
            val arr = root.optJSONArray("records") ?: JSONArray()
            val out = ArrayList<DivinationRecord>(arr.length())
            var skipped = 0
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i)
                if (o == null) {
                    skipped++
                    continue
                }
                val record = runCatching {
                    val moving = o.optJSONArray("movingIndexes") ?: JSONArray()
                    DivinationRecord(
                        id = o.getString("id"),
                        createdAt = o.getLong("createdAt"),
                        method = o.optString("method", ""),
                        question = o.optString("question", ""),
                        hexagramCode = o.getInt("hexagramCode"),
                        movingIndexes = (0 until moving.length()).map { moving.getInt(it) },
                        dayGanZhiIndex = o.getInt("dayGanZhiIndex"),
                        monthBranchOrdinal = o.getInt("monthBranchOrdinal"),
                        verdict = o.optString("verdict").ifBlank { null },
                        headline = o.optString("headline").ifBlank { null },
                    )
                }.getOrNull()
                if (record == null) skipped++ else out += record
            }
            Parsed(out, skipped)
        }
    }

    /** 导入结果：成功的记录 + 跳过的条数 */
    data class Parsed(val records: List<DivinationRecord>, val skipped: Int)
}
