package com.example.fitnesstracker.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.fitnesstracker.data.local.StepPreferences
import java.util.concurrent.TimeUnit

/** Startet das regelmäßige Lesen des Schrittzählers. Mehrfaches Aufrufen ist harmlos. */
object StepSyncScheduler {

    private const val WORK_NAME = "step_sync"

    fun schedule(context: Context) {
        val app = context.applicationContext

        // Einmalig: alten Rohwert verwerfen, damit nicht alle Schritte seit dem letzten Öffnen
        // des Screens dem heutigen Tag gutgeschrieben werden
        StepPreferences(app).resetBaselineOnce()

        val request = PeriodicWorkRequestBuilder<StepSyncWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
