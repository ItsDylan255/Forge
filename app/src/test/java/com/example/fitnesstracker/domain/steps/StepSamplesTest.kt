package com.example.fitnesstracker.domain.steps

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StepSamplesTest {

    private val berlin = ZoneId.of("Europe/Berlin")

    private fun ms(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, berlin).toInstant().toEpochMilli()

    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    private val boot = ms(2026, 10, 1, 8)

    private fun sample(time: Long, total: Long, bootMillis: Long = boot) = SampleData(time, total, bootMillis)

    private fun steps(samples: List<SampleData>) = StepSamples.dailySteps(samples, berlin)

    // ---------- Tage ----------

    @Test fun stepsWithinOneDayCountForThatDay() {
        val result = steps(listOf(sample(ms(2026, 10, 22, 9), 1000), sample(ms(2026, 10, 22, 18), 1600)))
        assertEquals(600.0, result.getValue(day(2026, 10, 22)), 0.001)
    }

    @Test fun stepsAcrossMidnightAreSplitByTime() {
        // 22:00 bis 02:00: zwei Stunden vor und zwei nach Mitternacht
        val result = steps(listOf(sample(ms(2026, 10, 22, 22), 1000), sample(ms(2026, 10, 23, 2), 1400)))
        assertEquals(200.0, result.getValue(day(2026, 10, 22)), 0.001)
        assertEquals(200.0, result.getValue(day(2026, 10, 23)), 0.001)
    }

    @Test fun longGapSpreadsInsteadOfDumpingOnOneDay() {
        // Acht Tage ohne Messung, 80.000 Schritte: kein Tag bekommt alles
        val result = steps(listOf(sample(ms(2026, 10, 14, 12), 0), sample(ms(2026, 10, 22, 12), 80_000)))

        for (d in 15..21) assertEquals(10_000.0, result.getValue(day(2026, 10, d)), 0.001)
        assertEquals(5_000.0, result.getValue(day(2026, 10, 14)), 0.001)
        assertEquals(5_000.0, result.getValue(day(2026, 10, 22)), 0.001)
        assertTrue(result.values.all { it <= 10_000.0 + 0.001 })
    }

    @Test fun unchangedTotalGivesNoSteps() {
        // Handy lag die Nacht still: gleicher Wert am Abend und am Morgen
        val result = steps(listOf(sample(ms(2026, 10, 22, 22, 30), 5000), sample(ms(2026, 10, 23, 7), 5000)))
        assertEquals(0.0, result.values.sum(), 0.001)
    }

    @Test fun morningStepsStayOnTheNewDay() {
        val result = steps(
            listOf(
                sample(ms(2026, 10, 22, 22, 30), 5000),
                sample(ms(2026, 10, 23, 7), 5000),
                sample(ms(2026, 10, 23, 8), 5800)
            )
        )
        assertEquals(800.0, result.getValue(day(2026, 10, 23)), 0.001)
    }

    @Test fun totalIsConservedOverTheWinterTimeDay() {
        // 24.10. 12:00 bis 26.10. 12:00 enthält den Tag mit 25 Stunden
        val result = steps(listOf(sample(ms(2026, 10, 24, 12), 0), sample(ms(2026, 10, 26, 12), 4800)))
        assertEquals(4800.0, result.values.sum(), 0.001)
    }

    @Test fun singleSampleGivesNothing() {
        assertTrue(steps(listOf(sample(ms(2026, 10, 22, 9), 1000))).isEmpty())
        assertTrue(steps(emptyList()).isEmpty())
    }

    @Test fun unsortedInputIsSortedByTime() {
        val result = steps(listOf(sample(ms(2026, 10, 22, 18), 1600), sample(ms(2026, 10, 22, 9), 1000)))
        assertEquals(600.0, result.getValue(day(2026, 10, 22)), 0.001)
    }

    // ---------- Neustart ----------

    @Test fun rebootCountsStepsSinceStart() {
        val newBoot = ms(2026, 10, 23, 5)
        val result = steps(
            listOf(
                sample(ms(2026, 10, 22, 23), 9000),
                sample(ms(2026, 10, 23, 7), 3000, bootMillis = newBoot)
            )
        )
        // 3000 Schritte seit 5 Uhr, alle am 23.10.
        assertEquals(3000.0, result.getValue(day(2026, 10, 23)), 0.001)
        assertNull(result[day(2026, 10, 22)])
    }

    @Test fun smallDropWithSameStartIsIgnored() {
        // Zeitstempel durcheinander: später gemessen, aber kleinerer Wert. Kein Neustart, keine Schritte.
        val result = steps(listOf(sample(ms(2026, 10, 22, 9), 5000), sample(ms(2026, 10, 22, 10), 4990)))
        assertEquals(0.0, result.values.sum(), 0.001)
    }

    @Test fun rebootIsDetectedByDroppingTotalAndNewStart() {
        val a = sample(ms(2026, 10, 22, 9), 5000)
        assertTrue(StepSamples.isReboot(a, sample(ms(2026, 10, 22, 10), 300, bootMillis = ms(2026, 10, 22, 9, 30))))
        assertFalse(StepSamples.isReboot(a, sample(ms(2026, 10, 22, 10), 5200)))
        // Uhr um 5 Minuten korrigiert, Zähler läuft weiter: kein Neustart
        assertFalse(StepSamples.isReboot(a, sample(ms(2026, 10, 22, 10), 5200, bootMillis = boot + 300_000L)))
        // Boot-Zeit weit weg: Neustart, auch wenn der Zähler schon höher ist
        assertTrue(StepSamples.isReboot(a, sample(ms(2026, 10, 22, 10), 9000, bootMillis = boot + 3_600_000L)))
    }

    // ---------- Speichern ----------

    @Test fun firstAndRebootSamplesAreAlwaysStored() {
        assertTrue(StepSamples.shouldStore(null, sample(1000, 10)))
        val last = sample(ms(2026, 10, 22, 9), 5000)
        val rebooted = sample(last.timeMillis + 1000, 100, bootMillis = boot + 3_600_000L)
        assertTrue(StepSamples.shouldStore(last, rebooted))
    }

    @Test fun changedTotalIsStoredAtMostEveryThirtySeconds() {
        val last = sample(100_000, 500)
        assertFalse(StepSamples.shouldStore(last, sample(100_000 + 29_999, 520)))
        assertTrue(StepSamples.shouldStore(last, sample(100_000 + 30_000, 520)))
    }

    @Test fun unchangedTotalIsStoredOnlyAfterFifteenMinutes() {
        val last = sample(100_000, 500)
        assertFalse(StepSamples.shouldStore(last, sample(100_000 + 14 * 60_000L, 500)))
        assertTrue(StepSamples.shouldStore(last, sample(100_000 + 15 * 60_000L, 500)))
    }

    // ---------- Zeitpunkt ----------

    @Test fun eventTimeUsesTheSensorTimestamp() {
        val now = ms(2026, 10, 23, 7)
        val elapsed = 500_000_000_000_000L
        // Schritte vor 8,5 Stunden
        val eventNanos = elapsed - (8 * 3600L + 1800L) * 1_000_000_000L
        assertEquals(now - (8 * 3600L + 1800L) * 1000L, StepSamples.eventTime(eventNanos, elapsed, now))
    }

    @Test fun unusableTimestampsCountAsNow() {
        val now = ms(2026, 10, 23, 7)
        val elapsed = 500_000_000_000_000L
        assertEquals(now, StepSamples.eventTime(elapsed + 5_000_000_000L, elapsed, now))
        assertEquals(now, StepSamples.eventTime(elapsed - 4L * 24 * 3600 * 1_000_000_000L, elapsed, now))
    }

    @Test fun bootTimeIsNowMinusUptime() {
        val now = ms(2026, 10, 23, 7)
        assertEquals(now - 3_600_000L, StepSamples.bootMillis(3_600_000L * 1_000_000L, now))
    }
}
