package com.example.fitnesstracker.data.local

import android.content.Context
import com.example.fitnesstracker.domain.calories.CalorieSettings
import com.example.fitnesstracker.domain.calories.DeficitMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Einstellungen zur Kalorienberechnung:
 * - fester Abzug vom berechneten Verbrauch (kcal pro Tag, 0 = kein Abzug)
 * - maximales Defizit (0 = Standardgrenze)
 * - Automatik an/aus und manuelles Defizit (feste kcal oder Prozent pro Woche)
 */
object CaloriePreferences {

    const val MAX_REDUCTION = 2000
    const val MIN_MAX_DEFICIT = 100
    const val MAX_MAX_DEFICIT = 2000
    const val MIN_FIXED_DEFICIT = 50
    const val MAX_FIXED_DEFICIT = 2000
    const val MIN_PERCENT = 0.1
    const val MAX_PERCENT = 2.0

    private const val PREFS = "calorie_prefs"
    private const val KEY_REDUCTION = "tdee_reduction_kcal"
    private const val KEY_MAX_DEFICIT = "max_deficit_kcal"
    private const val KEY_MODE = "deficit_mode"
    private const val KEY_FIXED = "fixed_deficit_kcal"
    private const val KEY_PERCENT = "percent_per_week"

    private val settings = MutableStateFlow(CalorieSettings())
    @Volatile private var loaded = false

    /** Alle Einstellungen als Flow; ändert sich sofort beim Speichern. */
    fun settingsFlow(context: Context): StateFlow<CalorieSettings> {
        ensureLoaded(context)
        return settings.asStateFlow()
    }

    fun getSettings(context: Context): CalorieSettings {
        ensureLoaded(context)
        return settings.value
    }

    /** Speichert die Einstellungen (Werte werden auf die erlaubten Bereiche begrenzt). */
    fun setSettings(context: Context, value: CalorieSettings) {
        val clean = value.copy(
            reductionKcal = value.reductionKcal.coerceIn(0, MAX_REDUCTION),
            maxDeficitKcal = value.maxDeficitKcal?.coerceIn(MIN_MAX_DEFICIT, MAX_MAX_DEFICIT),
            fixedDeficitKcal = value.fixedDeficitKcal.coerceIn(MIN_FIXED_DEFICIT, MAX_FIXED_DEFICIT),
            percentPerWeek = value.percentPerWeek.coerceIn(MIN_PERCENT, MAX_PERCENT)
        )
        prefs(context).edit()
            .putInt(KEY_REDUCTION, clean.reductionKcal)
            .putInt(KEY_MAX_DEFICIT, clean.maxDeficitKcal ?: 0)
            .putString(KEY_MODE, clean.mode.name)
            .putInt(KEY_FIXED, clean.fixedDeficitKcal)
            .putFloat(KEY_PERCENT, clean.percentPerWeek.toFloat())
            .apply()
        loaded = true
        settings.value = clean
    }

    fun setReduction(context: Context, kcal: Int) =
        setSettings(context, getSettings(context).copy(reductionKcal = kcal))

    private fun ensureLoaded(context: Context) {
        if (loaded) return
        val p = prefs(context)
        val defaults = CalorieSettings()
        settings.value = CalorieSettings(
            reductionKcal = p.getInt(KEY_REDUCTION, 0),
            maxDeficitKcal = p.getInt(KEY_MAX_DEFICIT, 0).takeIf { it > 0 },
            mode = p.getString(KEY_MODE, null)
                ?.let { name -> DeficitMode.entries.firstOrNull { it.name == name } }
                ?: DeficitMode.AUTO,
            fixedDeficitKcal = p.getInt(KEY_FIXED, defaults.fixedDeficitKcal),
            // Auf eine Nachkommastelle runden, damit Float-Ungenauigkeiten nicht sichtbar werden
            percentPerWeek = Math.round(p.getFloat(KEY_PERCENT, defaults.percentPerWeek.toFloat()) * 100.0) / 100.0
        )
        loaded = true
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
