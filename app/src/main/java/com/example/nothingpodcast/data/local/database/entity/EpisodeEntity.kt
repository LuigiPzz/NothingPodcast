package com.example.nothingpodcast.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("podcastId")]
)
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val podcastId: String,
    val podcastTitle: String,
    val podcastImageUrl: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String,
    val duration: Long,           // seconds
    val publishDate: Long,        // epoch ms
    val isPlayed: Boolean = false,
    val playbackPosition: Long = 0L,
    val isDownloaded: Boolean = false,
    val downloadPath: String? = null,
    val fileSize: Long = 0L,
    val chaptersJson: String? = null, // Serialized List<Chapter>
    val transcriptUrl: String? = null,
    val transcriptType: String? = null,
    val soundbitesJson: String? = null,
    val podcastGuid: String? = null,
    val season: Int? = null,
    val episodeNumber: Int? = null,
    val episodeType: String? = null,
    val lastPlayedAt: Long = 0L // epoch ms
)
