package com.hexaghost.flixora.domain.repository

import com.hexaghost.flixora.domain.model.Media
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    fun getWatchlist(): Flow<List<Media>>
    suspend fun addToWatchlist(media: Media)
    suspend fun removeFromWatchlist(mediaId: Int)
    fun isInWatchlist(mediaId: Int): Flow<Boolean>
}
