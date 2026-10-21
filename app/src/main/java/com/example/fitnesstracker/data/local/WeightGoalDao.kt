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

    // Setzt oder ersetzt das (einzige) Ziel.
    // Achtung: REPLACE \u00FCberschreibt die ganze Zeile, also immer ALLE Felder mitgeben.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGoal(goal: WeightGoal)

    // \u00C4ndert nur das Zieldatum (null = Datum entfernen). Ohne Ziel passiert nichts.
    @Query("UPDATE weight_goal SET targetDateEpochDay = :epochDay, updatedAt = :now WHERE id = 1")
    suspend fun setTargetDate(epochDay: Long?, now: Long)

    @Query("DELETE FROM weight_goal")
    suspend fun deleteGoal()
}