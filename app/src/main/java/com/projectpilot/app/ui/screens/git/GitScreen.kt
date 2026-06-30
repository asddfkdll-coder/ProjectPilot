package com.projectpilot.app.ui.screens.git

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectpilot.app.data.git.GitInfoReader
import com.projectpilot.app.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class GitUiState(
    val loading: Boolean = true,
    val projectName: String = "",
    val info: GitInfoReader.GitInfo? = null
)

@HiltViewModel
class GitViewModel @Inject constructor(
    private val repo: ProjectRepository,
    private val gitReader: GitInfoReader
) : ViewModel() {
    private val _state = MutableStateFlow(GitUiState())
    val state = _state.asStateFlow()

    fun load(projectId: Long) = viewModelScope.launch {
        val p = repo.getById(projectId) ?: return@launch
        _state.value = GitUiState(loading = true, projectName = p.name)
        val info = gitReader.read(File(p.path))
        _state.value = _state.value.copy(loading = false, info = info)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitScreen(
    projectId: Long,
    onBack: () -> Unit,
    vm: GitViewModel = hiltViewModel()
) {
    LaunchedEffect(projectId) { vm.load(projectId) }
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Git · ${state.projectName}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator()
                state.info?.isGitRepo == false ->
                    Text("This folder is not a Git repository.",
                        style = MaterialTheme.typography.bodyLarge)
                state.info != null -> GitInfoCard(state.info!!)
            }
        }
    }
}

@Composable
private fun GitInfoCard(info: GitInfoReader.GitInfo) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountTree, null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(info.branch ?: "—", fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleLarge)
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth()) {
                Text("Last commit", modifier = Modifier.weight(0.35f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(0.65f)) {
                    Text(info.lastCommitShort ?: "—",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold)
                    info.lastCommitMessage?.let { Text(it.trim()) }
                    info.lastCommitAuthor?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    info.lastCommitEpoch?.let {
                        Text(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth()) {
                Text("Changed (approx.)", modifier = Modifier.weight(0.35f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                AssistChip(
                    onClick = {},
                    label = { Text("${info.changedFilesApprox} file(s)") }
                )
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth()) {
                Text("Remote", modifier = Modifier.weight(0.35f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(info.remoteUrl ?: "—", modifier = Modifier.weight(0.65f),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
