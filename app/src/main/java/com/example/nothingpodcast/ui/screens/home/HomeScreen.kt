package com.example.nothingpodcast.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nothingpodcast.R
import coil3.compose.AsyncImage
import com.example.nothingpodcast.data.auth.AuthState
import com.example.nothingpodcast.domain.model.Podcast
import com.example.nothingpodcast.ui.auth.AuthViewModel
import com.example.nothingpodcast.ui.player.PlayerViewModel
import com.example.nothingpodcast.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPodcastClick:         (String) -> Unit,
    onNavigateToDownloads:  () -> Unit,
    onNavigateToSettings:   () -> Unit,
    onResetOnboarding:      () -> Unit,
    playerViewModel:        PlayerViewModel,
    viewModel:              HomeViewModel = hiltViewModel(),
    authViewModel:          AuthViewModel = hiltViewModel()
) {
    val uiState           by viewModel.uiState.collectAsStateWithLifecycle()
    val authState         by authViewModel.authState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val userEmail = (authState as? AuthState.LoggedIn)?.email

    val infiniteTransition = rememberInfiniteTransition(label = "wobble")
    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )
    val currentRotation = if (uiState.isEditMode) rotationAnim else 0f

    uiState.error?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Long)
            viewModel.dismissError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Row 1: Brand title + profile icon ────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = "Nothing Podcast",
                    style    = MaterialTheme.typography.displayMedium,
                    color    = NothingWhite,
                    modifier   = Modifier.weight(1f)
                )
                ProfileMenu(
                    userEmail             = userEmail,
                    onNavigateToDownloads = onNavigateToDownloads,
                    onNavigateToSettings  = onNavigateToSettings
                )
            }

            // ── Row 2: Section label "Iscrizioni" + view mode menu ───────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = stringResource(R.string.label_subscriptions),
                    style    = MaterialTheme.typography.headlineMedium,
                    color    = NothingWhite,
                    modifier = Modifier.weight(1f)
                )


                if (uiState.isEditMode) {
                    TextButton(onClick = viewModel::saveOrder) {
                        Text("FINE", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    ViewModeMenu(
                        currentMode        = uiState.viewMode,
                        showGridLabels     = uiState.showGridLabels,
                        onViewModeChange   = viewModel::setViewMode,
                        onToggleGridLabels = viewModel::toggleGridLabels
                    )
                }
            }

            HorizontalDivider(color = NothingBorder, thickness = 0.5.dp)

            // ── Podcast list / grid with Pull to Refresh ───────────────────
            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refreshAll,
                state = pullState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = uiState.isRefreshing,
                        containerColor = NothingSurfaceHigh,
                        color = NothingWhite,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            ) {
                when {
                    uiState.subscribedPodcasts.isEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 32.dp)
                                ) {
                                    Text(
                                        text = "Inizia la tua esperienza",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = NothingWhite
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Scopri alcuni dei podcast più amati dalla community o cercali cliccando il tasto +",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = NothingOnSurfaceDim
                                    )
                                }
                            }
                            
                            item {
                                Text(
                                    text = "SCELTI PER TE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NothingOnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    letterSpacing = 2.sp
                                )
                            }
                            
                            if (uiState.suggestedPodcasts.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = NothingWhite, strokeWidth = 1.dp)
                                    }
                                }
                            } else {
                                items(uiState.suggestedPodcasts) { podcast ->
                                    SearchResultItem(
                                        podcast = podcast,
                                        isSubscribed = false,
                                        onSubscribe = { viewModel.subscribeToPodcast(podcast) },
                                        onClick = { onPodcastClick(podcast.id) }
                                    )
                                    HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                    uiState.viewMode == PodcastViewMode.LIST -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            itemsIndexed(uiState.subscribedPodcasts, key = { _, p -> p.id }) { index, podcast ->
                                PodcastListItem(
                                    podcast     = podcast,
                                    rotation    = currentRotation,
                                    isEditMode  = uiState.isEditMode,
                                    onClick     = { onPodcastClick(podcast.id) },
                                    onLongClick = { viewModel.setEditMode(true) },
                                    onMove      = { offset ->
                                        val threshold = 100f
                                        val currentIndex = uiState.subscribedPodcasts.indexOf(podcast)
                                        if (currentIndex != -1) {
                                            if (offset > threshold && currentIndex < uiState.subscribedPodcasts.size - 1) {
                                                viewModel.movePodcast(currentIndex, currentIndex + 1)
                                                true
                                            } else if (offset < -threshold && currentIndex > 0) {
                                                viewModel.movePodcast(currentIndex, currentIndex - 1)
                                                true
                                            } else false
                                        } else false
                                    }
                                )
                                HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
                            }
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns               = GridCells.Fixed(uiState.viewMode.columns),
                            contentPadding        = PaddingValues(12.dp, 12.dp, 12.dp, 96.dp),
                            verticalArrangement   = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier              = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(uiState.subscribedPodcasts, key = { _, p -> p.id }) { index, podcast ->
                                val columns = uiState.viewMode.columns
                                PodcastGridItem(
                                    podcast     = podcast,
                                    showLabel   = uiState.showGridLabels,
                                    rotation    = currentRotation,
                                    isEditMode  = uiState.isEditMode,
                                    onClick     = { onPodcastClick(podcast.id) },
                                    onLongClick = { viewModel.setEditMode(true) },
                                    onMove      = { dx, dy ->
                                        val threshold = 120f
                                        val currentIndex = uiState.subscribedPodcasts.indexOf(podcast)
                                        if (currentIndex != -1) {
                                            when {
                                                dx > threshold && currentIndex % columns < columns - 1 -> {
                                                    viewModel.movePodcast(currentIndex, currentIndex + 1)
                                                    true
                                                }
                                                dx < -threshold && currentIndex % columns > 0 -> {
                                                    viewModel.movePodcast(currentIndex, currentIndex - 1)
                                                    true
                                                }
                                                dy > threshold && currentIndex + columns < uiState.subscribedPodcasts.size -> {
                                                    viewModel.movePodcast(currentIndex, currentIndex + columns)
                                                    true
                                                }
                                                dy < -threshold && currentIndex - columns >= 0 -> {
                                                    viewModel.movePodcast(currentIndex, currentIndex - columns)
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── FAB — round Material Design ───────────────────────────────────
        FloatingActionButton(
            onClick        = viewModel::showSearchSheet,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            shape          = CircleShape,
            containerColor = NothingWhite,
            contentColor   = NothingBlack
        ) {
            Icon(Icons.Rounded.Add, "Aggiungi podcast", modifier = Modifier.size(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }

    // ── Search bottom sheet ────────────────────────────────────────────────
    if (uiState.showSearchSheet) {
        SearchBottomSheet(
            uiState        = uiState,
            onQueryChange  = viewModel::onSearchQueryChanged,
            onSubscribe    = viewModel::subscribeToPodcast,
            onPodcastClick = { podcastId ->
                viewModel.hideSearchSheet()
                onPodcastClick(podcastId)
            },
            onDismiss     = viewModel::hideSearchSheet,
            subscribedIds = uiState.subscribedPodcasts.map { it.id }.toSet()
        )
    }
}

// ── Profile menu ──────────────────────────────────────────────────────────────

@Composable
private fun ProfileMenu(
    userEmail:             String?,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSettings:  () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            val initial = if (userEmail != null) {
                userEmail.first().uppercaseChar().toString()
            } else {
                "N"
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                DottedLetter(
                    char = initial.first(),
                    dotColor = NothingWhite
                )
            }
        }

        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            containerColor   = NothingSurfaceHigh
        ) {
            // Account info header
            if (userEmail != null) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = stringResource(R.string.label_account),
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingOnSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = NothingWhite
                    )
                }
            } else {
                // Guest mode
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = "Ospite",
                        style = MaterialTheme.typography.titleMedium,
                        color = NothingWhite
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Non loggato",
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingOnSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)

            // Downloads
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.action_downloads),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NothingWhite
                    )
                },
                leadingIcon = { Icon(Icons.Outlined.Download, null, tint = NothingOnSurfaceDim, modifier = Modifier.size(20.dp)) },
                onClick = { expanded = false; onNavigateToDownloads() }
            )

            // Settings
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.action_settings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NothingWhite
                    )
                },
                leadingIcon = { Icon(Icons.Outlined.Settings, null, tint = NothingOnSurfaceDim, modifier = Modifier.size(20.dp)) },
                onClick = { expanded = false; onNavigateToSettings() }
            )

        }
    }
}

