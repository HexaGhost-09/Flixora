package com.hexaghost.flixora.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexaghost.flixora.ui.components.MediaCard
import com.hexaghost.flixora.ui.components.MediaCardShimmer
import com.hexaghost.flixora.ui.theme.*

@Composable
fun BrowseScreen(
    onMediaClick: (Int, String) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlixoraDarkBg)
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
            Text(
                text = "Browse",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = FlixoraWhite
            )
        }

        // Movie / TV Tab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Movies", "TV Shows").forEachIndexed { index, label ->
                val isSelected = uiState.selectedTab == index
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectTab(index) },
                    label = {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FlixoraPurple,
                        selectedLabelColor = FlixoraWhite,
                        containerColor = FlixoraDarkCard,
                        labelColor = FlixoraWhite80
                    )
                )
            }
        }

        // Genre chips
        if (!uiState.isLoadingGenres) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.genres.forEach { genre ->
                    val isSelected = uiState.selectedGenre?.id == genre.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectGenre(genre) },
                        label = { Text(genre.name, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FlixoraCyan.copy(alpha = 0.2f),
                            selectedLabelColor = FlixoraCyan,
                            containerColor = FlixoraDarkCard,
                            labelColor = FlixoraWhite60
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Media grid
        if (uiState.isLoadingMedia) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(12) { MediaCardShimmer() }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = uiState.mediaList, key = { it.id }) { media ->
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
