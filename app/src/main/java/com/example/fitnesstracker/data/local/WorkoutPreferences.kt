package com.example.fitnesstracker.data.local

import android.content.Context

/** Einstellungen für das Training: Signalton und Vibration am Pausenende (beides standardmäßig an). */
class WorkoutPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)

    fun isSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND, true)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    fun isVibrationEnabled(): Boolean = prefs.getBoolean(KEY_VIBRATION, true)

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
    }

    private companion object {
        const val KEY_SOUND = "rest_sound"
        const val KEY_VIBRATION = "rest_vibration"
    }
}
