package com.projectpilot.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.projectpilot.app.domain.model.Project
import com.projectpilot.app.domain.model.ProjectType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    onOpen: (Long) -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ProjectPilot", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add project") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search projects…") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            if (state.loading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else if (state.projects.isEmpty()) {
                EmptyState(onAdd)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.projects, key = { it.id }) { p ->
                        ProjectCard(
                            project = p,
                            onClick = { onOpen(p.id) },
                            onToggleFavorite = { vm.toggleFavorite(p) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("No projects yet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Add a folder to start tracking it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAdd) { Text("Add project") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TypeBadge(project.type)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(project.framework ?: project.type.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(project.path, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline, maxLines = 1)
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (project.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (project.isFavorite)
                        MaterialTheme.colorScheme.tertiary else LocalContentColor.current
                )
            }
        }
    }
}

@Composable
private fun TypeBadge(type: ProjectType) {
    val (label, color) = when (type) {
        ProjectType.NODE -> "JS" to MaterialTheme.colorScheme.primary
        ProjectType.PYTHON -> "PY" to MaterialTheme.colorScheme.secondary
        ProjectType.PHP -> "PHP" to MaterialTheme.colorScheme.tertiary
        ProjectType.JAVA -> "JV" to MaterialTheme.colorScheme.primary
        ProjectType.GO -> "GO" to MaterialTheme.colorScheme.secondary
        ProjectType.RUST -> "RS" to MaterialTheme.colorScheme.tertiary
        ProjectType.DOCKER -> "DKR" to MaterialTheme.colorScheme.primary
        ProjectType.DOTNET -> ".NET" to MaterialTheme.colorScheme.secondary
        ProjectType.RUBY -> "RB" to MaterialTheme.colorScheme.tertiary
        ProjectType.STATIC_HTML -> "HTML" to MaterialTheme.colorScheme.outline
        ProjectType.UNKNOWN -> "?" to MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Box(Modifier.size(48.dp), Alignment.Center) {
            Text(label, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
