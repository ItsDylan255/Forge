package com.example.fitnesstracker.data.repository

import com.example.fitnesstracker.data.local.StepEntry
import com.example.fitnesstracker.data.local.StepEntryDao
import com.example.fitnesstracker.data.local.StepPreferences
import com.example.fitnesstracker.data.local.StepSample
import com.example.fitnesstracker.data.local.StepSampleDao
import com.example.fitnesstracker.domain.steps.SampleData
import com.example.fitnesstracker.domain.steps.StepSamples
import com.example.fitnesstracker.domain.steps.toData
import java.time.Instant
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Schritte: Der Dienst speichert Messwerte des Zählers, daraus entstehen die Tageswerte.
 * Die Tageswerte werden bei jedem neuen Messwert aus allen aufgehobenen Messwerten neu berechnet,
 * dadurch kann kein Wert auf einen falschen Tag fallen oder doppelt zählen.
 */
class StepRepository(
    private val entryDao: StepEntryDao,
    private val sampleDao: StepSampleDao,
    private val prefs: StepPreferences
) {
    private companion object {
        // Neue Messwerte werden nacheinander verarbeitet
        val mutex = Mutex()
    }

    private val _stepGoal = MutableStateFlow(prefs.getStepGoal())
    val stepGoal: StateFlow<Int> = _stepGoal.asStateFlow()

    fun observeEntries(): Flow<List<StepEntry>> = entryDao.observeAll()

    fun setStepGoal(goal: Int) {
        prefs.setStepGoal(goal)
        _stepGoal.value = goal
    }

    /** Speichert den Messwert (falls nötig) und berechnet die Tageswerte neu. */
    suspend fun recordSample(sample: SampleData) {
        mutex.withLock {
            val last = sampleDao.getLast()?.toData()
            if (!StepSamples.shouldStore(last, sample)) return

            sampleDao.insert(
                StepSample(
                    timeMillis = sample.timeMillis,
                    total = sample.total,
                    bootMillis = sample.bootMillis
                )
            )
            sampleDao.pruneBefore(sample.timeMillis - StepSamples.RETENTION_MILLIS)

            // Nur die Tage ab dem vorherigen Messwert können sich geändert haben
            recompute(fromMillis = minOf(last?.timeMillis ?: sample.timeMillis, sample.timeMillis))
        }
    }

    /** Löscht alle Schrittdaten (Tageswerte und Messwerte). */
    suspend fun resetAll() {
        mutex.withLock {
            entryDao.deleteAll()
            sampleDao.deleteAll()
        }
    }

    /**
     * Berechnet die Tageswerte ab dem Tag von [fromMillis] neu. Geladen werden nur die Messwerte ab
     * Beginn dieses Tages und der letzte davor, damit der Tag vollständig abgedeckt ist.
     */
    private suspend fun recompute(fromMillis: Long) {
        val zone = ZoneId.systemDefault()
        val fromDate = Instant.ofEpochMilli(fromMillis).atZone(zone).toLocalDate()
        val fromDay = fromDate.toEpochDay()
        val dayStart = fromDate.atStartOfDay(zone).toInstant().toEpochMilli()

        val samples = sampleDao.getFrom(dayStart).map { it.toData() }
        if (samples.size < 2) return

        val computed = StepSamples.dailySteps(samples, zone).filterKeys { it >= fromDay }
        val existing = entryDao.getAll().associateBy { it.dateEpochDay }
        val firstDay = Instant.ofEpochMilli(samples.minOf { it.timeMillis })
            .atZone(zone).toLocalDate().toEpochDay()
        val now = System.currentTimeMillis()

        for ((day, value) in computed) {
            var steps = value.roundToInt()

            // Der erste Tag ist nur teilweise abgedeckt (ältere Messwerte sind gelöscht):
            // er wird nie kleiner als der gespeicherte Wert
            if (day == firstDay) steps = max(steps, existing[day]?.steps ?: 0)

            val current = existing[day]
            if (current?.steps == steps) continue
            if (current == null && steps == 0) continue

            entryDao.upsert(
                StepEntry(
                    dateEpochDay = day,
                    steps = steps,
                    createdAt = current?.createdAt ?: now,
                    updatedAt = now
                )
            )
        }
    }
}
