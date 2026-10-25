package com.example.fitnesstracker.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import com.example.fitnesstracker.data.local.StepPreferences

/**
 * Startet oder stoppt das Schrittzählen im Hintergrund. Mehrfaches Aufrufen ist harmlos.
 * Aufgerufen beim Öffnen der App, nach einem Neustart und beim Umschalten in den Einstellungen.
 */
object StepTracking {

    fun start(context: Context) {
        val app = context.applicationContext
        val prefs = StepPreferences(app)

        // Der alte Hintergrund-Job bekommt auf manchen Handys nie einen Sensorwert und wird nicht mehr gebraucht
        WorkManager.getInstance(app).cancelUniqueWork("step_sync")

        // Einmalig: alten Rohwert verwerfen (siehe StepPreferences)
        prefs.resetBaselineOnce()

        // Genaue Messung um Mitternacht, falls der Dienst aus ist
        StepMidnightScheduler.schedule(app)

        if (prefs.isBackgroundCountingEnabled() && StepSensorReader.hasPermission(app)) {
            try {
                ContextCompat.startForegroundService(app, Intent(app, StepCounterService::class.java))
            } catch (_: Exception) {
                // Läuft die App im Hintergrund, darf Android den Dienst nicht starten. Beim nächsten Öffnen klappt es.
            }
        } else {
            stop(app)
        }
    }

    fun stop(context: Context) {
        val app = context.applicationContext
        app.stopService(Intent(app, StepCounterService::class.java))
    }
}
