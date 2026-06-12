package com.hexaghost.flixora.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexaghost.flixora.domain.model.Media
import com.hexaghost.flixora.domain.usecase.GetPopularMoviesUseCase
import com.hexaghost.flixora.domain.usecase.GetPopularTvUseCase
import com.hexaghost.flixora.domain.usecase.GetTrendingUseCase
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
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTrendingUseCase: GetTrendingUseCase,
    private val getPopularMoviesUseCase: GetPopularMoviesUseCase,
    private val getPopularTvUseCase: GetPopularTvUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val trendingDeferred = async { getTrendingUseCase() }
                val moviesDeferred = async { getPopularMoviesUseCase() }
                val tvDeferred = async { getPopularTvUseCase() }

                val trending = trendingDeferred.await().getOrDefault(emptyList())
                val movies = moviesDeferred.await().getOrDefault(emptyList())
                val tv = tvDeferred.await().getOrDefault(emptyList())

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        trending = trending,
                        popularMovies = movies,
                        popularTvShows = tv
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load content")
                }
            }
        }
    }
}
