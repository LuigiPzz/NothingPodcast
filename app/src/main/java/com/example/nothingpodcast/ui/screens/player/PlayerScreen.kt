package com.example.nothingpodcast.ui.screens.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.graphicsLayer
import coil3.compose.AsyncImage
import com.example.nothingpodcast.R
import com.example.nothingpodcast.ui.player.PlayerViewModel
import com.example.nothingpodcast.ui.player.PlayerUiState
import com.example.nothingpodcast.ui.theme.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate

@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val skipFwd  by playerViewModel.skipForwardSeconds.collectAsStateWithLifecycle()
    val skipBwd  by playerViewModel.skipBackwardSeconds.collectAsStateWithLifecycle()
    val episode = uiState.currentEpisode
    if (episode == null) {
        Box(modifier = Modifier.fillMaxSize().background(NothingBlack), contentAlignment = Alignment.Center) {
            Text("NESSUN EPISODIO IN RIPRODUZIONE", color = NothingWhite, style = MaterialTheme.typography.titleMedium)
        }
        return
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val vibrator = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
    }
    
    var showNotes    by remember { mutableStateOf(false) }
    var showChapters by remember { mutableStateOf(false) }
    var showSpeedSelector by remember { mutableStateOf(false) }
    var showAudioOutput   by remember { mutableStateOf(false) }
    var showTranscript    by remember { mutableStateOf(false) }
    
    val performClick = remember {
        {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10L)
            }
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
            .pointerInput(showSpeedSelector) {
                detectTapGestures {
                    if (showSpeedSelector) {
                        performClick()
                        showSpeedSelector = false
                    }
                }
            }
    ) {
        if (isLandscape) {
            PlayerLandscapeContent(
                episode = episode,
                uiState = uiState,
                skipFwd = skipFwd,
                skipBwd = skipBwd,
                showSpeedSelector = showSpeedSelector,
                onDismiss = onDismiss,
                onToggleSpeed = { performClick(); showSpeedSelector = !showSpeedSelector },
                onShowAudioOutput = { performClick(); showAudioOutput = true },
                onShowChapters = { showChapters = true },
                onShowNotes = { showNotes = true },
                onShowTranscript = { playerViewModel.fetchTranscript(); showTranscript = true },
                onTogglePlayPause = { performClick(); playerViewModel.togglePlayPause() },
                onSkipForward = { performClick(); playerViewModel.skipForward() },
                onSkipBackward = { performClick(); playerViewModel.skipBackward() },
                onSeek = { pos -> performClick(); playerViewModel.seekTo(pos) },
                onSpeedChange = { speed -> performClick(); playerViewModel.setSpeed(speed); showSpeedSelector = false }
            )
        } else {
            PlayerPortraitContent(
                episode = episode,
                uiState = uiState,
                skipFwd = skipFwd,
                skipBwd = skipBwd,
                showSpeedSelector = showSpeedSelector,
                onDismiss = onDismiss,
                onToggleSpeed = { performClick(); showSpeedSelector = !showSpeedSelector },
                onShowAudioOutput = { performClick(); showAudioOutput = true },
                onShowChapters = { showChapters = true },
                onShowNotes = { showNotes = true },
                onShowTranscript = { playerViewModel.fetchTranscript(); showTranscript = true },
                onTogglePlayPause = { performClick(); playerViewModel.togglePlayPause() },
                onSkipForward = { performClick(); playerViewModel.skipForward() },
                onSkipBackward = { performClick(); playerViewModel.skipBackward() },
                onSeek = { pos -> performClick(); playerViewModel.seekTo(pos) },
                onSpeedChange = { speed -> performClick(); playerViewModel.setSpeed(speed); showSpeedSelector = false }
            )
        }

        // --- Shared Dialogs ---
        if (showNotes) {
            PlayerInfoBottomSheet(
                title     = stringResource(R.string.label_episode_notes),
                content   = episode.description,
                onDismiss = { showNotes = false }
            )
        }

        if (showChapters) {
            ChaptersBottomSheet(
                chapters          = episode.chapters,
                soundbites        = episode.soundbites,
                currentPositionMs = uiState.positionMs,
                onChapterClick    = { seconds -> 
                    playerViewModel.seekTo(seconds * 1000)
                    showChapters = false
                },
                onDismiss         = { showChapters = false }
            )
        }

        if (showAudioOutput) {
            AudioOutputBottomSheet(
                onPerformClick = performClick,
                onDismiss = { showAudioOutput = false }
            )
        }

        if (showTranscript) {
            TranscriptBottomSheet(
                content = uiState.transcriptContent,
                isLoading = uiState.isTranscriptLoading,
                onDismiss = { showTranscript = false }
            )
        }
    }
}

@Composable
private fun PlayerPortraitContent(
    episode: com.example.nothingpodcast.domain.model.Episode,
    uiState: PlayerUiState,
    skipFwd: Int,
    skipBwd: Int,
    showSpeedSelector: Boolean,
    onDismiss: () -> Unit,
    onToggleSpeed: () -> Unit,
    onShowAudioOutput: () -> Unit,
    onShowChapters: () -> Unit,
    onShowNotes: () -> Unit,
    onShowTranscript: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerTopBar(
            podcastTitle = episode.podcastTitle,
            onDismiss = onDismiss,
            onShowAudioOutput = onShowAudioOutput
        )

        Spacer(Modifier.height(16.dp))

        PlayerArtwork(imageUrl = episode.imageUrl, title = episode.title, modifier = Modifier.size(280.dp))

        Spacer(Modifier.height(32.dp))

        PlayerEpisodeInfo(episode = episode)

        Spacer(Modifier.height(16.dp))

        PlayerTitle(title = episode.title)

        Spacer(Modifier.height(24.dp))

        PlayerChapterBar(
            chapters = episode.chapters,
            positionMs = uiState.positionMs,
            onClick = onShowChapters
        )

        Spacer(Modifier.height(24.dp))

        PlayerProgressBar(
            positionMs = uiState.positionMs,
            durationMs = uiState.durationMs,
            onSeek = onSeek
        )

        Spacer(Modifier.height(32.dp))

        PlayerControls(
            isPlaying = uiState.isPlaying,
            skipFwd = skipFwd,
            skipBwd = skipBwd,
            onTogglePlayPause = onTogglePlayPause,
            onSkipForward = onSkipForward,
            onSkipBackward = onSkipBackward
        )

        Spacer(Modifier.height(32.dp))

        androidx.compose.animation.AnimatedVisibility(visible = showSpeedSelector) {
            SpeedBar(
                currentSpeed = uiState.playbackSpeed,
                onPerformClick = {},
                onSpeedChange = onSpeedChange
            )
        }

        Spacer(Modifier.weight(1f))

        PlayerBottomUtilities(
            hasTranscript = !episode.transcriptUrl.isNullOrBlank(),
            playbackSpeed = uiState.playbackSpeed,
            showSpeedSelector = showSpeedSelector,
            onShowNotes = onShowNotes,
            onShowTranscript = onShowTranscript,
            onToggleSpeed = onToggleSpeed
        )
    }
}

