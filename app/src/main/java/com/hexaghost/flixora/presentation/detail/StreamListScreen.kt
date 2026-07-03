package com.hexaghost.flixora.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hexaghost.flixora.data.local.ContinueWatchingItem
import com.hexaghost.flixora.domain.model.StreamResult
import com.hexaghost.flixora.ui.theme.*
import com.hexaghost.flixora.data.local.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamListScreen(
    mediaId: Int,
    mediaType: String,
    preferencesManager: PreferencesManager,
    onBack: () -> Unit,
    onPlayStream: (StreamResult, String, Int, String) -> Unit,
    viewModel: StreamListViewModel = hiltViewModel()
) {
    LaunchedEffect(mediaId, mediaType) {
        viewModel.loadStreams(mediaId, mediaType)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlixoraDarkBg)
    ) {
        // Blurred backdrop image background
        uiState.detail?.backdropPath?.let { backdrop ->
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w500$backdrop",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
                    .align(Alignment.Center),
                alpha = 0.15f
            )
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.detail?.title ?: "Resolve Streams",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = FlixoraWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.detail != null) {
                                Text(
                                    text = if (uiState.detail?.mediaType == "tv") "TV Show" else "Movie",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FlixoraWhite60
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = FlixoraWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoadingDetail -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = FlixoraCyan)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Loading details...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FlixoraWhite60
                            )
                        }
                    }
                    uiState.isSearchingStreams -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(
                                color = FlixoraCyan,
                                modifier = Modifier.size(56.dp),
                                strokeWidth = 4.dp
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "Searching Providers...",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = FlixoraWhite
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Fetching links for: ${uiState.detail?.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FlixoraWhite60,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    uiState.error != null && uiState.streamResults.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = FlixoraWhite40,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = uiState.error ?: "An error occurred",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FlixoraWhite80,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.loadStreams(mediaId, mediaType) },
                                colors = ButtonDefaults.buttonColors(containerColor = FlixoraPurple),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Retry Search")
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            // Media info card at top
                            uiState.detail?.let { detail ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(FlixoraDarkSurface.copy(0.7f))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = "https://image.tmdb.org/t/p/w200${detail.posterPath}",
                                        contentDescription = null,
                                        modifier = Modifier
                                            .width(50.dp)
                                            .height(75.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = detail.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = FlixoraWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (detail.mediaType == "tv") Icons.Filled.Tv else Icons.Filled.Videocam,
                                                contentDescription = null,
                                                tint = FlixoraCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (detail.mediaType == "tv") "TV Show" else "Movie",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = FlixoraWhite60
                                            )
                                            detail.releaseDate?.take(4)?.let { year ->
                                                Text(
                                                    text = "•  $year",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = FlixoraWhite60
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                            }

                            Text(
                                text = "${uiState.streamResults.size} stream provider links found",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = FlixoraWhite60,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.streamResults) { stream ->
                                    StreamResultCard(
                                        stream = stream,
                                        onClick = {
                                            val detail = uiState.detail
                                            if (detail != null) {
                                                preferencesManager.saveContinueWatchingItem(
                                                    ContinueWatchingItem(
                                                        id = detail.id,
                                                        title = detail.title,
                                                        mediaType = detail.mediaType,
                                                        posterPath = detail.posterPath,
                                                        backdropPath = detail.backdropPath,
                                                        voteAverage = detail.voteAverage,
                                                        progress = 0.05f
                                                    )
                                                )
                                            }
                                            onPlayStream(stream, uiState.detail?.title ?: "", mediaId, mediaType)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamResultCard(
    stream: StreamResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FlixoraDarkSurface.copy(0.7f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play Circle Icon / Quality Badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    Brush.linearGradient(listOf(FlixoraCyan, FlixoraPurple)),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stream.providerName.ifBlank { "Unknown Provider" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = FlixoraWhite
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stream.url.substringAfter("://").take(35) + if (stream.url.length > 35) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = FlixoraWhite40,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        // Quality indicator
        Surface(
            color = when {
                stream.quality.contains("4K", ignoreCase = true) -> Color(0xFFFFD700).copy(0.12f)
                stream.quality.contains("1080", ignoreCase = true) -> FlixoraCyan.copy(0.12f)
                stream.quality.contains("720", ignoreCase = true) -> FlixoraPurple.copy(0.12f)
                else -> Color.White.copy(0.08f)
            },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = stream.quality,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = when {
                    stream.quality.contains("4K", ignoreCase = true) -> Color(0xFFFFD700)
                    stream.quality.contains("1080", ignoreCase = true) -> FlixoraCyan
                    stream.quality.contains("720", ignoreCase = true) -> FlixoraPurple
                    else -> FlixoraWhite80
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = FlixoraCyan,
            modifier = Modifier.size(20.dp)
        )
    }
}
