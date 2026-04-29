package com.example.nothingpodcast.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String,
    val feedUrl: String,
    val episodeCount: Int = 0,
    val isSubscribed: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0,
    val fundingUrl: String? = null,
    val fundingText: String? = null,
    val personsJson: String? = null,
    val locationName: String? = null,
    val locationGeo: String? = null,
    val socialInteractUrl: String? = null,
    val medium: String? = null,
    val podcastGuid: String? = null,
    val licenseUrl: String? = null,
    val licenseName: String? = null
)
