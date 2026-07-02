package com.hexaghost.flixora.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("flixora_settings_prefs", Context.MODE_PRIVATE)

    var wifiOnlyDownload: Boolean
        get() = prefs.getBoolean("wifi_only_download", true)
        set(value) = prefs.edit().putBoolean("wifi_only_download", value).apply()

    var highQualityStreaming: Boolean
        get() = prefs.getBoolean("high_quality_streaming", true)
        set(value) = prefs.edit().putBoolean("high_quality_streaming", value).apply()

    var selectedLanguage: String
        get() = prefs.getString("selected_language", "English") ?: "English"
        set(value) = prefs.edit().putString("selected_language", value).apply()

    // Trakt Connection
    var traktUsername: String
        get() = prefs.getString("trakt_username", "") ?: ""
        set(value) = prefs.edit().putString("trakt_username", value).apply()

    var traktToken: String
        get() = prefs.getString("trakt_token", "") ?: ""
        set(value) = prefs.edit().putString("trakt_token", value).apply()

    val isTraktConnected: Boolean
        get() = traktToken.isNotEmpty()

    fun disconnectTrakt() {
        prefs.edit().remove("trakt_username").remove("trakt_token").apply()
    }

    // Search History
    var searchHistory: List<String>
        get() {
            val historyString = prefs.getString("search_history", "") ?: ""
            if (historyString.isEmpty()) return emptyList()
            return historyString.split("|||")
        }
        set(value) {
            val historyString = value.joinToString("|||")
            prefs.edit().putString("search_history", historyString).apply()
        }

    fun addSearchQuery(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return
        val current = searchHistory.toMutableList()
        current.remove(cleanQuery) // remove duplicate if exists
        current.add(0, cleanQuery) // add to top
        if (current.size > 10) {
            current.removeAt(current.size - 1) // keep last 10
        }
        searchHistory = current
    }

    fun deleteSearchHistoryItem(query: String) {
        val current = searchHistory.toMutableList()
        current.remove(query)
        searchHistory = current
    }

    fun clearSearchHistory() {
        searchHistory = emptyList()
    }

    // Continue Watching and Trakt Progress Sync
    fun getContinueWatchingItems(): List<ContinueWatchingItem> {
        val json = prefs.getString("continue_watching_items", "[]") ?: "[]"
        val type = object : TypeToken<List<ContinueWatchingItem>>() {}.type
        return try {
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveContinueWatchingItem(item: ContinueWatchingItem) {
        val current = getContinueWatchingItems().toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item) // add to top
        if (current.size > 8) {
            current.removeAt(current.size - 1) // keep last 8
        }
        val json = Gson().toJson(current)
        prefs.edit().putString("continue_watching_items", json).apply()
    }

    fun updateContinueWatchingProgress(id: Int, progress: Float) {
        val current = getContinueWatchingItems().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = current[index].copy(progress = progress)
            current[index] = updated
            val json = Gson().toJson(current)
            prefs.edit().putString("continue_watching_items", json).apply()
        }
    }
}

data class ContinueWatchingItem(
    val id: Int,
    val title: String,
    val mediaType: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val progress: Float
)
