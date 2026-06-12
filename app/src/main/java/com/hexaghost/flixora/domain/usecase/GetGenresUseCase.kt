package com.hexaghost.flixora.domain.usecase

import com.hexaghost.flixora.domain.model.Genre
import com.hexaghost.flixora.domain.repository.MediaRepository
import javax.inject.Inject

class GetGenresUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend fun getMovieGenres(): Result<List<Genre>> = runCatching {
        repository.getMovieGenres()
    }

    suspend fun getTvGenres(): Result<List<Genre>> = runCatching {
        repository.getTvGenres()
    }

    suspend fun getAllGenres(): Result<List<Genre>> = runCatching {
        val movies = repository.getMovieGenres()
        val tv = repository.getTvGenres()
        (movies + tv).distinctBy { it.id }
    }
}
