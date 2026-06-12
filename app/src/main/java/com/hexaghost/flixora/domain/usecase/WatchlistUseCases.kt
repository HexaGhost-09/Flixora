package com.hexaghost.flixora.domain.usecase

import com.hexaghost.flixora.domain.model.Media
import com.hexaghost.flixora.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository
) {
    operator fun invoke(): Flow<List<Media>> = repository.getWatchlist()
}

class AddToWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository
) {
    suspend operator fun invoke(media: Media) = repository.addToWatchlist(media)
}

class RemoveFromWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository
) {
    suspend operator fun invoke(mediaId: Int) = repository.removeFromWatchlist(mediaId)
}

class IsInWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository
) {
    operator fun invoke(mediaId: Int): Flow<Boolean> = repository.isInWatchlist(mediaId)
}
