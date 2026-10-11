package com.example.fitnesstracker.domain.steps

import kotlin.math.roundToInt

/** Berechnet, wie viele Schritte seit dem letzten Sensorwert dazugekommen sind. */
object StepDeltaCalculator {

    /**
     * [lastTotal] = zuletzt gespeicherter Rohwert (kumuliert seit Gerätestart), null = noch nie gelesen.
     *
     * - Erster Wert überhaupt: nur Basiswert, es werden 0 Schritte gutgeschrieben.
     * - Neuer Wert < letzter Wert: Das Gerät wurde neu gestartet, der Zähler begann bei 0.
     */
    fun delta(lastTotal: Long?, newTotal: Long): Long = when {
        newTotal < 0L -> 0L
        lastTotal == null -> 0L
        newTotal >= lastTotal -> newTotal - lastTotal
        else -> newTotal
    }
}

data class StepGoalProgress(
    val steps: Int,
    val goal: Int,
    /** 0.0 bis 1.0, für den Balken */
    val barFraction: Double,
    /** Abgerundet und nicht begrenzt: 100 % gibt es erst, wenn das Ziel wirklich erreicht ist */
    val percent: Int,
    val remaining: Int,
    val reached: Boolean
)

data class StepSummary(
    val totalSteps: Long,
    /** Anzahl der Tage im Zeitraum, für die Schritte gespeichert sind */
    val daysWithData: Int,
    /** Durchschnitt über Tage mit Daten, null ohne Daten */
    val averagePerDay: Int?
)

object StepCalculator {

    fun stepsOn(days: List<StepDay>, epochDay: Long): Int =
        days.filter { it.dateEpochDay == epochDay }.sumOf { it.steps }

    fun goalProgress(steps: Int, goal: Int): StepGoalProgress {
        val safeGoal = goal.coerceAtLeast(1)
        val safeSteps = steps.coerceAtLeast(0)
        val fraction = safeSteps.toDouble() / safeGoal

        return StepGoalProgress(
            steps = safeSteps,
            goal = safeGoal,
            barFraction = fraction.coerceIn(0.0, 1.0),
            percent = (safeSteps.toLong() * 100L / safeGoal).toInt(),
            remaining = (safeGoal - safeSteps).coerceAtLeast(0),
            reached = safeSteps >= safeGoal
        )
    }

    /**
     * Summe und Durchschnitt der letzten [windowDays] Kalendertage inklusive heute.
     * Tage ohne gespeicherte Schritte zählen nicht in den Durchschnitt.
     */
    fun summary(days: List<StepDay>, windowDays: Int, today: Long): StepSummary {
        val windowStart = today - (windowDays - 1)
        val inWindow = days.filter { it.dateEpochDay in windowStart..today }

        val total = inWindow.sumOf { it.steps.toLong() }
        val dayCount = inWindow.map { it.dateEpochDay }.distinct().size

        return StepSummary(
            totalSteps = total,
            daysWithData = dayCount,
            averagePerDay = if (dayCount == 0) null else (total.toDouble() / dayCount).roundToInt()
        )
    }
}
