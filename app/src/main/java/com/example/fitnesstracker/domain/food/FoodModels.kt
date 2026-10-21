package com.example.fitnesstracker.domain.food

import com.example.fitnesstracker.data.local.FoodLogEntry

/** Kalorien und Makros (Gramm). Wird fuer Werte pro 100 g und fuer Summen benutzt. */
data class Macros(
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
) {
    operator fun plus(other: Macros) =
        Macros(kcal + other.kcal, protein + other.protein, carbs + other.carbs, fat + other.fat)

    operator fun times(factor: Double) =
        Macros(kcal * factor, protein * factor, carbs * factor, fat * factor)

    companion object {
        val ZERO = Macros(0.0, 0.0, 0.0, 0.0)
    }
}

/** Room-unabhaengige Sicht auf einen eingetragenen Eintrag (Snapshot-Werte). */
data class FoodLogData(
    val id: Long,
    val dateEpochDay: Long,
    val name: String,
    val grams: Double,
    val macros: Macros,
    /** Anzeige bei Eintrag per Portion, zum Beispiel "2 × Scheibe". Null = nur Gramm. */
    val servingNote: String? = null
)

fun FoodLogEntry.toData(): FoodLogData =
    FoodLogData(
        id = id,
        dateEpochDay = dateEpochDay,
        name = foodName,
        grams = grams,
        macros = Macros(kcal = calories, protein = protein, carbs = carbs, fat = fat),
        servingNote = servingNote
    )
