package com.example.feature.telegram.model

data class TelegramMessage(
    val id: Long,
    val chatId: Long,
    val text: String,
    val date: Int,
    val isOutgoing: Boolean
)
