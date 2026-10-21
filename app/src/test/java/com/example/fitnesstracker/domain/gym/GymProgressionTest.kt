package com.example.fitnesstracker.domain.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GymProgressionTest {

    private fun session(id: Long, start: Long, ended: Long? = start + 1000) =
        WorkoutSessionData(id, dateEpochDay = id, startedAt = start, endedAt = ended)

    private fun set(sessionId: Long, exerciseId: Long, reps: Int, order: Long, kg: Double = 50.0) =
        WorkoutSetData(sessionId, exerciseId, "Ü", kg, reps, order)

    @Test fun lastPerformancePicksNewestFinishedWithExercise() {
        val sessions = listOf(session(1, 100), session(2, 200), session(3, 300, null), session(4, 400))
        val sets = listOf(
            set(1, 7, 10, 1), set(2, 7, 9, 2), set(2, 7, 8, 3),
            set(3, 7, 5, 4), set(4, 9, 5, 5)
        )
        val r = GymCalculator.lastPerformance(7, currentSessionId = 4, sessions, sets)
        assertNotNull(r)
        assertEquals(2L, r!!.sessionId)
        assertEquals(listOf(9, 8), r.sets.map { it.reps })
    }

    @Test fun lastPerformanceIgnoresCurrentAndUnfinished() {
        val sessions = listOf(session(1, 100, null), session(2, 200))
        val sets = listOf(set(1, 7, 10, 1), set(2, 7, 10, 2))
        assertNull(GymCalculator.lastPerformance(7, 2, sessions, sets))
    }

    @Test fun reduceWhenAnySetBelowMin() {
        val last = listOf(set(1, 7, 12, 1), set(1, 7, 7, 2), set(1, 7, 12, 3))
        assertEquals(ProgressionHint.REDUCE, GymCalculator.progressionHint(3, 8, 12, last))
    }

    @Test fun increaseWhenAllSetsAtMax() {
        val last = listOf(set(1, 7, 12, 1), set(1, 7, 12, 2), set(1, 7, 13, 3))
        assertEquals(ProgressionHint.INCREASE, GymCalculator.progressionHint(3, 8, 12, last))
    }

    @Test fun noIncreaseWithFewerSetsThanPlanned() {
        val last = listOf(set(1, 7, 12, 1), set(1, 7, 12, 2))
        assertEquals(ProgressionHint.NONE, GymCalculator.progressionHint(3, 8, 12, last))
    }

    @Test fun noneInBetweenOrEmpty() {
        val last = listOf(set(1, 7, 10, 1), set(1, 7, 12, 2), set(1, 7, 9, 3))
        assertEquals(ProgressionHint.NONE, GymCalculator.progressionHint(3, 8, 12, last))
        assertEquals(ProgressionHint.NONE, GymCalculator.progressionHint(3, 8, 12, emptyList()))
    }

    @Test fun equalMinAndMaxAtTargetGivesIncrease() {
        val last = listOf(set(1, 7, 5, 1), set(1, 7, 5, 2))
        assertEquals(ProgressionHint.INCREASE, GymCalculator.progressionHint(2, 5, 5, last))
    }
}
