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

/** Ein Trainingsplan fasst Routinen zusammen (z. B. „Fitnessbasic“). archivedAt == null = aktiv. */
@Entity(tableName = "gym_plan")
data class GymPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val archivedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Eine Routine gehört zu höchstens einem Plan ([planId] null = ohne Plan).
 * [planPosition] ist die Reihenfolge im Plan, archivedAt == null = aktiv.
 */
@Entity(
    tableName = "gym_routine",
    indices = [Index(value = ["planId"])]
)
data class GymRoutine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val planId: Long? = null,
    val planPosition: Int = 0,
    val archivedAt: Long? = null,
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
 * Routinen- und Planname werden als Momentaufnahme gespeichert, damit der Verlauf erhalten bleibt.
 * [planId] null = Routine ohne Plan oder freies Workout.
 */
@Entity(tableName = "gym_session")
data class GymSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineId: Long?,
    val routineName: String,
    val planId: Long? = null,
    val planName: String? = null,
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

    @Query("UPDATE gym_routine SET archivedAt = :archivedAt, updatedAt = :now WHERE id = :id")
    abstract suspend fun setRoutineArchived(id: Long, archivedAt: Long?, now: Long)

    @Query("UPDATE gym_routine SET planPosition = :position, updatedAt = :now WHERE id = :id")
    abstract suspend fun setRoutinePlanPosition(id: Long, position: Int, now: Long)

    @Query("SELECT COALESCE(MAX(planPosition), -1) FROM gym_routine WHERE planId = :planId")
    abstract suspend fun maxPlanPosition(planId: Long): Int

    /** Nummeriert die Routinen eines Plans in der Reihenfolge von [routineIds] neu (0, 1, 2 …). */
    @Transaction
    open suspend fun setPlanOrder(routineIds: List<Long>, now: Long) {
        routineIds.forEachIndexed { index, id -> setRoutinePlanPosition(id, index, now) }
    }

    // ---------- Trainingspläne ----------

    @Query("SELECT * FROM gym_plan ORDER BY name COLLATE NOCASE ASC")
    abstract fun observePlans(): Flow<List<GymPlan>>

    @Query("SELECT * FROM gym_plan WHERE id = :id")
    abstract suspend fun getPlan(id: Long): GymPlan?

    @Query("SELECT * FROM gym_plan WHERE name = :name COLLATE NOCASE LIMIT 1")
    abstract suspend fun findPlanByName(name: String): GymPlan?

    @Insert
    abstract suspend fun insertPlan(plan: GymPlan): Long

    @Update
    abstract suspend fun updatePlan(plan: GymPlan)

    @Query("UPDATE gym_plan SET archivedAt = :archivedAt, updatedAt = :now WHERE id = :id")
    abstract suspend fun setPlanArchived(id: Long, archivedAt: Long?, now: Long)

    @Query("UPDATE gym_routine SET planId = NULL, planPosition = 0 WHERE planId = :planId")
    abstract suspend fun detachRoutinesFromPlan(planId: Long)

    @Query("DELETE FROM gym_plan WHERE id = :id")
    abstract suspend fun deletePlanRow(id: Long)

    @Query("SELECT * FROM gym_routine WHERE planId = :planId AND archivedAt IS NULL ORDER BY planPosition ASC")
    abstract suspend fun getActiveRoutinesOfPlan(planId: Long): List<GymRoutine>

    /**
     * Kopiert den Plan mit allen aktiven Routinen und ihren Übungen unter [newName].
     * [adjustItem] darf jede kopierte Übung ändern (z. B. einen Satz weniger). Gibt die Id des neuen Plans zurück.
     */
    @Transaction
    open suspend fun duplicatePlan(
        planId: Long,
        newName: String,
        now: Long,
        adjustItem: (GymRoutineExercise) -> GymRoutineExercise
    ): Long {
        val newPlanId = insertPlan(GymPlan(name = newName, createdAt = now, updatedAt = now))
        getActiveRoutinesOfPlan(planId).forEach { routine ->
            val newRoutineId = insertRoutine(
                routine.copy(id = 0L, planId = newPlanId, archivedAt = null, createdAt = now, updatedAt = now)
            )
            insertRoutineItems(getRoutineItems(routine.id).map { adjustItem(it.copy(id = 0L, routineId = newRoutineId)) })
        }
        return newPlanId
    }

    /** Löscht nur den Plan. Seine Routinen bleiben erhalten und sind danach ohne Plan. */
    @Transaction
    open suspend fun deletePlan(id: Long) {
        detachRoutinesFromPlan(id)
        deletePlanRow(id)
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
