package com.maohongyuan.magic.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.recordStore: DataStore<Preferences> by preferencesDataStore(name = "divination_records")

/**
 * 卦例历史记录。
 *
 * 存储：DataStore Preferences，把记录列表序列化成 JSON 存一个字符串键。
 * 之所以不引 Room —— 记录量上限只有 200 条、且只做「追加 + 全量读 + 删除」，
 * 用不着关系库，少一个依赖少一层构建负担。
 *
 * 全部操作都是**纯本地、零网络**，符合「不收集任何个人信息」的承诺。
 */
class DivinationRecordStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val key = stringPreferencesKey("records")

    val records: Flow<List<DivinationRecord>> = context.recordStore.data.map { prefs ->
        decode(prefs[key])
    }

    /**
     * 追加一条记录。超出 [DivinationRecord.MAX_RECORDS] 时淘汰最旧的。
     */
    suspend fun add(record: DivinationRecord) {
        context.recordStore.edit { prefs ->
            val current = decode(prefs[key])
            val next = (listOf(record) + current)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }
                .take(DivinationRecord.MAX_RECORDS)
            prefs[key] = json.encodeToString(next)
        }
    }

    suspend fun delete(id: String) {
        context.recordStore.edit { prefs ->
            val next = decode(prefs[key]).filterNot { it.id == id }
            prefs[key] = json.encodeToString(next)
        }
    }

    suspend fun clear() {
        context.recordStore.edit { prefs -> prefs.remove(key) }
    }

    /**
     * 解码。**任何异常都退化为空列表** ——
     * 历史记录损坏不应该让整个页面崩掉，最坏也只是丢掉历史。
     */
    private fun decode(raw: String?): List<DivinationRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<DivinationRecord>>(raw)
        }.getOrDefault(emptyList())
    }
}
