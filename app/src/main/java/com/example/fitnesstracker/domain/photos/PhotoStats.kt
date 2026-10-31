package com.example.fitnesstracker.domain.photos

import kotlin.math.roundToInt

/** Werte, die zusammen mit einem Fortschrittsfoto gespeichert werden (Stand an diesem Tag). */
data class PhotoStats(
    val weightKg: Double?,
    val goalWeightKg: Double?,
    val workoutsTotal: Int,
    val workouts30Days: Int,
    val avgSteps7Days: Int?,
    val avgKcal7Days: Int?,
    val kcalTarget: Int?
)

object PhotoStatsCalculator {

    /**
     * Baut die Momentaufnahme für [today].
     *
     * [weights] und [steps] sind Tag → Wert, [workoutDays] die Tage beendeter Trainings,
     * [kcalByDay] die gegessenen Kalorien pro Tag. Durchschnitte zählen nur Tage mit Einträgen
     * aus den letzten 7 Tagen (heute eingeschlossen).
     */
    fun snapshot(
        today: Long,
        weights: Map<Long, Double>,
        goalWeightKg: Double?,
        workoutDays: List<Long>,
        steps: Map<Long, Int>,
        kcalByDay: Map<Long, Double>,
        kcalTarget: Int?
    ): PhotoStats {
        val week = (today - 6)..today
        val stepsWeek = steps.filterKeys { it in week }.values.filter { it > 0 }
        val kcalWeek = kcalByDay.filterKeys { it in week }.values.filter { it > 0.0 }

        return PhotoStats(
            weightKg = weights[today] ?: weights.filterKeys { it <= today }.maxByOrNull { it.key }?.value,
            goalWeightKg = goalWeightKg,
            workoutsTotal = workoutDays.count { it <= today },
            workouts30Days = workoutDays.count { it in (today - 29)..today },
            avgSteps7Days = stepsWeek.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
            avgKcal7Days = kcalWeek.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
            kcalTarget = kcalTarget
        )
    }
}
