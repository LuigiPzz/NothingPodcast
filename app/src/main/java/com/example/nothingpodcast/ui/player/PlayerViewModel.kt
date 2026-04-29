package com.example.nothingpodcast.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.nothingpodcast.data.local.datastore.UserPreferencesDataStore
import com.example.nothingpodcast.data.repository.EpisodeRepository
import com.example.nothingpodcast.util.NothingGlyphManager
import com.example.nothingpodcast.util.AppLogger
import com.example.nothingpodcast.domain.model.Episode
import com.example.nothingpodcast.service.PodcastPlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val currentEpisode: Episode? = null,
    val currentPodcast: com.example.nothingpodcast.domain.model.Podcast? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isLoading: Boolean = false,
    val sleepTimerMinutesLeft: Int = 0,
    val transcriptContent: String? = null,
    val isTranscriptLoading: Boolean = false,
    val isGlyphEnabled: Boolean = true
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val episodeRepository: EpisodeRepository,
    private val podcastRepository: com.example.nothingpodcast.data.repository.PodcastRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val okHttpClient: okhttp3.OkHttpClient,
    private val glyphManager: com.example.nothingpodcast.util.NothingGlyphManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val skipForwardSeconds = preferencesDataStore.skipForwardSeconds
        .stateIn(viewModelScope, SharingStarted.Eagerly, 30)

    val skipBackwardSeconds = preferencesDataStore.skipBackwardSeconds
        .stateIn(viewModelScope, SharingStarted.Eagerly, 15)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    init {
        connectToService()
        observeGlyphSetting()
    }

    private fun observeGlyphSetting() {
        viewModelScope.launch {
            preferencesDataStore.glyphEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isGlyphEnabled = enabled)
            }
        }
    }

    private fun connectToService() {
        com.example.nothingpodcast.util.AppLogger.log(context, "INFO", "Connecting to PodcastPlaybackService...")
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PodcastPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                controller?.addListener(playerListener)
                com.example.nothingpodcast.util.AppLogger.log(context, "INFO", "MediaController connected.")
                startPositionPolling()
            } catch (e: Exception) {
                com.example.nothingpodcast.util.AppLogger.log(context, "ERROR", "Failed to connect to MediaController: ${e.message}")
            }
        }, MoreExecutors.directExecutor())
    }

    // ── Playback commands ─────────────────────────────────────────────────

    fun playEpisode(episode: Episode) {
        com.example.nothingpodcast.util.AppLogger.log(context, "INFO", "Playing episode: ${episode.title}")
        
        viewModelScope.launch {
            // Ensure controller is connected
            if (controller == null) {
                connectToService()
                // Wait up to 2 seconds for connection
                var retry = 0
                while (controller == null && retry < 10) {
                    delay(200)
                    retry++
                }
            }

            val currentController = controller
            if (currentController == null) {
                com.example.nothingpodcast.util.AppLogger.log(context, "ERROR", "Controller still NULL after waiting.")
                return@launch
            }

            // Fetch fresh data from DB to get latest playbackPosition
            val freshEpisode = episodeRepository.getEpisodeById(episode.id) ?: episode
            
            val uri = if (freshEpisode.isDownloaded && freshEpisode.downloadPath != null)
                android.net.Uri.fromFile(java.io.File(freshEpisode.downloadPath))
            else
                android.net.Uri.parse(freshEpisode.audioUrl)

            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMediaId(freshEpisode.id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(freshEpisode.title)
                        .setArtist(freshEpisode.podcastTitle)
                        .build()
                )
                .build()

            try {
                com.example.nothingpodcast.util.AppLogger.log(context, "INFO", "Executing setMediaItem for ID: ${freshEpisode.id} at position: ${freshEpisode.playbackPosition}s")
                
                // Use a single transaction to set item and seek if possible
                currentController.setMediaItem(mediaItem, freshEpisode.playbackPosition * 1000L)
                currentController.prepare()
                currentController.play()
                if (_uiState.value.isGlyphEnabled) glyphManager.pulseAction()
                
                AppLogger.log(context, "INFO", "Play command sent successfully")
            } catch (e: Exception) {
                AppLogger.log(context, "ERROR", "Crash during playEpisode: ${e.message}")
            }

            val podcast = podcastRepository.getPodcastById(freshEpisode.podcastId)
            _uiState.value = _uiState.value.copy(
                currentEpisode = freshEpisode,
                currentPodcast = podcast,
                isLoading = true
            )
        }
    }

    fun togglePlayPause() {
        controller?.let {
            if (_uiState.value.isGlyphEnabled) glyphManager.pulseAction()
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun skipForward() {
        controller?.let {
            val newPos = (it.currentPosition + skipForwardSeconds.value * 1000L)
                .coerceAtMost(it.duration.coerceAtLeast(0L))
            it.seekTo(newPos)
            android.util.Log.d("PlayerViewModel", "Skip forward triggered. Glyph enabled: ${_uiState.value.isGlyphEnabled}")
            if (_uiState.value.isGlyphEnabled) {
                val progress = (newPos * 100 / it.duration).toInt().coerceIn(0, 100)
                glyphManager.showProgressTemporarily(progress)
            }
        }
    }

    fun skipBackward() {
        controller?.let {
            val newPos = (it.currentPosition - skipBackwardSeconds.value * 1000L)
                .coerceAtLeast(0L)
            it.seekTo(newPos)
            if (_uiState.value.isGlyphEnabled) {
                val progress = (newPos * 100 / it.duration).toInt().coerceIn(0, 100)
                glyphManager.showProgressTemporarily(progress)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.let {
            it.seekTo(positionMs)
            if (_uiState.value.isGlyphEnabled) {
                val progress = (positionMs * 100 / it.duration).toInt().coerceIn(0, 100)
                glyphManager.showProgressTemporarily(progress)
            }
        }
    }

    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
        if (_uiState.value.isGlyphEnabled) glyphManager.pulseSpeedAction(speed)
        viewModelScope.launch { preferencesDataStore.setPlaybackSpeed(speed) }
    }

    fun setGlyphEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setGlyphEnabled(enabled)
        }
    }

    fun fetchTranscript() {
        val episode = _uiState.value.currentEpisode
        val url = episode?.transcriptUrl ?: return
        
        if (_uiState.value.transcriptContent != null) return // Already loaded

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isTranscriptLoading = true)
            try {
                val request = okhttp3.Request.Builder().url(url).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val rawBody = response.body?.string() ?: ""
                        val chapters = episode.chapters.sortedBy { it.startTime }
                        
                        // Parse SRT/VTT into segments
                        val segments = mutableListOf<Pair<Long, String>>()
                        val timestampRegex = Regex("(\\d{2}):(\\d{2}):(\\d{2})[.,](\\d{3})\\s*-->")
                        
                        val lines = rawBody.split("\n")
                        var currentStartTime = -1L
                        val currentText = StringBuilder()
                        
                        for (line in lines) {
                            val match = timestampRegex.find(line)
                            if (match != null) {
                                if (currentStartTime != -1L && currentText.isNotBlank()) {
                                    val text = currentText.toString().trim()
                                    // Skip if text is just noise/punctuation
                                    if (text.length > 1 || (text.isNotEmpty() && Character.isLetterOrDigit(text[0]))) {
                                        segments.add(currentStartTime to text)
                                    }
                                }
                                currentText.clear()
                                val h = match.groupValues[1].toLong()
                                val m = match.groupValues[2].toLong()
                                val s = match.groupValues[3].toLong()
                                currentStartTime = h * 3600 + m * 60 + s
                            } else if (currentStartTime != -1L && line.trim().toIntOrNull() == null && line.trim() != "WEBVTT" && line.isNotBlank()) {
                                val cleanedLine = line.replace(Regex("<[^>]*>"), "").trim()
                                if (cleanedLine.isNotEmpty() && cleanedLine != ".") {
                                    if (currentText.isNotEmpty()) currentText.append(" ")
                                    currentText.append(cleanedLine)
                                }
                            }
                        }
                        // Add last segment
                        if (currentStartTime != -1L && currentText.isNotBlank()) {
                            val text = currentText.toString().trim()
                            if (text.length > 1 || (text.isNotEmpty() && Character.isLetterOrDigit(text[0]))) {
                                segments.add(currentStartTime to text)
                            }
                        }

                        // Interleave chapters
                        val result = StringBuilder()
                        var chapterIndex = 0
                        
                        if (chapters.isNotEmpty() && chapters[0].startTime == 0L) {
                            result.append("[ ${chapters[0].title.uppercase()} ]\n\n")
                            chapterIndex = 1
                        }

                        for (segment in segments) {
                            while (chapterIndex < chapters.size && chapters[chapterIndex].startTime <= segment.first) {
                                val ch = chapters[chapterIndex]
                                result.append("\n\n[ ${ch.title.uppercase()} ]\n\n")
                                chapterIndex++
                            }
                            result.append(segment.second).append(" ")
                        }
                        
                        var cleanedText = result.toString().trim()
                        if (cleanedText.isBlank()) {
                            cleanedText = rawBody.replace(Regex("<[^>]*>"), " ")
                                .replace(Regex("\\n+"), " ")
                                .replace(Regex("[ \t]+"), " ")
                                .trim()
                        }
                        
                        // Final safety cleanup: remove any leading dots or symbols from the final text
                        // and also dots appearing immediately after a chapter marker
                        cleanedText = cleanedText
                            .replace(Regex("^[^\\p{L}\\d\\[]+"), "")
                            .replace(Regex("\\]\\n+\\s*\\.\\s*"), "]\n\n")
                            .trim()

                        _uiState.value = _uiState.value.copy(
                            transcriptContent = cleanedText,
                            isTranscriptLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isTranscriptLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isTranscriptLoading = false)
            }
        }
    }

    // ── Position polling ──────────────────────────────────────────────────

    private fun startPositionPolling() {
        viewModelScope.launch {
            while (isActive) {
                controller?.let { c ->
                    _uiState.value = _uiState.value.copy(
                        positionMs = c.currentPosition.coerceAtLeast(0L),
                        durationMs = c.duration.coerceAtLeast(0L),
                        isPlaying  = c.playWhenReady,
                        isLoading  = c.playbackState == Player.STATE_BUFFERING
                    )
                }
                delay(500L)
            }
        }
    }

    // ── Player listener ───────────────────────────────────────────────────

    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            _uiState.value = _uiState.value.copy(isPlaying = playWhenReady)
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.value = _uiState.value.copy(
                isLoading = playbackState == Player.STATE_BUFFERING
            )
            // Pulse on chapter end or specific transitions if needed
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (_uiState.value.isGlyphEnabled) glyphManager.pulseAction() // Pulse when changing episode
            val id = mediaItem?.mediaId
            if (id == null) {
                _uiState.value = _uiState.value.copy(currentEpisode = null)
                return
            }
            viewModelScope.launch {
                val episode = episodeRepository.getEpisodeById(id)
                val podcast = episode?.let { podcastRepository.getPodcastById(it.podcastId) }
                _uiState.value = _uiState.value.copy(
                    currentEpisode = episode,
                    currentPodcast = podcast
                )
            }
        }
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        glyphManager.closeSession()
        super.onCleared()
    }
}
