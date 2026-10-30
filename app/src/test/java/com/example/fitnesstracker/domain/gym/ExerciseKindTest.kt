package com.example.fitnesstracker.domain.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseKindTest {

    private fun set(
        session: Long, exercise: Long, kg: Double, reps: Int, order: Long,
        kind: ExerciseKind = ExerciseKind.REPS, body: Double? = null
    ) = WorkoutSetData(session, exercise, "Ü$exercise", kg, reps, order, SetSide.BOTH, kind, body)

    private fun session(id: Long, day: Long) =
        WorkoutSessionData(id, day, id * 1000, id * 1000 + 3_600_000)

    @Test fun kindCodes() {
        assertEquals(ExerciseKind.TIME, ExerciseKind.fromCode(1))
        assertEquals(ExerciseKind.ASSISTED, ExerciseKind.fromCode(2))
        assertEquals(ExerciseKind.REPS, ExerciseKind.fromCode(0))
        assertEquals(ExerciseKind.REPS, ExerciseKind.fromCode(77))
    }

    // ---------- Volumen ----------

    @Test fun volumePerKind() {
        assertEquals(800.0, GymCalculator.setVolume(set(1, 1, 80.0, 10, 1)), 0.001)
        assertEquals(0.0, GymCalculator.setVolume(set(1, 1, 10.0, 60, 1, ExerciseKind.TIME)), 0.001)
        assertEquals(560.0, GymCalculator.setVolume(set(1, 1, 20.0, 8, 1, ExerciseKind.ASSISTED, 90.0)), 0.001)
    }

    @Test fun assistedVolumeNeedsBodyWeightAndIsNeverNegative() {
        assertEquals(0.0, GymCalculator.setVolume(set(1, 1, 20.0, 8, 1, ExerciseKind.ASSISTED, null)), 0.001)
        assertEquals(0.0, GymCalculator.setVolume(set(1, 1, 120.0, 8, 1, ExerciseKind.ASSISTED, 90.0)), 0.001)
        assertEquals(720.0, GymCalculator.setVolume(set(1, 1, 0.0, 8, 1, ExerciseKind.ASSISTED, 90.0)), 0.001)
    }

    @Test fun summarizeUsesKindVolume() {
        val sets = listOf(
            set(1, 1, 80.0, 10, 1),
            set(1, 2, 0.0, 60, 2, ExerciseKind.TIME),
            set(1, 3, 20.0, 8, 3, ExerciseKind.ASSISTED, 90.0)
        )
        val summary = GymCalculator.summarize(session(1, 10), sets)
        assertEquals(800.0 + 0.0 + 560.0, summary.totalVolumeKg, 0.001)
        assertEquals(3, summary.setCount)
    }

    // ---------- Bestleistungen ----------

    @Test fun timeBestIsLongestTime() {
        val sessions = listOf(session(1, 10), session(2, 20))
        val sets = listOf(
            set(1, 1, 0.0, 45, 1, ExerciseKind.TIME),
            set(2, 1, 0.0, 70, 2, ExerciseKind.TIME),
            set(2, 1, 5.0, 70, 3, ExerciseKind.TIME)
        )
        val best = GymCalculator.personalBests(sessions, sets).single()
        assertEquals(ExerciseKind.TIME, best.kind)
        assertEquals(70, best.repsAtBest)
        assertEquals(5.0, best.bestWeightKg, 0.001)
    }

    @Test fun assistedBestIsLowestAssistanceThenMostReps() {
        val sessions = listOf(session(1, 10), session(2, 20))
        val sets = listOf(
            set(1, 1, 30.0, 12, 1, ExerciseKind.ASSISTED, 90.0),
            set(2, 1, 20.0, 6, 2, ExerciseKind.ASSISTED, 90.0),
            set(2, 1, 20.0, 8, 3, ExerciseKind.ASSISTED, 90.0)
        )
        val best = GymCalculator.personalBests(sessions, sets).single()
        assertEquals(20.0, best.bestWeightKg, 0.001)
        assertEquals(8, best.repsAtBest)
        assertEquals(20L, best.dateEpochDay)
    }

    @Test fun bestUsesOnlyLatestKind() {
        val sessions = listOf(session(1, 10), session(2, 20))
        val sets = listOf(
            set(1, 1, 100.0, 5, 1, ExerciseKind.REPS),
            set(2, 1, 0.0, 30, 2, ExerciseKind.TIME)
        )
        val best = GymCalculator.personalBests(sessions, sets).single()
        assertEquals(ExerciseKind.TIME, best.kind)
        assertEquals(30, best.repsAtBest)
    }

    @Test fun progressPerKind() {
        val sessions = listOf(session(1, 10), session(2, 20))
        val time = listOf(
            set(1, 1, 0.0, 40, 1, ExerciseKind.TIME), set(1, 1, 0.0, 50, 2, ExerciseKind.TIME),
            set(2, 1, 0.0, 60, 3, ExerciseKind.TIME)
        )
        assertEquals(listOf(50.0, 60.0), GymCalculator.exerciseProgress(1, sessions, time).map { it.maxWeightKg })

        val assisted = listOf(
            set(1, 2, 30.0, 8, 4, ExerciseKind.ASSISTED, 90.0), set(1, 2, 25.0, 8, 5, ExerciseKind.ASSISTED, 90.0),
            set(2, 2, 20.0, 8, 6, ExerciseKind.ASSISTED, 90.0)
        )
        assertEquals(listOf(25.0, 20.0), GymCalculator.exerciseProgress(2, sessions, assisted).map { it.maxWeightKg })
    }

    @Test fun progressWithoutSetsIsEmpty() {
        assertTrue(GymCalculator.exerciseProgress(1, listOf(session(1, 10)), emptyList()).isEmpty())
    }

    // ---------- Neue Bestleistungen ----------

    @Test fun newBestTimeMustBeLonger() {
        val sessions = listOf(session(1, 10), session(2, 20))
        val longer = listOf(set(1, 1, 0.0, 40, 1, ExerciseKind.TIME), set(2, 1, 0.0, 55, 2, ExerciseKind.TIME))
        val result = GymCalculator.newBestsIn(2, sessions, longer).single()
        assertEquals(ExerciseKind.TIME, result.kind)
        assertEquals(55.0, result.weightKg, 0.001)
        assertEquals(40.0, result.previousKg, 0.001)

        val same = listOf(set(1, 1, 0.0, 40, 1, ExerciseKind.TIME), set(2, 1, 0.0, 40, 2, ExerciseKind.TIME))
        assertTrue(GymCalculator.newBestsIn(2, sessions, same).isEmpty())
    }

    @Test fun newBestAssistedMeansLessAssistance() {
        val sessions = listOf(session(1, 10), session(2, 20))
        val less = listOf(
            set(1, 1, 30.0, 8, 1, ExerciseKind.ASSISTED, 90.0),
            set(2, 1, 25.0, 8, 2, ExerciseKind.ASSISTED, 90.0)
        )
        val result = GymCalculator.newBestsIn(2, sessions, less).single()
        assertEquals(25.0, result.weightKg, 0.001)
        assertEquals(30.0, result.previousKg, 0.001)

        val more = listOf(
            set(1, 1, 30.0, 8, 1, ExerciseKind.ASSISTED, 90.0),
            set(2, 1, 35.0, 8, 2, ExerciseKind.ASSISTED, 90.0)
        )
        assertTrue(GymCalculator.newBestsIn(2, sessions, more).isEmpty())
    }

    // ---------- Eingaben ----------

    @Test fun secondsParsing() {
        assertEquals(GymParseResult.Valid(45), GymInputParser.parseSeconds("45"))
        assertEquals(GymParseResult.Valid(45), GymInputParser.parseSeconds("45 s"))
        assertEquals(GymParseResult.Valid(45), GymInputParser.parseSeconds("45 Sek"))
        assertEquals(GymParseResult.Valid(45), GymInputParser.parseSeconds("45 Sek."))
        assertEquals(GymParseResult.Valid(90), GymInputParser.parseSeconds("1:30"))
        assertEquals(GymParseResult.Valid(3600), GymInputParser.parseSeconds("60:00"))
    }

    @Test fun secondsRejectsInvalid() {
        assertTrue(GymInputParser.parseSeconds("") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseSeconds("0") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseSeconds("3601") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseSeconds("1:75") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseSeconds("abc") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseSeconds("4,5") is GymParseResult.Invalid)
    }

    @Test fun secondsBounds() {
        assertEquals(GymParseResult.Valid(30..60), GymInputParser.parseSecondsBounds("30", "1:00"))
        assertEquals(GymParseResult.Valid(45..45), GymInputParser.parseSecondsBounds("45", "45"))
        assertTrue(GymInputParser.parseSecondsBounds("60", "30") is GymParseResult.Invalid)
        val minError = GymInputParser.parseSecondsBounds("x", "30") as GymParseResult.Invalid
        assertTrue(minError.message.contains("Min. Sek."))
        val maxError = GymInputParser.parseSecondsBounds("30", "x") as GymParseResult.Invalid
        assertTrue(maxError.message.contains("Max. Sek."))
    }

    @Test fun optionalWeightEmptyIsZero() {
        assertEquals(GymParseResult.Valid(0.0), GymInputParser.parseOptionalWeight(""))
        assertEquals(GymParseResult.Valid(0.0), GymInputParser.parseOptionalWeight("   "))
        assertEquals(GymParseResult.Valid(12.5), GymInputParser.parseOptionalWeight("12,5 kg"))
        assertTrue(GymInputParser.parseOptionalWeight("abc") is GymParseResult.Invalid)
    }

    // ---------- Anzeige ----------

    @Test fun secondsLabels() {
        assertEquals("45 Sek.", GymFormat.secondsLabel(45))
        assertEquals("1:00 Min", GymFormat.secondsLabel(60))
        assertEquals("1:30 Min", GymFormat.secondsLabel(90))
    }

    @Test fun planAndSetTexts() {
        assertEquals("3 × 12–15", GymFormat.plan(ExerciseKind.ASSISTED, 3, 12, 15))
        assertEquals("3 × 30–60 Sek.", GymFormat.plan(ExerciseKind.TIME, 3, 30, 60))
        assertEquals("3 × 45 Sek.", GymFormat.plan(ExerciseKind.TIME, 3, 45, 45))
        assertEquals("80 kg × 10", GymFormat.setText(ExerciseKind.REPS, 80.0, 10))
        assertEquals("45 Sek.", GymFormat.setText(ExerciseKind.TIME, 0.0, 45))
        assertEquals("45 Sek. · +10 kg", GymFormat.setText(ExerciseKind.TIME, 10.0, 45))
        assertEquals("20 kg Unterstützung × 8", GymFormat.setText(ExerciseKind.ASSISTED, 20.0, 8))
        assertEquals("Ohne Unterstützung × 8", GymFormat.setText(ExerciseKind.ASSISTED, 0.0, 8))
    }

    @Test fun metricTexts() {
        assertEquals("80 kg", GymFormat.metric(ExerciseKind.REPS, 80.0))
        assertEquals("1:10 Min", GymFormat.metric(ExerciseKind.TIME, 70.0))
        assertEquals("20 kg Unterstützung", GymFormat.metric(ExerciseKind.ASSISTED, 20.0))
    }

    @Test fun progressionTextsPerKind() {
        assertNull(GymFormat.progressionText(ExerciseKind.REPS, ProgressionHint.NONE, 8, 12))
        assertTrue(GymFormat.progressionText(ExerciseKind.REPS, ProgressionHint.REDUCE, 8, 12)!!.endsWith("Gewicht reduzieren."))
        assertTrue(GymFormat.progressionText(ExerciseKind.ASSISTED, ProgressionHint.REDUCE, 8, 12)!!.endsWith("Unterstützung erhöhen."))
        assertTrue(GymFormat.progressionText(ExerciseKind.ASSISTED, ProgressionHint.INCREASE, 8, 12)!!.endsWith("Unterstützung verringern möglich."))
        assertTrue(GymFormat.progressionText(ExerciseKind.TIME, ProgressionHint.REDUCE, 30, 60)!!.contains("unter 30 Sek."))
        assertTrue(GymFormat.progressionText(ExerciseKind.TIME, ProgressionHint.INCREASE, 30, 60)!!.contains("bei 60 Sek."))
    }
}
