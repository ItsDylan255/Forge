package com.example.fitnesstracker.domain.weight

enum class ChartRange(val label: String, val days: Int?) {
    WEEK("7 Tage", 7),
    MONTH("30 Tage", 30),
    QUARTER("90 Tage", 90),
    YEAR("1 Jahr", 365),
    ALL("Gesamt", null)
}

object WeightChartData {

    /** Ab dieser Anzahl Messungen im Zeitraum wird ein Diagramm gezeichnet. */
    const val MIN_POINTS = 2

    /** Echte Messungen im gewählten Zeitraum, chronologisch aufsteigend. Nichts wird erfunden. */
    fun pointsFor(
        measurements: List<WeightMeasurement>,
        range: ChartRange,
        today: Long
    ): List<WeightMeasurement> {
        val sorted = measurements.sortedBy { it.dateEpochDay }
        val days = range.days ?: return sorted
        return sorted.filter { it.dateEpochDay in (today - days)..today }
    }

    fun hasEnoughPoints(points: List<WeightMeasurement>): Boolean =
        points.size >= MIN_POINTS
}