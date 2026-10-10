
package com.example.fitnesstracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_entries",
    indices = [Index(value = ["dateEpochDay"], unique = true)]
)
data class WeightEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val weightKg: Double,

    // Das Datum als Anzahl der Tage seit dem 01.01.1970
    val dateEpochDay: Long,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)