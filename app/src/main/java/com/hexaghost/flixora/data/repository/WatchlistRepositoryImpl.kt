package com.hexaghost.flixora.data.repository

import com.hexaghost.flixora.data.local.dao.WatchlistDao
import com.hexaghost.flixora.data.local.entity.WatchlistEntity
import com.hexaghost.flixora.domain.model.Media
import com.hexaghost.flixora.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val watchlistDao: WatchlistDao
) : WatchlistRepository {

    override fun getWatchlist(): Flow<List<Media>> =
        watchlistDao.getAllWatchlist().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun addToWatchlist(media: Media) {
        watchlistDao.addToWatchlist(media.toEntity())
    }

    override suspend fun removeFromWatchlist(mediaId: Int) {
        watchlistDao.removeFromWatchlistById(mediaId)
    }

    override fun isInWatchlist(mediaId: Int): Flow<Boolean> =
        watchlistDao.isInWatchlist(mediaId)

    private fun WatchlistEntity.toDomain() = Media(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        mediaType = mediaType
    )

    private fun Media.toEntity() = WatchlistEntity(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        mediaType = mediaType
    )
}
