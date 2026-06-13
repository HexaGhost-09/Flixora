package com.hexaghost.flixora.presentation.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexaghost.flixora.domain.model.InstalledProvider
import com.hexaghost.flixora.domain.model.ProviderInfo
import com.hexaghost.flixora.domain.model.Repository
import com.hexaghost.flixora.domain.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProvidersUiState(
    val installedProviders: List<InstalledProvider> = emptyList(),
    val savedRepositoryUrls: List<String> = emptyList(),
    val fetchedRepositories: Map<String, Repository> = emptyMap(),
    val isLoadingRepo: Boolean = false,
    val isInstallingId: String? = null,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProviderViewModel @Inject constructor(
    private val providerRepository: ProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProvidersUiState())
    val uiState: StateFlow<ProvidersUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                installedProviders = providerRepository.getInstalledProviders(),
                savedRepositoryUrls = providerRepository.getSavedRepositoryUrls()
            )
        }
    }

    fun addRepository(url: String) {
        val cleanUrl = url.trim().trimEnd('/')
        if (cleanUrl.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRepo = true, error = null) }
            providerRepository.saveRepositoryUrl(cleanUrl)
            providerRepository.fetchManifest(cleanUrl)
                .onSuccess { repo ->
                    _uiState.update { state ->
                        state.copy(
                            isLoadingRepo = false,
                            savedRepositoryUrls = providerRepository.getSavedRepositoryUrls(),
                            fetchedRepositories = state.fetchedRepositories + (cleanUrl to repo),
                            successMessage = "Added repository: ${repo.name}"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoadingRepo = false, error = "Failed to fetch manifest: ${e.message}")
                    }
                }
        }
    }

    fun loadRepository(url: String) {
        if (url in (_uiState.value.fetchedRepositories)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRepo = true, error = null) }
            providerRepository.fetchManifest(url)
                .onSuccess { repo ->
                    _uiState.update { state ->
                        state.copy(
                            isLoadingRepo = false,
                            fetchedRepositories = state.fetchedRepositories + (url to repo)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoadingRepo = false, error = "Failed to load: ${e.message}")
                    }
                }
        }
    }

    fun loadAllRepositories() {
        _uiState.value.savedRepositoryUrls.forEach { url ->
            if (url !in _uiState.value.fetchedRepositories) loadRepository(url)
        }
    }

    fun removeRepository(url: String) {
        providerRepository.removeRepositoryUrl(url)
        _uiState.update { state ->
            state.copy(
                savedRepositoryUrls = providerRepository.getSavedRepositoryUrls(),
                fetchedRepositories = state.fetchedRepositories - url
            )
        }
    }

    fun installProvider(provider: ProviderInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInstallingId = provider.id, error = null) }
            providerRepository.installProvider(provider)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isInstallingId = null,
                            installedProviders = providerRepository.getInstalledProviders(),
                            successMessage = "Installed ${provider.name}"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isInstallingId = null, error = "Install failed: ${e.message}")
                    }
                }
        }
    }

    fun uninstallProvider(id: String) {
        viewModelScope.launch {
            providerRepository.uninstallProvider(id)
            _uiState.update {
                it.copy(installedProviders = providerRepository.getInstalledProviders())
            }
        }
    }

    fun toggleProvider(id: String, enabled: Boolean) {
        providerRepository.toggleProvider(id, enabled)
        _uiState.update {
            it.copy(installedProviders = providerRepository.getInstalledProviders())
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
