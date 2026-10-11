package com.example.fitnesstracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Eine eigene Lebensmittel-Vorlage.
 * Alle Nährwerte beziehen sich auf 100 g.
 * Der Name muss eindeutig sein.
 */
@Entity(
    tableName = "food_item",
    indices = [Index(value = ["name"], unique = true)]
)
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val name: String,

    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)