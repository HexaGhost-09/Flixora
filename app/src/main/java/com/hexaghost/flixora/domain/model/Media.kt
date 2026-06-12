package com.hexaghost.flixora.domain.model

data class Media(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val releaseDate: String?,
    val mediaType: String, // "movie" or "tv"
    val genreIds: List<Int> = emptyList(),
    val popularity: Double = 0.0
)

data class MediaDetail(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val voteCount: Int,
    val releaseDate: String?,
    val genres: List<Genre>,
    val runtime: Int?,
    val status: String,
    val tagline: String,
    val cast: List<CastMember>,
    val trailerKey: String?,
    val similarMedia: List<Media>,
    val mediaType: String,
    val numberOfSeasons: Int? = null,
    val numberOfEpisodes: Int? = null
)

data class Genre(
    val id: Int,
    val name: String
)

data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val profilePath: String?
)

enum class MediaType(val value: String) {
    MOVIE("movie"),
    TV("tv");

    companion object {
        fun from(value: String?) = entries.find { it.value == value } ?: MOVIE
    }
}
