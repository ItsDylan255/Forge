package com.example.fitnesstracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.StepPreferences
import com.example.fitnesstracker.data.repository.StepRepository
import com.example.fitnesstracker.domain.steps.StepDayBoundary
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Wird um Mitternacht vom Alarm gestartet: liest den Schrittzähler und plant die nächste Nacht. */
class StepMidnightReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext

        // Die nächste Nacht gleich wieder planen, falls unten etwas schiefgeht
        StepMidnightScheduler.schedule(app)

        // Läuft der Zähl-Dienst, übernimmt er die Nachtmessung (er hat den Sensor ohnehin angemeldet)
        if (StepCounterService.running) {
            try {
                app.startService(
                    Intent(app, StepCounterService::class.java).setAction(StepCounterService.ACTION_MIDNIGHT)
                )
                return
            } catch (_: Exception) {
                // Weiter mit der einmaligen Messung unten
            }
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val prefs = StepPreferences(app)
                if (!StepSensorReader.hasPermission(app)) {
                    prefs.appendReadLog("Nacht", "Keine Berechtigung \u201EK\u00F6rperliche Aktivit\u00E4t\u201C")
                } else {
                    val total = StepSensorReader.readTotal(app)
                    if (total == null) {
                        prefs.appendReadLog("Nacht", "Der Sensor lieferte keinen Wert")
                    } else {
                        val repository = StepRepository(
                            dao = DatabaseProvider.getDatabase(app).stepEntryDao(),
                            prefs = prefs
                        )
                        // Die Schritte seit dem letzten Lesen gehören zum Tag, der gerade endet
                        repository.recordSensorTotal(
                            total,
                            StepDayBoundary.dayToCredit(ZonedDateTime.now()),
                            "Nacht"
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
