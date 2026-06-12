package com.hexaghost.flixora.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hexaghost.flixora.ui.theme.FlixoraWhite
import com.hexaghost.flixora.ui.theme.StarYellow

@Composable
fun RatingBadge(
    rating: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "Rating",
            tint = StarYellow,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = String.format("%.1f", rating),
            style = MaterialTheme.typography.labelLarge,
            color = FlixoraWhite
        )
        Text(
            text = "/10",
            style = MaterialTheme.typography.labelMedium,
            color = FlixoraWhite.copy(alpha = 0.6f)
        )
    }
}
