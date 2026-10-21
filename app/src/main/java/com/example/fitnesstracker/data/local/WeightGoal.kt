package com.example.fitnesstracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Das optionale Zielgewicht.
 * Es gibt h\u00F6chstens eine Zeile (id = 1). Ohne Zeile ist kein Ziel gesetzt.
 */
@Entity(tableName = "weight_goal")
data class WeightGoal(
    @PrimaryKey
    val id: Int = SINGLE_GOAL_ID,

    val targetWeightKg: Double,

    // Gewicht beim Setzen des Ziels (Ausgangswert f\u00FCr den Fortschritt).
    // null = es gab zu diesem Zeitpunkt keine Messung.
    val startWeightKg: Double? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Gew\u00FCnschtes Zieldatum (Epoch-Tag). null = kein Datum gesetzt. Neu ab Datenbank-Version 7.
    val targetDateEpochDay: Long? = null
) {
    companion object {
        const val SINGLE_GOAL_ID = 1
    }
}