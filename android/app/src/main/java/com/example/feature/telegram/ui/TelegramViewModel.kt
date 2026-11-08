package com.example.feature.telegram.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.telegram.data.TelegramRepository
import com.example.feature.telegram.model.TelegramAuthState
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
            isNativeLibraryAvailable = repository.isNativeLibraryAvailable
        )
    )
    val uiState: StateFlow<TelegramUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
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
            }
        }
    }

    fun startConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.startTelegram()
        }
    }

    fun onPhoneChanged(newPhone: String) {
        // Keep valid phone characters (+ and digits)
        val filtered = newPhone.filter { it == '+' || it.isDigit() }
        _uiState.update { it.copy(phoneInput = filtered, errorMessage = null) }
    }

    fun onCodeChanged(newCode: String) {
        val filtered = newCode.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(codeInput = filtered, errorMessage = null) }
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.update { it.copy(passwordInput = newPassword, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun submitPhoneNumber() {
        val phone = _uiState.value.phoneInput.trim()
        if (phone.length < 8 || !phone.startsWith("+")) {
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
        val code = _uiState.value.codeInput.trim()
        if (code.isBlank()) {
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
            _uiState.update { it.copy(isLoading = true) }
            repository.logout()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    phoneInput = "",
                    codeInput = "",
                    passwordInput = ""
                )
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
