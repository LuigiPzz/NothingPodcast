package com.example.nothingpodcast.data.repository

import com.example.nothingpodcast.data.local.database.dao.EpisodeDao
import com.example.nothingpodcast.data.local.database.dao.PodcastDao
import com.example.nothingpodcast.data.local.database.entity.PodcastEntity
import com.example.nothingpodcast.data.local.datastore.UserPreferencesDataStore
import com.example.nothingpodcast.data.remote.google.GoogleDriveService
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val userPrefs: UserPreferencesDataStore,
    private val driveService: GoogleDriveService,
    private val podcastRepository: PodcastRepository // Inject this
) {
    private val gson = Gson()

    data class SyncData(
        val version: Int = 1,
        val podcasts: List<PodcastSyncInfo>,
        val settings: Map<String, Any?>
    )

    data class PodcastSyncInfo(
        val id: String, // Add ID
        val feedUrl: String,
        val title: String,
        val orderIndex: Int,
        val playedEpisodes: List<String>
    )

    suspend fun upload(account: GoogleSignInAccount): Result<Unit> = runCatching {
        val podcasts = podcastDao.getSubscribedPodcasts().first()
        val syncInfo = podcasts.map { p ->
            val played = episodeDao.getPlayedEpisodesForPodcast(p.id).first()
            PodcastSyncInfo(
                id = p.id,
                feedUrl = p.feedUrl,
                title = p.title,
                orderIndex = p.orderIndex,
                playedEpisodes = played.map { it.id }
            )
        }
        val data = SyncData(podcasts = syncInfo, settings = emptyMap())
        driveService.uploadSyncFile(account, gson.toJson(data))
    }

    suspend fun downloadAndApply(account: GoogleSignInAccount): Result<Int> = runCatching {
        val json = driveService.downloadSyncFile(account) ?: return Result.success(0)
        val data = gson.fromJson(json, SyncData::class.java)
        applyRemoteData(data)
        data.podcasts.size
    }

    private suspend fun applyRemoteData(data: SyncData) {
        val localPodcasts = podcastDao.getSubscribedPodcasts().first()
        val localUrls = localPodcasts.map { it.feedUrl }.toSet()

        data.podcasts.forEach { remote ->
            // 1. Subscribe if missing
            if (remote.feedUrl !in localUrls) {
                try {
                    podcastRepository.subscribeByUrl(remote.feedUrl)
                } catch (_: Exception) {}
            }

            // 2. Update order
            podcastDao.updateOrderIndex(remote.id, remote.orderIndex)

            // 3. Sync played episodes
            remote.playedEpisodes.forEach { episodeId ->
                episodeDao.updatePlaybackState(episodeId, true, 0)
            }
        }
    }
}
