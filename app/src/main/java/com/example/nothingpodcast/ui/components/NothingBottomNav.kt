package com.example.nothingpodcast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.nothingpodcast.ui.navigation.BottomNavItem
import com.example.nothingpodcast.ui.navigation.bottomNavItems
import com.example.nothingpodcast.ui.theme.*

/**
 * Nothing-style bottom navigation bar.
 * Sharp corners, thin top border, monospace uppercase labels.
 * Selected item is pure white; unselected is dim gray.
 */
@Composable
fun NothingBottomNav(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NothingBlack)
            .border(
                width = 1.dp,
                color = NothingBorder,
            )
            .navigationBarsPadding()
            .height(64.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        bottomNavItems.forEach { item ->
            NothingNavItem(
                item       = item,
                selected   = currentRoute == item.screen.route,
                onClick    = {
                    if (currentRoute != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NothingNavItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) NothingWhite else NothingOnSurfaceDim

    Column(
        modifier            = modifier
            .fillMaxHeight()
            .let { m ->
                if (selected) m.border(
                    width = 0.dp, color = NothingBorder
                ) else m
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector        = item.icon,
                contentDescription = item.label,
                tint               = tint,
                modifier           = Modifier.size(22.dp)
            )
        }
        Text(
            text  = item.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ),
            color = tint
        )
    }
}
