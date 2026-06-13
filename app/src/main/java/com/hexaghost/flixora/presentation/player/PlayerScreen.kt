package com.hexaghost.flixora.presentation.player

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hexaghost.flixora.ui.theme.FlixoraDarkBg
import com.hexaghost.flixora.ui.theme.FlixoraWhite

@Composable
fun PlayerScreen(
    trailerKey: String,
    autoplay: Boolean,
    showControls: Boolean,
    onBack: () -> Unit
) {
    val autoplayParam = if (autoplay) "1" else "0"
    val controlsParam = if (showControls) "1" else "0"
    val youtubeUrl = "https://www.youtube.com/embed/$trailerKey?autoplay=$autoplayParam&controls=$controlsParam&rel=0&showinfo=0"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FlixoraDarkBg)
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color(0x40FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = FlixoraWhite
                    )
                }
            }

            // WebView player
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                        }
                        loadUrl(youtubeUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
