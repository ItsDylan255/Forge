package com.example.fitnesstracker.domain.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightCalculatorTest {

    private val today = 100L

    private fun m(day: Long, kg: Double) = WeightMeasurement(dateEpochDay = day, weightKg = kg)

    @Test
    fun emptyListHasNoValues() {
        val s = WeightCalculator.statistics(emptyList(), today)

        assertNull(s.current)
        assertNull(s.latest)
        assertNull(s.changeToPrevious)
        assertNull(s.change7Days)
        assertNull(s.change30Days)
        assertNull(s.changeSinceFirst)
        assertNull(s.lowest)
        assertNull(s.highest)
        assertEquals(0, s.measurementCount)
    }

    @Test
    fun singleMeasurementTodayHasNoComparisons() {
        val s = WeightCalculator.statistics(listOf(m(today, 90.0)), today)

        assertTrue(s.current!!.isToday)
        assertEquals(90.0, s.current!!.weightKg, 0.0)
        assertNull(s.changeToPrevious)
        assertNull(s.changeSinceFirst)
        assertNull(s.change7Days)
        assertEquals(90.0, s.lowest!!.weightKg, 0.0)
        assertEquals(90.0, s.highest!!.weightKg, 0.0)
    }

    @Test
    fun currentIsMarkedAsLastMeasurementWhenNothingToday() {
        val s = WeightCalculator.statistics(listOf(m(90, 91.0), m(95, 90.5)), today)

        assertFalse(s.current!!.isToday)
        assertEquals(95L, s.current!!.dateEpochDay)
        assertEquals(90.5, s.current!!.weightKg, 0.0)
        assertEquals(95L, s.latest!!.dateEpochDay)
    }

    @Test
    fun todaysEntryIsCurrentEvenIfNotInsertedLast() {
        val s = WeightCalculator.statistics(listOf(m(today, 90.0), m(50, 99.0)), today)

        assertTrue(s.current!!.isToday)
        assertEquals(90.0, s.current!!.weightKg, 0.0)
    }

    @Test
    fun allChangesAreCalculatedFromRealMeasurements() {
        val s = WeightCalculator.statistics(
            listOf(m(90, 96.0), m(95, 95.0), m(100, 94.5)),
            today
        )

        assertEquals(-0.5, s.changeToPrevious!!.deltaKg, 1e-9)
        assertEquals(95L, s.changeToPrevious!!.fromEpochDay)
        assertEquals(100L, s.changeToPrevious!!.toEpochDay)

        assertEquals(-0.5, s.change7Days!!.deltaKg, 1e-9)
        assertEquals(95L, s.change7Days!!.fromEpochDay)

        assertEquals(-1.5, s.change30Days!!.deltaKg, 1e-9)
        assertEquals(90L, s.change30Days!!.fromEpochDay)

        assertEquals(-1.5, s.changeSinceFirst!!.deltaKg, 1e-9)
        assertEquals(90L, s.changeSinceFirst!!.fromEpochDay)

        assertEquals(94.5, s.lowest!!.weightKg, 0.0)
        assertEquals(100L, s.lowest!!.dateEpochDay)
        assertEquals(96.0, s.highest!!.weightKg, 0.0)
        assertEquals(90L, s.highest!!.dateEpochDay)
        assertEquals(3, s.measurementCount)
    }

    @Test
    fun unsortedInputGivesSameResult() {
        val sorted = WeightCalculator.statistics(
            listOf(m(90, 96.0), m(95, 95.0), m(100, 94.5)), today
        )
        val shuffled = WeightCalculator.statistics(
            listOf(m(100, 94.5), m(90, 96.0), m(95, 95.0)), today
        )

        assertEquals(sorted, shuffled)
    }

    @Test
    fun periodWithoutComparisonMeasurementIsNull() {
        val s = WeightCalculator.statistics(listOf(m(80, 90.0), m(100, 89.0)), today)

        assertNull(s.change7Days)
        assertEquals(-1.0, s.change30Days!!.deltaKg, 1e-9)
    }

    @Test
    fun sevenDayWindowIncludesDayMinusSevenButNotMinusEight() {
        val s = WeightCalculator.statistics(
            listOf(m(92, 90.0), m(93, 91.0), m(100, 92.0)),
            today
        )

        assertEquals(93L, s.change7Days!!.fromEpochDay)
        assertEquals(1.0, s.change7Days!!.deltaKg, 1e-9)
    }

    @Test
    fun futureMeasurementsAreNotUsedForPeriodChanges() {
        val s = WeightCalculator.statistics(listOf(m(99, 80.0), m(105, 85.0)), today)

        assertNull(s.change7Days)
    }

    @Test
    fun tiesForLowestAndHighestUseEarliestDate() {
        val s = WeightCalculator.statistics(listOf(m(20, 80.0), m(10, 80.0)), today)

        assertEquals(10L, s.lowest!!.dateEpochDay)
        assertEquals(10L, s.highest!!.dateEpochDay)
    }

    @Test
    fun historyIsNewestFirstWithDeltaToPrevious() {
        val items = WeightCalculator.historyItems(
            listOf(m(2, 91.5), m(1, 90.0), m(3, 91.0))
        )

        assertEquals(listOf(3L, 2L, 1L), items.map { it.measurement.dateEpochDay })

        assertEquals(-0.5, items[0].deltaToPreviousKg!!, 1e-9)
        assertEquals(2L, items[0].previousEpochDay)
        assertEquals(1.5, items[1].deltaToPreviousKg!!, 1e-9)
        assertNull(items[2].deltaToPreviousKg)
        assertNull(items[2].previousEpochDay)
    }

    @Test
    fun historyOfEmptyListIsEmpty() {
        assertTrue(WeightCalculator.historyItems(emptyList()).isEmpty())
        assertNotNull(WeightCalculator.historyItems(listOf(m(1, 90.0))).single())
    }
}