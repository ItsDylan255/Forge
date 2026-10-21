package com.example.fitnesstracker.domain.calories

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Reine Kalorienberechnung (Mifflin-St Jeor, TDEE, Zieldatum, Makros). */
object CalorieCalculator {

    const val KCAL_PER_KG = 7700.0

    /** Größtes erlaubtes Defizit: 25 % des Gesamtumsatzes, höchstens 1.000 kcal pro Tag. */
    const val MAX_DEFICIT_FRACTION = 0.25
    const val MAX_DEFICIT_KCAL = 1000.0

    /** Größtes erlaubtes Plus pro Tag. */
    const val MAX_SURPLUS_KCAL = 500.0

    /** Schätzungen über zehn Jahre hinaus gelten als "nicht absehbar". */
    const val MAX_ESTIMATE_DAYS = 3650L

    /** Grundumsatz nach Mifflin-St Jeor. */
    fun bmr(weightKg: Double, heightCm: Int, ageYears: Int, sex: Sex): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        return if (sex == Sex.MALE) base + 5.0 else base - 161.0
    }

    /** Gesamtumsatz = BMR * PAL. */
    fun tdee(bmr: Double, activity: ActivityLevel): Double = bmr * activity.pal

    /** Makros in Gramm: Protein/Carbs 4 kcal/g, Fett 9 kcal/g. */
    fun macros(kcal: Int, split: MacroSplit): MacroGrams = MacroGrams(
        proteinG = (kcal * split.proteinPercent / 100.0 / 4.0).roundToInt(),
        carbsG = (kcal * split.carbsPercent / 100.0 / 4.0).roundToInt(),
        fatG = (kcal * split.fatPercent / 100.0 / 9.0).roundToInt()
    )

    /**
     * Tagesziel. Richtung aus Start/Ziel; Rest-kg ab aktuellem Gewicht.
     * Abnehmen: höchstens 25 % des Gesamtumsatzes bzw. 1.000 kcal Defizit und nie unter dem BMR.
     * Zunehmen: höchstens 500 kcal Plus. Reicht das für das Zieldatum nicht, wird das Tagesziel auf
     * die Grenze gesetzt (Hinweis TOO_TIGHT) und der voraussichtliche Termin berechnet.
     */
    fun calculate(
        profile: CalorieProfile,
        currentKg: Double,
        goal: CalorieGoalInput?,
        todayEpochDay: Long
    ): CalorieTarget {
        val bmr = bmr(currentKg, profile.heightCm, profile.ageYears, profile.sex)
        val tdee = tdee(bmr, profile.activity)

        fun result(
            kcal: Double,
            dir: CalorieDirection,
            note: CalorieNote,
            estimatedEpochDay: Long? = null
        ): CalorieTarget {
            val k = kcal.roundToInt()
            return CalorieTarget(
                bmr = bmr.roundToInt(),
                tdee = tdee.roundToInt(),
                targetKcal = k,
                direction = dir,
                note = note,
                macros = macros(k, profile.split),
                appliedDeltaKcal = abs(tdee - kcal).roundToInt(),
                estimatedEpochDay = estimatedEpochDay
            )
        }

        if (goal == null) return result(tdee, CalorieDirection.MAINTAIN, CalorieNote.NO_GOAL)

        val start = goal.startKg ?: currentKg
        val direction = when {
            abs(start - goal.targetKg) < 1e-9 -> CalorieDirection.MAINTAIN
            start > goal.targetKg -> CalorieDirection.LOSE
            else -> CalorieDirection.GAIN
        }
        if (direction == CalorieDirection.MAINTAIN) {
            return result(tdee, direction, CalorieNote.NONE)
        }

        val remaining = if (direction == CalorieDirection.LOSE) {
            currentKg - goal.targetKg
        } else {
            goal.targetKg - currentKg
        }
        if (remaining <= 0.0) {
            return result(tdee, CalorieDirection.MAINTAIN, CalorieNote.REACHED)
        }

        val endDay = goal.targetEpochDay ?: return result(tdee, direction, CalorieNote.NO_DATE)
        val days = endDay - todayEpochDay
        if (days <= 0) return result(tdee, direction, CalorieNote.DATE_PASSED)

        val delta = KCAL_PER_KG * remaining / days

        /** Voraussichtlicher Tag mit dem begrenzten Ziel: Rest-kg geteilt durch das tatsächliche Defizit/Plus. */
        fun estimateWith(limitedKcal: Double): Long? {
            val applied = abs(tdee - limitedKcal)
            if (applied <= 0.0) return null
            val daysNeeded = ceil(remaining * KCAL_PER_KG / applied).toLong()
            return if (daysNeeded in 1..MAX_ESTIMATE_DAYS) todayEpochDay + daysNeeded else null
        }

        return if (direction == CalorieDirection.LOSE) {
            val maxDeficit = min(MAX_DEFICIT_KCAL, MAX_DEFICIT_FRACTION * tdee)
            val floor = max(bmr, tdee - maxDeficit)
            val raw = tdee - delta

            if (raw < floor) result(floor, direction, CalorieNote.TOO_TIGHT, estimateWith(floor))
            else result(raw, direction, CalorieNote.NONE)
        } else {
            val ceiling = tdee + MAX_SURPLUS_KCAL
            val raw = tdee + delta

            if (raw > ceiling) result(ceiling, direction, CalorieNote.TOO_TIGHT, estimateWith(ceiling))
            else result(raw, direction, CalorieNote.NONE)
        }
    }
}
