package com.projectpilot.app.ui.screens.detail

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: Long,
    onBack: () -> Unit,
    onOpenGit: (Long) -> Unit,
    onOpenRecipes: (Long) -> Unit,
    vm: ProjectDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(projectId) { vm.load(projectId) }
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(state.project?.name ?: "Project") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenRecipes(projectId) }) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Recipes")
                    }
                    IconButton(onClick = { onOpenGit(projectId) }) {
                        Icon(Icons.Default.AccountTree, contentDescription = "Git")
                    }
                }
            )
        }
    ) { padding ->
        val p = state.project ?: return@Scaffold
        var env by remember(state.env) { mutableStateOf(state.env) }
        var custom by remember { mutableStateOf("") }

        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Run table", fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Type", p.type.name)
                    InfoRow("Framework", p.framework ?: "—")
                    InfoRow("Default port", p.defaultPort?.toString() ?: "—")
                    InfoRow("Install", p.installCommand ?: "—")
                    InfoRow("Run", p.runCommand ?: "—")
                    InfoRow("Path", p.path)
                    if (p.notes.isNotBlank()) InfoRow("Notes", p.notes)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.runInstall() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("Install")
                }
                Button(onClick = { vm.runServer() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Start")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onOpenRecipes(projectId) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.MenuBook, null); Spacer(Modifier.width(6.dp)); Text("Recipes")
                }
                OutlinedButton(onClick = { onOpenGit(projectId) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.AccountTree, null); Spacer(Modifier.width(6.dp)); Text("Git")
                }
            }

            OutlinedTextField(
                value = custom, onValueChange = { custom = it },
                label = { Text("Custom Termux command") },
                placeholder = { Text("e.g. pkg install nodejs && npm install") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { vm.runCustom(custom); custom = "" },
                enabled = custom.isNotBlank()
            ) { Icon(Icons.Default.Terminal, null); Spacer(Modifier.width(6.dp)); Text("Run in Termux") }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(".env (encrypted)", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = env, onValueChange = { env = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        placeholder = { Text("KEY=VALUE\nDATABASE_URL=...") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.saveEnv(env) }) { Text("Save securely") }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label  ", color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f))
        Text(value, modifier = Modifier.weight(0.65f))
    }
}
