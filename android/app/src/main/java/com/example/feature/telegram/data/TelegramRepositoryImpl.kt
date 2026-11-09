package com.example.feature.telegram.data

import android.content.Context
import android.os.Build
import com.example.feature.telegram.bridge.NativeTdLibBridge
import com.example.feature.telegram.bridge.TelegramClientBridge
import com.example.feature.telegram.bridge.TelegramInitParams
import com.example.feature.telegram.model.TelegramAuthState
import com.example.feature.telegram.model.TelegramChat
import com.example.feature.telegram.model.TelegramChatDetails
import com.example.feature.telegram.model.TelegramMessage
import com.example.feature.telegram.model.TelegramUser
import com.example.feature.telegram.model.TelegramMediaConfig
import com.example.feature.telegram.security.TelegramSessionSecurity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import org.json.JSONObject

/**
 * Concrete implementation of [TelegramRepository] for DATAONE.
 *
 * Adheres to:
 * 1. Encrypted local session storage (Android Keystore + EncryptedSharedPreferences).
 * 2. Strict on-demand media loading (zero auto-download).
 * 3. Independent module coexistence with Gmail and Supabase.
 */
class TelegramRepositoryImpl(
    private val context: Context,
    private val security: TelegramSessionSecurity = TelegramSessionSecurity(context),
    private val clientBridge: TelegramClientBridge = NativeTdLibBridge(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TelegramRepository {

    override val authState: StateFlow<TelegramAuthState> = clientBridge.authState
    override val chats: StateFlow<List<TelegramChat>> = clientBridge.chats
    override val messages: StateFlow<List<TelegramMessage>> = clientBridge.messages

    override val isNativeLibraryAvailable: Boolean
        get() = clientBridge.isNativeLibraryAvailable

    private val draftsPreferences by lazy {
        context.getSharedPreferences("telegram_message_drafts", Context.MODE_PRIVATE)
    }

    override fun loadChatDrafts(): Map<Long, String> {
        val raw = draftsPreferences.getString("drafts", null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val chatId = key.toLongOrNull() ?: continue
                    val draft = json.optString(key, "")
                    if (draft.isNotEmpty()) put(chatId, draft)
                }
            }
        } catch (_: Exception) { emptyMap() }
    }

    override fun saveChatDrafts(drafts: Map<Long, String>) {
        val json = JSONObject()
        drafts.forEach { (chatId, draft) -> if (draft.isNotEmpty()) json.put(chatId.toString(), draft) }
        draftsPreferences.edit().putString("drafts", json.toString()).apply()
    }

    override fun clearChatDrafts() {
        draftsPreferences.edit().remove("drafts").apply()
    }

    override suspend fun startTelegram(): Unit = withContext(ioDispatcher) {
        val databaseDir = File(context.filesDir, "tdlib/database").apply { mkdirs() }
        val filesDir = File(context.filesDir, "tdlib/files").apply { mkdirs() }

        // Fetch 256-bit database encryption key from Keystore
        val dbKey = security.getOrCreateDatabaseEncryptionKey()

        // Fetch API credentials (loaded from secure storage or BuildConfig, never hardcoded)
        val apiId = security.getTelegramApiId() ?: 0
        val apiHash = security.getTelegramApiHash() ?: ""

        val params = TelegramInitParams(
            apiId = apiId,
            apiHash = apiHash,
            databaseDirectory = databaseDir,
            filesDirectory = filesDir,
            databaseEncryptionKey = dbKey,
            mediaConfig = TelegramMediaConfig.STRICT_ON_DEMAND,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            systemVersion = "Android ${Build.VERSION.RELEASE}",
            applicationVersion = "DATAONE-1.0.0"
        )

        clientBridge.initialize(params)
        Unit
    }

    override suspend fun submitPhoneNumber(phoneNumber: String): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.sendPhoneNumber(phoneNumber)
    }

    override suspend fun submitAuthCode(code: String): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.sendAuthCode(code)
    }

    override suspend fun submit2FaPassword(password: String): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.sendPassword(password)
    }

    override suspend fun submitRegistration(firstName: String, lastName: String): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.sendRegistration(firstName, lastName)
    }

    override suspend fun logout(): Result<Unit> = withContext(ioDispatcher) {
        security.clearSession()
        clientBridge.logout()
    }

    override suspend fun refreshChats(): Result<Unit> = withContext(ioDispatcher) { clientBridge.refreshChats() }

    override suspend fun searchContacts(query: String): Result<List<TelegramUser>> = withContext(ioDispatcher) { clientBridge.searchContacts(query) }
    override suspend fun createGroupChat(title: String, userIds: List<Long>): Result<Long> = withContext(ioDispatcher) { clientBridge.createGroupChat(title, userIds) }

    override suspend fun openPrivateChat(userId: Long): Result<Long> = withContext(ioDispatcher) { clientBridge.openPrivateChat(userId) }

    override suspend fun toggleChatPinned(chatId: Long, isPinned: Boolean): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.toggleChatPinned(chatId, isPinned)
    }

    override suspend fun toggleChatMarkedUnread(chatId: Long, isMarkedAsUnread: Boolean): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.toggleChatMarkedUnread(chatId, isMarkedAsUnread)
    }

    override suspend fun toggleChatArchived(chatId: Long, isArchived: Boolean): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.toggleChatArchived(chatId, isArchived)
    }

    override suspend fun toggleChatMuted(chatId: Long, isMuted: Boolean): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.toggleChatMuted(chatId, isMuted)
    }

    override suspend fun getChatDetails(chatId: Long): Result<TelegramChatDetails> = withContext(ioDispatcher) { clientBridge.getChatDetails(chatId) }
    override suspend fun openChat(chatId: Long): Result<Unit> = withContext(ioDispatcher) { clientBridge.openChat(chatId) }
    override suspend fun loadOlderMessages(chatId: Long): Result<Unit> = withContext(ioDispatcher) { clientBridge.loadOlderMessages(chatId) }
    override suspend fun searchChatMessages(chatId: Long, query: String): Result<Unit> = withContext(ioDispatcher) { clientBridge.searchChatMessages(chatId, query) }
    override suspend fun loadSharedMedia(chatId: Long, category: String): Result<Unit> = withContext(ioDispatcher) { clientBridge.loadSharedMedia(chatId, category) }

    override suspend fun sendTextMessage(chatId: Long, text: String, replyToMessageId: Long?): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.sendTextMessage(chatId, text, replyToMessageId)
    }

    override suspend fun forwardMessage(fromChatId: Long, messageId: Long, toChatId: Long): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.forwardMessage(fromChatId, messageId, toChatId)
    }

    override suspend fun editTextMessage(chatId: Long, messageId: Long, newText: String): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.editTextMessage(chatId, messageId, newText)
    }
    override suspend fun deleteMessage(chatId: Long, messageId: Long): Result<Unit> = withContext(ioDispatcher) { clientBridge.deleteMessage(chatId, messageId) }
    override suspend fun addMessageReaction(chatId: Long, messageId: Long, emoji: String): Result<Unit> = withContext(ioDispatcher) { clientBridge.addMessageReaction(chatId, messageId, emoji) }
    override suspend fun toggleMessagePinned(chatId: Long, messageId: Long, isPinned: Boolean): Result<Unit> = withContext(ioDispatcher) { clientBridge.toggleMessagePinned(chatId, messageId, isPinned) }

    override suspend fun sendPhotoMessage(chatId: Long, path: String, caption: String): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.sendPhotoMessage(chatId, path, caption)
    }

    override suspend fun sendVideoMessage(chatId: Long, path: String, caption: String): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.sendVideoMessage(chatId, path, caption)
    }

    override suspend fun sendDocumentMessage(chatId: Long, path: String, caption: String): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.sendDocumentMessage(chatId, path, caption)
    }

    override suspend fun sendAudioMessage(chatId: Long, path: String, caption: String): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.sendAudioMessage(chatId, path, caption)
    }

    override suspend fun sendVoiceMessage(chatId: Long, path: String, durationSeconds: Int): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.sendVoiceMessage(chatId, path, durationSeconds)
    }

    override suspend fun requestMediaDownload(fileId: Int): Result<Unit> = withContext(ioDispatcher) {
        clientBridge.downloadFileOnDemand(fileId, priority = 1)
    }

    override suspend fun configureApiCredentials(apiId: Int, apiHash: String) = withContext(ioDispatcher) {
        security.storeApiCredentials(apiId, apiHash)
    }
}
