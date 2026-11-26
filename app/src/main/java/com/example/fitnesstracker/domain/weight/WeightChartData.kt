package com.example.fitnesstracker.domain.weight

import com.example.fitnesstracker.i18n.tr

enum class ChartRange(private val de: String, private val en: String, val days: Int?) {
    WEEK("7 Tage", "7 days", 7),
    MONTH("30 Tage", "30 days", 30),
    QUARTER("90 Tage", "90 days", 90),
    YEAR("1 Jahr", "1 year", 365),
    ALL("Gesamt", "All", null);

    /** Text in der aktuellen Sprache. */
    val label: String get() = tr(de, en)
}

/**
 * Ausschnitt für ein verschiebbares Diagramm: [visible] liegt im Fenster [startDay]..[endDay],
 * [line] enthält zusätzlich je den nächsten Punkt davor und danach, damit die Linie bis zum Rand läuft.
 */
data class WeightChartSlice(
    val visible: List<WeightMeasurement>,
    val line: List<WeightMeasurement>,
    val startDay: Long,
    val endDay: Long,
    /** Gleitender 7-Tage-Durchschnitt pro Messtag (inkl. je einem Punkt außerhalb des Fensters). */
    val trend: List<TrendPoint> = emptyList()
)

/** Ein Punkt der Durchschnittslinie. */
data class TrendPoint(val dateEpochDay: Long, val weightKg: Double)

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

        val trend = movingAverage(sorted)

        val days = range.days ?: return WeightChartSlice(
            sorted, sorted, sorted.first().dateEpochDay, sorted.last().dateEpochDay, trend
        )
        val start = endDay - days
        val visible = sorted.filter { it.dateEpochDay in start..endDay }
        val before = sorted.lastOrNull { it.dateEpochDay < start }
        val after = sorted.firstOrNull { it.dateEpochDay > endDay }
        val trendVisible = trend.filter { it.dateEpochDay in start..endDay }
        val trendBefore = trend.lastOrNull { it.dateEpochDay < start }
        val trendAfter = trend.firstOrNull { it.dateEpochDay > endDay }
        return WeightChartSlice(
            visible, listOfNotNull(before) + visible + listOfNotNull(after), start, endDay,
            listOfNotNull(trendBefore) + trendVisible + listOfNotNull(trendAfter)
        )
    }

    /** Länge des gleitenden Durchschnitts in Tagen. */
    const val TREND_DAYS = 7

    /**
     * Gleitender Durchschnitt: Für jeden Messtag der Mittelwert aller Messungen der letzten
     * [TREND_DAYS] Tage (inklusive dieses Tages). Mehrere Messungen an einem Tag zählen einzeln.
     */
    fun movingAverage(measurements: List<WeightMeasurement>, windowDays: Int = TREND_DAYS): List<TrendPoint> {
        val sorted = measurements.sortedBy { it.dateEpochDay }
        val days = sorted.map { it.dateEpochDay }.distinct()
        var from = 0
        var sum = 0.0
        var to = 0
        return days.map { day ->
            while (to < sorted.size && sorted[to].dateEpochDay <= day) {
                sum += sorted[to].weightKg
                to++
            }
            while (sorted[from].dateEpochDay <= day - windowDays) {
                sum -= sorted[from].weightKg
                from++
            }
            TrendPoint(day, sum / (to - from))
        }
    }
}
