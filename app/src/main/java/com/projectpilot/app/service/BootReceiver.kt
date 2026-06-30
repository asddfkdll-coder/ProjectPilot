package com.projectpilot.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Don't auto-start servers; just re-arm the monitor service.
            ServerMonitorService.start(context)
        }
    }
}
