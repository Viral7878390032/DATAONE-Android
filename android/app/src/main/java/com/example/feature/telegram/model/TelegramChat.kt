package com.example.feature.telegram.model

data class TelegramChat(
    val id: Long,
    val title: String,
    val lastMessage: String,
    val lastMessageDate: Int,
    val unreadCount: Int,
    val isPinned: Boolean = false,
    val isMarkedAsUnread: Boolean = false,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val unreadMentionCount: Int = 0,
    val photoFileId: Int = 0,
    val photoFilePath: String? = null,
    val presenceText: String? = null,
    val isOnline: Boolean = false
)
