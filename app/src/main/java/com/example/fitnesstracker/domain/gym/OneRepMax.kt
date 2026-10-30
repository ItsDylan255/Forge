package com.example.fitnesstracker.domain.gym

import kotlin.math.roundToInt

/** Bestes geschätztes Maximalgewicht einer Übung und der Satz, aus dem es stammt. */
data class OneRepMaxBest(
    val estimateKg: Double,
    val weightKg: Double,
    val reps: Int,
    val dateEpochDay: Long
)

/** Eine Zeile der Prozent-Tabelle: [percent] vom 1RM, das Gewicht dazu und die ungefähr möglichen Wiederholungen. */
data class OneRepMaxRow(val percent: Int, val weightKg: Double, val reps: Int)

/**
 * Geschätztes Maximalgewicht für eine Wiederholung (1RM) nach Epley: Gewicht × (1 + Wdh. / 30).
 * Nur für Übungen mit Wiederholungen. Über [MAX_REPS] Wiederholungen wird die Schätzung zu ungenau.
 */
object OneRepMax {

    const val MAX_REPS = 10

    /** Prozentstufen der Tabelle. */
    val PERCENTS = listOf(100, 95, 90, 85, 80, 75, 70, 65, 60)

    /** Geschätztes 1RM, null bei ungültigem Satz (kein Gewicht, 0 oder mehr als [MAX_REPS] Wdh.). */
    fun estimate(weightKg: Double, reps: Int): Double? {
        if (weightKg <= 0.0 || reps < 1 || reps > MAX_REPS) return null
        return if (reps == 1) weightKg else weightKg * (1.0 + reps / 30.0)
    }

    private fun estimateOf(set: WorkoutSetData): Double? =
        if (set.kind == ExerciseKind.REPS) estimate(set.weightKg, set.reps) else null

    /** Bester Satz einer Übung über alle angegebenen Einheiten, null ohne passenden Satz. */
    fun best(exerciseId: Long, sessions: List<WorkoutSessionData>, sets: List<WorkoutSetData>): OneRepMaxBest? {
        val dateBySession = sessions.associate { it.id to it.dateEpochDay }
        return sets
            .filter { it.exerciseId == exerciseId && it.sessionId in dateBySession }
            .mapNotNull { set -> estimateOf(set)?.let { set to it } }
            // Bei Gleichstand zählt der frühere Satz (dort wurde der Wert zuerst erreicht)
            .maxWithOrNull(compareBy<Pair<WorkoutSetData, Double>>({ it.second }, { -it.first.order }))
            ?.let { (set, value) ->
                OneRepMaxBest(value, set.weightKg, set.reps, dateBySession.getValue(set.sessionId))
            }
    }

    /** Bestes geschätztes 1RM pro Trainingstag, chronologisch aufsteigend. */
    fun progress(exerciseId: Long, sessions: List<WorkoutSessionData>, sets: List<WorkoutSetData>): List<ProgressPoint> {
        val dateBySession = sessions.associate { it.id to it.dateEpochDay }
        return sets
            .filter { it.exerciseId == exerciseId && it.sessionId in dateBySession }
            .mapNotNull { set -> estimateOf(set)?.let { dateBySession.getValue(set.sessionId) to it } }
            .groupBy({ it.first }, { it.second })
            .map { (day, values) -> ProgressPoint(day, values.max()) }
            .sortedBy { it.dateEpochDay }
    }

    /**
     * Neuer Rekord in der laufenden Einheit [sessionId]: bestes 1RM dieser Einheit gegen das beste
     * aller früheren Einheiten. Ohne frühere Werte gibt es keinen Rekord (sonst wäre jedes erste Training einer).
     * Ergebnis: (neu, vorher) oder null.
     */
    fun newRecord(
        exerciseId: Long,
        sessionId: Long,
        sessions: List<WorkoutSessionData>,
        sets: List<WorkoutSetData>
    ): Pair<Double, Double>? {
        val current = sessions.firstOrNull { it.id == sessionId } ?: return null
        val earlier = sessions.filter { it.id != sessionId && it.endedAt != null && it.startedAt < current.startedAt }

        val now = best(exerciseId, listOf(current), sets)?.estimateKg ?: return null
        val before = best(exerciseId, earlier, sets)?.estimateKg ?: return null
        return if (now > before + 0.001) now to before else null
    }

    /** Gewicht auf 0,5 kg gerundet, wie es sich mit üblichen Scheiben zusammenstecken lässt. */
    fun roundToPlate(kg: Double): Double = (kg * 2).roundToInt() / 2.0

    /** Prozent-Tabelle. Wiederholungen aus der umgekehrten Epley-Formel, mindestens 1. */
    fun table(oneRepMaxKg: Double): List<OneRepMaxRow> =
        PERCENTS.map { percent ->
            val fraction = percent / 100.0
            val reps = if (percent == 100) 1 else (30.0 * (1.0 / fraction - 1.0)).roundToInt().coerceAtLeast(1)
            OneRepMaxRow(percent, roundToPlate(oneRepMaxKg * fraction), reps)
        }
}
