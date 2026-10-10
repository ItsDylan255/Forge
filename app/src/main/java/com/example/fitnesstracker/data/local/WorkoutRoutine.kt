
package com.example.fitnesstracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_routines")
data class WorkoutRoutine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val description: String = "",

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)