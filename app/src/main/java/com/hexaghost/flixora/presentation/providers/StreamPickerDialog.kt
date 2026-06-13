package com.hexaghost.flixora.presentation.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hexaghost.flixora.domain.model.StreamResult
import com.hexaghost.flixora.ui.theme.*

/**
 * Shows a list of resolved stream URLs for the user to select one for playback.
 */
@Composable
fun StreamPickerDialog(
    streams: List<StreamResult>,
    mediaTitle: String,
    onDismiss: () -> Unit,
    onSelect: (StreamResult) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = FlixoraDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                Brush.linearGradient(listOf(FlixoraCyan, FlixoraPurple)),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Select Stream",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = FlixoraWhite
                        )
                        Text(
                            text = mediaTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = FlixoraWhite60,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = FlixoraWhite60)
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0x1AFFFFFF))
                Spacer(Modifier.height(8.dp))

                if (streams.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.SearchOff, contentDescription = null, tint = FlixoraWhite40, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No streams found", color = FlixoraWhite60, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(streams) { stream ->
                            StreamItem(stream = stream, onClick = { onSelect(stream) })
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "${streams.size} stream${if (streams.size != 1) "s" else ""} found",
                    style = MaterialTheme.typography.bodySmall,
                    color = FlixoraWhite40
                )
            }
        }
    }
}

@Composable
private fun StreamItem(
    stream: StreamResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FlixoraDarkBg)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quality badge
        Surface(
            color = when {
                stream.quality.contains("4K", ignoreCase = true) -> Color(0xFFFFD700).copy(0.15f)
                stream.quality.contains("1080", ignoreCase = true) -> FlixoraCyan.copy(0.15f)
                stream.quality.contains("720", ignoreCase = true) -> FlixoraPurple.copy(0.15f)
                else -> FlixoraDarkElevated
            },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = stream.quality,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = when {
                    stream.quality.contains("4K", ignoreCase = true) -> Color(0xFFFFD700)
                    stream.quality.contains("1080", ignoreCase = true) -> FlixoraCyan
                    stream.quality.contains("720", ignoreCase = true) -> FlixoraPurple
                    else -> FlixoraWhite80
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stream.providerName.ifBlank { "Unknown Provider" },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = FlixoraWhite
            )
            Text(
                text = stream.url.take(50) + if (stream.url.length > 50) "…" else "",
                style = MaterialTheme.typography.bodySmall,
                color = FlixoraWhite40,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = FlixoraCyan, modifier = Modifier.size(20.dp))
    }
}
