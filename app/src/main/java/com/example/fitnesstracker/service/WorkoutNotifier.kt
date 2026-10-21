package com.example.fitnesstracker.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Schnittstelle für die Bildschirme: startet, aktualisiert oder stoppt die Trainings-Benachrichtigung. */
object WorkoutNotifier {

    /**
     * [sessionName] null = kein Training läuft, der Dienst wird beendet.
     * Die Pause ist null, wenn gerade keine läuft.
     */
    fun sync(
        context: Context,
        sessionName: String?,
        startedAt: Long?,
        restLabel: String?,
        restEndsAt: Long?,
        restTotalMillis: Long?
    ) {
        val app = context.applicationContext

        if (sessionName == null || startedAt == null) {
            app.stopService(Intent(app, WorkoutService::class.java))
            return
        }

        val intent = Intent(app, WorkoutService::class.java).apply {
            putExtra(WorkoutService.EXTRA_SESSION_NAME, sessionName)
            putExtra(WorkoutService.EXTRA_STARTED_AT, startedAt)
            putExtra(WorkoutService.EXTRA_REST_LABEL, restLabel.orEmpty())
            putExtra(WorkoutService.EXTRA_REST_ENDS_AT, restEndsAt ?: 0L)
            putExtra(WorkoutService.EXTRA_REST_TOTAL, restTotalMillis ?: 0L)
        }

        try {
            ContextCompat.startForegroundService(app, intent)
        } catch (_: Exception) {
            // Läuft die App im Hintergrund, darf Android den Dienst nicht neu starten.
            // Das Training selbst ist davon nicht betroffen.
        }
    }
}
