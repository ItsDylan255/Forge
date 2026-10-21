package com.example.fitnesstracker.domain.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightTrendForecastTest {

    private fun m(day: Long, kg: Double) = WeightMeasurement(day, kg)

    /** Gewicht sinkt jeden Tag um 0,1 kg: 90,0 an Tag 0 bis 89,1 an Tag 9. */
    private val falling = (0L..9L).map { m(it, 90.0 - 0.1 * it) }

    @Test fun predictsFromRecentTrend() {
        val result = WeightForecaster.predictTrend(85.0, 90.0, falling, today = 9)
        // Noch 4,1 kg bei 0,1 kg pro Tag = 41 Tage ab Tag 9
        assertEquals(WeightForecast.Predicted(50), result)
    }

    @Test fun worksRightAfterSettingTheGoal() {
        // Das Ziel wurde erst heute angelegt, die Messungen davor zählen trotzdem
        val result = WeightForecaster.predictTrend(85.0, 89.1, falling, today = 9)
        assertTrue(result is WeightForecast.Predicted)
    }

    @Test fun sevenDailyMeasurementsAlreadyGiveATrend() {
        // 7 Tage in Folge gewogen (Tag 0 bis 6) reichen
        val week = (0L..6L).map { m(it, 90.0 - 0.1 * it) }
        // Noch 4,4 kg bei 0,1 kg pro Tag = 44 Tage ab Tag 6
        assertEquals(WeightForecast.Predicted(50), WeightForecaster.predictTrend(85.0, 90.0, week, today = 6))
    }

    @Test fun sixMeasurementsAreNotEnough() {
        val six = (0L..5L).map { m(it, 90.0 - 0.1 * it) }
        assertEquals(WeightForecast.NotEnoughData, WeightForecaster.predictTrend(85.0, 90.0, six, today = 5))
        assertEquals(WeightForecast.NotEnoughData, WeightForecaster.predictTrend(85.0, 90.0, emptyList(), today = 5))
    }

    @Test fun sparseMeasurementsCountToo() {
        // Sieben Messungen über drei Wochen
        val sparse = listOf(0L, 3L, 6L, 10L, 14L, 17L, 20L).map { m(it, 90.0 - 0.1 * it) }
        assertTrue(WeightForecaster.predictTrend(85.0, 90.0, sparse, today = 20) is WeightForecast.Predicted)
    }

    @Test fun measurementsOutsideTheWindowDoNotCountTowardsTheMinimum() {
        // Nur 3 Messungen in den letzten 28 Tagen, der Rest liegt weiter zurück
        val old = (-60L..-54L).map { m(it, 95.0) }
        val recent = listOf(m(5, 90.0), m(7, 89.8), m(9, 89.6))
        assertEquals(WeightForecast.NotEnoughData, WeightForecaster.predictTrend(85.0, 95.0, old + recent, today = 9))
    }

    @Test fun oldMeasurementsOutsideWindowAreIgnored() {
        // Vor 100 Tagen stieg das Gewicht, in den letzten Tagen sinkt es
        val old = (-100L..-90L).map { m(it, 80.0 + 0.2 * (it + 100)) }
        val result = WeightForecaster.predictTrend(85.0, 90.0, old + falling, today = 9)
        assertEquals(WeightForecast.Predicted(50), result)
    }

    @Test fun windowIncludesTheFirstOfTwentyEightDays() {
        // Tag -19 bis 9 sind 29 Tage, Tag -19 liegt außerhalb
        val inside = m(-18, 95.0)
        val outside = m(-19, 200.0)
        val result = WeightForecaster.predictTrend(85.0, 95.0, listOf(outside, inside) + falling, today = 9)
        // Tag -18 zählt mit (Tempo ca. -0,22 kg pro Tag), der Ausreißer an Tag -19 nicht
        assertEquals(WeightForecast.Predicted(28), result)
    }

    @Test fun reachedWhenTargetPassed() {
        // Letzte Messung 89,1 kg liegt schon unter dem Ziel von 89,5 kg
        val result = WeightForecaster.predictTrend(89.5, 90.0, falling, today = 9)
        assertEquals(WeightForecast.Reached, result)
    }

    @Test fun movingAwayFromGoalGivesNoProgress() {
        val rising = (0L..9L).map { m(it, 90.0 + 0.1 * it) }
        assertEquals(WeightForecast.NoProgress, WeightForecaster.predictTrend(80.0, 90.0, rising, today = 9))
    }

    @Test fun flatWeightGivesNoProgress() {
        val flat = (0L..9L).map { m(it, 90.0) }
        assertEquals(WeightForecast.NoProgress, WeightForecaster.predictTrend(85.0, 90.0, flat, today = 9))
    }

    @Test fun veryFarGoalGivesTooFar() {
        val slow = (0L..9L).map { m(it, 90.0 - 0.011 * it) }
        assertEquals(WeightForecast.TooFar, WeightForecaster.predictTrend(40.0, 90.0, slow, today = 9))
    }
}
