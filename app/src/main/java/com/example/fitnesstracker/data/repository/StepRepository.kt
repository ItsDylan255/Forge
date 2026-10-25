package com.example.fitnesstracker.data.repository

import com.example.fitnesstracker.data.local.StepEntry
import com.example.fitnesstracker.data.local.StepEntryDao
import com.example.fitnesstracker.data.local.StepPreferences
import com.example.fitnesstracker.domain.steps.StepDeltaCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StepRepository(
    private val dao: StepEntryDao,
    private val prefs: StepPreferences
) {
    private companion object {
        // Sensorwerte werden nacheinander verarbeitet, damit keine Schritte doppelt zählen.
        // Die Sperre gilt für die ganze App, auch für das Lesen im Hintergrund.
        val mutex = Mutex()
        val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.")
    }

    private val _stepGoal = MutableStateFlow(prefs.getStepGoal())
    val stepGoal: StateFlow<Int> = _stepGoal.asStateFlow()

    fun observeEntries(): Flow<List<StepEntry>> = dao.observeAll()

    fun setStepGoal(goal: Int) {
        prefs.setStepGoal(goal)
        _stepGoal.value = goal
    }

    /**
     * Verarbeitet einen Rohwert des Schrittzähler-Sensors.
     * Die Differenz zum zuletzt gespeicherten Wert wird dem Tag [todayEpochDay] gutgeschrieben.
     * Um Mitternacht entsteht dadurch automatisch ein neuer Tageseintrag.
     */
    suspend fun recordSensorTotal(total: Long, todayEpochDay: Long, source: String = "App") {
        mutex.withLock {
            val last = prefs.getLastSensorTotal()
            val delta = StepDeltaCalculator.delta(last, total)
            prefs.setLastSensorTotal(total)

            if (delta > 0L) {
                addSteps(todayEpochDay, delta.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            }

            // Eintrag fürs Mess-Protokoll in den Einstellungen
            val day = LocalDate.ofEpochDay(todayEpochDay).format(DAY_FORMAT)
            val note = when {
                last == null -> "Rohwert $total \u00B7 Basiswert gesetzt"
                total < last -> "Rohwert $total \u00B7 Neustart erkannt, +$delta Schritte \u2192 $day"
                else -> "Rohwert $total \u00B7 +$delta Schritte \u2192 $day"
            }
            prefs.appendReadLog(source, note)
        }
    }

    private suspend fun addSteps(dateEpochDay: Long, delta: Int) {
        val now = System.currentTimeMillis()
        val updatedRows = dao.addSteps(dateEpochDay, delta, now)
        if (updatedRows == 0) {
            dao.insert(
                StepEntry(
                    dateEpochDay = dateEpochDay,
                    steps = delta,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }
}
