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
import com.hexaghost.flixora.presentation.browse.BrowseScreen
import com.hexaghost.flixora.presentation.detail.DetailScreen
import com.hexaghost.flixora.presentation.home.HomeScreen
import com.hexaghost.flixora.presentation.player.PlayerScreen
import com.hexaghost.flixora.presentation.search.SearchScreen
import com.hexaghost.flixora.presentation.splash.SplashScreen
import com.hexaghost.flixora.presentation.watchlist.WatchlistScreen
import com.hexaghost.flixora.ui.theme.*

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Screen.Home.route, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("Browse", Screen.Browse.route, Icons.Filled.Explore, Icons.Outlined.Explore),
    BottomNavItem("Search", Screen.Search.route, Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem("Watchlist", Screen.Watchlist.route, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder)
)

@Composable
fun FlixoraNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

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
            modifier = Modifier.padding(innerPadding),
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
                    onSeeAllMovies = { navController.navigate(Screen.Browse.route) },
                    onSeeAllTv = { navController.navigate(Screen.Browse.route) }
                )
            }

            composable(Screen.Browse.route) {
                BrowseScreen(
                    onMediaClick = { id, type ->
                        navController.navigate(Screen.Detail.createRoute(id, type))
                    }
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
    Surface(
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            FlixoraDarkSurface.copy(alpha = 0.95f),
                            FlixoraDarkSurface
                        )
                    )
                )
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                contentColor = FlixoraWhite
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = isSelected,
                        onClick = { onNavItemClick(item.route) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FlixoraCyan,
                            selectedTextColor = FlixoraCyan,
                            unselectedIconColor = FlixoraWhite60,
                            unselectedTextColor = FlixoraWhite60,
                            indicatorColor = FlixoraPurple.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }
}
