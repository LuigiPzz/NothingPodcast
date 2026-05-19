package com.example.nothingpodcast.ui.screens.downloads

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.nothingpodcast.R
import com.example.nothingpodcast.domain.model.Episode
import com.example.nothingpodcast.ui.player.PlayerViewModel
import com.example.nothingpodcast.ui.theme.*
import java.util.concurrent.TimeUnit

@Composable
fun DownloadsScreen(
    playerViewModel: PlayerViewModel,
    onBack:          () -> Unit = {},
    onNavigateToPlayer: () -> Unit = {},
    viewModel:       DownloadsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint               = NothingWhite
                )
            }
            Text(
                text  = "DOWNLOAD",
                style = MaterialTheme.typography.displaySmall,
                color = NothingWhite
            )
        }

        HorizontalDivider(color = NothingBorder, thickness = 0.5.dp)

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NothingWhite, strokeWidth = 1.dp)
            }
        } else if (uiState.episodes.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = stringResource(R.string.label_no_downloads_empty),
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = NothingOnSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            var selectionMode by remember { mutableStateOf(false) }
            var selectedIds by remember { mutableStateOf(setOf<String>()) }
            val episodes = uiState.episodes

            Column(modifier = Modifier.fillMaxSize()) {
                // Multi-selection toolbar (only visible in selection mode)
                if (selectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedIds.size} selezionati",
                            style = MaterialTheme.typography.titleMedium,
                            color = NothingWhite,
                            modifier = Modifier.weight(1f)
                        )

                        // Select All
                        val allSelected = selectedIds.size == episodes.size && episodes.isNotEmpty()
                        IconButton(onClick = {
                            if (allSelected) selectedIds = emptySet()
                            else selectedIds = episodes.map { it.id }.toSet()
                        }) {
                            Icon(
                                imageVector = if (allSelected) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                contentDescription = stringResource(R.string.action_select_all),
                                tint = NothingWhite
                            )
                        }

                        // Delete selected
                        IconButton(
                            onClick = {
                                val toDelete = episodes.filter { it.id in selectedIds }
                                viewModel.deleteEpisodes(toDelete)
                                selectionMode = false
                                selectedIds = emptySet()
                            },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Elimina selezionati",
                                tint = if (selectedIds.isNotEmpty()) NothingWhite else NothingOnSurfaceDim
                            )
                        }

                        // Exit
                        IconButton(onClick = { selectionMode = false; selectedIds = emptySet() }) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close), tint = NothingWhite)
                        }
                    }
                    HorizontalDivider(color = NothingBorder, thickness = 0.5.dp)
                }

                LazyColumn {
                    items(episodes, key = { it.id }) { episode ->
                        val isSelected = selectedIds.contains(episode.id)
                        DownloadedEpisodeItem(
                            episode = episode,
                            selectionMode = selectionMode,
                            isSelected = isSelected,
                            onToggleSelection = {
                                selectedIds = if (isSelected) selectedIds - episode.id else selectedIds + episode.id
                            },
                            onEnterSelectionMode = { selectionMode = true; selectedIds = setOf(episode.id) },
                            onPlay = { 
                                playerViewModel.playEpisode(episode)
                                onNavigateToPlayer()
                            },
                            onDelete = { viewModel.deleteDownload(episode) }
                        )
                        HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DownloadedEpisodeItem(
    episode: Episode,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val durationStr = remember(episode.duration) {
        val h = TimeUnit.SECONDS.toHours(episode.duration)
        val m = TimeUnit.SECONDS.toMinutes(episode.duration) % 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    }
    val sizeStr = remember(episode.fileSize) {
        if (episode.fileSize > 0) "%.1f MB".format(episode.fileSize / 1_048_576.0) else ""
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleSelection()
                    else onPlay()
                },
                onLongClick = {
                    if (!selectionMode) onEnterSelectionMode()
                }
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                colors = CheckboxDefaults.colors(
                    checkedColor = NothingWhite,
                    checkmarkColor = NothingBlack,
                    uncheckedColor = NothingBorder
                ),
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        AsyncImage(
            model              = episode.imageUrl,
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .size(64.dp)
                .border(1.dp, NothingBorderDim)
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = episode.podcastTitle,
                style    = MaterialTheme.typography.labelMedium,
                color    = NothingRed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text     = episode.title,
                style    = MaterialTheme.typography.titleMedium,
                color    = NothingWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text     = listOfNotNull(durationStr.takeIf { it.isNotBlank() }, sizeStr.takeIf { it.isNotBlank() })
                    .joinToString(" · "),
                style    = MaterialTheme.typography.labelMedium,
                color    = NothingOnSurfaceDim
            )

            // Progress Bar for started episodes
            if (episode.playbackPosition > 0 && !episode.isPlayed) {
                Spacer(Modifier.height(10.dp))
                val progress = if (episode.duration > 0) (episode.playbackPosition.toFloat() / episode.duration).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(NothingBorderDim)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(1.dp)
                            .background(NothingOnSurfaceDim)
                    )
                }
            }
        }

        if (!selectionMode) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline, 
                    contentDescription = "Delete", 
                    tint = NothingOnSurfaceDim, 
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
