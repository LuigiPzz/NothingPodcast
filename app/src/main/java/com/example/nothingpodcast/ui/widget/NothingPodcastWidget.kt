package com.example.nothingpodcast.ui.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import android.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.action.clickable
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition

class NothingPodcastWidget : GlanceAppWidget() {

    override val stateDefinition: androidx.glance.state.GlanceStateDefinition<*> = androidx.glance.state.PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val podcastTitle = prefs[KEY_PODCAST_TITLE] ?: "Nothing Podcast"
            val episodeTitle = prefs[KEY_EPISODE_TITLE] ?: "Nessun episodio"
            val isEmpty = episodeTitle == "Nessun episodio" || episodeTitle.isBlank()

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color.parseColor("#161616")))
                    .clickable(androidx.glance.action.actionStartActivity<com.example.nothingpodcast.MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                if (isEmpty) {
                    Image(
                        provider = androidx.glance.ImageProvider(com.example.nothingpodcast.R.drawable.nothing_podcast_empty_cover),
                        contentDescription = "Empty Cover",
                        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 100.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = podcastTitle,
                            style = TextStyle(
                                color = ColorProvider(Color.WHITE),
                                fontSize = 15.sp,
                                fontFamily = androidx.glance.text.FontFamily("ndot55")
                            )
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = episodeTitle,
                            style = TextStyle(color = ColorProvider(Color.LTGRAY), fontSize = 9.sp, fontFamily = androidx.glance.text.FontFamily.SansSerif)
                        )
                    }
                }
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
