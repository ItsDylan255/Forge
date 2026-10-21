package com.example.fitnesstracker.domain.reminder

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderCalculatorTest {

    private val berlin = ZoneId.of("Europe/Berlin")
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int, s: Int = 0) =
        ZonedDateTime.of(y, m, d, h, min, s, 0, berlin)

    private val hour = 3_600_000L

    @Test fun laterTodayCountsToday() =
        assertEquals(1 * hour, ReminderCalculator.delayMillis(at(2026, 10, 16, 7, 0), 8, 0))

    @Test fun alreadyPassedCountsTomorrow() =
        assertEquals(23 * hour, ReminderCalculator.delayMillis(at(2026, 10, 16, 9, 0), 8, 0))

    @Test fun exactlyNowCountsTomorrow() =
        assertEquals(24 * hour, ReminderCalculator.delayMillis(at(2026, 10, 16, 8, 0), 8, 0))

    @Test fun justAfterTriggerCountsTomorrow() {
        val delay = ReminderCalculator.delayMillis(at(2026, 10, 16, 8, 0, 5), 8, 0)
        assertEquals(24 * hour - 5_000L, delay)
    }

    @Test fun minutesAreRespected() =
        assertEquals(90 * 60_000L, ReminderCalculator.delayMillis(at(2026, 10, 16, 6, 30), 8, 0))

    @Test fun summerToWinterTimeIsHandled() {
        // 25.10.2026: Umstellung auf Winterzeit, der Tag hat 25 Stunden.
        // 24.10. 12:00 (MESZ) bis 25.10. 08:00 (MEZ) sind real 21 Stunden.
        assertEquals(21 * hour, ReminderCalculator.delayMillis(at(2026, 10, 24, 12, 0), 8, 0))
    }

    @Test fun remindsOnlyWithoutEntry() {
        assertTrue(ReminderCalculator.shouldRemind(false))
        assertFalse(ReminderCalculator.shouldRemind(true))
    }

    @Test fun hourStepWraps() {
        assertEquals(9, ReminderCalculator.stepHour(8, 1))
        assertEquals(0, ReminderCalculator.stepHour(23, 1))
        assertEquals(23, ReminderCalculator.stepHour(0, -1))
    }

    @Test fun minuteStepWrapsWithoutChangingHour() {
        assertEquals(5, ReminderCalculator.stepMinute(0, 5))
        assertEquals(0, ReminderCalculator.stepMinute(55, 5))
        assertEquals(55, ReminderCalculator.stepMinute(0, -5))
    }

    @Test fun formatsTwoDigits() {
        assertEquals("08:05", ReminderCalculator.format(8, 5))
        assertEquals("23:00", ReminderCalculator.format(23, 0))
    }
}
