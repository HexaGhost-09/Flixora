package com.hexaghost.flixora.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexaghost.flixora.data.local.PreferencesManager
import com.hexaghost.flixora.domain.model.Media
import com.hexaghost.flixora.domain.usecase.SearchMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<Media> = emptyList(),
    val error: String? = null,
    val hasSearched: Boolean = false,
    val searchHistory: List<String> = emptyList()
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMediaUseCase: SearchMediaUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        loadSearchHistory()
        viewModelScope.launch {
            queryFlow
                .debounce(400)
                .filter { it.isNotBlank() }
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun loadSearchHistory() {
        _uiState.update { it.copy(searchHistory = preferencesManager.searchHistory) }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), hasSearched = false, isLoading = false) }
            loadSearchHistory()
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            preferencesManager.addSearchQuery(query)
            loadSearchHistory()
            _uiState.update { it.copy(isLoading = true, error = null) }
            searchMediaUseCase(query)
                .onSuccess { results ->
                    _uiState.update {
                        it.copy(isLoading = false, results = results, hasSearched = true)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message, hasSearched = true)
                    }
                }
        }
    }

    fun deleteHistoryItem(item: String) {
        preferencesManager.deleteSearchHistoryItem(item)
        loadSearchHistory()
    }

    fun clearAllHistory() {
        preferencesManager.clearSearchHistory()
        loadSearchHistory()
    }
}
