package com.hexaghost.flixora.domain.repository

import com.hexaghost.flixora.domain.model.InstalledProvider
import com.hexaghost.flixora.domain.model.ProviderInfo
import com.hexaghost.flixora.domain.model.Repository
import com.hexaghost.flixora.domain.model.StreamResult

interface ProviderRepository {
    /** Fetch and parse a repository manifest from [url]. */
    suspend fun fetchManifest(url: String): Result<Repository>

    /** Download a provider's JS file and register it locally. */
    suspend fun installProvider(provider: ProviderInfo): Result<InstalledProvider>

    /** Remove a provider's JS file and registration. */
    suspend fun uninstallProvider(id: String)

    /** Toggle a provider's enabled/disabled state. */
    fun toggleProvider(id: String, enabled: Boolean)

    /** Return all currently installed providers. */
    fun getInstalledProviders(): List<InstalledProvider>

    /** Return all saved repository URLs. */
    fun getSavedRepositoryUrls(): List<String>

    /** Save a repository URL to persistent storage. */
    fun saveRepositoryUrl(url: String)

    /** Remove a repository URL from persistent storage. */
    fun removeRepositoryUrl(url: String)

    /**
     * Execute [provider]'s JS file, passing media info, and return resolved stream URLs.
     * [season] and [episode] are required for TV shows (0 for movies).
     */
    suspend fun resolveStreams(
        provider: InstalledProvider,
        tmdbId: Int,
        title: String,
        mediaType: String,
        year: Int,
        season: Int = 0,
        episode: Int = 0
    ): Result<List<StreamResult>>
}
