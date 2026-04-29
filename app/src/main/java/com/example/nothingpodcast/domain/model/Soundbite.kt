package com.example.nothingpodcast.domain.model

/**
 * Represents a soundbite (highlight clip) from an episode.
 * Part of Podcasting 2.0 standard.
 */
data class Soundbite(
    val title: String,
    val startTime: Long, // in seconds
    val duration: Long   // in seconds
)
