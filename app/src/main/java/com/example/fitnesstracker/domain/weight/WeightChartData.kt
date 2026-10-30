package com.example.fitnesstracker.domain.weight

enum class ChartRange(val label: String, val days: Int?) {
    WEEK("7 Tage", 7),
    MONTH("30 Tage", 30),
    QUARTER("90 Tage", 90),
    YEAR("1 Jahr", 365),
    ALL("Gesamt", null)
}

/**
 * Ausschnitt für ein verschiebbares Diagramm: [visible] liegt im Fenster [startDay]..[endDay],
 * [line] enthält zusätzlich je den nächsten Punkt davor und danach, damit die Linie bis zum Rand läuft.
 */
data class WeightChartSlice(
    val visible: List<WeightMeasurement>,
    val line: List<WeightMeasurement>,
    val startDay: Long,
    val endDay: Long
)

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

    /**
     * Fenster des Zeitraums mit dem Ende [endDay] (statt heute). Bei „Gesamt“ reicht es vom ersten
     * bis zum letzten Eintrag. Ohne Messungen null.
     */
    fun sliceFor(measurements: List<WeightMeasurement>, range: ChartRange, endDay: Long): WeightChartSlice? {
        val sorted = measurements.sortedBy { it.dateEpochDay }
        if (sorted.isEmpty()) return null

        val days = range.days ?: return WeightChartSlice(
            sorted, sorted, sorted.first().dateEpochDay, sorted.last().dateEpochDay
        )
        val start = endDay - days
        val visible = sorted.filter { it.dateEpochDay in start..endDay }
        val before = sorted.lastOrNull { it.dateEpochDay < start }
        val after = sorted.firstOrNull { it.dateEpochDay > endDay }
        return WeightChartSlice(visible, listOfNotNull(before) + visible + listOfNotNull(after), start, endDay)
    }
}
