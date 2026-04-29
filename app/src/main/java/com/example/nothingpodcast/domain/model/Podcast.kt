package com.example.nothingpodcast.domain.model

/**
 * Domain model representing a Podcast.
 * Decoupled from both network DTOs and Room entities.
 */
data class Podcast(
    val id: String,               // iTunes collectionId (as String) or feed URL hash
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String,
    val feedUrl: String,
    val episodeCount: Int = 0,
    val unplayedCount: Int = 0,
    val isSubscribed: Boolean = false,
    val lastUpdated: Long = 0L,
    val orderIndex: Int = 0,
    val fundingUrl: String? = null,
    val fundingText: String? = null,
    val persons: List<Person> = emptyList(),
    val locationName: String? = null,
    val locationGeo: String? = null,
    val socialInteractUrl: String? = null,
    val medium: String? = null,
    val podcastGuid: String? = null,
    val licenseUrl: String? = null,
    val licenseName: String? = null
)
