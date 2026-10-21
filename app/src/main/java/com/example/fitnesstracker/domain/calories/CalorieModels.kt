package com.example.fitnesstracker.domain.calories

/** Geschlecht für die BMR-Formel. */
enum class Sex { MALE, FEMALE }

/** Aktivitätslevel (PAL-Faktor). */
enum class ActivityLevel(val pal: Double) {
    SEDENTARY(1.2),
    LIGHT(1.4),
    MODERATE(1.55),
    VERY_ACTIVE(1.725)
}

/** Makro-Verteilung in Prozent. */
data class MacroSplit(val proteinPercent: Int, val carbsPercent: Int, val fatPercent: Int)

/** Profildaten für die Berechnung. */
data class CalorieProfile(
    val heightCm: Int,
    val ageYears: Int,
    val sex: Sex,
    val activity: ActivityLevel,
    val split: MacroSplit
)

/** Zielangaben aus dem Gewichtsziel. startKg null = aktuelles Gewicht gilt als Start. */
data class CalorieGoalInput(
    val startKg: Double?,
    val targetKg: Double,
    val targetEpochDay: Long?
)

/** Richtung, automatisch aus Start- und Zielgewicht abgeleitet. */
enum class CalorieDirection { LOSE, MAINTAIN, GAIN }

/**
 * Hinweis zum Ergebnis. TOO_TIGHT = das Zieldatum ist mit dem erlaubten Defizit bzw. Überschuss
 * nicht zu schaffen, das Tagesziel wurde auf die Grenze gesetzt.
 */
enum class CalorieNote { NONE, NO_GOAL, NO_DATE, DATE_PASSED, TOO_TIGHT, REACHED }

/** Makros in Gramm. */
data class MacroGrams(val proteinG: Int, val carbsG: Int, val fatG: Int)

/** Ergebnis der Kalorienberechnung. */
data class CalorieTarget(
    val bmr: Int,
    val tdee: Int,
    val targetKcal: Int,
    val direction: CalorieDirection,
    val note: CalorieNote,
    val macros: MacroGrams,
    /** Tatsächliches Defizit bzw. Plus gegenüber dem Gesamtumsatz in kcal (immer positiv, 0 = keins). */
    val appliedDeltaKcal: Int = 0,
    /** Bei TOO_TIGHT: voraussichtlicher Tag, an dem das Gewicht mit dem begrenzten Ziel erreicht wird. */
    val estimatedEpochDay: Long? = null
)
