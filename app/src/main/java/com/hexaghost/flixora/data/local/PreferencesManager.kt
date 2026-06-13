package com.hexaghost.flixora.data.local

import android.content.Context
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

    var autoplayTrailers: Boolean
        get() = prefs.getBoolean("autoplay_trailers", false)
        set(value) = prefs.edit().putBoolean("autoplay_trailers", value).apply()

    var showPlayerControls: Boolean
        get() = prefs.getBoolean("show_player_controls", false)
        set(value) = prefs.edit().putBoolean("show_player_controls", value).apply()
}
