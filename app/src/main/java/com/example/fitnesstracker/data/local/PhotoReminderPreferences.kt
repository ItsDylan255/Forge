package com.example.fitnesstracker.data.local

import android.content.Context
import com.example.fitnesstracker.domain.photos.PhotoSchedule
import java.time.DayOfWeek
import java.time.LocalDate

/** Rhythmus und Uhrzeit der Erinnerung an Fortschrittsfotos. */
class PhotoReminderPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("photo_reminder_prefs", Context.MODE_PRIVATE)

    fun getSchedule(): PhotoSchedule {
        val day = DayOfWeek.of(prefs.getInt(KEY_WEEKDAY, DayOfWeek.SUNDAY.value).coerceIn(1, 7))
        return when (prefs.getString(KEY_MODE, MODE_OFF)) {
            MODE_DAYS -> PhotoSchedule.EveryDays(
                prefs.getInt(KEY_DAYS, DEFAULT_DAYS).coerceIn(PhotoSchedule.MIN_DAYS, PhotoSchedule.MAX_DAYS)
            )
            MODE_WEEKLY -> PhotoSchedule.Weekly(day)
            MODE_MONTHLY -> PhotoSchedule.MonthlyFirst(day)
            else -> PhotoSchedule.Off
        }
    }

    /** Speichert den Rhythmus. Ab heute wird gezählt, frühere Termine gelten nicht als verpasst. */
    fun setSchedule(schedule: PhotoSchedule) {
        val editor = prefs.edit().putLong(KEY_ANCHOR, LocalDate.now().toEpochDay())
        when (schedule) {
            PhotoSchedule.Off -> editor.putString(KEY_MODE, MODE_OFF)
            is PhotoSchedule.EveryDays -> editor.putString(KEY_MODE, MODE_DAYS).putInt(KEY_DAYS, schedule.days)
            is PhotoSchedule.Weekly -> editor.putString(KEY_MODE, MODE_WEEKLY).putInt(KEY_WEEKDAY, schedule.day.value)
            is PhotoSchedule.MonthlyFirst -> editor.putString(KEY_MODE, MODE_MONTHLY).putInt(KEY_WEEKDAY, schedule.day.value)
        }
        editor.apply()
    }

    fun getAnchorDay(): Long = prefs.getLong(KEY_ANCHOR, LocalDate.now().toEpochDay())

    fun getHour(): Int = prefs.getInt(KEY_HOUR, DEFAULT_HOUR)

    fun getMinute(): Int = prefs.getInt(KEY_MINUTE, 0)

    fun setTime(hour: Int, minute: Int) {
        prefs.edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply()
    }

    companion object {
        const val DEFAULT_DAYS = 7
        const val DEFAULT_HOUR = 9

        private const val KEY_MODE = "mode"
        private const val KEY_DAYS = "days"
        private const val KEY_WEEKDAY = "weekday"
        private const val KEY_ANCHOR = "anchor_day"
        private const val KEY_HOUR = "hour"
        private const val KEY_MINUTE = "minute"

        private const val MODE_OFF = "off"
        private const val MODE_DAYS = "days"
        private const val MODE_WEEKLY = "weekly"
        private const val MODE_MONTHLY = "monthly"
    }
}
