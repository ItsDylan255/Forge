
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeightViewModel(application: Application) :
    AndroidViewModel(application) {

    private val database = DatabaseProvider.getDatabase(application)

    private val repository = WeightEntryRepository(
        database.weightEntryDao()
    )

    private val weightGoalDao = database.weightGoalDao()

    // Alle Gewichtseinträge, chronologisch sortiert.
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

    fun saveWeight(weightKg: Double, date: LocalDate = LocalDate.now()) {
        if (!weightKg.isFinite() || weightKg <= 0.0) return

        viewModelScope.launch {
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
        }
    }

    fun deleteEntry(entry: WeightEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    /**
     * Speichert ein Zielgewicht.
     * Wenn bereits eine Messung vorhanden ist, wird sie als Ausgangsgewicht
     * für die Fortschrittsberechnung übernommen.
     */
    fun saveGoal(targetWeightKg: Double) {
        if (!targetWeightKg.isFinite() || targetWeightKg <= 0.0) return

        viewModelScope.launch {
            val currentGoal = goal.value
            val latestEntry = entries.value.maxByOrNull { it.dateEpochDay }

            val newGoal = WeightGoal(
                id = WeightGoal.SINGLE_GOAL_ID,
                targetWeightKg = targetWeightKg,
                startWeightKg = currentGoal?.startWeightKg
                    ?: latestEntry?.weightKg,
                createdAt = currentGoal?.createdAt
                    ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            weightGoalDao.saveGoal(newGoal)
        }
    }

    fun deleteGoal() {
        viewModelScope.launch {
            weightGoalDao.deleteGoal()
        }
    }
}