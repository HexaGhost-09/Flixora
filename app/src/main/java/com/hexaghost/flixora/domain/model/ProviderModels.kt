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
 */
data class ProviderInfo(
    val id: String,
    val name: String,
    val version: String,
    val filename: String,
    val description: String = "",
    val repositoryUrl: String = ""
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
    val isEnabled: Boolean = true
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
