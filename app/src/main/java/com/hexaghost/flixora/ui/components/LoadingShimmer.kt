package com.hexaghost.flixora.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hexaghost.flixora.ui.theme.FlixoraDarkCard
import com.hexaghost.flixora.ui.theme.FlixoraDarkElevated

@Composable
fun ShimmerBrush(): Brush {
    val shimmerColors = listOf(
        FlixoraDarkCard,
        FlixoraDarkElevated,
        FlixoraDarkCard
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
}

@Composable
fun MediaCardShimmer(modifier: Modifier = Modifier) {
    val brush = ShimmerBrush()
    Box(
        modifier = modifier
            .width(130.dp)
            .height(195.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(brush)
    )
}

@Composable
fun HeroBannerShimmer() {
    val brush = ShimmerBrush()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .background(brush)
    )
}

@Composable
fun MediaRowShimmer() {
    Column {
        // Section title shimmer
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .width(150.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ShimmerBrush())
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(6) {
                MediaCardShimmer()
            }
        }
    }
}

@Composable
fun FullScreenLoadingShimmer() {
    Column {
        HeroBannerShimmer()
        Spacer(modifier = Modifier.height(16.dp))
        repeat(2) {
            MediaRowShimmer()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
