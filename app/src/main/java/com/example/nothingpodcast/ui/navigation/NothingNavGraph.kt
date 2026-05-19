package com.example.nothingpodcast.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.nothingpodcast.ui.player.PlayerViewModel
import com.example.nothingpodcast.ui.screens.downloads.DownloadsScreen
import com.example.nothingpodcast.ui.screens.home.HomeScreen
import com.example.nothingpodcast.ui.screens.player.PlayerScreen
import com.example.nothingpodcast.ui.screens.detail.PodcastDetailScreen
import com.example.nothingpodcast.ui.screens.settings.SettingsScreen

@Composable
fun NothingNavGraph(
    navController:     NavHostController,
    playerViewModel:   PlayerViewModel,
    onResetOnboarding: () -> Unit
) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onPodcastClick        = { podcastId ->
                    navController.navigate(Screen.PodcastDetail.createRoute(podcastId))
                },
                onNavigateToDownloads = {
                    navController.navigate(Screen.Downloads.route) { launchSingleTop = true }
                },
                onNavigateToSettings  = {
                    navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                },
                onResetOnboarding = onResetOnboarding,
                playerViewModel = playerViewModel
            )
        }

        composable(Screen.PodcastDetail.route) { backStack ->
            val podcastId = backStack.arguments?.getString("podcastId") ?: return@composable
            PodcastDetailScreen(
                podcastId       = podcastId,
                onBack          = { navController.popBackStack() },
                playerViewModel = playerViewModel,
                onNavigateToPlayer = {
                    navController.navigate(Screen.Player.route) { launchSingleTop = true }
                }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                playerViewModel = playerViewModel,
                onBack          = { navController.popBackStack() },
                onNavigateToPlayer = {
                    navController.navigate(Screen.Player.route) { launchSingleTop = true }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onResetOnboarding = onResetOnboarding
            )
        }

        composable(Screen.Player.route) {
            PlayerScreen(
                playerViewModel = playerViewModel,
                onDismiss       = { navController.popBackStack() }
            )
        }
    }
}
