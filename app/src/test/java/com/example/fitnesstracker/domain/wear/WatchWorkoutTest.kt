package com.example.fitnesstracker.domain.wear

import com.example.fitnesstracker.domain.gym.ExerciseKind
import com.example.fitnesstracker.domain.gym.SetSide
import com.example.fitnesstracker.domain.gym.WorkoutSessionData
import com.example.fitnesstracker.domain.gym.WorkoutSetData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchWorkoutTest {

    private val names = mapOf(1L to "Bankdrücken", 2L to "Rudern", 3L to "Curls")
    private val kinds = mapOf(1L to ExerciseKind.REPS, 2L to ExerciseKind.REPS, 3L to ExerciseKind.REPS)

    private val plan = listOf(
        WatchPlanItem(1, plannedSets = 2, repsMin = 8, repsMax = 10, restSeconds = 90),
        WatchPlanItem(2, plannedSets = 2, repsMin = 10, repsMax = 12, restSeconds = 60)
    )

    private val earlier = WorkoutSessionData(1, 10, startedAt = 1_000, endedAt = 2_000)
    private val current = WorkoutSessionData(2, 20, startedAt = 10_000, endedAt = null)

    private fun set(session: Long, exercise: Long, kg: Double, reps: Int, order: Long, side: SetSide = SetSide.BOTH) =
        WorkoutSetData(session, exercise, names.getValue(exercise), kg, reps, order, side)

    private fun state(sets: List<WorkoutSetData>, plan: List<WatchPlanItem> = this.plan, rest: WatchRest? = null) =
        WatchWorkout.state(2, "Push", 10_000, plan, names, kinds, listOf(earlier, current), sets, rest)

    @Test fun startsWithFirstExerciseAndLastPerformance() {
        val s = state(listOf(set(1, 1, 80.0, 8, 1), set(1, 1, 80.0, 7, 2)))
        assertEquals("Bankdrücken", s.exerciseName)
        assertEquals("Satz 1 von 2", s.setText)
        assertEquals("Ziel: 8–10 Wdh.", s.targetText)
        assertEquals("Letztes Mal: 80 kg × 8", s.lastText)
        assertEquals("Übung 1 von 2", s.progressText)
        assertNull(s.recordText)
    }

    @Test fun lastPerformanceMatchesSetNumber() {
        val s = state(listOf(set(1, 1, 80.0, 8, 1), set(1, 1, 80.0, 7, 2), set(2, 1, 80.0, 8, 3)))
        assertEquals("Satz 2 von 2", s.setText)
        assertEquals("Letztes Mal: 80 kg × 7", s.lastText)
    }

    @Test fun movesOnWhenExerciseIsDone() {
        val s = state(listOf(set(2, 1, 80.0, 8, 1), set(2, 1, 80.0, 8, 2)))
        assertEquals("Rudern", s.exerciseName)
        assertEquals("Satz 1 von 2", s.setText)
        assertEquals("Übung 2 von 2", s.progressText)
    }

    @Test fun staysOnExerciseTheUserWorksOn() {
        // Rudern vorgezogen: die Uhr bleibt bei Rudern, solange dort Sätze offen sind
        val s = state(listOf(set(2, 2, 60.0, 10, 1)))
        assertEquals("Rudern", s.exerciseName)
        assertEquals("Satz 2 von 2", s.setText)
    }

    @Test fun allDoneKeepsLastExercise() {
        val sets = listOf(
            set(2, 1, 80.0, 8, 1), set(2, 1, 80.0, 8, 2),
            set(2, 2, 60.0, 10, 3), set(2, 2, 60.0, 10, 4)
        )
        val s = state(sets)
        assertEquals("Rudern", s.exerciseName)
        assertEquals("Alle Sätze erledigt", s.setText)
    }

    @Test fun unplannedExerciseStaysWhileWorkingOnIt() {
        val s = state(listOf(set(2, 3, 12.0, 10, 1)))
        assertEquals("Curls", s.exerciseName)
        assertEquals("Satz 2", s.setText)
        assertNull(s.targetText)
        assertEquals("Übung 3 von 3", s.progressText)
    }

    @Test fun unilateralShowsSide() {
        val uni = listOf(WatchPlanItem(1, plannedSets = 2, repsMin = 8, repsMax = 10, restSeconds = 90, unilateral = true))
        assertEquals("Satz 1 von 2 · rechts", state(emptyList(), uni).setText)
        assertEquals("Satz 1 von 2 · links", state(listOf(set(2, 1, 20.0, 8, 1, SetSide.RIGHT)), uni).setText)
    }

    @Test fun supersetIsMarked() {
        val superset = listOf(plan[0].copy(supersetWithNext = true), plan[1])
        val s = state(listOf(set(2, 1, 80.0, 8, 1)), superset)
        assertTrue(s.superset)
        assertEquals("Rudern", s.exerciseName)
        assertEquals("Übung 1 von 1", s.progressText)
        assertFalse(state(emptyList()).superset)
    }

    @Test fun newRecordIsShown() {
        val s = state(listOf(set(1, 1, 80.0, 8, 1), set(2, 1, 85.0, 8, 2)))
        assertEquals("Neuer Rekord! 1RM 107,5 kg", s.recordText)
    }

    @Test fun emptyWorkoutHasNoExercise() {
        val s = state(emptyList(), emptyList(), WatchRest("Bankdrücken", 5_000, 90_000))
        assertNull(s.exerciseName)
        assertEquals(5_000L, s.rest?.endsAtMillis)
    }
}
