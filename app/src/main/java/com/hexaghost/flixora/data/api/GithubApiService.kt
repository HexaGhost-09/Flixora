package com.hexaghost.flixora.data.api

import com.hexaghost.flixora.data.api.model.GithubRelease
import retrofit2.http.GET

interface GithubApiService {
    @GET("repos/HexaGhost-09/Flixora/releases/latest")
    suspend fun getLatestRelease(): GithubRelease
}
