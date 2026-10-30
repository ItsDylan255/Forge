package com.example.fitnesstracker.domain.steps

enum class StepChartRange(val label: String, val days: Int) {
    WEEK("7 Tage", 7),
    MONTH("30 Tage", 30)
}

data class StepBar(
    val dateEpochDay: Long,
    val steps: Int
)

object StepChartData {

    /**
     * Genau ein Balken pro Kalendertag der [StepChartRange.days] Tage bis einschließlich [endDay]
     * (normalerweise heute), chronologisch aufsteigend. Tage ohne Eintrag haben 0 Schritte.
     */
    fun barsFor(days: List<StepDay>, range: StepChartRange, endDay: Long): List<StepBar> {
        val stepsByDay = days.groupBy { it.dateEpochDay }
            .mapValues { (_, list) -> list.sumOf { it.steps } }

        val start = endDay - (range.days - 1)
        return (start..endDay).map { day -> StepBar(day, stepsByDay[day] ?: 0) }
    }

    fun hasAnySteps(bars: List<StepBar>): Boolean = bars.any { it.steps > 0 }
}
