package com.example.fitnesstracker.domain.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightInputParserTest {

    private fun assertValid(input: String, expectedKg: Double) {
        val result = WeightInputParser.parse(input)
        assertTrue("'$input' sollte gültig sein, war aber $result", result is WeightParseResult.Valid)
        assertEquals(expectedKg, (result as WeightParseResult.Valid).weightKg, 1e-9)
    }

    private fun assertInvalid(input: String, expected: WeightInputError) {
        assertEquals(
            "Eingabe '$input'",
            WeightParseResult.Invalid(expected),
            WeightInputParser.parse(input)
        )
    }

    @Test
    fun acceptsDecimalComma() = assertValid("95,4", 95.4)

    @Test
    fun acceptsDecimalPoint() = assertValid("95.4", 95.4)

    @Test
    fun acceptsWholeNumbers() = assertValid("95", 95.0)

    @Test
    fun trimsWhitespace() = assertValid("  95,4  ", 95.4)

    @Test
    fun acceptsKgSuffix() {
        assertValid("95,4 kg", 95.4)
        assertValid("95KG", 95.0)
    }

    @Test
    fun acceptsTrailingSeparatorAndLeadingSeparator() {
        assertValid("95,", 95.0)
        assertValid(".5", 0.5)
    }

    @Test
    fun emptyInputIsReported() {
        assertInvalid("", WeightInputError.EMPTY)
        assertInvalid("   ", WeightInputError.EMPTY)
    }

    @Test
    fun nonNumbersAreRejected() {
        assertInvalid("abc", WeightInputError.NOT_A_NUMBER)
        assertInvalid("kg", WeightInputError.NOT_A_NUMBER)
        assertInvalid(",", WeightInputError.NOT_A_NUMBER)
    }

    @Test
    fun javaSpecificNumberFormatsAreRejected() {
        assertInvalid("NaN", WeightInputError.NOT_A_NUMBER)
        assertInvalid("Infinity", WeightInputError.NOT_A_NUMBER)
        assertInvalid("1e3", WeightInputError.NOT_A_NUMBER)
        assertInvalid("95d", WeightInputError.NOT_A_NUMBER)
        assertInvalid("0x10", WeightInputError.NOT_A_NUMBER)
    }

    @Test
    fun ambiguousSeparatorsAreRejected() {
        assertInvalid("95,4,5", WeightInputError.NOT_A_NUMBER)
        assertInvalid("9.5.4", WeightInputError.NOT_A_NUMBER)
        assertInvalid("1.234,5", WeightInputError.NOT_A_NUMBER)
    }

    @Test
    fun overflowingNumberIsRejected() {
        assertInvalid("9".repeat(400), WeightInputError.NOT_A_NUMBER)
    }

    @Test
    fun zeroAndNegativeAreNotPositive() {
        assertInvalid("0", WeightInputError.NOT_POSITIVE)
        assertInvalid("0,0", WeightInputError.NOT_POSITIVE)
        assertInvalid("-5", WeightInputError.NOT_POSITIVE)
        assertInvalid("-0", WeightInputError.NOT_POSITIVE)
    }

    @Test
    fun unusualRangeIsOnlyAWarningAndBoundariesAreNotUnusual() {
        fun unusual(input: String) =
            (WeightInputParser.parse(input) as WeightParseResult.Valid).isUnusual

        assertTrue(unusual("19,9"))
        assertFalse(unusual("20"))
        assertFalse(unusual("95,4"))
        assertFalse(unusual("400"))
        assertTrue(unusual("400,1"))
        assertTrue(unusual("954"))
    }
}