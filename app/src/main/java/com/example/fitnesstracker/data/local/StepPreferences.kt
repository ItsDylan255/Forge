package com.example.fitnesstracker.data.local

import android.content.Context
import com.example.fitnesstracker.domain.steps.StepReadEntry
import com.example.fitnesstracker.domain.steps.StepReadLog

/**
 * Kleine Einstellungen für den Step-Counter (ohne zusätzliche Bibliothek):
 * - letzter Rohwert des Sensors (kumuliert seit Gerätestart)
 * - Tagesziel
 */
class StepPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("step_prefs", Context.MODE_PRIVATE)

    fun getLastSensorTotal(): Long? =
        if (prefs.contains(KEY_LAST_TOTAL)) prefs.getLong(KEY_LAST_TOTAL, 0L) else null

    fun setLastSensorTotal(total: Long) {
        prefs.edit().putLong(KEY_LAST_TOTAL, total).apply()
    }

    /**
     * Einmalig nach dem Umstieg auf das Zählen im Hintergrund: Der gespeicherte Rohwert stammt vom
     * letzten Öffnen des Screens und würde alle Schritte seitdem dem heutigen Tag gutschreiben.
     * Er wird verworfen, der nächste Sensorwert dient dann nur als Basis.
     */
    fun resetBaselineOnce() {
        if (prefs.getBoolean(KEY_BASELINE_V2, false)) return
        prefs.edit().remove(KEY_LAST_TOTAL).putBoolean(KEY_BASELINE_V2, true).apply()
    }

    /**
     * Schreibt eine Zeile ins Mess-Protokoll (für die Diagnose in den Einstellungen).
     * Messungen der App bei offenem Screen werden nur alle 10 Minuten eingetragen.
     */
    @Synchronized
    fun appendReadLog(source: String, text: String, nowMillis: Long = System.currentTimeMillis()) {
        val log = prefs.getString(KEY_READ_LOG, "") ?: ""
        if (!StepReadLog.shouldLog(log, source, nowMillis)) return
        prefs.edit().putString(KEY_READ_LOG, StepReadLog.append(log, nowMillis, source, text)).apply()
    }

    fun getReadLog(): List<StepReadEntry> =
        StepReadLog.entries(prefs.getString(KEY_READ_LOG, "") ?: "")

    /** Schritte im Hintergrund zählen (Dienst mit kleiner Benachrichtigung). Standardmäßig an. */
    fun isBackgroundCountingEnabled(): Boolean = prefs.getBoolean(KEY_BACKGROUND, true)

    fun setBackgroundCountingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND, enabled).apply()
    }

    fun getStepGoal(): Int = prefs.getInt(KEY_GOAL, DEFAULT_GOAL)

    fun setStepGoal(goal: Int) {
        prefs.edit().putInt(KEY_GOAL, goal).apply()
    }

    companion object {
        const val DEFAULT_GOAL = 10_000
        private const val KEY_LAST_TOTAL = "last_sensor_total"
        private const val KEY_GOAL = "step_goal"
        private const val KEY_BASELINE_V2 = "baseline_v2"
        private const val KEY_READ_LOG = "read_log"
        private const val KEY_BACKGROUND = "background_counting"
    }
}
