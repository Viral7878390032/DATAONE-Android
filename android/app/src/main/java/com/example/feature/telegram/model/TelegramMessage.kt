package com.example.feature.telegram.model

data class TelegramMessage(
    val id: Long,
    val chatId: Long,
    val text: String,
    val date: Int,
    val isOutgoing: Boolean,
    val mediaType: String? = null,
    val mediaFileId: Int? = null,
    val mediaFilePath: String? = null,
    val isMediaDownloaded: Boolean = false,
    val isMediaDownloading: Boolean = false,
    val isPinned: Boolean = false,
    val previewFileId: Int? = null,
    val previewFilePath: String? = null,
    val mediaFileName: String? = null
)
