package com.example.feature.telegram.bridge

import com.example.feature.telegram.model.TelegramAuthState
import com.example.feature.telegram.model.TelegramChat
import com.example.feature.telegram.model.TelegramMessage
import com.example.feature.telegram.model.TelegramMediaConfig
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Parameters required to bootstrap official Telegram TDLib.
 */
data class TelegramInitParams(
    val apiId: Int,
    val apiHash: String,
    val databaseDirectory: File,
    val filesDirectory: File,
    val databaseEncryptionKey: ByteArray,
    val useFileDatabase: Boolean = true,
    val useChatInfoDatabase: Boolean = true,
    val useMessageDatabase: Boolean = true,
    val enableStorageOptimizer: Boolean = true,
    val mediaConfig: TelegramMediaConfig = TelegramMediaConfig.STRICT_ON_DEMAND,
    val systemLanguageCode: String = "en",
    val deviceModel: String = "Android Device",
    val systemVersion: String = "Android",
    val applicationVersion: String = "1.0.0"
)

/**
 * Clean bridge interface separating DATAONE business logic from TDLib native C++/JNI bindings.
 *
 * This fulfills the requirement:
 * "Keep native TDLib integration behind a clean interface until its Android libraries are properly built."
 */
interface TelegramClientBridge {
    /**
     * Observable stream of TDLib authorization states.
     */
    val authState: StateFlow<TelegramAuthState>

    /**
     * Indicates whether the native TDLib C++ binary (libtdjni.so) is compiled and loaded.
     */
    val chats: StateFlow<List<TelegramChat>>
    val messages: StateFlow<List<TelegramMessage>>
    val isNativeLibraryAvailable: Boolean

    /**
     * Initializes TDLib client instance and begins listener thread.
     */
    suspend fun initialize(params: TelegramInitParams): Result<Unit>

    /**
     * Sends phone number to TDLib setAuthenticationPhoneNumber.
     */
    suspend fun sendPhoneNumber(phoneNumber: String): Result<Unit>

    /**
     * Sends verification code to TDLib checkAuthenticationCode.
     */
    suspend fun sendAuthCode(code: String): Result<Unit>

    /**
     * Sends 2FA cloud password to TDLib checkAuthenticationPassword.
     */
    suspend fun sendPassword(password: String): Result<Unit>

    /**
     * Sends user registration data to TDLib registerUser.
     */
    suspend fun sendRegistration(firstName: String, lastName: String): Result<Unit>

    /**
     * Initiates media download on demand for a specific Telegram file ID.
     * Enforces the zero-auto-download requirement.
     */
    suspend fun refreshChats(): Result<Unit>

    suspend fun openChat(chatId: Long): Result<Unit>
    suspend fun sendTextMessage(chatId: Long, text: String): Result<Unit>
    suspend fun sendPhotoMessage(chatId: Long, path: String, caption: String): Result<Unit>
    suspend fun sendVideoMessage(chatId: Long, path: String, caption: String): Result<Unit>
    suspend fun sendDocumentMessage(chatId: Long, path: String, caption: String): Result<Unit>

    suspend fun downloadFileOnDemand(fileId: Int, priority: Int = 1): Result<Unit>

    /**
     * Gracefully logs out from TDLib and revokes authorization.
     */
    suspend fun logout(): Result<Unit>

    /**
     * Closes TDLib client instance and releases native memory.
     */
    suspend fun close(): Result<Unit>
}
