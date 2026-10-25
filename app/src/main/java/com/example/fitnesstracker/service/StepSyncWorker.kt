package com.example.fitnesstracker.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.StepPreferences
import com.example.fitnesstracker.data.repository.StepRepository
import java.time.LocalDate

/**
 * Liest den Schrittzähler im Hintergrund (ca. alle 15 Minuten, Android darf es verschieben).
 * Der genaue Tageswechsel kommt von der Nachtmessung um Mitternacht (StepMidnightReceiver).
 */
class StepSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = StepPreferences(context)

        if (!StepSensorReader.hasPermission(context)) {
            prefs.appendReadLog("Hintergrund", "Keine Berechtigung \u201EK\u00F6rperliche Aktivit\u00E4t\u201C")
            return Result.success()
        }

        val total = StepSensorReader.readTotal(context)
        if (total == null) {
            prefs.appendReadLog("Hintergrund", "Der Sensor lieferte keinen Wert")
            return Result.success()
        }

        val repository = StepRepository(
            dao = DatabaseProvider.getDatabase(context).stepEntryDao(),
            prefs = prefs
        )
        repository.recordSensorTotal(total, LocalDate.now().toEpochDay(), "Hintergrund")

        return Result.success()
    }
}
