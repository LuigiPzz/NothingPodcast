package com.example.nothingpodcast.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.nothingpodcast.data.repository.EpisodeRepository
import com.example.nothingpodcast.data.local.datastore.UserPreferencesDataStore
import com.example.nothingpodcast.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val episodeRepository: EpisodeRepository,
    private val okHttpClient: OkHttpClient,
    private val glyphManager: com.example.nothingpodcast.util.NothingGlyphManager,
    private val preferencesDataStore: UserPreferencesDataStore
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val episodeId  = inputData.getString(KEY_EPISODE_ID)  ?: return@withContext Result.failure()
        val audioUrl   = inputData.getString(KEY_AUDIO_URL)   ?: return@withContext Result.failure()
        val fileName   = inputData.getString(KEY_FILE_NAME)   ?: return@withContext Result.failure()

        val outputDir = File(applicationContext.filesDir, "podcasts").apply { mkdirs() }
        val outputFile = File(outputDir, fileName)

        try {
            val request = Request.Builder().url(audioUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    outputFile.delete()
                    return@withContext Result.retry()
                }
                val body = response.body ?: run {
                    outputFile.delete()
                    return@withContext Result.retry()
                }
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                var lastGlyphUpdate = 0

                FileOutputStream(outputFile).use { fos ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            
                            if (totalBytes > 0) {
                                val progress = (downloadedBytes * 100 / totalBytes).toInt()
                                setProgress(workDataOf(
                                    KEY_PROGRESS to progress,
                                    KEY_EPISODE_ID to episodeId
                                ))
                                
                                // Update LEDs every 2% for a smooth "flowing" effect
                                if (progress >= lastGlyphUpdate + 2) {
                                    glyphManager.showDownloadProgress(progress)
                                    lastGlyphUpdate = progress
                                }
                            }
                        }
                    }
                }
            }
            episodeRepository.markDownloaded(episodeId, outputFile.absolutePath)
            
            // All LEDs on!
            glyphManager.showDownloadComplete()

            try {
                val prefEnabled = preferencesDataStore.downloadCompletedNotificationEnabled.first()
                if (prefEnabled) {
                    val episode = episodeRepository.getEpisodeById(episodeId)
                    if (episode != null) {
                        NotificationHelper.showDownloadCompletedNotification(applicationContext, episode.title, episode.id)
                    }
                }
            } catch (e: Exception) {
                com.example.nothingpodcast.util.AppLogger.e("Failed to show download completed notification", e)
            }
            
            Result.success(workDataOf(KEY_FILE_PATH to outputFile.absolutePath))
        } catch (e: Exception) {
            outputFile.delete()
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    companion object {
        const val TAG_DOWNLOADS = "download_worker_all"
        const val KEY_EPISODE_ID = "episode_id"
        const val KEY_AUDIO_URL  = "audio_url"
        const val KEY_FILE_NAME  = "file_name"
        const val KEY_PROGRESS   = "progress"
        const val KEY_FILE_PATH  = "file_path"
        const val KEY_ERROR      = "error"

        /** Build a unique work tag for this episode */
        fun tag(episodeId: String) = "download_$episodeId"
    }
}
