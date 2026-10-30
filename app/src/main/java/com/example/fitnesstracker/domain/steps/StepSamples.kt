package com.example.fitnesstracker.domain.steps

import com.example.fitnesstracker.data.local.StepSample
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Ein gelesener Wert des Schrittzählers.
 * [timeMillis] = wann die Schritte gegangen wurden (Uhrzeit), [total] = Rohwert seit dem Gerätestart,
 * [bootMillis] = Uhrzeit des Gerätestarts. Daran erkennt man einen Neustart, bei dem der Zähler bei 0 beginnt.
 */
data class SampleData(val timeMillis: Long, val total: Long, val bootMillis: Long)

fun StepSample.toData(): SampleData = SampleData(timeMillis, total, bootMillis)

/**
 * Berechnet die Schritte pro Tag aus einzelnen Messwerten des Schrittzählers.
 *
 * Die Schritte zwischen zwei Messwerten verteilen sich nach Zeit auf die Tage, die dazwischen liegen.
 * So landet nichts auf einem falschen Tag, wenn lange nicht gemessen wurde: Sechs Tage ohne Messung
 * ergeben keinen Tag mit 70.000 Schritten, sondern die Schritte verteilen sich auf diese Tage.
 */
object StepSamples {

    /** Ändert sich der Wert, wird höchstens alle 30 Sekunden gespeichert. */
    const val MIN_INTERVAL_CHANGED_MILLIS = 30_000L

    /** Bleibt der Wert gleich, ist ein neuer Messpunkt erst nach 15 Minuten sinnvoll (er schließt "keine Schritte"). */
    const val MIN_INTERVAL_SAME_MILLIS = 15 * 60_000L

    /** Weicht der Zeitpunkt des Gerätestarts um mehr als das ab, gab es einen Neustart. */
    const val BOOT_CHANGE_MILLIS = 30 * 60_000L

    /** Ist der Zähler gesunken, genügt schon eine kleine Abweichung als Hinweis auf einen Neustart. */
    const val BOOT_NOISE_MILLIS = 2 * 60_000L

    /** Ältere Zeitstempel gelten nicht (zum Beispiel falsche Zeitbasis des Sensors): dann zählt "jetzt". */
    const val MAX_EVENT_AGE_MILLIS = 3L * 24 * 60 * 60 * 1000

    /** So lange werden Messwerte aufgehoben, danach stehen nur noch die Tageswerte. */
    const val RETENTION_MILLIS = 7L * 24 * 60 * 60 * 1000

    /**
     * Zeitpunkt, an dem die Schritte eines Sensor-Ereignisses gegangen wurden. Der Sensor stempelt in
     * Nanosekunden seit dem Gerätestart ([eventTimestampNanos], Vergleich mit [elapsedRealtimeNanos]).
     * Ist der Zeitstempel unbrauchbar, gilt "jetzt".
     */
    fun eventTime(eventTimestampNanos: Long, elapsedRealtimeNanos: Long, nowMillis: Long): Long {
        val ageMillis = (elapsedRealtimeNanos - eventTimestampNanos) / 1_000_000L
        return if (ageMillis in 0..MAX_EVENT_AGE_MILLIS) nowMillis - ageMillis else nowMillis
    }

    /** Uhrzeit des Gerätestarts. */
    fun bootMillis(elapsedRealtimeNanos: Long, nowMillis: Long): Long =
        nowMillis - elapsedRealtimeNanos / 1_000_000L

    /** Neustart: Der Zähler beginnt wieder bei 0. */
    fun isReboot(previous: SampleData, current: SampleData): Boolean {
        val bootDiff = abs(current.bootMillis - previous.bootMillis)
        return bootDiff > BOOT_CHANGE_MILLIS ||
                (current.total < previous.total && bootDiff > BOOT_NOISE_MILLIS)
    }

    /**
     * Ob ein neuer Messwert gespeichert wird. Der erste und jeder nach einem Neustart immer,
     * sonst bei neuem Wert höchstens alle 30 Sekunden und bei gleichem Wert höchstens alle 15 Minuten.
     */
    fun shouldStore(last: SampleData?, new: SampleData): Boolean {
        if (last == null || isReboot(last, new)) return true

        val elapsed = new.timeMillis - last.timeMillis
        return if (new.total != last.total) {
            elapsed >= MIN_INTERVAL_CHANGED_MILLIS || elapsed < 0
        } else {
            elapsed >= MIN_INTERVAL_SAME_MILLIS
        }
    }

    /**
     * Schritte pro Tag (Epoch-Tag), aus allen Messwerten. Ein einzelner Messwert ergibt nichts,
     * er ist nur der Ausgangspunkt. Nach einem Neustart zählen die Schritte seit dem Start.
     */
    fun dailySteps(samples: List<SampleData>, zone: ZoneId): Map<Long, Double> {
        val sorted = samples.sortedBy { it.timeMillis }
        val result = linkedMapOf<Long, Double>()

        for (i in 1 until sorted.size) {
            val a = sorted[i - 1]
            val b = sorted[i]

            if (isReboot(a, b)) {
                // Seit dem Start (oder seit dem letzten Messwert, falls der später war)
                val from = min(max(b.bootMillis, a.timeMillis), b.timeMillis)
                distribute(b.total.toDouble(), from, b.timeMillis, zone, result)
            } else {
                // Ein kleiner Rückgang bei gleichem Start ist Rauschen der Zeitstempel: keine Schritte
                val delta = max(b.total - a.total, 0L)
                distribute(delta.toDouble(), a.timeMillis, b.timeMillis, zone, result)
            }
        }
        return result
    }

    /** Verteilt [amount] Schritte nach Zeit auf die Tage zwischen [from] und [to]. */
    private fun distribute(
        amount: Double,
        from: Long,
        to: Long,
        zone: ZoneId,
        into: MutableMap<Long, Double>
    ) {
        if (to <= from) {
            val day = dayOf(to, zone)
            into[day] = (into[day] ?: 0.0) + amount
            return
        }

        val total = (to - from).toDouble()
        var cursor = from
        while (cursor < to) {
            val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val segmentEnd = min(dayEnd, to)

            val share = amount * (segmentEnd - cursor) / total
            val day = date.toEpochDay()
            into[day] = (into[day] ?: 0.0) + share

            cursor = segmentEnd
        }
    }

    private fun dayOf(millis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()
}
