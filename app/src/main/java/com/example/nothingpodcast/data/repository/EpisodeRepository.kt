package com.example.nothingpodcast.data.repository

import com.example.nothingpodcast.data.local.database.dao.EpisodeDao
import com.example.nothingpodcast.data.mapper.toDomain
import com.example.nothingpodcast.domain.model.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeRepository @Inject constructor(
    private val episodeDao: EpisodeDao
) {
    fun getEpisodesForPodcast(podcastId: String): Flow<List<Episode>> =
        episodeDao.getEpisodesForPodcast(podcastId).map { it.map { e -> e.toDomain() } }

    fun getDownloadedEpisodes(): Flow<List<Episode>> =
        episodeDao.getDownloadedEpisodes().map { it.map { e -> e.toDomain() } }

    fun getUnplayedCount(podcastId: String): Flow<Int> =
        episodeDao.getUnplayedCount(podcastId)

    suspend fun getEpisodeById(id: String): Episode? =
        episodeDao.getEpisodeById(id)?.toDomain()

    suspend fun getLastPlayedEpisode(): Episode? =
        episodeDao.getLastPlayedEpisode()?.toDomain()

    suspend fun markPlayed(episodeId: String, position: Long = 0L) {
        episodeDao.updatePlaybackState(episodeId, played = true, position = position)
    }

    suspend fun markUnplayed(episodeId: String) {
        episodeDao.updatePlaybackState(episodeId, played = false, position = 0L)
    }

    suspend fun savePlaybackPosition(episodeId: String, positionSeconds: Long) {
        // Use position-only update to preserve the isPlayed flag
        episodeDao.updatePlaybackPosition(episodeId, positionSeconds)
    }

    suspend fun markDownloaded(episodeId: String, localPath: String) {
        episodeDao.updateDownloadState(episodeId, downloaded = true, path = localPath)
    }

    suspend fun markNotDownloaded(episodeId: String) {
        // Only reset the download state — do NOT delete the episode from the DB
        episodeDao.updateDownloadState(episodeId, downloaded = false, path = null)
    }

    fun getTotalEpisodeCount(): Flow<Int> = episodeDao.getTotalCount()
    fun getPlayedEpisodeCount(): Flow<Int> = episodeDao.getPlayedCount()
}
