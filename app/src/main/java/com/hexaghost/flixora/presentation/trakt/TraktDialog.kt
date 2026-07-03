package com.hexaghost.flixora.presentation.trakt

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hexaghost.flixora.data.api.TraktManager
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
    val traktManager = remember { TraktManager() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Tab state: 0 = Device Authorization Flow, 1 = Manual Login
    var selectedTab by remember { mutableStateOf(0) }

    // API credentials
    var clientId by remember { mutableStateOf(preferencesManager.traktClientId.ifBlank { "39d3753235b3c4349ab9c50adbc54b1f4c781604a11db9d2551e7fb53e77f0d0" }) }
    var clientSecret by remember { mutableStateOf(preferencesManager.traktClientSecret) }

    // Manual connection inputs
    var manualUsername by remember { mutableStateOf("") }
    var manualToken by remember { mutableStateOf("") }

    // Device Code Flow status
    var deviceCodeResponse by remember { mutableStateOf<TraktManager.DeviceCodeResponse?>(null) }
    var isGeneratingCode by remember { mutableStateOf(false) }
    var isPollingToken by remember { mutableStateOf(false) }
    var pollError by remember { mutableStateOf<String?>(null) }

    // Start polling when deviceCodeResponse is fetched
    LaunchedEffect(deviceCodeResponse) {
        val codeResponse = deviceCodeResponse ?: return@LaunchedEffect
        isPollingToken = true
        pollError = null
        val intervalMs = (codeResponse.interval.coerceAtLeast(1) * 1000).toLong()
        var elapsed = 0L
        val maxDuration = codeResponse.expiresIn.toLong() * 1000L

        while (isPollingToken && elapsed < maxDuration) {
            delay(intervalMs)
            elapsed += intervalMs

            val tokenRes = traktManager.pollDeviceToken(
                clientId = clientId.trim(),
                clientSecret = clientSecret.trim(),
                deviceCode = codeResponse.deviceCode
            )

            if (tokenRes != null) {
                // Success! Get username
                val fetchedUsername = traktManager.fetchUsername(tokenRes.accessToken, clientId.trim()) ?: "Trakt User"
                
                preferencesManager.traktClientId = clientId.trim()
                preferencesManager.traktClientSecret = clientSecret.trim()
                preferencesManager.traktToken = tokenRes.accessToken
                preferencesManager.traktUsername = fetchedUsername

                isPollingToken = false
                Toast.makeText(context, "Successfully connected to Trakt!", Toast.LENGTH_LONG).show()
                onSuccess()
                onDismiss()
                break
            }
        }
        if (isPollingToken) {
            isPollingToken = false
            pollError = "Activation code expired. Please try again."
        }
    }

    Dialog(
        onDismissRequest = {
            isPollingToken = false
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = FlixoraDarkSurface,
            border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
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
                        text = "TJ",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Trakt Synchronization",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = FlixoraWhite
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = FlixoraDarkCard,
                    contentColor = FlixoraCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFED1C24)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Device Activation", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) Color.White else FlixoraWhite60) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Manual Tokens", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) Color.White else FlixoraWhite60) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (selectedTab == 0) {
                    // Device Code Authorization Flow
                    if (deviceCodeResponse == null) {
                        Text(
                            text = "Register or open your Trakt API App settings to get your client credentials. Default keys are pre-filled below for convenience.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FlixoraWhite60,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Client ID Field
                        OutlinedTextField(
                            value = clientId,
                            onValueChange = { clientId = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Client ID", color = FlixoraWhite60) },
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // Client Secret Field
                        OutlinedTextField(
                            value = clientSecret,
                            onValueChange = { clientSecret = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Client Secret (Required for polling)", color = FlixoraWhite60) },
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = FlixoraWhite60)
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

                        Button(
                            onClick = {
                                if (clientId.isBlank() || clientSecret.isBlank()) {
                                    Toast.makeText(context, "Please enter both Client ID and Client Secret", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    isGeneratingCode = true
                                    val codeRes = traktManager.generateDeviceCode(clientId.trim())
                                    isGeneratingCode = false
                                    if (codeRes != null) {
                                        deviceCodeResponse = codeRes
                                    } else {
                                        Toast.makeText(context, "Error generating device code. Verify Client ID.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFED1C24),
                                contentColor = Color.White
                            ),
                            enabled = !isGeneratingCode
                        ) {
                            if (isGeneratingCode) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text("Generate Activation Code", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Displaying generated device activation code
                        val code = deviceCodeResponse!!
                        
                        Text(
                            text = "1. Go to this URL in your web browser:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = FlixoraWhite80,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(FlixoraDarkCard)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(code.verificationUrl))
                                    context.startActivity(intent)
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = code.verificationUrl,
                                color = FlixoraCyan,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Filled.OpenInNew, contentDescription = "Open", tint = FlixoraCyan, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "2. Enter this authorization code:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = FlixoraWhite80,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SelectionContainer {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FlixoraDarkCard)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = code.userCode,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(code.userCode))
                                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = FlixoraWhite80)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (isPollingToken) {
                            CircularProgressIndicator(
                                color = Color(0xFFED1C24),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Waiting for activation on Trakt...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FlixoraWhite60,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        pollError?.let { err ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    deviceCodeResponse = null
                                    isPollingToken = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FlixoraPurple)
                            ) {
                                Text("Retry")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = {
                                isPollingToken = false
                                deviceCodeResponse = null
                            }
                        ) {
                            Text("Change Credentials / Go Back", color = FlixoraWhite60)
                        }
                    }
                } else {
                    // Manual Token Input
                    Text(
                        text = "Manually enter your Trakt tokens if you have generated them elsewhere.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FlixoraWhite60,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = clientId,
                        onValueChange = { clientId = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Client ID", color = FlixoraWhite60) },
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

                    OutlinedTextField(
                        value = manualUsername,
                        onValueChange = { manualUsername = it },
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

                    OutlinedTextField(
                        value = manualToken,
                        onValueChange = { manualToken = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Access Token", color = FlixoraWhite60) },
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

                    Button(
                        onClick = {
                            if (clientId.isBlank() || manualUsername.isBlank() || manualToken.isBlank()) {
                                Toast.makeText(context, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            preferencesManager.traktClientId = clientId.trim()
                            preferencesManager.traktClientSecret = clientSecret.trim()
                            preferencesManager.traktToken = manualToken.trim()
                            preferencesManager.traktUsername = manualUsername.trim()
                            
                            Toast.makeText(context, "Connected manually!", Toast.LENGTH_SHORT).show()
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
                        Text("Connect Account", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = {
                    isPollingToken = false
                    onDismiss()
                }) {
                    Text("Cancel", color = FlixoraWhite60)
                }
            }
        }
    }
}
