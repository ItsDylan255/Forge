package com.example.fitnesstracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ein tatsächlich gegessener Eintrag im Tagebuch.
 *
 * Wichtig: Name und Nährwerte werden hier als Kopie gespeichert
 * (Snapshot). Wenn das zugehörige FoodItem später geändert oder
 * gelöscht wird, bleibt dieser Eintrag unverändert.
 */
@Entity(
    tableName = "food_log_entry",
    indices = [Index(value = ["dateEpochDay"])]
)
data class FoodLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Verweis auf die Vorlage. null = Vorlage existiert nicht mehr.
    val foodItemId: Long?,

    // Kopie zum Zeitpunkt des Eintrags
    val foodName: String,

    // Menge in Gramm
    val grams: Double,

    // Berechnete Werte für genau diese Menge
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,

    // Tageszuordnung
    val dateEpochDay: Long,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)