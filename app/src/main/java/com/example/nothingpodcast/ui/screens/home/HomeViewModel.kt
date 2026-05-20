package com.example.nothingpodcast.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nothingpodcast.data.local.datastore.UserPreferencesDataStore
import com.example.nothingpodcast.data.repository.PodcastRepository
import com.example.nothingpodcast.domain.model.Podcast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

enum class PodcastSortOrder(val label: String) {
    CUSTOM("Manuale"),
    A_Z("A → Z"),
    Z_A("Z → A"),
    RECENT("Recente")
}

// ── UI state ──────────────────────────────────────────────────────────────────

data class HomeUiState(
    val subscribedPodcasts: List<Podcast>  = emptyList(),
    val suggestedPodcasts:  List<Podcast>  = emptyList(),
    val recommendedPodcasts: List<Podcast> = emptyList(),
    val searchResults:      List<Podcast>  = emptyList(),
    val searchQuery:        String         = "",
    val isSearching:        Boolean        = false,
    val isRefreshing:       Boolean        = false,
    val error:              String?        = null,
    val showSearchSheet:    Boolean        = false,
    val viewMode:           PodcastViewMode  = PodcastViewMode.GRID3,
    val sortOrder:          PodcastSortOrder = PodcastSortOrder.CUSTOM,
    val showGridLabels:     Boolean        = false,
    val isEditMode:         Boolean        = false,
    val isLoadingRecommendations: Boolean  = false
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
        // Observe subscribed podcasts + sort order together
        viewModelScope.launch {
            combine(
                podcastRepository.getSubscribedPodcasts(),
                preferences.podcastSortOrder
            ) { podcasts, sortName ->
                val sort = runCatching { PodcastSortOrder.valueOf(sortName) }.getOrDefault(PodcastSortOrder.CUSTOM)
                val sorted = when (sort) {
                    PodcastSortOrder.CUSTOM -> podcasts
                    PodcastSortOrder.A_Z    -> podcasts.sortedBy { it.title.lowercase() }
                    PodcastSortOrder.Z_A    -> podcasts.sortedByDescending { it.title.lowercase() }
                    PodcastSortOrder.RECENT -> podcasts.sortedByDescending { it.lastUpdated }
                }
                Pair(sorted, sort)
            }
            .catch { e -> _uiState.update { it.copy(error = e.message) } }
            .collect { (podcasts, sort) ->
                if (!_uiState.value.isEditMode) {
                    _uiState.update { it.copy(subscribedPodcasts = podcasts, sortOrder = sort) }
                    if (podcasts.isEmpty() && _uiState.value.suggestedPodcasts.isEmpty()) {
                        loadSuggestions()
                    }
                    loadRecommendations(podcasts)
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
            // Refresh tutti i podcast in parallelo per ridurre i tempi di attesa
            _uiState.value.subscribedPodcasts
                .map { podcast -> async { runCatching { podcastRepository.refreshEpisodes(podcast) } } }
                .awaitAll()
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

    fun setSortOrder(order: PodcastSortOrder) {
        viewModelScope.launch {
            preferences.setPodcastSortOrder(order.name)
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

    private fun loadSuggestions() {
        viewModelScope.launch {
            // Curated list for the "Nothing" community
            val suggestions = listOf(
                "Digitalia",
                "Waveform: The MKBHD Podcast",
                "The Vergecast",
                "Nothing Podcast",
                "The Futur with Chris Do",
                "TED Talks Daily"
            )
            
            val results = suggestions.mapNotNull { query ->
                runCatching { podcastRepository.searchPodcasts(query).firstOrNull() }.getOrNull()
            }.distinctBy { it.id }
            
            _uiState.update { it.copy(suggestedPodcasts = results) }
        }
    }

    private fun loadRecommendations(subscribed: List<Podcast>) {
        if (subscribed.isEmpty()) {
            _uiState.update { it.copy(recommendedPodcasts = emptyList()) }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRecommendations = true) }
            val recommendedList = mutableListOf<Podcast>()
            
            // Limit to at most 3 subscribed podcasts to search to avoid excessive API requests
            val sourcePodcasts = subscribed.shuffled().take(3)
            val subscribedIds = subscribed.map { it.id }.toSet()
            val subscribedUrls = subscribed.map { it.feedUrl.lowercase().trim() }.toSet()
            
            for (podcast in sourcePodcasts) {
                // Try searching by author first
                val cleanAuthor = podcast.author
                    .split(",", " and ", "&", " - ", " -")
                    .firstOrNull { it.isNotBlank() }
                    ?.trim()
                
                if (!cleanAuthor.isNullOrBlank() && cleanAuthor.length > 2) {
                    runCatching { podcastRepository.searchPodcasts(cleanAuthor) }
                        .onSuccess { results ->
                            val filtered = results.filter { 
                                it.id !in subscribedIds && 
                                it.feedUrl.lowercase().trim() !in subscribedUrls &&
                                it.id !in recommendedList.map { p -> p.id } &&
                                it.feedUrl.lowercase().trim() !in recommendedList.map { p -> p.feedUrl.lowercase().trim() }
                            }
                            recommendedList.addAll(filtered.take(3))
                        }
                }
                
                // If we don't have enough recommendations yet, try with title keywords
                if (recommendedList.size < 6) {
                    val cleanTitle = podcast.title
                        .split(" ", ":", "-", "|")
                        .filter { it.length > 3 && it.lowercase() !in listOf("podcast", "show", "with", "radio", "network", "news") }
                        .randomOrNull()
                    
                    if (!cleanTitle.isNullOrBlank()) {
                        runCatching { podcastRepository.searchPodcasts(cleanTitle) }
                            .onSuccess { results ->
                                val filtered = results.filter { 
                                    it.id !in subscribedIds && 
                                    it.feedUrl.lowercase().trim() !in subscribedUrls &&
                                    it.id !in recommendedList.map { p -> p.id } &&
                                    it.feedUrl.lowercase().trim() !in recommendedList.map { p -> p.feedUrl.lowercase().trim() }
                                }
                                recommendedList.addAll(filtered.take(3))
                            }
                    }
                }
            }
            
            // If we still have few or no recommendations, fetch some generic ones based on generic terms
            if (recommendedList.size < 3) {
                val fallbacks = listOf("Tech", "Scienza", "Notizie", "Storie", "Cultura")
                val randomTerm = fallbacks.random()
                runCatching { podcastRepository.searchPodcasts(randomTerm) }
                    .onSuccess { results ->
                        val filtered = results.filter { 
                            it.id !in subscribedIds && 
                            it.feedUrl.lowercase().trim() !in subscribedUrls &&
                            it.id !in recommendedList.map { p -> p.id } &&
                            it.feedUrl.lowercase().trim() !in recommendedList.map { p -> p.feedUrl.lowercase().trim() }
                        }
                        recommendedList.addAll(filtered.take(5))
                    }
            }
            
            _uiState.update { 
                it.copy(
                    recommendedPodcasts = recommendedList.distinctBy { p -> p.id }.take(6),
                    isLoadingRecommendations = false
                ) 
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
