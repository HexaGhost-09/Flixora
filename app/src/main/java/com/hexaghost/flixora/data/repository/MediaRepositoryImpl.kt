package com.hexaghost.flixora.data.repository

import com.hexaghost.flixora.data.api.TmdbApiService
import com.hexaghost.flixora.data.api.model.TmdbMedia
import com.hexaghost.flixora.data.api.model.TmdbMediaDetail
import com.hexaghost.flixora.domain.model.*
import com.hexaghost.flixora.domain.repository.MediaRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val api: TmdbApiService
) : MediaRepository {

    private val imageBase = "https://image.tmdb.org/t/p/"

    private fun TmdbMedia.toDomain(): Media = Media(
        id = id,
        title = title ?: name ?: "Unknown",
        overview = overview,
        posterPath = posterPath?.let { "${imageBase}w500$it" },
        backdropPath = backdropPath?.let { "${imageBase}w780$it" },
        voteAverage = voteAverage,
        releaseDate = releaseDate ?: firstAirDate,
        mediaType = mediaType ?: if (title != null) "movie" else "tv",
        genreIds = genreIds,
        popularity = popularity
    )

    private fun TmdbMediaDetail.toDomain(mediaType: String): MediaDetail {
        val trailer = videos?.results
            ?.filter { it.site == "YouTube" && (it.type == "Trailer" || it.type == "Teaser") }
            ?.sortedWith(compareByDescending<com.hexaghost.flixora.data.api.model.TmdbVideo> { it.official }.thenByDescending { it.type == "Trailer" })
            ?.firstOrNull()

        return MediaDetail(
            id = id,
            title = title ?: name ?: "Unknown",
            overview = overview,
            posterPath = posterPath?.let { "${imageBase}w500$it" },
            backdropPath = backdropPath?.let { "${imageBase}w1280$it" },
            voteAverage = voteAverage,
            voteCount = voteCount,
            releaseDate = releaseDate ?: firstAirDate,
            genres = genres.map { Genre(it.id, it.name) },
            runtime = runtime ?: episodeRunTime.firstOrNull(),
            status = status,
            tagline = tagline,
            cast = credits?.cast?.take(10)?.map {
                CastMember(
                    id = it.id,
                    name = it.name,
                    character = it.character,
                    profilePath = it.profilePath?.let { p -> "${imageBase}w185$p" }
                )
            } ?: emptyList(),
            trailerKey = trailer?.key,
            similarMedia = similar?.results?.map { it.copy(mediaType = mediaType).toDomain() } ?: emptyList(),
            mediaType = mediaType,
            numberOfSeasons = numberOfSeasons,
            numberOfEpisodes = numberOfEpisodes
        )
    }

    override suspend fun getTrending(page: Int): List<Media> =
        api.getTrending(page).results.map { it.toDomain() }

    override suspend fun getPopularMovies(page: Int): List<Media> =
        api.getPopularMovies(page).results.map { it.copy(mediaType = "movie").toDomain() }

    override suspend fun getTopRatedMovies(page: Int): List<Media> =
        api.getTopRatedMovies(page).results.map { it.copy(mediaType = "movie").toDomain() }

    override suspend fun getNowPlayingMovies(page: Int): List<Media> =
        api.getNowPlayingMovies(page).results.map { it.copy(mediaType = "movie").toDomain() }

    override suspend fun getUpcomingMovies(page: Int): List<Media> =
        api.getUpcomingMovies(page).results.map { it.copy(mediaType = "movie").toDomain() }

    override suspend fun getPopularTvShows(page: Int): List<Media> =
        api.getPopularTvShows(page).results.map { it.copy(mediaType = "tv").toDomain() }

    override suspend fun getTopRatedTvShows(page: Int): List<Media> =
        api.getTopRatedTvShows(page).results.map { it.copy(mediaType = "tv").toDomain() }

    override suspend fun getOnAirTvShows(page: Int): List<Media> =
        api.getOnAirTvShows(page).results.map { it.copy(mediaType = "tv").toDomain() }

    override suspend fun getMovieGenres(): List<Genre> =
        api.getMovieGenres().genres.map { Genre(it.id, it.name) }

    override suspend fun getTvGenres(): List<Genre> =
        api.getTvGenres().genres.map { Genre(it.id, it.name) }

    override suspend fun discoverMoviesByGenre(genreId: Int, page: Int): List<Media> =
        api.discoverMoviesByGenre(genreId, page).results.map { it.copy(mediaType = "movie").toDomain() }

    override suspend fun discoverTvByGenre(genreId: Int, page: Int): List<Media> =
        api.discoverTvByGenre(genreId, page).results.map { it.copy(mediaType = "tv").toDomain() }

    override suspend fun getMovieDetail(movieId: Int): MediaDetail =
        api.getMovieDetail(movieId).toDomain("movie")

    override suspend fun getTvDetail(seriesId: Int): MediaDetail =
        api.getTvDetail(seriesId).toDomain("tv")

    override suspend fun searchMulti(query: String, page: Int): List<Media> =
        api.searchMulti(query, page).results
            .filter { it.mediaType in listOf("movie", "tv") }
            .map { it.toDomain() }
}
