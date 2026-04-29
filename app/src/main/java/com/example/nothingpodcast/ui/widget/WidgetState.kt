package com.example.nothingpodcast.ui.widget

import kotlinx.serialization.Serializable

@Serializable
data class WidgetState(
    val podcastTitle: String = "Nothing Podcast",
    val episodeTitle: String = "Nessun episodio",
    val isPlaying: Boolean = false,
    val currentPosition: String = "00:00",
    val totalDuration: String = "00:00"
)
