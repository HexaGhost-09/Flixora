package com.hexaghost.flixora.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hexaghost.flixora.domain.model.Media
import com.hexaghost.flixora.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HeroBanner(
    mediaList: List<Media>,
    onMediaClick: (Media) -> Unit,
    modifier: Modifier = Modifier
) {
    if (mediaList.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { mediaList.size })

    // Auto-scroll
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000)
            val nextPage = (pagerState.currentPage + 1) % mediaList.size
            pagerState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(600, easing = EaseInOutCubic)
            )
        }
    }

    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val media = mediaList[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onMediaClick(media) }
            ) {
                // Backdrop image
                AsyncImage(
                    model = media.backdropPath ?: media.posterPath,
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient overlays
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x40000000),
                                    Color.Transparent,
                                    Color(0xCC000000),
                                    FlixoraDarkBg
                                )
                            )
                        )
                )

                // Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Media type badge
                    Surface(
                        color = if (media.mediaType == "tv") FlixoraCyan.copy(0.2f)
                                else FlixoraPurple.copy(0.3f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (media.mediaType == "tv") "TV SERIES" else "MOVIE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (media.mediaType == "tv") FlixoraCyan else FlixoraPurple,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Title
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = FlixoraWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Rating + date
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (media.voteAverage > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = StarYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = String.format("%.1f", media.voteAverage),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = FlixoraWhite
                                )
                            }
                        }
                        if (media.voteAverage > 0 && media.releaseDate != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        media.releaseDate?.take(4)?.let { year ->
                            Text(
                                text = year,
                                style = MaterialTheme.typography.labelMedium,
                                color = FlixoraWhite60
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play button
                    Button(
                        onClick = { onMediaClick(media) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FlixoraWhite,
                            contentColor = FlixoraDarkBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Watch Now",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Pager indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(mediaList.size) { index ->
                val isSelected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 6.dp,
                    animationSpec = tween(300),
                    label = "indicator_width"
                )
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(if (isSelected) FlixoraCyan else FlixoraWhite40)
                )
            }
        }
    }
}