@Composable
private fun PlayerLandscapeContent(
    episode: com.example.nothingpodcast.domain.model.Episode,
    uiState: PlayerUiState,
    skipFwd: Int,
    skipBwd: Int,
    showSpeedSelector: Boolean,
    onDismiss: () -> Unit,
    onToggleSpeed: () -> Unit,
    onShowAudioOutput: () -> Unit,
    onShowChapters: () -> Unit,
    onShowNotes: () -> Unit,
    onShowTranscript: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Artwork
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            PlayerArtwork(
                imageUrl = episode.imageUrl,
                title = episode.title,
                modifier = Modifier.size(240.dp)
            )
        }

        Spacer(Modifier.width(32.dp))

        // Right Column: Info & Controls
        Column(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight()
        ) {
            PlayerTopBar(
                podcastTitle = episode.podcastTitle,
                onDismiss = onDismiss,
                onShowAudioOutput = onShowAudioOutput
            )

            Spacer(Modifier.height(8.dp))

            PlayerTitle(title = episode.title, textAlign = TextAlign.Start)
            
            Spacer(Modifier.height(16.dp))

            PlayerProgressBar(
                positionMs = uiState.positionMs,
                durationMs = uiState.durationMs,
                onSeek = onSeek
            )

            Spacer(Modifier.height(16.dp))

            PlayerControls(
                isPlaying = uiState.isPlaying,
                skipFwd = skipFwd,
                skipBwd = skipBwd,
                onTogglePlayPause = onTogglePlayPause,
                onSkipForward = onSkipForward,
                onSkipBackward = onSkipBackward
            )

            Spacer(Modifier.weight(1f))

            if (showSpeedSelector) {
                SpeedBar(
                    currentSpeed = uiState.playbackSpeed,
                    onPerformClick = {},
                    onSpeedChange = onSpeedChange
                )
            } else {
                PlayerBottomUtilities(
                    hasTranscript = !episode.transcriptUrl.isNullOrBlank(),
                    playbackSpeed = uiState.playbackSpeed,
                    showSpeedSelector = showSpeedSelector,
                    onShowNotes = onShowNotes,
                    onShowTranscript = onShowTranscript,
                    onToggleSpeed = onToggleSpeed
                )
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    podcastTitle: String,
    onDismiss: () -> Unit,
    onShowAudioOutput: () -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.KeyboardArrowDown, "Close", tint = NothingWhite)
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = podcastTitle.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = NothingOnSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        
        IconButton(onClick = onShowAudioOutput) {
            Icon(Icons.Outlined.SpeakerGroup, "Uscita audio", tint = NothingOnSurfaceVariant)
        }
    }
}

@Composable
private fun PlayerArtwork(imageUrl: String, title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, NothingWhite)
            .background(NothingBlack),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model              = imageUrl,
            contentDescription = title,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PlayerEpisodeInfo(episode: com.example.nothingpodcast.domain.model.Episode) {
    val infoParts = mutableListOf<String>()
    episode.season?.takeIf { it > 0 }?.let { infoParts.add("STAGIONE $it") }
    episode.episodeNumber?.let { infoParts.add("EPISODIO $it") }
    
    val metaText = infoParts.joinToString(" • ")
    if (metaText.isNotEmpty() || !episode.episodeType.isNullOrBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (metaText.isNotEmpty()) {
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingOnSurfaceDim
                )
            }
            if (!episode.episodeType.isNullOrBlank() && episode.episodeType != "full") {
                if (metaText.isNotEmpty()) {
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingOnSurfaceDim
                    )
                }
                Surface(
                    color = NothingRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = episode.episodeType!!.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingRed,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerTitle(title: String, textAlign: TextAlign = TextAlign.Center) {
    Text(
        text      = title,
        style     = MaterialTheme.typography.titleLarge,
        fontFamily = SpaceMonoFamily,
        fontSize  = 16.sp,
        color     = NothingWhite,
        textAlign = textAlign,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
    )
}

@Composable
private fun PlayerChapterBar(
    chapters: List<com.example.nothingpodcast.domain.model.Chapter>,
    positionMs: Long,
    onClick: () -> Unit
) {
    val currentChapter = remember(chapters, positionMs) {
        val currentSeconds = positionMs / 1000
        chapters.findLast { it.startTime <= currentSeconds }
    }

    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.List,
            contentDescription = "Capitoli",
            tint = NothingWhite,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = currentChapter?.title?.uppercase() ?: stringResource(R.string.label_chapters).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = NothingWhite,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlayerProgressBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(horizontal = 0.dp)) {
            DottedProgressBar(
                progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                onSeek   = { frac -> onSeek((frac * durationMs).toLong()) }
            )
        }
        Row(
            modifier              = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatMs(positionMs), style = MaterialTheme.typography.labelSmall, color = NothingOnSurfaceDim)
            Text(formatMs(durationMs), style = MaterialTheme.typography.labelSmall, color = NothingOnSurfaceDim)
        }
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    skipFwd: Int,
    skipBwd: Int,
    onTogglePlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally)
    ) {
        // Backward
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSkipBackward)
            )
            DotIconSkip(direction = -1)
            Text(
                text = "${skipBwd}s",
                style = MaterialTheme.typography.labelSmall,
                color = NothingOnSurfaceDim,
                modifier = Modifier.align(Alignment.Center).padding(top = 36.dp)
            )
        }

        // Play/Pause
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTogglePlayPause)
            )
            DotIconPlayPause(isPlaying = isPlaying)
        }

        // Forward
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSkipForward)
            )
            DotIconSkip(direction = 1)
            Text(
                text = "${skipFwd}s",
                style = MaterialTheme.typography.labelSmall,
                color = NothingOnSurfaceDim,
                modifier = Modifier.align(Alignment.Center).padding(top = 36.dp)
            )
        }
    }
}

