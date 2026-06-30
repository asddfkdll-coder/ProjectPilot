package com.projectpilot.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class ProjectType {
    NODE, PYTHON, PHP, JAVA, GO, RUST, DOCKER, DOTNET, RUBY, STATIC_HTML, UNKNOWN
}

/**
 * Per-project metadata. Sensitive values (.env) are stored encrypted separately.
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val path: String,
    val type: ProjectType,
    val framework: String? = null,
    val installCommand: String? = null,
    val runCommand: String? = null,
    val customCommands: String = "",   // JSON array of custom Termux commands
    val defaultPort: Int? = null,
    val dependenciesJson: String = "[]",
    val notes: String = "",
    val sizeBytes: Long = 0L,
    val isFavorite: Boolean = false,
    val lastRunAt: Long? = null,
    val lastPid: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class CustomCommand(
    val title: String,
    val command: String,
    val workdir: String? = null,
    val background: Boolean = false
)

@Serializable
data class DetectionResult(
    val type: ProjectType,
    val framework: String? = null,
    val installCommand: String? = null,
    val runCommand: String? = null,
    val defaultPort: Int? = null,
    val dependencies: List<String> = emptyList(),
    val notes: String = ""
)
