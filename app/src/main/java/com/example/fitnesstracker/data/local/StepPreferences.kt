package com.example.fitnesstracker.data.local

import android.content.Context

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

    fun getStepGoal(): Int = prefs.getInt(KEY_GOAL, DEFAULT_GOAL)

    fun setStepGoal(goal: Int) {
        prefs.edit().putInt(KEY_GOAL, goal).apply()
    }

    companion object {
        const val DEFAULT_GOAL = 10_000
        private const val KEY_LAST_TOTAL = "last_sensor_total"
        private const val KEY_GOAL = "step_goal"
    }
}
