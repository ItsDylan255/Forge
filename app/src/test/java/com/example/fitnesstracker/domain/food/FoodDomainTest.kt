package com.example.fitnesstracker.domain.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodDomainTest {

    private val per100 = Macros(kcal = 100.0, protein = 10.0, carbs = 20.0, fat = 5.0)

    private fun entry(id: Long, day: Long, kcal: Double, p: Double = 0.0, c: Double = 0.0, f: Double = 0.0) =
        FoodLogData(id, day, "E$id", 100.0, Macros(kcal, p, c, f))

    // ---------- Hochrechnung ----------

    @Test
    fun scale_twoHundredGrams_doublesValues() {
        val m = FoodCalculator.scale(per100, 200.0)
        assertEquals(200.0, m.kcal, 0.0001)
        assertEquals(20.0, m.protein, 0.0001)
        assertEquals(40.0, m.carbs, 0.0001)
        assertEquals(10.0, m.fat, 0.0001)
    }

    @Test
    fun scale_fiftyGrams_halvesValues() {
        val m = FoodCalculator.scale(per100, 50.0)
        assertEquals(50.0, m.kcal, 0.0001)
        assertEquals(2.5, m.fat, 0.0001)
    }

    @Test
    fun rescale_isProportionalAndSafeForZero() {
        val saved = Macros(300.0, 30.0, 60.0, 15.0)
        val half = FoodCalculator.rescale(saved, 200.0, 100.0)
        assertEquals(150.0, half.kcal, 0.0001)
        assertEquals(7.5, half.fat, 0.0001)

        assertEquals(saved, FoodCalculator.rescale(saved, 0.0, 100.0))
    }

    // ---------- Summen ----------

    @Test
    fun dayTotals_sumsOnlyThatDay() {
        val entries = listOf(entry(1, 10, 100.0, p = 5.0), entry(2, 10, 250.0, p = 7.0), entry(3, 11, 999.0))
        val totals = FoodCalculator.dayTotals(entries, 10)
        assertEquals(350.0, totals.kcal, 0.0001)
        assertEquals(12.0, totals.protein, 0.0001)
    }

    @Test
    fun dayTotals_emptyDayIsZero() {
        assertEquals(Macros.ZERO, FoodCalculator.dayTotals(emptyList(), 10))
    }

    @Test
    fun dailySums_oneEntryPerCalendarDayWithZeros() {
        val entries = listOf(entry(1, 10, 500.0), entry(2, 8, 300.0))
        val sums = FoodCalculator.dailySums(entries, 7, 10)

        assertEquals(7, sums.size)
        assertEquals(4L, sums.first().epochDay)
        assertEquals(10L, sums.last().epochDay)
        assertEquals(500.0, sums.last().totals.kcal, 0.0001)
        assertEquals(0, sums[sums.size - 2].entryCount)
        assertEquals(300.0, sums[sums.size - 3].totals.kcal, 0.0001)
    }

    @Test
    fun average_ignoresDaysWithoutEntries() {
        val sums = FoodCalculator.dailySums(listOf(entry(1, 10, 2000.0), entry(2, 9, 1000.0)), 7, 10)
        val avg = FoodCalculator.average(sums)
        assertEquals(2, avg.daysWithData)
        assertEquals(1500.0, avg.perDay!!.kcal, 0.0001)
    }

    @Test
    fun average_withoutDataIsNull() {
        val avg = FoodCalculator.average(FoodCalculator.dailySums(emptyList(), 7, 10))
        assertEquals(0, avg.daysWithData)
        assertNull(avg.perDay)
    }

    // ---------- Tagesziel ----------

    @Test
    fun goal_fractionAndPercent() {
        assertEquals(0.5, FoodCalculator.goalFraction(1000.0, 2000)!!, 0.0001)
        assertEquals(1.0, FoodCalculator.goalFraction(3000.0, 2000)!!, 0.0001)
        assertEquals(50, FoodCalculator.goalPercent(1000.0, 2000))
        assertEquals(99, FoodCalculator.goalPercent(1999.0, 2000))
        assertEquals(150, FoodCalculator.goalPercent(3000.0, 2000))
        assertNull(FoodCalculator.goalFraction(1000.0, null))
        assertNull(FoodCalculator.goalPercent(1000.0, 0))
    }

    // ---------- Eingaben ----------

    @Test
    fun grams_acceptsCommaDotAndSuffix() {
        assertEquals(FoodParseResult.Valid(200.0), FoodInputParser.parseGrams("200"))
        assertEquals(FoodParseResult.Valid(200.5), FoodInputParser.parseGrams("200,5"))
        assertEquals(FoodParseResult.Valid(150.0), FoodInputParser.parseGrams("150 g"))
    }

    @Test
    fun grams_rejectsInvalid() {
        assertTrue(FoodInputParser.parseGrams("") is FoodParseResult.Invalid)
        assertTrue(FoodInputParser.parseGrams("0") is FoodParseResult.Invalid)
        assertTrue(FoodInputParser.parseGrams("-5") is FoodParseResult.Invalid)
        assertTrue(FoodInputParser.parseGrams("abc") is FoodParseResult.Invalid)
        assertTrue(FoodInputParser.parseGrams("10001") is FoodParseResult.Invalid)
    }

    @Test
    fun kcal_isRequiredAndBounded() {
        assertEquals(FoodParseResult.Valid(0.0), FoodInputParser.parseKcal("0"))
        assertEquals(FoodParseResult.Valid(52.5), FoodInputParser.parseKcal("52,5"))
        assertTrue(FoodInputParser.parseKcal("") is FoodParseResult.Invalid)
        assertTrue(FoodInputParser.parseKcal("-1") is FoodParseResult.Invalid)
        assertTrue(FoodInputParser.parseKcal("1001") is FoodParseResult.Invalid)
    }

    @Test
    fun macro_emptyMeansZeroAndMaxIsHundred() {
        assertEquals(FoodParseResult.Valid(0.0), FoodInputParser.parseMacro("", "Protein"))
        assertEquals(FoodParseResult.Valid(12.5), FoodInputParser.parseMacro("12,5", "Protein"))
        assertTrue(FoodInputParser.parseMacro("101", "Fett") is FoodParseResult.Invalid)
        assertTrue(FoodInputParser.parseMacro("-3", "Fett") is FoodParseResult.Invalid)
        assertTrue(FoodInputParser.parseMacro("x", "Fett") is FoodParseResult.Invalid)
    }

    @Test
    fun name_mustNotBeEmpty() {
        assertEquals(FoodParseResult.Valid("Haferflocken"), FoodInputParser.parseName("  Haferflocken "))
        assertTrue(FoodInputParser.parseName("   ") is FoodParseResult.Invalid)
    }

    // ---------- Format ----------

    @Test
    fun format_numbers() {
        assertEquals("2.150 kcal", FoodFormat.kcal(2150.4))
        assertEquals("200 g", FoodFormat.grams(200.0))
        assertEquals("12,5 g", FoodFormat.grams(12.5))
        assertEquals("12,5 g", FoodFormat.macro(12.54))
        assertEquals("200", FoodFormat.number(200.0))
        assertEquals("82,5", FoodFormat.number(82.5))
    }

    @Test
    fun format_dayLabels() {
        assertEquals("Heute", FoodFormat.dayLabel(100, 100))
        assertEquals("Gestern", FoodFormat.dayLabel(99, 100))
        assertEquals("Morgen", FoodFormat.dayLabel(101, 100))
    }
}
