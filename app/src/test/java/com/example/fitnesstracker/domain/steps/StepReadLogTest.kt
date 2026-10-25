package com.example.fitnesstracker.domain.steps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepReadLogTest {

    @Test fun appendAndReadBack() {
        var log = ""
        log = StepReadLog.append(log, 1000, "Hintergrund", "Rohwert 100 · +10 Schritte")
        log = StepReadLog.append(log, 2000, "Nacht", "keine Berechtigung")
        val entries = StepReadLog.entries(log)

        assertEquals(2, entries.size)
        assertEquals(StepReadEntry(1000, "Hintergrund", "Rohwert 100 · +10 Schritte"), entries[0])
        assertEquals("Nacht", entries[1].source)
    }

    @Test fun keepsOnlyTheLastThirty() {
        var log = ""
        for (i in 1..40) log = StepReadLog.append(log, i.toLong(), "Hintergrund", "Eintrag $i")
        val entries = StepReadLog.entries(log)

        assertEquals(30, entries.size)
        assertEquals(11L, entries.first().atMillis)
        assertEquals(40L, entries.last().atMillis)
    }

    @Test fun separatorsAndLineBreaksInTextAreCleaned() {
        val log = StepReadLog.append("", 5, "App", "a|b\nc")
        val entry = StepReadLog.entries(log).single()
        assertEquals("a b c", entry.text)
    }

    @Test fun brokenLinesAreSkipped() {
        val log = "kaputt\n1000|Nacht|ok\nabc|x|y\n2000|nur zwei"
        val entries = StepReadLog.entries(log)
        assertEquals(1, entries.size)
        assertEquals("ok", entries[0].text)
    }

    @Test fun appMeasurementsAreThrottled() {
        val log = StepReadLog.append("", 1_000, "App", "x")
        val tenMinutes = StepReadLog.APP_LOG_INTERVAL_MILLIS

        assertFalse(StepReadLog.shouldLog(log, "App", 1_000 + tenMinutes - 1))
        assertTrue(StepReadLog.shouldLog(log, "App", 1_000 + tenMinutes))
    }

    @Test fun otherSourcesAreAlwaysLogged() {
        val log = StepReadLog.append("", 1_000, "App", "x")
        assertTrue(StepReadLog.shouldLog(log, "Hintergrund", 1_001))
        assertTrue(StepReadLog.shouldLog(log, "Nacht", 1_001))
        assertTrue(StepReadLog.shouldLog("", "App", 1_001))
    }

    @Test fun serviceMeasurementsAreThrottledPerSource() {
        val log = StepReadLog.append("", 1_000, "Dienst", "x")
        val tenMinutes = StepReadLog.APP_LOG_INTERVAL_MILLIS

        assertFalse(StepReadLog.shouldLog(log, "Dienst", 1_000 + tenMinutes - 1))
        assertTrue(StepReadLog.shouldLog(log, "Dienst", 1_000 + tenMinutes))
        // Eine App-Zeile wird von einer Dienst-Zeile nicht blockiert
        assertTrue(StepReadLog.shouldLog(log, "App", 1_001))
    }
}
