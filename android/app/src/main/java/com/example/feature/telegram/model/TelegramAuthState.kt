package com.example.feature.telegram.model

/**
 * Represents the official Telegram TDLib authorization lifecycle states.
 * Mapped from TDLib's internal TdApi.AuthorizationState updates.
 */
sealed interface TelegramAuthState {
    /**
     * TDLib client is not yet initialized or waiting for start signal.
     */
    data object Uninitialized : TelegramAuthState

    /**
     * Client is starting up, generating cryptographic database key and loading libraries.
     */
    data object Connecting : TelegramAuthState

    /**
     * TDLib is waiting for application parameters (API ID, API Hash, database directory, encryption key).
     */
    data object WaitTdLibParameters : TelegramAuthState

    /**
     * TDLib is waiting for user phone number in international format (+1234567890).
     */
    data class WaitPhoneNumber(val error: String? = null) : TelegramAuthState

    /**
     * TDLib sent a verification code (SMS, Telegram app, or phone call) and awaits user entry.
     */
    data class WaitCode(
        val phoneNumber: String,
        val codeLength: Int = 5,
        val deliveryType: String = "Telegram",
        val timeoutSeconds: Int = 60,
        val error: String? = null
    ) : TelegramAuthState

    /**
     * Account has Two-Step Verification (2FA Cloud Password) enabled.
     */
    data class WaitPassword(
        val passwordHint: String? = null,
        val hasRecoveryEmail: Boolean = false,
        val error: String? = null
    ) : TelegramAuthState

    /**
     * New user account requiring first name and optional last name.
     */
    data class WaitRegistration(
        val error: String? = null
    ) : TelegramAuthState

    /**
     * Successfully authorized with Telegram TDLib.
     */
    data class Authenticated(
        val user: TelegramUser
    ) : TelegramAuthState

    /**
     * Session is closing down gracefully.
     */
    data object Closing : TelegramAuthState

    /**
     * TDLib client has shut down.
     */
    data object Closed : TelegramAuthState

    /**
     * Unrecoverable error during initialization or operation.
     */
    data class Error(
        val message: String,
        val errorCode: Int? = null,
        val isRecoverable: Boolean = true
    ) : TelegramAuthState
}
