package com.hexaghost.flixora.presentation.search

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.ui.input.nestedscroll.nestedScroll

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

    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            browseViewModel.refreshData()
            pullToRefreshState.endRefresh()
        }
    }

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
                text = "Explore",
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(pullToRefreshState.nestedScrollConnection),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search History Section
                        if (searchUiState.searchHistory.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent Searches",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = FlixoraCyan
                                    )
                                    TextButton(onClick = { searchViewModel.clearAllHistory() }) {
                                        Text(
                                            text = "Clear All",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = FlixoraWhite60
                                        )
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    searchUiState.searchHistory.forEach { historyQuery ->
                                        Surface(
                                            color = FlixoraDarkCard,
                                            shape = RoundedCornerShape(18.dp),
                                            border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                                            modifier = Modifier.clickable {
                                                searchViewModel.onQueryChange(historyQuery)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = historyQuery,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = FlixoraWhite80
                                                )
                                                IconButton(
                                                    onClick = { searchViewModel.deleteHistoryItem(historyQuery) },
                                                    modifier = Modifier.size(16.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Clear,
                                                        contentDescription = "Remove",
                                                        tint = FlixoraWhite40,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

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

                    if (pullToRefreshState.verticalOffset > 0f || pullToRefreshState.isRefreshing) {
                        PullToRefreshContainer(
                            state = pullToRefreshState,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = FlixoraDarkSurface,
                            contentColor = FlixoraCyan
                        )
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
