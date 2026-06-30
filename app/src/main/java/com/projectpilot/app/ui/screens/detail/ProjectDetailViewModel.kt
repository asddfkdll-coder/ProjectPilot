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

    fun runInstall() = act { p -> 
        termux.runInstall(p.path, p.installCommand ?: "echo 'No install command'") 
    }

    fun runServer() = act { p ->
        // To capture PID, we need to use the base run method with a receiver
        termux.run(
            shellLine = p.runCommand ?: "echo 'No run command'",
            workdir = p.path,
            background = false,
            sessionLabel = p.name,
            onResult = { resultCode, data ->
                val pid = data?.getInt("stdout") // Termux can be configured to return PID in stdout for background or via special scripts
                // Note: Termux RUN_COMMAND doesn't natively return PID in a simple way for foreground tasks.
                // This is a placeholder for the logic to handle the result.
                if (resultCode == 0) {
                    // Update project lastRunAt
                    viewModelScope.launch {
                        repo.update(p.copy(lastRunAt = System.currentTimeMillis()))
                    }
                }
            }
        )
    }

    fun runCustom(cmd: String) = act { p -> termux.run(shellLine = cmd, workdir = p.path, sessionLabel = p.name) }

    fun deleteProject() = viewModelScope.launch {
        val p = _state.value.project ?: return@launch
        repo.delete(p)
        crypto.clearEnv(p.id)
    }

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
