package com.hexaghost.flixora.presentation.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexaghost.flixora.domain.model.InstalledProvider
import com.hexaghost.flixora.domain.model.ProviderInfo
import com.hexaghost.flixora.domain.model.Repository
import com.hexaghost.flixora.ui.theme.*

@Composable
fun ProvidersScreen(
    viewModel: ProviderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Repositories", "Installed")

    LaunchedEffect(Unit) {
        viewModel.refresh()
        viewModel.loadAllRepositories()
    }

    if (showAddDialog) {
        AddRepositoryDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { url -> viewModel.addRepository(url) }
        )
    }

    // Snackbars
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = FlixoraDarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(FlixoraDarkBg)
                .statusBarsPadding()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(FlixoraDarkSurface, FlixoraDarkBg))
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Providers",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = FlixoraWhite
                        )
                        Text(
                            text = "${uiState.installedProviders.size} installed · ${uiState.savedRepositoryUrls.size} repos",
                            style = MaterialTheme.typography.bodySmall,
                            color = FlixoraWhite60
                        )
                    }
                    // Add repo FAB
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = FlixoraCyan,
                        contentColor = Color.Black,
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Repository", modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = FlixoraDarkBg,
                contentColor = FlixoraCyan,
                indicator = { tabPositions ->
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(2.dp)
                            .background(FlixoraCyan)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (selectedTab == index) FlixoraCyan else FlixoraWhite60
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> RepositoriesTab(uiState, viewModel)
                1 -> InstalledTab(uiState, viewModel)
            }
        }
    }
}

// ─── Repositories Tab ─────────────────────────────────────────────────────────

@Composable
private fun RepositoriesTab(
    uiState: ProvidersUiState,
    viewModel: ProviderViewModel
) {
    if (uiState.isLoadingRepo) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FlixoraCyan)
        }
        return
    }

    if (uiState.savedRepositoryUrls.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Storage,
            title = "No repositories added",
            subtitle = "Tap + to add a provider repository"
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        uiState.savedRepositoryUrls.forEach { url ->
            val repo = uiState.fetchedRepositories[url]
            item(key = url) {
                RepositoryCard(
                    url = url,
                    repo = repo,
                    installedIds = uiState.installedProviders.map { it.id }.toSet(),
                    isInstallingId = uiState.isInstallingId,
                    onInstall = viewModel::installProvider,
                    onRemoveRepo = { viewModel.removeRepository(url) }
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun RepositoryCard(
    url: String,
    repo: Repository?,
    installedIds: Set<String>,
    isInstallingId: String?,
    onInstall: (ProviderInfo) -> Unit,
    onRemoveRepo: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FlixoraDarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.linearGradient(listOf(FlixoraPurple, FlixoraCyan)),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Storage,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = repo?.name ?: "Loading...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = FlixoraWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = FlixoraWhite40,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = FlixoraWhite60
                    )
                }
                IconButton(onClick = onRemoveRepo) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = Color(0xFFFF385C))
                }
            }

            if (repo != null && expanded) {
                if (repo.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = repo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = FlixoraWhite60
                    )
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0x1AFFFFFF))
                Spacer(Modifier.height(12.dp))

                if (repo.providers.isEmpty()) {
                    Text(
                        "No providers in this repository",
                        style = MaterialTheme.typography.bodySmall,
                        color = FlixoraWhite40
                    )
                } else {
                    repo.providers.forEach { provider ->
                        ProviderListItem(
                            provider = provider,
                            isInstalled = provider.id in installedIds,
                            isInstalling = isInstallingId == provider.id,
                            onInstall = { onInstall(provider) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderListItem(
    provider: ProviderInfo,
    isInstalled: Boolean,
    isInstalling: Boolean,
    onInstall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(FlixoraDarkBg)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (isInstalled) FlixoraCyan.copy(alpha = 0.15f) else FlixoraDarkElevated,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isInstalled) Icons.Filled.Check else Icons.Filled.Extension,
                contentDescription = null,
                tint = if (isInstalled) FlixoraCyan else FlixoraWhite60,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = provider.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = FlixoraWhite
            )
            Text(
                text = "v${provider.version}",
                style = MaterialTheme.typography.labelSmall,
                color = FlixoraWhite40
            )
        }
        if (isInstalling) {
            CircularProgressIndicator(color = FlixoraCyan, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else if (!isInstalled) {
            Button(
                onClick = onInstall,
                colors = ButtonDefaults.buttonColors(containerColor = FlixoraCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Install", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        } else {
            Surface(
                color = FlixoraCyan.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Installed",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = FlixoraCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ─── Installed Tab ────────────────────────────────────────────────────────────

@Composable
private fun InstalledTab(
    uiState: ProvidersUiState,
    viewModel: ProviderViewModel
) {
    if (uiState.installedProviders.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Extension,
            title = "No providers installed",
            subtitle = "Go to Repositories tab to install providers"
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(uiState.installedProviders, key = { it.id }) { provider ->
            InstalledProviderCard(
                provider = provider,
                onToggle = { enabled -> viewModel.toggleProvider(provider.id, enabled) },
                onUninstall = { viewModel.uninstallProvider(provider.id) }
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun InstalledProviderCard(
    provider: InstalledProvider,
    onToggle: (Boolean) -> Unit,
    onUninstall: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = FlixoraDarkSurface,
            title = { Text("Uninstall Provider", color = FlixoraWhite, fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"${provider.name}\"? This cannot be undone.", color = FlixoraWhite80) },
            confirmButton = {
                Button(
                    onClick = { onUninstall(); showConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF385C))
                ) { Text("Uninstall") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel", color = FlixoraWhite60) }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (provider.isEnabled) FlixoraDarkSurface else FlixoraDarkBg
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (provider.isEnabled) FlixoraCyan.copy(alpha = 0.2f) else Color(0x1AFFFFFF)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (provider.isEnabled)
                            Brush.linearGradient(listOf(FlixoraCyan.copy(0.3f), FlixoraPurple.copy(0.3f)))
                        else Brush.linearGradient(listOf(FlixoraDarkElevated, FlixoraDarkElevated)),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Extension,
                    contentDescription = null,
                    tint = if (provider.isEnabled) FlixoraCyan else FlixoraWhite40,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (provider.isEnabled) FlixoraWhite else FlixoraWhite60
                )
                Text(
                    text = "v${provider.version}",
                    style = MaterialTheme.typography.labelSmall,
                    color = FlixoraWhite40,
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = provider.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = FlixoraCyan,
                    checkedTrackColor = FlixoraCyan.copy(alpha = 0.3f),
                    uncheckedThumbColor = FlixoraWhite60,
                    uncheckedTrackColor = FlixoraDarkSurface
                )
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Uninstall", tint = Color(0xFFFF385C).copy(0.7f))
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Brush.linearGradient(listOf(FlixoraPurple.copy(0.2f), FlixoraCyan.copy(0.2f))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = FlixoraWhite60, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = FlixoraWhite)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = FlixoraWhite60)
        }
    }
}
