package com.hexaghost.flixora.presentation.watchlist

import androidx.lifecycle.ViewModel
import com.hexaghost.flixora.domain.usecase.GetWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    getWatchlistUseCase: GetWatchlistUseCase
) : ViewModel() {
    val watchlist = getWatchlistUseCase()
}
