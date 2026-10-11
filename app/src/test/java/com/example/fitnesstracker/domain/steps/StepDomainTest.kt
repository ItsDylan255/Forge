package com.example.fitnesstracker.domain.steps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StepDomainTest {

    // ---------- Delta ----------

    @Test
    fun delta_firstReadingIsOnlyBaseline() {
        assertEquals(0L, StepDeltaCalculator.delta(null, 5_000L))
    }

    @Test
    fun delta_normalIncrease() {
        assertEquals(250L, StepDeltaCalculator.delta(1_000L, 1_250L))
    }

    @Test
    fun delta_sameValueIsZero() {
        assertEquals(0L, StepDeltaCalculator.delta(1_000L, 1_000L))
    }

    @Test
    fun delta_rebootCountsNewValueFromZero() {
        assertEquals(80L, StepDeltaCalculator.delta(9_000L, 80L))
    }

    @Test
    fun delta_negativeNewValueIsIgnored() {
        assertEquals(0L, StepDeltaCalculator.delta(100L, -5L))
    }

    // ---------- Ziel ----------

    @Test
    fun goalProgress_halfway() {
        val p = StepCalculator.goalProgress(5_000, 10_000)
        assertEquals(0.5, p.barFraction, 0.0001)
        assertEquals(50, p.percent)
        assertEquals(5_000, p.remaining)
        assertFalse(p.reached)
    }

    @Test
    fun goalProgress_justBelowGoalIsNot100Percent() {
        val p = StepCalculator.goalProgress(9_999, 10_000)
        assertEquals(99, p.percent)
        assertFalse(p.reached)
    }

    @Test
    fun goalProgress_overGoalClampsBarButNotPercent() {
        val p = StepCalculator.goalProgress(12_000, 10_000)
        assertEquals(1.0, p.barFraction, 0.0001)
        assertEquals(120, p.percent)
        assertEquals(0, p.remaining)
        assertTrue(p.reached)
    }

    @Test
    fun goalProgress_zeroGoalDoesNotCrash() {
        val p = StepCalculator.goalProgress(10, 0)
        assertEquals(1, p.goal)
    }

    // ---------- Zusammenfassung ----------

    private val today = 1_000L

    @Test
    fun summary_sumsOnlyWindowIncludingToday() {
        val days = listOf(
            StepDay(today, 1_000),
            StepDay(today - 6, 2_000),   // letzter Tag im 7-Tage-Fenster
            StepDay(today - 7, 9_999)    // schon außerhalb
        )
        val s = StepCalculator.summary(days, 7, today)
        assertEquals(3_000L, s.totalSteps)
        assertEquals(2, s.daysWithData)
        assertEquals(1_500, s.averagePerDay)
    }

    @Test
    fun summary_withoutDataHasNoAverage() {
        val s = StepCalculator.summary(emptyList(), 30, today)
        assertEquals(0L, s.totalSteps)
        assertEquals(0, s.daysWithData)
        assertNull(s.averagePerDay)
    }

    @Test
    fun stepsOn_returnsZeroForMissingDay() {
        assertEquals(0, StepCalculator.stepsOn(listOf(StepDay(1, 10)), 2))
        assertEquals(10, StepCalculator.stepsOn(listOf(StepDay(1, 10)), 1))
    }

    // ---------- Diagramm ----------

    @Test
    fun chart_hasOneBarPerDayAndFillsGapsWithZero() {
        val bars = StepChartData.barsFor(
            listOf(StepDay(today, 500), StepDay(today - 2, 300)),
            StepChartRange.WEEK,
            today
        )
        assertEquals(7, bars.size)
        assertEquals(today - 6, bars.first().dateEpochDay)
        assertEquals(today, bars.last().dateEpochDay)
        assertEquals(500, bars.last().steps)
        assertEquals(0, bars[bars.size - 2].steps)
        assertEquals(300, bars[bars.size - 3].steps)
    }

    @Test
    fun chart_monthHas30Bars() {
        assertEquals(30, StepChartData.barsFor(emptyList(), StepChartRange.MONTH, today).size)
    }

    @Test
    fun chart_hasAnySteps() {
        val empty = StepChartData.barsFor(emptyList(), StepChartRange.WEEK, today)
        assertFalse(StepChartData.hasAnySteps(empty))
        val filled = StepChartData.barsFor(listOf(StepDay(today, 1)), StepChartRange.WEEK, today)
        assertTrue(StepChartData.hasAnySteps(filled))
    }

    // ---------- Ziel-Eingabe ----------

    @Test
    fun parser_acceptsPlainAndGroupedNumbers() {
        assertEquals(StepGoalParseResult.Valid(10_000), StepGoalParser.parse("10000"))
        assertEquals(StepGoalParseResult.Valid(10_000), StepGoalParser.parse("10.000"))
        assertEquals(StepGoalParseResult.Valid(12_500), StepGoalParser.parse(" 12 500 "))
    }

    @Test
    fun parser_rejectsInvalidInput() {
        assertEquals(StepGoalParseResult.Invalid(StepGoalError.EMPTY), StepGoalParser.parse("  "))
        assertEquals(StepGoalParseResult.Invalid(StepGoalError.NOT_A_NUMBER), StepGoalParser.parse("abc"))
        assertEquals(StepGoalParseResult.Invalid(StepGoalError.NOT_A_NUMBER), StepGoalParser.parse("10,5"))
        assertEquals(StepGoalParseResult.Invalid(StepGoalError.NOT_A_NUMBER), StepGoalParser.parse("-5"))
        assertEquals(StepGoalParseResult.Invalid(StepGoalError.TOO_LOW), StepGoalParser.parse("50"))
        assertEquals(StepGoalParseResult.Invalid(StepGoalError.TOO_HIGH), StepGoalParser.parse("100001"))
        assertEquals(StepGoalParseResult.Invalid(StepGoalError.TOO_HIGH), StepGoalParser.parse("99999999999999999999"))
    }

    // ---------- Format ----------

    @Test
    fun format_usesGermanThousandsSeparator() {
        assertEquals("10.000", StepFormat.steps(10_000))
        assertEquals("742", StepFormat.steps(742))
    }
}
