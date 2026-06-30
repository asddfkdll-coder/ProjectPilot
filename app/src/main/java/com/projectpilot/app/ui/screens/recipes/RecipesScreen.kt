package com.projectpilot.app.ui.screens.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectpilot.app.data.recipes.CommandRecipes
import com.projectpilot.app.data.recipes.Recipe
import com.projectpilot.app.data.repository.ProjectRepository
import com.projectpilot.app.domain.model.Project
import com.projectpilot.app.termux.TermuxCommandRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipesUiState(
    val project: Project? = null,
    val recipes: List<Recipe> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val repo: ProjectRepository,
    private val termux: TermuxCommandRunner
) : ViewModel() {
    private val _state = MutableStateFlow(RecipesUiState())
    val state = _state.asStateFlow()

    fun load(projectId: Long) = viewModelScope.launch {
        val p = repo.getById(projectId) ?: return@launch
        _state.value = RecipesUiState(project = p, recipes = CommandRecipes.forType(p.type))
    }

    fun runRecipe(r: Recipe) = viewModelScope.launch {
        val p = _state.value.project ?: return@launch
        val workdir = if (r.needsProjectDir) p.path else TermuxCommandRunner.HOME
        val res = termux.run(shellLine = r.command, workdir = workdir, sessionLabel = r.title)
        _state.value = _state.value.copy(message = when (res) {
            TermuxCommandRunner.Result.Ok -> "▶ ${r.title} sent to Termux"
            TermuxCommandRunner.Result.TermuxNotInstalled -> "Termux not installed"
            is TermuxCommandRunner.Result.Failed -> "Failed: ${res.message}"
        })
    }

    fun clearMsg() { _state.value = _state.value.copy(message = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    projectId: Long,
    onBack: () -> Unit,
    vm: RecipesViewModel = hiltViewModel()
) {
    LaunchedEffect(projectId) { vm.load(projectId) }
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); vm.clearMsg() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Recipes · ${state.project?.name ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.recipes) { recipe ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(recipe.title, fontWeight = FontWeight.SemiBold)
                                Text(recipe.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FilledTonalIconButton(onClick = { vm.runRecipe(recipe) }) {
                                Icon(Icons.Default.PlayArrow, null)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                recipe.command,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
