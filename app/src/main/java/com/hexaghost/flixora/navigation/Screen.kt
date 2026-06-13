package com.hexaghost.flixora.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object Search : Screen("search")
    data object Watchlist : Screen("watchlist")
    data object Providers : Screen("providers")
    data object Detail : Screen("detail/{mediaId}/{mediaType}") {
        fun createRoute(mediaId: Int, mediaType: String) = "detail/$mediaId/$mediaType"
    }
    data object StreamPlayer : Screen("stream_player/{encodedUrl}/{quality}/{providerName}/{title}") {
        fun createRoute(
            encodedUrl: String,
            quality: String,
            providerName: String,
            title: String
        ) = "stream_player/${encodedUrl}/${quality}/${providerName}/${title}"
    }
}
