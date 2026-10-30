package com.example.fitnesstracker.domain.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartWindowTest {

    @Test fun endStaysInsideRange() = assertEquals(50L, ChartWindow.clampEnd(50, 7, 0, 100))

    @Test fun cannotGoPastLastDay() = assertEquals(100L, ChartWindow.clampEnd(120, 7, 0, 100))

    @Test fun windowStartStopsAtFirstDay() = assertEquals(7L, ChartWindow.clampEnd(3, 7, 0, 100))

    @Test fun shortDataRangeStaysAtLastDay() = assertEquals(100L, ChartWindow.clampEnd(10, 30, 90, 100))

    @Test fun canPanOnlyWithMoreDataThanWindow() {
        assertTrue(ChartWindow.canPan(7, 0, 100))
        assertFalse(ChartWindow.canPan(7, 95, 100))
        assertFalse(ChartWindow.canPan(5, 95, 100))
    }
}
