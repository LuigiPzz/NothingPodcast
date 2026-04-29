package com.example.nothingpodcast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nothingpodcast.data.auth.AuthState
import com.example.nothingpodcast.ui.auth.AuthViewModel
import com.example.nothingpodcast.ui.auth.LoginScreen
import com.example.nothingpodcast.ui.components.NothingMiniPlayer
import com.example.nothingpodcast.ui.navigation.NothingNavGraph
import com.example.nothingpodcast.ui.navigation.Screen
import com.example.nothingpodcast.ui.player.PlayerViewModel
import com.example.nothingpodcast.ui.theme.NothingBlack
import com.example.nothingpodcast.ui.theme.NothingPodcastTheme
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MainActivityRoot() }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // This is crucial for LaunchedEffect to see the new intent
    }
}

@Composable
fun MainActivityRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // ── Database Compatibility Check ──────────────────────────────────────
    var showCompatibilityDialog by remember { 
        mutableStateOf(!com.example.nothingpodcast.util.DatabaseCompatibilityHelper.isDatabaseCompatible(context)) 
    }
    
    if (showCompatibilityDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { /* Force decision */ }) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                color = com.example.nothingpodcast.ui.theme.NothingSurfaceHigh,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = com.example.nothingpodcast.ui.theme.NothingRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    androidx.compose.material3.Text(
                        text = "Incompatibilità Database",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                        color = com.example.nothingpodcast.ui.theme.NothingWhite,
                        fontFamily = com.example.nothingpodcast.ui.theme.OutfitFamily
                    )
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.material3.Text(
                        text = "La versione attuale dell'app richiede una nuova struttura dati. Desideri resettare il database per continuare? (Le iscrizioni andranno perse)",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = com.example.nothingpodcast.ui.theme.NothingOnSurfaceDim,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { (context as? android.app.Activity)?.finish() },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.nothingpodcast.ui.theme.NothingBorder)
                        ) {
                            androidx.compose.material3.Text("Esci", color = com.example.nothingpodcast.ui.theme.NothingWhite)
                        }
                        androidx.compose.material3.Button(
                            onClick = {
                                com.example.nothingpodcast.util.DatabaseCompatibilityHelper.resetDatabase(context)
                                showCompatibilityDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = com.example.nothingpodcast.ui.theme.NothingWhite,
                                contentColor = com.example.nothingpodcast.ui.theme.NothingBlack
                            )
                        ) {
                            androidx.compose.material3.Text("Resetta")
                        }
                    }
                }
            }
        }
        return // IMPORTANT: Do not proceed to ViewModel creation yet
    }

    // ONLY create ViewModels if database is compatible
    val authViewModel: AuthViewModel = hiltViewModel()
    val settingsViewModel: com.example.nothingpodcast.ui.screens.settings.SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(settingsState.loggingEnabled) {
        com.example.nothingpodcast.util.AppLogger.isEnabled = settingsState.loggingEnabled
        if (settingsState.loggingEnabled) {
            com.example.nothingpodcast.util.AppLogger.log(context, "INFO", "App started, logging enabled.")
        }
    }

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val hasSeenOnboarding by authViewModel.hasSeenOnboarding.collectAsStateWithLifecycle()

    NothingPodcastTheme {
        when {
            authState is AuthState.LoggedIn || (hasSeenOnboarding && authState !is AuthState.Loading) -> {
                NothingPodcastApp(
                    onSignOut = {
                        authViewModel.signOut()
                    },
                    onResetOnboarding = {
                        authViewModel.setHasSeenOnboarding(false)
                    }
                )
            }
            authState is AuthState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) { }
            }
            else -> {
                LoginScreen(
                    onContinueAsGuest = { authViewModel.setHasSeenOnboarding(true) },
                    viewModel         = authViewModel
                )
            }
        }
        
        // Also ensure that if they log in successfully, we mark onboarding as seen
        LaunchedEffect(authState) {
            if (authState is AuthState.LoggedIn) {
                authViewModel.setHasSeenOnboarding(true)
            }
        }
    }
}

@Composable
private fun NothingPodcastApp(
    onSignOut:         () -> Unit = {},
    onResetOnboarding: () -> Unit = {}
) {
    val context         = androidx.compose.ui.platform.LocalContext.current
    val navController   = rememberNavController()
    val playerViewModel = hiltViewModel<PlayerViewModel>()
    val playerUiState   by playerViewModel.uiState.collectAsStateWithLifecycle()

    // --- Deep Link Handling (from Widget) ---
    LaunchedEffect(Unit) {
        val activity = context as? android.app.Activity
        if (activity?.intent?.getBooleanExtra("OPEN_PLAYER", false) == true) {
            navController.navigate(Screen.Player.route) {
                launchSingleTop = true
            }
            // Clear the extra so it doesn't trigger again on config change
            activity.intent.removeExtra("OPEN_PLAYER")
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute      = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = NothingBlack,
        bottomBar = {
            // Hide mini player on Player screen or if nothing is playing
            if (playerUiState.currentEpisode != null && currentRoute != Screen.Player.route) {
                NothingMiniPlayer(
                    uiState           = playerUiState,
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    modifier          = Modifier.navigationBarsPadding(),
                    onClick           = {
                        navController.navigate(Screen.Player.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        ) {
            NothingNavGraph(
                navController     = navController,
                playerViewModel   = playerViewModel,
                onResetOnboarding = onResetOnboarding
            )
        }
    }
}