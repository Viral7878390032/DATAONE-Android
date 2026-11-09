package com.example.feature.telegram.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.OpenableColumns
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.telegram.model.TelegramAuthState
import com.example.feature.telegram.model.TelegramUser
import com.example.feature.telegram.model.TelegramUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Premium Dark Theme Palette for DATAONE Telegram Module
private val BackgroundDark = Color(0xFF0B111A)
private val SurfaceDark = Color(0xFF131B27)
private val SurfaceCard = Color(0xFF182333)
private val BorderHairline = Color(0xFF223348)
private val TelegramBlue = Color(0xFF229ED9)
private val TelegramBlueGradiantEnd = Color(0xFF1877A9)
private val AccentCyan = Color(0xFF50A2E9)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)
private val TextMuted = Color(0xFF64748B)
private val StatusSuccess = Color(0xFF10B981)
private val StatusWarning = Color(0xFFF59E0B)
private val StatusError = Color(0xFFEF4444)

/**
 * Premium Dark Telegram Connection Screen for DATAONE.
 *
 * Implements real TDLib authentication states without fake login simulation.
 * Preserves coexistence with Supabase and Gmail modules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramScreen(
    viewModel: TelegramViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Telegram",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(if (uiState.authState is TelegramAuthState.Authenticated) Modifier else Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Keep the same clean brand header through every login step.
            if (uiState.authState !is TelegramAuthState.Authenticated) {
                TelegramBrandHero()
            }

            // Dynamic Authorization Card driven by TDLib AuthState
            when (val state = uiState.authState) {
                is TelegramAuthState.Uninitialized -> {
                    UninitializedStateCard(
                        isLoading = uiState.isLoading,
                        onConnectClick = { viewModel.startConnection() }
                    )
                }

                is TelegramAuthState.Closed -> {
                    ErrorStateCard(
                        errorMessage = "Telegram engine closed the session before DATAONE confirmed your account. Tap Retry Connection and note any message that appears.",
                        isRecoverable = true,
                        onRetry = { viewModel.startConnection() }
                    )
                }

                is TelegramAuthState.Connecting -> {
                    ConnectingStateCard()
                }

                is TelegramAuthState.WaitTdLibParameters -> {
                    WaitParametersCard()
                }

                is TelegramAuthState.WaitPhoneNumber -> {
                    AuthStepIndicator(step = 1, title = "Your phone number", subtitle = "Start by entering the number linked to Telegram.")
                    PhoneNumberInputCard(
                        phoneNumber = uiState.phoneInput,
                        isLoading = uiState.isLoading,
                        onPhoneChange = { viewModel.onPhoneChanged(it) },
                        onSubmit = { viewModel.submitPhoneNumber() }
                    )
                }

                is TelegramAuthState.WaitCode -> {
                    AuthStepIndicator(step = 2, title = "Verify your number", subtitle = "Enter the login code sent by Telegram.")
                    CodeVerificationCard(
                        phoneNumber = state.phoneNumber,
                        code = uiState.codeInput,
                        deliveryType = state.deliveryType,
                        isLoading = uiState.isLoading,
                        onCodeChange = { viewModel.onCodeChanged(it) },
                        onSubmit = { viewModel.submitCode() }
                    )
                }

                is TelegramAuthState.WaitPassword -> {
                    AuthStepIndicator(step = 3, title = "Extra account security", subtitle = "Complete your Telegram two-step verification.")
                    TwoFactorPasswordCard(
                        password = uiState.passwordInput,
                        hint = state.passwordHint,
                        isPasswordVisible = uiState.isPasswordVisible,
                        isLoading = uiState.isLoading,
                        onPasswordChange = { viewModel.onPasswordChanged(it) },
                        onToggleVisibility = { viewModel.togglePasswordVisibility() },
                        onSubmit = { viewModel.submitPassword() }
                    )
                }

                is TelegramAuthState.WaitRegistration -> {
                    AuthStepIndicator(step = 3, title = "Create your profile", subtitle = "Add your name to finish Telegram sign-up.")
                    RegistrationCard(
                        firstName = uiState.registrationFirstName,
                        lastName = uiState.registrationLastName,
                        isLoading = uiState.isLoading,
                        onFirstNameChange = { viewModel.onRegistrationFirstNameChanged(it) },
                        onLastNameChange = { viewModel.onRegistrationLastNameChanged(it) },
                        onSubmit = { viewModel.submitRegistration() }
                    )
                }

                is TelegramAuthState.Authenticated -> {
                    AuthenticatedUserCard(
                        user = state.user,
                        isLoading = uiState.isLoading,
                        chats = uiState.chats,
                        contacts = uiState.contacts,
                        isLoadingContacts = uiState.isLoadingContacts,
                        onSearchContacts = { viewModel.searchContacts(it) },
                        onOpenPrivateChat = { viewModel.openPrivateChat(it) },
                        messages = uiState.messages,
                        selectedChatId = uiState.selectedChatId,
                        isLoadingChats = uiState.isLoadingChats,
                        messageDraft = uiState.messageDraft,
                        onMessageDraftChange = { viewModel.onMessageDraftChanged(it) },
                        onSendMessage = { replyToId -> viewModel.sendMessage(replyToId) },
                        onForwardMessage = { fromChat, message, toChat -> viewModel.forwardMessage(fromChat, message, toChat) },
                        onEditMessage = { messageId, text -> viewModel.editTextMessage(messageId, text) },
                        onDeleteMessage = { viewModel.deleteMessage(it) },
                        onReactMessage = { id, emoji -> viewModel.addMessageReaction(id, emoji) },
                        onToggleMessagePinned = { id, pinned -> viewModel.toggleMessagePinned(id, pinned) },
                        onSendPhoto = { path -> viewModel.sendPhotoMessage(path) },
                        onSendVideo = { path -> viewModel.sendVideoMessage(path) },
                        onSendDocument = { path -> viewModel.sendDocumentMessage(path) },
                        onSendAudio = { path -> viewModel.sendAudioMessage(path) },
                        onSendVoice = { path, duration -> viewModel.sendVoiceMessage(path, duration) },
                        onDownloadMedia = { viewModel.downloadMessageMedia(it) },
                        onRefreshChats = { viewModel.refreshChats() },
                        onToggleChatPinned = { chatId, isPinned -> viewModel.toggleChatPinned(chatId, isPinned) },
                        onToggleChatMarkedUnread = { chatId, marked -> viewModel.toggleChatMarkedUnread(chatId, marked) },
                        onToggleChatArchived = { chatId, archived -> viewModel.toggleChatArchived(chatId, archived) },
                        onToggleChatMuted = { chatId, muted -> viewModel.toggleChatMuted(chatId, muted) },
                        onOpenChat = { viewModel.openChat(it) },
                        onLoadOlderMessages = { viewModel.loadOlderMessages() },
                        onSearchMessages = { viewModel.searchChatMessages(it) },
                        onLoadSharedMedia = { viewModel.loadSharedMedia(it) },
                        onLoadChatDetails = { viewModel.loadChatDetails(it) },
                        chatDetails = uiState.chatDetails,
                        isLoadingChatDetails = uiState.isLoadingChatDetails,
                        onCloseChat = { viewModel.closeChat() },
                        onLogout = { viewModel.logout() }
                    )
                }

                is TelegramAuthState.Closing -> {
                    ClosingSessionCard()
                }

                is TelegramAuthState.Error -> {
                    ErrorStateCard(
                        errorMessage = state.message,
                        isRecoverable = state.isRecoverable,
                        onRetry = { viewModel.startConnection() }
                    )
                }

                else -> {
                    ConnectingStateCard()
                }
            }

        }
    }

}

@Composable
private fun TelegramBrandHero() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 22.dp)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(TelegramBlue, TelegramBlueGradiantEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Telegram",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Telegram",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Your conversations, together in DATAONE.",
            fontSize = 14.sp,
            color = TextSecondary
        )
    }
}
@Composable
private fun AuthStepIndicator(
    step: Int,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (index < step) TelegramBlue else BorderHairline)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "STEP $step OF 3",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = TelegramBlue
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun MissingNativeLibraryBanner(onConfigureClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF281C10),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5B3A1C)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = StatusWarning,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TDLib Native JNI Binary Required",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFDE68A)
                )
                Text(
                    text = "Clean interface active. Native libtdjni.so builds independently without simulated mock sessions.",
                    fontSize = 12.sp,
                    color = Color(0xFFF3D299)
                )
            }
        }
    }
}

@Composable
private fun UninitializedStateCard(
    isLoading: Boolean,
    onConnectClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Connect your Telegram",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sign in with your Telegram phone number to see your chats and media in DATAONE.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onConnectClick,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TelegramBlue,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue with Telegram",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneNumberInputCard(
    phoneNumber: String,
    isLoading: Boolean,
    onPhoneChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Enter Phone Number",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Enter your phone number with your country code. We will send you a Telegram login code.",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("+91 98765 43210", color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone Icon",
                        tint = TelegramBlue
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TelegramBlue,
                    unfocusedBorderColor = BorderHairline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onSubmit,
                enabled = !isLoading && phoneNumber.length >= 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TelegramBlue,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Send Login Code", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CodeVerificationCard(
    phoneNumber: String,
    code: String,
    deliveryType: String,
    isLoading: Boolean,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Verification Code",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Code dispatched via $deliveryType to $phoneNumber.",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("12345", color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Code Icon",
                        tint = TelegramBlue
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TelegramBlue,
                    unfocusedBorderColor = BorderHairline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onSubmit,
                enabled = !isLoading && code.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TelegramBlue,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Confirm Code", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TwoFactorPasswordCard(
    password: String,
    hint: String?,
    isPasswordVisible: Boolean,
    isLoading: Boolean,
    onPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Two-Step Verification (2FA)",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = hint?.let { "Your Telegram account has 2FA enabled. Hint: $it" }
                    ?: "Your Telegram account has 2FA enabled. Enter your Cloud Password.",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cloud Password", color = TextMuted) },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Password",
                            tint = TextSecondary
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TelegramBlue,
                    unfocusedBorderColor = BorderHairline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onSubmit,
                enabled = !isLoading && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TelegramBlue,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Unlock Session", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedUserCard(
    user: TelegramUser,
    isLoading: Boolean,
    chats: List<com.example.feature.telegram.model.TelegramChat>,
    contacts: List<TelegramUser>,
    isLoadingContacts: Boolean,
    onSearchContacts: (String) -> Unit,
    onOpenPrivateChat: (Long) -> Unit,
    messages: List<com.example.feature.telegram.model.TelegramMessage>,
    selectedChatId: Long?,
    isLoadingChats: Boolean,
    messageDraft: String,
    onMessageDraftChange: (String) -> Unit,
    onSendMessage: (Long?) -> Unit,
    onForwardMessage: (Long, Long, Long) -> Unit,
    onEditMessage: (Long, String) -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onReactMessage: (Long, String) -> Unit,
    onToggleMessagePinned: (Long, Boolean) -> Unit,
    onSendPhoto: (String) -> Unit,
    onSendVideo: (String) -> Unit,
    onSendDocument: (String) -> Unit,
    onSendAudio: (String) -> Unit,
    onSendVoice: (String, Int) -> Unit,
    onDownloadMedia: (Int) -> Unit,
    onRefreshChats: () -> Unit,
    onToggleChatPinned: (Long, Boolean) -> Unit,
    onToggleChatMarkedUnread: (Long, Boolean) -> Unit,
    onToggleChatArchived: (Long, Boolean) -> Unit,
    onToggleChatMuted: (Long, Boolean) -> Unit,
    onOpenChat: (Long) -> Unit,
    onLoadOlderMessages: () -> Unit,
    onSearchMessages: (String) -> Unit,
    onLoadSharedMedia: (String) -> Unit,
    onLoadChatDetails: (Long) -> Unit,
    chatDetails: com.example.feature.telegram.model.TelegramChatDetails?,
    isLoadingChatDetails: Boolean,
    onCloseChat: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var voiceRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var voiceRecordingFile by remember { mutableStateOf<File?>(null) }
    var voiceRecordingStartedAt by remember { mutableStateOf(0L) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            try {
                if (Build.VERSION.SDK_INT < 29) {
                    Toast.makeText(context, "Voice-note recording requires Android 10 or newer.", Toast.LENGTH_LONG).show()
                } else {
                    val file = File(context.cacheDir, "telegram-voice-${System.currentTimeMillis()}.ogg")
                    val recorder = MediaRecorder()
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.OGG)
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                    recorder.setAudioSamplingRate(48000)
                    recorder.setAudioEncodingBitRate(64000)
                    recorder.setOutputFile(file.absolutePath)
                    recorder.prepare()
                    recorder.start()
                    voiceRecordingFile = file
                    voiceRecorder = recorder
                    voiceRecordingStartedAt = System.currentTimeMillis()
                    isRecordingVoice = true
                }
            } catch (error: Exception) {
                voiceRecorder?.release()
                voiceRecorder = null
                isRecordingVoice = false
                Toast.makeText(context, error.message ?: "Unable to start recording.", Toast.LENGTH_LONG).show()
            }
        } else Toast.makeText(context, "Microphone permission is required to record a voice message.", Toast.LENGTH_LONG).show()
    }
    var chatSearchQuery by remember { mutableStateOf("") }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var contactSearchQuery by remember { mutableStateOf("") }
    LaunchedEffect(showNewChatDialog) { if (showNewChatDialog) onSearchContacts("") }
    LaunchedEffect(contactSearchQuery, showNewChatDialog) {
        if (showNewChatDialog) { kotlinx.coroutines.delay(300); onSearchContacts(contactSearchQuery) }
    }
    var showUnreadOnly by remember { mutableStateOf(false) }
    var showArchivedOnly by remember { mutableStateOf(false) }
    var showMutedOnly by remember { mutableStateOf(false) }
    var sortChatsByName by remember { mutableStateOf(false) }
    val visibleChats = chats.filter { it.isArchived == showArchivedOnly }
    val unreadChatsCount = chats.count { !it.isArchived && (it.unreadCount > 0 || it.isMarkedAsUnread) }
    val filteredChats = visibleChats.filter { chat ->
        (!showUnreadOnly || chat.unreadCount > 0 || chat.isMarkedAsUnread) &&
            (!showMutedOnly || chat.isMuted) &&
            (chat.title.contains(chatSearchQuery, ignoreCase = true) ||
                chat.lastMessage.contains(chatSearchQuery, ignoreCase = true))
    }.sortedWith(
        compareByDescending<com.example.feature.telegram.model.TelegramChat> { it.isPinned }
            .thenBy { if (sortChatsByName) it.title.lowercase() else "" }
            .thenByDescending { if (sortChatsByName) 0 else it.lastMessageDate }
    )
    var conversationSearchQuery by remember(selectedChatId) { mutableStateOf("") }
    var showConversationSearch by remember(selectedChatId) { mutableStateOf(false) }
    LaunchedEffect(selectedChatId, conversationSearchQuery) {
        if (selectedChatId != null && showConversationSearch) {
            kotlinx.coroutines.delay(350)
            onSearchMessages(conversationSearchQuery)
        }
    }
    var showSharedMedia by remember(selectedChatId) { mutableStateOf(false) }
    var sharedMediaTab by remember(selectedChatId) { mutableStateOf("All") }
    var showChatDetails by remember(selectedChatId) { mutableStateOf(false) }
    val linkPattern = remember { Regex("""(https?://\\S+|www\\.\\S+)""", RegexOption.IGNORE_CASE) }
    val isSharedLink: (com.example.feature.telegram.model.TelegramMessage) -> Boolean = { message ->
        linkPattern.containsMatchIn(message.text)
    }
    val sharedMediaMessages = messages.filter { message ->
        message.mediaType in listOf("photo", "video", "audio", "voice", "document") || isSharedLink(message)
    }
    val filteredMessages = messages.filter { message ->
        val matchesSearch = message.text.contains(conversationSearchQuery, ignoreCase = true)
        val matchesTab = !showSharedMedia || when (sharedMediaTab) {
            "Photos" -> message.mediaType == "photo"
            "Videos" -> message.mediaType == "video"
            "Files" -> message.mediaType in listOf("document", "audio", "voice")
            "Links" -> isSharedLink(message)
            else -> message.mediaType in listOf("photo", "video", "audio", "voice", "document") || isSharedLink(message)
        }
        matchesSearch && matchesTab
    }
    val sharedMediaCount = sharedMediaMessages.size
    var replyTarget by remember(selectedChatId) { mutableStateOf<com.example.feature.telegram.model.TelegramMessage?>(null) }
    var forwardTarget by remember(selectedChatId) { mutableStateOf<com.example.feature.telegram.model.TelegramMessage?>(null) }
    var editTarget by remember(selectedChatId) { mutableStateOf<com.example.feature.telegram.model.TelegramMessage?>(null) }
    var editDraft by remember(selectedChatId) { mutableStateOf("") }
    var deleteTarget by remember(selectedChatId) { mutableStateOf<com.example.feature.telegram.model.TelegramMessage?>(null) }
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false; contactSearchQuery = "" },
            containerColor = SurfaceCard,
            title = { Text("New message", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = contactSearchQuery, onValueChange = { contactSearchQuery = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search your Telegram contacts") })
                    if (isLoadingContacts) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = TelegramBlue) }
                    else if (contacts.isEmpty()) Text("No contacts found. Add contacts in Telegram first, then search here.", color = TextSecondary)
                    else Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                        contacts.forEach { contact ->
                            TextButton(onClick = { onOpenPrivateChat(contact.id); showNewChatDialog = false; contactSearchQuery = "" }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(contact.displayName, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(contact.handle.ifBlank { "Telegram contact" }, color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNewChatDialog = false }) { Text("Close", color = TelegramBlue) } }
        )
    }
    if (showChatDetails) {
        val details = chatDetails
        AlertDialog(
            onDismissRequest = { showChatDetails = false },
            title = { Text("Chat details", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isLoadingChatDetails) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TelegramBlue)
                        }
                    } else if (details != null) {
                        Text(details.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(details.chatType, color = TextSecondary)
                        Text("Username: " + if (details.username.isBlank()) "Not available" else "@${details.username}", color = TextPrimary)
                        Text("Phone: " + if (details.phoneNumber.isBlank()) "Not available" else details.phoneNumber, color = TextPrimary)
                        Text("Chat ID: ${details.chatId}", color = TextSecondary, fontSize = 12.sp)
                    } else {
                        Text("Chat details are not available.", color = TextSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChatDetails = false; showSharedMedia = true }) {
                    Text("Shared media", color = TelegramBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChatDetails = false }) { Text("Close") }
            }
        )
    }
        if (deleteTarget != null) AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete message?") }, text = { Text("Delete this message for everyone where Telegram permits?") }, confirmButton = { TextButton(onClick = { deleteTarget?.let { onDeleteMessage(it.id) }; deleteTarget = null }) { Text("Delete", color = Color(0xFFFF7777)) } }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } })
    if (editTarget != null) {
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Edit message") },
            text = {
                OutlinedTextField(
                    value = editDraft,
                    onValueChange = { editDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Message text") },
                    minLines = 2,
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editTarget?.let { onEditMessage(it.id, editDraft) }
                    editTarget = null
                }, enabled = editDraft.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text("Cancel") } }
        )
    }
    if (forwardTarget != null) {
        AlertDialog(
            onDismissRequest = { forwardTarget = null },
            title = { Text("Forward message to") },
            text = {
                Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    chats.filter { it.id != selectedChatId }.forEach { destination ->
                        TextButton(onClick = {
                            val message = forwardTarget
                            val sourceChat = selectedChatId
                            if (message != null && sourceChat != null) onForwardMessage(sourceChat, message.id, destination.id)
                            forwardTarget = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(destination.title, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { forwardTarget = null }) { Text("Cancel") } }
        )
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val copiedFile = withContext(Dispatchers.IO) {
                    val destination = File(context.cacheDir, "telegram-photo-${System.currentTimeMillis()}.jpg")
                    val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                        destination.outputStream().use { output -> input.copyTo(output) }
                        destination
                    }
                    copied
                }
                if (copiedFile != null) onSendPhoto(copiedFile.absolutePath)
            }
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val copiedFile = withContext(Dispatchers.IO) {
                    val destination = File(context.cacheDir, "telegram-video-${System.currentTimeMillis()}.mp4")
                    val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                        destination.outputStream().use { output -> input.copyTo(output) }
                        destination
                    }
                    copied
                }
                if (copiedFile != null) onSendVideo(copiedFile.absolutePath)
            }
        }
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val copiedFile = withContext(Dispatchers.IO) {
                    val displayName = context.contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: "attachment-${System.currentTimeMillis()}"
                    val safeName = displayName.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(100)
                        .ifBlank { "attachment.bin" }
                    val destination = File(context.cacheDir, "telegram-file-${System.currentTimeMillis()}-$safeName")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destination.outputStream().use { output -> input.copyTo(output) }
                        destination
                    }
                }
                if (copiedFile != null) onSendDocument(copiedFile.absolutePath)
            }
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val copiedFile = withContext(Dispatchers.IO) {
                    val destination = File(context.cacheDir, "telegram-audio-${System.currentTimeMillis()}.audio")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destination.outputStream().use { output -> input.copyTo(output) }
                        destination
                    }
                }
                if (copiedFile != null) onSendAudio(copiedFile.absolutePath)
            }
        }
    }
    Surface(color = BackgroundDark, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectedChatId == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(SurfaceDark).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(TelegramBlue), contentAlignment = Alignment.Center) {
                        Text(user.firstName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Telegram", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(user.displayName, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                    }
                    IconButton(onClick = onRefreshChats, enabled = !isLoadingChats) {
                        if (isLoadingChats) CircularProgressIndicator(color = TelegramBlue, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, contentDescription = "Refresh chats", tint = TelegramBlue)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (showArchivedOnly) "Archived chats" else "All chats", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { showNewChatDialog = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("+ New chat", color = TelegramBlue, fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = { sortChatsByName = !sortChatsByName },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHairline)
                    ) {
                        Text(if (sortChatsByName) "Sort: Name" else "Sort: Recent", fontSize = 11.sp, color = TelegramBlue)
                    }
                    Text("${visibleChats.size}", color = TextSecondary, fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = chatSearchQuery,
                    onValueChange = { chatSearchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("Search chats or recent messages", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = BorderHairline,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        cursorColor = TelegramBlue
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showUnreadOnly = false; showArchivedOnly = false; showMutedOnly = false },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (!showUnreadOnly && !showArchivedOnly && !showMutedOnly) TelegramBlue.copy(alpha = 0.18f) else SurfaceDark,
                            contentColor = if (!showUnreadOnly && !showArchivedOnly && !showMutedOnly) TelegramBlue else TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (!showUnreadOnly && !showArchivedOnly && !showMutedOnly) TelegramBlue else BorderHairline),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) { Text("All (${chats.size})", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = { showUnreadOnly = true; showArchivedOnly = false; showMutedOnly = false },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (showUnreadOnly && !showArchivedOnly) TelegramBlue.copy(alpha = 0.18f) else SurfaceDark,
                            contentColor = if (showUnreadOnly && !showArchivedOnly) TelegramBlue else TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (showUnreadOnly && !showArchivedOnly) TelegramBlue else BorderHairline),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) { Text("Unread ($unreadChatsCount)", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = { showArchivedOnly = !showArchivedOnly; showUnreadOnly = false; showMutedOnly = false },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (showArchivedOnly) TelegramBlue.copy(alpha = 0.18f) else SurfaceDark,
                            contentColor = if (showArchivedOnly) TelegramBlue else TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (showArchivedOnly) TelegramBlue else BorderHairline),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text("Archived (${chats.count { it.isArchived }})", fontSize = 12.sp) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    OutlinedButton(
                        onClick = { showMutedOnly = !showMutedOnly; showArchivedOnly = false; showUnreadOnly = false },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (showMutedOnly) TelegramBlue.copy(alpha = 0.18f) else SurfaceDark,
                            contentColor = if (showMutedOnly) TelegramBlue else TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (showMutedOnly) TelegramBlue else BorderHairline),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Muted (${chats.count { !it.isArchived && it.isMuted }})", fontSize = 12.sp)
                    }
                }
                if (chats.isEmpty() && isLoadingChats) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = TelegramBlue) }
                } else if (filteredChats.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (chats.isEmpty()) "No chats found. Tap refresh to try again." else "No chats match your search.",
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
                        items(filteredChats, key = { it.id }) { chat ->
                            var showChatActions by remember(chat.id) { mutableStateOf(false) }
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onOpenChat(chat.id) }.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val chatAvatar by produceState<android.graphics.Bitmap?>(initialValue = null, chat.photoFilePath) {
                                    value = if (chat.photoFilePath.isNullOrBlank()) null else withContext(Dispatchers.IO) {
                                        runCatching { BitmapFactory.decodeFile(chat.photoFilePath) }.getOrNull()
                                    }
                                }
                                Box(
                                    modifier = Modifier.size(52.dp).clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(TelegramBlue.copy(alpha = 0.9f), AccentCyan.copy(alpha = 0.65f)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (chatAvatar != null) {
                                        Image(bitmap = chatAvatar!!.asImageBitmap(), contentDescription = "${chat.title} profile photo",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                    } else {
                                        Text(chat.title.trim().take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(13.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        if (chat.isPinned) Text("📌", fontSize = 11.sp, modifier = Modifier.padding(end = 5.dp))
                                        Text(chat.title, modifier = Modifier.weight(1f), color = if (chat.unreadCount > 0 || chat.isMarkedAsUnread) Color.White else TextPrimary, fontWeight = if (chat.unreadCount > 0 || chat.isMarkedAsUnread) FontWeight.Bold else FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                                        if (chat.isOnline) Text("●", color = Color(0xFF35C875), fontSize = 10.sp, modifier = Modifier.padding(end = 5.dp))
                                        if (chat.isMuted) Text("🔕", fontSize = 12.sp, modifier = Modifier.padding(end = 5.dp))
                                        if (chat.lastMessageDate > 0) Text(formatTelegramChatTime(chat.lastMessageDate), color = TextMuted, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            chat.presenceText ?: chat.lastMessage.ifBlank { " " },
                                            color = if (chat.isOnline) Color(0xFF35C875) else TextSecondary,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (chat.unreadMentionCount > 0) {
                                            Text("@ ${chat.unreadMentionCount}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(start = 5.dp).clip(RoundedCornerShape(7.dp)).background(AccentCyan.copy(alpha = 0.8f)).padding(horizontal = 5.dp, vertical = 2.dp))
                                        }
                                        if (chat.unreadCount > 0) {
                                            Box(modifier = Modifier.padding(start = 6.dp).clip(CircleShape).background(TelegramBlue).padding(horizontal = 7.dp, vertical = 3.dp), contentAlignment = Alignment.Center) {
                                                Text(if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else if (chat.isMarkedAsUnread) {
                                            Box(modifier = Modifier.padding(start = 6.dp).size(9.dp).clip(CircleShape).background(TelegramBlue))
                                        }
                                    }
                                }
                                Box {
                                    IconButton(onClick = { showChatActions = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Chat options", tint = TextMuted)
                                    }
                                    DropdownMenu(expanded = showChatActions, onDismissRequest = { showChatActions = false }) {
                                        DropdownMenuItem(text = { Text(if (chat.isPinned) "Unpin chat" else "Pin chat") }, onClick = { showChatActions = false; onToggleChatPinned(chat.id, !chat.isPinned) })
                                        DropdownMenuItem(text = { Text(if (chat.isMarkedAsUnread) "Mark as read" else "Mark as unread") }, onClick = { showChatActions = false; onToggleChatMarkedUnread(chat.id, !chat.isMarkedAsUnread) })
                                        DropdownMenuItem(text = { Text(if (chat.isMuted) "Unmute notifications" else "Mute notifications") }, onClick = { showChatActions = false; onToggleChatMuted(chat.id, !chat.isMuted) })
                                        DropdownMenuItem(text = { Text(if (chat.isArchived) "Restore from archive" else "Archive chat") }, onClick = { showChatActions = false; onToggleChatArchived(chat.id, !chat.isArchived) })
                                    }
                                }
                            }
                            androidx.compose.material3.HorizontalDivider(color = BorderHairline, thickness = 0.5.dp)
                        }
                    }
                }
            } else {
                val activeChat = chats.firstOrNull { it.id == selectedChatId }
                Row(
                    modifier = Modifier.fillMaxWidth().background(SurfaceDark).padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCloseChat) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to chats", tint = TelegramBlue)
                    }
                    val conversationAvatar by produceState<android.graphics.Bitmap?>(initialValue = null, activeChat?.photoFilePath) {
                        value = if (activeChat?.photoFilePath.isNullOrBlank()) null else withContext(Dispatchers.IO) {
                            runCatching { BitmapFactory.decodeFile(activeChat?.photoFilePath) }.getOrNull()
                        }
                    }
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(TelegramBlue, AccentCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        if (conversationAvatar != null) {
                            Image(bitmap = conversationAvatar!!.asImageBitmap(), contentDescription = "Chat profile photo",
                                modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Text(activeChat?.title?.trim()?.take(1)?.uppercase() ?: "T", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(11.dp))
                    Column(
                        modifier = Modifier.weight(1f).clickable {
                            showChatDetails = true
                            selectedChatId?.let(onLoadChatDetails)
                        }
                    ) {
                        Text(activeChat?.title ?: "Chat", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            activeChat?.presenceText ?: "Tap to view profile",
                            color = if (activeChat?.isOnline == true) Color(0xFF35C875) else TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                    TextButton(onClick = {
                        showSharedMedia = !showSharedMedia
                        if (showSharedMedia) {
                            sharedMediaTab = "All"
                            onLoadSharedMedia("All")
                        } else {
                            sharedMediaTab = "All"
                            selectedChatId?.let(onOpenChat)
                        }
                    }) {
                        Text(if (showSharedMedia) "Messages" else "Media ($sharedMediaCount)", color = TelegramBlue, fontSize = 12.sp)
                    }
                    IconButton(onClick = { showConversationSearch = !showConversationSearch; if (!showConversationSearch) conversationSearchQuery = "" }) {
                        Icon(Icons.Default.Search, contentDescription = "Search messages", tint = TelegramBlue)
                    }
                }
                if (showConversationSearch) {
                    OutlinedTextField(
                        value = conversationSearchQuery,
                        onValueChange = { conversationSearchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        placeholder = { Text("Search messages in this chat", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = TelegramBlue,
                            unfocusedBorderColor = BorderHairline,
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            cursorColor = TelegramBlue
                        )
                    )
                }
                if (showSharedMedia) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(SurfaceDark).padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("All", "Photos", "Videos", "Files", "Links").forEach { tab ->
                            TextButton(
                                onClick = { sharedMediaTab = tab; onLoadSharedMedia(tab) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    tab,
                                    color = if (sharedMediaTab == tab) TelegramBlue else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (sharedMediaTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                if (isLoadingChats && messages.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = TelegramBlue) }
                } else if (filteredMessages.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (showSharedMedia) "No ${sharedMediaTab.lowercase()} found in the loaded messages." else if (messages.isEmpty()) "No messages in this conversation." else "No messages match your search.",
                            color = TextSecondary
                        )
                    }
                } else {
                    val conversationListState = androidx.compose.foundation.lazy.rememberLazyListState()
                    LaunchedEffect(selectedChatId) {
                        if (filteredMessages.isNotEmpty()) {
                            conversationListState.scrollToItem(filteredMessages.lastIndex)
                        }
                    }
                    LaunchedEffect(messages.size, selectedChatId, showSharedMedia, conversationSearchQuery) {
                        if (!showSharedMedia && conversationSearchQuery.isBlank() && messages.isNotEmpty()) {
                            conversationListState.animateScrollToItem(messages.lastIndex)
                        }
                    }
                    LaunchedEffect(selectedChatId, conversationListState.firstVisibleItemIndex) {
                        if (!showSharedMedia && conversationSearchQuery.isBlank() && conversationListState.firstVisibleItemIndex <= 2 && filteredMessages.isNotEmpty()) {
                            onLoadOlderMessages()
                        }
                    }
                    LazyColumn(
                        state = conversationListState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        items(filteredMessages, key = { it.id }) { message ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (message.isOutgoing) Color(0xFF164C6B) else SurfaceDark,
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp, topEnd = 16.dp,
                                        bottomStart = if (message.isOutgoing) 16.dp else 4.dp,
                                        bottomEnd = if (message.isOutgoing) 4.dp else 16.dp
                                    ),
                                    modifier = Modifier.fillMaxWidth(0.86f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        if (message.mediaType == "photo") {
                                            TelegramPhotoContent(message = message, onDownloadMedia = onDownloadMedia)
                                        }
                                        if (message.mediaType == "video") {
                                            TelegramVideoContent(message = message, onDownloadMedia = onDownloadMedia)
                                        }
                                        if (message.mediaType == "audio" || message.mediaType == "voice") {
                                            TelegramAudioContent(message = message, onDownloadMedia = onDownloadMedia)
                                        }
                                        if (message.mediaType == "document") {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.AttachFile, contentDescription = "File", tint = TelegramBlue)
                                                Text("Shared file", color = TextPrimary, modifier = Modifier.weight(1f).padding(start = 8.dp))
                                                TextButton(onClick = { message.mediaFileId?.let(onDownloadMedia) }, enabled = message.mediaFileId != null) { Text("Download") }
                                            }
                                        }
                                        if (message.isPinned) {
                                            Text("📌 Pinned message", color = AccentCyan, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(
                                                onClick = { onToggleMessagePinned(message.id, message.isPinned) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) { Text(if (message.isPinned) "Unpin" else "Pin", color = AccentCyan, fontSize = 11.sp) }
                                            if (message.isOutgoing && message.mediaType == null && message.text.isNotBlank()) {
                                                TextButton(
                                                    onClick = { editTarget = message; editDraft = message.text },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                ) { Text("Edit", color = TelegramBlue, fontSize = 11.sp) }
                                            }
                                            TextButton(onClick = { onReactMessage(message.id, "👍") }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text("👍", fontSize = 12.sp) }
                                            TextButton(onClick = { onReactMessage(message.id, "❤️") }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text("❤️", fontSize = 12.sp) }
                                            TextButton(onClick = { onReactMessage(message.id, "😂") }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text("😂", fontSize = 12.sp) }
                                            TextButton(
                                                onClick = { replyTarget = message },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) { Text("Reply", color = TelegramBlue, fontSize = 11.sp) }
                                            TextButton(
                                                onClick = { forwardTarget = message },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) { Text("Forward", color = TelegramBlue, fontSize = 11.sp) }
                                            TextButton(onClick = { deleteTarget = message }, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) { Text("Delete", color = Color(0xFFFF7777), fontSize = 11.sp) }
                                        }
                                        if (message.text.isNotBlank()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = if (message.mediaType == "photo") 6.dp else 0.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    message.text,
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    formatTelegramChatTime(message.date),
                                                    color = TextSecondary.copy(alpha = 0.8f),
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(start = 6.dp, top = 5.dp)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("Telegram message", message.text))
                                                        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copy message",
                                                        tint = TextSecondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (replyTarget != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(SurfaceDark).padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Replying to message", color = TelegramBlue, fontSize = 12.sp)
                            Text(replyTarget?.text?.ifBlank { "Media message" }.orEmpty(), color = TextSecondary, maxLines = 1, fontSize = 13.sp)
                        }
                        TextButton(onClick = { replyTarget = null }) { Text("Cancel") }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().background(SurfaceDark).padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isRecordingVoice) {
                                try {
                                    voiceRecorder?.stop()
                                    voiceRecorder?.release()
                                    voiceRecorder = null
                                    isRecordingVoice = false
                                    val file = voiceRecordingFile
                                    val seconds = ((System.currentTimeMillis() - voiceRecordingStartedAt) / 1000L).toInt().coerceAtLeast(1)
                                    if (file != null && file.exists() && file.length() > 0L) onSendVoice(file.absolutePath, seconds)
                                    else Toast.makeText(context, "No voice audio was captured.", Toast.LENGTH_SHORT).show()
                                } catch (error: Exception) {
                                    Toast.makeText(context, error.message ?: "Recording could not be saved.", Toast.LENGTH_LONG).show()
                                    voiceRecorder?.release()
                                    voiceRecorder = null
                                    isRecordingVoice = false
                                }
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        enabled = !isLoadingChats
                    ) {
                        Icon(
                            imageVector = if (isRecordingVoice) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecordingVoice) "Stop and send voice message" else "Record voice message",
                            tint = if (isRecordingVoice) Color(0xFFFF7777) else TelegramBlue
                        )
                    }
                    OutlinedTextField(
                        value = messageDraft,
                        onValueChange = onMessageDraftChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message", color = TextMuted) },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TelegramBlue,
                            unfocusedBorderColor = BorderHairline,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = BackgroundDark,
                            unfocusedContainerColor = BackgroundDark
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    IconButton(onClick = { photoPicker.launch("image/*") }, enabled = !isLoadingChats) {
                        Icon(Icons.Default.Image, contentDescription = "Choose photo", tint = TelegramBlue)
                    }
                    IconButton(onClick = { videoPicker.launch("video/*") }, enabled = !isLoadingChats) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = "Choose video", tint = TelegramBlue)
                    }
                    IconButton(onClick = { audioPicker.launch("audio/*") }, enabled = !isLoadingChats) {
                        Icon(Icons.Default.Audiotrack, contentDescription = "Choose audio", tint = TelegramBlue)
                    }
                    IconButton(onClick = { documentPicker.launch(arrayOf("*/*")) }, enabled = !isLoadingChats) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Choose file", tint = TelegramBlue)
                    }
                    IconButton(onClick = { onSendMessage(replyTarget?.id); replyTarget = null }, enabled = messageDraft.isNotBlank() && !isLoadingChats && !isRecordingVoice) {
                        Icon(Icons.Default.Send, contentDescription = "Send message", tint = TelegramBlue)
                    }
                }
            }
        }
    }
}
@Composable
private fun TelegramAudioContent(
    message: com.example.feature.telegram.model.TelegramMessage,
    onDownloadMedia: (Int) -> Unit
) {
    var isPlaying by remember(message.id) { mutableStateOf(false) }
    var player by remember(message.id) { mutableStateOf<MediaPlayer?>(null) }

    androidx.compose.runtime.DisposableEffect(message.id) {
        onDispose {
            player?.release()
            player = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(12.dp)
    ) {
        Text(
            if (message.mediaType == "voice") "Voice message" else "Audio",
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
        if (message.isMediaDownloading) {
            Text("Downloading audio...", color = TextSecondary, fontSize = 12.sp)
            CircularProgressIndicator(
                color = TelegramBlue,
                modifier = Modifier.padding(top = 8.dp).size(20.dp),
                strokeWidth = 2.dp
            )
        } else if (!message.mediaFilePath.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        val path = message.mediaFilePath ?: return@Button
                        if (isPlaying) {
                            player?.pause()
                            isPlaying = false
                        } else {
                            try {
                                if (player == null) {
                                    player = MediaPlayer().apply {
                                        setDataSource(path)
                                        setOnCompletionListener { isPlaying = false }
                                        prepare()
                                    }
                                }
                                player?.start()
                                isPlaying = true
                            } catch (error: Exception) {
                                player?.release()
                                player = null
                                isPlaying = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                ) {
                    Text(if (isPlaying) "Pause" else "Play")
                }
                Text(
                    if (message.mediaType == "voice") "Voice note" else "Audio file",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        } else {
            Button(
                onClick = { message.mediaFileId?.let(onDownloadMedia) },
                enabled = message.mediaFileId != null,
                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
            ) {
                Text("Download ${if (message.mediaType == "voice") "voice message" else "audio"}")
            }
        }
    }
}

@Composable
private fun TelegramPhotoContent(
    message: com.example.feature.telegram.model.TelegramMessage,
    onDownloadMedia: (Int) -> Unit
) {
    val photoBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = message.mediaFilePath
    ) {
        value = withContext(Dispatchers.IO) {
            message.mediaFilePath
                ?.takeIf { it.isNotBlank() }
                ?.let { path -> BitmapFactory.decodeFile(path)?.asImageBitmap() }
        }
    }

    if (photoBitmap != null) {
        Image(
            bitmap = photoBitmap!!,
            contentDescription = "Telegram photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (message.isMediaDownloading) "Downloading photo..." else "Photo",
                color = TextSecondary,
                fontSize = 13.sp
            )
            if (message.isMediaDownloading) {
                CircularProgressIndicator(
                    color = TelegramBlue,
                    modifier = Modifier.padding(top = 10.dp).size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Button(
                    onClick = { message.mediaFileId?.let(onDownloadMedia) },
                    enabled = message.mediaFileId != null,
                    modifier = Modifier.padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                ) {
                    Text(if (message.isMediaDownloaded) "Retry photo" else "Download photo")
                }
            }
        }
    }
}

@Composable
private fun TelegramVideoContent(
    message: com.example.feature.telegram.model.TelegramMessage,
    onDownloadMedia: (Int) -> Unit
) {
    var isPlaying by remember(message.id) { mutableStateOf(false) }
    val videoPath = message.mediaFilePath

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(12.dp)
    ) {
        if (videoPath != null && isPlaying) {
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        setVideoPath(videoPath)
                        setMediaController(MediaController(context).also { it.setAnchorView(this) })
                        setOnPreparedListener { player -> player.start() }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        } else {
            Text("Video message", color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(
                text = if (message.isMediaDownloading) "Downloading video..." else "Video is not downloaded",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (message.isMediaDownloading) {
                CircularProgressIndicator(
                    color = TelegramBlue,
                    modifier = Modifier.padding(top = 10.dp).size(22.dp),
                    strokeWidth = 2.dp
                )
            } else if (videoPath != null) {
                Button(
                    onClick = { isPlaying = true },
                    modifier = Modifier.padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                ) {
                    Text("Play video")
                }
            } else {
                Button(
                    onClick = { message.mediaFileId?.let(onDownloadMedia) },
                    enabled = message.mediaFileId != null,
                    modifier = Modifier.padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                ) {
                    Text("Download video")
                }
            }
        }
    }
}

@Composable
private fun RegistrationCard(
    firstName: String,
    lastName: String,
    isLoading: Boolean,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Complete Telegram Registration", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Your number is verified. Enter your name to finish setting up your Telegram account.", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("First name") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Last name (optional)") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSubmit,
                enabled = !isLoading && firstName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Finish Registration")
            }
        }
    }
}

@Composable
private fun ConnectingStateCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = TelegramBlue,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connecting to TDLib Engine...",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Initializing Keystore-encrypted database and MTProto transport.",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun WaitParametersCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            CircularProgressIndicator(color = TelegramBlue, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Preparing Telegram",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Please wait while DATAONE connects securely.",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ClosingSessionCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = StatusWarning, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Closing Telegram session...", color = TextPrimary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ErrorStateCard(
    errorMessage: String,
    isRecoverable: Boolean,
    onRetry: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF261313),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5A2020)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "TDLib Engine State",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFCA5A5)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage,
                fontSize = 13.sp,
                color = Color(0xFFFECACA),
                lineHeight = 18.sp
            )
            if (isRecoverable) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B))
                ) {
                    Text("Retry Connection", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DataoneSecurityPolicyCard() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Architecture Notice",
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DATAONE Architecture Invariants",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "• Zero Auto-Download: Photos, videos, and files are downloaded strictly on-demand.\n" +
                        "• SQLite Encryption: TDLib database encrypted with AES-256 key from Android Keystore.\n" +
                        "• Coexistence: Operates isolated from Supabase Auth and Gmail integrations.\n" +
                        "• Zero Secrets in Code: API credentials managed dynamically without hardcoding.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ApiCredentialsDialog(
    apiId: String,
    apiHash: String,
    onApiIdChange: (String) -> Unit,
    onApiHashChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = {
            Text(
                text = "Configure Telegram API",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Obtain from my.telegram.org. Stored securely in Android Keystore, never in Git.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = apiId,
                    onValueChange = onApiIdChange,
                    label = { Text("API ID (numeric)", color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = BorderHairline
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = apiHash,
                    onValueChange = onApiHashChange,
                    label = { Text("API Hash (32-char hex)", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = BorderHairline
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
            ) {
                Text("Save to Keystore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}


private fun formatTelegramChatTime(timestamp: Int): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp * 1000L))
}
