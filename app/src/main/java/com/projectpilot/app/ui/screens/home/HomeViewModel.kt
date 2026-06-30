package com.projectpilot.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectpilot.app.data.repository.ProjectRepository
import com.projectpilot.app.domain.model.Project
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val projects: List<Project> = emptyList(),
    val query: String = "",
    val loading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: ProjectRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val state: StateFlow<HomeUiState> =
        combine(repo.observeAll(), _query) { items, q ->
            val filtered = if (q.isBlank()) items
            else items.filter { it.name.contains(q, true) || it.path.contains(q, true) }
            HomeUiState(filtered, q, loading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setQuery(q: String) { _query.value = q }

    fun toggleFavorite(p: Project) = viewModelScope.launch {
        repo.update(p.copy(isFavorite = !p.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    fun delete(p: Project) = viewModelScope.launch { repo.delete(p) }
}
