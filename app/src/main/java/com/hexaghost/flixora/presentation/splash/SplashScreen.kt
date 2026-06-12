package com.hexaghost.flixora.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.hexaghost.flixora.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var animationStarted by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "splash_alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splash_scale"
    )

    LaunchedEffect(Unit) {
        animationStarted = true
        delay(2200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        FlixoraDeepPurple,
                        FlixoraDarkBg
                    ),
                    radius = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alpha)
                .scale(scale)
        ) {
            // App logo text
            Text(
                text = "FLIXORA",
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlixoraWhite,
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Stream Everything",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = FlixoraCyan,
                letterSpacing = 3.sp
            )
        }
    }
}
