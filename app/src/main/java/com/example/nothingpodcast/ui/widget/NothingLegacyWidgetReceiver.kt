package com.example.nothingpodcast.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.nothingpodcast.R
import com.example.nothingpodcast.service.PodcastPlaybackService

class NothingLegacyWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.very_simple_layout)
        
        // Open App Intent
        val openAppIntent = Intent(context, com.example.nothingpodcast.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_PLAYER", true)
        }
        val openAppPending = PendingIntent.getActivity(context, 20, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_player_container, openAppPending)
        views.setOnClickPendingIntent(R.id.simple_progress_image, openAppPending)
        views.setOnClickPendingIntent(R.id.widget_empty_cover, openAppPending)

        // Play/Pause - Click on container for better touch area
        val playPauseIntent = Intent(context, PodcastPlaybackService::class.java).apply {
            action = PodcastPlaybackService.COMMAND_PLAY_PAUSE
        }
        val playPausePending = PendingIntent.getForegroundService(context, 11, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.simple_btn_play_container, playPausePending)

        // Skip Back
        val skipBackIntent = Intent(context, PodcastPlaybackService::class.java).apply {
            action = PodcastPlaybackService.COMMAND_SKIP_BACKWARD
        }
        val skipBackPending = PendingIntent.getForegroundService(context, 10, skipBackIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.simple_btn_back, skipBackPending)

        // Skip Forward
        val skipForwardIntent = Intent(context, PodcastPlaybackService::class.java).apply {
            action = PodcastPlaybackService.COMMAND_SKIP_FORWARD
        }
        val skipForwardPending = PendingIntent.getForegroundService(context, 12, skipForwardIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.simple_btn_forward, skipForwardPending)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
