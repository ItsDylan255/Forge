package com.example.fitnesstracker.domain.reminder

import com.example.fitnesstracker.i18n.L10n
import java.time.Duration
import java.time.ZonedDateTime

/** Rechenlogik für die tägliche Gewicht-Erinnerung. */
object ReminderCalculator {

    const val DEFAULT_HOUR = 8
    const val DEFAULT_MINUTE = 0

    /**
     * Wartezeit in Millisekunden bis zur nächsten Erinnerung um [hour]:[minute].
     * Ist die Uhrzeit heute schon erreicht oder vorbei, zählt der nächste Tag.
     * Mit Zeitzone gerechnet, damit auch die Umstellung auf Sommer-/Winterzeit stimmt.
     */
    fun delayMillis(now: ZonedDateTime, hour: Int, minute: Int): Long {
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }

    /** Erinnert wird nur, wenn für heute noch kein Gewicht eingetragen ist. */
    fun shouldRemind(hasEntryToday: Boolean): Boolean = !hasEntryToday

    /** Stunde um [delta] verschieben, über Mitternacht hinweg (23 + 1 = 0). */
    fun stepHour(hour: Int, delta: Int): Int = Math.floorMod(hour + delta, 24)

    /** Minute um [delta] verschieben, ohne die Stunde zu ändern (55 + 5 = 0). */
    fun stepMinute(minute: Int, delta: Int): Int = Math.floorMod(minute + delta, 60)

    /** "08:05" */
    fun format(hour: Int, minute: Int): String = String.format(L10n.locale, "%02d:%02d", hour, minute)
}
