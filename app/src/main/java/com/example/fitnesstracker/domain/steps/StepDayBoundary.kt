package com.example.fitnesstracker.domain.steps

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Tageswechsel der Schritte: wann um Mitternacht gelesen wird und welchem Tag die Schritte gehören. */
object StepDayBoundary {

    /** Ältere Zeitstempel gelten nicht (zum Beispiel falsche Zeitbasis des Sensors): dann zählt "jetzt". */
    const val MAX_EVENT_AGE_MILLIS = 3L * 24 * 60 * 60 * 1000

    /**
     * Tag, an dem die Schritte eines Sensor-Ereignisses gegangen wurden. Der Sensor stempelt seine Werte
     * in Nanosekunden seit dem Gerätestart ([eventTimestampNanos], Vergleich mit [elapsedRealtimeNanos]).
     * Liefert Android einen Wert erst Stunden später (Handy lag in der Tasche), zeigt der Zeitstempel
     * trotzdem den richtigen Tag. Ist der Zeitstempel unbrauchbar, gilt "jetzt".
     */
    fun dayOfEvent(
        eventTimestampNanos: Long,
        elapsedRealtimeNanos: Long,
        nowMillis: Long,
        zone: ZoneId
    ): Long {
        val ageMillis = (elapsedRealtimeNanos - eventTimestampNanos) / 1_000_000L
        val eventMillis = if (ageMillis in 0..MAX_EVENT_AGE_MILLIS) nowMillis - ageMillis else nowMillis
        return Instant.ofEpochMilli(eventMillis).atZone(zone).toLocalDate().toEpochDay()
    }

    /** Vor dieser Uhrzeit gehören neu gelesene Schritte noch zum Vortag (Nachtmessung). */
    const val CUTOFF_HOUR = 6

    /** Nächste Mitternacht plus 10 Sekunden, damit die Uhr sicher im neuen Tag liegt. */
    fun nextMidnightMillis(now: ZonedDateTime): Long =
        now.toLocalDate()
            .plusDays(1)
            .atStartOfDay(now.zone)
            .plusSeconds(10)
            .toInstant()
            .toEpochMilli()

    /**
     * Tag, dem die seit dem letzten Lesen gegangenen Schritte bei der Nachtmessung gutgeschrieben werden:
     * zwischen 0 und 6 Uhr der Vortag, danach heute.
     */
    fun dayToCredit(now: ZonedDateTime): Long {
        val today = now.toLocalDate().toEpochDay()
        return if (now.hour < CUTOFF_HOUR) today - 1 else today
    }
}
