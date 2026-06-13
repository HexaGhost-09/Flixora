package com.hexaghost.flixora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.hexaghost.flixora.data.local.PreferencesManager
import com.hexaghost.flixora.domain.update.UpdateManager
import com.hexaghost.flixora.navigation.FlixoraNavigation
import com.hexaghost.flixora.ui.theme.FlixoraDarkBg
import com.hexaghost.flixora.ui.theme.FlixoraTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var updateManager: UpdateManager

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (updateManager.isAutoCheckEnabled) {
            lifecycleScope.launch {
                updateManager.checkForUpdates(isManual = false)
            }
        }

        enableEdgeToEdge()
        setContent {
            FlixoraTheme {
                FlixoraNavigation(
                    updateManager = updateManager,
                    preferencesManager = preferencesManager
                )
            }
        }
    }
}

