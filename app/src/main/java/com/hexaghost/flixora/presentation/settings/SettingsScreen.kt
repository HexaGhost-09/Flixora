package com.hexaghost.flixora.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hexaghost.flixora.BuildConfig
import com.hexaghost.flixora.ui.theme.*
import com.hexaghost.flixora.domain.update.UpdateManager
import com.hexaghost.flixora.domain.update.UpdateState
import androidx.hilt.navigation.compose.hiltViewModel
import com.hexaghost.flixora.presentation.auth.AuthViewModel
import com.hexaghost.flixora.presentation.auth.AuthDialog
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.hexaghost.flixora.data.local.PreferencesManager

@Composable
fun SettingsScreen(
    updateManager: UpdateManager,
    preferencesManager: PreferencesManager,
    authViewModel: AuthViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val authUiState by authViewModel.uiState.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    var showAuthDialog by remember { mutableStateOf(false) }

    var wifiOnlyDownload by remember { mutableStateOf(preferencesManager.wifiOnlyDownload) }
    var highQualityStreaming by remember { mutableStateOf(preferencesManager.highQualityStreaming) }
    var selectedLanguage by remember { mutableStateOf(preferencesManager.selectedLanguage) }
    var autoCheckUpdates by remember { mutableStateOf(updateManager.isAutoCheckEnabled) }
    var autoplayTrailers by remember { mutableStateOf(preferencesManager.autoplayTrailers) }
    var showPlayerControls by remember { mutableStateOf(preferencesManager.showPlayerControls) }
    
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isCheckingUpdates by remember { mutableStateOf(false) }

    if (showAuthDialog) {
        AuthDialog(
            onDismiss = { showAuthDialog = false },
            viewModel = authViewModel
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlixoraDarkBg)
            .statusBarsPadding()
            .verticalScroll(scrollState)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(FlixoraDarkSurface, FlixoraDarkBg)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = FlixoraWhite
            )
        }

        // Profile Section Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clickable { if (!isLoggedIn) showAuthDialog = true },
            colors = CardDefaults.cardColors(containerColor = FlixoraDarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0x1AFFFFFF))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    color = FlixoraPurple.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isLoggedIn) (authUiState.user?.name?.take(1)?.uppercase() ?: "U") else "?",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FlixoraCyan
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLoggedIn) (authUiState.user?.name ?: "User") else "Guest User",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = FlixoraWhite
                    )
                    Text(
                        text = if (isLoggedIn) (authUiState.user?.email ?: "") else "Tap to log in & sync data",
                        style = MaterialTheme.typography.bodySmall,
                        color = FlixoraCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (!isLoggedIn) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = FlixoraWhite60
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Settings Categories
        SettingsHeader(title = "App Settings")
        SettingsToggleRow(
            icon = Icons.Filled.Wifi,
            title = "Download over Wi-Fi only",
            description = "Saves mobile data usage",
            checked = wifiOnlyDownload,
            onCheckedChange = {
                wifiOnlyDownload = it
                preferencesManager.wifiOnlyDownload = it
            }
        )
        SettingsToggleRow(
            icon = Icons.Filled.HighQuality,
            title = "High Quality Streaming",
            description = "Stream in HD quality where available",
            checked = highQualityStreaming,
            onCheckedChange = {
                highQualityStreaming = it
                preferencesManager.highQualityStreaming = it
            }
        )
        SettingsToggleRow(
            icon = Icons.Filled.SystemUpdate,
            title = "Auto Check Updates",
            description = "Check for updates on app startup",
            checked = autoCheckUpdates,
            onCheckedChange = {
                autoCheckUpdates = it
                updateManager.isAutoCheckEnabled = it
            }
        )

        SettingsClickableRow(
            icon = Icons.Filled.Language,
            title = "App Language",
            value = selectedLanguage,
            onClick = {
                val nextLanguage = if (selectedLanguage == "English") "Spanish" else "English"
                selectedLanguage = nextLanguage
                preferencesManager.selectedLanguage = nextLanguage
            }
        )

        SettingsClickableRow(
            icon = Icons.Filled.AccountCircle,
            title = if (isLoggedIn) "Account Logout" else "Account Login",
            value = if (isLoggedIn) (authUiState.user?.name ?: "Logged In") else "Not Logged In",
            onClick = {
                if (isLoggedIn) {
                    authViewModel.signOut()
                    android.widget.Toast.makeText(context, "Logged out successfully", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    showAuthDialog = true
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsHeader(title = "Trailer Settings")
        SettingsToggleRow(
            icon = Icons.Filled.PlayCircle,
            title = "Autoplay Trailers",
            description = "Automatically start trailer videos",
            checked = autoplayTrailers,
            onCheckedChange = {
                autoplayTrailers = it
                preferencesManager.autoplayTrailers = it
            }
        )
        SettingsToggleRow(
            icon = Icons.Filled.Tune,
            title = "Show Player Controls",
            description = "Show seekbar and volume controls in the player",
            checked = showPlayerControls,
            onCheckedChange = {
                showPlayerControls = it
                preferencesManager.showPlayerControls = it
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsHeader(title = "Cache & Info")
        SettingsClickableRow(
            icon = Icons.Filled.DeleteSweep,
            title = "Clear Search History",
            value = "",
            onClick = { /* Clear logic */ }
        )
        SettingsClickableRow(
            icon = Icons.Filled.CloudDownload,
            title = if (isCheckingUpdates) "Checking for Updates..." else "Check for Updates",
            value = "",
            onClick = {
                if (!isCheckingUpdates) {
                    scope.launch {
                        isCheckingUpdates = true
                        val result = updateManager.checkForUpdates(isManual = true)
                        isCheckingUpdates = false
                        when (result) {
                            is UpdateState.UpToDate -> {
                                android.widget.Toast.makeText(context, "You are on the latest version!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            is UpdateState.Error -> {
                                android.widget.Toast.makeText(context, "Update check failed: ${result.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }
            }
        )
        SettingsClickableRow(
            icon = Icons.Filled.Info,
            title = "App Version",
            value = BuildConfig.VERSION_NAME,
            onClick = {}
        )
        SettingsClickableRow(
            icon = Icons.Filled.BugReport,
            title = "Build Code",
            value = BuildConfig.VERSION_CODE.toString(),
            onClick = {}
        )

        Spacer(modifier = Modifier.height(80.dp)) // Offset bottom bar
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = FlixoraCyan,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FlixoraWhite80,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = FlixoraWhite
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = FlixoraWhite60
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FlixoraCyan,
                checkedTrackColor = FlixoraCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = FlixoraWhite60,
                uncheckedTrackColor = FlixoraDarkSurface
            )
        )
    }
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FlixoraWhite80,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = FlixoraWhite,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = FlixoraCyan,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = FlixoraWhite40
        )
    }
}
