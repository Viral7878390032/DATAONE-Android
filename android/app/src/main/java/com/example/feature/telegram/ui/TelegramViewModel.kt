package com.example.feature.telegram.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.telegram.data.TelegramRepository
import com.example.feature.telegram.model.TelegramAuthState
import com.example.feature.telegram.model.TelegramUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Telegram integration lifecycle, user inputs, and TDLib states.
 */
class TelegramViewModel(
    private val repository: TelegramRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TelegramUiState(
            isNativeLibraryAvailable = repository.isNativeLibraryAvailable,
            chatDrafts = repository.loadChatDrafts()
        )
    )
    val uiState: StateFlow<TelegramUiState> = _uiState.asStateFlow()

    private var refreshChatsJob: Job? = null
    private var openChatJob: Job? = null
    private var chatDetailsJob: Job? = null
    private var olderMessagesJob: Job? = null
    private var refreshChatsAgain = false

    init {
        observeAuthState()
        // TDLib keeps the authorized session in its local encrypted database.
        // Re-initialize it automatically so returning users go straight to chats.
        startConnection()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            repository.authState.collect { authState ->
                _uiState.update { current ->
                    current.copy(
                        authState = authState,
                        isLoading = authState is TelegramAuthState.Connecting || authState is TelegramAuthState.Closing,
                        errorMessage = when (authState) {
                            is TelegramAuthState.Error -> authState.message
                            is TelegramAuthState.WaitPhoneNumber -> authState.error
                            is TelegramAuthState.WaitCode -> authState.error
                            is TelegramAuthState.WaitPassword -> authState.error
                            else -> null
                        }
                    )
                }
                if (authState is TelegramAuthState.Authenticated) refreshChats()
            }
        }
        viewModelScope.launch {
            repository.chats.collect { chats -> _uiState.update { it.copy(chats = chats) } }
        }
        viewModelScope.launch {
            repository.messages.collect { messages -> _uiState.update { it.copy(messages = messages) } }
        }
    }

    fun searchContacts(query: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingContacts = true, errorMessage = null) }
            val result = repository.searchContacts(query)
            _uiState.update { it.copy(contacts = result.getOrDefault(emptyList()), isLoadingContacts = false, errorMessage = result.exceptionOrNull()?.message) }
        }
    }

    fun createGroupChat(title: String, userIds: List<Long>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChats = true, errorMessage = null) }
            repository.createGroupChat(title, userIds).onSuccess { chatId ->
                openChat(chatId)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoadingChats = false, errorMessage = error.message ?: "Could not create group.") }
            }
        }
    }

    fun renameGroupChat(chatId: Long, title: String) {
        viewModelScope.launch {
            val result = repository.renameGroupChat(chatId, title)
            _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            if (result.isSuccess) loadChatDetails(chatId)
        }
    }

    fun addGroupMembers(chatId: Long, userIds: List<Long>) {
        viewModelScope.launch {
            val result = repository.addGroupMembers(chatId, userIds)
            _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            if (result.isSuccess) loadChatDetails(chatId)
        }
    }

    fun leaveGroupChat(chatId: Long) {
        viewModelScope.launch {
            val result = repository.leaveGroupChat(chatId)
            _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            if (result.isSuccess) closeChat()
        }
    }

    fun openPrivateChat(userId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChats = true, errorMessage = null) }
            val result = repository.openPrivateChat(userId)
            result.onSuccess { chatId -> openChat(chatId) }
                .onFailure { error -> _uiState.update { it.copy(isLoadingChats = false, errorMessage = error.message ?: "Unable to open contact chat.") } }
        }
    }

    fun refreshChats() {
        if (refreshChatsJob?.isActive == true) {
            refreshChatsAgain = true
            return
        }
        refreshChatsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChats = true, errorMessage = null) }
            var latestError: String? = null
            do {
                refreshChatsAgain = false
                val result = try {
                    repository.refreshChats()
                } catch (error: Exception) {
                    Result.failure(error)
                }
                latestError = result.exceptionOrNull()?.message
            } while (refreshChatsAgain)
            _uiState.update { it.copy(isLoadingChats = false, errorMessage = latestError) }
        }
    }

    fun toggleChatPinned(chatId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleChatPinned(chatId, isPinned)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Unable to update pinned chat.") }
            }
        }
    }

    fun toggleChatMarkedUnread(chatId: Long, isMarkedAsUnread: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleChatMarkedUnread(chatId, isMarkedAsUnread)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Unable to update unread status.") }
            }
        }
    }

    fun toggleChatArchived(chatId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleChatArchived(chatId, isArchived)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Unable to archive chat.") }
            }
        }
    }

    fun toggleChatMuted(chatId: Long, isMuted: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleChatMuted(chatId, isMuted)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Unable to update chat notifications.") }
            }
        }
    }

    fun loadChatDetails(chatId: Long) {
        chatDetailsJob?.cancel()
        chatDetailsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChatDetails = true, chatDetails = null, errorMessage = null) }
            val result = try {
                repository.getChatDetails(chatId)
            } catch (error: Exception) {
                Result.failure(error)
            }
            if (_uiState.value.selectedChatId == chatId) {
                _uiState.update {
                    it.copy(
                        isLoadingChatDetails = false,
                        chatDetails = result.getOrNull(),
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    fun openChat(chatId: Long) {
        openChatJob?.cancel()
        openChatJob = viewModelScope.launch {
            _uiState.update { current ->
                val drafts = current.selectedChatId?.let { current.chatDrafts + (it to current.messageDraft) } ?: current.chatDrafts
                current.copy(
                    isLoadingChats = true,
                    selectedChatId = chatId,
                    messageDraft = drafts[chatId].orEmpty(),
                    chatDrafts = drafts,
                    chatDetails = null,
                    isLoadingChatDetails = false,
                    errorMessage = null
                )
            }
            repository.saveChatDrafts(_uiState.value.chatDrafts)
            val result = try {
                repository.openChat(chatId)
            } catch (error: Exception) {
                Result.failure(error)
            }
            if (_uiState.value.selectedChatId == chatId) {
                _uiState.update { it.copy(isLoadingChats = false, errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun searchChatMessages(query: String) {
        val chatId = _uiState.value.selectedChatId ?: return
        viewModelScope.launch {
            val result = repository.searchChatMessages(chatId, query)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Message search failed.") }
            }
        }
    }

    fun loadSharedMedia(category: String) {
        val chatId = _uiState.value.selectedChatId ?: return
        viewModelScope.launch {
            val result = repository.loadSharedMedia(chatId, category)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Unable to load shared media.") }
            }
        }
    }

    fun loadOlderMessages() {
        val chatId = _uiState.value.selectedChatId ?: return
        if (olderMessagesJob?.isActive == true) return
        olderMessagesJob = viewModelScope.launch {
            val result = try {
                repository.loadOlderMessages(chatId)
            } catch (error: Exception) {
                Result.failure(error)
            }
            if (_uiState.value.selectedChatId == chatId && result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun closeChat() {
        _uiState.update { current ->
            val chatId = current.selectedChatId
            val drafts = if (chatId != null) current.chatDrafts + (chatId to current.messageDraft) else current.chatDrafts
            current.copy(selectedChatId = null, chatDrafts = drafts)
        }
        repository.saveChatDrafts(_uiState.value.chatDrafts)
    }

    fun onMessageDraftChanged(value: String) {
        _uiState.update { current ->
            val chatId = current.selectedChatId
            val drafts = if (chatId != null) current.chatDrafts + (chatId to value) else current.chatDrafts
            current.copy(messageDraft = value, chatDrafts = drafts)
        }
        repository.saveChatDrafts(_uiState.value.chatDrafts)
    }

    fun downloadMessageMedia(fileId: Int) {
        viewModelScope.launch {
            val result = repository.requestMediaDownload(fileId)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Unable to download photo.") }
            }
        }
    }

    private fun beginSendingMessage(): Boolean {
        // Use compareAndSet so simultaneous taps cannot both pass the guard.
        while (true) {
            val current = _uiState.value
            if (current.isSendingMessage) return false
            val sending = current.copy(isSendingMessage = true, errorMessage = null)
            if (_uiState.compareAndSet(current, sending)) return true
        }
    }

    /**
     * Runs every outgoing message through one guarded path. The guard is released even
     * when a repository implementation throws, and a send finishing after the user
     * switches chats will never erase the new chat's draft.
     */
    private fun performMessageSend(
        chatId: Long,
        clearDraftOnSuccess: Boolean = true,
        send: suspend () -> Result<Unit>
    ) {
        if (!beginSendingMessage()) return
        viewModelScope.launch {
            val result = try {
                send()
            } catch (error: Exception) {
                Result.failure(error)
            }
            _uiState.update { current ->
                val succeeded = result.isSuccess
                val stillInSendingChat = current.selectedChatId == chatId
                current.copy(
                    isSendingMessage = false,
                    messageDraft = if (succeeded && clearDraftOnSuccess && stillInSendingChat) "" else current.messageDraft,
                    chatDrafts = if (succeeded && clearDraftOnSuccess) current.chatDrafts - chatId else current.chatDrafts,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
            repository.saveChatDrafts(_uiState.value.chatDrafts)
        }
    }

    fun sendMessage(replyToMessageId: Long? = null) {
        val chatId = _uiState.value.selectedChatId ?: return
        val message = _uiState.value.messageDraft.trim()
        if (message.isBlank()) return
        performMessageSend(chatId) {
            repository.sendTextMessage(chatId, message, replyToMessageId)
        }
    }

    fun forwardMessage(fromChatId: Long, messageId: Long, toChatId: Long) {
        viewModelScope.launch {
            val result = repository.forwardMessage(fromChatId, messageId, toChatId)
            _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
        }
    }

    fun editTextMessage(messageId: Long, newText: String) {
        val chatId = _uiState.value.selectedChatId ?: return
        if (newText.isBlank()) return
        viewModelScope.launch {
            val result = repository.editTextMessage(chatId, messageId, newText.trim())
            _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
        }
    }

    fun deleteMessage(messageId: Long) {
        val chatId = _uiState.value.selectedChatId ?: return
        viewModelScope.launch {
            val result = repository.deleteMessage(chatId, messageId)
            _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
        }
    }

    fun addMessageReaction(messageId: Long, emoji: String) {
        val chatId = _uiState.value.selectedChatId ?: return
        if (emoji.isBlank()) return
        viewModelScope.launch {
            val result = repository.addMessageReaction(chatId, messageId, emoji)
            _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
        }
    }

    fun toggleMessagePinned(messageId: Long, isPinned: Boolean) {
        val chatId = _uiState.value.selectedChatId ?: return
        viewModelScope.launch {
            val result = repository.toggleMessagePinned(chatId, messageId, isPinned)
            _uiState.update {
                it.copy(errorMessage = result.exceptionOrNull()?.message ?: if (result.isSuccess) null else "Unable to update pinned message.")
            }
        }
    }

    fun sendPhotoMessage(path: String) {
        val chatId = _uiState.value.selectedChatId ?: return
        if (path.isBlank()) return
        val caption = _uiState.value.messageDraft.trim()
        performMessageSend(chatId) { repository.sendPhotoMessage(chatId, path, caption) }
    }

    fun sendVideoMessage(path: String) {
        val chatId = _uiState.value.selectedChatId ?: return
        if (path.isBlank()) return
        val caption = _uiState.value.messageDraft.trim()
        performMessageSend(chatId) { repository.sendVideoMessage(chatId, path, caption) }
    }

    fun sendAudioMessage(path: String) {
        val chatId = _uiState.value.selectedChatId ?: return
        if (path.isBlank()) return
        val caption = _uiState.value.messageDraft.trim()
        performMessageSend(chatId) { repository.sendAudioMessage(chatId, path, caption) }
    }

    fun sendVoiceMessage(path: String, durationSeconds: Int) {
        val chatId = _uiState.value.selectedChatId ?: return
        if (path.isBlank()) return
        performMessageSend(chatId, clearDraftOnSuccess = false) {
            repository.sendVoiceMessage(chatId, path, durationSeconds)
        }
    }

    fun sendDocumentMessage(path: String) {
        val chatId = _uiState.value.selectedChatId ?: return
        if (path.isBlank()) return
        val caption = _uiState.value.messageDraft.trim()
        performMessageSend(chatId) { repository.sendDocumentMessage(chatId, path, caption) }
    }

    fun startConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.startTelegram()
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to start Telegram. Please try again."
                    )
                }
            }
        }
    }

    fun onPhoneChanged(newPhone: String) {
        // Keep valid phone characters (+ and digits)
        val digits = newPhone.filter { it.isDigit() }.take(15)
        val filtered = (if (newPhone.trimStart().startsWith("+")) "+" else "") + digits
        _uiState.update { it.copy(phoneInput = filtered, errorMessage = null) }
    }

    fun onCodeChanged(newCode: String) {
        val filtered = newCode.filter { it.isDigit() }.take(8)
        _uiState.update { it.copy(codeInput = filtered, errorMessage = null) }
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.update { it.copy(passwordInput = newPassword, errorMessage = null) }
    }

    fun onRegistrationFirstNameChanged(value: String) {
        _uiState.update { it.copy(registrationFirstName = value, errorMessage = null) }
    }

    fun onRegistrationLastNameChanged(value: String) {
        _uiState.update { it.copy(registrationLastName = value, errorMessage = null) }
    }

    fun submitRegistration() {
        if (_uiState.value.isLoading) return
        val firstName = _uiState.value.registrationFirstName.trim().take(64)
        val lastName = _uiState.value.registrationLastName.trim().take(64)
        if (firstName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your first name to continue.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.submitRegistration(firstName, lastName)
            result.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Registration failed.") }
            }
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun submitPhoneNumber() {
        if (_uiState.value.isLoading) return
        val phone = _uiState.value.phoneInput.trim()
        if (phone.length < 9 || !phone.startsWith("+") || phone.drop(1).length !in 7..15) {
            _uiState.update {
                it.copy(errorMessage = "Enter phone number in international format starting with + (e.g. +1234567890)")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.submitPhoneNumber(phone)
            result.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Failed to submit phone number") }
            }
        }
    }

    fun submitCode() {
        if (_uiState.value.isLoading) return
        val code = _uiState.value.codeInput.trim()
        if (code.length !in 4..8) {
            _uiState.update { it.copy(errorMessage = "Verification code cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.submitAuthCode(code)
            result.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Invalid code") }
            }
        }
    }

    fun submitPassword() {
        if (_uiState.value.isLoading) return
        val password = _uiState.value.passwordInput
        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Password cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.submit2FaPassword(password)
            result.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Incorrect 2FA password") }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.logout()
            if (result.isSuccess) {
                repository.clearChatDrafts()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        phoneInput = "",
                        codeInput = "",
                        passwordInput = "",
                        registrationFirstName = "",
                        registrationLastName = "",
                        messageDraft = "",
                        chatDrafts = emptyMap(),
                        selectedChatId = null,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Unable to log out. Please try again."
                    )
                }
            }
        }
    }

    fun openApiCredentialDialog() {
        _uiState.update { it.copy(showApiCredentialDialog = true) }
    }

    fun dismissApiCredentialDialog() {
        _uiState.update { it.copy(showApiCredentialDialog = false) }
    }

    fun onApiIdChanged(apiId: String) {
        _uiState.update { it.copy(apiIdInput = apiId.filter { char -> char.isDigit() }) }
    }

    fun onApiHashChanged(apiHash: String) {
        _uiState.update { it.copy(apiHashInput = apiHash.trim()) }
    }

    fun saveApiCredentials() {
        val apiId = _uiState.value.apiIdInput.toIntOrNull()
        val apiHash = _uiState.value.apiHashInput.trim()

        if (apiId == null || apiId <= 0 || apiHash.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Valid Telegram API ID and Hash are required.") }
            return
        }

        viewModelScope.launch {
            repository.configureApiCredentials(apiId, apiHash)
            _uiState.update { it.copy(showApiCredentialDialog = false, errorMessage = null) }
            startConnection()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
