package com.hexaghost.flixora.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexaghost.flixora.domain.model.MediaDetail
import com.hexaghost.flixora.domain.model.StreamResult
import com.hexaghost.flixora.domain.repository.ProviderRepository
import com.hexaghost.flixora.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val detail: MediaDetail? = null,
    val isInWatchlist: Boolean = false,
    val error: String? = null,
    // Provider stream resolution
    val isFindingStreams: Boolean = false,
    val streamResults: List<StreamResult> = emptyList(),
    val streamError: String? = null,
    val showStreamPicker: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getMediaDetailUseCase: GetMediaDetailUseCase,
    private val addToWatchlistUseCase: AddToWatchlistUseCase,
    private val removeFromWatchlistUseCase: RemoveFromWatchlistUseCase,
    private val isInWatchlistUseCase: IsInWatchlistUseCase,
    private val providerRepository: ProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _isScrobbling = MutableStateFlow(false)
    val isScrobbling: StateFlow<Boolean> = _isScrobbling.asStateFlow()

    private val _hasScrobbled = MutableStateFlow(false)
    val hasScrobbled: StateFlow<Boolean> = _hasScrobbled.asStateFlow()

    fun loadDetail(mediaId: Int, mediaType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getMediaDetailUseCase(mediaId, mediaType)
                .onSuccess { detail ->
                    _uiState.update { it.copy(isLoading = false, detail = detail) }
                    observeWatchlistStatus(mediaId)
                    _hasScrobbled.value = false // reset for new content
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

    fun scrobbleMedia(title: String) {
        viewModelScope.launch {
            _isScrobbling.value = true
            kotlinx.coroutines.delay(2000)
            _isScrobbling.value = false
            _hasScrobbled.value = true
        }
    }

    // ── Stream Resolution ─────────────────────────────────────────────────────

    fun findStreams() {
        val detail = _uiState.value.detail ?: return
        val enabledProviders = providerRepository.getInstalledProviders().filter { it.isEnabled }
        if (enabledProviders.isEmpty()) {
            _uiState.update {
                it.copy(
                    streamError = "No providers enabled. Please enable a provider in the Providers tab."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isFindingStreams = true, streamError = null, streamResults = emptyList()) }

            val year = detail.releaseDate?.take(4)?.toIntOrNull() ?: 0
            val allStreams = mutableListOf<StreamResult>()

            enabledProviders.forEach { provider ->
                providerRepository.resolveStreams(
                    provider = provider,
                    tmdbId = detail.id,
                    title = detail.title,
                    mediaType = detail.mediaType,
                    year = year
                ).onSuccess { streams ->
                    allStreams.addAll(streams)
                }
            }

            if (allStreams.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isFindingStreams = false,
                        streamError = "No streams found by any provider.",
                        showStreamPicker = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isFindingStreams = false,
                        streamResults = allStreams,
                        showStreamPicker = true
                    )
                }
            }
        }
    }

    fun dismissStreamPicker() {
        _uiState.update { it.copy(showStreamPicker = false) }
    }

    fun clearStreamError() {
        _uiState.update { it.copy(streamError = null) }
    }

    fun hasEnabledProviders(): Boolean =
        providerRepository.getInstalledProviders().any { it.isEnabled }
}
