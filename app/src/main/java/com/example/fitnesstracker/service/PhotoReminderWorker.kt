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
import com.example.fitnesstracker.data.local.PhotoReminderPreferences
import com.example.fitnesstracker.domain.photos.PhotoReminderCalculator
import com.example.fitnesstracker.domain.photos.PhotoSchedule
import com.example.fitnesstracker.i18n.tr
import java.time.LocalDate

/** Erinnert an das Fortschrittsfoto, wenn es heute fällig oder schon überfällig ist. */
class PhotoReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = PhotoReminderPreferences(context)
        val schedule = prefs.getSchedule()
        if (schedule == PhotoSchedule.Off) return Result.success()

        val lastPhoto = DatabaseProvider.getDatabase(context).progressPhotoDao().lastPhotoDay()
        val due = PhotoReminderCalculator.status(schedule, lastPhoto, prefs.getAnchorDay(), LocalDate.now().toEpochDay())

        if (PhotoReminderCalculator.shouldRemind(due)) showNotification(context, overdue = due?.isOverdue == true)

        PhotoReminderScheduler.schedule(context)
        return Result.success()
    }

    private fun showNotification(context: Context, overdue: Boolean) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, tr("Fortschrittsfotos", "Progress photos"), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = tr("Erinnerung an dein nächstes Fortschrittsfoto", "Reminder for your next progress photo")
            }
        )

        val open = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_ROUTE, MainActivity.ROUTE_PHOTOS)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_workout)
            .setContentTitle(
                if (overdue) tr("Fortschrittsfoto überfällig", "Progress photo overdue")
                else tr("Zeit für dein Fortschrittsfoto", "Time for your progress photo")
            )
            .setContentText(tr("Mach ein Foto im Gym-Tab unter „Fotos“.", "Take one in the Gym tab under “Photos”."))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "photo_reminder"
        const val NOTIFICATION_ID = 2003
        const val REQUEST_CODE = 3
    }
}
