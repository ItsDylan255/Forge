package com.example.fitnesstracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogEntryDao {

    @Query("SELECT * FROM food_log_entry ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<FoodLogEntry>>

    @Query(
        "SELECT * FROM food_log_entry " +
                "WHERE dateEpochDay = :dateEpochDay " +
                "ORDER BY id ASC"
    )
    fun observeForDate(dateEpochDay: Long): Flow<List<FoodLogEntry>>

    @Query(
        "SELECT * FROM food_log_entry " +
                "WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay " +
                "ORDER BY dateEpochDay ASC, id ASC"
    )
    fun observeBetween(
        startEpochDay: Long,
        endEpochDay: Long
    ): Flow<List<FoodLogEntry>>

    @Query("SELECT * FROM food_log_entry WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FoodLogEntry?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: FoodLogEntry): Long

    @Update
    suspend fun update(entry: FoodLogEntry)

    @Delete
    suspend fun delete(entry: FoodLogEntry)

    @Query("DELETE FROM food_log_entry WHERE id = :id")
    suspend fun deleteById(id: Long)
}