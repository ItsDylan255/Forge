package com.example.fitnesstracker.domain.photos

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoScheduleTest {

    private fun day(text: String) = LocalDate.parse(text).toEpochDay()

    @Test
    fun `off has no due date`() {
        assertNull(PhotoReminderCalculator.nextDue(PhotoSchedule.Off, day("2026-10-01"), day("2026-10-01")))
    }

    @Test
    fun `every n days counts from the last photo`() {
        val due = PhotoReminderCalculator.nextDue(PhotoSchedule.EveryDays(5), day("2026-10-20"), day("2026-10-01"))
        assertEquals(day("2026-10-25"), due)
    }

    @Test
    fun `every n days without photo is due on the day it was set up`() {
        val due = PhotoReminderCalculator.nextDue(PhotoSchedule.EveryDays(5), null, day("2026-10-31"))
        assertEquals(day("2026-10-31"), due)
    }

    @Test
    fun `old photo before setting up does not make it overdue right away`() {
        val due = PhotoReminderCalculator.nextDue(PhotoSchedule.EveryDays(5), day("2026-08-01"), day("2026-10-31"))
        assertEquals(day("2026-10-31"), due)
    }

    @Test
    fun `weekly is the next weekday after the last photo`() {
        // 2026-10-25 ist ein Sonntag
        val due = PhotoReminderCalculator.nextDue(PhotoSchedule.Weekly(DayOfWeek.SUNDAY), day("2026-10-25"), day("2026-10-01"))
        assertEquals(day("2026-11-01"), due)
    }

    @Test
    fun `weekly without photo can be due on the setup day`() {
        // 2026-11-01 ist ein Sonntag
        val due = PhotoReminderCalculator.nextDue(PhotoSchedule.Weekly(DayOfWeek.SUNDAY), null, day("2026-11-01"))
        assertEquals(day("2026-11-01"), due)
    }

    @Test
    fun `monthly is the first weekday of the next month once this one is done`() {
        // Erster Sonntag im November 2026 ist der 1., im Dezember der 6.
        val schedule = PhotoSchedule.MonthlyFirst(DayOfWeek.SUNDAY)
        assertEquals(day("2026-11-01"), PhotoReminderCalculator.nextDue(schedule, day("2026-10-04"), day("2026-10-01")))
        assertEquals(day("2026-12-06"), PhotoReminderCalculator.nextDue(schedule, day("2026-11-01"), day("2026-10-01")))
    }

    @Test
    fun `due today, overdue and reminders`() {
        val schedule = PhotoSchedule.EveryDays(5)
        val onTime = PhotoReminderCalculator.status(schedule, day("2026-10-20"), day("2026-10-01"), day("2026-10-25"))!!
        assertTrue(onTime.isToday)
        assertFalse(onTime.isOverdue)
        assertTrue(PhotoReminderCalculator.shouldRemind(onTime))

        val late = PhotoReminderCalculator.status(schedule, day("2026-10-20"), day("2026-10-01"), day("2026-10-28"))!!
        assertTrue(late.isOverdue)
        assertEquals(3L, late.daysOverdue)
        assertTrue(PhotoReminderCalculator.shouldRemind(late))

        val early = PhotoReminderCalculator.status(schedule, day("2026-10-20"), day("2026-10-01"), day("2026-10-22"))!!
        assertFalse(early.isOverdue)
        assertFalse(PhotoReminderCalculator.shouldRemind(early))
    }

    @Test
    fun `snapshot uses today's weight and recent averages`() {
        val today = day("2026-10-31")
        val stats = PhotoStatsCalculator.snapshot(
            today = today,
            weights = mapOf(today - 3 to 84.0, today to 83.2),
            goalWeightKg = 80.0,
            workoutDays = listOf(today - 40, today - 10, today - 2, today),
            steps = mapOf(today to 4_000, today - 1 to 8_000, today - 20 to 30_000),
            kcalByDay = mapOf(today - 1 to 2_000.0, today - 2 to 2_400.0),
            kcalTarget = 2_300
        )
        assertEquals(83.2, stats.weightKg!!, 0.001)
        assertEquals(4, stats.workoutsTotal)
        assertEquals(3, stats.workouts30Days)
        assertEquals(6_000, stats.avgSteps7Days)
        assertEquals(2_200, stats.avgKcal7Days)
        assertEquals(2_300, stats.kcalTarget)
    }

    @Test
    fun `snapshot falls back to the latest earlier weight`() {
        val today = day("2026-10-31")
        val stats = PhotoStatsCalculator.snapshot(today, mapOf(today - 3 to 84.0), null, emptyList(), emptyMap(), emptyMap(), null)
        assertEquals(84.0, stats.weightKg!!, 0.001)
        assertNull(stats.avgSteps7Days)
        assertNull(stats.avgKcal7Days)
    }
}
