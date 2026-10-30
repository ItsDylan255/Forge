package com.example.fitnesstracker.domain.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OneRepMaxTest {

    private fun session(id: Long, day: Long, ended: Boolean = true) =
        WorkoutSessionData(id, day, startedAt = day * 1000, endedAt = if (ended) day * 1000 + 1 else null)

    private fun set(session: Long, kg: Double, reps: Int, order: Long, kind: ExerciseKind = ExerciseKind.REPS) =
        WorkoutSetData(session, 1, "Bankdrücken", kg, reps, order, kind = kind)

    @Test fun epleyEstimate() {
        assertEquals(100.0, OneRepMax.estimate(100.0, 1)!!, 0.001)
        assertEquals(80.0 * (1 + 8 / 30.0), OneRepMax.estimate(80.0, 8)!!, 0.001)
    }

    @Test fun invalidSetsHaveNoEstimate() {
        assertNull(OneRepMax.estimate(0.0, 5))
        assertNull(OneRepMax.estimate(80.0, 0))
        assertNull(OneRepMax.estimate(80.0, 11))
    }

    @Test fun bestUsesHighestEstimateAndIgnoresOtherKinds() {
        val sessions = listOf(session(1, 10), session(2, 20))
        val sets = listOf(
            set(1, 100.0, 1, 1),            // 100
            set(2, 80.0, 8, 2),             // 101,3
            set(2, 60.0, 15, 3),            // zu viele Wdh.
            set(2, 200.0, 5, 4, ExerciseKind.TIME)
        )
        val best = OneRepMax.best(1, sessions, sets)!!
        assertEquals(80.0, best.weightKg, 0.0)
        assertEquals(8, best.reps)
        assertEquals(20L, best.dateEpochDay)
    }

    @Test fun progressPerDay() {
        val sessions = listOf(session(1, 10), session(2, 20))
        val sets = listOf(set(1, 100.0, 1, 1), set(1, 90.0, 1, 2), set(2, 105.0, 1, 3))
        assertEquals(
            listOf(ProgressPoint(10, 100.0), ProgressPoint(20, 105.0)),
            OneRepMax.progress(1, sessions, sets)
        )
    }

    @Test fun newRecordOnlyAgainstEarlierSessions() {
        val sessions = listOf(session(1, 10), session(2, 20, ended = false))
        val sets = listOf(set(1, 100.0, 1, 1), set(2, 102.5, 1, 2))
        val record = OneRepMax.newRecord(1, 2, sessions, sets)!!
        assertEquals(102.5, record.first, 0.001)
        assertEquals(100.0, record.second, 0.001)
    }

    @Test fun noRecordWithoutEarlierValueOrWhenNotBetter() {
        assertNull(OneRepMax.newRecord(1, 1, listOf(session(1, 10)), listOf(set(1, 100.0, 1, 1))))
        val sessions = listOf(session(1, 10), session(2, 20, ended = false))
        assertNull(OneRepMax.newRecord(1, 2, sessions, listOf(set(1, 100.0, 1, 1), set(2, 100.0, 1, 2))))
    }

    @Test fun tableRoundsToHalfKilos() {
        val rows = OneRepMax.table(101.3)
        assertEquals(OneRepMaxRow(100, 101.5, 1), rows.first())
        assertEquals(OneRepMaxRow(80, 81.0, 8), rows.first { it.percent == 80 })
        assertEquals(OneRepMaxRow(75, 76.0, 10), rows.first { it.percent == 75 })
    }
}