@Composable
private fun DottedLetter(
    char: Char,
    modifier: Modifier = Modifier,
    dotColor: Color = Color.White
) {
    val dotRadius = 0.9.dp // Much smaller dots for correct proportions
    val spacing = 1.8.dp   // Tighter grid
    
    // Grid patterns (no spaces between dots for correct horizontal density)
    val pattern = when (char.uppercaseChar()) {
        'L' -> listOf(
            ".",
            ".",
            ".",
            ".",
            ".",
            ".",
            ".",
            "....."
        )
        'A' -> listOf(
            "  .  ",
            " . . ",
            ".   .",
            ".....",
            ".   .",
            ".   .",
            ".   .",
            ".   ."
        )
        'G' -> listOf(
            " .... ",
            ".     ",
            ".     ",
            ".  ...",
            ".    .",
            ".    .",
            ".    .",
            " .... "
        )
        'N' -> listOf(
            ".    .",
            "..   .",
            ". .  .",
            ".  . .",
            ".   ..",
            ".    .",
            ".    .",
            ".    ."
        )
        else -> listOf(
            "...",
            ". .",
            "...",
            ". .",
            ". ."
        )
    }

    val gridWidth = pattern.maxOf { it.length }
    val gridHeight = pattern.size

    Canvas(modifier = modifier.size(width = (spacing * (gridWidth - 1)) + (dotRadius * 2), height = (spacing * (gridHeight - 1)) + (dotRadius * 2))) {
        // Draw the 4 "anchor" dots in the corners of the letter grid
        val corners = mutableListOf(
            androidx.compose.ui.geometry.Offset(0f, 0f),
            androidx.compose.ui.geometry.Offset((gridWidth - 1) * spacing.toPx(), 0f),
            androidx.compose.ui.geometry.Offset(0f, (gridHeight - 1) * spacing.toPx()),
            androidx.compose.ui.geometry.Offset((gridWidth - 1) * spacing.toPx(), (gridHeight - 1) * spacing.toPx())
        )
        
        // Rimuove il puntino in alto a destra per la lettera L
        if (char.uppercaseChar() == 'L') {
            corners.removeAt(1)
        }
        
        corners.forEach { offset ->
            drawCircle(
                color = dotColor,
                radius = dotRadius.toPx(),
                center = androidx.compose.ui.geometry.Offset(offset.x + dotRadius.toPx(), offset.y + dotRadius.toPx())
            )
        }

        pattern.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, c ->
                if (c == '.') {
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius.toPx(),
                        center = androidx.compose.ui.geometry.Offset(
                            colIndex * spacing.toPx() + dotRadius.toPx(),
                            rowIndex * spacing.toPx() + dotRadius.toPx()
                        )
                    )
                }
            }
        }
    }
}

