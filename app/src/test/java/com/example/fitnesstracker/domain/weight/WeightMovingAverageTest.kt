package com.example.fitnesstracker.domain.weight

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightMovingAverageTest {

    private fun m(day: Long, kg: Double) = WeightMeasurement(dateEpochDay = day, weightKg = kg)

    @Test fun averagesTheLastSevenDays() {
        val trend = WeightChartData.movingAverage(listOf(m(0, 80.0), m(1, 82.0), m(6, 84.0), m(7, 86.0), m(20, 70.0)))
        assertEquals(listOf(0L, 1L, 6L, 7L, 20L), trend.map { it.dateEpochDay })
        assertEquals(80.0, trend[0].weightKg, 1e-9)
        assertEquals(81.0, trend[1].weightKg, 1e-9)
        assertEquals(82.0, trend[2].weightKg, 1e-9)
        // Tag 0 fällt raus (Fenster 1..7)
        assertEquals(84.0, trend[3].weightKg, 1e-9)
        assertEquals(70.0, trend[4].weightKg, 1e-9)
    }

    @Test fun sliceKeepsTrendNeighbors() {
        val data = listOf(m(80, 80.0), m(91, 81.0), m(95, 82.0), m(100, 83.0))
        val slice = WeightChartData.sliceFor(data, ChartRange.WEEK, 98)!!
        assertEquals(listOf(80L, 91L, 95L, 100L), slice.trend.map { it.dateEpochDay })
    }
}
