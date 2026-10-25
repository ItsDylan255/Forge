package com.example.fitnesstracker.domain.steps

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class StepDayBoundaryTest {

    private val berlin = ZoneId.of("Europe/Berlin")
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int, s: Int = 0) =
        ZonedDateTime.of(y, m, d, h, min, s, 0, berlin)

    private fun millis(z: ZonedDateTime) = z.toInstant().toEpochMilli()

    @Test fun nextMidnightIsTenSecondsAfterStartOfNextDay() {
        assertEquals(
            millis(at(2026, 10, 23, 0, 0, 10)),
            StepDayBoundary.nextMidnightMillis(at(2026, 10, 22, 15, 0))
        )
    }

    @Test fun justAfterMidnightStillTargetsTheNextOne() {
        assertEquals(
            millis(at(2026, 10, 23, 0, 0, 10)),
            StepDayBoundary.nextMidnightMillis(at(2026, 10, 22, 0, 0, 5))
        )
    }

    @Test fun winterTimeSwitchIsHandled() {
        // 25.10.2026 hat 25 Stunden. Mitternacht davor liegt noch in der Sommerzeit.
        assertEquals(
            millis(at(2026, 10, 25, 0, 0, 10)),
            StepDayBoundary.nextMidnightMillis(at(2026, 10, 24, 12, 0))
        )
        assertEquals(
            millis(at(2026, 10, 26, 0, 0, 10)),
            StepDayBoundary.nextMidnightMillis(at(2026, 10, 25, 12, 0))
        )
    }

    @Test fun nightReadingBelongsToThePreviousDay() {
        val yesterday = LocalDate.of(2026, 10, 22).toEpochDay()
        assertEquals(yesterday, StepDayBoundary.dayToCredit(at(2026, 10, 23, 0, 0, 10)))
        assertEquals(yesterday, StepDayBoundary.dayToCredit(at(2026, 10, 23, 5, 59)))
    }

    @Test fun readingFromSixOClockBelongsToToday() {
        val today = LocalDate.of(2026, 10, 23).toEpochDay()
        assertEquals(today, StepDayBoundary.dayToCredit(at(2026, 10, 23, 6, 0)))
        assertEquals(today, StepDayBoundary.dayToCredit(at(2026, 10, 23, 23, 59)))
    }

    // ---------- Tag eines Sensor-Ereignisses ----------

    private val nowBerlin = at(2026, 10, 23, 7, 0)
    private val nowMillis = millis(nowBerlin)
    private val elapsedNow = 500_000_000_000_000L
    private val today = LocalDate.of(2026, 10, 23).toEpochDay()

    /** Ereignis, das [seconds] Sekunden vor "jetzt" gestempelt wurde. */
    private fun eventDay(secondsAgo: Long) =
        StepDayBoundary.dayOfEvent(elapsedNow - secondsAgo * 1_000_000_000L, elapsedNow, nowMillis, berlin)

    @Test fun eventFromLastNightBelongsToYesterday() {
        // 8,5 Stunden vor 7:00 Uhr: gestern 22:30
        assertEquals(today - 1, eventDay(8 * 3600L + 1800L))
    }

    @Test fun eventFromThisMorningBelongsToToday() {
        assertEquals(today, eventDay(3600L))
        assertEquals(today, eventDay(0L))
    }

    @Test fun midnightIsTheBoundary() {
        // Genau 7 Stunden vorher ist heute 0:00, eine Sekunde mehr ist gestern 23:59:59
        assertEquals(today, eventDay(7 * 3600L))
        assertEquals(today - 1, eventDay(7 * 3600L + 1L))
    }

    @Test fun eventInTheFutureCountsAsNow() {
        assertEquals(today, StepDayBoundary.dayOfEvent(elapsedNow + 5_000_000_000L, elapsedNow, nowMillis, berlin))
    }

    @Test fun veryOldTimestampIsIgnored() {
        // 4 Tage alt: wahrscheinlich falsche Zeitbasis, es zählt "jetzt"
        assertEquals(today, eventDay(4 * 24 * 3600L))
    }

    @Test fun threeDaysOldIsStillUsed() {
        assertEquals(today - 3, eventDay(3 * 24 * 3600L - 1L))
    }
}
