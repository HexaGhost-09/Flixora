package com.hexaghost.flixora.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Browse : Screen("browse")
    data object Search : Screen("search")
    data object Watchlist : Screen("watchlist")
    data object Detail : Screen("detail/{mediaId}/{mediaType}") {
        fun createRoute(mediaId: Int, mediaType: String) = "detail/$mediaId/$mediaType"
    }
    data object Player : Screen("player/{trailerKey}") {
        fun createRoute(trailerKey: String) = "player/$trailerKey"
    }
}
