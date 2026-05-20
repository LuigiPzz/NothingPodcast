package com.example.nothingpodcast.di

import android.content.Context
import androidx.room.Room
import com.example.nothingpodcast.data.local.database.AppDatabase
import com.example.nothingpodcast.data.local.database.dao.EpisodeDao
import com.example.nothingpodcast.data.local.database.dao.PodcastDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .build()

    @Provides
    fun providePodcastDao(db: AppDatabase): PodcastDao = db.podcastDao()

    @Provides
    fun provideEpisodeDao(db: AppDatabase): EpisodeDao = db.episodeDao()
}
