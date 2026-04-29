package com.example.nothingpodcast.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.example.nothingpodcast.R
import com.example.nothingpodcast.domain.model.Episode
import com.example.nothingpodcast.ui.player.PlayerUiState
import com.example.nothingpodcast.ui.theme.*

/**
 * Persistent mini player bar — sits above the bottom nav bar.
 * Mimics Nothing X's persistent status bar style.
 */
@Composable
fun NothingMiniPlayer(
    uiState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val episode = uiState.currentEpisode ?: return

    val progress by animateFloatAsState(
        targetValue = if (uiState.durationMs > 0)
            (uiState.positionMs.toFloat() / uiState.durationMs).coerceIn(0f, 1f) else 0f,
        label = "mini_progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NothingBlack)
            .clickable(onClick = onClick)
    ) {
        // Thin progress line at top (Nothing X style indicator)
        LinearProgressIndicator(
            progress           = { progress },
            modifier           = Modifier.fillMaxWidth().height(2.dp),
            color              = NothingRed,
            trackColor         = NothingBorderDim,
            drawStopIndicator  = {}
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp), // Increased bottom padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork — Square with white border
            AsyncImage(
                model             = episode.imageUrl,
                contentDescription = null,
                contentScale      = ContentScale.Crop,
                modifier          = Modifier
                    .size(44.dp)
                    .border(1.dp, NothingWhite)
            )

            Spacer(Modifier.width(12.dp))

            // Episode + podcast title
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = episode.title,
                    style    = MaterialTheme.typography.titleMedium,
                    color    = NothingWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Play / Pause button — Outline style, no circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = NothingWhite, strokeWidth = 1.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = NothingWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

