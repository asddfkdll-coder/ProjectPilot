package com.projectpilot.app.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectpilot.app.data.repository.ProjectRepository
import com.projectpilot.app.domain.model.Project
import com.projectpilot.app.security.EncryptionManager
import com.projectpilot.app.termux.TermuxCommandRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val project: Project? = null,
    val env: String = "",
    val message: String? = null
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val repo: ProjectRepository,
    private val termux: TermuxCommandRunner,
    private val crypto: EncryptionManager
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state = _state.asStateFlow()

    fun load(id: Long) = viewModelScope.launch {
        val p = repo.getById(id) ?: return@launch
        _state.value = DetailUiState(project = p, env = crypto.getEnv(id).orEmpty())
    }

    fun saveEnv(env: String) = viewModelScope.launch {
        val id = _state.value.project?.id ?: return@launch
        crypto.putEnv(id, env)
        _state.value = _state.value.copy(env = env, message = "Saved .env (encrypted)")
    }

    fun runInstall() = act { p -> termux.runInstall(p.path, p.installCommand ?: "echo 'No install command'") }
    fun runServer() = act { p -> termux.runServer(p.path, p.runCommand ?: "echo 'No run command'", p.name) }
    fun runCustom(cmd: String) = act { p -> termux.run(shellLine = cmd, workdir = p.path, sessionLabel = p.name) }

    private fun act(block: (Project) -> TermuxCommandRunner.Result) = viewModelScope.launch {
        val p = _state.value.project ?: return@launch
        val r = block(p)
        val msg = when (r) {
            TermuxCommandRunner.Result.Ok -> "Sent to Termux"
            TermuxCommandRunner.Result.TermuxNotInstalled -> "Termux not installed (install via F-Droid)"
            is TermuxCommandRunner.Result.Failed -> "Failed: ${r.message}"
        }
        _state.value = _state.value.copy(message = msg)
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }
}
