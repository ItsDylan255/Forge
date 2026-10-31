package com.example.fitnesstracker.watch

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status

/**
 * Hinweis während eines Trainings: ein Symbol auf dem Zifferblatt und in der App-Liste, über das man
 * jederzeit zur Trainingsansicht zurückkommt. Zeigt die Übung oder die laufende Pause.
 */
object WorkoutOngoing {

    private const val CHANNEL_ID = "workout"
    private const val NOTIFICATION_ID = 2001

    fun update(context: Context, state: WorkoutState?) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (state == null) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Training", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Laufendes Training vom Handy"
                setShowBadge(false)
            }
        )

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val now = System.currentTimeMillis()
        val text = state.exerciseName?.let { name -> listOfNotNull(name, state.setText).joinToString(" · ") }
            ?: state.routineName

        // Die Zeitangaben laufen auf der Uhr selbst weiter (Basis: Zeit seit dem Einschalten)
        val status = if (state.restRunning(now)) {
            Status.Builder()
                .addTemplate("Pause #rest#")
                .addPart("rest", Status.TimerPart(SystemClock.elapsedRealtime() + (state.restEndsAt - now)))
                .build()
        } else {
            Status.Builder()
                .addTemplate("#time# · $text")
                .addPart("time", Status.StopwatchPart(SystemClock.elapsedRealtime() - (now - state.startedAt)))
                .build()
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_workout)
            .setContentTitle(state.routineName)
            .setContentText(text)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)

        OngoingActivity.Builder(context, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_stat_workout)
            .setTouchIntent(open)
            .setStatus(status)
            .build()
            .apply(context)

        manager.notify(NOTIFICATION_ID, builder.build())
    }
}
