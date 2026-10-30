package com.example.fitnesstracker.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Ein Messwert des Schrittzählers. Aus diesen Werten berechnet die App die Schritte pro Tag
 * (siehe StepSamples). Nur die letzten 7 Tage werden aufgehoben.
 */
@Entity(
    tableName = "step_sample",
    indices = [Index(value = ["timeMillis"])]
)
data class StepSample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Wann die Schritte gegangen wurden (Uhrzeit in Millisekunden). */
    val timeMillis: Long,
    /** Rohwert des Zählers, kumuliert seit dem Gerätestart. */
    val total: Long,
    /** Uhrzeit des Gerätestarts. Ändert sie sich, gab es einen Neustart. */
    val bootMillis: Long
)

@Dao
interface StepSampleDao {

    @Query("SELECT * FROM step_sample ORDER BY timeMillis ASC, id ASC")
    suspend fun getAll(): List<StepSample>

    @Query("SELECT * FROM step_sample ORDER BY timeMillis DESC, id DESC LIMIT 1")
    suspend fun getLast(): StepSample?

    /** Messwerte ab [fromMillis] plus der letzte davor (Ausgangspunkt), chronologisch. */
    @Query(
        "SELECT * FROM step_sample WHERE timeMillis >= :fromMillis OR id = " +
                "(SELECT id FROM step_sample WHERE timeMillis < :fromMillis ORDER BY timeMillis DESC, id DESC LIMIT 1) " +
                "ORDER BY timeMillis ASC, id ASC"
    )
    suspend fun getFrom(fromMillis: Long): List<StepSample>

    @Query("SELECT * FROM step_sample ORDER BY timeMillis DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<StepSample>>

    @Insert
    suspend fun insert(sample: StepSample): Long

    /** Löscht ältere Messwerte, behält aber den neuesten davon als Ausgangspunkt. */
    @Query(
        "DELETE FROM step_sample WHERE timeMillis < :cutoff AND id NOT IN " +
                "(SELECT id FROM step_sample WHERE timeMillis < :cutoff ORDER BY timeMillis DESC, id DESC LIMIT 1)"
    )
    suspend fun pruneBefore(cutoff: Long)

    @Query("DELETE FROM step_sample")
    suspend fun deleteAll()
}
