package com.example.fitnesstracker.domain.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanProgressTest {

    // 2026-10-26 (Epoch-Tag 20752) ist ein Montag
    private val monday = 20752L

    private fun s(routine: Long?, day: Long, plan: Long? = 1, name: String = "R$routine") =
        PlanSessionRef(plan, routine, name, day, startedAt = day * 1000 + (routine ?: 0))

    private val routines = setOf(10L, 11L)

    @Test fun noSessionsGivesNull() =
        assertNull(PlanProgressCalculator.progress(1, routines, listOf(s(10, monday, plan = 2)), monday))

    @Test fun weeksRoundsAndThisWeek() {
        val sessions = listOf(
            s(10, monday - 14), s(11, monday - 13),   // Durchgang 1 (vor zwei Wochen)
            s(10, monday - 7), s(10, monday - 6),     // nur eine Routine
            s(11, monday + 1),                        // Durchgang 2 fertig
            s(10, monday + 2, name = "Leg Day 1")
        )
        val p = PlanProgressCalculator.progress(1, routines, sessions, monday + 3)!!
        assertEquals(3, p.week)
        assertEquals(2, p.rounds)
        assertEquals(2, p.doneThisWeek)
        assertEquals(2, p.routineCount)
        assertEquals("Leg Day 1", p.lastRoutineName)
        assertEquals(monday + 2, p.lastEpochDay)
    }

    @Test fun deletedRoutinesCountOnlyForWeekAndLast() {
        val sessions = listOf(s(99, monday, name = "Alt"), s(10, monday + 1))
        val p = PlanProgressCalculator.progress(1, routines, sessions, monday + 1)!!
        assertEquals(0, p.rounds)
        assertEquals(1, p.doneThisWeek)
        assertEquals(1, p.week)
    }

    @Test fun daysAgo() {
        assertEquals("heute", PlanProgressCalculator.daysAgoText(5, 5))
        assertEquals("gestern", PlanProgressCalculator.daysAgoText(4, 5))
        assertEquals("vor 3 Tagen", PlanProgressCalculator.daysAgoText(2, 5))
    }
}
