package com.example.nothingpodcast.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.nothingpodcast.data.local.database.dao.EpisodeDao
import com.example.nothingpodcast.data.local.database.dao.PodcastDao
import com.example.nothingpodcast.data.local.database.entity.EpisodeEntity
import com.example.nothingpodcast.data.local.database.entity.PodcastEntity

@Database(
    entities = [PodcastEntity::class, EpisodeEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao

    companion object {
        const val DATABASE_NAME = "nothing_podcast_db"

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE podcasts ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
