package com.example.nothingpodcast.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nothingpodcast.data.repository.PodcastRepository
import com.example.nothingpodcast.util.AppLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class PodcastUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val podcastRepository: PodcastRepository,
    private val preferencesDataStore: com.example.nothingpodcast.data.local.datastore.UserPreferencesDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        AppLogger.log(applicationContext, "INFO", "PodcastUpdateWorker: Starting background update check")
        
        return try {
            val podcasts = podcastRepository.getSubscribedPodcasts().first()
            val notificationsEnabled = preferencesDataStore.notificationsEnabled.first()
            
            if (podcasts.isEmpty()) {
                AppLogger.log(applicationContext, "INFO", "PodcastUpdateWorker: No subscriptions found, skipping")
                return Result.success()
            }

            AppLogger.log(applicationContext, "INFO", "PodcastUpdateWorker: Refreshing ${podcasts.size} podcasts")
            
            podcasts.forEach { podcast ->
                try {
                    val (newCount, latestTitle) = podcastRepository.refreshEpisodes(podcast)
                    if (newCount > 0 && notificationsEnabled && latestTitle != null) {
                        com.example.nothingpodcast.util.NotificationHelper.showNewEpisodeNotification(
                            applicationContext,
                            podcast,
                            latestTitle
                        )
                    }
                } catch (e: Exception) {
                    AppLogger.log(applicationContext, "ERROR", "PodcastUpdateWorker: Failed to refresh ${podcast.title}: ${e.message}")
                }
            }

            AppLogger.log(applicationContext, "INFO", "PodcastUpdateWorker: Background update completed successfully")
            Result.success()
        } catch (e: Exception) {
            AppLogger.log(applicationContext, "ERROR", "PodcastUpdateWorker: Critical failure: ${e.message}")
            Result.retry()
        }
    }
}
