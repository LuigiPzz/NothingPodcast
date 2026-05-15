package com.example.nothingpodcast.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.nothingpodcast.data.repository.EpisodeRepository
import com.example.nothingpodcast.data.repository.PodcastRepository
import com.example.nothingpodcast.domain.model.Episode
import com.example.nothingpodcast.domain.model.Podcast
import com.example.nothingpodcast.worker.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EpisodeFilter { ALL, PLAYED, UNPLAYED }
enum class EpisodeSort { DATE_DESC, DATE_ASC }

data class DetailUiState(
    val podcast: Podcast? = null,
    val episodes: List<Episode> = emptyList(),
    val downloadProgress: Map<String, Int> = emptyMap(), // Episode ID -> Progress (0-100)
    val filterType: EpisodeFilter = EpisodeFilter.ALL,
    val sortType: EpisodeSort = EpisodeSort.DATE_DESC,
    val showFilterSheet: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
) {
    val filteredEpisodes: List<Episode>
        get() = episodes
            .filter { ep ->
                when (filterType) {
                    EpisodeFilter.ALL -> true
                    EpisodeFilter.PLAYED -> ep.isPlayed
                    EpisodeFilter.UNPLAYED -> !ep.isPlayed
                }
            }
            .sortedWith { a, b ->
                when (sortType) {
                    EpisodeSort.DATE_DESC -> b.publishDate.compareTo(a.publishDate)
                    EpisodeSort.DATE_ASC -> a.publishDate.compareTo(b.publishDate)
                }
            }
}

@HiltViewModel
class PodcastDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository,
    private val workManager: WorkManager,
    private val preferences: com.example.nothingpodcast.data.local.datastore.UserPreferencesDataStore
) : ViewModel() {

    private val podcastId: String = checkNotNull(savedStateHandle["podcastId"])

    private val _podcast = MutableStateFlow<Podcast?>(null)
    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _showFilterSheet = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)
    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DetailUiState> = combine(
        _podcast,
        _episodes,
        _downloadProgress,
        preferences.episodeFilter,
        preferences.episodeSort,
        _showFilterSheet,
        _isLoading,
        _isRefreshing,
        _error
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        DetailUiState(
            podcast = args[0] as Podcast?,
            episodes = args[1] as List<Episode>,
            downloadProgress = args[2] as Map<String, Int>,
            filterType = runCatching { EpisodeFilter.valueOf(args[3] as String) }.getOrDefault(EpisodeFilter.UNPLAYED),
            sortType = runCatching { EpisodeSort.valueOf(args[4] as String) }.getOrDefault(EpisodeSort.DATE_DESC),
            showFilterSheet = args[5] as Boolean,
            isLoading = args[6] as Boolean,
            isRefreshing = args[7] as Boolean,
            error = args[8] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    init {
        loadData()
        observeDownloadProgress()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load podcast info
            val podcast = podcastRepository.getPodcastById(podcastId)
            _podcast.value = podcast
            _isLoading.value = false

            // Observe episodes reactively
            episodeRepository.getEpisodesForPodcast(podcastId)
                .catch { e -> _error.value = e.message }
                .collect { episodes ->
                    _episodes.value = episodes
                }
        }
    }

    private fun observeDownloadProgress() {
        workManager.getWorkInfosByTagFlow(DownloadWorker.TAG_DOWNLOADS)
            .onEach { workInfos ->
                val progressMap = workInfos
                    .filter { !it.state.isFinished }
                    .mapNotNull { info ->
                        val idTag = info.tags.find { it.startsWith("download_") && it != DownloadWorker.TAG_DOWNLOADS }
                        val episodeId = idTag?.removePrefix("download_") ?: info.progress.getString(DownloadWorker.KEY_EPISODE_ID)
                        val progress = info.progress.getInt(DownloadWorker.KEY_PROGRESS, -1)
                        if (episodeId != null) {
                            episodeId to if (progress == -1) 0 else progress
                        } else null
                    }.toMap()
                _downloadProgress.value = progressMap
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            val podcast = _podcast.value ?: return@launch
            _isRefreshing.value = true
            runCatching { podcastRepository.refreshEpisodes(podcast) }
                .onFailure { e -> _error.value = e.message }
            _isRefreshing.value = false
        }
    }

    fun subscribe() {
        viewModelScope.launch {
            val podcast = _podcast.value ?: return@launch
            runCatching { podcastRepository.subscribe(podcast) }
                .onFailure { e -> _error.value = e.message }
        }
    }

    fun unsubscribe() {
        viewModelScope.launch {
            runCatching { podcastRepository.unsubscribe(podcastId) }
                .onFailure { e -> _error.value = e.message }
        }
    }

    fun markEpisodePlayed(episodeId: String) {
        viewModelScope.launch { episodeRepository.markPlayed(episodeId) }
    }

    fun markEpisodesPlayed(episodeIds: Collection<String>) {
        viewModelScope.launch { 
            episodeIds.forEach { episodeRepository.markPlayed(it) }
        }
    }

    fun markEpisodeUnplayed(episodeId: String) {
        viewModelScope.launch { episodeRepository.markUnplayed(episodeId) }
    }

    fun markEpisodesUnplayed(episodeIds: Collection<String>) {
        viewModelScope.launch {
            episodeIds.forEach { episodeRepository.markUnplayed(it) }
        }
    }

    fun downloadEpisode(episode: Episode) {
        val fileName = "${episode.id.hashCode()}.mp3"
        val inputData = workDataOf(
            DownloadWorker.KEY_EPISODE_ID to episode.id,
            DownloadWorker.KEY_AUDIO_URL  to episode.audioUrl,
            DownloadWorker.KEY_FILE_NAME  to fileName
        )
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .addTag(DownloadWorker.TAG_DOWNLOADS)
            .addTag(DownloadWorker.tag(episode.id))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniqueWork(
            DownloadWorker.tag(episode.id),
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun deleteDownload(episode: Episode) {
        viewModelScope.launch {
            episode.downloadPath?.let { runCatching { java.io.File(it).delete() } }
            episodeRepository.markNotDownloaded(episode.id)
        }
    }

    fun setShowFilterSheet(show: Boolean) {
        _showFilterSheet.value = show
    }

    fun setFilter(filter: EpisodeFilter) {
        viewModelScope.launch { preferences.setEpisodeFilter(filter.name) }
    }

    fun setSort(sort: EpisodeSort) {
        viewModelScope.launch { preferences.setEpisodeSort(sort.name) }
    }

    fun dismissError() {
        _error.value = null
    }
}
