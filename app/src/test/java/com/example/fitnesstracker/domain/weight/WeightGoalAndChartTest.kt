package com.example.fitnesstracker.domain.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightGoalCalculatorTest {

    @Test
    fun loseGoalHalfWay() {
        val p = WeightGoalCalculator.progress(targetKg = 80.0, currentKg = 85.0, startKg = 90.0)

        assertEquals(GoalDirection.LOSE, p.direction)
        assertEquals(5.0, p.remainingKg, 1e-9)
        assertEquals(0.5, p.progressFraction!!, 1e-9)
    }

    @Test
    fun gainGoalHalfWay() {
        val p = WeightGoalCalculator.progress(targetKg = 80.0, currentKg = 75.0, startKg = 70.0)

        assertEquals(GoalDirection.GAIN, p.direction)
        assertEquals(5.0, p.remainingKg, 1e-9)
        assertEquals(0.5, p.progressFraction!!, 1e-9)
    }

    @Test
    fun reachedAndOvershotIsFullProgress() {
        val lose = WeightGoalCalculator.progress(80.0, 79.0, 90.0)
        assertEquals(GoalDirection.REACHED, lose.direction)
        assertEquals(1.0, lose.progressFraction!!, 1e-9)

        val gain = WeightGoalCalculator.progress(80.0, 81.0, 70.0)
        assertEquals(GoalDirection.REACHED, gain.direction)
        assertEquals(1.0, gain.progressFraction!!, 1e-9)
    }

    @Test
    fun movingAwayFromGoalIsZeroProgressNotNegative() {
        val p = WeightGoalCalculator.progress(80.0, 95.0, 90.0)

        assertEquals(GoalDirection.LOSE, p.direction)
        assertEquals(15.0, p.remainingKg, 1e-9)
        assertEquals(0.0, p.progressFraction!!, 1e-9)
    }

    @Test
    fun withoutStartValueThereIsNoPercentage() {
        assertNull(WeightGoalCalculator.progress(80.0, 85.0, null).progressFraction)
        assertEquals(GoalDirection.LOSE, WeightGoalCalculator.progress(80.0, 85.0, null).direction)
        assertEquals(GoalDirection.GAIN, WeightGoalCalculator.progress(80.0, 75.0, null).direction)
        assertEquals(GoalDirection.REACHED, WeightGoalCalculator.progress(80.0, 80.0, null).direction)
    }

    @Test
    fun startEqualToTargetHasNoPercentage() {
        val p = WeightGoalCalculator.progress(80.0, 82.0, 80.0)

        assertNull(p.progressFraction)
        assertEquals(GoalDirection.LOSE, p.direction)
    }
}

class WeightChartDataTest {

    private val today = 1000L

    private fun m(day: Long, kg: Double) = WeightMeasurement(dateEpochDay = day, weightKg = kg)

    private val data = listOf(
        m(1000, 80.0), m(993, 81.0), m(992, 82.0),
        m(900, 83.0), m(700, 84.0), m(600, 85.0)
    )

    private fun days(range: ChartRange, list: List<WeightMeasurement> = data) =
        WeightChartData.pointsFor(list, range, today).map { it.dateEpochDay }

    @Test
    fun weekAndMonthWindows() {
        assertEquals(listOf(993L, 1000L), days(ChartRange.WEEK))
        assertEquals(listOf(993L, 1000L, 992L).sorted(), days(ChartRange.MONTH))
    }

    @Test
    fun quarterAndYearWindows() {
        assertEquals(listOf(992L, 993L, 1000L), days(ChartRange.QUARTER))
        assertEquals(listOf(700L, 900L, 992L, 993L, 1000L), days(ChartRange.YEAR))
    }

    @Test
    fun allReturnsEverythingChronologically() {
        assertEquals(listOf(600L, 700L, 900L, 992L, 993L, 1000L), days(ChartRange.ALL))
    }

    @Test
    fun futureMeasurementsOnlyAppearInAll() {
        val withFuture = data + m(1001, 79.0)

        assertFalse(1001L in days(ChartRange.WEEK, withFuture))
        assertTrue(1001L in days(ChartRange.ALL, withFuture))
    }

    @Test
    fun enoughPointsNeedsAtLeastTwo() {
        assertFalse(WeightChartData.hasEnoughPoints(emptyList()))
        assertFalse(WeightChartData.hasEnoughPoints(listOf(m(1, 80.0))))
        assertTrue(WeightChartData.hasEnoughPoints(listOf(m(1, 80.0), m(2, 81.0))))
    }
}