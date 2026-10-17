package com.maohongyuan.magic.core.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.maohongyuan.magic.core.fortune.BirthProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.birthProfileDataStore by preferencesDataStore(name = "birth_profile")

/**
 * 命主档案的存储。
 *
 * 单份对象，照 [SettingsStore] 的模式：一个 key 存整份 JSON。
 * 没有档案时返回 `null` —— 调用方用空态引导填写，而不是拿一个默认生辰去算。
 *
 * ⚠️ 这里的生辰是**公历**，与八字页的输入口径一致。
 */
class BirthProfileStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val profile: Flow<BirthProfile?> = context.birthProfileDataStore.data.map { prefs ->
        decode(prefs[KEY])
    }

    suspend fun save(profile: BirthProfile) {
        context.birthProfileDataStore.edit { it[KEY] = json.encodeToString(profile) }
    }

    suspend fun clear() {
        context.birthProfileDataStore.edit { it.remove(KEY) }
    }

    private fun decode(raw: String?): BirthProfile? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<BirthProfile>(raw) }.getOrNull()
    }

    private companion object {
        val KEY: Preferences.Key<String> = stringPreferencesKey("profile")
    }
}
