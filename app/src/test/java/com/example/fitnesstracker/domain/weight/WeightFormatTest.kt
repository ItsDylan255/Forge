package com.example.fitnesstracker.domain.weight

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeightFormatTest {

    @Test
    fun forInputUsesCommaAndKeepsExactValue() {
        assertEquals("95,4", WeightFormat.forInput(95.4))
        assertEquals("95", WeightFormat.forInput(95.0))
        assertEquals("95,45", WeightFormat.forInput(95.45))
        assertEquals("100", WeightFormat.forInput(100.0))
    }

    @Test
    fun forInputOfNonFiniteValueIsEmpty() {
        assertEquals("", WeightFormat.forInput(Double.NaN))
        assertEquals("", WeightFormat.forInput(Double.POSITIVE_INFINITY))
    }

    @Test
    fun forInputRoundTripsThroughParser() {
        val original = 95.45
        val parsed = WeightInputParser.parse(WeightFormat.forInput(original))
        assertEquals(original, (parsed as WeightParseResult.Valid).weightKg, 0.0)
    }

    @Test
    fun weightShowsOneDecimalWithComma() {
        assertEquals("95,4 kg", WeightFormat.weight(95.44))
        assertEquals("80,0 kg", WeightFormat.weight(80.0))
    }

    @Test
    fun deltaHasSignAndTreatsTinyChangesAsZero() {
        assertEquals("+0,4 kg", WeightFormat.delta(0.4))
        assertEquals("\u22121,2 kg", WeightFormat.delta(-1.2))
        assertEquals("\u00B10,0 kg", WeightFormat.delta(0.04))
        assertEquals("\u00B10,0 kg", WeightFormat.delta(-0.04))
        assertEquals("\u00B10,0 kg", WeightFormat.delta(0.0))
    }

    @Test
    fun dateIsFormattedGerman() {
        assertEquals("01.01.1970", WeightFormat.date(0))
        assertEquals("10.10.2026", WeightFormat.date(LocalDate.of(2026, 10, 10).toEpochDay()))
    }
}