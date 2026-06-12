package com.hexaghost.flixora.domain.repository

import com.hexaghost.flixora.domain.model.Genre
import com.hexaghost.flixora.domain.model.Media
import com.hexaghost.flixora.domain.model.MediaDetail
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    suspend fun getTrending(page: Int = 1): List<Media>
    suspend fun getPopularMovies(page: Int = 1): List<Media>
    suspend fun getTopRatedMovies(page: Int = 1): List<Media>
    suspend fun getNowPlayingMovies(page: Int = 1): List<Media>
    suspend fun getUpcomingMovies(page: Int = 1): List<Media>
    suspend fun getPopularTvShows(page: Int = 1): List<Media>
    suspend fun getTopRatedTvShows(page: Int = 1): List<Media>
    suspend fun getOnAirTvShows(page: Int = 1): List<Media>
    suspend fun getMovieGenres(): List<Genre>
    suspend fun getTvGenres(): List<Genre>
    suspend fun discoverMoviesByGenre(genreId: Int, page: Int = 1): List<Media>
    suspend fun discoverTvByGenre(genreId: Int, page: Int = 1): List<Media>
    suspend fun getMovieDetail(movieId: Int): MediaDetail
    suspend fun getTvDetail(seriesId: Int): MediaDetail
    suspend fun searchMulti(query: String, page: Int = 1): List<Media>
}
