package com.maohongyuan.magic.core.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 敏感字符串存储（目前只用于 API Key）。
 *
 * ── 为什么不放 DataStore ──
 * DataStore 是明文的。API Key 是**用户的凭据**，泄露等于替别人烧钱，
 * 所以单独走 Android Keystore：密钥由系统 TEE/StrongBox 托管，
 * 应用只能「用」不能「读」，root 之外拿不走。
 *
 * ── 为什么不用 EncryptedSharedPreferences ──
 * `androidx.security:security-crypto` 已被官方标记废弃，不再维护。
 * 这里直接用 Keystore + AES-GCM，代码不到百行，没有额外依赖。
 *
 * ── 失败一律静默 ──
 * 换机、系统升级、Keystore 被清都可能让旧密文解不开。
 * 这时 [get] 返回空串 —— 效果是「Key 没了，需要重填」，
 * 而不是让应用崩溃。降级链会自动落到 Tier1。
 */
class SecretStore(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun put(key: String, value: String) {
        if (value.isEmpty()) {
            prefs.edit().remove(key).apply()
            return
        }
        runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            // IV 与密文拼在一起存 —— GCM 的 IV 每次加密都不同，必须随密文保存
            val packed = cipher.iv + encrypted
            prefs.edit().putString(key, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
        }
    }

    fun get(key: String): String {
        val stored = prefs.getString(key, null) ?: return ""
        return runCatching {
            val packed = Base64.decode(stored, Base64.NO_WRAP)
            val iv = packed.copyOfRange(0, IV_LENGTH)
            val body = packed.copyOfRange(IV_LENGTH, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(body), Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // 不要求用户认证：解卦时弹指纹会打断流程，
                // 而这里的威胁模型是「其他应用/备份泄露」，不是「设备被他人拿到」
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        /** API Key 的存储键 */
        const val KEY_CLOUD_API_KEY = "cloud_api_key"

        private const val PREFS = "magic_secrets"
        private const val ALIAS = "magic_secret_master"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_BITS = 128
    }
}
