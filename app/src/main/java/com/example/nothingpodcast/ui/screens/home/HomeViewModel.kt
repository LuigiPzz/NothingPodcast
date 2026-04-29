package com.example.nothingpodcast.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nothingpodcast.data.local.datastore.UserPreferencesDataStore
import com.example.nothingpodcast.data.repository.PodcastRepository
import com.example.nothingpodcast.domain.model.Podcast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── View mode enum ────────────────────────────────────────────────────────────

enum class PodcastViewMode(val columns: Int, val label: String) {
    LIST(1,  "LIST"),
    GRID2(2, "GRID  2×"),
    GRID3(3, "GRID  3×"),
    GRID4(4, "GRID  4×"),
    GRID5(5, "GRID  5×")
}

// ── UI state ──────────────────────────────────────────────────────────────────

data class HomeUiState(
    val subscribedPodcasts: List<Podcast>  = emptyList(),
    val searchResults:      List<Podcast>  = emptyList(),
    val searchQuery:        String         = "",
    val isSearching:        Boolean        = false,
    val isRefreshing:       Boolean        = false,
    val error:              String?        = null,
    val showSearchSheet:    Boolean        = false,
    val viewMode:           PodcastViewMode = PodcastViewMode.GRID3,
    val showGridLabels:     Boolean        = false,
    val isEditMode:         Boolean        = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val preferences: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        // Observe subscribed podcasts
        viewModelScope.launch {
            podcastRepository.getSubscribedPodcasts()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { podcasts ->
                    // Only update from DB if not currently reordering
                    if (!_uiState.value.isEditMode) {
                        _uiState.update { it.copy(subscribedPodcasts = podcasts) }
                    }
                }
        }

        // Observe persisted view mode
        viewModelScope.launch {
            preferences.podcastViewMode.collect { name ->
                val mode = runCatching { PodcastViewMode.valueOf(name) }.getOrDefault(PodcastViewMode.GRID3)
                _uiState.update { it.copy(viewMode = mode) }
            }
        }

        // Observe persisted grid labels toggle
        viewModelScope.launch {
            preferences.gridShowLabels.collect { show ->
                _uiState.update { it.copy(showGridLabels = show) }
            }
        }

        // Debounced search
        viewModelScope.launch {
            _searchQuery
                .debounce(400L)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
                    } else {
                        performSearch(query)
                    }
                }
        }
    }

    // ── Search ─────────────────────────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    fun showSearchSheet()  = _uiState.update { it.copy(showSearchSheet = true) }
    fun hideSearchSheet()  {
        clearSearch()
        _uiState.update { it.copy(showSearchSheet = false) }
    }

    // ── Subscriptions ──────────────────────────────────────────────────────

    fun subscribeToPodcast(podcast: Podcast) {
        viewModelScope.launch {
            runCatching { podcastRepository.subscribe(podcast) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun unsubscribeFromPodcast(podcastId: String) {
        viewModelScope.launch {
            runCatching { podcastRepository.unsubscribe(podcastId) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            _uiState.value.subscribedPodcasts.forEach { podcast ->
                runCatching { podcastRepository.refreshEpisodes(podcast) }
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    // ── View mode ──────────────────────────────────────────────────────────

    fun setViewMode(mode: PodcastViewMode) {
        viewModelScope.launch {
            preferences.setPodcastViewMode(mode.name)
            // State updates via the Flow observer above
        }
    }

    fun toggleGridLabels() {
        viewModelScope.launch {
            preferences.setGridShowLabels(!_uiState.value.showGridLabels)
        }
    }

    // ── Misc ───────────────────────────────────────────────────────────────

    fun dismissError() = _uiState.update { it.copy(error = null) }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            runCatching { podcastRepository.searchPodcasts(query) }
                .onSuccess { results ->
                    _uiState.update { it.copy(searchResults = results, isSearching = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isSearching = false) }
                }
        }
    }

    // ── Reordering ────────────────────────────────────────────────────────

    fun setEditMode(enabled: Boolean) {
        _uiState.update { it.copy(isEditMode = enabled) }
    }

    fun movePodcast(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val list = state.subscribedPodcasts.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
            }
            state.copy(subscribedPodcasts = list)
        }
    }

    fun saveOrder() {
        viewModelScope.launch {
            podcastRepository.updatePodcastsOrder(_uiState.value.subscribedPodcasts)
            _uiState.update { it.copy(isEditMode = false) }
        }
    }
}
