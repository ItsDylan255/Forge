package com.example.fitnesstracker.domain.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GymDomainTest {

    private fun set(session: Long, exercise: Long, kg: Double, reps: Int, order: Long, name: String = "Ü$exercise") =
        WorkoutSetData(session, exercise, name, kg, reps, order)

    private fun session(id: Long, day: Long, started: Long = id * 1000, ended: Long? = id * 1000 + 3_600_000) =
        WorkoutSessionData(id, day, started, ended)

    // ---------- Eingaben ----------

    @Test
    fun weight_acceptsCommaDotAndSuffix() {
        assertEquals(GymParseResult.Valid(82.5), GymInputParser.parseWeight("82,5"))
        assertEquals(GymParseResult.Valid(82.5), GymInputParser.parseWeight("82.5 kg"))
        assertEquals(GymParseResult.Valid(0.0), GymInputParser.parseWeight("0"))
    }

    @Test
    fun weight_rejectsInvalid() {
        assertTrue(GymInputParser.parseWeight("") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseWeight("abc") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseWeight("-5") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseWeight("1001") is GymParseResult.Invalid)
    }

    @Test
    fun reps_mustBePositiveInteger() {
        assertEquals(GymParseResult.Valid(10), GymInputParser.parseReps("10"))
        assertTrue(GymInputParser.parseReps("0") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseReps("8,5") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseReps("1001") is GymParseResult.Invalid)
    }

    @Test
    fun repsBounds_minAndMaxAreIndependentFields() {
        assertEquals(GymParseResult.Valid(8..12), GymInputParser.parseRepsBounds("8", "12"))
        assertEquals(GymParseResult.Valid(10..10), GymInputParser.parseRepsBounds("10", "10"))
        assertEquals(GymParseResult.Valid(1..1000), GymInputParser.parseRepsBounds(" 1 ", "1000"))
    }

    @Test
    fun repsBounds_rejectsMinGreaterThanMax() {
        assertTrue(GymInputParser.parseRepsBounds("12", "8") is GymParseResult.Invalid)
    }

    @Test
    fun repsBounds_rejectsEmptyOrInvalidValues() {
        assertTrue(GymInputParser.parseRepsBounds("", "12") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseRepsBounds("8", "") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseRepsBounds("0", "12") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseRepsBounds("8", "1001") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseRepsBounds("8,5", "12") is GymParseResult.Invalid)
        assertTrue(GymInputParser.parseRepsBounds("8-12", "12") is GymParseResult.Invalid)
    }

    @Test
    fun repsBounds_errorNamesTheBrokenField() {
        val minError = GymInputParser.parseRepsBounds("x", "12") as GymParseResult.Invalid
        assertTrue(minError.message.contains("Min. Wdh."))

        val maxError = GymInputParser.parseRepsBounds("8", "x") as GymParseResult.Invalid
        assertTrue(maxError.message.contains("Max. Wdh."))
    }

    @Test
    fun plannedSets_andName() {
        assertEquals(GymParseResult.Valid(4), GymInputParser.parsePlannedSets("4"))
        assertTrue(GymInputParser.parsePlannedSets("21") is GymParseResult.Invalid)
        assertEquals(GymParseResult.Valid("Push Day"), GymInputParser.parseName("  Push Day "))
        assertTrue(GymInputParser.parseName("   ") is GymParseResult.Invalid)
    }

    // ---------- Zusammenfassung ----------

    @Test
    fun summarize_volumeSetsExercisesAndDuration() {
        val s = session(1, 100)
        val sets = listOf(
            set(1, 1, 80.0, 10, 1),
            set(1, 1, 80.0, 8, 2),
            set(1, 2, 20.0, 12, 3),
            set(2, 1, 999.0, 99, 4) // andere Einheit, zählt nicht
        )
        val summary = GymCalculator.summarize(s, sets)
        assertEquals(3, summary.setCount)
        assertEquals(2, summary.exerciseCount)
        assertEquals(80.0 * 10 + 80.0 * 8 + 20.0 * 12, summary.totalVolumeKg, 0.0001)
        assertEquals(3_600_000L, summary.durationMillis)
    }

    @Test
    fun summarize_runningSessionHasNoDuration() {
        assertNull(GymCalculator.summarize(session(1, 100, ended = null), emptyList()).durationMillis)
    }

    // ---------- Volumen ----------

    @Test
    fun weekStart_isMonday() {
        // 5.1.1970 war ein Montag (Epoch-Tag 4)
        assertEquals(4L, GymCalculator.weekStart(4L))
        assertEquals(4L, GymCalculator.weekStart(10L)) // Sonntag derselben Woche
        assertEquals(11L, GymCalculator.weekStart(11L)) // nächster Montag
        assertEquals(-3L, GymCalculator.weekStart(0L))  // Donnerstag 1.1.1970
    }

    @Test
    fun weeklyVolumes_hasExactBucketsAndFillsEmptyWeeks() {
        val today = 11L + 2 // Mittwoch der Woche ab Epoch-Tag 11
        val sessions = listOf(session(1, 12), session(2, 4))
        val sets = listOf(set(1, 1, 100.0, 10, 1), set(2, 1, 50.0, 10, 2))

        val weeks = GymCalculator.weeklyVolumes(sessions, sets, 3, today)

        assertEquals(3, weeks.size)
        assertEquals(listOf(-3L, 4L, 11L), weeks.map { it.startEpochDay })
        assertEquals(0.0, weeks[0].volumeKg, 0.0001)
        assertEquals(500.0, weeks[1].volumeKg, 0.0001)
        assertEquals(1000.0, weeks[2].volumeKg, 0.0001)
    }

    @Test
    fun sessionVolumes_lastNChronological() {
        val sessions = listOf(session(1, 1), session(2, 2), session(3, 3))
        val sets = listOf(set(1, 1, 10.0, 1, 1), set(2, 1, 20.0, 1, 2), set(3, 1, 30.0, 1, 3))
        val result = GymCalculator.sessionVolumes(sessions, sets, 2)
        assertEquals(listOf(2L, 3L), result.map { it.sessionId })
        assertEquals(listOf(20.0, 30.0), result.map { it.volumeKg })
    }

    // ---------- Bestleistungen ----------

    @Test
    fun personalBests_heaviestSetThenMostReps() {
        val sessions = listOf(session(1, 10), session(2, 20))
        val sets = listOf(
            set(1, 1, 100.0, 5, 1),
            set(2, 1, 100.0, 8, 2),
            set(2, 1, 90.0, 12, 3),
            set(1, 2, 40.0, 10, 4)
        )
        val bests = GymCalculator.personalBests(sessions, sets).associateBy { it.exerciseId }
        assertEquals(100.0, bests.getValue(1).bestWeightKg, 0.0001)
        assertEquals(8, bests.getValue(1).repsAtBest)
        assertEquals(20L, bests.getValue(1).dateEpochDay)
        assertEquals(40.0, bests.getValue(2).bestWeightKg, 0.0001)
    }

    @Test
    fun personalBests_ignoreSetsOfUnknownSessions() {
        val bests = GymCalculator.personalBests(listOf(session(1, 1)), listOf(set(99, 1, 500.0, 1, 1)))
        assertTrue(bests.isEmpty())
    }

    @Test
    fun exerciseProgress_maxPerDayAscending() {
        val sessions = listOf(session(1, 30), session(2, 10), session(3, 20))
        val sets = listOf(
            set(1, 1, 80.0, 5, 1),
            set(2, 1, 60.0, 5, 2),
            set(2, 1, 70.0, 5, 3),
            set(3, 1, 65.0, 5, 4),
            set(3, 2, 10.0, 5, 5)
        )
        val points = GymCalculator.exerciseProgress(1, sessions, sets)
        assertEquals(listOf(10L, 20L, 30L), points.map { it.dateEpochDay })
        assertEquals(listOf(70.0, 65.0, 80.0), points.map { it.maxWeightKg })
    }

    @Test
    fun newBests_onlyWhenEarlierHistoryExistsAndWeightIsHigher() {
        val sessions = listOf(session(1, 10), session(2, 20))
        val sets = listOf(
            set(1, 1, 80.0, 5, 1),
            set(1, 2, 50.0, 5, 2),
            set(2, 1, 85.0, 5, 3),   // neue Bestleistung
            set(2, 2, 50.0, 5, 4),   // gleich -> keine
            set(2, 3, 30.0, 5, 5)    // erstes Mal -> keine
        )
        val result = GymCalculator.newBestsIn(2, sessions, sets)
        assertEquals(1, result.size)
        assertEquals(1L, result[0].exerciseId)
        assertEquals(85.0, result[0].weightKg, 0.0001)
        assertEquals(80.0, result[0].previousKg, 0.0001)
    }

    @Test
    fun newBests_firstSessionHasNone() {
        val sessions = listOf(session(1, 10))
        assertTrue(GymCalculator.newBestsIn(1, sessions, listOf(set(1, 1, 80.0, 5, 1))).isEmpty())
    }

    // ---------- Format ----------

    @Test
    fun format_numbersAndSets() {
        assertEquals("80", GymFormat.number(80.0))
        assertEquals("82,5", GymFormat.number(82.5))
        assertEquals("80 kg × 10", GymFormat.setText(80.0, 10))
        assertEquals("Körpergewicht × 15", GymFormat.setText(0.0, 15))
        assertEquals("4 × 8–12", GymFormat.plan(4, 8, 12))
        assertEquals("3 × 10", GymFormat.plan(3, 10, 10))
    }

    @Test
    fun format_timeAndVolume() {
        assertEquals("05:32", GymFormat.timer(5 * 60_000L + 32_000L))
        assertEquals("1:05:32", GymFormat.timer(3_932_000L))
        assertEquals("45 Min", GymFormat.duration(45 * 60_000L))
        assertEquals("1 Std 05 Min", GymFormat.duration(65 * 60_000L))
        assertEquals("< 1 Min", GymFormat.duration(20_000L))
        assertEquals("12.450 kg", GymFormat.volume(12_450.0))
        assertEquals("12,5k", GymFormat.compact(12_450.0))
        assertEquals("850", GymFormat.compact(850.0))
    }

    // ---------- Pause ----------

    @Test
    fun rest_defaultIsNinetySeconds() {
        assertEquals(90, GymDefaults.REST_SECONDS)
        assertEquals(15, GymDefaults.REST_STEP_SECONDS)
    }

    @Test
    fun rest_labelShowsMinutesAndSeconds() {
        assertEquals("1:30", GymFormat.restLabel(90))
        assertEquals("2:00", GymFormat.restLabel(120))
        assertEquals("0:15", GymFormat.restLabel(15))
        assertEquals("10:00", GymFormat.restLabel(600))
        assertEquals("Aus", GymFormat.restLabel(0))
    }
}
