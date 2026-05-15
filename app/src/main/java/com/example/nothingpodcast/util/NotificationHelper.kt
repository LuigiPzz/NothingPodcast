package com.example.nothingpodcast.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nothingpodcast.MainActivity
import com.example.nothingpodcast.R
import com.example.nothingpodcast.domain.model.Podcast

object NotificationHelper {
    private const val CHANNEL_UPDATES = "podcast_updates"
    private const val CHANNEL_DOWNLOADS = "podcast_downloads"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val updatesChannel = NotificationChannel(
                CHANNEL_UPDATES,
                "Nuovi Episodi",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifiche per nuovi episodi dei podcast seguiti"
            }

            val downloadsChannel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Download",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Stato dei download degli episodi"
            }

            manager.createNotificationChannel(updatesChannel)
            manager.createNotificationChannel(downloadsChannel)
        }
    }

    fun showNewEpisodeNotification(context: Context, podcast: Podcast, episodeTitle: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_nothing_play)
            .setContentTitle("Nuovo episodio: ${podcast.title}")
            .setContentText(episodeTitle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(podcast.id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Permission missing
        }
    }
}
