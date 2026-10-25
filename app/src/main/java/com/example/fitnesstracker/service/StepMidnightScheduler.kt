package com.example.fitnesstracker.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.fitnesstracker.domain.steps.StepDayBoundary
import java.time.ZonedDateTime

/**
 * Plant die Nachtmessung: Um Mitternacht liest ein genauer Alarm den Schrittzähler. Damit werden
 * die Schritte bis Mitternacht dem alten Tag gutgeschrieben, auch wenn Android die regelmäßigen
 * Messungen über Nacht verschiebt. Mehrfaches Aufrufen ist harmlos.
 */
object StepMidnightScheduler {

    private const val REQUEST_CODE = 3001

    fun schedule(context: Context) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(AlarmManager::class.java) ?: return

        val pending = PendingIntent.getBroadcast(
            app,
            REQUEST_CODE,
            Intent(app, StepMidnightReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val triggerAt = StepDayBoundary.nextMidnightMillis(ZonedDateTime.now())

        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (exactAllowed) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                // Ohne Erlaubnis für genaue Alarme: ungefährer Zeitpunkt
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }
}
