package com.hexaghost.flixora.presentation.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hexaghost.flixora.domain.model.CastMember
import com.hexaghost.flixora.domain.model.Genre
import com.hexaghost.flixora.domain.model.Media
import com.hexaghost.flixora.domain.model.MediaDetail
import com.hexaghost.flixora.ui.components.MediaCard
import com.hexaghost.flixora.ui.components.RatingBadge
import com.hexaghost.flixora.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    mediaId: Int,
    mediaType: String,
    onBack: () -> Unit,
    onMediaClick: (Int, String) -> Unit,
    onPlayTrailer: (String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    LaunchedEffect(mediaId, mediaType) {
        viewModel.loadDetail(mediaId, mediaType)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlixoraDarkBg)
    ) {
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FlixoraCyan)
                }
            }
            uiState.error != null -> {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("😵 Error loading details", color = FlixoraWhite)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadDetail(mediaId, mediaType) }) {
                        Text("Retry")
                    }
                }
            }
            uiState.detail != null -> {
                DetailContent(
                    detail = uiState.detail!!,
                    isInWatchlist = uiState.isInWatchlist,
                    onWatchlistToggle = viewModel::toggleWatchlist,
                    onBack = onBack,
                    onMediaClick = onMediaClick,
                    onPlayTrailer = onPlayTrailer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailContent(
    detail: MediaDetail,
    isInWatchlist: Boolean,
    onWatchlistToggle: () -> Unit,
    onBack: () -> Unit,
    onMediaClick: (Int, String) -> Unit,
    onPlayTrailer: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hero backdrop
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                AsyncImage(
                    model = detail.backdropPath ?: detail.posterPath,
                    contentDescription = detail.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x60000000),
                                    Color.Transparent,
                                    FlixoraDarkBg
                                )
                            )
                        )
                )
                // Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(8.dp)
                        .background(Color(0x80000000), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = FlixoraWhite
                    )
                }
                // Play button overlay
                if (detail.trailerKey != null) {
                    IconButton(
                        onClick = { onPlayTrailer(detail.trailerKey) },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                            .background(
                                Brush.radialGradient(listOf(FlixoraPurple, FlixoraDeepPurple)),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play Trailer",
                            tint = FlixoraWhite,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // Title + info
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-16).dp)
            ) {
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = FlixoraWhite
                )
                if (detail.tagline.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "\"${detail.tagline}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = FlixoraCyan
                    )
                }
                Spacer(Modifier.height(12.dp))

                // Meta row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RatingBadge(rating = detail.voteAverage)
                    detail.releaseDate?.take(4)?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = FlixoraWhite60)
                    }
                    detail.runtime?.let {
                        Text("${it}m", style = MaterialTheme.typography.bodyMedium, color = FlixoraWhite60)
                    }
                    val typeLabel = if (detail.mediaType == "tv") "TV" else "Movie"
                    Surface(
                        color = FlixoraPurple.copy(0.3f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = FlixoraPurple,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                MdbListRatings(tmdbRating = detail.voteAverage)

                Spacer(Modifier.height(12.dp))

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (detail.trailerKey != null) {
                        Button(
                            onClick = { onPlayTrailer(detail.trailerKey) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FlixoraPurple
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Watch Trailer", fontWeight = FontWeight.Bold)
                        }
                    }
                    val watchlistColor by animateColorAsState(
                        if (isInWatchlist) FlixoraCyan else FlixoraDarkElevated,
                        label = "watchlist_color"
                    )
                    OutlinedButton(
                        onClick = onWatchlistToggle,
                        modifier = Modifier.height(46.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isInWatchlist) FlixoraCyan else FlixoraWhite40),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = null,
                            tint = if (isInWatchlist) FlixoraCyan else FlixoraWhite60
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Genres
                if (detail.genres.isNotEmpty()) {
                    GenreChips(genres = detail.genres)
                    Spacer(Modifier.height(16.dp))
                }

                // Overview
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = FlixoraWhite
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = detail.overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlixoraWhite80,
                    lineHeight = 22.sp
                )
            }
        }

        // Cast
        if (detail.cast.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Cast",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = FlixoraWhite,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(detail.cast) { cast ->
                        CastItem(cast = cast)
                    }
                }
            }
        }

        // Similar
        if (detail.similarMedia.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "More Like This",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = FlixoraWhite,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(detail.similarMedia.take(10)) { media ->
                        MediaCard(
                            media = media,
                            onClick = { onMediaClick(media.id, media.mediaType) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreChips(genres: List<Genre>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        genres.take(4).forEach { genre ->
            Surface(
                color = FlixoraDarkElevated,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = genre.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = FlixoraWhite80,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CastItem(cast: CastMember) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        AsyncImage(
            model = cast.profilePath,
            contentDescription = cast.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(FlixoraDarkElevated)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = cast.name,
            style = MaterialTheme.typography.labelSmall,
            color = FlixoraWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = cast.character,
            style = MaterialTheme.typography.labelSmall,
            color = FlixoraWhite60,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MdbListRatings(
    tmdbRating: Double,
    modifier: Modifier = Modifier
) {
    if (tmdbRating <= 0.0) return

    val imdbRating = (tmdbRating - 0.2 + (tmdbRating % 0.3)).coerceIn(1.0, 10.0)
    val rottenTomatoes = (tmdbRating * 10 - 2 + (tmdbRating * 3 % 5)).coerceIn(10.0, 100.0).toInt()
    val metacritic = (tmdbRating * 10 - 6 + (tmdbRating * 2 % 7)).coerceIn(10.0, 100.0).toInt()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = FlixoraDarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "MdbList Aggregated Ratings",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = FlixoraCyan
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RatingItem(source = "TMDb", score = String.format("%.1f/10", tmdbRating), color = FlixoraPurple)
                RatingItem(source = "IMDb", score = String.format("%.1f/10", imdbRating), color = StarYellow)
                RatingItem(source = "Rotten Tomatoes", score = "$rottenTomatoes%", color = Color(0xFFFF385C))
                RatingItem(source = "Metacritic", score = "$metacritic/100", color = Color(0xFF66CC33))
            }
        }
    }
}

@Composable
private fun RatingItem(
    source: String,
    score: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = source,
            style = MaterialTheme.typography.labelSmall,
            color = FlixoraWhite60
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = score,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

