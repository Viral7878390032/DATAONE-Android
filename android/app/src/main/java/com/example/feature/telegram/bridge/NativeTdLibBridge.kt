package com.example.feature.telegram.bridge

import android.util.Log
import com.example.feature.telegram.model.TelegramAuthState
import com.example.feature.telegram.model.TelegramUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.concurrent.atomic.AtomicBoolean
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
                        if (update is TdApi.UpdateAuthorizationState) {
                            scope.launch { handleAuthorizationState(update.authorizationState) }
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
                is TdApi.AuthorizationStateWaitEncryptionKey -> {
                    sendRequest(TdApi.CheckDatabaseEncryptionKey(params.databaseEncryptionKey))
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
                }
                is TdApi.AuthorizationStateLoggingOut -> _authState.value = TelegramAuthState.Closing
                is TdApi.AuthorizationStateClosing -> _authState.value = TelegramAuthState.Closing
                is TdApi.AuthorizationStateClosed -> {
                    client = null
                    _authState.value = TelegramAuthState.Closed
                }
                else -> {
                    Log.w(tag, "Unhandled TDLib authorization state: ${state.javaClass.simpleName}")
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
