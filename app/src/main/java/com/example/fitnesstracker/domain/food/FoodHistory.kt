package com.example.fitnesstracker.domain.food

import com.example.fitnesstracker.i18n.L10n
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Ein Tag im Verlauf mit seinen Summen. */
data class HistoryDay(
    val epochDay: Long,
    val totals: Macros,
    val entryCount: Int
)

object FoodHistory {

    private val monthFormatter get() = DateTimeFormatter.ofPattern("LLLL yyyy", L10n.locale)

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
