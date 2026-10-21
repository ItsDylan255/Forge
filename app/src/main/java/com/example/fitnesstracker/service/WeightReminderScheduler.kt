package com.example.fitnesstracker.service

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.fitnesstracker.data.local.ReminderPreferences
import com.example.fitnesstracker.domain.reminder.ReminderCalculator
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Plant die nächste Gewicht-Erinnerung. Jeder Lauf plant den nächsten Tag selbst, so bleibt die
 * Uhrzeit genau. Mehrfaches Aufrufen ist harmlos, es gibt immer nur einen geplanten Lauf.
 */
object WeightReminderScheduler {

    private const val WORK_NAME = "weight_reminder"

    fun schedule(context: Context) {
        val app = context.applicationContext
        val prefs = ReminderPreferences(app)
        val workManager = WorkManager.getInstance(app)

        if (!prefs.isEnabled()) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val delay = ReminderCalculator.delayMillis(ZonedDateTime.now(), prefs.getHour(), prefs.getMinute())
        val request = OneTimeWorkRequestBuilder<WeightReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
