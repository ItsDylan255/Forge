package com.example.fitnesstracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Zugriff auf die vorhandenen Tabellen food_item und food_log_entry (seit Datenbank-Version 4).
 * Die bisherigen DAOs FoodItemDao und FoodLogEntryDao bleiben unberuehrt.
 */
@Dao
abstract class FoodDao {

    // ---------- Lebensmittel (Vorlagen) ----------

    @Query("SELECT * FROM food_item ORDER BY name COLLATE NOCASE ASC")
    abstract fun observeFoods(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_item WHERE id = :id")
    abstract suspend fun getFood(id: Long): FoodItem?

    @Query("SELECT * FROM food_item WHERE name = :name COLLATE NOCASE LIMIT 1")
    abstract suspend fun findFoodByName(name: String): FoodItem?

    @Insert
    abstract suspend fun insertFood(food: FoodItem): Long

    @Update
    abstract suspend fun updateFood(food: FoodItem)

    @Query("UPDATE food_log_entry SET foodItemId = NULL WHERE foodItemId = :foodId")
    abstract suspend fun detachLogEntries(foodId: Long)

    @Query("DELETE FROM food_item WHERE id = :id")
    abstract suspend fun deleteFoodRow(id: Long)

    /** Loescht die Vorlage samt Portionen. Eingetragene Mahlzeiten bleiben erhalten (Snapshot), nur ohne Verweis. */
    @Transaction
    open suspend fun deleteFood(id: Long) {
        detachLogEntries(id)
        deletePortions(id)
        deleteFoodRow(id)
    }

    // ---------- Portionen ----------

    @Query("SELECT * FROM food_portion ORDER BY foodItemId ASC, position ASC")
    abstract fun observePortions(): Flow<List<FoodPortion>>

    @Insert
    abstract suspend fun insertPortions(portions: List<FoodPortion>)

    @Query("DELETE FROM food_portion WHERE foodItemId = :foodId")
    abstract suspend fun deletePortions(foodId: Long)

    /** Ersetzt alle Portionen eines Lebensmittels. */
    @Transaction
    open suspend fun savePortions(foodId: Long, portions: List<FoodPortion>) {
        deletePortions(foodId)
        if (portions.isNotEmpty()) insertPortions(portions)
    }

    // ---------- Eintraege ----------

    @Query("SELECT * FROM food_log_entry ORDER BY dateEpochDay ASC, id ASC")
    abstract fun observeLog(): Flow<List<FoodLogEntry>>

    @Query("SELECT * FROM food_log_entry WHERE id = :id")
    abstract suspend fun getLogEntry(id: Long): FoodLogEntry?

    @Insert
    abstract suspend fun insertLogEntry(entry: FoodLogEntry): Long

    @Update
    abstract suspend fun updateLogEntry(entry: FoodLogEntry)

    @Query("DELETE FROM food_log_entry WHERE id = :id")
    abstract suspend fun deleteLogEntry(id: Long)
}
