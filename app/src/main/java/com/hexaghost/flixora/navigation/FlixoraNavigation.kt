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
import com.hexaghost.flixora.data.local.PreferencesManager
import com.hexaghost.flixora.domain.model.StreamResult
import com.hexaghost.flixora.presentation.settings.SettingsScreen
import com.hexaghost.flixora.presentation.detail.DetailScreen
import com.hexaghost.flixora.presentation.home.HomeScreen
import com.hexaghost.flixora.presentation.providers.ProvidersScreen
import com.hexaghost.flixora.presentation.player.StreamPlayerScreen
import com.hexaghost.flixora.presentation.search.SearchScreen
import com.hexaghost.flixora.presentation.splash.SplashScreen
import com.hexaghost.flixora.presentation.watchlist.WatchlistScreen
import com.hexaghost.flixora.presentation.update.UpdateDialog
import com.hexaghost.flixora.domain.update.UpdateManager
import com.hexaghost.flixora.domain.update.UpdateState
import com.hexaghost.flixora.ui.theme.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
    BottomNavItem("Providers", Screen.Providers.route, Icons.Filled.Extension, Icons.Outlined.Extension),
    BottomNavItem("Settings", Screen.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun FlixoraNavigation(
    updateManager: UpdateManager,
    preferencesManager: PreferencesManager
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
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) }
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

            composable(Screen.Providers.route) {
                ProvidersScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    updateManager = updateManager,
                    preferencesManager = preferencesManager
                )
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
                    preferencesManager = preferencesManager,
                    onBack = { navController.popBackStack() },
                    onMediaClick = { id, type ->
                        navController.navigate(Screen.Detail.createRoute(id, type))
                    },
                    onPlayStream = { stream, title ->
                        val encodedUrl = URLEncoder.encode(stream.url, StandardCharsets.UTF_8.toString())
                        val encodedQuality = URLEncoder.encode(stream.quality, StandardCharsets.UTF_8.toString())
                        val encodedProvider = URLEncoder.encode(stream.providerName, StandardCharsets.UTF_8.toString())
                        val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
                        navController.navigate(
                            Screen.StreamPlayer.createRoute(encodedUrl, encodedQuality, encodedProvider, encodedTitle)
                        )
                    }
                )
            }

            composable(
                route = Screen.StreamPlayer.route,
                arguments = listOf(
                    navArgument("encodedUrl") { type = NavType.StringType },
                    navArgument("quality") { type = NavType.StringType },
                    navArgument("providerName") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("encodedUrl") ?: return@composable
                val quality = backStackEntry.arguments?.getString("quality") ?: "Auto"
                val providerName = backStackEntry.arguments?.getString("providerName") ?: ""
                val title = backStackEntry.arguments?.getString("title") ?: ""
                val decodedUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                val decodedQuality = URLDecoder.decode(quality, StandardCharsets.UTF_8.toString())
                val decodedProvider = URLDecoder.decode(providerName, StandardCharsets.UTF_8.toString())
                val decodedTitle = URLDecoder.decode(title, StandardCharsets.UTF_8.toString())

                StreamPlayerScreen(
                    stream = StreamResult(
                        url = decodedUrl,
                        quality = decodedQuality,
                        providerName = decodedProvider
                    ),
                    mediaTitle = decodedTitle,
                    preferencesManager = preferencesManager,
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall.copy(
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
