package com.hexaghost.flixora.presentation.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexaghost.flixora.domain.model.Genre
import com.hexaghost.flixora.domain.model.Media
import com.hexaghost.flixora.domain.usecase.DiscoverByGenreUseCase
import com.hexaghost.flixora.domain.usecase.GetGenresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowseUiState(
    val isLoadingGenres: Boolean = true,
    val isLoadingMedia: Boolean = false,
    val genres: List<Genre> = emptyList(),
    val selectedGenre: Genre? = null,
    val selectedTab: Int = 0, // 0=Movies, 1=TV
    val mediaList: List<Media> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val getGenresUseCase: GetGenresUseCase,
    private val discoverByGenreUseCase: DiscoverByGenreUseCase
) : ViewModel() {

    companion object {
        private var cachedState: BrowseUiState? = null
    }

    private val _uiState = MutableStateFlow(cachedState ?: BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        if (cachedState == null) {
            loadGenres()
        }
    }

    private fun loadGenres() {
        viewModelScope.launch {
            getGenresUseCase.getAllGenres().onSuccess { genres ->
                _uiState.update {
                    it.copy(isLoadingGenres = false, genres = genres)
                }
                cachedState = _uiState.value
                genres.firstOrNull()?.let { selectGenre(it) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoadingGenres = false, error = e.message)
                }
                cachedState = _uiState.value
            }
        }
    }

    fun selectGenre(genre: Genre) {
        _uiState.update { it.copy(selectedGenre = genre, isLoadingMedia = true, mediaList = emptyList()) }
        cachedState = _uiState.value
        loadMediaForGenre(genre.id, _uiState.value.selectedTab)
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab, isLoadingMedia = true, mediaList = emptyList()) }
        cachedState = _uiState.value
        _uiState.value.selectedGenre?.let { genre ->
            loadMediaForGenre(genre.id, tab)
        }
    }

    private fun loadMediaForGenre(genreId: Int, tab: Int) {
        viewModelScope.launch {
            val result = if (tab == 0) {
                discoverByGenreUseCase.discoverMovies(genreId)
            } else {
                discoverByGenreUseCase.discoverTv(genreId)
            }
            result.onSuccess { media ->
                _uiState.update { it.copy(isLoadingMedia = false, mediaList = media) }
                cachedState = _uiState.value
            }.onFailure { e ->
                _uiState.update { it.copy(isLoadingMedia = false, error = e.message) }
                cachedState = _uiState.value
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            cachedState = _uiState.value

            val currentGenre = _uiState.value.selectedGenre
            val currentTab = _uiState.value.selectedTab

            getGenresUseCase.getAllGenres().onSuccess { genres ->
                _uiState.update { it.copy(isLoadingGenres = false, genres = genres) }
                
                val genreToLoad = genres.find { it.id == currentGenre?.id } ?: genres.firstOrNull()
                _uiState.update { it.copy(selectedGenre = genreToLoad) }
                cachedState = _uiState.value

                if (genreToLoad != null) {
                    val result = if (currentTab == 0) {
                        discoverByGenreUseCase.discoverMovies(genreToLoad.id)
                    } else {
                        discoverByGenreUseCase.discoverTv(genreToLoad.id)
                    }
                    result.onSuccess { media ->
                        _uiState.update { it.copy(isRefreshing = false, isLoadingMedia = false, mediaList = media) }
                        cachedState = _uiState.value
                    }.onFailure { e ->
                        _uiState.update { it.copy(isRefreshing = false, isLoadingMedia = false, error = e.message) }
                        cachedState = _uiState.value
                    }
                } else {
                    _uiState.update { it.copy(isRefreshing = false, isLoadingMedia = false) }
                    cachedState = _uiState.value
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isRefreshing = false, isLoadingGenres = false, isLoadingMedia = false, error = e.message)
                }
                cachedState = _uiState.value
            }
        }
    }
}
