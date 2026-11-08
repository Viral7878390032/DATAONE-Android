package com.example.feature.telegram.model

/**
 * Domain representation of an authenticated Telegram user.
 * Kept clean of native TDLib object dependencies to ensure modularity.
 */
data class TelegramUser(
    val id: Long,
    val firstName: String,
    val lastName: String = "",
    val username: String? = null,
    val phoneNumber: String = "",
    val profilePhotoLocalPath: String? = null,
    val isPremium: Boolean = false,
    val isVerified: Boolean = false
) {
    val displayName: String
        get() = if (lastName.isBlank()) firstName else "$firstName $lastName"

    val handle: String
        get() = username?.let { "@$it" } ?: phoneNumber
}
