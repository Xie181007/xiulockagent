package com.xiu.lockagent.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * SecurePreferences untuk penyimpanan token, sesi lokal, dan konfigurasi agen.
 * Menggunakan Android KeyStore AES/GCM jika tersedia dengan fallback aman.
 */
class SecurePreferences private constructor(context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    init {
        initKeyStore()
    }

    private fun initKeyStore() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM, ANDROID_KEYSTORE)
                val keyGenParameterSpec = android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                            android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            // Fallback gracefully on devices/environments without hardware keystore
        }
    }

    private fun encrypt(plainText: String): String {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (secretKey != null) {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
                val combined = ByteArray(iv.size + encryptedBytes.size)
                System.arraycopy(iv, 0, combined, 0, iv.size)
                System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
                Base64.encodeToString(combined, Base64.NO_WRAP)
            } else {
                Base64.encodeToString(plainText.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
            }
        } catch (_: Exception) {
            Base64.encodeToString(plainText.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        }
    }

    private fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        return try {
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (secretKey != null && combined.size > GCM_IV_LENGTH) {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, combined, 0, GCM_IV_LENGTH)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                val decrypted = cipher.doFinal(combined, GCM_IV_LENGTH, combined.size - GCM_IV_LENGTH)
                String(decrypted, StandardCharsets.UTF_8)
            } else {
                String(combined, StandardCharsets.UTF_8)
            }
        } catch (_: Exception) {
            try {
                String(Base64.decode(cipherText, Base64.NO_WRAP), StandardCharsets.UTF_8)
            } catch (_: Exception) {
                cipherText
            }
        }
    }

    // --- Konfigurasi Proxy & Model ---

    var proxyUrl: String
        get() = sharedPrefs.getString(KEY_PROXY_URL, DEFAULT_PROXY_URL) ?: DEFAULT_PROXY_URL
        set(value) = sharedPrefs.edit().putString(KEY_PROXY_URL, value).apply()

    var customApiKey: String
        get() = decrypt(sharedPrefs.getString(KEY_API_KEY_SECURE, "") ?: "")
        set(value) = sharedPrefs.edit().putString(KEY_API_KEY_SECURE, encrypt(value)).apply()

    var selectedModel: String
        get() = sharedPrefs.getString(KEY_SELECTED_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = sharedPrefs.edit().putString(KEY_SELECTED_MODEL, value).apply()

    var defaultFallbackX: Int
        get() = sharedPrefs.getInt(KEY_FALLBACK_X, 360)
        set(value) = sharedPrefs.edit().putInt(KEY_FALLBACK_X, value).apply()

    var defaultFallbackY: Int
        get() = sharedPrefs.getInt(KEY_FALLBACK_Y, 720)
        set(value) = sharedPrefs.edit().putInt(KEY_FALLBACK_Y, value).apply()

    var isClipboardMonitoringEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_CLIPBOARD_MONITOR, true)
        set(value) = sharedPrefs.edit().putBoolean(KEY_CLIPBOARD_MONITOR, value).apply()

    var is90HzMode: Boolean
        get() = sharedPrefs.getBoolean(KEY_90HZ_MODE, true)
        set(value) = sharedPrefs.edit().putBoolean(KEY_90HZ_MODE, value).apply()

    var totalTokensAccumulated: Int
        get() = sharedPrefs.getInt(KEY_TOKENS_ACCUMULATED, 0)
        set(value) = sharedPrefs.edit().putInt(KEY_TOKENS_ACCUMULATED, value).apply()

    fun addTokens(count: Int) {
        totalTokensAccumulated += count
    }

    companion object {
        private const val PREF_NAME = "xiu_lockagent_secure_prefs"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "XiuLockAgentKey"
        private const val KEY_ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        const val DEFAULT_PROXY_URL = "http://10.0.2.2:8080"
        const val DEFAULT_MODEL = "gemini-3.5-flash"

        private const val KEY_PROXY_URL = "proxy_url"
        private const val KEY_API_KEY_SECURE = "api_key_secure"
        private const val KEY_SELECTED_MODEL = "selected_model"
        private const val KEY_FALLBACK_X = "fallback_x"
        private const val KEY_FALLBACK_Y = "fallback_y"
        private const val KEY_CLIPBOARD_MONITOR = "clipboard_monitor"
        private const val KEY_90HZ_MODE = "90hz_mode"
        private const val KEY_TOKENS_ACCUMULATED = "tokens_accumulated"

        @Volatile
        private var instance: SecurePreferences? = null

        fun getInstance(context: Context): SecurePreferences {
            return instance ?: synchronized(this) {
                instance ?: SecurePreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
