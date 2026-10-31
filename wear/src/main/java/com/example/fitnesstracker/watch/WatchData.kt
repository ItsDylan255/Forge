package com.example.fitnesstracker.watch

import android.content.Context
import com.google.android.gms.wearable.DataMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Pfade der Data Layer. Müssen mit `WearBridge` in der Handy-App übereinstimmen. */
object WearProtocol {
    const val PATH_WORKOUT = "/forge/workout"
    const val PATH_TODAY = "/forge/today"
    const val PATH_REST_EXTEND = "/forge/rest/extend"
    const val PATH_REST_SKIP = "/forge/rest/skip"
    const val PATH_REST_OVER = "/forge/rest/over"
}

/** Das laufende Training, so wie es das Handy schickt. Alle Texte sind schon fertig formatiert. */
data class WorkoutState(
    val routineName: String,
    val startedAt: Long,
    val exerciseName: String?,
    val setText: String?,
    val targetText: String?,
    val lastText: String?,
    val progressText: String?,
    val superset: Boolean,
    val recordText: String?,
    val restLabel: String?,
    val restEndsAt: Long,
    val restTotal: Long
) {
    fun restRunning(now: Long): Boolean = restEndsAt > now

    companion object {
        /** null = kein Training läuft. */
        fun from(map: DataMap): WorkoutState? {
            if (!map.getBoolean("active", false)) return null
            fun text(key: String) = map.getString(key)?.takeIf { it.isNotEmpty() }
            return WorkoutState(
                routineName = map.getString("routine") ?: "Training",
                startedAt = map.getLong("startedAt"),
                exerciseName = text("exercise"),
                setText = text("set"),
                targetText = text("target"),
                lastText = text("last"),
                progressText = text("progress"),
                superset = map.getBoolean("superset", false),
                recordText = text("record"),
                restLabel = text("restLabel"),
                restEndsAt = map.getLong("restEndsAt"),
                restTotal = map.getLong("restTotal")
            )
        }
    }
}

/** Tageswerte für Kachel und Komplikation. Negative Werte vom Handy bedeuten: kein Wert. */
data class TodayState(
    val dateEpochDay: Long,
    val steps: Int,
    val stepGoal: Int,
    val weightKg: Double?,
    val kcal: Int?,
    val kcalTarget: Int?
) {
    companion object {
        fun from(map: DataMap) = TodayState(
            dateEpochDay = map.getLong("date"),
            steps = map.getInt("steps"),
            stepGoal = map.getInt("stepGoal"),
            weightKg = map.getDouble("weight").takeIf { it > 0.0 },
            kcal = map.getInt("kcal").takeIf { it >= 0 },
            kcalTarget = map.getInt("kcalTarget").takeIf { it > 0 }
        )
    }
}

/** Der zuletzt empfangene Stand, für die Ansicht auf der Uhr. */
object WatchStore {
    private val _workout = MutableStateFlow<WorkoutState?>(null)
    val workout: StateFlow<WorkoutState?> = _workout.asStateFlow()

    fun setWorkout(state: WorkoutState?) {
        _workout.value = state
    }
}

/**
 * Speichert die Tageswerte auf der Uhr, damit Kachel und Komplikation sie auch zeigen,
 * wenn das Handy gerade nicht erreichbar ist.
 */
class TodayStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("today", Context.MODE_PRIVATE)

    fun save(today: TodayState) {
        prefs.edit()
            .putLong("date", today.dateEpochDay)
            .putInt("steps", today.steps)
            .putInt("stepGoal", today.stepGoal)
            .putFloat("weight", (today.weightKg ?: -1.0).toFloat())
            .putInt("kcal", today.kcal ?: -1)
            .putInt("kcalTarget", today.kcalTarget ?: -1)
            .apply()
    }

    /** null = noch nie Daten vom Handy bekommen. */
    fun load(): TodayState? {
        if (!prefs.contains("date")) return null
        return TodayState(
            dateEpochDay = prefs.getLong("date", 0L),
            steps = prefs.getInt("steps", 0),
            stepGoal = prefs.getInt("stepGoal", 10_000),
            weightKg = prefs.getFloat("weight", -1f).toDouble().takeIf { it > 0.0 },
            kcal = prefs.getInt("kcal", -1).takeIf { it >= 0 },
            kcalTarget = prefs.getInt("kcalTarget", -1).takeIf { it > 0 }
        )
    }
}
