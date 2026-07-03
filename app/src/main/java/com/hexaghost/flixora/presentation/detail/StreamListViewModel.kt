package com.hexaghost.flixora.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexaghost.flixora.domain.model.MediaDetail
import com.hexaghost.flixora.domain.model.StreamResult
import com.hexaghost.flixora.domain.repository.ProviderRepository
import com.hexaghost.flixora.domain.usecase.GetMediaDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StreamListUiState(
    val isLoadingDetail: Boolean = true,
    val isSearchingStreams: Boolean = false,
    val detail: MediaDetail? = null,
    val streamResults: List<StreamResult> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class StreamListViewModel @Inject constructor(
    private val getMediaDetailUseCase: GetMediaDetailUseCase,
    private val providerRepository: ProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreamListUiState())
    val uiState: StateFlow<StreamListUiState> = _uiState.asStateFlow()

    fun loadStreams(mediaId: Int, mediaType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true, isSearchingStreams = false, error = null, streamResults = emptyList()) }
            getMediaDetailUseCase(mediaId, mediaType)
                .onSuccess { detail ->
                    _uiState.update { it.copy(isLoadingDetail = false, detail = detail, isSearchingStreams = true) }
                    findStreams(detail)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingDetail = false, error = e.message ?: "Failed to load media details") }
                }
        }
    }

    private suspend fun findStreams(detail: MediaDetail) {
        val enabledProviders = providerRepository.getInstalledProviders().filter { it.isEnabled }
        if (enabledProviders.isEmpty()) {
            _uiState.update {
                it.copy(
                    isSearchingStreams = false,
                    error = "No providers enabled. Please enable a provider in the Providers tab."
                )
            }
            return
        }

        val year = detail.releaseDate?.take(4)?.toIntOrNull() ?: 0
        val allStreams = mutableListOf<StreamResult>()
        val isTv = detail.mediaType.equals("tv", ignoreCase = true)

        enabledProviders.forEach { provider ->
            providerRepository.resolveStreams(
                provider = provider,
                tmdbId = detail.id,
                title = detail.title,
                mediaType = detail.mediaType,
                year = year,
                season = if (isTv) 1 else 0,
                episode = if (isTv) 1 else 0
            ).onSuccess { streams ->
                allStreams.addAll(streams)
            }
        }

        _uiState.update {
            it.copy(
                isSearchingStreams = false,
                streamResults = allStreams,
                error = if (allStreams.isEmpty()) "No streams found by any provider." else null
            )
        }
    }
}
