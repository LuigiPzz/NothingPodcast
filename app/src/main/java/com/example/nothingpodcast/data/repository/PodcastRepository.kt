package com.example.nothingpodcast.data.repository

import com.example.nothingpodcast.data.local.database.dao.EpisodeDao
import com.example.nothingpodcast.data.local.database.dao.PodcastDao
import com.example.nothingpodcast.data.mapper.toDomain
import com.example.nothingpodcast.data.mapper.toEntity
import com.example.nothingpodcast.data.remote.api.ItunesApiService
import com.example.nothingpodcast.data.remote.rss.RssFeedParser
import com.example.nothingpodcast.domain.model.Episode
import com.example.nothingpodcast.domain.model.Podcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import android.content.Context
import com.example.nothingpodcast.data.local.datastore.UserPreferencesDataStore
import com.example.nothingpodcast.util.WorkScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val itunesApi: ItunesApiService,
    private val rssFeedParser: RssFeedParser,
    private val preferencesDataStore: UserPreferencesDataStore
) {
    // ── Local subscriptions ───────────────────────────────────────────────

    fun getSubscribedPodcasts(): Flow<List<Podcast>> =
        podcastDao.getSubscribedPodcastsWithCount().map { items ->
            items.map { item ->
                item.podcast.toDomain(item.unplayedCount)
            }
        }

    suspend fun getPodcastById(id: String): Podcast? =
        podcastDao.getPodcastById(id)?.toDomain()

    suspend fun subscribe(podcast: Podcast) {
        val maxIndex = podcastDao.getMaxOrderIndex() ?: -1
        podcastDao.insertPodcast(podcast.copy(
            isSubscribed = true,
            orderIndex = maxIndex + 1
        ).toEntity())
        refreshEpisodes(podcast, isFirstSync = true)
    }

    suspend fun subscribeByUrl(feedUrl: String) {
        val podcast = rssFeedParser.fetchPodcastMetadata(feedUrl)
        subscribe(podcast)
    }

    suspend fun unsubscribe(podcastId: String) {
        // 1. Find all downloaded episodes for this podcast
        val downloadedEpisodes = episodeDao.getDownloadedEpisodesForPodcast(podcastId).first()
        
        // 2. Delete the physical files
        downloadedEpisodes.forEach { entity ->
            entity.downloadPath?.let { path ->
                try {
                    val file = java.io.File(path)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    // Log error if needed
                }
            }
        }

        // 3. Update status or delete episodes
        podcastDao.updateSubscriptionStatus(podcastId, false)
        episodeDao.deleteEpisodesForPodcast(podcastId)
    }

    suspend fun updatePodcastsOrder(podcasts: List<Podcast>) {
        podcasts.forEachIndexed { index, podcast ->
            podcastDao.updateOrderIndex(podcast.id, index)
        }
    }

    // ── Search ────────────────────────────────────────────────────────────

    suspend fun searchPodcasts(query: String): List<Podcast> {
        if (query.isBlank()) return emptyList()
        return itunesApi.searchPodcasts(query)
            .results
            .filter { it.feedUrl != null }
            .map { it.toDomain() }
    }

    // ── Episode refresh ───────────────────────────────────────────────────

    /** Returns Pair(count of new episodes, title of latest new episode) */
    suspend fun refreshEpisodes(podcast: Podcast, isFirstSync: Boolean = false): Pair<Int, String?> {
        val episodes = rssFeedParser.parseEpisodes(
            feedUrl       = podcast.feedUrl,
            podcastId     = podcast.id,
            podcastTitle  = podcast.title,
            podcastImageUrl = podcast.imageUrl
        )
        val newEpisodes = episodeDao.upsertEpisodes(episodes)
        podcastDao.updateLastRefreshed(podcast.id, System.currentTimeMillis())
        
        val newCount = newEpisodes.size

        if (newCount > 0 && !isFirstSync) {
            val autoDownload = preferencesDataStore.autoDownloadEnabled.first()
            if (autoDownload) {
                val wifiOnly = preferencesDataStore.autoDownloadWifiOnly.first()
                newEpisodes.forEach { newEpisode ->
                    WorkScheduler.enqueueDownload(
                        context = context,
                        episodeId = newEpisode.id,
                        audioUrl = newEpisode.audioUrl,
                        wifiOnly = wifiOnly
                    )
                }
            }
        }
        
        val latestTitle = if (newCount > 0) episodes.maxByOrNull { it.publishDate }?.title else null
        return Pair(newCount, latestTitle)
    }
}
