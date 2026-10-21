package com.example.fitnesstracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Eine Portion eines Lebensmittels, zum Beispiel "Scheibe" mit 30 g (Reihenfolge = position).
 * Ohne Fremdschlüssel: Beim Löschen eines Lebensmittels werden die Portionen im FoodDao mitgelöscht.
 */
@Entity(
    tableName = "food_portion",
    indices = [Index(value = ["foodItemId"])]
)
data class FoodPortion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val foodItemId: Long,
    val name: String,
    val grams: Double,
    val position: Int
)
