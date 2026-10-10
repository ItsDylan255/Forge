
package com.example.fitnesstracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightEntryDao {

    // Alle Einträge chronologisch abrufen
    @Query("SELECT * FROM weight_entries ORDER BY dateEpochDay ASC")
    fun getAllEntries(): Flow<List<WeightEntry>>

    // Eintrag für einen bestimmten Tag abrufen
    @Query("SELECT * FROM weight_entries WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getEntryForDate(dateEpochDay: Long): WeightEntry?

    // Neuen Eintrag speichern
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: WeightEntry): Long

    // Vorhandenen Eintrag korrigieren
    @Update
    suspend fun updateEntry(entry: WeightEntry)

    // Eintrag löschen
    @Delete
    suspend fun deleteEntry(entry: WeightEntry)
}