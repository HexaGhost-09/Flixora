package com.hexaghost.flixora.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexaghost.flixora.domain.model.Media
import com.hexaghost.flixora.domain.usecase.GetPopularMoviesUseCase
import com.hexaghost.flixora.domain.usecase.GetPopularTvUseCase
import com.hexaghost.flixora.domain.usecase.GetTrendingUseCase
import com.hexaghost.flixora.data.local.PreferencesManager
import com.hexaghost.flixora.data.local.ContinueWatchingItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val trending: List<Media> = emptyList(),
    val popularMovies: List<Media> = emptyList(),
    val popularTvShows: List<Media> = emptyList(),
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTrendingUseCase: GetTrendingUseCase,
    private val getPopularMoviesUseCase: GetPopularMoviesUseCase,
    private val getPopularTvUseCase: GetPopularTvUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private var cachedState: HomeUiState? = null
    }

    private val _uiState = MutableStateFlow(cachedState ?: HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        if (cachedState == null) {
            loadContent()
        }
    }

    fun loadContent() {
        viewModelScope.launch {
            // Only show loading if we don't have cached data already
            val isFirstLoad = _uiState.value.trending.isEmpty()
            _uiState.update { it.copy(isLoading = isFirstLoad, error = null) }
            try {
                val trendingDeferred = async { getTrendingUseCase() }
                val moviesDeferred = async { getPopularMoviesUseCase() }
                val tvDeferred = async { getPopularTvUseCase() }

                val trending = trendingDeferred.await().getOrDefault(emptyList())
                val movies = moviesDeferred.await().getOrDefault(emptyList())
                val tv = tvDeferred.await().getOrDefault(emptyList())

                val newState = HomeUiState(
                    isLoading = false,
                    trending = trending,
                    popularMovies = movies,
                    popularTvShows = tv,
                    continueWatching = preferencesManager.getContinueWatchingItems(),
                    error = null
                )
                _uiState.value = newState
                cachedState = newState
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        continueWatching = preferencesManager.getContinueWatchingItems(),
                        error = e.message ?: "Failed to load content"
                    )
                }
            }
        }
    }

    fun refreshContinueWatching() {
        _uiState.update {
            it.copy(continueWatching = preferencesManager.getContinueWatchingItems())
        }
    }
}