@Composable
private fun PlayerBottomUtilities(
    hasTranscript: Boolean,
    playbackSpeed: Float,
    showSpeedSelector: Boolean,
    onShowNotes: () -> Unit,
    onShowTranscript: () -> Unit,
    onToggleSpeed: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Info button
        UtilityButton(
            icon = Icons.Outlined.Info,
            label = "NOTE",
            onClick = onShowNotes
        )

        // Transcript button
        if (hasTranscript) {
            UtilityButton(
                icon = Icons.Outlined.Description,
                label = "TESTO",
                onClick = onShowTranscript
            )
        }

        // Speed toggle
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggleSpeed)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Speed,
                    contentDescription = "Velocità",
                    tint = if (showSpeedSelector) NothingWhite else NothingOnSurfaceDim,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "${playbackSpeed}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (showSpeedSelector) NothingWhite else NothingOnSurfaceDim
                )
            }
        }
    }
}

@Composable
private fun UtilityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NothingOnSurfaceDim,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = NothingOnSurfaceDim
            )
        }
    }
}

@Composable
private fun DottedProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit
) {
    val dotCount = 55
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        val width = size.width
        val centerY = size.height / 2
        val dotSpacing = width / (dotCount - 1)
        
        for (i in 0 until dotCount) {
            val dotX = i * dotSpacing
            val dotProgress = i.toFloat() / (dotCount - 1)
            
            if (dotProgress > progress) {
                drawCircle(
                    color = NothingBorderDim,
                    radius = 1.5.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(dotX, centerY)
                )
            } else {
                drawCircle(
                    color = NothingRed,
                    radius = 1.5.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(dotX, centerY)
                )
            }
        }
        
        val thumbX = progress * width
        drawCircle(
            color = NothingBlack,
            radius = 7.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(thumbX, centerY)
        )
        drawCircle(
            color = NothingWhite,
            radius = 5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(thumbX, centerY)
        )
    }
}

@Composable
private fun DotIconPlayPause(isPlaying: Boolean) {
    Canvas(modifier = Modifier.size(92.dp)) {
        val dotRadius = 2.8.dp.toPx() // Finer dots
        val spacing = 7.5.dp.toPx()   // More compact
        
        // Draw circular border
        drawCircle(
            color = NothingWhite,
            radius = size.width / 2,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.1.dp.toPx()) // Slightly thicker but subtle
        )

        if (isPlaying) {
            // Pause: two vertical bars of 5 dots
            val xGap = 9.dp.toPx() 
            for (y in 0..4) {
                val yPos = (y - 2f) * spacing + size.height/2
                drawCircle(NothingWhite, dotRadius, androidx.compose.ui.geometry.Offset(size.width/2 - xGap, yPos))
                drawCircle(NothingWhite, dotRadius, androidx.compose.ui.geometry.Offset(size.width/2 + xGap, yPos))
            }
        } else {
            // Play: Triangle 5-4-3-2-1
            val xStart = size.width/2 - (2 * spacing).toFloat() + 3.dp.toPx() 
            for (col in 0..4) {
                val dotsInCol = 5 - col
                for (row in 0 until dotsInCol) {
                    drawCircle(
                        color = NothingWhite,
                        radius = dotRadius,
                        center = androidx.compose.ui.geometry.Offset(xStart + col * spacing, size.height/2 - (dotsInCol-1)*spacing/2 + row * spacing)
                    )
                }
            }
        }
    }
}

@Composable
private fun DotIconSkip(direction: Int) {
    Canvas(modifier = Modifier.size(42.dp)) {
        val dotRadius = 1.6.dp.toPx() // Finer dots
        val hSpacing = 4.2.dp.toPx()
        val vSpacing = 3.8.dp.toPx()
        val xCenter = size.width / 2
        val yCenter = size.height / 2
        
        // Spaced out gap as in Turn 209 ideal image
        val chevronGap = 12.dp.toPx()

        for (chevron in 0..1) {
            val offset = if (chevron == 0) -chevronGap / 2 else chevronGap / 2
            val xBase = xCenter + (if (direction > 0) offset + hSpacing else offset - hSpacing)
            
            drawCircle(NothingWhite, dotRadius, androidx.compose.ui.geometry.Offset(xBase, yCenter))
            drawCircle(NothingWhite, dotRadius, androidx.compose.ui.geometry.Offset(xBase - (hSpacing * direction), yCenter - vSpacing))
            drawCircle(NothingWhite, dotRadius, androidx.compose.ui.geometry.Offset(xBase - (hSpacing * direction), yCenter + vSpacing))
            drawCircle(NothingWhite, dotRadius, androidx.compose.ui.geometry.Offset(xBase - (2 * hSpacing * direction), yCenter - 2 * vSpacing))
            drawCircle(NothingWhite, dotRadius, androidx.compose.ui.geometry.Offset(xBase - (2 * hSpacing * direction), yCenter + 2 * vSpacing))
        }
    }
}

