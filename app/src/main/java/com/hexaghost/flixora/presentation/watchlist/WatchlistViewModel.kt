package com.hexaghost.flixora.presentation.watchlist

import androidx.lifecycle.ViewModel
import com.hexaghost.flixora.domain.usecase.GetWatchlistUseCase
import com.hexaghost.flixora.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    getWatchlistUseCase: GetWatchlistUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    val watchlist = getWatchlistUseCase()
    val isTraktConnected get() = preferencesManager.isTraktConnected
    val traktUsername get() = preferencesManager.traktUsername
}
