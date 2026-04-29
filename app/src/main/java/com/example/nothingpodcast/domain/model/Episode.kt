package com.example.nothingpodcast.domain.model

/**
 * Domain model representing a single Podcast Episode.
 */
data class Episode(
    val id: String,                    // GUID from RSS feed
    val podcastId: String,
    val podcastTitle: String,
    val podcastImageUrl: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String,
    val duration: Long,                // duration in seconds
    val publishDate: Long,             // epoch milliseconds
    val isPlayed: Boolean = false,
    val playbackPosition: Long = 0L,   // seconds already played
    val isDownloaded: Boolean = false,
    val downloadPath: String? = null,
    val fileSize: Long = 0L,
    val downloadProgress: Float = 0f,  // 0..1
    val chapters: List<Chapter> = emptyList(),
    val transcriptUrl: String? = null,
    val transcriptType: String? = null,
    val soundbites: List<Soundbite> = emptyList(),
    val podcastGuid: String? = null,
    val season: Int? = null,
    val episodeNumber: Int? = null,
    val episodeType: String? = null // trailer, bonus, full
)

data class Chapter(
    val title: String,
    val startTime: Long, // in seconds
    val duration: Long = 0L,
    val imageUrl: String? = null
)
