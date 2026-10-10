
package com.example.fitnesstracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutRoutineDao {

    @Query("SELECT * FROM workout_routines ORDER BY createdAt DESC")
    fun getAllRoutines(): Flow<List<WorkoutRoutine>>

    @Query("SELECT * FROM workout_routines WHERE id = :id")
    fun getRoutineById(id: Long): Flow<WorkoutRoutine?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRoutine(routine: WorkoutRoutine): Long

    @Update
    suspend fun updateRoutine(routine: WorkoutRoutine)

    @Delete
    suspend fun deleteRoutine(routine: WorkoutRoutine)
}