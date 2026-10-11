package com.example.fitnesstracker.ui.weight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.WeightEntry
import com.example.fitnesstracker.data.local.WeightGoal
import com.example.fitnesstracker.data.repository.WeightEntryRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeightViewModel(application: Application) :
    AndroidViewModel(application) {

    private val database = DatabaseProvider.getDatabase(application)

    private val repository = WeightEntryRepository(
        database.weightEntryDao()
    )

    private val weightGoalDao = database.weightGoalDao()

    // Alle Gewichtseinträge.
    val entries = repository.getAllEntries().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // Das aktuell gespeicherte Zielgewicht.
    val goal = weightGoalDao.observeGoal().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    /**
     * Insert-or-update: Existiert für das Datum schon ein Eintrag, wird er überschrieben.
     *
     * Sonderfall: Wurde ein Ziel gesetzt, bevor es überhaupt eine Messung gab
     * (startWeightKg = null), wird der allererste Eintrag als Startgewicht übernommen.
     */
    fun saveWeight(weightKg: Double, date: LocalDate = LocalDate.now()) {
        if (!weightKg.isFinite() || weightKg <= 0.0) return

        viewModelScope.launch {
            val hadNoEntries = repository.getAllEntries().first().isEmpty()

            val dateEpochDay = date.toEpochDay()
            val existingEntry = repository.getEntryForDate(dateEpochDay)

            if (existingEntry != null) {
                repository.updateEntry(
                    existingEntry.copy(weightKg = weightKg)
                )
            } else {
                repository.insertEntry(
                    WeightEntry(
                        weightKg = weightKg,
                        dateEpochDay = dateEpochDay
                    )
                )
            }

            if (hadNoEntries) {
                val currentGoal = weightGoalDao.observeGoal().first()
                if (currentGoal != null && currentGoal.startWeightKg == null) {
                    weightGoalDao.saveGoal(
                        WeightGoal(
                            id = WeightGoal.SINGLE_GOAL_ID,
                            targetWeightKg = currentGoal.targetWeightKg,
                            startWeightKg = weightKg,
                            createdAt = currentGoal.createdAt,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun deleteEntry(entry: WeightEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    /**
     * Speichert ein Zielgewicht.
     * - Existiert schon ein Ziel: Startgewicht bleibt erhalten.
     * - Sonst: neuester Gewichtseintrag wird Startgewicht (oder null ohne Eintrag).
     */
    fun saveGoal(targetWeightKg: Double) {
        if (!targetWeightKg.isFinite() || targetWeightKg <= 0.0) return

        viewModelScope.launch {
            val currentGoal = weightGoalDao.observeGoal().first()
            val latestEntry = repository.getAllEntries().first()
                .maxByOrNull { it.dateEpochDay }

            val now = System.currentTimeMillis()

            weightGoalDao.saveGoal(
                WeightGoal(
                    id = WeightGoal.SINGLE_GOAL_ID,
                    targetWeightKg = targetWeightKg,
                    startWeightKg = currentGoal?.startWeightKg
                        ?: latestEntry?.weightKg,
                    createdAt = currentGoal?.createdAt ?: now,
                    updatedAt = now
                )
            )
        }
    }

    fun deleteGoal() {
        viewModelScope.launch {
            weightGoalDao.deleteGoal()
        }
    }
}
