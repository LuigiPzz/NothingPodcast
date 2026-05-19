package com.example.nothingpodcast.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nothingpodcast.data.local.datastore.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.nothingpodcast.data.repository.PodcastRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject
import com.example.nothingpodcast.util.NothingGlyphManager

data class SettingsUiState(
    val notificationsEnabled: Boolean = false,
    val skipForwardSeconds: Int = 30,
    val skipBackwardSeconds: Int = 15,
    val playbackSpeed: Float = 1.0f,
    val loggingEnabled: Boolean = false,
    val autoUpdateEnabled: Boolean = true,
    val updateIntervalHours: Int = 3,
    val updateWifiOnly: Boolean = true,
    val isImporting: Boolean = false,
    val importProgress: Int = 0,
    val importTotal: Int = 0,
    val importError: String? = null,
    val isGlyphEnabled: Boolean = true,
    val autoDownloadEnabled: Boolean = false,
    val autoDownloadWifiOnly: Boolean = true,
    val downloadCompletedNotificationEnabled: Boolean = true,
    val localPodcastCount: Int = 0,
    val totalEpisodeCount: Int = 0,
    val playedEpisodeCount: Int = 0,
    val cloudSyncSummary: com.example.nothingpodcast.data.repository.SyncRepository.SyncSummary? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val podcastRepository:   PodcastRepository,
    private val syncRepository:      com.example.nothingpodcast.data.repository.SyncRepository,
    private val episodeRepository:   com.example.nothingpodcast.data.repository.EpisodeRepository,
    private val glyphManager:        com.example.nothingpodcast.util.NothingGlyphManager,
    val driveService:               com.example.nothingpodcast.data.remote.google.GoogleDriveService
) : ViewModel() {

    init {
        // Auto-refresh cloud summary on startup if logged in
        viewModelScope.launch {
            try {
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(com.example.nothingpodcast.NothingPodcastApplication.instance)
                if (account != null) {
                    refreshCloudSummary(account)
                }
            } catch (e: Exception) {
                com.example.nothingpodcast.util.AppLogger.e("Failed to check Google Sign-In status", e)
            }
        }
    }

    fun testGlyph() {
        glyphManager.pulseAction()
    }

    fun uploadToDrive(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(syncRepository.upload(account))
        }
    }

    fun downloadFromDrive(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val res = syncRepository.downloadAndApply(account)
            onResult(res)
            if (res.isSuccess) {
                refreshCloudSummary(account)
            }
        }
    }

    private val _cloudSyncSummary = MutableStateFlow<com.example.nothingpodcast.data.repository.SyncRepository.SyncSummary?>(null)

    fun refreshCloudSummary(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
            syncRepository.getRemoteSyncSummary(account).onSuccess {
                _cloudSyncSummary.value = it
            }
        }
    }

    private val _importStatus = MutableStateFlow(ImportStatus())
    private data class ImportStatus(
        val isImporting: Boolean = false,
        val progress: Int = 0,
        val total: Int = 0,
        val error: String? = null
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesDataStore.notificationsEnabled,
        preferencesDataStore.skipForwardSeconds,
        preferencesDataStore.skipBackwardSeconds,
        preferencesDataStore.playbackSpeed,
        preferencesDataStore.loggingEnabled,
        preferencesDataStore.autoUpdateEnabled,
        preferencesDataStore.updateIntervalHours,
        preferencesDataStore.updateWifiOnly,
        preferencesDataStore.glyphEnabled,
        preferencesDataStore.autoDownloadEnabled,
        preferencesDataStore.autoDownloadWifiOnly,
        preferencesDataStore.downloadCompletedNotificationEnabled,
        _importStatus,
        podcastRepository.getSubscribedPodcasts(),
        _cloudSyncSummary,
        episodeRepository.getTotalEpisodeCount(),
        episodeRepository.getPlayedEpisodeCount()
    ) { args ->
        val importStatus = args[12] as ImportStatus
        val localPodcasts = args[13] as List<*>
        SettingsUiState(
            notificationsEnabled = args[0] as Boolean,
            skipForwardSeconds = args[1] as Int,
            skipBackwardSeconds = args[2] as Int,
            playbackSpeed = args[3] as Float,
            loggingEnabled = args[4] as Boolean,
            autoUpdateEnabled = args[5] as Boolean,
            updateIntervalHours = args[6] as Int,
            updateWifiOnly = args[7] as Boolean,
            isGlyphEnabled = args[8] as Boolean,
            autoDownloadEnabled = args[9] as Boolean,
            autoDownloadWifiOnly = args[10] as Boolean,
            downloadCompletedNotificationEnabled = args[11] as Boolean,
            isImporting = importStatus.isImporting,
            importProgress = importStatus.progress,
            importTotal = importStatus.total,
            importError = importStatus.error,
            localPodcastCount = localPodcasts.size,
            cloudSyncSummary = args[14] as com.example.nothingpodcast.data.repository.SyncRepository.SyncSummary?,
            totalEpisodeCount = args[15] as Int,
            playedEpisodeCount = args[16] as Int
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setNotificationsEnabled(enabled) }
    }

    fun setSkipForward(seconds: Int) {
        viewModelScope.launch { preferencesDataStore.setSkipForwardSeconds(seconds) }
    }

    fun setSkipBackward(seconds: Int) {
        viewModelScope.launch { preferencesDataStore.setSkipBackwardSeconds(seconds) }
    }

    fun setLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setLoggingEnabled(enabled) }
        com.example.nothingpodcast.util.AppLogger.isEnabled = enabled
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setAutoUpdateEnabled(enabled) }
    }

    fun setUpdateInterval(hours: Int) {
        viewModelScope.launch { preferencesDataStore.setUpdateIntervalHours(hours) }
    }

    fun setUpdateWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch { preferencesDataStore.setUpdateWifiOnly(wifiOnly) }
    }

    fun setGlyphEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setGlyphEnabled(enabled) }
    }

    fun setAutoDownloadEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setAutoDownloadEnabled(enabled) }
    }

    fun setAutoDownloadWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch { preferencesDataStore.setAutoDownloadWifiOnly(wifiOnly) }
    }

    fun setDownloadCompletedNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setDownloadCompletedNotificationEnabled(enabled) }
    }

    suspend fun getOpmlData(): String {
        val podcasts = podcastRepository.getSubscribedPodcasts().first()
        return com.example.nothingpodcast.util.OpmlManager.generateOpml(podcasts)
    }

    fun clearImportError() {
        _importStatus.update { it.copy(error = null) }
    }

    fun importOpml(inputStream: InputStream, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            _importStatus.value = ImportStatus(isImporting = true)
            val urls = try {
                com.example.nothingpodcast.util.OpmlManager.parseOpml(inputStream)
            } catch (e: Exception) {
                _importStatus.value = ImportStatus(isImporting = false, error = "Errore nel formato del file OPML")
                onResult(0)
                return@launch
            }

            if (urls.isEmpty()) {
                _importStatus.value = ImportStatus(isImporting = false, error = "Nessun feed trovato nel file OPML")
                onResult(0)
                return@launch
            }

            _importStatus.value = ImportStatus(isImporting = true, total = urls.size)
            
            var count = 0
            var errors = 0
            urls.forEachIndexed { index, url ->
                _importStatus.update { it.copy(progress = index + 1) }
                try {
                    podcastRepository.subscribeByUrl(url)
                    count++
                } catch (e: Exception) {
                    errors++
                }
            }
            
            _importStatus.value = ImportStatus(
                isImporting = false, 
                progress = count, 
                total = urls.size,
                error = if (errors > 0) "Importazione completata con $errors errori" else null
            )
            onResult(count)
        }
    }
}
