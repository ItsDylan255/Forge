package com.example.fitnesstracker.data.repository

import com.example.fitnesstracker.data.local.StepEntry
import com.example.fitnesstracker.data.local.StepEntryDao
import com.example.fitnesstracker.data.local.StepPreferences
import com.example.fitnesstracker.domain.steps.StepDeltaCalculator
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
    // Sensorwerte werden nacheinander verarbeitet, damit keine Schritte doppelt zählen.
    private val mutex = Mutex()

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
    suspend fun recordSensorTotal(total: Long, todayEpochDay: Long) {
        mutex.withLock {
            val delta = StepDeltaCalculator.delta(prefs.getLastSensorTotal(), total)
            prefs.setLastSensorTotal(total)

            if (delta > 0L) {
                addSteps(todayEpochDay, delta.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            }
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
