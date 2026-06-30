package com.projectpilot.app.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.projectpilot.app.ProjectPilotApp
import com.projectpilot.app.R
import com.projectpilot.app.data.repository.ProjectRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

/**
 * Foreground service that reads /proc/[pid]/stat to monitor CPU/RAM of tracked PIDs.
 * NOTE: On non-rooted Android we can only inspect our own PIDs; Termux PIDs belong to
 * another UID and are typically NOT readable via /proc. We still publish a heartbeat
 * notification and let the user act manually. Future: bind to Termux via IPC.
 */
@AndroidEntryPoint
class ServerMonitorService : Service() {

    @Inject lateinit var repo: ProjectRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tracked = mutableSetOf<Int>()
    private var loopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Monitoring..."))
        
        intent?.getIntExtra(EXTRA_ADD_PID, -1)?.takeIf { it > 0 }?.let { tracked.add(it) }
        intent?.getIntExtra(EXTRA_REMOVE_PID, -1)?.takeIf { it > 0 }?.let { tracked.remove(it) }
        
        if (loopJob == null || loopJob?.isActive == false) {
            loopJob = scope.launch { 
                // Initialize from DB on first run
                if (tracked.isEmpty()) {
                    val active = repo.getActiveProjects()
                    active.forEach { it.lastPid?.let { pid -> tracked.add(pid) } }
                }
                loop() 
            }
        }
        
        return START_STICKY
    }

    private suspend fun loop() {
        while (currentCoroutineContext().isActive) {
            // Filter alive PIDs. Note: /proc access limitation on Android 7+
            val alive = tracked.filter { pid ->
                try {
                    File("/proc/$pid").exists()
                } catch (e: Exception) {
                    // On some devices/OS versions, we might get permission denied instead of exists=false
                    false 
                }
            }.toSet()
            
            val died = tracked - alive
            tracked.clear()
            tracked.addAll(alive)
            
            updateNotification("Monitoring ${alive.size} server(s)" +
                if (died.isNotEmpty()) " · ${died.size} stopped" else "")
            
            delay(5_000)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, ProjectPilotApp.CHANNEL_MONITOR)
            .setContentTitle("ProjectPilot")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_monitor)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val EXTRA_ADD_PID = "extra_add_pid"
        const val EXTRA_REMOVE_PID = "extra_remove_pid"

        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, ServerMonitorService::class.java))
        }
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, ServerMonitorService::class.java))
        }
    }
}
