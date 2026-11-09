package com.example.feature.telegram.model

/**
 * Telegram chat/profile information returned by TDLib.
 * Fields that Telegram does not expose are kept empty.
 */
data class TelegramChatDetails(
    val chatId: Long,
    val title: String,
    val chatType: String,
    val username: String = "",
    val phoneNumber: String = ""
)
