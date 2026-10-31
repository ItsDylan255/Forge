package com.example.fitnesstracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fitnesstracker.MainActivity
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.ReminderPreferences
import com.example.fitnesstracker.domain.reminder.ReminderCalculator
import com.example.fitnesstracker.i18n.tr
import java.time.LocalDate

/** Zeigt die Erinnerung "Gewicht eintragen", falls für heute noch kein Eintrag existiert. */
class WeightReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext

        if (!ReminderPreferences(context).isEnabled()) return Result.success()

        val today = LocalDate.now().toEpochDay()
        val hasEntryToday = DatabaseProvider.getDatabase(context)
            .weightEntryDao()
            .getEntryForDate(today) != null

        if (ReminderCalculator.shouldRemind(hasEntryToday)) showNotification(context)

        // Nächste Erinnerung für morgen planen
        WeightReminderScheduler.schedule(context)

        return Result.success()
    }

    private fun showNotification(context: Context) {
        // Ohne Erlaubnis (Android 13 und höher) bleibt die Erinnerung stumm
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, tr("Erinnerungen", "Reminders"), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = tr("Tägliche Erinnerung, das Gewicht einzutragen", "Daily reminder to log your weight")
            }
        )

        val open = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_ROUTE, "weight")
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_workout)
            .setContentTitle(tr("Gewicht eintragen", "Log weight"))
            .setContentText(tr("Du hast heute noch kein Gewicht eingetragen.", "You haven't logged your weight today."))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "weight_reminder"
        const val NOTIFICATION_ID = 2001
        const val REQUEST_CODE = 2
    }
}
