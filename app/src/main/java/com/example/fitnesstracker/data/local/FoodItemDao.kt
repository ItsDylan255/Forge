package com.example.fitnesstracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {

    @Query("SELECT * FROM food_item ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_item WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FoodItem?

    @Query("SELECT * FROM food_item WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): FoodItem?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: FoodItem): Long

    @Update
    suspend fun update(item: FoodItem)

    @Delete
    suspend fun delete(item: FoodItem)
}