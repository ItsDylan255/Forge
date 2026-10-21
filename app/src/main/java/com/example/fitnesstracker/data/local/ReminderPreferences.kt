package com.example.fitnesstracker.data.local

import android.content.Context
import com.example.fitnesstracker.domain.reminder.ReminderCalculator

/** Einstellungen der Gewicht-Erinnerung: an/aus und Uhrzeit. */
class ReminderPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getHour(): Int = prefs.getInt(KEY_HOUR, ReminderCalculator.DEFAULT_HOUR)

    fun getMinute(): Int = prefs.getInt(KEY_MINUTE, ReminderCalculator.DEFAULT_MINUTE)

    fun setTime(hour: Int, minute: Int) {
        prefs.edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply()
    }

    private companion object {
        const val KEY_ENABLED = "weight_reminder_enabled"
        const val KEY_HOUR = "weight_reminder_hour"
        const val KEY_MINUTE = "weight_reminder_minute"
    }
}
