package com.example.nothingpodcast.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("nothing_podcast_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val NOTIFICATIONS_ENABLED   = booleanPreferencesKey("notifications_enabled")
        val SKIP_FORWARD_SECONDS    = intPreferencesKey("skip_forward_seconds")
        val SKIP_BACKWARD_SECONDS   = intPreferencesKey("skip_backward_seconds")
        val DOWNLOAD_PATH           = stringPreferencesKey("download_path")
        val SLEEP_TIMER_MINUTES     = intPreferencesKey("sleep_timer_minutes")
        val PLAYBACK_SPEED          = stringPreferencesKey("playback_speed")
        val PODCAST_VIEW_MODE       = stringPreferencesKey("podcast_view_mode")
        val GRID_SHOW_LABELS        = booleanPreferencesKey("grid_show_labels")
        val LOGGING_ENABLED         = booleanPreferencesKey("logging_enabled")
        val HAS_SEEN_ONBOARDING     = booleanPreferencesKey("has_seen_onboarding")
        val EPISODE_FILTER          = stringPreferencesKey("episode_filter")
        val EPISODE_SORT            = stringPreferencesKey("episode_sort")
        val AUTO_UPDATE_ENABLED     = booleanPreferencesKey("auto_update_enabled")
        val UPDATE_INTERVAL_HOURS   = intPreferencesKey("update_interval_hours")
        val UPDATE_WIFI_ONLY        = booleanPreferencesKey("update_wifi_only")
        val GLYPH_ENABLED           = booleanPreferencesKey("glyph_enabled")
    }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.HAS_SEEN_ONBOARDING] ?: false }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    val skipForwardSeconds: Flow<Int> = context.dataStore.data
        .map { it[Keys.SKIP_FORWARD_SECONDS] ?: 30 }

    val skipBackwardSeconds: Flow<Int> = context.dataStore.data
        .map { it[Keys.SKIP_BACKWARD_SECONDS] ?: 15 }

    val downloadPath: Flow<String> = context.dataStore.data
        .map { it[Keys.DOWNLOAD_PATH] ?: "" }

    val playbackSpeed: Flow<Float> = context.dataStore.data
        .map { it[Keys.PLAYBACK_SPEED]?.toFloatOrNull() ?: 1.0f }

    val podcastViewMode: Flow<String> = context.dataStore.data
        .map { it[Keys.PODCAST_VIEW_MODE] ?: "GRID3" }

    val gridShowLabels: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.GRID_SHOW_LABELS] ?: false }

    val loggingEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.LOGGING_ENABLED] ?: false }

    val episodeFilter: Flow<String> = context.dataStore.data
        .map { it[Keys.EPISODE_FILTER] ?: "UNPLAYED" }

    val episodeSort: Flow<String> = context.dataStore.data
        .map { it[Keys.EPISODE_SORT] ?: "DATE_DESC" }

    val autoUpdateEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.AUTO_UPDATE_ENABLED] ?: true }

    val updateIntervalHours: Flow<Int> = context.dataStore.data
        .map { it[Keys.UPDATE_INTERVAL_HOURS] ?: 3 }

    val updateWifiOnly: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.UPDATE_WIFI_ONLY] ?: true }

    val glyphEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.GLYPH_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setSkipForwardSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.SKIP_FORWARD_SECONDS] = seconds }
    }

    suspend fun setSkipBackwardSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.SKIP_BACKWARD_SECONDS] = seconds }
    }

    suspend fun setDownloadPath(path: String) {
        context.dataStore.edit { it[Keys.DOWNLOAD_PATH] = path }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed.toString() }
    }

    suspend fun setPodcastViewMode(mode: String) {
        context.dataStore.edit { it[Keys.PODCAST_VIEW_MODE] = mode }
    }

    suspend fun setGridShowLabels(show: Boolean) {
        context.dataStore.edit { it[Keys.GRID_SHOW_LABELS] = show }
    }

    suspend fun setLoggingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOGGING_ENABLED] = enabled }
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.dataStore.edit { it[Keys.HAS_SEEN_ONBOARDING] = seen }
    }

    suspend fun setEpisodeFilter(filter: String) {
        context.dataStore.edit { it[Keys.EPISODE_FILTER] = filter }
    }

    suspend fun setEpisodeSort(sort: String) {
        context.dataStore.edit { it[Keys.EPISODE_SORT] = sort }
    }

    suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_UPDATE_ENABLED] = enabled }
    }

    suspend fun setUpdateIntervalHours(hours: Int) {
        context.dataStore.edit { it[Keys.UPDATE_INTERVAL_HOURS] = hours }
    }

    suspend fun setUpdateWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { it[Keys.UPDATE_WIFI_ONLY] = wifiOnly }
    }

    suspend fun setGlyphEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GLYPH_ENABLED] = enabled }
    }
}
