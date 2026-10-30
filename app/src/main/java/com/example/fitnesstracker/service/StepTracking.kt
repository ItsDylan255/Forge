package com.example.fitnesstracker.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import com.example.fitnesstracker.data.local.StepPreferences

/**
 * Startet oder stoppt das Schrittzählen im Hintergrund. Mehrfaches Aufrufen ist harmlos.
 * Aufgerufen beim Öffnen der App, nach einem Neustart und beim Umschalten in den Einstellungen.
 */
object StepTracking {

    /** Ohne die Berechtigung "Körperliche Aktivität" liefert der Sensor keine Werte. */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED

    fun start(context: Context) {
        val app = context.applicationContext

        // Der alte Hintergrund-Job aus früheren Versionen wird nicht mehr gebraucht
        WorkManager.getInstance(app).cancelUniqueWork("step_sync")

        if (StepPreferences(app).isBackgroundCountingEnabled() && hasPermission(app)) {
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

    /** Dienst neu starten: meldet den Sensor neu an und liefert sofort einen frischen Wert. */
    fun restart(context: Context) {
        stop(context)
        start(context)
    }
}
