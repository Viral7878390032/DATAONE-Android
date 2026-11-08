package com.example.feature.telegram.model

/**
 * Enforces DATAONE on-demand media policy:
 * Automatic background downloading of photos, videos, audio, and documents is strictly disabled.
 * Media files are downloaded only when explicitly initiated by the user.
 */
data class TelegramMediaConfig(
    val autoDownloadPhotos: Boolean = false,
    val autoDownloadVideos: Boolean = false,
    val autoDownloadDocuments: Boolean = false,
    val autoDownloadVoiceNotes: Boolean = false,
    val maxThumbnailCacheSizeBytes: Long = 50 * 1024 * 1024L, // 50MB thumbnail limit
    val onDemandDownloadChunkSize: Int = 32 * 1024 // 32KB streaming chunk
) {
    companion object {
        /**
         * Production default configuration with zero automatic caching.
         */
        val STRICT_ON_DEMAND = TelegramMediaConfig(
            autoDownloadPhotos = false,
            autoDownloadVideos = false,
            autoDownloadDocuments = false,
            autoDownloadVoiceNotes = false
        )
    }
}
