package com.example.fitnesstracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightGoalDao {

    // Liefert null, solange kein Ziel gesetzt ist
    @Query("SELECT * FROM weight_goal WHERE id = 1 LIMIT 1")
    fun observeGoal(): Flow<WeightGoal?>

    // Setzt oder ersetzt das (einzige) Ziel
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGoal(goal: WeightGoal)

    @Query("DELETE FROM weight_goal")
    suspend fun deleteGoal()
}