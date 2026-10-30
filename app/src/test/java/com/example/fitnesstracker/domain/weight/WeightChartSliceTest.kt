package com.example.fitnesstracker.domain.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightChartSliceTest {

    private fun m(day: Long) = WeightMeasurement(dateEpochDay = day, weightKg = 80.0)

    private val data = listOf(m(100), m(80), m(95), m(60), m(91))

    @Test fun emptyGivesNull() = assertNull(WeightChartData.sliceFor(emptyList(), ChartRange.WEEK, 100))

    @Test fun windowWithNeighborsForTheLine() {
        val slice = WeightChartData.sliceFor(data, ChartRange.WEEK, 98)!!
        assertEquals(listOf(91L, 95L), slice.visible.map { it.dateEpochDay })
        assertEquals(listOf(80L, 91L, 95L, 100L), slice.line.map { it.dateEpochDay })
        assertEquals(91L, slice.startDay)
        assertEquals(98L, slice.endDay)
    }

    @Test fun windowWithoutPointsStillHasNeighbors() {
        val slice = WeightChartData.sliceFor(data, ChartRange.WEEK, 75)!!
        assertEquals(emptyList<Long>(), slice.visible.map { it.dateEpochDay })
        assertEquals(listOf(60L, 80L), slice.line.map { it.dateEpochDay })
    }

    @Test fun allSpansFirstToLastEntry() {
        val slice = WeightChartData.sliceFor(data, ChartRange.ALL, 0)!!
        assertEquals(5, slice.visible.size)
        assertEquals(60L, slice.startDay)
        assertEquals(100L, slice.endDay)
    }
}
