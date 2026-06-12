package com.hexaghost.flixora.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hexaghost.flixora.presentation.settings.SettingsScreen
import com.hexaghost.flixora.presentation.detail.DetailScreen
import com.hexaghost.flixora.presentation.home.HomeScreen
import com.hexaghost.flixora.presentation.player.PlayerScreen
import com.hexaghost.flixora.presentation.search.SearchScreen
import com.hexaghost.flixora.presentation.splash.SplashScreen
import com.hexaghost.flixora.presentation.watchlist.WatchlistScreen
import com.hexaghost.flixora.presentation.update.UpdateDialog
import com.hexaghost.flixora.domain.update.UpdateManager
import com.hexaghost.flixora.domain.update.UpdateState
import com.hexaghost.flixora.ui.theme.*

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Screen.Home.route, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("Explore", Screen.Search.route, Icons.Filled.Explore, Icons.Outlined.Explore),
    BottomNavItem("Watchlist", Screen.Watchlist.route, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    BottomNavItem("Settings", Screen.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun FlixoraNavigation(
    updateManager: UpdateManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    // Observe update state globally
    val updateState by updateManager.updateState.collectAsState()

    if (updateState is UpdateState.UpdateAvailable) {
        val state = updateState as UpdateState.UpdateAvailable
        UpdateDialog(
            currentVersion = state.currentVersion,
            latestVersion = state.latestVersion,
            downloadUrl = state.downloadUrl,
            releaseNotes = state.releaseNotes,
            onDismiss = { updateManager.resetState() }
        )
    }

    Scaffold(
        containerColor = FlixoraDarkBg,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                FlixoraBottomBar(
                    currentRoute = currentRoute,
                    onNavItemClick = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 300 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -300 },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -300 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 300 },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onMediaClick = { id, type ->
                        navController.navigate(Screen.Detail.createRoute(id, type))
                    },
                    onSeeAllMovies = { navController.navigate(Screen.Search.route) },
                    onSeeAllTv = { navController.navigate(Screen.Search.route) }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onMediaClick = { id, type ->
                        navController.navigate(Screen.Detail.createRoute(id, type))
                    }
                )
            }

            composable(Screen.Watchlist.route) {
                WatchlistScreen(
                    onMediaClick = { id, type ->
                        navController.navigate(Screen.Detail.createRoute(id, type))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(updateManager = updateManager)
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.IntType },
                    navArgument("mediaType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getInt("mediaId") ?: return@composable
                val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                DetailScreen(
                    mediaId = mediaId,
                    mediaType = mediaType,
                    onBack = { navController.popBackStack() },
                    onMediaClick = { id, type ->
                        navController.navigate(Screen.Detail.createRoute(id, type))
                    },
                    onPlayTrailer = { key ->
                        navController.navigate(Screen.Player.createRoute(key))
                    }
                )
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("trailerKey") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val trailerKey = backStackEntry.arguments?.getString("trailerKey") ?: return@composable
                PlayerScreen(
                    trailerKey = trailerKey,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun FlixoraBottomBar(
    currentRoute: String?,
    onNavItemClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = FlixoraDarkSurface.copy(alpha = 0.85f),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF)),
            tonalElevation = 8.dp
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                contentColor = FlixoraWhite,
                modifier = Modifier.height(72.dp)
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        selected = isSelected,
                        onClick = { onNavItemClick(item.route) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FlixoraCyan,
                            selectedTextColor = FlixoraCyan,
                            unselectedIconColor = FlixoraWhite60,
                            unselectedTextColor = FlixoraWhite60,
                            indicatorColor = FlixoraCyan.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    }
}
