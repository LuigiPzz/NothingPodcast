package com.example.nothingpodcast.ui.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import android.graphics.Color

class NothingPodcastWidget : GlanceAppWidget() {

    override val stateDefinition: androidx.glance.state.GlanceStateDefinition<*> = androidx.glance.state.PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color.BLACK))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nothing Podcast",
                    style = TextStyle(
                        color = ColorProvider(Color.WHITE)
                    )
                )
                Text(
                    text = "Versione Semplice",
                    style = TextStyle(
                        color = ColorProvider(Color.GRAY)
                    )
                )
            }
        }
    }

    companion object {
        val KEY_PODCAST_TITLE = androidx.datastore.preferences.core.stringPreferencesKey("podcast_title")
        val KEY_EPISODE_TITLE = androidx.datastore.preferences.core.stringPreferencesKey("episode_title")
        val KEY_IS_PLAYING = androidx.datastore.preferences.core.booleanPreferencesKey("is_playing")
        val KEY_CURRENT_TIME = androidx.datastore.preferences.core.stringPreferencesKey("current_time")
        val KEY_TOTAL_TIME = androidx.datastore.preferences.core.stringPreferencesKey("total_time")
    }
}
