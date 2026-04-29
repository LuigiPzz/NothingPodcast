package com.example.nothingpodcast.ui.screens.detail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.example.nothingpodcast.R
import com.example.nothingpodcast.domain.model.Episode
import com.example.nothingpodcast.domain.model.Podcast
import com.example.nothingpodcast.ui.player.PlayerViewModel
import com.example.nothingpodcast.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun PodcastDetailScreen(
    podcastId: String,
    onBack: () -> Unit,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    viewModel: PodcastDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val playingEpisodeId = playerState.currentEpisode?.id
    val isPlayerPlaying = playerState.isPlaying

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
    ) {
        // ── Top bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Indietro",
                tint = NothingWhite
            )
        }
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NothingWhite, strokeWidth = 1.dp)
            }
        } else {
            val episodes = uiState.filteredEpisodes
            var selectionMode by remember { mutableStateOf(false) }
            var selectedIds by remember { mutableStateOf(setOf<String>()) }
            var showSummarySheet by remember { mutableStateOf(false) }

            LazyColumn {
                // Podcast header
                uiState.podcast?.let { podcast ->
                    item { 
                        PodcastHeader(
                            podcast = podcast,
                            onUnsubscribe = viewModel::unsubscribe,
                            onRefresh = viewModel::refresh,
                            onShowFilters = { viewModel.setShowFilterSheet(true) },
                            onShowSummary = { showSummarySheet = true }
                        ) 
                    }
                }

                if (showSummarySheet) {
                    item {
                        PodcastSummaryBottomSheet(
                            podcast = uiState.podcast!!,
                            onToggleSubscription = {
                                if (uiState.podcast?.isSubscribed == true) viewModel.unsubscribe()
                                else viewModel.subscribe() // Need to add subscribe() to ViewModel or check if it exists
                                showSummarySheet = false
                            },
                            onDismiss = { showSummarySheet = false }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectionMode) {
                            val allSelected = selectedIds.size == episodes.size && episodes.isNotEmpty()
                            IconButton(onClick = {
                                if (allSelected) selectedIds = emptySet()
                                else selectedIds = episodes.map { it.id }.toSet()
                            }) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                    contentDescription = "Seleziona tutto",
                                    tint = NothingWhite
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            IconButton(
                                onClick = {
                                    viewModel.markEpisodesPlayed(selectedIds)
                                    selectionMode = false
                                    selectedIds = emptySet()
                                },
                                enabled = selectedIds.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle, 
                                    contentDescription = "Segna come riprodotti", 
                                    tint = if (selectedIds.isNotEmpty()) NothingWhite else NothingOnSurfaceDim
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.markEpisodesUnplayed(selectedIds)
                                    selectionMode = false
                                    selectedIds = emptySet()
                                },
                                enabled = selectedIds.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Outlined.RadioButtonUnchecked, 
                                    contentDescription = "Segna come non riprodotti", 
                                    tint = if (selectedIds.isNotEmpty()) NothingWhite else NothingOnSurfaceDim
                                )
                            }

                            IconButton(onClick = { selectionMode = false; selectedIds = emptySet() }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Chiudi", tint = NothingWhite)
                            }
                        } else {
                            Text(
                                text     = "EPISODI",
                                fontFamily = SpaceMonoFamily,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp,
                                color    = NothingOnSurfaceDim,
                                modifier = Modifier.weight(1f)
                            )

                        }
                    }
                    HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 24.dp))
                }



                itemsIndexed(episodes, key = { _, ep -> ep.id }) { index, episode ->
                    val isSelected = selectedIds.contains(episode.id)
                    val isPlayingThis = (episode.id == playingEpisodeId)
                    EpisodeListItem(
                        episode        = episode,
                        isPlaying      = isPlayingThis && isPlayerPlaying,
                        downloadProgress = uiState.downloadProgress[episode.id],
                        selectionMode = selectionMode,
                        isSelected = isSelected,
                        onToggleSelection = {
                            selectedIds = if (isSelected) selectedIds - episode.id else selectedIds + episode.id
                        },
                        onEnterSelectionMode = { selectionMode = true; selectedIds = setOf(episode.id) },
                        onPlay         = { 
                            playerViewModel.playEpisode(episode)
                            onNavigateToPlayer()
                        },
                        onPause        = { playerViewModel.togglePlayPause() },
                        onMarkPlayed   = { viewModel.markEpisodePlayed(episode.id) },
                        onMarkUnplayed = { viewModel.markEpisodeUnplayed(episode.id) },
                        onDownload     = { viewModel.downloadEpisode(episode) },
                        onDeleteDownload = { viewModel.deleteDownload(episode) } 
                    )
                    HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 24.dp))
                }
            }
        }
    }

    if (uiState.showFilterSheet) {
        FilterSortBottomSheet(
            filterType = uiState.filterType,
            sortType = uiState.sortType,
            onFilterChange = viewModel::setFilter,
            onSortChange = viewModel::setSort,
            onDismiss = { viewModel.setShowFilterSheet(false) }
        )
    }
}

