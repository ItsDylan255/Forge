
package com.example.fitnesstracker.ui.gym

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.WorkoutRoutine
import com.example.fitnesstracker.data.repository.WorkoutRoutineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GymViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseProvider.getDatabase(application)

    private val repository = WorkoutRoutineRepository(
        database.workoutRoutineDao()
    )

    val routines = repository.getAllRoutines().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun addRoutine(name: String) {
        val trimmedName = name.trim()

        if (trimmedName.isBlank()) return

        viewModelScope.launch {
            repository.insertRoutine(
                WorkoutRoutine(name = trimmedName)
            )
        }
    }

    fun deleteRoutine(routine: WorkoutRoutine) {
        viewModelScope.launch {
            repository.deleteRoutine(routine)
        }
    }
}