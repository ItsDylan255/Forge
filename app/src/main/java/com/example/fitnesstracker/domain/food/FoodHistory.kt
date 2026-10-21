package com.example.fitnesstracker.domain.food

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ein Tag im Verlauf mit seinen Summen. */
data class HistoryDay(
    val epochDay: Long,
    val totals: Macros,
    val entryCount: Int
)

object FoodHistory {

    private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.GERMANY)

    /** Alle Tage mit mindestens einem Eintrag, der neueste zuerst. */
    fun days(entries: List<FoodLogData>): List<HistoryDay> =
        entries
            .groupBy { it.dateEpochDay }
            .map { (day, list) ->
                HistoryDay(
                    epochDay = day,
                    totals = list.fold(Macros.ZERO) { sum, entry -> sum + entry.macros },
                    entryCount = list.size
                )
            }
            .sortedByDescending { it.epochDay }

    /** Monat eines Tages für die Gruppierung: "Oktober 2026". */
    fun monthLabel(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(monthFormatter)
}
