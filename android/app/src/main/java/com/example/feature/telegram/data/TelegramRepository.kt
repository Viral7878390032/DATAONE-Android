package com.example.feature.telegram.data

import com.example.feature.telegram.model.TelegramAuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * High-level repository interface for Telegram feature in DATAONE.
 * Follows the existing DATAONE Repository pattern (matching Supabase and Gmail modules).
 */
interface TelegramRepository {
    /**
     * Reactive stream of authorization state for UI observation.
     */
    val authState: StateFlow<TelegramAuthState>

    /**
     * Flag indicating whether TDLib native C++ binaries are loaded.
     */
    val isNativeLibraryAvailable: Boolean

    /**
     * Initializes the TDLib engine with Keystore-backed database encryption.
     */
    suspend fun startTelegram()

    /**
     * Submits phone number in international format (+1...).
     */
    suspend fun submitPhoneNumber(phoneNumber: String): Result<Unit>

    /**
     * Submits authentication code received via Telegram/SMS.
     */
    suspend fun submitAuthCode(code: String): Result<Unit>

    /**
     * Submits Two-Step Verification 2FA cloud password if requested.
     */
    suspend fun submit2FaPassword(password: String): Result<Unit>

    /**
     * Submits first and last name for new user accounts.
     */
    suspend fun submitRegistration(firstName: String, lastName: String): Result<Unit>

    /**
     * Disconnects and logs out user, wiping session encryption keys.
     */
    suspend fun logout(): Result<Unit>

    /**
     * Downloads specific media file on demand (strictly no auto-download).
     */
    suspend fun requestMediaDownload(fileId: Int): Result<Unit>

    /**
     * Sets API ID and Hash into secure Keystore storage.
     */
    suspend fun configureApiCredentials(apiId: Int, apiHash: String)
}
