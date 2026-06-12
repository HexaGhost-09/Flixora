package com.hexaghost.flixora.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexaghost.flixora.domain.model.Media
import com.hexaghost.flixora.ui.components.*
import com.hexaghost.flixora.ui.theme.FlixoraDarkBg
import com.hexaghost.flixora.ui.theme.FlixoraDarkSurface
import com.hexaghost.flixora.ui.theme.FlixoraCyan
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMediaClick: (Int, String) -> Unit,
    onSeeAllMovies: () -> Unit,
    onSeeAllTv: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.loadContent()
        }
    }

    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            pullToRefreshState.startRefresh()
        } else {
            pullToRefreshState.endRefresh()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlixoraDarkBg)
    ) {
        when {
            uiState.isLoading && uiState.trending.isEmpty() -> FullScreenLoadingShimmer()
            uiState.error != null -> ErrorView(
                message = uiState.error!!,
                onRetry = viewModel::loadContent
            )
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(pullToRefreshState.nestedScrollConnection)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // Hero Banner
                        item {
                            val heroItems = uiState.trending.take(8)
                            if (heroItems.isNotEmpty()) {
                                HeroBanner(
                                    mediaList = heroItems,
                                    onMediaClick = { media ->
                                        onMediaClick(media.id, media.mediaType)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(480.dp)
                                    )
                            }
                        }

                        // Trending Now
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeader(
                                title = "🔥 Trending Now",
                                onSeeAllClick = onSeeAllMovies
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.trending.drop(8).take(15),
                                    key = { it.id }
                                ) { media ->
                                    MediaCard(
                                        media = media,
                                        onClick = { onMediaClick(media.id, media.mediaType) }
                                    )
                                }
                            }
                        }

                        // Popular Movies
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            SectionHeader(
                                title = "🎬 Popular Movies",
                                onSeeAllClick = onSeeAllMovies
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.popularMovies.take(20),
                                    key = { it.id }
                                ) { media ->
                                    MediaCard(
                                        media = media,
                                        onClick = { onMediaClick(media.id, media.mediaType) }
                                    )
                                }
                            }
                        }

                        // Popular TV Shows
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            SectionHeader(
                                title = "📺 Popular TV Shows",
                                onSeeAllClick = onSeeAllTv
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.popularTvShows.take(20),
                                    key = { it.id }
                                ) { media ->
                                    MediaCard(
                                        media = media,
                                        onClick = { onMediaClick(media.id, media.mediaType) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    PullToRefreshContainer(
                        state = pullToRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = FlixoraDarkSurface,
                        contentColor = FlixoraCyan
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "😵 Oops!",
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
