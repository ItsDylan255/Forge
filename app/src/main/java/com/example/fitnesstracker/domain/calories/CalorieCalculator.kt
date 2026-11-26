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
        todayEpochDay: Long,
        settings: CalorieSettings = CalorieSettings()
    ): CalorieTarget {
        val bmr = bmr(currentKg, profile.heightCm, profile.ageYears, profile.sex)
        val tdee = max(0.0, tdee(bmr, profile.activity) - settings.reductionKcal.coerceAtLeast(0))
        val maxDeficit = settings.maxDeficitKcal?.coerceAtLeast(0)?.toDouble()
            ?: min(MAX_DEFICIT_KCAL, MAX_DEFICIT_FRACTION * tdee)
        /** Untergrenze beim Abnehmen: höchstens [maxDeficit] unter dem Gesamtumsatz und nie unter dem BMR. */
        val loseFloor = max(bmr, tdee - maxDeficit)
        val gainCeiling = tdee + MAX_SURPLUS_KCAL

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

        if (settings.mode != DeficitMode.AUTO) {
            return manual(settings, currentKg, goal, todayEpochDay, tdee, loseFloor, gainCeiling, ::result)
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
            val raw = tdee - delta

            if (raw < loseFloor) result(loseFloor, direction, CalorieNote.TOO_TIGHT, estimateWith(loseFloor))
            else result(raw, direction, CalorieNote.NONE)
        } else {
            val raw = tdee + delta

            if (raw > gainCeiling) result(gainCeiling, direction, CalorieNote.TOO_TIGHT, estimateWith(gainCeiling))
            else result(raw, direction, CalorieNote.NONE)
        }
    }

    /** kcal pro Tag für den gewünschten Gewichtsverlust: [percentPerWeek] % von [currentKg] pro Woche. */
    fun kcalForPercentPerWeek(currentKg: Double, percentPerWeek: Double): Double =
        currentKg * percentPerWeek / 100.0 * KCAL_PER_KG / 7.0

    /**
     * Manuelles Defizit (Automatik aus): feste kcal oder Prozent pro Woche, unabhängig vom Zieldatum.
     * Ohne Gewichtsziel wird abgenommen. Bei einem Zunahme-Ziel gilt derselbe Wert als Plus.
     * Die Grenzen (maximales Defizit, BMR, maximales Plus) gelten weiter.
     */
    private fun manual(
        settings: CalorieSettings,
        currentKg: Double,
        goal: CalorieGoalInput?,
        todayEpochDay: Long,
        tdee: Double,
        loseFloor: Double,
        gainCeiling: Double,
        result: (Double, CalorieDirection, CalorieNote, Long?) -> CalorieTarget
    ): CalorieTarget {
        val direction = if (goal == null) {
            CalorieDirection.LOSE
        } else {
            val start = goal.startKg ?: currentKg
            when {
                abs(start - goal.targetKg) < 1e-9 -> CalorieDirection.MAINTAIN
                start > goal.targetKg -> CalorieDirection.LOSE
                else -> CalorieDirection.GAIN
            }
        }
        if (direction == CalorieDirection.MAINTAIN) return result(tdee, direction, CalorieNote.NONE, null)

        val remaining = goal?.let {
            if (direction == CalorieDirection.LOSE) currentKg - it.targetKg else it.targetKg - currentKg
        }
        if (remaining != null && remaining <= 0.0) {
            return result(tdee, CalorieDirection.MAINTAIN, CalorieNote.REACHED, null)
        }

        val delta = when (settings.mode) {
            DeficitMode.PERCENT_PER_WEEK -> kcalForPercentPerWeek(currentKg, settings.percentPerWeek.coerceAtLeast(0.0))
            else -> settings.fixedDeficitKcal.coerceAtLeast(0).toDouble()
        }

        val raw = if (direction == CalorieDirection.LOSE) tdee - delta else tdee + delta
        val limited = if (direction == CalorieDirection.LOSE) max(raw, loseFloor) else min(raw, gainCeiling)
        val note = if (abs(limited - raw) > 0.5) CalorieNote.MANUAL_CAPPED else CalorieNote.MANUAL

        val applied = abs(tdee - limited)
        val eta = if (remaining != null && applied > 0.0) {
            val daysNeeded = ceil(remaining * KCAL_PER_KG / applied).toLong()
            if (daysNeeded in 1..MAX_ESTIMATE_DAYS) todayEpochDay + daysNeeded else null
        } else {
            null
        }
        return result(limited, direction, note, eta)
    }
}
