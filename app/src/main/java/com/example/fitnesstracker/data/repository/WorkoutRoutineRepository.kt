
package com.example.fitnesstracker.data.repository

import com.example.fitnesstracker.data.local.WorkoutRoutine
import com.example.fitnesstracker.data.local.WorkoutRoutineDao
import kotlinx.coroutines.flow.Flow

class WorkoutRoutineRepository(
    private val workoutRoutineDao: WorkoutRoutineDao
) {

    fun getAllRoutines(): Flow<List<WorkoutRoutine>> {
        return workoutRoutineDao.getAllRoutines()
    }

    fun getRoutineById(id: Long): Flow<WorkoutRoutine?> {
        return workoutRoutineDao.getRoutineById(id)
    }

    suspend fun insertRoutine(routine: WorkoutRoutine): Long {
        return workoutRoutineDao.insertRoutine(routine)
    }

    suspend fun updateRoutine(routine: WorkoutRoutine) {
        workoutRoutineDao.updateRoutine(
            routine.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteRoutine(routine: WorkoutRoutine) {
        workoutRoutineDao.deleteRoutine(routine)
    }
}