// ── View mode dropdown ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewModeBottomSheet(
    currentMode:        PodcastViewMode,
    showGridLabels:     Boolean,
    onViewModeChange:   (PodcastViewMode) -> Unit,
    onToggleGridLabels: () -> Unit,
    onDismiss:          () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = NothingSurfaceHigh,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Header Visualizzazione
            Text(
                text     = stringResource(R.string.header_view_mode).uppercase(),
                style    = MaterialTheme.typography.titleMedium,
                color    = NothingWhite,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Layout options (horizontal segment buttons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NothingBorderDim, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(NothingBlack),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PodcastViewMode.entries.forEachIndexed { index, mode ->
                    val isSelected = mode == currentMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isSelected) NothingWhite else NothingBlack)
                            .clickable { 
                                onViewModeChange(mode) 
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when(mode) {
                                PodcastViewMode.LIST -> "LISTA"
                                PodcastViewMode.GRID2 -> "2×"
                                PodcastViewMode.GRID3 -> "3×"
                                PodcastViewMode.GRID4 -> "4×"
                                PodcastViewMode.GRID5 -> "5×"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) NothingBlack else NothingWhite,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (index < PodcastViewMode.entries.size - 1) {
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(NothingBorderDim)
                        )
                    }
                }
            }

            if (currentMode != PodcastViewMode.LIST) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
                Spacer(Modifier.height(24.dp))

                Text(
                    text     = "TITOLI PODCAST",
                    style    = MaterialTheme.typography.titleMedium,
                    color    = NothingWhite,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Toggle labels (Grid names option)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NothingBorderDim, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingBlack),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // NOMI SI
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (showGridLabels) NothingWhite else NothingBlack)
                            .clickable { if (!showGridLabels) onToggleGridLabels() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MOSTRA",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (showGridLabels) NothingBlack else NothingWhite,
                            fontWeight = if (showGridLabels) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(NothingBorderDim)
                    )

                    // NOMI NO
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (!showGridLabels) NothingWhite else NothingBlack)
                            .clickable { if (showGridLabels) onToggleGridLabels() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NASCONDI",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (!showGridLabels) NothingBlack else NothingWhite,
                            fontWeight = if (!showGridLabels) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewModeMenu(
    currentMode:        PodcastViewMode,
    showGridLabels:     Boolean,
    onViewModeChange:   (PodcastViewMode) -> Unit,
    onToggleGridLabels: () -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { showSheet = true }) {
            Icon(Icons.Outlined.GridView, "Opzioni vista", tint = NothingOnSurfaceVariant)
        }

        if (showSheet) {
            ViewModeBottomSheet(
                currentMode        = currentMode,
                showGridLabels     = showGridLabels,
                onViewModeChange   = onViewModeChange,
                onToggleGridLabels = onToggleGridLabels,
                onDismiss          = { showSheet = false }
            )
        }
    }
}

