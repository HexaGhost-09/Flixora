package com.hexaghost.flixora.domain.usecase

import com.hexaghost.flixora.domain.model.Media
import com.hexaghost.flixora.domain.repository.MediaRepository
import javax.inject.Inject

class DiscoverByGenreUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend fun discoverMovies(genreId: Int, page: Int = 1): Result<List<Media>> = runCatching {
        repository.discoverMoviesByGenre(genreId, page)
    }

    suspend fun discoverTv(genreId: Int, page: Int = 1): Result<List<Media>> = runCatching {
        repository.discoverTvByGenre(genreId, page)
    }
}
