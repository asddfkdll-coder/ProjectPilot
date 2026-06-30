package com.projectpilot.app.data.local

import androidx.room.*
import com.projectpilot.app.domain.model.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY isFavorite DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): Project?

    @Query("SELECT * FROM projects WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): Project?

    @Query("SELECT * FROM projects WHERE lastPid IS NOT NULL")
    suspend fun getActiveProjects(): List<Project>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)
}