// ── Search bottom sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBottomSheet(
    uiState:        HomeUiState,
    onQueryChange:  (String) -> Unit,
    onSubscribe:    (Podcast) -> Unit,
    onPodcastClick: (String) -> Unit,
    onDismiss:      () -> Unit,
    subscribedIds:  Set<String>
) {
    val sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val focusManager   = LocalFocusManager.current

    val checkSubscribed = remember(subscribedIds, uiState.subscribedPodcasts) {
        { podcast: Podcast ->
            podcast.id in subscribedIds || uiState.subscribedPodcasts.any { 
                it.feedUrl.lowercase().trim() == podcast.feedUrl.lowercase().trim() 
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = NothingSurfaceHigh,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text       = stringResource(R.string.label_add_podcast),
                style      = MaterialTheme.typography.displaySmall,
                color      = NothingWhite,
                modifier   = Modifier.padding(bottom = 14.dp)
            )

            OutlinedTextField(
                value         = uiState.searchQuery,
                onValueChange = onQueryChange,
                placeholder   = {
                    Text("Cerca titolo o autore…", style = MaterialTheme.typography.bodyMedium, color = NothingOnSurfaceDim)
                },
                leadingIcon   = { Icon(Icons.Outlined.Search, null, tint = NothingOnSurfaceVariant) },
                trailingIcon  = if (uiState.searchQuery.isNotBlank()) ({
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Close, null, tint = NothingOnSurfaceVariant)
                    }
                }) else null,
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape         = RoundedCornerShape(16.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = NothingWhite,
                    unfocusedBorderColor    = NothingBorder,
                    cursorColor             = NothingWhite,
                    focusedTextColor        = NothingWhite,
                    unfocusedTextColor      = NothingWhite,
                    focusedContainerColor   = NothingSurfaceHigh,
                    unfocusedContainerColor = NothingSurfaceHigh
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier  = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(Modifier.height(16.dp))

            var previewPodcast by remember { mutableStateOf<Podcast?>(null) }

            when {
                uiState.isSearching -> Box(
                    modifier         = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NothingWhite, strokeWidth = 1.5.dp, modifier = Modifier.size(32.dp))
                }

                uiState.searchQuery.isBlank() -> {
                    val listToShow = if (uiState.subscribedPodcasts.isNotEmpty()) {
                        uiState.recommendedPodcasts
                    } else {
                        uiState.suggestedPodcasts
                    }
                    val headerText = if (uiState.subscribedPodcasts.isNotEmpty()) {
                        "CONSIGLIATI PER TE"
                    } else {
                        "SCELTI PER TE"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.labelSmall,
                            color = NothingOnSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp),
                            letterSpacing = 2.sp
                        )
                        
                        if (uiState.isLoadingRecommendations && listToShow.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = NothingWhite, strokeWidth = 1.dp, modifier = Modifier.size(24.dp))
                            }
                        } else if (listToShow.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Nessun consiglio disponibile", style = MaterialTheme.typography.bodyMedium, color = NothingOnSurfaceDim)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 350.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(listToShow, key = { it.id }) { podcast ->
                                    val isSubscribed = checkSubscribed(podcast)
                                    SearchResultItem(
                                        podcast = podcast,
                                        isSubscribed = isSubscribed,
                                        onSubscribe = { onSubscribe(podcast) },
                                        onClick = {
                                            if (isSubscribed) {
                                                onPodcastClick(podcast.id)
                                            } else {
                                                previewPodcast = podcast
                                            }
                                        }
                                    )
                                    HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }

                uiState.searchResults.isEmpty() -> Box(
                    modifier         = Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nessun risultato", style = MaterialTheme.typography.bodyMedium, color = NothingOnSurfaceDim)
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier       = Modifier.fillMaxWidth()
                ) {
                    items(uiState.searchResults, key = { it.id }) { podcast ->
                        val isSubscribed = checkSubscribed(podcast)
                        SearchResultItem(
                            podcast      = podcast,
                            isSubscribed = isSubscribed,
                            onSubscribe  = { onSubscribe(podcast) },
                            onClick = { 
                                if (isSubscribed) {
                                    onPodcastClick(podcast.id)
                                } else {
                                    previewPodcast = podcast 
                                }
                            }
                        )
                        HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
                    }
                }
            }

            if (previewPodcast != null) {
                PodcastPreviewBottomSheet(
                    podcast      = previewPodcast!!,
                    isSubscribed = checkSubscribed(previewPodcast!!),
                    onSubscribe  = { 
                        onSubscribe(previewPodcast!!)
                        previewPodcast = null 
                    },
                    onDismiss    = { previewPodcast = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodcastPreviewBottomSheet(
    podcast:      Podcast,
    isSubscribed: Boolean,
    onSubscribe:  () -> Unit,
    onDismiss:    () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = NothingSurfaceHigh,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model              = podcast.imageUrl,
                contentDescription = podcast.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(160.dp)
                    .border(1.dp, NothingWhite)
            )
            
            Spacer(Modifier.height(20.dp))
            
            Text(
                text      = podcast.title,
                style     = MaterialTheme.typography.headlineSmall,
                color     = NothingWhite,
                textAlign = TextAlign.Center
            )
            
            Text(
                text      = podcast.author,
                style     = MaterialTheme.typography.bodyMedium,
                color     = NothingOnSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text      = podcast.description,
                style     = MaterialTheme.typography.bodySmall,
                color     = NothingOnSurfaceDim,
                maxLines  = 5,
                overflow  = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = onSubscribe,
                enabled = !isSubscribed,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = NothingWhite,
                    contentColor   = NothingBlack,
                    disabledContainerColor = NothingBorder,
                    disabledContentColor   = NothingOnSurfaceDim
                )
            ) {
                Text(
                    text  = if (isSubscribed) "Iscritto" else "Iscriviti",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// ── List item ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PodcastListItem(
    podcast:     Podcast,
    rotation:    Float,
    isEditMode:  Boolean,
    onClick:     () -> Unit,
    onLongClick: () -> Unit,
    onMove:      (Float) -> Boolean // Returns true if it triggered a move
) {
    var totalDrag by remember { mutableStateOf(0f) }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .zIndex(if (totalDrag != 0f) 1f else 0f)
            .graphicsLayer { 
                rotationZ    = rotation
                translationY = totalDrag
                scaleX       = if (totalDrag != 0f) 1.05f else 1f
                scaleY       = if (totalDrag != 0f) 1.05f else 1f
            }
            .then(
                if (isEditMode) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDrag += dragAmount.y
                                if (onMove(totalDrag)) {
                                    // When we move, we don't reset to 0 immediately to avoid jumps
                                    // but we adjust for the item height. For now, reset is safer 
                                    // but we'll try to keep it fluid.
                                    totalDrag = 0f 
                                }
                            },
                            onDragEnd = { totalDrag = 0f },
                            onDragCancel = { totalDrag = 0f }
                        )
                    }
                } else {
                    Modifier.combinedClickable(
                        onClick     = onClick,
                        onLongClick = onLongClick
                    )
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model              = podcast.imageUrl,
            contentDescription = podcast.title,
            contentScale       = ContentScale.Crop,
            colorFilter        = if (podcast.unplayedCount == 0) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
            modifier           = Modifier
                .size(56.dp)
                .border(1.dp, NothingWhite)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(podcast.title, style = MaterialTheme.typography.titleMedium, color = NothingWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(podcast.author, style = MaterialTheme.typography.bodySmall, color = NothingOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (podcast.unplayedCount > 0) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier         = Modifier
                    .clip(CircleShape)
                    .background(NothingBlack)
                    .border(1.dp, NothingWhite, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = podcast.unplayedCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontFamily = NDot55Family,
                    color = NothingWhite
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Rounded.ChevronRight, null, tint = NothingOnSurfaceDim, modifier = Modifier.size(20.dp))
    }
}

// ── Grid item ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PodcastGridItem(
    podcast:     Podcast,
    showLabel:   Boolean,
    rotation:    Float,
    isEditMode:  Boolean,
    onClick:     () -> Unit,
    onLongClick: () -> Unit,
    onMove:      (Float, Float) -> Boolean
) {
    var totalDragX by remember { mutableStateOf(0f) }
    var totalDragY by remember { mutableStateOf(0f) }

    Column(
        modifier            = Modifier
            .zIndex(if (totalDragX != 0f || totalDragY != 0f) 1f else 0f)
            .graphicsLayer { 
                rotationZ    = rotation
                translationX = totalDragX
                translationY = totalDragY
                scaleX       = if (totalDragX != 0f || totalDragY != 0f) 1.1f else 1f
                scaleY       = if (totalDragX != 0f || totalDragY != 0f) 1.1f else 1f
            }
            .then(
                if (isEditMode) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { 
                                totalDragX = 0f
                                totalDragY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y
                                if (onMove(totalDragX, totalDragY)) {
                                    totalDragX = 0f
                                    totalDragY = 0f
                                }
                            },
                            onDragEnd = { 
                                totalDragX = 0f
                                totalDragY = 0f
                            },
                            onDragCancel = { 
                                totalDragX = 0f
                                totalDragY = 0f
                            }
                        )
                    }
                } else {
                    Modifier.combinedClickable(
                        onClick     = onClick,
                        onLongClick = onLongClick
                    )
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            AsyncImage(
                model              = podcast.imageUrl,
                contentDescription = podcast.title,
                contentScale       = ContentScale.Crop,
                colorFilter        = if (podcast.unplayedCount == 0) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                modifier           = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .border(1.dp, NothingWhite)
            )
            if (podcast.unplayedCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(NothingBlack)
                        .border(1.dp, NothingWhite, CircleShape)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = podcast.unplayedCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontFamily = NDot55Family,
                        color = NothingWhite
                    )
                }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(
                text      = podcast.title,
                style     = MaterialTheme.typography.labelMedium,
                color     = NothingOnSurfaceVariant,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Search result item ────────────────────────────────────────────────────────

@Composable
private fun SearchResultItem(
    podcast:      Podcast,
    isSubscribed: Boolean,
    onSubscribe:  () -> Unit,
    onClick:      () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model              = podcast.imageUrl,
            contentDescription = podcast.title,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .size(52.dp)
                .border(1.dp, NothingWhite)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(podcast.title, style = MaterialTheme.typography.titleSmall, color = NothingWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(podcast.author, style = MaterialTheme.typography.bodySmall, color = NothingOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        if (isSubscribed) {
            Icon(
                imageVector        = Icons.Outlined.Check,
                contentDescription = "Iscritto",
                tint               = NothingOnSurfaceDim,
                modifier           = Modifier.size(24.dp).padding(4.dp)
            )
        } else {
            IconButton(onClick = onSubscribe) {
                Icon(
                    imageVector        = Icons.Outlined.Add,
                    contentDescription = "Iscriviti",
                    tint               = NothingWhite,
                    modifier           = Modifier.size(24.dp)
                )
            }
        }
    }
}
