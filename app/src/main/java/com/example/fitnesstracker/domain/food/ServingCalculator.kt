package com.example.fitnesstracker.domain.food

import kotlin.math.roundToLong

/** Eine Portion eines Lebensmittels, zum Beispiel "Scheibe" mit 30 g. */
data class PortionData(val name: String, val grams: Double)

/** Rechenlogik für Portionen und Stückzahlen. */
object ServingCalculator {

    const val MIN_COUNT = 0.5
    const val MAX_COUNT = 100.0

    /** Gramm für [count] Portionen zu je [portionGrams], auf zwei Nachkommastellen gerundet. */
    fun gramsFor(portionGrams: Double, count: Double): Double = round2(portionGrams * count)

    /** Gramm einer Portion einer Mahlzeit, die [servings] Portionen ergibt. Ohne gültige Zahl: das Ganze. */
    fun mealServingGrams(totalGrams: Double, servings: Double): Double =
        if (servings <= 0.0) totalGrams else round2(totalGrams / servings)

    /**
     * Schritt der Anzahl-Knöpfe: Von unten geht es 0,5 → 1 → 2 → 3 …, nach unten 3 → 2 → 1 → 0,5.
     * Weniger als 0,5 und mehr als 100 gibt es nicht.
     */
    fun stepCount(count: Double, up: Boolean): Double =
        if (up) {
            if (count < 1.0) 1.0 else (count + 1.0).coerceAtMost(MAX_COUNT)
        } else {
            (count - 1.0).coerceAtLeast(MIN_COUNT)
        }

    /** Vorauswahl auf der Mengen-Seite: die erste Portion, falls es eine gibt, sonst Gramm (null). */
    fun defaultPortionIndex(portionCount: Int): Int? = if (portionCount > 0) 0 else null

    /** Hinweis für das Tagebuch: "2 × Scheibe", "1,5 × Portion". */
    fun entryNote(count: Double, portionName: String): String =
        "${FoodFormat.number(count)} \u00D7 $portionName"

    private fun round2(value: Double): Double = (value * 100.0).roundToLong() / 100.0
}
