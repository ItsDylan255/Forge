package com.example.fitnesstracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Nach einem Neustart oder App-Update: Schritt-Dienst und Nachtmessung werden wieder gestartet. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                StepTracking.start(context)
        }
    }
}
