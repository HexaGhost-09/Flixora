package com.hexaghost.flixora.presentation.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexaghost.flixora.presentation.browse.BrowseViewModel
import com.hexaghost.flixora.ui.components.MediaCard
import com.hexaghost.flixora.ui.components.MediaCardShimmer
import com.hexaghost.flixora.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMediaClick: (Int, String) -> Unit,
    searchViewModel: SearchViewModel = hiltViewModel(),
    browseViewModel: BrowseViewModel = hiltViewModel()
) {
    val searchUiState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val browseUiState by browseViewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlixoraDarkBg)
            .statusBarsPadding()
    ) {
        // Top bar
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
                text = "Search & Explore",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = FlixoraWhite
            )
        }

        // Search field
        OutlinedTextField(
            value = searchUiState.query,
            onValueChange = searchViewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focusRequester),
            placeholder = { Text("Search movies, TV shows...", color = FlixoraWhite60) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = FlixoraCyan)
            },
            trailingIcon = {
                if (searchUiState.query.isNotBlank()) {
                    IconButton(onClick = { searchViewModel.onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = FlixoraWhite60)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FlixoraCyan,
                unfocusedBorderColor = FlixoraWhite40,
                focusedTextColor = FlixoraWhite,
                unfocusedTextColor = FlixoraWhite,
                cursorColor = FlixoraCyan,
                focusedContainerColor = FlixoraDarkCard,
                unfocusedContainerColor = FlixoraDarkCard
            ),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (searchUiState.query.isBlank()) {
                // Combined BROWSE UX (Categories / Discovered grid)
                Column(modifier = Modifier.fillMaxSize()) {
                    // Movie / TV Tab
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Movies", "TV Shows").forEachIndexed { index, label ->
                            val isSelected = browseUiState.selectedTab == index
                            FilterChip(
                                selected = isSelected,
                                onClick = { browseViewModel.selectTab(index) },
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
                    if (!browseUiState.isLoadingGenres) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            browseUiState.genres.forEach { genre ->
                                val isSelected = browseUiState.selectedGenre?.id == genre.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { browseViewModel.selectGenre(genre) },
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

                    // Media Grid for the selected category/genre
                    if (browseUiState.isLoadingMedia) {
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
                            items(items = browseUiState.mediaList, key = { it.id }) { media ->
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
            } else {
                // SEARCH RESULTS UX
                when {
                    searchUiState.isLoading -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(9) { MediaCardShimmer() }
                        }
                    }
                    searchUiState.results.isEmpty() && searchUiState.hasSearched -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🔍", style = MaterialTheme.typography.displayMedium)
                            Text(
                                text = "No results for \"${searchUiState.query}\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = FlixoraWhite60,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items = searchUiState.results, key = { it.id }) { media ->
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
        }
    }
}
