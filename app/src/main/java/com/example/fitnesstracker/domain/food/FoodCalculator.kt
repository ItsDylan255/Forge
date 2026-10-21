package com.example.fitnesstracker.domain.food

import kotlin.math.roundToInt

data class DaySum(
    val epochDay: Long,
    val totals: Macros,
    val entryCount: Int
)

data class FoodAverage(
    /** Anzahl der Tage mit mindestens einem Eintrag */
    val daysWithData: Int,
    /** Durchschnitt pro Tag ueber Tage mit Eintraegen, null ohne Daten */
    val perDay: Macros?
)

object FoodCalculator {

    /** Faktor = Gramm / 100. Werte pro 100 g werden auf die gegessene Menge hochgerechnet. */
    fun scale(per100g: Macros, grams: Double): Macros = per100g * (grams / 100.0)

    /** Rechnet einen gespeicherten Eintrag proportional auf eine neue Grammzahl um. */
    fun rescale(macros: Macros, oldGrams: Double, newGrams: Double): Macros =
        if (oldGrams <= 0.0) macros else macros * (newGrams / oldGrams)

    fun dayTotals(entries: List<FoodLogData>, epochDay: Long): Macros =
        entries.filter { it.dateEpochDay == epochDay }
            .fold(Macros.ZERO) { sum, entry -> sum + entry.macros }

    /** Genau ein Eintrag pro Kalendertag der letzten [days] Tage inklusive [today], aelteste zuerst. */
    fun dailySums(entries: List<FoodLogData>, days: Int, today: Long): List<DaySum> {
        val byDay = entries.groupBy { it.dateEpochDay }
        return (today - (days - 1)..today).map { day ->
            val list = byDay[day].orEmpty()
            DaySum(
                epochDay = day,
                totals = list.fold(Macros.ZERO) { sum, entry -> sum + entry.macros },
                entryCount = list.size
            )
        }
    }

    /** Durchschnitt pro Tag, nur ueber Tage mit mindestens einem Eintrag. */
    fun average(sums: List<DaySum>): FoodAverage {
        val withData = sums.filter { it.entryCount > 0 }
        if (withData.isEmpty()) return FoodAverage(0, null)

        val total = withData.fold(Macros.ZERO) { sum, day -> sum + day.totals }
        return FoodAverage(withData.size, total * (1.0 / withData.size))
    }

    /** Anteil des Tagesziels fuer den Balken (0.0 bis 1.0). null = kein Ziel gesetzt. */
    fun goalFraction(kcal: Double, goalKcal: Int?): Double? =
        if (goalKcal == null || goalKcal <= 0) null
        else (kcal / goalKcal).coerceIn(0.0, 1.0)

    /** Prozent des Tagesziels, abgerundet und nicht begrenzt. null = kein Ziel gesetzt. */
    fun goalPercent(kcal: Double, goalKcal: Int?): Int? =
        if (goalKcal == null || goalKcal <= 0) null
        else (kcal / goalKcal * 100.0).toInt()

    fun roundedKcal(kcal: Double): Int = kcal.roundToInt()
}
