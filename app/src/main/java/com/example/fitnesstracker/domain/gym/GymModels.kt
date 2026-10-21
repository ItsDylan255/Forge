package com.example.fitnesstracker.domain.gym

import com.example.fitnesstracker.data.local.GymSession
import com.example.fitnesstracker.data.local.GymSet

/** Room-unabhängige Sicht auf eine Trainingseinheit. */
data class WorkoutSessionData(
    val id: Long,
    val dateEpochDay: Long,
    val startedAt: Long,
    val endedAt: Long?
)

/** Room-unabhängige Sicht auf einen Satz. [order] gibt die Reihenfolge des Loggens an. */
data class WorkoutSetData(
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val order: Long,
    /** Seite bei einseitigen Übungen, sonst BOTH. */
    val side: SetSide = SetSide.BOTH,
    /** Art der Übung beim Loggen. Bei TIME stehen die Sekunden in [reps]. */
    val kind: ExerciseKind = ExerciseKind.REPS,
    /** Körpergewicht beim Loggen (nur für assistierte Übungen nötig), sonst null. */
    val bodyWeightKg: Double? = null
)

fun GymSession.toData(): WorkoutSessionData =
    WorkoutSessionData(id = id, dateEpochDay = dateEpochDay, startedAt = startedAt, endedAt = endedAt)

fun GymSet.toData(): WorkoutSetData =
    WorkoutSetData(
        sessionId = sessionId,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        weightKg = weightKg,
        reps = reps,
        order = id,
        side = SetSide.fromCode(side),
        kind = ExerciseKind.fromCode(kind),
        bodyWeightKg = bodyWeightKg
    )

/** Standardwerte für das Training. */
object GymDefaults {
    /** Pause nach einem Satz, wenn nichts anderes eingestellt ist. */
    const val REST_SECONDS = 90

    /** Schrittweite beim Einstellen der Pause. */
    const val REST_STEP_SECONDS = 15

    const val MAX_REST_SECONDS = 600
}
