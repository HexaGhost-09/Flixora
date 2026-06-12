package com.hexaghost.flixora.data.api.model

import com.google.gson.annotations.SerializedName

data class TmdbResponse<T>(
    @SerializedName("page") val page: Int = 1,
    @SerializedName("results") val results: List<T> = emptyList(),
    @SerializedName("total_pages") val totalPages: Int = 1,
    @SerializedName("total_results") val totalResults: Int = 0
)

data class TmdbMedia(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("overview") val overview: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("vote_average") val voteAverage: Double = 0.0,
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerializedName("media_type") val mediaType: String? = null,
    @SerializedName("popularity") val popularity: Double = 0.0,
    @SerializedName("adult") val adult: Boolean = false,
    @SerializedName("original_language") val originalLanguage: String = ""
)

data class TmdbMediaDetail(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("overview") val overview: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("vote_average") val voteAverage: Double = 0.0,
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("genres") val genres: List<TmdbGenre> = emptyList(),
    @SerializedName("runtime") val runtime: Int? = null,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @SerializedName("status") val status: String = "",
    @SerializedName("tagline") val tagline: String = "",
    @SerializedName("popularity") val popularity: Double = 0.0,
    @SerializedName("credits") val credits: TmdbCredits? = null,
    @SerializedName("similar") val similar: TmdbResponse<TmdbMedia>? = null,
    @SerializedName("videos") val videos: TmdbVideosResponse? = null,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int? = null
)

data class TmdbGenre(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = ""
)

data class TmdbGenreResponse(
    @SerializedName("genres") val genres: List<TmdbGenre> = emptyList()
)

data class TmdbCredits(
    @SerializedName("cast") val cast: List<TmdbCast> = emptyList(),
    @SerializedName("crew") val crew: List<TmdbCrew> = emptyList()
)

data class TmdbCast(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("character") val character: String = "",
    @SerializedName("profile_path") val profilePath: String? = null,
    @SerializedName("order") val order: Int = 0
)

data class TmdbCrew(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("job") val job: String = "",
    @SerializedName("profile_path") val profilePath: String? = null
)

data class TmdbVideosResponse(
    @SerializedName("results") val results: List<TmdbVideo> = emptyList()
)

data class TmdbVideo(
    @SerializedName("id") val id: String = "",
    @SerializedName("key") val key: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("site") val site: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("official") val official: Boolean = false
)
