package com.example.fitnesstracker.service

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.fitnesstracker.data.local.PhotoReminderPreferences
import com.example.fitnesstracker.domain.photos.PhotoSchedule
import com.example.fitnesstracker.domain.reminder.ReminderCalculator
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Plant die tägliche Prüfung, ob ein Fortschrittsfoto fällig ist. Wie bei der Gewicht-Erinnerung
 * plant jeder Lauf den nächsten Tag selbst.
 */
object PhotoReminderScheduler {

    private const val WORK_NAME = "photo_reminder"

    fun schedule(context: Context) {
        val app = context.applicationContext
        val prefs = PhotoReminderPreferences(app)
        val workManager = WorkManager.getInstance(app)

        if (prefs.getSchedule() == PhotoSchedule.Off) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val delay = ReminderCalculator.delayMillis(ZonedDateTime.now(), prefs.getHour(), prefs.getMinute())
        val request = OneTimeWorkRequestBuilder<PhotoReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
