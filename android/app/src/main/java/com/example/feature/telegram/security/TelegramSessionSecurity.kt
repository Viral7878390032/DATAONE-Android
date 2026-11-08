package com.example.feature.telegram.security

import android.content.Context
import com.example.dataone.BuildConfig
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import java.util.Base64

/**
 * Handles encrypted storage for Telegram session secrets and TDLib database encryption key.
 *
 * TDLib encrypts its internal SQLite database using a 256-bit key (database_encryption_key).
 * This class creates, securely persists in Android Keystore via EncryptedSharedPreferences,
 * and supplies this key.
 *
 * CRITICAL RULE: Telegram API credentials (api_id, api_hash) and session secrets are NEVER
 * hardcoded in source code.
 */
class TelegramSessionSecurity(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Retrieves or generates a cryptographically secure 256-bit key for TDLib database encryption.
     * Returned as ByteArray to be passed directly to TDLib SetTdlibParameters / CheckDatabaseEncryptionKey.
     */
    @Synchronized
    fun getOrCreateDatabaseEncryptionKey(): ByteArray {
        val existingBase64 = encryptedPrefs.getString(KEY_TDLIB_DB_ENCRYPTION_KEY, null)
        if (existingBase64 != null) {
            return Base64.getDecoder().decode(existingBase64)
        }

        // Generate 32 bytes (256-bit) random key
        val secureRandom = SecureRandom()
        val keyBytes = ByteArray(32)
        secureRandom.nextBytes(keyBytes)

        val encoded = Base64.getEncoder().encodeToString(keyBytes)
        encryptedPrefs.edit()
            .putString(KEY_TDLIB_DB_ENCRYPTION_KEY, encoded)
            .apply()

        return keyBytes
    }

    /**
     * Retrieves the configured Telegram API ID.
     * Can be set via BuildConfig (injected from local.properties/CI secrets) or entered in developer settings.
     */
    fun getTelegramApiId(): Int? {
        val bundled = BuildConfig.TELEGRAM_API_ID
        if (bundled > 0) return bundled
        val stored = encryptedPrefs.getInt(KEY_API_ID, 0)
        return if (stored > 0) stored else null
    }

    /**
     * Retrieves the configured Telegram API Hash.
     */
    fun getTelegramApiHash(): String? {
        val encoded = BuildConfig.TELEGRAM_API_HASH_BASE64
        if (encoded.isNotBlank()) return String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
        val stored = encryptedPrefs.getString(KEY_API_HASH, null)
        return if (!stored.isNullOrBlank()) stored else null
    }

    /**
     * Safely stores API credentials if supplied at runtime (e.g. from developer setup or backend).
     */
    fun storeApiCredentials(apiId: Int, apiHash: String) {
        encryptedPrefs.edit()
            .putInt(KEY_API_ID, apiId)
            .putString(KEY_API_HASH, apiHash)
            .apply()
    }

    /**
     * Clears all session keys, forcing a fresh authorization flow.
     * Called on user logout or session revocation.
     */
    fun clearSession() {
        encryptedPrefs.edit()
            .remove(KEY_TDLIB_DB_ENCRYPTION_KEY)
            .remove(KEY_API_ID)
            .remove(KEY_API_HASH)
            .apply()
    }

    companion object {
        private const val SECURE_PREFS_NAME = "dataone_telegram_secure_prefs"
        private const val KEY_TDLIB_DB_ENCRYPTION_KEY = "tdlib_db_encryption_key_v1"
        private const val KEY_API_ID = "telegram_api_id"
        private const val KEY_API_HASH = "telegram_api_hash"
    }
}
