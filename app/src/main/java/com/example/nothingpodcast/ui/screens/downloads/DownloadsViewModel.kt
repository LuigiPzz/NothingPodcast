package com.example.nothingpodcast.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nothingpodcast.data.repository.EpisodeRepository
import com.example.nothingpodcast.domain.model.Episode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadsUiState(
    val episodes: List<Episode> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val episodeRepository: EpisodeRepository
) : ViewModel() {

    val uiState: StateFlow<DownloadsUiState> =
        episodeRepository.getDownloadedEpisodes()
            .map { DownloadsUiState(episodes = it, isLoading = false) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

    fun deleteDownload(episode: Episode) {
        viewModelScope.launch {
            deleteEpisodeFileAndRecord(episode)
        }
    }

    fun deleteEpisodes(episodes: List<Episode>) {
        viewModelScope.launch {
            episodes.forEach { deleteEpisodeFileAndRecord(it) }
        }
    }

    private suspend fun deleteEpisodeFileAndRecord(episode: Episode) {
        episode.downloadPath?.let {
            runCatching { java.io.File(it).delete() }
        }
        episodeRepository.markNotDownloaded(episode.id)
    }
}
