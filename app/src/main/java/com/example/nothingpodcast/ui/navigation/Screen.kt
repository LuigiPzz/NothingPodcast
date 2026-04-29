package com.example.nothingpodcast.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Bottom nav destinations
    data object Home      : Screen("home")
    data object Downloads : Screen("downloads")
    data object Settings  : Screen("settings")

    // Detail screens (not in bottom nav)
    data object PodcastDetail : Screen("podcast_detail/{podcastId}") {
        fun createRoute(podcastId: String) = "podcast_detail/$podcastId"
    }
    data object Player : Screen("player")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,      "HOME",      Icons.Outlined.Home),
    BottomNavItem(Screen.Downloads, "DOWNLOADS", Icons.Outlined.Download),
    BottomNavItem(Screen.Settings,  "SETTINGS",  Icons.Outlined.Settings),
)
