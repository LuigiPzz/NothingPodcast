package com.example.nothingpodcast.data.local.database.dao

import androidx.room.*
import com.example.nothingpodcast.data.local.database.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishDate DESC")
    fun getEpisodesForPodcast(podcastId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE isDownloaded = 1 ORDER BY publishDate DESC")
    fun getDownloadedEpisodes(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getEpisodeById(id: String): EpisodeEntity?

    @Query("SELECT COUNT(*) FROM episodes WHERE podcastId = :podcastId AND isPlayed = 0")
    fun getUnplayedCount(podcastId: String): Flow<Int>

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId AND isPlayed = 1")
    fun getPlayedEpisodesForPodcast(podcastId: String): Flow<List<EpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    /** Insert but keep existing playback/download state intact */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEpisodesIgnoreExisting(episodes: List<EpisodeEntity>)

    @Transaction
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>): List<EpisodeEntity> {
        val newEpisodes = mutableListOf<EpisodeEntity>()
        episodes.forEach { episode ->
            val existing = getEpisodeById(episode.id)
            if (existing != null) {
                updateMetadata(
                    id           = episode.id,
                    title        = episode.title,
                    description  = episode.description,
                    duration     = episode.duration,
                    imageUrl     = episode.imageUrl,
                    chaptersJson = episode.chaptersJson
                )
            } else {
                insertEpisodes(listOf(episode))
                newEpisodes.add(episode)
            }
        }
        return newEpisodes
    }

    @Query("UPDATE episodes SET title = :title, description = :description, duration = :duration, imageUrl = :imageUrl, chaptersJson = :chaptersJson WHERE id = :id")
    suspend fun updateMetadata(id: String, title: String, description: String, duration: Long, imageUrl: String, chaptersJson: String?)

    @Query("UPDATE episodes SET isPlayed = :played, playbackPosition = :position, lastPlayedAt = :lastPlayed WHERE id = :id")
    suspend fun updatePlaybackState(id: String, played: Boolean, position: Long, lastPlayed: Long = System.currentTimeMillis())

    @Query("UPDATE episodes SET isDownloaded = :downloaded, downloadPath = :path WHERE id = :id")
    suspend fun updateDownloadState(id: String, downloaded: Boolean, path: String?)

    /** Update position ONLY — does not touch the isPlayed flag */
    @Query("UPDATE episodes SET playbackPosition = :position, lastPlayedAt = :lastPlayed WHERE id = :id")
    suspend fun updatePlaybackPosition(id: String, position: Long, lastPlayed: Long = System.currentTimeMillis())

    @Query("SELECT * FROM episodes ORDER BY lastPlayedAt DESC LIMIT 1")
    suspend fun getLastPlayedEpisode(): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId AND isDownloaded = 1")
    fun getDownloadedEpisodesForPodcast(podcastId: String): Flow<List<EpisodeEntity>>

    @Query("DELETE FROM episodes WHERE podcastId = :podcastId AND isDownloaded = 0")
    suspend fun deleteNonDownloadedEpisodesForPodcast(podcastId: String)

    @Query("DELETE FROM episodes WHERE podcastId = :podcastId")
    suspend fun deleteEpisodesForPodcast(podcastId: String)

    @Query("DELETE FROM episodes WHERE id = :id")
    suspend fun deleteEpisodeById(id: String)

    @Query("SELECT COUNT(*) FROM episodes")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM episodes WHERE isPlayed = 1")
    fun getPlayedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM episodes WHERE podcastId = :podcastId")
    suspend fun getCountForPodcast(podcastId: String): Int
}