@Composable
private fun SpeedBar(
    currentSpeed: Float,
    onPerformClick: () -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f) // New options
    Column(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            speeds.forEach { speed ->
                val isSelected = speed == currentSpeed
                Text(
                    text = "${speed}x",
                    style = if (isSelected) MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.labelMedium,
                    color = if (isSelected) NothingWhite else NothingOnSurfaceDim,
                    modifier = Modifier.clickable { 
                        onPerformClick()
                        onSpeedChange(speed) 
                    }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(speeds) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        val index = (ratio * (speeds.size - 1)).roundToInt()
                        onSpeedChange(speeds[index])
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackHeight = 1.dp.toPx()
                val tickHeight = 8.dp.toPx()
                val indicatorHeight = 16.dp.toPx()
                val indicatorWidth = 2.dp.toPx()
                
                // Track
                drawLine(
                    color = NothingBorderDim,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                    strokeWidth = trackHeight
                )
                
                // Ticks
                speeds.forEachIndexed { index, speed ->
                    val x = (index.toFloat() / (speeds.size - 1)) * size.width
                    val isSelected = speed == currentSpeed
                    
                    if (isSelected) {
                        drawLine(
                            color = NothingWhite,
                            start = androidx.compose.ui.geometry.Offset(x, size.height / 2 - indicatorHeight / 2),
                            end = androidx.compose.ui.geometry.Offset(x, size.height / 2 + indicatorHeight / 2),
                            strokeWidth = indicatorWidth
                        )
                    } else {
                        drawLine(
                            color = NothingBorderDim,
                            start = androidx.compose.ui.geometry.Offset(x, size.height / 2 - tickHeight / 2),
                            end = androidx.compose.ui.geometry.Offset(x, size.height / 2 + tickHeight / 2),
                            strokeWidth = trackHeight * 1.5f
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChaptersBottomSheet(
    chapters: List<com.example.nothingpodcast.domain.model.Chapter>,
    soundbites: List<com.example.nothingpodcast.domain.model.Soundbite>,
    currentPositionMs: Long,
    onChapterClick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Chapters, 1 = Highlights

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = NothingSurfaceHigh,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = NothingBorder) },
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = stringResource(R.string.label_chapters).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selectedTab == 0) NothingWhite else NothingOnSurfaceDim,
                    modifier = Modifier
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 8.dp)
                )
                if (soundbites.isNotEmpty()) {
                    Spacer(Modifier.width(24.dp))
                    Text(
                        text  = "HIGHLIGHTS",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selectedTab == 1) NothingWhite else NothingOnSurfaceDim,
                        modifier = Modifier
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
            Spacer(Modifier.height(24.dp))

            if (selectedTab == 0) {
                // Chapters List
                if (chapters.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("NESSUN CAPITOLO TROVATO.", style = MaterialTheme.typography.labelMedium, color = NothingOnSurfaceDim)
                    }
                } else {
                    val currentSeconds = currentPositionMs / 1000
                    LazyColumn {
                        items(chapters.indices.toList()) { index ->
                            val chapter = chapters[index]
                            val nextChapter = if (index + 1 < chapters.size) chapters[index + 1] else null
                            val isCurrent = currentSeconds >= chapter.startTime && (nextChapter == null || currentSeconds < nextChapter.startTime)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onChapterClick(chapter.startTime); onDismiss() }
                                    .padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) NothingRed else androidx.compose.ui.graphics.Color.Transparent)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isCurrent) NothingWhite else NothingOnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatMs(chapter.startTime * 1000),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NothingOnSurfaceDim
                                )
                            }
                            HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
                        }
                    }
                }
            } else {
                // Highlights List
                LazyColumn {
                    items(soundbites) { bite ->
                        val currentSeconds = currentPositionMs / 1000
                        val isCurrent = currentSeconds >= bite.startTime && currentSeconds < (bite.startTime + bite.duration)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChapterClick(bite.startTime); onDismiss() }
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayCircle,
                                contentDescription = null,
                                tint = if (isCurrent) NothingRed else NothingOnSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = bite.title.ifBlank { "HIGHLIGHT" },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isCurrent) NothingWhite else NothingOnSurfaceVariant
                                )
                                Text(
                                    text = "${bite.duration}s clip",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NothingOnSurfaceDim
                                )
                            }
                            Text(
                                text = formatMs(bite.startTime * 1000),
                                style = MaterialTheme.typography.labelMedium,
                                color = NothingOnSurfaceDim
                            )
                        }
                        HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerInfoBottomSheet(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = NothingSurfaceHigh,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = NothingBorder) },
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text  = title.uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = NothingWhite
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text  = content,
                style = MaterialTheme.typography.bodyLarge,
                color = NothingOnSurfaceVariant
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%d:%02d", m, s)
}
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AudioOutputBottomSheet(
    onPerformClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
    val prefs = remember { context.getSharedPreferences("nothing_devices_prefs", android.content.Context.MODE_PRIVATE) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    // Refresh device list and status when devices change
    val audioDeviceCallback = remember {
        object : android.media.AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) { refreshTrigger++ }
            override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) { refreshTrigger++ }
        }
    }

    DisposableEffect(audioManager) {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        onDispose { audioManager.unregisterAudioDeviceCallback(audioDeviceCallback) }
    }

    // Get MediaRouter service early
    val mediaRouter = remember { context.getSystemService(android.content.Context.MEDIA_ROUTER_SERVICE) as android.media.MediaRouter }

    // Unified Discovery Callback for both legacy and modern APIs
    DisposableEffect(Unit) {
        val callback = object : android.media.MediaRouter.SimpleCallback() {
            override fun onRouteSelected(mr: android.media.MediaRouter?, type: Int, group: android.media.MediaRouter.RouteInfo?) { refreshTrigger++ }
            override fun onRouteUnselected(mr: android.media.MediaRouter?, type: Int, group: android.media.MediaRouter.RouteInfo?) { refreshTrigger++ }
            override fun onRouteAdded(mr: android.media.MediaRouter?, route: android.media.MediaRouter.RouteInfo?) { refreshTrigger++ }
            override fun onRouteRemoved(mr: android.media.MediaRouter?, route: android.media.MediaRouter.RouteInfo?) { refreshTrigger++ }
            override fun onRouteChanged(mr: android.media.MediaRouter?, route: android.media.MediaRouter.RouteInfo?) { refreshTrigger++ }
        }
        
        val callback2 = if (android.os.Build.VERSION.SDK_INT >= 30) {
            object : android.media.MediaRouter2.RouteCallback() {
                override fun onRoutesAdded(routes: List<android.media.MediaRoute2Info>) { refreshTrigger++ }
                override fun onRoutesRemoved(routes: List<android.media.MediaRoute2Info>) { refreshTrigger++ }
                override fun onRoutesChanged(routes: List<android.media.MediaRoute2Info>) { refreshTrigger++ }
            }
        } else null

        mediaRouter.addCallback(android.media.MediaRouter.ROUTE_TYPE_LIVE_AUDIO, callback, android.media.MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
        
        if (android.os.Build.VERSION.SDK_INT >= 30 && callback2 != null) {
            val mr2 = android.media.MediaRouter2.getInstance(context)
            val preference = android.media.RouteDiscoveryPreference.Builder(
                listOf(android.media.MediaRoute2Info.FEATURE_LIVE_AUDIO), true
            ).build()
            mr2.registerRouteCallback(context.mainExecutor, callback2, preference)
        }

        onDispose { 
            mediaRouter.removeCallback(callback) 
            if (android.os.Build.VERSION.SDK_INT >= 30 && callback2 != null) {
                android.media.MediaRouter2.getInstance(context).unregisterRouteCallback(callback2)
            }
        }
    }

    // EXTREME Normalization for audio devices
    fun getNorm(name: String): String {
        val clean = name.lowercase()
            .split("(")[0] // Remove everything after (
            .replace("connected", "").replace("connesso", "")
            .replace("active", "").replace("attivo", "")
            .filter { it.isLetterOrDigit() }
        
        return if (clean.contains("phone") || clean.contains("speaker") || clean.contains("altoparlante") || clean.contains("telefono")) 
            "internal_speaker_id" 
        else clean
    }

    // Unified Device Model
    data class UnifiedDevice(
        val id: String,
        val name: String,
        val route: android.media.MediaRouter.RouteInfo? = null,
        val route2: android.media.MediaRoute2Info? = null,
        val device: android.media.AudioDeviceInfo? = null
    )

    val devices = remember(refreshTrigger) {
        val deviceMap = mutableMapOf<String, UnifiedDevice>()
        
        // Using the top-level getNorm defined above

        // Bluetooth Aliases (Only for name lookup, NOT for discovery)
        val btAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        val friendlyNames = mutableMapOf<String, String>()
        try {
            btAdapter?.bondedDevices?.forEach { device ->
                val name = device.name ?: ""
                val alias = if (android.os.Build.VERSION.SDK_INT >= 31) {
                    try { device.alias ?: name } catch (e: Exception) { name }
                } else name
                if (name.isNotEmpty()) {
                    friendlyNames[getNorm(name)] = alias
                    friendlyNames[name.lowercase()] = alias
                }
            }
        } catch (e: Exception) {}

        fun addOrUpdate(rawName: String, route: android.media.MediaRouter.RouteInfo? = null, route2: android.media.MediaRoute2Info? = null, device: android.media.AudioDeviceInfo? = null) {
            val norm = getNorm(rawName)
            if (norm.isEmpty()) return
            
            // Check if this is a Bluetooth/Hardware device
            val nL = rawName.lowercase()
            val isBtOrHw = (route2?.type ?: -1).let { t -> t == 8 || t == 26 || t == 23 } || 
                           (device != null) ||
                           nL.contains("ear") || nL.contains("nothing") || nL.contains("cmf") ||
                           nL.contains("headphone") || nL.contains("cuffie") || nL.contains("buds") ||
                           nL.contains("airpod") || nL.contains("wireless") || nL.contains("bt ") || 
                           nL.contains("bluetooth") || nL.contains("headset") || nL.contains("audio") ||
                           nL.contains("stereo") || nL.contains("a142") || nL.contains("b155")

            // STRICT FILTER: If it looks like a physical/BT device, it MUST have a 'device' (AudioManager) entry
            // to be considered "CONNECTED". MediaRouter often returns disconnected BT devices as "available".
            if (isBtOrHw && device == null && norm != "internal_speaker_id") {
                return 
            }

            val betterName = friendlyNames[norm] ?: friendlyNames[rawName.lowercase()] ?: rawName
            val existing = deviceMap[norm]
            
            if (existing == null) {
                deviceMap[norm] = UnifiedDevice(id = norm, name = betterName, route = route, route2 = route2, device = device)
            } else {
                deviceMap[norm] = existing.copy(
                    route = route ?: existing.route,
                    route2 = route2 ?: existing.route2,
                    device = device ?: existing.device,
                    // Keep the "best" name found so far
                    name = if (betterName.length > existing.name.length && !betterName.contains("A142", true)) betterName else existing.name
                )
            }
        }

        // 1. MediaRouter2: The ONLY source of truth for CONNECTED routes
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            val mr2 = android.media.MediaRouter2.getInstance(context)
            
            // Get all routes and filter strictly by CONNECTION_STATE_CONNECTED (2)
            mr2.routes.forEach { r2 ->
                val isConnected = r2.connectionState == 2
                
                // We show it if it's connected
                if (isConnected) {
                    val r2Name = r2.name?.toString() ?: return@forEach
                    
                    // Identify the physical device type if possible for the icon
                    val deviceType = when (r2.type) {
                        2 -> android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                        8, 26, 23 -> android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        3, 4, 22 -> android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        else -> -1
                    }
                    
                    // Create a dummy AudioDeviceInfo-like behavior for our mapper
                    // Or just pass the route2 info
                    addOrUpdate(r2Name, route2 = r2)
                }
            }
        } else {
            // Fallback for older devices (Legacy MediaRouter)
            val currentRoute = mediaRouter.getSelectedRoute(android.media.MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
            currentRoute?.let { lr ->
                val lrName = lr.getName(context)?.toString() ?: ""
                if (lrName.isNotEmpty()) addOrUpdate(lrName, route = lr)
            }
        }
        
        // Ensure Speaker is always there if the list is too empty or Speaker wasn't found
        if (deviceMap.values.none { getNorm(it.name) == "internal_speaker_id" }) {
            addOrUpdate("Nothing Phone (Speaker)")
        }

        deviceMap.values.toList()
              .map { 
                  val norm = getNorm(it.name)
                  if (norm == "internal_speaker_id" && android.os.Build.MANUFACTURER.contains("Nothing", true)) {
                      it.copy(name = "Nothing Phone (Speaker)")
                  } else it
              }
              .sortedByDescending { getNorm(it.name) == "internal_speaker_id" }
    }

    // Active route name for highlighting
    val activeRouteName = remember(refreshTrigger) {
        try {
            mediaRouter.getSelectedRoute(android.media.MediaRouter.ROUTE_TYPE_LIVE_AUDIO)?.getName(context)?.toString()
        } catch (e: Exception) { null }
    }

    // State to track if speaker is explicitly forced by the user
    var isSpeakerForced by remember { mutableStateOf(false) }
    
    // Update isSpeakerForced based on actual system state initially and on refreshes
    LaunchedEffect(refreshTrigger) {
        val isInCommMode = audioManager.mode == android.media.AudioManager.MODE_IN_COMMUNICATION
        val isCurrentlySpeaker = if (android.os.Build.VERSION.SDK_INT >= 31) {
            audioManager.communicationDevice?.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn
        }
        isSpeakerForced = isInCommMode && isCurrentlySpeaker
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NothingSurfaceHigh,
        dragHandle = { BottomSheetDefaults.DragHandle(color = NothingBorder) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
        ) {
            Text(
                text = "USCITA AUDIO",
                style = MaterialTheme.typography.titleMedium,
                color = NothingWhite,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices) { deviceItem ->
                    val name = deviceItem.name
                    val nLow = name.lowercase()
                    
                    // Identify if it's the internal speaker
                    val isInternalSpeaker = nLow.contains("phone") || nLow.contains("speaker") || 
                                          nLow.contains("altoparlante") || nLow.contains("telefono")
                    
                    val isNothingPhone = isInternalSpeaker && android.os.Build.MANUFACTURER.contains("Nothing", true)

                    // Nothing/CMF Model Codes Mapping
                    val modelCodes = mapOf(
                        "a142" to "Nothing Headphone (1)",
                        "b155" to "Nothing Ear (2)",
                        "b157" to "Nothing Ear (stick)",
                        "b181" to "Nothing Ear (1)",
                        "b163" to "Nothing Ear (a)"
                    )
                    
                    val mappedName = modelCodes[nLow]
                    val displayName = when {
                        isNothingPhone -> "Nothing Phone (Speaker)"
                        mappedName != null -> mappedName
                        else -> name
                    }
                    
                    val isManuallyTagged = remember(refreshTrigger, name) {
                        prefs.getBoolean("tagged_$name", false)
                    }
                    
                    val isNothing = nLow.contains("nothing") || isManuallyTagged || isNothingPhone || mappedName != null
                    val isCMF = nLow.contains("cmf")
                    val isBrandDevice = isNothing || isCMF

                    val isActive = remember(refreshTrigger, activeRouteName, name, isSpeakerForced, devices) {
                        val normActive = if (activeRouteName != null) getNorm(activeRouteName) else ""
                        val normDevice = getNorm(name)
                        
                        // 1. Forced Speaker Check
                        if (isInternalSpeaker && isSpeakerForced) return@remember true
                        
                        // 2. Direct Norm Matching
                        if (normActive.isNotEmpty() && normActive == normDevice) return@remember true
                        
                        // 3. Fallback for Internal Speaker (if nothing else is clearly selected or no other devices present)
                        val otherDevicesPresent = devices.any { getNorm(it.name) != "internal_speaker_id" }
                        if (isInternalSpeaker && (!otherDevicesPresent || normActive.isEmpty() || normActive == "internal_speaker_id")) return@remember true
                        
                        // 4. Name-based containing checks (Original logic)
                        val rn = activeRouteName?.lowercase() ?: ""
                        rn.isNotEmpty() && (rn.contains(nLow) || nLow.contains(rn) || (mappedName != null && rn.contains(mappedName.lowercase())))
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    onPerformClick()
                                    try {
                                        var selectionHandled = false
                                        
                                        // 0. PREEMPTIVE RESET: Release current locks
                                        audioManager.mode = android.media.AudioManager.MODE_NORMAL
                                        if (android.os.Build.VERSION.SDK_INT >= 31) {
                                            audioManager.clearCommunicationDevice()
                                        }

                                        // 1. SELECT VIA MEDIA ROUTER (Multi-channel selection for maximum compatibility)
                                        deviceItem.route?.let { 
                                            mediaRouter.selectRoute(android.media.MediaRouter.ROUTE_TYPE_LIVE_AUDIO, it)
                                            mediaRouter.selectRoute(android.media.MediaRouter.ROUTE_TYPE_USER, it)
                                            selectionHandled = true
                                        } ?: run {
                                            // Fallback: try to find the route by name if it wasn't pre-mapped
                                            for (i in 0 until mediaRouter.routeCount) {
                                                val lr = mediaRouter.getRouteAt(i)
                                                val lrName = lr.getName(context)?.toString() ?: ""
                                                val normName = nLow // Use nLow from outer scope
                                                if (lrName.lowercase().contains(normName) || normName.contains(lrName.lowercase())) {
                                                    mediaRouter.selectRoute(android.media.MediaRouter.ROUTE_TYPE_LIVE_AUDIO, lr)
                                                    mediaRouter.selectRoute(android.media.MediaRouter.ROUTE_TYPE_USER, lr)
                                                    selectionHandled = true
                                                    break
                                                }
                                            }
                                        }

                                        // 2. Legacy fallback
                                        if (!selectionHandled) {
                                            deviceItem.route?.let { 
                                                mediaRouter.selectRoute(android.media.MediaRouter.ROUTE_TYPE_LIVE_AUDIO, it)
                                                selectionHandled = true
                                            }
                                        }
                                        
                                        // 3. Hardware Fallback (Direct forcing via AudioManager)
                                        if (isInternalSpeaker) {
                                            audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
                                            if (android.os.Build.VERSION.SDK_INT >= 31) {
                                                val speaker = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                                                    .find { it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                                                speaker?.let { audioManager.setCommunicationDevice(it) }
                                            } else {
                                                @Suppress("DEPRECATION")
                                                audioManager.isSpeakerphoneOn = true
                                            }
                                            isSpeakerForced = true
                                        } else {
                                            isSpeakerForced = false
                                            
                                            if (android.os.Build.VERSION.SDK_INT >= 31) {
                                                deviceItem.device?.let { ad ->
                                                    if (ad.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO || 
                                                        ad.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET) {
                                                        audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
                                                    } else {
                                                        audioManager.mode = android.media.AudioManager.MODE_NORMAL
                                                    }
                                                    try { audioManager.setCommunicationDevice(ad) } catch(e: Exception) {}
                                                } ?: run {
                                                    audioManager.mode = android.media.AudioManager.MODE_NORMAL
                                                    audioManager.clearCommunicationDevice()
                                                }
                                            } else {
                                                @Suppress("DEPRECATION")
                                                audioManager.mode = android.media.AudioManager.MODE_NORMAL
                                                @Suppress("DEPRECATION")
                                                audioManager.isSpeakerphoneOn = false
                                            }
                                        }
                                        
                                        scope.launch {
                                            kotlinx.coroutines.delay(600)
                                            refreshTrigger++
                                        }
                                    } catch (e: Exception) { }
                                },
                                onLongClick = {
                                    if (!isInternalSpeaker) {
                                        val newState = !isManuallyTagged
                                        prefs.edit().putBoolean("tagged_$name", newState).apply()
                                        refreshTrigger++
                                    }
                                }
                            ),
                        color = if (isActive) NothingSurfaceMid else NothingBlack,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isActive) NothingWhite else if (isBrandDevice) NothingBorder else NothingBorderDim)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tint = if (isActive) NothingWhite else if (isBrandDevice) NothingWhite else NothingOnSurfaceDim
                            
                            // Icon Logic
                            val isHeadphone = nLow.contains("headphone") || nLow.contains("cuffie")
                            val isStick = nLow.contains("stick")
                            val isNeckband = nLow.contains("neckband")
                            val isEar = nLow.contains("ear") && !isStick && !isHeadphone
                            
                            Box(contentAlignment = Alignment.Center) {
                                when {
                                    isNothingPhone -> NothingPhoneIcon(tint = tint, modifier = Modifier.size(24.dp))
                                    isInternalSpeaker -> Icon(Icons.Outlined.VolumeUp, null, tint = tint, modifier = Modifier.size(24.dp))
                                    isHeadphone && isBrandDevice -> NothingHeadphoneIcon(tint = tint, modifier = Modifier.size(24.dp))
                                    isStick && isBrandDevice -> NothingStickIcon(tint = tint, modifier = Modifier.size(24.dp))
                                    isNeckband && isBrandDevice -> NothingNeckbandIcon(tint = tint, modifier = Modifier.size(24.dp))
                                    isEar && isBrandDevice -> NothingEarIcon(tint = tint, modifier = Modifier.size(24.dp))
                                    isCMF -> CmfBudIcon(tint = tint, modifier = Modifier.size(24.dp))
                                    isNothing -> Icon(Icons.Outlined.RadioButtonChecked, null, tint = tint, modifier = Modifier.size(24.dp))
                                    isHeadphone -> Icon(Icons.Outlined.Headset, null, tint = tint, modifier = Modifier.size(24.dp))
                                    else -> Icon(Icons.Outlined.Bluetooth, null, tint = tint, modifier = Modifier.size(24.dp))
                                }
                                
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(NothingRed)
                                            .border(1.dp, NothingSurfaceMid, CircleShape)
                                    )
                                }
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (isBrandDevice) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isNothing) "NOTHING DEVICE" else "CMF DEVICE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isActive) NothingRed else NothingOnSurfaceDim,
                                            letterSpacing = 1.sp
                                        )
                                        if (isManuallyTagged) {
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Outlined.PushPin,
                                                contentDescription = null,
                                                tint = if (isActive) NothingRed else NothingOnSurfaceDim,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isActive) NothingWhite else NothingOnSurface
                                )
                            }
                            
                        }
                    }
                }
                
                // --- Nothing X Shortcut (Redesigned with Red Accent) ---
                item {

                    Spacer(Modifier.height(32.dp))
                    
                    Surface(
                        onClick = {
                            onPerformClick()
                            val packages = listOf("com.nothing.hextra", "com.nothing.smartcenter")
                            var launched = false
                            for (pkg in packages) {
                                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                                if (intent != null) {
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                    launched = true
                                    break
                                }
                            }
                            if (!launched) {
                                val playStorePackage = "com.nothing.hextra"
                                try {
                                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$playStorePackage")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                                } catch (e: Exception) {
                                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$playStorePackage")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                            }
                        },
                        color = NothingSurfaceMid,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, NothingBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                NothingXLogo(modifier = Modifier.size(32.dp))
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "NOTHING X",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NothingRed,
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Impostazioni audio avanzate",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NothingWhite
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Outlined.ArrowForward,
                                contentDescription = null,
                                tint = NothingOnSurfaceDim,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NothingEarIcon(
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // 1. Stem (Clean solid outline)
        val stemWidth = w * 0.35f
        val stemHeight = h * 0.65f
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset((w - stemWidth) / 2, h * 0.3f),
            size = androidx.compose.ui.geometry.Size(stemWidth, stemHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        
        // 2. Bud top (Simple circle)
        drawCircle(
            color = tint,
            radius = w * 0.32f,
            center = androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.35f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        
        // 3. Small red dot (Signature)
        drawCircle(
            color = NothingRed,
            radius = 1.5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(w / 2, h * 0.45f)
        )
    }
}

@Composable
private fun NothingStickIcon(
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // 1. Stick body (Thin rounded rect)
        val stickW = w * 0.25f
        val stickH = h * 0.9f
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset((w - stickW) / 2, (h - stickH) / 2),
            size = androidx.compose.ui.geometry.Size(stickW, stickH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(stickW / 2, stickW / 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
        )
        
        // 2. Head part (Circular but no rubber tip)
        drawCircle(
            color = tint,
            radius = w * 0.28f,
            center = androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.25f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        
        // 3. Internal dots
        drawCircle(tint, 0.8.dp.toPx(), androidx.compose.ui.geometry.Offset(w / 2, h * 0.5f))
        drawCircle(tint, 0.8.dp.toPx(), androidx.compose.ui.geometry.Offset(w / 2, h * 0.6f))
    }
}

@Composable
private fun NothingNeckbandIcon(
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // 1. Cable (Arc at bottom)
        drawArc(
            color = tint.copy(alpha = 0.5f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.4f),
            size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.5f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        
        // 2. Earbuds on ends
        drawCircle(tint, w * 0.15f, androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.45f))
        drawCircle(tint, w * 0.15f, androidx.compose.ui.geometry.Offset(w * 0.9f, h * 0.45f))
        
        // 3. Controller part
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.4f, h * 0.75f),
            size = androidx.compose.ui.geometry.Size(w * 0.2f, h * 0.15f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
}

@Composable
private fun NothingPhoneIcon(
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // 1. Phone outline (Simple)
        val pw = w * 0.55f
        val ph = h * 0.9f
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset((w - pw) / 2, (h - ph) / 2),
            size = androidx.compose.ui.geometry.Size(pw, ph),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        
        // 2. Minimal Glyph hints
        val x = (w - pw) / 2
        val y = (h - ph) / 2
        
        // Camera hint
        drawCircle(tint, 2.dp.toPx(), androidx.compose.ui.geometry.Offset(x + 5.dp.toPx(), y + 5.dp.toPx()))
        
        // Center strip hint
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w / 2, y + ph * 0.3f),
            end = androidx.compose.ui.geometry.Offset(w / 2, y + ph * 0.7f),
            strokeWidth = 0.8.dp.toPx()
        )
    }
}

@Composable
private fun NothingHeadphoneIcon(
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // 1. Headband (Arc)
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.5f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )
        
        // 2. Ear Cups (Rounded Rects)
        val cupWidth = w * 0.32f
        val cupHeight = h * 0.45f
        val cupY = h * 0.45f
        
        // Left Cup
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(0f, cupY),
            size = androidx.compose.ui.geometry.Size(cupWidth, cupHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
        )
        
        // Right Cup
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w - cupWidth, cupY),
            size = androidx.compose.ui.geometry.Size(cupWidth, cupHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
        )
        
        // 3. Connection/Pivot Points
        drawCircle(
            color = tint,
            radius = 1.5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(cupWidth / 2, cupY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = tint,
            radius = 1.5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(w - cupWidth / 2, cupY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        
        // 4. Dot matrix pattern inside cups (subtle)
        val dotRadius = 0.5.dp.toPx()
        for (i in 0..1) {
            val xBase = if (i == 0) cupWidth / 2 else w - cupWidth / 2
            drawCircle(tint.copy(alpha = 0.4f), dotRadius, androidx.compose.ui.geometry.Offset(xBase, cupY + cupHeight * 0.4f))
            drawCircle(tint.copy(alpha = 0.4f), dotRadius, androidx.compose.ui.geometry.Offset(xBase - 3.dp.toPx(), cupY + cupHeight * 0.6f))
            drawCircle(tint.copy(alpha = 0.4f), dotRadius, androidx.compose.ui.geometry.Offset(xBase + 3.dp.toPx(), cupY + cupHeight * 0.6f))
        }
    }
}

@Composable
private fun CmfBudIcon(
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // 1. Ear tip (subtle circle behind stem)
        drawCircle(
            color = tint.copy(alpha = 0.2f),
            radius = w * 0.35f,
            center = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.35f)
        )
        
        // 2. Main Stem (Pill shape)
        val stemWidth = w * 0.42f
        val stemHeight = h * 0.88f
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset((w - stemWidth) / 2, (h - stemHeight) / 2),
            size = androidx.compose.ui.geometry.Size(stemWidth, stemHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(stemWidth / 2, stemWidth / 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
        )
        
        // 3. Top Sensor/Button Area
        drawCircle(
            color = tint.copy(alpha = 0.5f),
            radius = stemWidth * 0.32f,
            center = androidx.compose.ui.geometry.Offset(w / 2, h * 0.22f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        
        // 4. Dot Matrix Grid (3x3) - Matching the provided image!
        val dotRadius = 0.7.dp.toPx()
        val gridSpacing = 2.2.dp.toPx()
        val gridYStart = h * 0.62f
        for (row in 0..2) {
            for (col in 0..2) {
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = androidx.compose.ui.geometry.Offset(
                        w / 2 + (col - 1) * gridSpacing,
                        gridYStart + row * gridSpacing
                    )
                )
            }
        }
        
        // 5. Bottom Solid Dot (CMF signature)
        drawCircle(
            color = tint,
            radius = stemWidth * 0.22f,
            center = androidx.compose.ui.geometry.Offset(w / 2, h * 0.80f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranscriptBottomSheet(
    content: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = NothingSurfaceHigh,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = NothingBorder) },
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "TRASCRIZIONE",
                style = MaterialTheme.typography.headlineSmall,
                color = NothingWhite,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NothingWhite,
                        strokeWidth = 2.dp
                    )
                }
            } else if (content != null) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NothingOnSurfaceVariant
                )
            } else {
                Text(
                    text = "Impossibile caricare la trascrizione.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NothingOnSurfaceDim
                )
            }
        }
    }
}

@Composable
private fun NothingXLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // 1. Stylized earbud shape (Pill angled)
        rotate(degrees = 45f) {
            val pillWidth = w * 0.45f
            val pillHeight = h * 0.75f
            drawRoundRect(
                color = NothingWhite,
                topLeft = androidx.compose.ui.geometry.Offset((w - pillWidth) / 2, (h - pillHeight) / 2),
                size = androidx.compose.ui.geometry.Size(pillWidth, pillHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillWidth / 2, pillWidth / 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )
            
            // 2. Red Dot (Signature)
            drawCircle(
                color = NothingRed,
                radius = 2.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(w / 2, h * 0.4f)
            )
        }
        
        // 3. Dot matrix hints outside
        drawCircle(NothingWhite.copy(alpha = 0.6f), 0.8.dp.toPx(), androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.2f))
        drawCircle(NothingWhite.copy(alpha = 0.6f), 0.8.dp.toPx(), androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.8f))
    }
}
