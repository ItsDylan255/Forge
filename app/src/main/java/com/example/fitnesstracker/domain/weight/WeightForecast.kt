package com.example.fitnesstracker.domain.weight

import kotlin.math.abs
import kotlin.math.ceil

sealed interface WeightForecast {
    /** Laut letzter Messung ist das Ziel schon erreicht. */
    object Reached : WeightForecast

    /** Voraussichtlicher Tag (Epoch-Tag), an dem das Ziel erreicht wird. */
    data class Predicted(val epochDay: Long) : WeightForecast

    /** Zu wenig Daten, kein Fortschritt in Richtung Ziel oder Ziel in mehr als 10 Jahren. */
    object Unavailable : WeightForecast

    /** Trend-Prognose: weniger als 7 Messungen in den letzten 28 Tagen. */
    object NotEnoughData : WeightForecast

    /** Trend-Prognose: Das Gewicht entwickelt sich nicht in Richtung Ziel (oder kaum). */
    object NoProgress : WeightForecast

    /** Trend-Prognose: Das Ziel wäre erst in mehr als 10 Jahren erreicht. */
    object TooFar : WeightForecast
}

object WeightForecaster {

    /** So viele Tage m\u00FCssen zwischen Zielbeginn und letzter Messung liegen. */
    const val MIN_DAYS_OF_DATA = 7

    /** Langsamer als ca. 70 g pro Woche gilt als "kein Fortschritt". */
    const val MIN_PACE_KG_PER_DAY = 0.01

    const val MAX_FORECAST_DAYS = 3650

    /** Der Trend wird aus den Messungen dieser letzten Tage berechnet. */
    const val TREND_WINDOW_DAYS = 28

    /** Mindestens so viele Messungen im Zeitraum: 7 Messungen, also etwa eine Woche. */
    const val TREND_MIN_POINTS = 7

    /**
     * Prognose bei gleichbleibendem Tempo.
     *
     * Das Tempo ist die Steigung einer Ausgleichsgeraden durch alle Messungen seit Zielbeginn
     * ([startEpochDay]) plus dem Startgewicht am Zielbeginn. Gerechnet wird ab der letzten Messung.
     */
    fun predict(
        targetKg: Double,
        startKg: Double?,
        startEpochDay: Long,
        measurements: List<WeightMeasurement>
    ): WeightForecast {
        val sorted = measurements.sortedBy { it.dateEpochDay }
        val latest = sorted.lastOrNull() ?: return WeightForecast.Unavailable

        val remaining = targetKg - latest.weightKg

        val reached = remaining == 0.0 ||
                (startKg != null && startKg > targetKg && latest.weightKg <= targetKg) ||
                (startKg != null && startKg < targetKg && latest.weightKg >= targetKg)
        if (reached) return WeightForecast.Reached

        val points = mutableListOf<Pair<Double, Double>>()
        if (startKg != null) points.add(startEpochDay.toDouble() to startKg)
        sorted.filter { it.dateEpochDay >= startEpochDay }
            .forEach { points.add(it.dateEpochDay.toDouble() to it.weightKg) }

        if (points.size < 2) return WeightForecast.Unavailable

        val spanDays = points.maxOf { it.first } - points.minOf { it.first }
        if (spanDays < MIN_DAYS_OF_DATA) return WeightForecast.Unavailable

        val meanX = points.sumOf { it.first } / points.size
        val meanY = points.sumOf { it.second } / points.size
        val denominator = points.sumOf { (it.first - meanX) * (it.first - meanX) }
        if (denominator == 0.0) return WeightForecast.Unavailable

        val slope = points.sumOf { (it.first - meanX) * (it.second - meanY) } / denominator

        // Das Tempo muss in Richtung Ziel zeigen und gro\u00DF genug sein
        if (abs(slope) < MIN_PACE_KG_PER_DAY || remaining * slope <= 0.0) {
            return WeightForecast.Unavailable
        }

        val daysNeeded = ceil(remaining / slope - 1e-9)
        if (daysNeeded > MAX_FORECAST_DAYS) return WeightForecast.Unavailable

        return WeightForecast.Predicted(latest.dateEpochDay + daysNeeded.toLong())
    }

    /**
     * Prognose aus dem Trend der letzten [TREND_WINDOW_DAYS] Tage, unabhängig davon, wann das Ziel
     * angelegt wurde. Das Tempo ist die Steigung einer Ausgleichsgeraden durch die Messungen im
     * Zeitraum, gerechnet wird ab der letzten Messung. Ein Trend gibt es ab [TREND_MIN_POINTS]
     * Messungen. Pro Tag zählt höchstens eine Messung, 7 Messungen sind also mindestens eine Woche.
     * Sagt bei Misserfolg auch, warum.
     */
    fun predictTrend(
        targetKg: Double,
        startKg: Double?,
        measurements: List<WeightMeasurement>,
        today: Long
    ): WeightForecast {
        val sorted = measurements.sortedBy { it.dateEpochDay }
        val latest = sorted.lastOrNull() ?: return WeightForecast.NotEnoughData

        val remaining = targetKg - latest.weightKg

        val reached = remaining == 0.0 ||
                (startKg != null && startKg > targetKg && latest.weightKg <= targetKg) ||
                (startKg != null && startKg < targetKg && latest.weightKg >= targetKg)
        if (reached) return WeightForecast.Reached

        val windowStart = today - (TREND_WINDOW_DAYS - 1)
        val points = sorted
            .filter { it.dateEpochDay in windowStart..today }
            .map { it.dateEpochDay.toDouble() to it.weightKg }

        if (points.size < TREND_MIN_POINTS) return WeightForecast.NotEnoughData

        val meanX = points.sumOf { it.first } / points.size
        val meanY = points.sumOf { it.second } / points.size
        val denominator = points.sumOf { (it.first - meanX) * (it.first - meanX) }
        if (denominator == 0.0) return WeightForecast.NotEnoughData

        val slope = points.sumOf { (it.first - meanX) * (it.second - meanY) } / denominator

        if (abs(slope) < MIN_PACE_KG_PER_DAY || remaining * slope <= 0.0) {
            return WeightForecast.NoProgress
        }

        val daysNeeded = ceil(remaining / slope - 1e-9)
        if (daysNeeded > MAX_FORECAST_DAYS) return WeightForecast.TooFar

        return WeightForecast.Predicted(latest.dateEpochDay + daysNeeded.toLong())
    }
}
