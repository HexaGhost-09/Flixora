package com.hexaghost.flixora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.hexaghost.flixora.navigation.FlixoraNavigation
import com.hexaghost.flixora.ui.theme.FlixoraDarkBg
import com.hexaghost.flixora.ui.theme.FlixoraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlixoraTheme {
                FlixoraNavigation()
            }
        }
    }
}
