package com.hexaghost.flixora.presentation.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hexaghost.flixora.ui.theme.*

@Composable
fun AddRepositoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    val isValidUrl = url.startsWith("http://") || url.startsWith("https://")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = FlixoraDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                Brush.linearGradient(listOf(FlixoraCyan, FlixoraPurple)),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Add Repository",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = FlixoraWhite
                        )
                        Text(
                            "Paste a repository URL",
                            style = MaterialTheme.typography.bodySmall,
                            color = FlixoraWhite60
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = FlixoraWhite60)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // URL Input
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Repository URL") },
                    placeholder = { Text("https://example.com/repo", color = FlixoraWhite40) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isValidUrl) {
                                onAdd(url.trim())
                                onDismiss()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlixoraCyan,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = FlixoraCyan,
                        unfocusedLabelColor = FlixoraWhite60,
                        cursorColor = FlixoraCyan,
                        focusedTextColor = FlixoraWhite,
                        unfocusedTextColor = FlixoraWhite
                    )
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "The URL should point to a directory containing a manifest.json file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FlixoraWhite40
                )

                Spacer(Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FlixoraWhite40)
                    ) {
                        Text("Cancel", color = FlixoraWhite80)
                    }
                    Button(
                        onClick = {
                            onAdd(url.trim())
                            onDismiss()
                        },
                        enabled = isValidUrl,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FlixoraCyan,
                            contentColor = Color.Black,
                            disabledContainerColor = FlixoraCyan.copy(alpha = 0.3f)
                        )
                    ) {
                        Text("Add Repository", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
