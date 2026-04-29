package com.example.nothingpodcast.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class PodcastWithCount(
    @Embedded val podcast: PodcastEntity,
    @ColumnInfo(name = "unplayedCount") val unplayedCount: Int
)
