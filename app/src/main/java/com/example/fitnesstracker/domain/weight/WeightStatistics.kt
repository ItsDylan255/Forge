package com.example.fitnesstracker.domain.weight

data class CurrentWeight(
    val weightKg: Double,
    val dateEpochDay: Long,
    /** true = Eintrag von heute, false = letzter Messwert an einem früheren Tag */
    val isToday: Boolean
)

/** Veränderung zwischen zwei echten Messungen (mit deren Daten, damit die UI transparent bleibt). */
data class WeightChange(
    val deltaKg: Double,
    val fromEpochDay: Long,
    val toEpochDay: Long
)

data class WeightExtreme(
    val weightKg: Double,
    val dateEpochDay: Long
)

data class WeightStatistics(
    val current: CurrentWeight?,
    val latest: WeightMeasurement?,
    val changeToPrevious: WeightChange?,
    val change7Days: WeightChange?,
    val change30Days: WeightChange?,
    val changeSinceFirst: WeightChange?,
    val lowest: WeightExtreme?,
    val highest: WeightExtreme?,
    val measurementCount: Int
)

data class WeightHistoryItem(
    val measurement: WeightMeasurement,
    /** null = keine vorherige Messung vorhanden */
    val deltaToPreviousKg: Double?,
    val previousEpochDay: Long?
)

object WeightCalculator {

    fun statistics(
        measurements: List<WeightMeasurement>,
        today: Long
    ): WeightStatistics {
        val sorted = measurements.sortedBy { it.dateEpochDay }
        val latest = sorted.lastOrNull()
        val todayEntry = sorted.lastOrNull { it.dateEpochDay == today }

        val current = when {
            todayEntry != null ->
                CurrentWeight(todayEntry.weightKg, todayEntry.dateEpochDay, isToday = true)

            latest != null ->
                CurrentWeight(latest.weightKg, latest.dateEpochDay, isToday = false)

            else -> null
        }

        // Bei gleichem Gewicht gewinnt der früheste Tag (Reihenfolge ist aufsteigend nach Datum).
        val lowest = sorted.minByOrNull { it.weightKg }
            ?.let { WeightExtreme(it.weightKg, it.dateEpochDay) }
        val highest = sorted.maxByOrNull { it.weightKg }
            ?.let { WeightExtreme(it.weightKg, it.dateEpochDay) }

        return WeightStatistics(
            current = current,
            latest = latest,
            changeToPrevious = changeBetween(sorted.getOrNull(sorted.size - 2), latest),
            change7Days = changeOverDays(sorted, 7, today),
            change30Days = changeOverDays(sorted, 30, today),
            changeSinceFirst =
                if (sorted.size >= 2) changeBetween(sorted.first(), latest) else null,
            lowest = lowest,
            highest = highest,
            measurementCount = sorted.size
        )
    }

    /**
     * Veränderung innerhalb der letzten [days] Tage.
     * Betrachtet werden nur echte Messungen im Zeitraum [today - days, today].
     * Verglichen werden die erste und die letzte Messung darin.
     * Gibt es weniger als zwei Messungen im Zeitraum, ist das Ergebnis null.
     */
    fun changeOverDays(
        measurements: List<WeightMeasurement>,
        days: Int,
        today: Long
    ): WeightChange? {
        val windowStart = today - days
        val inWindow = measurements
            .filter { it.dateEpochDay in windowStart..today }
            .sortedBy { it.dateEpochDay }

        if (inWindow.size < 2) return null
        return changeBetween(inWindow.first(), inWindow.last())
    }

    /** Neueste Messung zuerst, jeweils mit Differenz zur vorherigen Messung. */
    fun historyItems(measurements: List<WeightMeasurement>): List<WeightHistoryItem> {
        val sorted = measurements.sortedBy { it.dateEpochDay }
        return sorted
            .mapIndexed { index, measurement ->
                val previous = sorted.getOrNull(index - 1)
                WeightHistoryItem(
                    measurement = measurement,
                    deltaToPreviousKg = previous?.let { measurement.weightKg - it.weightKg },
                    previousEpochDay = previous?.dateEpochDay
                )
            }
            .reversed()
    }

    private fun changeBetween(
        from: WeightMeasurement?,
        to: WeightMeasurement?
    ): WeightChange? {
        if (from == null || to == null) return null
        return WeightChange(
            deltaKg = to.weightKg - from.weightKg,
            fromEpochDay = from.dateEpochDay,
            toEpochDay = to.dateEpochDay
        )
    }
}