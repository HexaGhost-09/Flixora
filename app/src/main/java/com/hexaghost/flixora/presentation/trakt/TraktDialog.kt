package com.hexaghost.flixora.presentation.trakt

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hexaghost.flixora.data.local.PreferencesManager
import com.hexaghost.flixora.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraktDialog(
    onDismiss: () -> Unit,
    preferencesManager: PreferencesManager,
    onSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var isSimulatingAuth by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = FlixoraDarkSurface,
            border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Branded Header (Trakt is known for red/black)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFED1C24), Color(0xFFB81D24))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TJ", // Trakt representation
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Connect Trakt",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = FlixoraWhite
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sync your watchlist, history, and scrobble what you watch automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlixoraWhite60,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isSimulatingAuth) {
                    CircularProgressIndicator(
                        color = Color(0xFFED1C24),
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                    Text(
                        text = "Authenticating with Trakt...",
                        color = FlixoraWhite80,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    // Username Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Trakt Username", color = FlixoraWhite60) },
                        leadingIcon = {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = FlixoraWhite60)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFED1C24),
                            unfocusedBorderColor = FlixoraWhite40,
                            focusedTextColor = FlixoraWhite,
                            unfocusedTextColor = FlixoraWhite,
                            cursorColor = Color(0xFFED1C24),
                            focusedContainerColor = FlixoraDarkCard,
                            unfocusedContainerColor = FlixoraDarkCard
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Access Token Field
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Access Token / PIN", color = FlixoraWhite60) },
                        leadingIcon = {
                            Icon(Icons.Filled.Key, contentDescription = null, tint = FlixoraWhite60)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFED1C24),
                            unfocusedBorderColor = FlixoraWhite40,
                            focusedTextColor = FlixoraWhite,
                            unfocusedTextColor = FlixoraWhite,
                            cursorColor = Color(0xFFED1C24),
                            focusedContainerColor = FlixoraDarkCard,
                            unfocusedContainerColor = FlixoraDarkCard
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Simulated OAuth / Connect Buttons
                    Button(
                        onClick = {
                            if (username.isBlank() || token.isBlank()) {
                                Toast.makeText(context, "Please enter username and token", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            preferencesManager.traktUsername = username.trim()
                            preferencesManager.traktToken = token.trim()
                            Toast.makeText(context, "Connected to Trakt!", Toast.LENGTH_SHORT).show()
                            onSuccess()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFED1C24),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Connect Manually", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isSimulatingAuth = true
                                delay(1500) // Simulate request
                                preferencesManager.traktUsername = "flixora_cinephile"
                                preferencesManager.traktToken = "trakt_oauth_token_simulated_777"
                                isSimulatingAuth = false
                                Toast.makeText(context, "OAuth Connection Successful!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = FlixoraWhite
                        )
                    ) {
                        Text("OAuth Sign In (Instant)", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = FlixoraWhite60)
                    }
                }
            }
        }
    }
}
