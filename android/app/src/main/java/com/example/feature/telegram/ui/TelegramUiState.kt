package com.example.feature.telegram.ui

import com.example.feature.telegram.model.TelegramAuthState
import com.example.feature.telegram.model.TelegramChat
import com.example.feature.telegram.model.TelegramMessage

/**
 * Immutable UI state for TelegramScreen.
 */
data class TelegramUiState(
    val authState: TelegramAuthState = TelegramAuthState.Uninitialized,
    val phoneInput: String = "",
    val codeInput: String = "",
    val passwordInput: String = "",
    val registrationFirstName: String = "",
    val registrationLastName: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isNativeLibraryAvailable: Boolean = true,
    val showApiCredentialDialog: Boolean = false,
    val apiIdInput: String = "",
    val apiHashInput: String = "",
    val chats: List<TelegramChat> = emptyList(),
    val messages: List<TelegramMessage> = emptyList(),
    val selectedChatId: Long? = null,
    val isLoadingChats: Boolean = false,
    val messageDraft: String = "",
    val chatDrafts: Map<Long, String> = emptyMap()
)
