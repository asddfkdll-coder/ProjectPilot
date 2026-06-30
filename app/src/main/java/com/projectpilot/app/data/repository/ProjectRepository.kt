package com.projectpilot.app.data.repository

import com.projectpilot.app.data.local.ProjectDao
import com.projectpilot.app.domain.model.Project
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val dao: ProjectDao
) {
    fun observeAll(): Flow<List<Project>> = dao.observeAll()
    suspend fun getById(id: Long): Project? = dao.getById(id)
    suspend fun getByPath(path: String): Project? = dao.getByPath(path)
    suspend fun upsert(project: Project): Long = dao.upsert(project)
    suspend fun update(project: Project) = dao.update(project)
    suspend fun delete(project: Project) = dao.delete(project)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
