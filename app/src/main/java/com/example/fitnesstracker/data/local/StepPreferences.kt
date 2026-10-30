package com.example.fitnesstracker.data.local

import android.content.Context

/** Kleine Einstellungen für die Schritte: Tagesziel und ob im Hintergrund gezählt wird. */
class StepPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("step_prefs", Context.MODE_PRIVATE)

    fun getStepGoal(): Int = prefs.getInt(KEY_GOAL, DEFAULT_GOAL)

    fun setStepGoal(goal: Int) {
        prefs.edit().putInt(KEY_GOAL, goal).apply()
    }

    /** Schritte im Hintergrund zählen (Dienst mit kleiner Benachrichtigung). Standardmäßig an. */
    fun isBackgroundCountingEnabled(): Boolean = prefs.getBoolean(KEY_BACKGROUND, true)

    fun setBackgroundCountingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND, enabled).apply()
    }

    companion object {
        const val DEFAULT_GOAL = 10_000
        private const val KEY_GOAL = "step_goal"
        private const val KEY_BACKGROUND = "background_counting"
    }
}
