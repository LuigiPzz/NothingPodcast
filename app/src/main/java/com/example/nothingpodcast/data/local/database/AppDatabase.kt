package com.example.nothingpodcast.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.nothingpodcast.data.local.database.dao.EpisodeDao
import com.example.nothingpodcast.data.local.database.dao.PodcastDao
import com.example.nothingpodcast.data.local.database.entity.EpisodeEntity
import com.example.nothingpodcast.data.local.database.entity.PodcastEntity

@Database(
    entities = [PodcastEntity::class, EpisodeEntity::class],
    version = 5,
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

        /**
         * v3 → v4: aggiunta colonne metadati estesi agli episodi
         * (Podcast Namespace: chapters, transcript, soundbites, guid, season, episodeNumber, episodeType)
         */
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE episodes ADD COLUMN fileSize INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE episodes ADD COLUMN chaptersJson TEXT")
                database.execSQL("ALTER TABLE episodes ADD COLUMN transcriptUrl TEXT")
                database.execSQL("ALTER TABLE episodes ADD COLUMN transcriptType TEXT")
                database.execSQL("ALTER TABLE episodes ADD COLUMN soundbitesJson TEXT")
                database.execSQL("ALTER TABLE episodes ADD COLUMN podcastGuid TEXT")
                database.execSQL("ALTER TABLE episodes ADD COLUMN season INTEGER")
                database.execSQL("ALTER TABLE episodes ADD COLUMN episodeNumber INTEGER")
                database.execSQL("ALTER TABLE episodes ADD COLUMN episodeType TEXT")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE episodes ADD COLUMN lastPlayedAt INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
