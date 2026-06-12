package com.hexaghost.flixora.data.api

import com.hexaghost.flixora.data.api.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    // Trending
    @GET("trending/all/week")
    suspend fun getTrending(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMedia>

    // Movies
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMedia>

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMedia>

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMedia>

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMedia>

    // TV Shows
    @GET("tv/popular")
    suspend fun getPopularTvShows(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMedia>

    @GET("tv/top_rated")
    suspend fun getTopRatedTvShows(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMedia>

    @GET("tv/on_the_air")
    suspend fun getOnAirTvShows(
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMedia>

    // Details
    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: Int,
        @Query("append_to_response") appendToResponse: String = "credits,similar,videos"
    ): TmdbMediaDetail

    @GET("tv/{series_id}")
    suspend fun getTvDetail(
        @Path("series_id") seriesId: Int,
        @Query("append_to_response") appendToResponse: String = "credits,similar,videos"
    ): TmdbMediaDetail

    // Genres
    @GET("genre/movie/list")
    suspend fun getMovieGenres(): TmdbGenreResponse

    @GET("genre/tv/list")
    suspend fun getTvGenres(): TmdbGenreResponse

    // Discover by genre
    @GET("discover/movie")
    suspend fun discoverMoviesByGenre(
        @Query("with_genres") genreId: Int,
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "popularity.desc"
    ): TmdbResponse<TmdbMedia>

    @GET("discover/tv")
    suspend fun discoverTvByGenre(
        @Query("with_genres") genreId: Int,
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "popularity.desc"
    ): TmdbResponse<TmdbMedia>

    // Search
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMedia>

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMedia>

    @GET("search/tv")
    suspend fun searchTvShows(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<TmdbMedia>
}
