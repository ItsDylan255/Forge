package com.example.fitnesstracker.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Ein Tageswert der Schritte. Pro Tag gibt es höchstens einen Eintrag (Unique Index). */
@Entity(
    tableName = "step_entry",
    indices = [Index(value = ["dateEpochDay"], unique = true)]
)
data class StepEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateEpochDay: Long,
    val steps: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface StepEntryDao {

    @Query("SELECT * FROM step_entry ORDER BY dateEpochDay ASC")
    fun observeAll(): Flow<List<StepEntry>>

    /** Zählt Schritte auf einen vorhandenen Tag. Gibt die Anzahl geänderter Zeilen zurück (0 = Tag fehlt). */
    @Query(
        "UPDATE step_entry SET steps = steps + :delta, updatedAt = :now " +
                "WHERE dateEpochDay = :dateEpochDay"
    )
    suspend fun addSteps(dateEpochDay: Long, delta: Int, now: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: StepEntry): Long
}
