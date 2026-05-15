package com.example.nothingpodcast.util

import android.content.Context
import androidx.work.*
import com.example.nothingpodcast.worker.PodcastUpdateWorker
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val UPDATE_WORK_NAME = "podcast_periodic_update"

    fun schedulePodcastUpdates(
        context: Context,
        enabled: Boolean,
        intervalHours: Int,
        wifiOnly: Boolean
    ) {
        val workManager = WorkManager.getInstance(context)

        if (!enabled) {
            workManager.cancelUniqueWork(UPDATE_WORK_NAME)
            AppLogger.log(context, "INFO", "WorkScheduler: Periodic updates disabled, work cancelled")
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val updateRequest = PeriodicWorkRequestBuilder<PodcastUpdateWorker>(
            intervalHours.toLong(), TimeUnit.HOURS,
            15, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .addTag("background_update")
            .build()

        workManager.enqueueUniquePeriodicWork(
            UPDATE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            updateRequest
        )
        
        AppLogger.log(context, "INFO", "WorkScheduler: Periodic updates scheduled every $intervalHours hours (WiFi only: $wifiOnly)")
    }
}
