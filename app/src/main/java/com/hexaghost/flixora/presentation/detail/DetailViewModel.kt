package com.hexaghost.flixora.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexaghost.flixora.domain.model.MediaDetail
import com.hexaghost.flixora.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val detail: MediaDetail? = null,
    val isInWatchlist: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getMediaDetailUseCase: GetMediaDetailUseCase,
    private val addToWatchlistUseCase: AddToWatchlistUseCase,
    private val removeFromWatchlistUseCase: RemoveFromWatchlistUseCase,
    private val isInWatchlistUseCase: IsInWatchlistUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadDetail(mediaId: Int, mediaType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getMediaDetailUseCase(mediaId, mediaType)
                .onSuccess { detail ->
                    _uiState.update { it.copy(isLoading = false, detail = detail) }
                    observeWatchlistStatus(mediaId)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun observeWatchlistStatus(mediaId: Int) {
        viewModelScope.launch {
            isInWatchlistUseCase(mediaId).collect { isIn ->
                _uiState.update { it.copy(isInWatchlist = isIn) }
            }
        }
    }

    fun toggleWatchlist() {
        val detail = _uiState.value.detail ?: return
        val isIn = _uiState.value.isInWatchlist
        viewModelScope.launch {
            if (isIn) {
                removeFromWatchlistUseCase(detail.id)
            } else {
                addToWatchlistUseCase(
                    com.hexaghost.flixora.domain.model.Media(
                        id = detail.id,
                        title = detail.title,
                        overview = detail.overview,
                        posterPath = detail.posterPath,
                        backdropPath = detail.backdropPath,
                        voteAverage = detail.voteAverage,
                        releaseDate = detail.releaseDate,
                        mediaType = detail.mediaType
                    )
                )
            }
        }
    }
}
