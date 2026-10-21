package com.example.fitnesstracker.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Übung aus der Bibliothek. Der Name ist eindeutig. */
@Entity(
    tableName = "gym_exercise",
    indices = [Index(value = ["name"], unique = true)]
)
data class GymExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val muscleGroup: String?,
    /** Art der Übung: 0 = Wiederholungen, 1 = Zeit, 2 = Assistiert. */
    val kind: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "gym_routine")
data class GymRoutine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)

/** Eine Übung innerhalb einer Routine (Reihenfolge = position). */
@Entity(
    tableName = "gym_routine_exercise",
    indices = [Index(value = ["routineId"]), Index(value = ["exerciseId"])]
)
data class GymRoutineExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineId: Long,
    val exerciseId: Long,
    val position: Int,
    val plannedSets: Int,
    val repsMin: Int,
    val repsMax: Int,
    /** Pause nach jedem Satz dieser Übung in Sekunden (0 = keine Pause). Standard: 90. */
    val restSeconds: Int = 90,
    /** Jeder Satz besteht aus rechts und links (einseitige Übung). */
    val unilateral: Boolean = false,
    /** Diese Übung bildet mit der nächsten in der Liste ein Superset. */
    val supersetWithNext: Boolean = false
)

/**
 * Eine Trainingseinheit. endedAt == null bedeutet: läuft gerade.
 * Der Routinenname wird als Momentaufnahme gespeichert, damit der Verlauf erhalten bleibt.
 */
@Entity(tableName = "gym_session")
data class GymSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineId: Long?,
    val routineName: String,
    val dateEpochDay: Long,
    val startedAt: Long,
    val endedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

/** Ein geloggter Satz. Der Übungsname ist eine Momentaufnahme (bleibt auch nach dem Löschen der Übung). */
@Entity(
    tableName = "gym_set",
    indices = [Index(value = ["sessionId"]), Index(value = ["exerciseId"])]
)
data class GymSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    /** 0 = beide Seiten, 1 = rechts, 2 = links (einseitige Übung). */
    val side: Int = 0,
    /** Art der Übung beim Loggen (Momentaufnahme): 0 = Wdh., 1 = Zeit (Sekunden in reps), 2 = Assistiert. */
    val kind: Int = 0,
    /** Körpergewicht beim Loggen (Momentaufnahme, nur bei assistierten Übungen), sonst null. */
    val bodyWeightKg: Double? = null,
    val createdAt: Long
)

@Dao
abstract class GymDao {

    // ---------- Übungen ----------

    @Query("SELECT * FROM gym_exercise ORDER BY name COLLATE NOCASE ASC")
    abstract fun observeExercises(): Flow<List<GymExercise>>

    @Query("SELECT * FROM gym_exercise ORDER BY name COLLATE NOCASE ASC")
    abstract suspend fun getAllExercises(): List<GymExercise>

    @Query("SELECT * FROM gym_exercise WHERE id = :id")
    abstract suspend fun getExercise(id: Long): GymExercise?

    @Query("SELECT * FROM gym_exercise WHERE name = :name COLLATE NOCASE LIMIT 1")
    abstract suspend fun findExerciseByName(name: String): GymExercise?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertExercise(exercise: GymExercise): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertExercises(exercises: List<GymExercise>)

    @Update
    abstract suspend fun updateExercise(exercise: GymExercise)

    @Query("DELETE FROM gym_exercise WHERE id = :id")
    abstract suspend fun deleteExerciseRow(id: Long)

    @Query("DELETE FROM gym_routine_exercise WHERE exerciseId = :exerciseId")
    abstract suspend fun deleteRoutineItemsForExercise(exerciseId: Long)

    /** Entfernt die Übung auch aus allen Routinen. Bereits geloggte Sätze bleiben erhalten. */
    @Transaction
    open suspend fun deleteExercise(id: Long) {
        deleteRoutineItemsForExercise(id)
        deleteExerciseRow(id)
    }

    // ---------- Routinen ----------

    @Query("SELECT * FROM gym_routine ORDER BY name COLLATE NOCASE ASC")
    abstract fun observeRoutines(): Flow<List<GymRoutine>>

    @Query("SELECT * FROM gym_routine_exercise ORDER BY routineId ASC, position ASC")
    abstract fun observeRoutineExercises(): Flow<List<GymRoutineExercise>>

    @Query("SELECT * FROM gym_routine WHERE id = :id")
    abstract suspend fun getRoutine(id: Long): GymRoutine?

    @Query("SELECT * FROM gym_routine_exercise WHERE routineId = :routineId ORDER BY position ASC")
    abstract suspend fun getRoutineItems(routineId: Long): List<GymRoutineExercise>

    @Insert
    abstract suspend fun insertRoutine(routine: GymRoutine): Long

    @Update
    abstract suspend fun updateRoutine(routine: GymRoutine)

    @Insert
    abstract suspend fun insertRoutineItems(items: List<GymRoutineExercise>)

    @Query("DELETE FROM gym_routine_exercise WHERE routineId = :routineId")
    abstract suspend fun deleteRoutineItems(routineId: Long)

    @Query("DELETE FROM gym_routine WHERE id = :id")
    abstract suspend fun deleteRoutineRow(id: Long)

    @Transaction
    open suspend fun saveRoutine(routine: GymRoutine, items: List<GymRoutineExercise>): Long {
        val id = if (routine.id == 0L) {
            insertRoutine(routine)
        } else {
            updateRoutine(routine)
            routine.id
        }
        deleteRoutineItems(id)
        insertRoutineItems(items.map { it.copy(routineId = id) })
        return id
    }

    @Transaction
    open suspend fun deleteRoutine(id: Long) {
        deleteRoutineItems(id)
        deleteRoutineRow(id)
    }

    // ---------- Trainingseinheiten ----------

    @Query("SELECT * FROM gym_session ORDER BY startedAt DESC")
    abstract fun observeSessions(): Flow<List<GymSession>>

    @Query("SELECT * FROM gym_session WHERE id = :id")
    abstract suspend fun getSession(id: Long): GymSession?

    @Insert
    abstract suspend fun insertSession(session: GymSession): Long

    @Update
    abstract suspend fun updateSession(session: GymSession)

    @Query("DELETE FROM gym_set WHERE sessionId = :sessionId")
    abstract suspend fun deleteSetsOfSession(sessionId: Long)

    @Query("DELETE FROM gym_session WHERE id = :id")
    abstract suspend fun deleteSessionRow(id: Long)

    @Transaction
    open suspend fun deleteSession(id: Long) {
        deleteSetsOfSession(id)
        deleteSessionRow(id)
    }

    // ---------- Sätze ----------

    @Query("SELECT * FROM gym_set ORDER BY id ASC")
    abstract fun observeSets(): Flow<List<GymSet>>

    @Insert
    abstract suspend fun insertSet(gymSet: GymSet): Long

    @Query("DELETE FROM gym_set WHERE id = :id")
    abstract suspend fun deleteSet(id: Long)

    @Query(
        "SELECT COALESCE(MAX(setNumber), 0) FROM gym_set " +
                "WHERE sessionId = :sessionId AND exerciseId = :exerciseId AND side = :side"
    )
    abstract suspend fun maxSetNumber(sessionId: Long, exerciseId: Long, side: Int): Int
}
