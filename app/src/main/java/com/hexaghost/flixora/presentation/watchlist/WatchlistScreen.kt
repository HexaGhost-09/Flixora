package com.hexaghost.flixora.presentation.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexaghost.flixora.ui.components.MediaCard
import com.hexaghost.flixora.ui.theme.*

@Composable
fun WatchlistScreen(
    onMediaClick: (Int, String) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlixoraDarkBg)
            .statusBarsPadding()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(FlixoraDarkSurface, FlixoraDarkBg)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Text(
                    text = "My Watchlist",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = FlixoraWhite
                )
                if (watchlist.isNotEmpty()) {
                    Text(
                        text = "${watchlist.size} ${if (watchlist.size == 1) "title" else "titles"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FlixoraWhite60
                    )
                }
            }
        }

        if (watchlist.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "🎭", style = MaterialTheme.typography.displayMedium)
                    Text(
                        text = "Your watchlist is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = FlixoraWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Save movies and shows to\nwatch them later",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FlixoraWhite60,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = watchlist, key = { it.id }) { media ->
                    MediaCard(
                        media = media,
                        onClick = { onMediaClick(media.id, media.mediaType) },
                        width = 110.dp,
                        height = 165.dp
                    )
                }
            }
        }
    }
}
