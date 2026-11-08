package com.example.feature.telegram.model

data class TelegramChat(
    val id: Long,
    val title: String,
    val lastMessage: String,
    val unreadCount: Int
)

data class TelegramMessage(
    val id: Long,
    val chatId: Long,
    val text: String,
    val date: Int,
    val isOutgoing: Boolean
)
