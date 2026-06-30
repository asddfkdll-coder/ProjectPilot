package com.projectpilot.app.ui.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectpilot.app.data.repository.ProjectRepository
import com.projectpilot.app.data.scanner.ProjectScanner
import com.projectpilot.app.data.scanner.ScanItem
import com.projectpilot.app.domain.model.Project
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AddUiState(
    val scanning: Boolean = false,
    val items: List<ScanItem> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class AddProjectViewModel @Inject constructor(
    private val scanner: ProjectScanner,
    private val repo: ProjectRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddUiState())
    val state = _state.asStateFlow()

    fun addSingle(path: String, onAdded: () -> Unit) = viewModelScope.launch {
        val dir = File(path)
        if (!dir.isDirectory) { _state.value = _state.value.copy(message = "Invalid folder"); return@launch }
        val det = scanner.detectSingle(dir)
        repo.upsert(
            Project(
                name = dir.name,
                path = dir.absolutePath,
                type = det.type,
                framework = det.framework,
                installCommand = det.installCommand,
                runCommand = det.runCommand,
                defaultPort = det.defaultPort,
                notes = det.notes
            )
        )
        onAdded()
    }

    fun scanTree(path: String) = viewModelScope.launch {
        _state.value = _state.value.copy(scanning = true, items = emptyList(), message = null)
        val dir = File(path)
        if (!dir.isDirectory) {
            _state.value = AddUiState(message = "Invalid root folder"); return@launch
        }
        val list = scanner.scanTree(dir)
        _state.value = AddUiState(items = list,
            message = if (list.isEmpty()) "No projects found" else "Found ${list.size} project(s)")
    }

    fun importAll() = viewModelScope.launch {
        val list = _state.value.items
        list.forEach { item ->
            if (repo.getByPath(item.dir.absolutePath) == null) {
                repo.upsert(
                    Project(
                        name = item.dir.name,
                        path = item.dir.absolutePath,
                        type = item.result.type,
                        framework = item.result.framework,
                        installCommand = item.result.installCommand,
                        runCommand = item.result.runCommand,
                        defaultPort = item.result.defaultPort,
                        notes = item.result.notes
                    )
                )
            }
        }
        _state.value = _state.value.copy(message = "Imported ${list.size} project(s)")
    }
}
