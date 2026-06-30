package com.projectpilot.app.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectpilot.app.data.backup.BackupWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class BackupItem(val name: String, val sizeKb: Long, val path: String)

data class SettingsUiState(
    val backupEnabled: Boolean = false,
    val backups: List<BackupItem> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val ctx: android.content.Context
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun toggleBackup(enabled: Boolean) {
        if (enabled) BackupWorker.schedule(ctx) else BackupWorker.cancel(ctx)
        _state.value = _state.value.copy(
            backupEnabled = enabled,
            message = if (enabled) "Auto-backup enabled (every 24h)" else "Auto-backup disabled"
        )
    }

    fun runBackupNow() = viewModelScope.launch {
        val req = androidx.work.OneTimeWorkRequestBuilder<BackupWorker>().build()
        androidx.work.WorkManager.getInstance(ctx).enqueue(req)
        _state.value = _state.value.copy(message = "Backup queued")
        kotlinx.coroutines.delay(1500)
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        val list = withContext(Dispatchers.IO) {
            val dir = File(ctx.getExternalFilesDir(null), "backups")
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.map {
                BackupItem(it.name, it.length() / 1024, it.absolutePath)
            } ?: emptyList()
        }
        _state.value = _state.value.copy(backups = list)
    }

    fun clearMsg() { _state.value = _state.value.copy(message = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); vm.clearMsg() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { p ->
        Column(
            Modifier.padding(p).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Auto-backup", fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium)
                    Text("Saves project list + encrypted .env every 24 hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Switch(checked = state.backupEnabled, onCheckedChange = vm::toggleBackup)
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(onClick = { vm.runBackupNow() }) { Text("Backup now") }
                    }
                }
            }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Backups (${state.backups.size})", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (state.backups.isEmpty()) {
                        Text("No backups yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.backups.forEach { b ->
                            ListItem(
                                headlineContent = { Text(b.name) },
                                supportingContent = { Text("${b.sizeKb} KB") },
                                trailingContent = {
                                    IconButton(onClick = { shareFile(ctx, File(b.path)) }) {
                                        Icon(Icons.Default.Share, null)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Termux setup", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("1. Install Termux from F-Droid (not Play Store).")
                    Text("2. In Termux:")
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            "echo 'allow-external-apps = true' >> ~/.termux/termux.properties\n" +
                                "termux-reload-settings",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Text("ProjectPilot v1.1.0",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun shareFile(ctx: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(intent, "Share backup"))
}
