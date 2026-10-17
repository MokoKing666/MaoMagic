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

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "magic_settings")

/**
 * 设置持久化。
 *
 * 与 [DivinationRecordStore] 同样的取舍：DataStore Preferences + 单个 JSON 字符串键。
 * 设置项只有十来个字段、读写都是整份替换，用不着关系库。
 *
 * **读失败一律回退到默认值** —— 设置损坏不该让应用起不来。
 */
class SettingsStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val key = stringPreferencesKey("settings")

    val settings: Flow<MagicSettings> = context.settingsStore.data.map { prefs ->
        decode(prefs[key])
    }

    /** 原子更新：读当前值 → 变换 → 写回。 */
    suspend fun update(transform: (MagicSettings) -> MagicSettings) {
        context.settingsStore.edit { prefs ->
            val next = transform(decode(prefs[key]))
            prefs[key] = json.encodeToString(next)
        }
    }

    suspend fun reset() {
        context.settingsStore.edit { prefs -> prefs.remove(key) }
    }

    private fun decode(raw: String?): MagicSettings {
        if (raw.isNullOrBlank()) return MagicSettings.DEFAULT
        return runCatching { json.decodeFromString<MagicSettings>(raw) }
            .getOrDefault(MagicSettings.DEFAULT)
        // 迁移：engineTier 早期的默认值是 "RULE"，而那时根本没有 UI 能改它 ——
        // 所以存量值 "RULE" 一定是旧默认，不是用户选择。改回 AUTO，
        // 否则升级上来的用户会被强制锁死在基础模式。
        .let { if (it.engineTier == "RULE") it.copy(engineTier = "AUTO") else it }
    }
}
