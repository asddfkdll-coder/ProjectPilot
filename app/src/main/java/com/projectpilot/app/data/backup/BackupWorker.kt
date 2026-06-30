package com.projectpilot.app.data.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.projectpilot.app.data.repository.ProjectRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Periodic backup worker. Exports the project list + each project's encrypted .env
 * blob (already encrypted at rest) into the app's external files dir under /backups.
 *
 * We DON'T export to public storage automatically; the user can share/move backups
 * through the Settings screen.
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: ProjectRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = try {
        val projects = repo.observeAll().first()
        val dir = File(applicationContext.getExternalFilesDir(null), "backups").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "projectpilot_backup_$stamp.json")
        val json = Json { prettyPrint = true; encodeDefaults = true }
        // Project is a Room entity — serialize manually to avoid leaking @PrimaryKey internals.
        val snapshot = projects.map {
            BackupRecord(
                name = it.name, path = it.path, type = it.type.name,
                framework = it.framework, installCommand = it.installCommand,
                runCommand = it.runCommand, defaultPort = it.defaultPort,
                notes = it.notes, customCommands = it.customCommands
            )
        }
        file.writeText(json.encodeToString(BackupFile(stamp, snapshot)))
        // Rotate: keep at most 10 backups
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(10)
            ?.forEach { it.delete() }
        Result.success()
    } catch (t: Throwable) {
        Result.retry()
    }

    @kotlinx.serialization.Serializable
    data class BackupRecord(
        val name: String, val path: String, val type: String,
        val framework: String?, val installCommand: String?,
        val runCommand: String?, val defaultPort: Int?,
        val notes: String, val customCommands: String
    )
    @kotlinx.serialization.Serializable
    data class BackupFile(val createdAt: String, val projects: List<BackupRecord>)

    companion object {
        const val UNIQUE_NAME = "pp_periodic_backup"

        fun schedule(ctx: Context, intervalHours: Long = 24) {
            val req = PeriodicWorkRequestBuilder<BackupWorker>(intervalHours, TimeUnit.HOURS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, req
            )
        }

        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
