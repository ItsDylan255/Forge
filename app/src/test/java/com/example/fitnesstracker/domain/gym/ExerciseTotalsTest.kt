package com.example.fitnesstracker.domain.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseTotalsTest {

    private fun set(
        session: Long, exercise: Long, kg: Double, reps: Int, order: Long,
        kind: ExerciseKind = ExerciseKind.REPS, body: Double? = null
    ) = WorkoutSetData(session, exercise, "Ü$exercise", kg, reps, order, SetSide.BOTH, kind, body)

    private fun session(id: Long, day: Long) =
        WorkoutSessionData(id, day, id * 1000, id * 1000 + 1000)

    private val sessions = listOf(session(1, 10), session(2, 20))

    @Test fun repsVolumeSumsAllSetsOfATraining() {
        val sets = listOf(
            set(1, 1, 50.0, 10, 1), set(1, 1, 50.0, 8, 2),
            set(2, 1, 55.0, 10, 3), set(2, 1, 55.0, 10, 4), set(2, 1, 55.0, 8, 5)
        )
        val totals = GymCalculator.exerciseTotals(1, sessions, sets)

        assertEquals(TotalMetric.VOLUME, totals.metric)
        assertEquals(listOf(10L, 20L), totals.points.map { it.dateEpochDay })
        assertEquals(900.0, totals.points[0].total, 0.001)
        assertEquals(1540.0, totals.points[1].total, 0.001)
        assertEquals(listOf(2, 3), totals.points.map { it.setCount })
    }

    @Test fun moreSetsWithTheSameWeightGiveMoreTotal() {
        val sets = listOf(
            set(1, 1, 50.0, 10, 1),
            set(2, 1, 50.0, 10, 2), set(2, 1, 50.0, 10, 3)
        )
        val totals = GymCalculator.exerciseTotals(1, sessions, sets)
        assertTrue(totals.points[1].total > totals.points[0].total)
    }

    @Test fun bodyweightExercisesCountRepetitions() {
        val sets = listOf(
            set(1, 1, 0.0, 15, 1), set(1, 1, 0.0, 12, 2),
            set(2, 1, 0.0, 20, 3)
        )
        val totals = GymCalculator.exerciseTotals(1, sessions, sets)

        assertEquals(TotalMetric.REPS, totals.metric)
        assertEquals(27.0, totals.points[0].total, 0.001)
        assertEquals(20.0, totals.points[1].total, 0.001)
    }

    @Test fun timeExercisesSumSeconds() {
        val sets = listOf(
            set(1, 1, 0.0, 30, 1, ExerciseKind.TIME), set(1, 1, 0.0, 40, 2, ExerciseKind.TIME),
            set(2, 1, 0.0, 60, 3, ExerciseKind.TIME), set(2, 1, 0.0, 60, 4, ExerciseKind.TIME)
        )
        val totals = GymCalculator.exerciseTotals(1, sessions, sets)

        assertEquals(TotalMetric.TIME, totals.metric)
        assertEquals(70.0, totals.points[0].total, 0.001)
        assertEquals(120.0, totals.points[1].total, 0.001)
    }

    @Test fun assistedUsesBodyWeightMinusAssistance() {
        val sets = listOf(
            set(1, 1, 20.0, 8, 1, ExerciseKind.ASSISTED, 90.0), set(1, 1, 20.0, 8, 2, ExerciseKind.ASSISTED, 90.0),
            set(2, 1, 10.0, 8, 3, ExerciseKind.ASSISTED, 90.0)
        )
        val totals = GymCalculator.exerciseTotals(1, sessions, sets)

        assertEquals(TotalMetric.VOLUME, totals.metric)
        assertEquals(1120.0, totals.points[0].total, 0.001)
        assertEquals(640.0, totals.points[1].total, 0.001)
    }

    @Test fun assistedWithoutBodyWeightFallsBackToRepetitions() {
        val sets = listOf(
            set(1, 1, 20.0, 8, 1, ExerciseKind.ASSISTED, null),
            set(2, 1, 20.0, 10, 2, ExerciseKind.ASSISTED, null)
        )
        val totals = GymCalculator.exerciseTotals(1, sessions, sets)

        assertEquals(TotalMetric.REPS, totals.metric)
        assertEquals(listOf(8.0, 10.0), totals.points.map { it.total })
    }

    @Test fun onlyTheLatestKindCounts() {
        val sets = listOf(
            set(1, 1, 100.0, 5, 1, ExerciseKind.REPS),
            set(2, 1, 0.0, 45, 2, ExerciseKind.TIME)
        )
        val totals = GymCalculator.exerciseTotals(1, sessions, sets)

        assertEquals(TotalMetric.TIME, totals.metric)
        assertEquals(1, totals.points.size)
        assertEquals(45.0, totals.points[0].total, 0.001)
    }

    @Test fun trainingsOnTheSameDayAreMerged() {
        val sameDay = listOf(session(1, 10), session(2, 10))
        val sets = listOf(set(1, 1, 50.0, 10, 1), set(2, 1, 50.0, 10, 2))
        val totals = GymCalculator.exerciseTotals(1, sameDay, sets)

        assertEquals(1, totals.points.size)
        assertEquals(1000.0, totals.points[0].total, 0.001)
    }

    @Test fun emptyAndUnknownSessionsGiveNoPoints() {
        assertTrue(GymCalculator.exerciseTotals(1, sessions, emptyList()).points.isEmpty())
        val unknown = listOf(set(99, 1, 50.0, 10, 1))
        assertTrue(GymCalculator.exerciseTotals(1, sessions, unknown).points.isEmpty())
    }

    // ---------- Anzeige ----------

    @Test fun totalTexts() {
        assertEquals("1.240 kg", GymFormat.total(TotalMetric.VOLUME, 1240.0))
        assertEquals("24 Wdh.", GymFormat.total(TotalMetric.REPS, 24.0))
        assertEquals("2:30 Min", GymFormat.total(TotalMetric.TIME, 150.0))
        assertEquals("45 Sek.", GymFormat.total(TotalMetric.TIME, 45.0))
    }

    @Test fun deltaTexts() {
        assertEquals("+120 kg · +10 %", GymFormat.totalDelta(TotalMetric.VOLUME, 120.0, 1200.0))
        assertEquals("−80 kg · −8 %", GymFormat.totalDelta(TotalMetric.VOLUME, -80.0, 1000.0))
        assertEquals("±0 kg · ±0 %", GymFormat.totalDelta(TotalMetric.VOLUME, 0.0, 500.0))
        assertEquals("+5 Wdh.", GymFormat.totalDelta(TotalMetric.REPS, 5.0, 0.0))
        assertEquals("+30 Sek. · +25 %", GymFormat.totalDelta(TotalMetric.TIME, 30.0, 120.0))
    }

    @Test fun labelsNameTheMeasure() {
        assertEquals("GESAMTVOLUMEN PRO TRAINING", GymFormat.totalLabel(TotalMetric.VOLUME))
        assertEquals("GESAMTZEIT PRO TRAINING", GymFormat.totalLabel(TotalMetric.TIME))
        assertTrue(GymFormat.totalLabel(TotalMetric.REPS).contains("WIEDERHOLUNGEN"))
    }
}
