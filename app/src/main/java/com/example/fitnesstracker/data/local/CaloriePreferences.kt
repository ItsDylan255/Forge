package com.example.fitnesstracker.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fester Abzug vom berechneten Kalorienverbrauch (kcal pro Tag, 0 = kein Abzug).
 * Wird nach der Berechnung immer vom Gesamtumsatz abgezogen, das Tagesziel sinkt mit.
 */
object CaloriePreferences {

    const val MAX_REDUCTION = 2000

    private const val PREFS = "calorie_prefs"
    private const val KEY_REDUCTION = "tdee_reduction_kcal"

    private val reduction = MutableStateFlow(0)
    @Volatile private var loaded = false

    /** Aktueller Abzug als Flow; ändert sich sofort beim Speichern. */
    fun reductionFlow(context: Context): StateFlow<Int> {
        ensureLoaded(context)
        return reduction.asStateFlow()
    }

    fun getReduction(context: Context): Int {
        ensureLoaded(context)
        return reduction.value
    }

    fun setReduction(context: Context, kcal: Int) {
        val value = kcal.coerceIn(0, MAX_REDUCTION)
        prefs(context).edit().putInt(KEY_REDUCTION, value).apply()
        loaded = true
        reduction.value = value
    }

    private fun ensureLoaded(context: Context) {
        if (loaded) return
        reduction.value = prefs(context).getInt(KEY_REDUCTION, 0)
        loaded = true
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
