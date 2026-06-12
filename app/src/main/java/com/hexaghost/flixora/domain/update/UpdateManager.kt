package com.hexaghost.flixora.domain.update

import android.content.Context
import com.hexaghost.flixora.BuildConfig
import com.hexaghost.flixora.data.api.GithubApiService
import com.hexaghost.flixora.data.api.model.GithubRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String?
    ) : UpdateState
    object UpToDate : UpdateState
    data class Error(val message: String) : UpdateState
}

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val githubApiService: GithubApiService
) {
    private val prefs = context.getSharedPreferences("flixora_update_prefs", Context.MODE_PRIVATE)

    var isAutoCheckEnabled: Boolean
        get() = prefs.getBoolean("auto_check_updates", true)
        set(value) = prefs.edit().putBoolean("auto_check_updates", value).apply()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    // Flag to ensure auto check only runs once per app session
    private var hasAutoCheckedThisSession = false

    suspend fun checkForUpdates(isManual: Boolean = false): UpdateState {
        if (!isManual && hasAutoCheckedThisSession) {
            return UpdateState.Idle
        }
        if (!isManual) {
            hasAutoCheckedThisSession = true
        }

        _updateState.value = UpdateState.Checking
        try {
            val latestRelease = githubApiService.getLatestRelease()
            val currentVersion = BuildConfig.VERSION_NAME
            val latestVersion = latestRelease.tagName

            val isAvailable = isUpdateAvailable(currentVersion, latestVersion)
            val state = if (isAvailable) {
                UpdateState.UpdateAvailable(
                    currentVersion = currentVersion,
                    latestVersion = latestVersion,
                    downloadUrl = latestRelease.htmlUrl,
                    releaseNotes = latestRelease.body
                )
            } else {
                UpdateState.UpToDate
            }
            _updateState.value = state
            return state
        } catch (e: Exception) {
            val errorState = UpdateState.Error(e.message ?: "Unknown error checking updates")
            _updateState.value = errorState
            return errorState
        }
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        val currentClean = currentVersion.trim().removePrefix("v")
        val latestClean = latestVersion.trim().removePrefix("v")

        val currentParts = currentClean.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latestClean.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLength) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }
            if (latestPart > currentPart) return true
            if (currentPart > latestPart) return false
        }
        return false
    }
}
