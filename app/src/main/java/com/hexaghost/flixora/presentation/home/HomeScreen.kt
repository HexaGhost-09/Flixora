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
import com.hexaghost.flixora.ui.theme.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle

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

    LaunchedEffect(Unit) {
        viewModel.refreshContinueWatching()
    }

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

                        // Continue Watching (Trakt Sync / Watch Progress)
                        if (uiState.continueWatching.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                SectionHeader(
                                    title = "⏳ Continue Watching",
                                    onSeeAllClick = {}
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(uiState.continueWatching) { item ->
                                        ContinueWatchingCard(
                                            item = item,
                                            onClick = { onMediaClick(item.id, item.mediaType) }
                                        )
                                    }
                                }
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

                    if (pullToRefreshState.verticalOffset > 0f || pullToRefreshState.isRefreshing) {
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

@Composable
fun ContinueWatchingCard(
    item: com.hexaghost.flixora.data.local.ContinueWatchingItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FlixoraDarkSurface),
        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            coil.compose.AsyncImage(
                model = item.backdropPath ?: item.posterPath,
                contentDescription = item.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xE0000000)
                            )
                        )
                    )
            )
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = null,
                tint = FlixoraCyan.copy(alpha = 0.9f),
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = item.title,
                    color = FlixoraWhite,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = FlixoraCyan,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
            }
        }
    }
}
