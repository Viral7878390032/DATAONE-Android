package com.example.feature.telegram.bridge

import android.util.Log
import com.example.feature.telegram.model.TelegramAuthState
import com.example.feature.telegram.model.TelegramChat
import com.example.feature.telegram.model.TelegramChatDetails
import com.example.feature.telegram.model.TelegramMessage
import com.example.feature.telegram.model.TelegramUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import kotlin.coroutines.resume

/**
 * Real TDLib bridge. Uses the Java Client API directly so the database encryption
 * key supplied by TelegramSessionSecurity is passed to TDLib (the third-party KTX
 * convenience wrapper currently initializes TDLib with an empty key).
 */
class NativeTdLibBridge : TelegramClientBridge {

    private val tag = "DATAONE-TDLib"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _authState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Uninitialized)
    override val authState: StateFlow<TelegramAuthState> = _authState.asStateFlow()

    private val _chats = MutableStateFlow<List<TelegramChat>>(emptyList())
    override val chats: StateFlow<List<TelegramChat>> = _chats.asStateFlow()
    private val _messages = MutableStateFlow<List<TelegramMessage>>(emptyList())
    override val messages: StateFlow<List<TelegramMessage>> = _messages.asStateFlow()
    @Volatile private var selectedChatId: Long? = null
    @Volatile private var isLoadingOlderHistory = false
    @Volatile private var hasMoreChatHistory = true

    private val nativeLoaded = AtomicBoolean(false)
    override val isNativeLibraryAvailable: Boolean
        get() = nativeLoaded.get()

    @Volatile private var client: Client? = null
    @Volatile private var activeParams: TelegramInitParams? = null

    init {
        try {
            System.loadLibrary("tdjni")
            nativeLoaded.set(true)
            Log.i(tag, "TDLib native library loaded.")
        } catch (error: UnsatisfiedLinkError) {
            nativeLoaded.set(false)
            Log.e(tag, "TDLib native library could not be loaded.", error)
        }
    }

    override suspend fun initialize(params: TelegramInitParams): Result<Unit> {
        activeParams = params
        if (!nativeLoaded.get()) {
            val message = "Telegram engine is unavailable: TDLib native library could not be loaded."
            _authState.value = TelegramAuthState.Error(message, 1001, false)
            return Result.failure(IllegalStateException(message))
        }
        if (params.apiId <= 0 || params.apiHash.isBlank()) {
            _authState.value = TelegramAuthState.WaitTdLibParameters
            return Result.failure(IllegalStateException("Enter your Telegram API ID and API Hash in the Telegram settings."))
        }
        if (params.databaseEncryptionKey.size != 32) {
            val message = "The Telegram database encryption key is invalid. Please reset the Telegram setup."
            _authState.value = TelegramAuthState.Error(message, 1002, false)
            return Result.failure(IllegalStateException(message))
        }

        return try {
            if (client == null) {
                _authState.value = TelegramAuthState.Connecting
                client = Client.create(
                    { update ->
                        when (update) {
                            is TdApi.UpdateAuthorizationState ->
                                scope.launch { handleAuthorizationState(update.authorizationState) }
                            is TdApi.UpdateFile -> updateMessageFile(update.file)
                            is TdApi.UpdateNewChat -> scope.launch { refreshChats() }
                            is TdApi.UpdateChatLastMessage -> scope.launch { refreshChats() }
                            is TdApi.UpdateNewMessage -> {
                                if (update.message.chatId == selectedChatId) {
                                    val incoming = toTelegramMessage(update.message)
                                    _messages.update { current ->
                                        (current.filterNot { it.id == incoming.id } + incoming)
                                            .sortedWith(compareBy<TelegramMessage> { it.date }.thenBy { it.id })
                                    }
                                } else {
                                    scope.launch { refreshChats() }
                                }
                            }
                        }
                    },
                    { error -> Log.e(tag, "TDLib update callback failed.", error) },
                    { error ->
                        Log.e(tag, "TDLib callback failed.", error)
                        scope.launch {
                            _authState.value = TelegramAuthState.Error(
                                error.message ?: "Telegram engine callback failed."
                            )
                        }
                    }
                )
            }
            // Client.create() loads the JNI class; explicitly setting this also gives
            // a clear failure here rather than crashing later on the first request.
            Client.execute(TdApi.SetLogVerbosityLevel(1))
            Result.success(Unit)
        } catch (error: Throwable) {
            Log.e(tag, "Unable to create TDLib client.", error)
            _authState.value = TelegramAuthState.Error(error.message ?: "Unable to start Telegram engine.")
            Result.failure(error)
        }
    }

    private suspend fun handleAuthorizationState(state: TdApi.AuthorizationState) {
        val params = activeParams ?: return
        try {
            when (state) {
                is TdApi.AuthorizationStateWaitTdlibParameters -> {
                    _authState.value = TelegramAuthState.Connecting
                    val request = TdApi.SetTdlibParameters()
                    request.useTestDc = false
                    request.databaseDirectory = params.databaseDirectory.absolutePath
                    request.filesDirectory = params.filesDirectory.absolutePath
                    request.databaseEncryptionKey = params.databaseEncryptionKey
                    request.useFileDatabase = params.useFileDatabase
                    request.useChatInfoDatabase = params.useChatInfoDatabase
                    request.useMessageDatabase = params.useMessageDatabase
                    request.useSecretChats = false
                    request.apiId = params.apiId
                    request.apiHash = params.apiHash
                    request.systemLanguageCode = params.systemLanguageCode
                    request.deviceModel = params.deviceModel
                    request.systemVersion = params.systemVersion
                    request.applicationVersion = params.applicationVersion
                    sendRequest(request)
                }
                is TdApi.AuthorizationStateWaitPhoneNumber -> {
                    _authState.value = TelegramAuthState.WaitPhoneNumber()
                }
                is TdApi.AuthorizationStateWaitCode -> {
                    val info = state.codeInfo
                    _authState.value = TelegramAuthState.WaitCode(
                        phoneNumber = info.phoneNumber.orEmpty(),
                        codeLength = 5,
                        deliveryType = info.type.javaClass.simpleName
                            .removePrefix("AuthenticationCodeType")
                            .ifBlank { "Telegram" }
                    )
                }
                is TdApi.AuthorizationStateWaitPassword -> {
                    _authState.value = TelegramAuthState.WaitPassword(
                        passwordHint = state.passwordHint,
                        hasRecoveryEmail = state.hasRecoveryEmailAddress
                    )
                }
                is TdApi.AuthorizationStateWaitRegistration -> {
                    _authState.value = TelegramAuthState.WaitRegistration()
                }
                is TdApi.AuthorizationStateReady -> {
                    val me = sendRequest(TdApi.GetMe()) as TdApi.User
                    _authState.value = TelegramAuthState.Authenticated(
                        TelegramUser(
                            id = me.id,
                            firstName = me.firstName,
                            lastName = me.lastName,
                            phoneNumber = me.phoneNumber
                        )
                    )
                    refreshChats()
                }
                is TdApi.AuthorizationStateLoggingOut -> _authState.value = TelegramAuthState.Closing
                is TdApi.AuthorizationStateClosing -> _authState.value = TelegramAuthState.Closing
                is TdApi.AuthorizationStateClosed -> {
                    client = null
                    _authState.value = TelegramAuthState.Closed
                }
                else -> {
                    val stateName = state.javaClass.simpleName
                    val details = state.toString()
                    Log.e(tag, "Unhandled TDLib authorization state: $stateName | $details")
                    _authState.value = TelegramAuthState.Error(
                        "Telegram needs an authorization step that DATAONE does not support yet: $stateName. Details: $details"
                    )
                }
            }
        } catch (error: Throwable) {
            Log.e(tag, "Authorization state handling failed.", error)
            _authState.value = TelegramAuthState.Error(error.message ?: "Telegram authorization failed.")
        }
    }

    private suspend fun sendRequest(function: TdApi.Function<*>): TdApi.Object =
        suspendCancellableCoroutine { continuation ->
            val currentClient = client
            if (currentClient == null) {
                continuation.resume(TdApi.Error(500, "Telegram client is not initialized."))
                return@suspendCancellableCoroutine
            }
            try {
                currentClient.send(function, Client.ResultHandler { result ->
                    if (continuation.isActive) continuation.resume(result)
                })
            } catch (error: Throwable) {
                if (continuation.isActive) {
                    continuation.resume(TdApi.Error(500, error.message ?: "TDLib request failed."))
                }
            }
        }.let { result ->
            if (result is TdApi.Error) throw IllegalStateException("Telegram error ${result.code}: ${result.message}")
            result
        }

    override suspend fun sendPhoneNumber(phoneNumber: String): Result<Unit> {
        val clean = phoneNumber.trim()
        if (!clean.startsWith("+") || clean.count { it.isDigit() } < 7) {
            return Result.failure(IllegalArgumentException("Enter a valid international phone number, including + and country code."))
        }
        return runRequest { TdApi.SetAuthenticationPhoneNumber(clean, null) }
    }

    override suspend fun sendAuthCode(code: String): Result<Unit> {
        val clean = code.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Verification code cannot be empty."))
        return runRequest { TdApi.CheckAuthenticationCode(clean) }
    }

    override suspend fun sendPassword(password: String): Result<Unit> {
        if (password.isBlank()) return Result.failure(IllegalArgumentException("Telegram password cannot be empty."))
        return runRequest { TdApi.CheckAuthenticationPassword(password) }
    }

    override suspend fun sendRegistration(firstName: String, lastName: String): Result<Unit> {
        if (firstName.isBlank()) return Result.failure(IllegalArgumentException("First name is required."))
        return runRequest { TdApi.RegisterUser(firstName.trim(), lastName.trim(), false) }
    }

    override suspend fun searchContacts(query: String): Result<List<TelegramUser>> = try {
        val result = sendRequest(TdApi.SearchContacts(query.trim(), 100)) as TdApi.Users
        val contacts = mutableListOf<TelegramUser>()
        for (userId in result.userIds) {
            val contact = runCatching {
                val user = sendRequest(TdApi.GetUser(userId)) as TdApi.User
                TelegramUser(
                    id = user.id,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    username = user.usernames?.activeUsernames?.firstOrNull(),
                    phoneNumber = user.phoneNumber,
                    isPremium = user.isPremium
                )
            }.getOrNull()
            if (contact != null) contacts.add(contact)
        }
        Result.success(contacts)
    } catch (error: Throwable) { Result.failure(error) }

    override suspend fun createGroupChat(title: String, userIds: List<Long>): Result<Long> = try {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty() && cleanTitle.length <= 128) { "Group name must be 1–128 characters." }
        require(userIds.distinct().size >= 2) { "Select at least two contacts." }
        val chat = sendRequest(TdApi.CreateNewBasicGroupChat(userIds.distinct().toLongArray(), cleanTitle, 0)) as TdApi.Chat
        refreshChats()
        Result.success(chat.id)
    } catch (error: Exception) {
        Result.failure(error)
    }

    override suspend fun renameGroupChat(chatId: Long, title: String): Result<Unit> = try {
        val cleanTitle = title.trim()
        require(chatId != 0L && cleanTitle.isNotBlank() && cleanTitle.length <= 128) { "Enter a group name between 1 and 128 characters." }
        sendRequest(TdApi.SetChatTitle(chatId, cleanTitle))
        refreshChats()
        Result.success(Unit)
    } catch (error: Throwable) { Result.failure(error) }

    override suspend fun addGroupMembers(chatId: Long, userIds: List<Long>): Result<Unit> = try {
        val ids = userIds.distinct().filter { it != 0L }
        require(chatId != 0L && ids.isNotEmpty()) { "Select at least one contact to add." }
        val request = TdApi.AddChatMembers()
        request.chatId = chatId
        request.userIds = ids.toLongArray()
        sendRequest(request)
        refreshChats()
        Result.success(Unit)
    } catch (error: Throwable) { Result.failure(error) }

    override suspend fun leaveGroupChat(chatId: Long): Result<Unit> = try {
        require(chatId != 0L) { "Select a group first." }
        sendRequest(TdApi.LeaveChat(chatId))
        if (selectedChatId == chatId) {
            selectedChatId = null
            _messages.value = emptyList()
        }
        refreshChats()
        Result.success(Unit)
    } catch (error: Throwable) { Result.failure(error) }

    override suspend fun openPrivateChat(userId: Long): Result<Long> = try {
        require(userId != 0L) { "Choose a valid Telegram contact." }
        val chat = sendRequest(TdApi.CreatePrivateChat(userId, false)) as TdApi.Chat
        refreshChats()
        Result.success(chat.id)
    } catch (error: Throwable) { Result.failure(error) }

    override suspend fun refreshChats(): Result<Unit> = try {
        // Load every available page from both TDLib chat lists before reading IDs.
        // GetChats only returns chats already loaded into TDLib's local chat list;
        // LoadChats asks TDLib to continue loading older entries from the server.
        suspend fun loadAllChats(chatList: TdApi.ChatList) {
            repeat(100) {
                try {
                    sendRequest(TdApi.LoadChats(chatList, 1000))
                } catch (error: Throwable) {
                    // TDLib returns 404 when this list has no more chats to load.
                    if (error.message.orEmpty().contains("404")) return
                    throw error
                }
            }
        }
        loadAllChats(TdApi.ChatListMain())
        loadAllChats(TdApi.ChatListArchive())

        val mainResult = sendRequest(TdApi.GetChats(TdApi.ChatListMain(), 1000)) as TdApi.Chats
        val archiveResult = sendRequest(TdApi.GetChats(TdApi.ChatListArchive(), 1000)) as TdApi.Chats
        val archivedIds = archiveResult.chatIds.toSet()
        val allChatIds = (mainResult.chatIds.toList() + archiveResult.chatIds.toList()).distinct()
        val loaded = mutableListOf<TelegramChat>()
        val semaphore = Semaphore(12)

        allChatIds.chunked(40).forEach { batch ->
            val hydrated = coroutineScope {
                batch.map { id ->
                    async {
                        semaphore.withPermit {
                            try {
                                val chat = sendRequest(TdApi.GetChat(id)) as TdApi.Chat
                                val photoFile = chat.photo?.small
                                val photoLocal = photoFile?.local
                                val presence = when (val type = chat.type) {
                                    is TdApi.ChatTypePrivate -> userPresence(type.userId)
                                    is TdApi.ChatTypeSecret -> userPresence(type.userId)
                                    else -> null
                                }
                                val completedPhotoPath = photoLocal?.let { local ->
                                    if (local.isDownloadingCompleted && local.path.isNotBlank()) local.path else null
                                }
                                TelegramChat(
                                    id = chat.id,
                                    title = chat.title,
                                    lastMessage = chat.lastMessage?.let { messagePreview(it.content) } ?: "No messages yet",
                                    lastMessageDate = chat.lastMessage?.date ?: 0,
                                    unreadCount = chat.unreadCount,
                                    isPinned = chat.positions.any { it.list is TdApi.ChatListMain && it.isPinned },
                                    isMarkedAsUnread = chat.isMarkedAsUnread,
                                    isArchived = chat.id in archivedIds,
                                    isMuted = chat.notificationSettings.muteFor > 0,
                                    unreadMentionCount = chat.unreadMentionCount,
                                    photoFileId = photoFile?.id ?: 0,
                                    photoFilePath = completedPhotoPath,
                                    presenceText = presence?.first,
                                    isOnline = presence?.second == true
                                ).also {
                                    if (photoFile != null && photoLocal?.isDownloadingCompleted != true) {
                                        runCatching { sendRequest(TdApi.DownloadFile(photoFile.id, 16, 0, 0, false)) }
                                    }
                                }
                            } catch (error: Throwable) {
                                Log.w(tag, "Could not load Telegram chat $id", error)
                                null
                            }
                        }
                    }
                }.awaitAll()
            }
            loaded.addAll(hydrated.filterNotNull())
            // Publish each batch so the list fills progressively instead of
            // leaving the user staring at a spinner until every chat is hydrated.
            _chats.value = loaded.toList()
        }
        Result.success(Unit)
    } catch (error: Throwable) { Result.failure(error) }

    override suspend fun toggleChatPinned(chatId: Long, isPinned: Boolean): Result<Unit> = try {
        sendRequest(TdApi.ToggleChatIsPinned(TdApi.ChatListMain(), chatId, isPinned))
        refreshChats()
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(error)
    }

    override suspend fun toggleChatMarkedUnread(chatId: Long, isMarkedAsUnread: Boolean): Result<Unit> = try {
        sendRequest(TdApi.ToggleChatIsMarkedAsUnread(chatId, isMarkedAsUnread))
        refreshChats()
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(error)
    }

    override suspend fun toggleChatArchived(chatId: Long, isArchived: Boolean): Result<Unit> = try {
        val destination = if (isArchived) TdApi.ChatListArchive() else TdApi.ChatListMain()
        sendRequest(TdApi.AddChatToList(chatId, destination))
        refreshChats()
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(error)
    }

    override suspend fun toggleChatMuted(chatId: Long, isMuted: Boolean): Result<Unit> = try {
        val chat = sendRequest(TdApi.GetChat(chatId)) as TdApi.Chat
        val settings = chat.notificationSettings
        settings.useDefaultMuteFor = false
        settings.muteFor = if (isMuted) 31_708_800 else 0
        sendRequest(TdApi.SetChatNotificationSettings(chatId, settings))
        refreshChats()
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(error)
    }

    override suspend fun getChatDetails(chatId: Long): Result<TelegramChatDetails> = try {
        require(chatId != 0L) { "Select a Telegram chat first." }
        val chat = sendRequest(TdApi.GetChat(chatId)) as TdApi.Chat
        var username = ""
        var phoneNumber = ""
        val chatType = when (val type = chat.type) {
            is TdApi.ChatTypePrivate -> {
                val user = sendRequest(TdApi.GetUser(type.userId)) as TdApi.User
                username = user.usernames?.activeUsernames?.firstOrNull().orEmpty()
                phoneNumber = user.phoneNumber.orEmpty()
                "Private chat"
            }
            is TdApi.ChatTypeSecret -> {
                val user = sendRequest(TdApi.GetUser(type.userId)) as TdApi.User
                username = user.usernames?.activeUsernames?.firstOrNull().orEmpty()
                phoneNumber = user.phoneNumber.orEmpty()
                "Secret chat"
            }
            is TdApi.ChatTypeBasicGroup -> "Group"
            is TdApi.ChatTypeSupergroup -> if (type.isChannel) "Channel" else "Supergroup"
            else -> "Chat"
        }
        Result.success(
            TelegramChatDetails(
                chatId = chat.id,
                title = chat.title,
                chatType = chatType,
                username = username,
                phoneNumber = phoneNumber
            )
        )
    } catch (error: Throwable) {
        Result.failure(error)
    }

    override suspend fun openChat(chatId: Long): Result<Unit> = try {
        require(chatId != 0L) { "Select a valid Telegram chat." }
        selectedChatId = chatId
        hasMoreChatHistory = false
        isLoadingOlderHistory = false
        _messages.value = emptyList()

        // Ask TDLib to open this conversation, then page through its complete
        // server history. TDLib returns newest-first; the UI needs oldest-first.
        sendRequest(TdApi.OpenChat(chatId))
        val allMessages = LinkedHashMap<Long, TdApi.Message>()
        var fromMessageId = 0L
        var newestBatch: Array<TdApi.Message> = emptyArray()
        var keepLoading = true

        while (keepLoading && selectedChatId == chatId) {
            val page = sendRequest(
                TdApi.GetChatHistory(chatId, fromMessageId, 0, 100, false)
            ) as TdApi.Messages
            if (page.messages.isEmpty()) {
                keepLoading = false
            } else {
                newestBatch = page.messages
                var added = 0
                page.messages.forEach { message ->
                    if (allMessages.put(message.id, message) == null) added++
                }
                val oldestInPage = page.messages.minByOrNull { it.id }?.id ?: 0L
                if (page.messages.size < 100 || added == 0 || oldestInPage == 0L || oldestInPage == fromMessageId) {
                    keepLoading = false
                } else {
                    fromMessageId = oldestInPage
                }
            }
        }

        if (selectedChatId == chatId) {
            _messages.value = allMessages.values
                .sortedWith(compareBy<TdApi.Message> { it.date }.thenBy { it.id })
                .map(::toTelegramMessage)
            // The entire available history has been fetched; no extra scroll paging is needed.
            hasMoreChatHistory = false
            if (newestBatch.isNotEmpty()) {
                sendRequest(
                    TdApi.ViewMessages(
                        chatId,
                        newestBatch.map { it.id }.toLongArray(),
                        null,
                        true
                    )
                )
                refreshChats()
            }
        }
        Result.success(Unit)
    } catch (error: Throwable) { Result.failure(error) }

    override suspend fun searchChatMessages(chatId: Long, query: String): Result<Unit> = try {
        require(chatId != 0L) { "Select a Telegram chat before searching." }
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            if (selectedChatId == chatId) openChat(chatId)
        } else {
            val request = TdApi.SearchChatMessages()
            request.chatId = chatId
            request.query = cleanQuery
            request.fromMessageId = 0
            request.offset = 0
            request.limit = 100
            val result = sendRequest(request) as TdApi.FoundChatMessages
            if (selectedChatId == chatId) {
                _messages.value = result.messages.reversed().map(::toTelegramMessage)
            }
        }
        Result.success(Unit)
    } catch (error: Throwable) { Result.failure(error) }

    override suspend fun loadSharedMedia(chatId: Long, category: String): Result<Unit> = try {
        require(chatId != 0L) { "Select a Telegram chat first." }
        val filters: List<TdApi.SearchMessagesFilter> = when (category) {
            "Photos" -> listOf(TdApi.SearchMessagesFilterPhoto())
            "Videos" -> listOf(TdApi.SearchMessagesFilterVideo())
            "Files" -> listOf(TdApi.SearchMessagesFilterDocument(), TdApi.SearchMessagesFilterAudio())
            "Links" -> listOf(TdApi.SearchMessagesFilterUrl())
            else -> listOf(TdApi.SearchMessagesFilterPhoto(), TdApi.SearchMessagesFilterVideo(), TdApi.SearchMessagesFilterDocument(), TdApi.SearchMessagesFilterAudio(), TdApi.SearchMessagesFilterUrl())
        }
        val found = filters.flatMap { filter ->
            val request = TdApi.SearchChatMessages()
            request.chatId = chatId
            request.query = ""
            request.fromMessageId = 0
            request.offset = 0
            request.limit = 100
            request.filter = filter
            (sendRequest(request) as TdApi.FoundChatMessages).messages.toList()
        }.distinctBy { it.id }.sortedBy { it.date }
        if (selectedChatId == chatId) _messages.value = found.map(::toTelegramMessage)
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(error)
    }

    override suspend fun loadOlderMessages(chatId: Long): Result<Unit> {
        if (chatId != selectedChatId || isLoadingOlderHistory || !hasMoreChatHistory) return Result.success(Unit)
        val oldestMessageId = _messages.value.firstOrNull()?.id ?: return Result.success(Unit)
        isLoadingOlderHistory = true
        return try {
            val result = sendRequest(TdApi.GetChatHistory(chatId, oldestMessageId, 0, 50, false)) as TdApi.Messages
            val olderMessages = result.messages.reversed().map(::toTelegramMessage)
            if (olderMessages.isEmpty()) {
                hasMoreChatHistory = false
            } else {
                val existingIds = _messages.value.mapTo(mutableSetOf()) { it.id }
                val uniqueOlder = olderMessages.filterNot { it.id in existingIds }
                _messages.value = uniqueOlder + _messages.value
                if (result.messages.size < 50 || uniqueOlder.isEmpty()) hasMoreChatHistory = false
            }
            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(error)
        } finally {
            isLoadingOlderHistory = false
        }
    }

    override suspend fun forwardMessage(fromChatId: Long, messageId: Long, toChatId: Long): Result<Unit> = try {
        require(fromChatId != 0L && toChatId != 0L && messageId != 0L) { "Select a valid message and destination chat." }
        sendRequest(TdApi.ForwardMessages(toChatId, null, fromChatId, longArrayOf(messageId), null, false, false))
        refreshChats()
        if (selectedChatId == toChatId) openChat(toChatId)
        Result.success(Unit)
    } catch (error: Throwable) { Result.failure(error) }

    override suspend fun editTextMessage(chatId: Long, messageId: Long, newText: String): Result<Unit> = try {
        val cleanText = newText.trim()
        require(chatId != 0L && messageId != 0L) { "Select a valid message to edit." }
        require(cleanText.isNotEmpty()) { "Message cannot be empty." }
        val content = TdApi.InputMessageText(TdApi.FormattedText(cleanText, emptyArray()), null, true)
        sendRequest(TdApi.EditMessageText(chatId, messageId, null, content))
        if (selectedChatId == chatId) openChat(chatId)
        Result.success(Unit)
    } catch (error: Throwable) { Result.failure(error) }

    override suspend fun deleteMessage(chatId: Long, messageId: Long): Result<Unit> = try {
        require(chatId != 0L && messageId != 0L) { "Select a valid message to delete." }
        sendRequest(TdApi.DeleteMessages(chatId, longArrayOf(messageId), true))
        if (selectedChatId == chatId) openChat(chatId)
        Result.success(Unit)
    } catch (error: Throwable) { Result.failure(error) }

    override suspend fun addMessageReaction(chatId: Long, messageId: Long, emoji: String): Result<Unit> = try {
        require(chatId != 0L && messageId != 0L && emoji.isNotBlank()) { "Select a valid message and reaction." }
        sendRequest(TdApi.AddMessageReaction(chatId, messageId, TdApi.ReactionTypeEmoji(emoji), false, true))
        if (selectedChatId == chatId) openChat(chatId)
        Result.success(Unit)
    } catch (error: Throwable) { Result.failure(error) }

    override suspend fun toggleMessagePinned(chatId: Long, messageId: Long, isPinned: Boolean): Result<Unit> = try {
        require(chatId != 0L && messageId != 0L) { "Select a valid message to pin." }
        if (isPinned) {
            sendRequest(TdApi.UnpinChatMessage(chatId, messageId))
        } else {
            sendRequest(TdApi.PinChatMessage(chatId, messageId, true, false))
        }
        if (selectedChatId == chatId) openChat(chatId)
        Result.success(Unit)
    } catch (error: Throwable) { Result.failure(error) }

    override suspend fun sendPhotoMessage(chatId: Long, path: String, caption: String): Result<Unit> = try {
        require(chatId != 0L) { "Choose a Telegram chat first." }
        require(path.isNotBlank() && File(path).isFile) { "The selected photo could not be opened." }
        // InputMessagePhoto expects an InputPhoto wrapper, which contains the local file.
        val inputPhoto = TdApi.InputPhoto()
        inputPhoto.photo = TdApi.InputFileLocal(path)
        val content = TdApi.InputMessagePhoto()
        content.photo = inputPhoto
        content.caption = TdApi.FormattedText(caption, emptyArray())
        sendRequest(TdApi.SendMessage(chatId, null, null, null, null, content))
        openChat(chatId)
    } catch (error: Throwable) {
        Result.failure(error)
    }

    override suspend fun sendVideoMessage(chatId: Long, path: String, caption: String): Result<Unit> = try {
        require(chatId != 0L) { "Choose a Telegram chat first." }
        require(path.isNotBlank() && File(path).isFile) { "The selected video could not be opened." }
        val inputVideo = TdApi.InputVideo()
        inputVideo.video = TdApi.InputFileLocal(path)
        val content = TdApi.InputMessageVideo()
        content.video = inputVideo
        content.caption = TdApi.FormattedText(caption, emptyArray())
        sendRequest(TdApi.SendMessage(chatId, null, null, null, null, content))
        openChat(chatId)
    } catch (error: Throwable) {
        Result.failure(error)
    }

    override suspend fun sendDocumentMessage(chatId: Long, path: String, caption: String): Result<Unit> = try {
        require(chatId != 0L) { "Choose a Telegram chat first." }
        require(path.isNotBlank() && File(path).isFile) { "The selected file could not be opened." }
        val inputDocument = TdApi.InputDocument()
        inputDocument.document = TdApi.InputFileLocal(path)
        val content = TdApi.InputMessageDocument()
        content.document = inputDocument
        content.caption = TdApi.FormattedText(caption, emptyArray())
        sendRequest(TdApi.SendMessage(chatId, null, null, null, null, content))
        openChat(chatId)
    } catch (error: Throwable) {
        Result.failure(error)
    }

    override suspend fun sendAudioMessage(chatId: Long, path: String, caption: String): Result<Unit> = try {
        require(chatId != 0L) { "Choose a Telegram chat first." }
        require(path.isNotBlank() && File(path).isFile) { "The selected audio could not be opened." }
        val inputAudio = TdApi.InputAudio()
        inputAudio.audio = TdApi.InputFileLocal(path)
        inputAudio.title = File(path).nameWithoutExtension.take(64)
        inputAudio.performer = ""
        val content = TdApi.InputMessageAudio(
            inputAudio,
            TdApi.FormattedText(caption, emptyArray())
        )
        sendRequest(TdApi.SendMessage(chatId, null, null, null, null, content))
        openChat(chatId)
    } catch (error: Throwable) {
        Result.failure(error)
    }

    override suspend fun sendVoiceMessage(chatId: Long, path: String, durationSeconds: Int): Result<Unit> = try {
        require(chatId != 0L) { "Choose a Telegram chat first." }
        require(path.isNotBlank() && File(path).isFile) { "The voice recording could not be opened." }
        require(durationSeconds > 0) { "Record a voice message before sending." }
        val voiceNote = TdApi.InputVoiceNote()
        voiceNote.voiceNote = TdApi.InputFileLocal(path)
        voiceNote.duration = durationSeconds
        voiceNote.waveform = ByteArray(0)
        val content = TdApi.InputMessageVoiceNote()
        content.voiceNote = voiceNote
        content.caption = TdApi.FormattedText("", emptyArray())
        sendRequest(TdApi.SendMessage(chatId, null, null, null, null, content))
        openChat(chatId)
    } catch (error: Throwable) {
        Result.failure(error)
    }

    override suspend fun sendTextMessage(chatId: Long, text: String, replyToMessageId: Long?): Result<Unit> = try {
        val cleanText = text.trim()
        require(chatId != 0L) { "Choose a Telegram chat first." }
        require(cleanText.isNotEmpty()) { "Type a message before sending." }
        val content = TdApi.InputMessageText(
            TdApi.FormattedText(cleanText, emptyArray()),
            null,
            true
        )
        val replyTo = replyToMessageId?.let { messageId ->
            TdApi.InputMessageReplyToMessage().apply { this.messageId = messageId }
        }
        sendRequest(TdApi.SendMessage(chatId, null, replyTo, null, null, content))
        openChat(chatId)
    } catch (error: Throwable) {
        Result.failure(error)
    }

    private fun toTelegramMessage(message: TdApi.Message): TelegramMessage {
        val messageContent = message.content
        val photoSizes = (messageContent as? TdApi.MessagePhoto)?.photo?.sizes.orEmpty()
        val photoFile = photoSizes.maxByOrNull { size -> size.width.toLong() * size.height.toLong() }?.photo
        val previewFile = photoSizes.minByOrNull { size -> size.width.toLong() * size.height.toLong() }?.photo
        val audioFile = when (val content = messageContent) {
            is TdApi.MessageAudio -> content.audio.audio
            is TdApi.MessageVoiceNote -> content.voiceNote.voice
            else -> null
        }
        val video = (messageContent as? TdApi.MessageVideo)?.video
        val videoFile = video?.video
        val videoPreviewFile = video?.thumbnail?.file
        val documentFile = (messageContent as? TdApi.MessageDocument)?.document?.document
        val localFile = photoFile?.local ?: videoFile?.local ?: documentFile?.local ?: audioFile?.local
        val mediaType = when {
            photoFile != null -> "photo"
            videoFile != null -> "video"
            documentFile != null -> "document"
            messageContent is TdApi.MessageAudio -> "audio"
            messageContent is TdApi.MessageVoiceNote -> "voice"
            else -> null
        }
        val caption = when (val content = messageContent) {
            is TdApi.MessagePhoto -> content.caption.text
            is TdApi.MessageAudio -> content.caption.text
            is TdApi.MessageVoiceNote -> content.caption.text
            else -> ""
        }
        return TelegramMessage(
            id = message.id,
            chatId = message.chatId,
            text = when (messageContent) {
                is TdApi.MessageText -> messagePreview(messageContent)
                is TdApi.MessagePhoto -> caption
                is TdApi.MessageAudio -> caption.ifBlank { messageContent.audio.title.ifBlank { "Audio" } }
                is TdApi.MessageVoiceNote -> "Voice message"
                else -> messagePreview(messageContent)
            },
            date = message.date,
            isOutgoing = message.isOutgoing,
            isPinned = message.isPinned,
            mediaType = mediaType,
            mediaFileId = photoFile?.id ?: videoFile?.id ?: documentFile?.id ?: audioFile?.id,
            mediaFilePath = localFile?.path?.takeIf { localFile.isDownloadingCompleted && it.isNotBlank() },
            isMediaDownloaded = localFile?.isDownloadingCompleted == true,
            isMediaDownloading = localFile?.isDownloadingActive == true,
            previewFileId = previewFile?.id ?: videoPreviewFile?.id,
            previewFilePath = (previewFile?.local ?: videoPreviewFile?.local)?.let { local -> local.path.takeIf { local.isDownloadingCompleted && it.isNotBlank() } }
        )
    }

    private suspend fun userPresence(userId: Long): Pair<String, Boolean>? = try {
        val user = sendRequest(TdApi.GetUser(userId)) as TdApi.User
        when (val status = user.status) {
            is TdApi.UserStatusOnline -> "online" to true
            is TdApi.UserStatusOffline -> {
                val seconds = (System.currentTimeMillis() / 1000L) - status.wasOnline
                val label = when {
                    seconds < 60 -> "last seen just now"
                    seconds < 3600 -> "last seen ${seconds / 60} min ago"
                    seconds < 86400 -> "last seen ${seconds / 3600} hr ago"
                    seconds < 604800 -> "last seen ${seconds / 86400} days ago"
                    else -> "last seen a while ago"
                }
                label to false
            }
            is TdApi.UserStatusRecently -> "last seen recently" to false
            is TdApi.UserStatusLastWeek -> "last seen within a week" to false
            is TdApi.UserStatusLastMonth -> "last seen within a month" to false
            else -> null
        }
    } catch (error: Throwable) {
        Log.d(tag, "Could not load Telegram presence for user $userId", error)
        null
    }

    private fun updateMessageFile(file: TdApi.File) {
        _chats.update { current ->
            current.map { chat ->
                if (chat.photoFileId == file.id) {
                    chat.copy(photoFilePath = file.local.path.takeIf { file.local.isDownloadingCompleted && it.isNotBlank() })
                } else chat
            }
        }
        _messages.update { current ->
            current.map { message ->
                when {
                    message.mediaFileId == file.id -> message.copy(mediaFilePath = file.local.path.takeIf { file.local.isDownloadingCompleted && it.isNotBlank() }, isMediaDownloaded = file.local.isDownloadingCompleted, isMediaDownloading = file.local.isDownloadingActive)
                    message.previewFileId == file.id -> message.copy(previewFilePath = file.local.path.takeIf { file.local.isDownloadingCompleted && it.isNotBlank() })
                    else -> message
                }
            }
        }
    }

    private fun messagePreview(content: TdApi.MessageContent): String = when (content) {
        is TdApi.MessageText -> content.text.text
        is TdApi.MessagePhoto -> "Photo"
        is TdApi.MessageVideo -> "Video"
        is TdApi.MessageDocument -> content.document.fileName.ifBlank { "File" }
        is TdApi.MessageVoiceNote -> "Voice message"
        is TdApi.MessageAudio -> content.audio.title.ifBlank { "Audio" }
        is TdApi.MessageAnimation -> "GIF"
        is TdApi.MessageSticker -> "Sticker"
        else -> "Message"
    }

    override suspend fun downloadFileOnDemand(fileId: Int, priority: Int): Result<Unit> {
        if (fileId <= 0) return Result.failure(IllegalArgumentException("Invalid Telegram file ID."))
        return runRequest { TdApi.DownloadFile(fileId, priority.coerceIn(1, 32), 0, 0, false) }
    }

    private suspend fun runRequest(request: () -> TdApi.Function<*>): Result<Unit> =
        try {
            sendRequest(request())
            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(error)
        }

    override suspend fun logout(): Result<Unit> =
        runRequest { TdApi.LogOut() }.also {
            if (it.isFailure) _authState.value = TelegramAuthState.Error(
                it.exceptionOrNull()?.message ?: "Unable to log out of Telegram."
            )
        }

    override suspend fun close(): Result<Unit> {
        val current = client ?: run {
            _authState.value = TelegramAuthState.Closed
            return Result.success(Unit)
        }
        return try {
            _authState.value = TelegramAuthState.Closing
            sendRequest(TdApi.Close())
            client = null
            _authState.value = TelegramAuthState.Closed
            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }
}
