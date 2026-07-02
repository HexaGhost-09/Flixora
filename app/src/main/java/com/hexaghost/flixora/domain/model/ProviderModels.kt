package com.hexaghost.flixora.domain.model

/**
 * Represents a remote provider repository fetched from a manifest URL.
 */
data class Repository(
    val url: String,
    val name: String,
    val description: String,
    val providers: List<ProviderInfo>
)

/**
 * Metadata for a single provider listed in a repository manifest.
 * Compatible with both Flixora-native (providers[]) and Nuvio (scrapers[]) manifest formats.
 */
data class ProviderInfo(
    val id: String,
    val name: String,
    val version: String,
    val filename: String,
    val description: String = "",
    val repositoryUrl: String = "",
    val author: String = "",
    val logo: String = "",
    val supportedTypes: List<String> = listOf("movie", "tv"),
    val formats: List<String> = emptyList(),
    val enabled: Boolean = true
)

/**
 * An installed (downloaded) provider stored locally.
 */
data class InstalledProvider(
    val id: String,
    val name: String,
    val version: String,
    val jsFilePath: String,
    val repositoryUrl: String,
    val isEnabled: Boolean = true,
    val logo: String = "",
    val supportedTypes: List<String> = listOf("movie", "tv")
)

/**
 * A resolved stream result returned by a JS provider.
 */
data class StreamResult(
    val url: String,
    val quality: String = "Auto",
    val headers: Map<String, String> = emptyMap(),
    val providerName: String = ""
)
