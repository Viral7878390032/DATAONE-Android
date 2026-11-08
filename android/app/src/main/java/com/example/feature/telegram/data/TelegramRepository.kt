package com.example.feature.telegram.data

import com.example.feature.telegram.model.TelegramAuthState
import com.example.feature.telegram.model.TelegramChat
import com.example.feature.telegram.model.TelegramMessage
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
    val chats: StateFlow<List<TelegramChat>>
    val messages: StateFlow<List<TelegramMessage>>
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
    suspend fun refreshChats(): Result<Unit>
    suspend fun toggleChatPinned(chatId: Long, isPinned: Boolean): Result<Unit>
    suspend fun toggleChatMarkedUnread(chatId: Long, isMarkedAsUnread: Boolean): Result<Unit>
    suspend fun toggleChatArchived(chatId: Long, isArchived: Boolean): Result<Unit>
    fun loadChatDrafts(): Map<Long, String>
    fun saveChatDrafts(drafts: Map<Long, String>)
    fun clearChatDrafts()

    suspend fun openChat(chatId: Long): Result<Unit>
    suspend fun sendTextMessage(chatId: Long, text: String): Result<Unit>

    suspend fun sendPhotoMessage(chatId: Long, path: String, caption: String): Result<Unit>
    suspend fun sendVideoMessage(chatId: Long, path: String, caption: String): Result<Unit>
    suspend fun sendDocumentMessage(chatId: Long, path: String, caption: String): Result<Unit>

    suspend fun requestMediaDownload(fileId: Int): Result<Unit>

    /**
     * Sets API ID and Hash into secure Keystore storage.
     */
    suspend fun configureApiCredentials(apiId: Int, apiHash: String)
}