@Composable
private fun PodcastHeader(
    podcast: Podcast,
    onUnsubscribe: () -> Unit,
    onRefresh: () -> Unit,
    onShowFilters: () -> Unit,
    onShowSummary: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Image box with rounded corners
        Box(
            modifier = Modifier
                .size(100.dp)
                .border(1.dp, NothingWhite)
                .clickable { onShowSummary() }
        ) {
            AsyncImage(
                model = podcast.imageUrl,
                contentDescription = podcast.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(Modifier.width(20.dp))
        
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = podcast.title,
                    fontFamily = PlayfairFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = NothingWhite,
                    maxLines = 3,
                    lineHeight = 30.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                // Menu
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    Icon(
                        imageVector = Icons.Outlined.MoreHoriz, 
                        contentDescription = "Menu", 
                        tint = NothingOnSurfaceDim,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { menuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = NothingSurfaceHigh
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ordinamento e filtri", style = MaterialTheme.typography.labelMedium, color = NothingWhite) },
                            onClick = {
                                menuExpanded = false
                                onShowFilters()
                            }
                        )
                        HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_refresh), style = MaterialTheme.typography.labelMedium, color = NothingWhite) },
                            onClick = {
                                menuExpanded = false
                                onRefresh()
                            }
                        )
                        if (podcast.isSubscribed) {
                            HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_unsubscribe), style = MaterialTheme.typography.labelMedium, color = NothingError) },
                                onClick = {
                                    menuExpanded = false
                                    onUnsubscribe()
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = podcast.author,
                fontFamily = SpaceMonoFamily,
                fontSize = 14.sp,
                color = NothingOnSurfaceDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!podcast.fundingUrl.isNullOrBlank()) {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clickable { uriHandler.openUri(podcast.fundingUrl) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Favorite,
                        contentDescription = null,
                        tint = NothingError,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = (podcast.fundingText ?: "Support this podcast").uppercase(),
                        fontFamily = SpaceMonoFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun EpisodeListItem(
    episode: Episode,
    isPlaying: Boolean,
    downloadProgress: Int?,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onMarkPlayed: () -> Unit,
    onMarkUnplayed: () -> Unit,
    onDownload: () -> Unit,
    onDeleteDownload: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dateStr    = remember(episode.publishDate) {
        if (episode.publishDate > 0) dateFormat.format(Date(episode.publishDate)) else ""
    }
    val durationStr = remember(episode.duration) { formatDuration(episode.duration) }
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleSelection()
                    else if (isPlaying) onPause()
                    else onPlay()
                },
                onLongClick = { 
                    if (!selectionMode) expanded = true 
                }
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
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

        Column(modifier = Modifier.weight(1f)) {
            // Episode Info (Season, Episode, Type)
            val infoParts = mutableListOf<String>()
            episode.season?.takeIf { it > 0 }?.let { infoParts.add("S$it") }
            episode.episodeNumber?.let { infoParts.add("E$it") }
            
            val metaText = infoParts.joinToString(" ")
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (metaText.isNotEmpty()) {
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingRed,
                        fontFamily = SpaceMonoFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingOnSurfaceDim,
                        fontFamily = SpaceMonoFamily,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingOnSurfaceDim,
                    fontFamily = SpaceMonoFamily,
                    fontSize = 10.sp
                )
                
                if (durationStr.isNotBlank()) {
                    Text(
                        text = " • $durationStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingOnSurfaceDim,
                        fontFamily = SpaceMonoFamily,
                        fontSize = 10.sp
                    )
                }
                
                if (!episode.episodeType.isNullOrBlank() && episode.episodeType != "full") {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = NothingOnSurfaceDim.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = episode.episodeType!!.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = NothingOnSurfaceDim,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))

            Text(
                text     = episode.title,
                fontFamily = SpaceMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color    = if (episode.isPlayed && !isPlaying) NothingOnSurfaceVariant else NothingWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            val isStarted = episode.playbackPosition > 0 && !episode.isPlayed
            if (isPlaying || isStarted) {
                // Progress bar for active or started episodes
                Spacer(Modifier.height(12.dp))
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
                            .background(if (isPlaying) NothingWhite else NothingOnSurfaceDim)
                    )
                    if (isPlaying) {
                        // Thumb dot only when playing
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (progress * 220).dp) // Scaled for list width
                                .size(6.dp)
                                .background(NothingWhite, CircleShape)
                        )
                    }
                }
                
                if (isPlaying) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDurationMs(episode.playbackPosition * 1000),
                            fontFamily = SpaceMonoFamily,
                            fontSize = 10.sp,
                            color = NothingOnSurfaceDim
                        )
                        Text(
                            text = formatDurationMs(episode.duration * 1000),
                            fontFamily = SpaceMonoFamily,
                            fontSize = 10.sp,
                            color = NothingOnSurfaceDim
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        // Action Icon (Equalizer or Download)
        if (!selectionMode) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.CenterVertically)
                    .border(
                        width = 1.dp,
                        color = if (isPlaying) NothingWhite else NothingBorderDim,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { 
                        if (isPlaying) onPause()
                        else if (episode.isDownloaded) onPlay() 
                        else onDownload()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    EqualizerIcon()
                } else if (downloadProgress != null && !episode.isDownloaded) {
                    if (downloadProgress == 0) {
                        CircularProgressIndicator(
                            color = NothingWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            progress = { downloadProgress.toFloat() / 100f },
                            color = NothingWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else if (episode.isDownloaded) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Play",
                        tint = NothingWhite,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = "Download",
                        tint = NothingWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Context menu (triggered by long press)
        Box {
            DropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false },
                containerColor   = NothingSurfaceHigh
            ) {
                // Streaming
                if (!episode.isDownloaded) {
                    DropdownMenuItem(
                        text    = { Text(stringResource(R.string.action_stream), style = MaterialTheme.typography.labelMedium, color = NothingWhite) },
                        onClick = {
                            expanded = false
                            onPlay()
                        }
                    )
                }
                
                // Played / Unplayed
                DropdownMenuItem(
                    text    = { 
                        Text(
                            text = if (episode.isPlayed) stringResource(R.string.action_mark_unplayed) 
                                   else stringResource(R.string.action_mark_played), 
                            style = MaterialTheme.typography.labelMedium, 
                            color = NothingWhite
                        ) 
                    },
                    onClick = {
                        expanded = false
                        if (episode.isPlayed) onMarkUnplayed() else onMarkPlayed()
                    }
                )

                // Multi selection
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_multi_selection), style = MaterialTheme.typography.labelMedium, color = NothingWhite) },
                    onClick = {
                        expanded = false
                        onEnterSelectionMode()
                    }
                )
                
                // Delete
                if (episode.isDownloaded) {
                    HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
                    DropdownMenuItem(
                        text    = { Text(stringResource(R.string.action_delete_download), style = MaterialTheme.typography.labelMedium, color = NothingError) },
                        onClick = { expanded = false; onDeleteDownload() }
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0L) return ""
    val h = TimeUnit.SECONDS.toHours(seconds)
    val m = TimeUnit.SECONDS.toMinutes(seconds) % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun formatDurationMs(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%d:%02d", m, s)
}

@Composable
private fun EqualizerIcon() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    
    @Composable
    fun Bar(duration: Int) {
        val height by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(duration),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            )
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight(height)
                .background(NothingWhite, RoundedCornerShape(1.dp))
        )
    }

    Row(
        modifier = Modifier.size(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Bar(400)
        Bar(600)
        Bar(350)
        Bar(500)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSortBottomSheet(
    filterType: EpisodeFilter,
    sortType: EpisodeSort,
    onFilterChange: (EpisodeFilter) -> Unit,
    onSortChange: (EpisodeSort) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = NothingSurfaceHigh,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Ordinamento e filtri",
                fontFamily = PlayfairFamily,
                fontSize = 22.sp,
                color = NothingWhite,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Filtro
            Text(
                text = "FILTRA",
                style = MaterialTheme.typography.labelSmall,
                color = NothingOnSurfaceDim,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterType == EpisodeFilter.ALL,
                    onClick = { onFilterChange(EpisodeFilter.ALL) },
                    label = { Text("Tutti", style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NothingWhite,
                        selectedLabelColor = NothingBlack,
                        containerColor = NothingBlack,
                        labelColor = NothingWhite
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = NothingBorder,
                        enabled = true,
                        selected = filterType == EpisodeFilter.ALL
                    )
                )
                FilterChip(
                    selected = filterType == EpisodeFilter.UNPLAYED,
                    onClick = { onFilterChange(EpisodeFilter.UNPLAYED) },
                    label = { Text("Non riprodotti", style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NothingWhite,
                        selectedLabelColor = NothingBlack,
                        containerColor = NothingBlack,
                        labelColor = NothingWhite
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = NothingBorder,
                        enabled = true,
                        selected = filterType == EpisodeFilter.UNPLAYED
                    )
                )
                FilterChip(
                    selected = filterType == EpisodeFilter.PLAYED,
                    onClick = { onFilterChange(EpisodeFilter.PLAYED) },
                    label = { Text("Riprodotti", style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NothingWhite,
                        selectedLabelColor = NothingBlack,
                        containerColor = NothingBlack,
                        labelColor = NothingWhite
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = NothingBorder,
                        enabled = true,
                        selected = filterType == EpisodeFilter.PLAYED
                    )
                )
            }

            Spacer(Modifier.height(24.dp))

            // Ordinamento
            Text(
                text = "ORDINA",
                style = MaterialTheme.typography.labelSmall,
                color = NothingOnSurfaceDim,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = sortType == EpisodeSort.DATE_DESC,
                    onClick = { onSortChange(EpisodeSort.DATE_DESC) },
                    label = { Text("Più recenti", style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NothingWhite,
                        selectedLabelColor = NothingBlack,
                        containerColor = NothingBlack,
                        labelColor = NothingWhite
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = NothingBorder,
                        enabled = true,
                        selected = sortType == EpisodeSort.DATE_DESC
                    )
                )
                FilterChip(
                    selected = sortType == EpisodeSort.DATE_ASC,
                    onClick = { onSortChange(EpisodeSort.DATE_ASC) },
                    label = { Text("Meno recenti", style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NothingWhite,
                        selectedLabelColor = NothingBlack,
                        containerColor = NothingBlack,
                        labelColor = NothingWhite
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = NothingBorder,
                        enabled = true,
                        selected = sortType == EpisodeSort.DATE_ASC
                    )
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodcastSummaryBottomSheet(
    podcast: Podcast,
    onToggleSubscription: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NothingSurfaceHigh,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = podcast.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .border(1.dp, NothingWhite)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = podcast.title,
                        fontFamily = PlayfairFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = podcast.author,
                        fontFamily = SpaceMonoFamily,
                        fontSize = 12.sp,
                        color = NothingOnSurfaceDim
                    )
                    
                    if (!podcast.medium.isNullOrBlank()) {
                        Surface(
                            color = NothingWhite.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = podcast.medium!!.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = NothingOnSurfaceVariant,
                                fontFamily = SpaceMonoFamily,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 8.sp
                            )
                        }
                    }
                    
                    if (!podcast.locationName.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Outlined.Place, null, tint = NothingOnSurfaceDim, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = podcast.locationName.uppercase(),
                                fontFamily = SpaceMonoFamily,
                                fontSize = 10.sp,
                                color = NothingOnSurfaceDim
                            )
                        }
                    }
                }
            }
            
            if (!podcast.socialInteractUrl.isNullOrBlank()) {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { uriHandler.openUri(podcast.socialInteractUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NothingBorderDim),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingWhite)
                ) {
                    Icon(Icons.Outlined.Chat, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PARTECIPA ALLA DISCUSSIONE", fontFamily = SpaceMonoFamily, fontSize = 12.sp)
                }
            }

            if (!podcast.licenseName.isNullOrBlank()) {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !podcast.licenseUrl.isNullOrBlank()) { 
                            podcast.licenseUrl?.let { uriHandler.openUri(it) } 
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Copyright, null, tint = NothingOnSurfaceDim, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "LICENZA: ${podcast.licenseName.uppercase()}",
                        fontFamily = SpaceMonoFamily,
                        fontSize = 10.sp,
                        color = NothingOnSurfaceDim
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = "Descrizione",
                fontFamily = SpaceMonoFamily,
                fontSize = 14.sp,
                color = NothingWhite,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = podcast.description,
                style = MaterialTheme.typography.bodyMedium,
                color = NothingOnSurfaceDim,
                lineHeight = 24.sp
            )

            if (podcast.persons.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "CREDITI",
                    fontFamily = SpaceMonoFamily,
                    fontSize = 14.sp,
                    color = NothingWhite,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                podcast.persons.forEach { person ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar or Placeholder
                        if (!person.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = person.imageUrl,
                                contentDescription = person.name,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, NothingBorderDim, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(NothingSurfaceHigh, CircleShape)
                                    .border(1.dp, NothingBorderDim, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = person.name.take(1).uppercase(),
                                    color = NothingWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = person.name,
                                fontFamily = SpaceMonoFamily,
                                fontSize = 14.sp,
                                color = NothingWhite
                            )
                            if (!person.role.isNullOrBlank()) {
                                Text(
                                    text = person.role.uppercase(),
                                    fontFamily = SpaceMonoFamily,
                                    fontSize = 10.sp,
                                    color = NothingOnSurfaceDim
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            
            // Subscribe / Unsubscribe Button at the bottom
            Button(
                onClick = onToggleSubscription,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (podcast.isSubscribed) NothingBlack else NothingWhite,
                    contentColor = if (podcast.isSubscribed) NothingWhite else NothingBlack
                ),
                shape = RoundedCornerShape(8.dp),
                border = if (podcast.isSubscribed) BorderStroke(1.dp, NothingBorderDim) else null
            ) {
                Text(
                    text = if (podcast.isSubscribed) "DISISCRIVITI" else "ISCRIVITI",
                    fontFamily = SpaceMonoFamily,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}
