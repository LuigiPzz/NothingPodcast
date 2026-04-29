package com.example.nothingpodcast.service

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.example.nothingpodcast.MainActivity
import com.example.nothingpodcast.data.repository.EpisodeRepository
import com.example.nothingpodcast.data.repository.PodcastRepository
import com.example.nothingpodcast.domain.model.Episode
import com.example.nothingpodcast.domain.model.Podcast
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import com.example.nothingpodcast.R
import javax.inject.Inject

@AndroidEntryPoint
class PodcastPlaybackService : MediaLibraryService() {

    @Inject
    lateinit var podcastRepository: PodcastRepository

    @Inject
    lateinit var episodeRepository: EpisodeRepository

    private lateinit var player: ExoPlayer
    private lateinit var mediaLibrarySession: MediaLibrarySession

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionSaveJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var currentEpisodeId: String? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        com.example.nothingpodcast.util.AppLogger.log(this, "INFO", "PlaybackService: onCreate started")

        // Build ExoPlayer with audio focus handling
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Back to app intent when notification is tapped
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, object : MediaLibrarySession.Callback {
            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                return Futures.immediateFuture(LibraryResult.ofItem(getRootItem(), params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                return serviceScope.future {
                    val children = when (parentId) {
                        ROOT_ID -> listOf(getSubscriptionsCategory(), getDownloadsCategory())
                        ID_SUBSCRIPTIONS -> podcastRepository.getSubscribedPodcasts().first().map { it.toMediaItem() }
                        ID_DOWNLOADS -> episodeRepository.getDownloadedEpisodes().first().map { it.toMediaItem() }
                        else -> {
                            episodeRepository.getEpisodesForPodcast(parentId).first().map { it.toMediaItem() }
                        }
                    }
                    LibraryResult.ofItemList(children, params)
                }
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String
            ): ListenableFuture<LibraryResult<MediaItem>> {
                return serviceScope.future {
                    val episode = episodeRepository.getEpisodeById(mediaId)
                    if (episode != null) {
                        LibraryResult.ofItem(episode.toMediaItem(), null)
                    } else {
                        val podcast = podcastRepository.getPodcastById(mediaId)
                        if (podcast != null) {
                            LibraryResult.ofItem(podcast.toMediaItem(), null)
                        } else {
                            LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                        }
                    }
                }
            }

            override fun onSetMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: List<MediaItem>,
                startIndex: Int,
                startPositionMs: Long
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                return serviceScope.future {
                    // If no explicit start position is requested (C.TIME_UNSET), try to resume from DB
                    var actualStartPositionMs = startPositionMs
                    
                    // If we are starting from 0 or UNSET, check if we have a saved position
                    if ((startPositionMs == C.TIME_UNSET || startPositionMs == 0L) && mediaItems.isNotEmpty()) {
                        val firstItem = mediaItems[startIndex.coerceIn(0, mediaItems.size - 1)]
                        val episode = episodeRepository.getEpisodeById(firstItem.mediaId)
                        if (episode != null && episode.playbackPosition > 0) {
                            com.example.nothingpodcast.util.AppLogger.log(this@PodcastPlaybackService, "INFO", "Service: Resuming ${episode.title} from ${episode.playbackPosition}s")
                            actualStartPositionMs = episode.playbackPosition * 1000L
                        }
                    }
                    MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, actualStartPositionMs)
                }
            }

            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand(COMMAND_SKIP_FORWARD, android.os.Bundle.EMPTY))
                    .add(SessionCommand(COMMAND_SKIP_BACKWARD, android.os.Bundle.EMPTY))
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: android.os.Bundle
            ): ListenableFuture<androidx.media3.session.SessionResult> {
                when (customCommand.customAction) {
                    COMMAND_SKIP_FORWARD -> {
                        val newPos = (player.currentPosition + 30_000L).coerceAtMost(player.duration)
                        player.seekTo(newPos)
                    }
                    COMMAND_SKIP_BACKWARD -> {
                        val newPos = (player.currentPosition - 15_000L).coerceAtLeast(0L)
                        player.seekTo(newPos)
                    }
                }
                return Futures.immediateFuture(androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS))
            }
        })
            .setSessionActivity(sessionActivity)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val oldId = currentEpisodeId
                val newId = mediaItem?.mediaId
                com.example.nothingpodcast.util.AppLogger.log(this@PodcastPlaybackService, "INFO", "Service: Transition from $oldId to $newId (Reason: $reason)")
                
                // SAVE OLD POSITION BEFORE SWITCHING
                if (oldId != null && oldId != newId) {
                    savePositionInternal(oldId)
                }

                // Update current ID
                currentEpisodeId = newId
                updateWidget()
                super.onMediaItemTransition(mediaItem, reason)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                updateWidget()
                if (!playWhenReady) {
                    com.example.nothingpodcast.util.AppLogger.log(this@PodcastPlaybackService, "INFO", "Service: Playback paused - saving position")
                    savePositionNow()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                com.example.nothingpodcast.util.AppLogger.log(this@PodcastPlaybackService, "ERROR", "Service: Player Error: ${error.message}")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    val id = currentEpisodeId ?: return
                    serviceScope.launch(Dispatchers.IO) {
                        episodeRepository.markPlayed(id, 0L)
                        val episode = episodeRepository.getEpisodeById(id)
                        if (episode?.isDownloaded == true) {
                            episode.downloadPath?.let { path ->
                                runCatching { java.io.File(path).delete() }
                            }
                            episodeRepository.markNotDownloaded(id)
                        }
                    }
                }
            }
        })

        // Periodically save playback position to DB
        startPositionTracking()
        updateWidget()
    }

    private fun updateWidget() {
        if (!::player.isInitialized) return
        
        val podcastTitle = player.mediaMetadata.artist?.toString() ?: "Nothing"
        val episodeTitle = player.mediaMetadata.title?.toString() ?: "Nessun episodio"
        val isPlaying = player.isPlaying
        val currentPos = formatDuration(player.currentPosition)
        val duration = if (player.duration > 0) formatDuration(player.duration) else "00:00"

        serviceScope.launch {
            try {
                val context = this@PodcastPlaybackService
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(com.example.nothingpodcast.ui.widget.NothingPodcastWidget::class.java)
                
                if (glanceIds.isNotEmpty()) {
                    glanceIds.forEach { id ->
                        try {
                            androidx.glance.appwidget.state.updateAppWidgetState(context, id) { prefs ->
                                prefs[com.example.nothingpodcast.ui.widget.NothingPodcastWidget.KEY_PODCAST_TITLE] = podcastTitle
                                prefs[com.example.nothingpodcast.ui.widget.NothingPodcastWidget.KEY_EPISODE_TITLE] = episodeTitle
                                prefs[com.example.nothingpodcast.ui.widget.NothingPodcastWidget.KEY_IS_PLAYING] = isPlaying
                                prefs[com.example.nothingpodcast.ui.widget.NothingPodcastWidget.KEY_CURRENT_TIME] = currentPos
                                prefs[com.example.nothingpodcast.ui.widget.NothingPodcastWidget.KEY_TOTAL_TIME] = duration
                            }
                            com.example.nothingpodcast.ui.widget.NothingPodcastWidget().update(context, id)
                        } catch (e: Exception) {
                            com.example.nothingpodcast.util.AppLogger.log(context, "ERROR", "Failed to update specific widget: ${e.message}")
                        }
                    }
                }

                // --- LEGACY WIDGET UPDATE ---
                val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                val componentName = android.content.ComponentName(context, com.example.nothingpodcast.ui.widget.NothingLegacyWidgetReceiver::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                if (appWidgetIds.isNotEmpty()) {
                    val views = android.widget.RemoteViews(context.packageName, R.layout.very_simple_layout)
                    
                    val durationMs = player.duration.coerceAtLeast(1L)
                    val timeDisplay = "$currentPos / $duration"

                    views.setTextViewText(R.id.simple_text_title, episodeTitle)
                    views.setTextViewText(R.id.simple_text_time, timeDisplay)
                    
                    // Generate dotted progress bitmap
                    val progressFrac = if (durationMs > 0) (player.currentPosition.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                    val dottedBitmap = createDottedProgressBitmap(context, progressFrac)
                    views.setImageViewBitmap(R.id.simple_progress_image, dottedBitmap)
                    
                    val playPauseIcon = if (isPlaying) R.drawable.widget_dot_pause else R.drawable.widget_dot_play
                    views.setImageViewResource(R.id.simple_btn_play, playPauseIcon)
                    
                    // Setup PendingIntents
                    val openAppIntent = android.content.Intent(context, com.example.nothingpodcast.MainActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("OPEN_PLAYER", true)
                    }
                    val openAppPending = android.app.PendingIntent.getActivity(context, 20, openAppIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(R.id.widget_header_container, openAppPending)
                    views.setOnClickPendingIntent(R.id.simple_progress_image, openAppPending)

                    val playPauseIntent = android.content.Intent(context, com.example.nothingpodcast.service.PodcastPlaybackService::class.java).apply { action = com.example.nothingpodcast.service.PodcastPlaybackService.COMMAND_PLAY_PAUSE }
                    views.setOnClickPendingIntent(R.id.simple_btn_play_container, android.app.PendingIntent.getService(context, 11, playPauseIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
                    
                    val skipBackIntent = android.content.Intent(context, com.example.nothingpodcast.service.PodcastPlaybackService::class.java).apply { action = com.example.nothingpodcast.service.PodcastPlaybackService.COMMAND_SKIP_BACKWARD }
                    views.setOnClickPendingIntent(R.id.simple_btn_back, android.app.PendingIntent.getService(context, 10, skipBackIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
                    
                    val skipForwardIntent = android.content.Intent(context, com.example.nothingpodcast.service.PodcastPlaybackService::class.java).apply { action = com.example.nothingpodcast.service.PodcastPlaybackService.COMMAND_SKIP_FORWARD }
                    views.setOnClickPendingIntent(R.id.simple_btn_forward, android.app.PendingIntent.getService(context, 12, skipForwardIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))

                    appWidgetManager.updateAppWidget(appWidgetIds, views)
                }
            } catch (e: Exception) {
                com.example.nothingpodcast.util.AppLogger.log(this@PodcastPlaybackService, "ERROR", "Service: Failed to update widget: ${e.message}")
            }
        }
    }

    private fun createDottedProgressBitmap(context: android.content.Context, progress: Float): android.graphics.Bitmap {
        val width = 800
        val height = 64 // Increased height for thumb clearance
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        
        val dotCount = 55
        val dotRadius = 4f
        val horizontalPadding = 8f
        val drawWidth = width - (horizontalPadding * 2)
        val centerY = height / 2f
        val dotSpacing = drawWidth / (dotCount - 1)
        
        // Colors from Nothing Palette
        val redColor = 0xFFFF2A2A.toInt()
        val greyColor = 0xFF1F1F1F.toInt()
        val whiteColor = 0xFFFFFFFF.toInt()
        val blackColor = 0xFF000000.toInt()

        for (i in 0 until dotCount) {
            val dotX = horizontalPadding + (i * dotSpacing)
            val dotProgress = i.toFloat() / (dotCount - 1)
            
            paint.color = if (dotProgress <= progress) redColor else greyColor
            canvas.drawCircle(dotX, centerY, dotRadius, paint)
        }
        
        // Draw Thumb (White circle with black outline)
        val thumbX = horizontalPadding + (progress * drawWidth)
        paint.color = blackColor
        canvas.drawCircle(thumbX, centerY, 15f, paint)
        paint.color = whiteColor
        canvas.drawCircle(thumbX, centerY, 11f, paint)
        
        return bitmap
    }

    private fun formatDuration(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%02d:%02d".format(mins, secs)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        com.example.nothingpodcast.util.AppLogger.log(this, "INFO", "Service: onTaskRemoved - saving position")
        savePositionNow()
        super.onTaskRemoved(rootIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                COMMAND_SKIP_FORWARD -> {
                    val newPos = (player.currentPosition + 30_000L).coerceAtMost(player.duration)
                    player.seekTo(newPos)
                }
                COMMAND_SKIP_BACKWARD -> {
                    val newPos = (player.currentPosition - 15_000L).coerceAtLeast(0L)
                    player.seekTo(newPos)
                }
                COMMAND_PLAY_PAUSE -> {
                    if (player.isPlaying) player.pause() else player.play()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        // Save position one last time before destroying
        val id = currentEpisodeId
        if (id != null && ::player.isInitialized) {
            val posMs = try { player.currentPosition } catch (e: Exception) { 0L }
            if (posMs > 0) {
                // Use GlobalScope for the final save to ensure it finishes after service is destroyed
                GlobalScope.launch(Dispatchers.IO) {
                    episodeRepository.savePlaybackPosition(id, posMs / 1000L)
                }
            }
        }
        
        positionSaveJob?.cancel()
        sleepTimerJob?.cancel()
        mediaLibrarySession.release()
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Sleep timer ───────────────────────────────────────────────────────

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = serviceScope.launch {
            delay(minutes * 60_000L)
            player.pause()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }

    // ── Position tracking ─────────────────────────────────────────────────

    private fun startPositionTracking() {
        positionSaveJob = serviceScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(10_000L) // Save every 10 seconds
                savePositionNow()
                updateWidget()
            }
        }
    }

    private fun savePositionNow() {
        val id = currentEpisodeId ?: return
        savePositionInternal(id)
    }

    private fun savePositionInternal(episodeId: String) {
        // IMPORTANT: We must be on the Main thread to access 'player'
        serviceScope.launch(Dispatchers.Main) {
            if (!::player.isInitialized) return@launch
            
            val state = player.playbackState
            // Avoid saving during buffering as position might be 0 incorrectly
            if (state == Player.STATE_BUFFERING) return@launch

            val posMs = try { player.currentPosition } catch (e: Exception) { -1L }
            val durMs = try { player.duration } catch (e: Exception) { -1L }

            if (posMs >= 0) { 
                withContext(Dispatchers.IO) {
                    try {
                        episodeRepository.savePlaybackPosition(episodeId, posMs / 1000L)
                        // If near the end (99%), mark as played and reset to 0 for next time
                        if (durMs > 0 && durMs != C.TIME_UNSET && posMs >= durMs * 0.99) {
                            episodeRepository.markPlayed(episodeId, 0L)
                        }
                    } catch (e: Exception) {
                        com.example.nothingpodcast.util.AppLogger.log(this@PodcastPlaybackService, "ERROR", "Service: Failed to save position: ${e.message}")
                    }
                }
            }
        }
    }

    // ── Media Item Mappers ───────────────────────────────────────────────

    private fun getRootItem(): MediaItem = MediaItem.Builder()
        .setMediaId(ROOT_ID)
        .setMediaMetadata(MediaMetadata.Builder()
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build())
        .build()

    private fun getSubscriptionsCategory(): MediaItem = MediaItem.Builder()
        .setMediaId(ID_SUBSCRIPTIONS)
        .setMediaMetadata(MediaMetadata.Builder()
            .setTitle("Iscrizioni")
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build())
        .build()

    private fun getDownloadsCategory(): MediaItem = MediaItem.Builder()
        .setMediaId(ID_DOWNLOADS)
        .setMediaMetadata(MediaMetadata.Builder()
            .setTitle("Download")
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build())
        .build()

    private fun Podcast.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(author)
            .setArtworkUri(android.net.Uri.parse(imageUrl))
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build())
        .build()

    private fun Episode.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(podcastTitle)
            .setArtworkUri(android.net.Uri.parse(imageUrl))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build())
        .setUri(android.net.Uri.parse(downloadPath ?: audioUrl))
        .build()

    companion object {
        private const val ROOT_ID = "[ROOT]"
        private const val ID_SUBSCRIPTIONS = "[SUBSCRIPTIONS]"
        private const val ID_DOWNLOADS = "[DOWNLOADS]"

        const val EXTRA_EPISODE_ID = "episode_id"
        const val EXTRA_EPISODE_TITLE = "episode_title"
        const val EXTRA_PODCAST_TITLE = "podcast_title"
        const val EXTRA_ARTWORK_URI = "artwork_uri"
        const val EXTRA_AUDIO_URI = "audio_uri"
        const val EXTRA_START_POSITION = "start_position"
        
        const val COMMAND_SKIP_FORWARD = "com.example.nothingpodcast.SKIP_FORWARD"
        const val COMMAND_SKIP_BACKWARD = "com.example.nothingpodcast.SKIP_BACKWARD"
        const val COMMAND_PLAY_PAUSE = "com.example.nothingpodcast.PLAY_PAUSE"
    }
}
