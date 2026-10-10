package com.example.fitnesstracker.domain.weight

import kotlin.math.abs

enum class GoalDirection { LOSE, GAIN, REACHED }

data class GoalProgress(
    val targetKg: Double,
    val currentKg: Double,
    /** Betrag der Differenz zum Ziel */
    val remainingKg: Double,
    val direction: GoalDirection,
    /** 0.0 bis 1.0, oder null, wenn es keinen sinnvollen Ausgangswert gibt */
    val progressFraction: Double?
)

object WeightGoalCalculator {

    /**
     * [startKg] ist das Gewicht beim Setzen des Ziels.
     * Ein Prozentwert wird nur berechnet, wenn [startKg] bekannt ist und vom Ziel abweicht.
     * Es werden keine Zeitprognosen berechnet.
     */
    fun progress(
        targetKg: Double,
        currentKg: Double,
        startKg: Double?
    ): GoalProgress {
        val direction = when {
            startKg != null && startKg > targetKg ->
                if (currentKg <= targetKg) GoalDirection.REACHED else GoalDirection.LOSE

            startKg != null && startKg < targetKg ->
                if (currentKg >= targetKg) GoalDirection.REACHED else GoalDirection.GAIN

            currentKg == targetKg -> GoalDirection.REACHED
            currentKg > targetKg -> GoalDirection.LOSE
            else -> GoalDirection.GAIN
        }

        val fraction =
            if (startKg != null && startKg != targetKg) {
                ((startKg - currentKg) / (startKg - targetKg)).coerceIn(0.0, 1.0)
            } else {
                null
            }

        return GoalProgress(
            targetKg = targetKg,
            currentKg = currentKg,
            remainingKg = abs(currentKg - targetKg),
            direction = direction,
            progressFraction = fraction
        )
    }
}