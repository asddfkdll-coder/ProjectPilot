package com.projectpilot.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ProjectPilotApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITOR,
                "Server Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Tracks running servers' CPU & RAM" }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                "Server Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts when a server stops or crashes" }
        )
    }

    companion object {
        const val CHANNEL_MONITOR = "monitor_channel"
        const val CHANNEL_ALERTS = "alerts_channel"
    }
}
