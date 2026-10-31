package com.example.fitnesstracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Ein Fortschrittsfoto. Das Bild liegt im App-Speicher (siehe `PhotoStorage`), hier stehen nur der
 * Dateiname und die Werte vom Tag der Aufnahme.
 */
@Entity(
    tableName = "progress_photo",
    indices = [Index(value = ["dateEpochDay"])]
)
data class ProgressPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val fileName: String,
    val dateEpochDay: Long,
    val weightKg: Double?,
    val goalWeightKg: Double?,
    val workoutsTotal: Int,
    val workouts30Days: Int,
    val avgSteps7Days: Int?,
    val avgKcal7Days: Int?,
    val kcalTarget: Int?,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ProgressPhotoDao {

    /** Neueste zuerst. */
    @Query("SELECT * FROM progress_photo ORDER BY dateEpochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<ProgressPhoto>>

    @Query("SELECT MAX(dateEpochDay) FROM progress_photo")
    suspend fun lastPhotoDay(): Long?

    @Insert
    suspend fun insert(photo: ProgressPhoto): Long

    @Delete
    suspend fun delete(photo: ProgressPhoto)
}
