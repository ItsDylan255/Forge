package com.example.fitnesstracker.domain.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutNotificationPlannerTest {

    private val started = 1_000_000L

    @Test fun runningRestCountsDownWithProgress() {
        val now = 2_000_000L
        val plan = WorkoutNotificationPlanner.plan("Push", started, now + 60_000, 90_000, "Bankdrücken", now)
        assertEquals("Push", plan.title)
        assertEquals("Pause · Bankdrücken", plan.text)
        assertTrue(plan.countDown)
        assertEquals(now + 60_000, plan.chronometerBase)
        assertEquals(90, plan.progressMax)
        assertEquals(30, plan.progress)
    }

    @Test fun finishedRestShowsHintAndWorkoutClock() {
        val now = 2_000_000L
        val plan = WorkoutNotificationPlanner.plan("Push", started, now - 1, 90_000, "Bankdrücken", now)
        assertEquals("Pause vorbei · weiter mit dem nächsten Satz", plan.text)
        assertFalse(plan.countDown)
        assertEquals(started, plan.chronometerBase)
        assertEquals(0, plan.progressMax)
    }

    @Test fun noRestShowsWorkoutRunning() {
        val plan = WorkoutNotificationPlanner.plan("Push", started, 0, 0, "", 2_000_000L)
        assertEquals("Training läuft", plan.text)
        assertFalse(plan.countDown)
        assertEquals(started, plan.chronometerBase)
    }

    @Test fun blankNamesGetDefaults() {
        val now = 2_000_000L
        val running = WorkoutNotificationPlanner.plan("  ", started, now + 10_000, 30_000, "", now)
        assertEquals("Training", running.title)
        assertEquals("Pause", running.text)
    }

    @Test fun progressIsClamped() {
        val now = 2_000_000L
        val tiny = WorkoutNotificationPlanner.plan("A", started, now + 500, 0, "x", now)
        assertEquals(1, tiny.progressMax)
        assertEquals(0, tiny.progress)

        // Restzeit länger als die Gesamtdauer (z. B. Uhr verstellt): Fortschritt nie negativ
        val odd = WorkoutNotificationPlanner.plan("A", started, now + 200_000, 90_000, "x", now)
        assertEquals(0, odd.progress)
    }
}
