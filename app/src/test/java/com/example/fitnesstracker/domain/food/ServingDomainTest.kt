package com.example.fitnesstracker.domain.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServingDomainTest {

    // ---------- Rechnen ----------

    @Test fun gramsForMultipliesAndRounds() {
        assertEquals(60.0, ServingCalculator.gramsFor(30.0, 2.0), 0.0001)
        assertEquals(45.0, ServingCalculator.gramsFor(30.0, 1.5), 0.0001)
        assertEquals(49.95, ServingCalculator.gramsFor(33.3, 1.5), 0.0001)
        assertEquals(10.0, ServingCalculator.gramsFor(20.0, 0.5), 0.0001)
    }

    @Test fun mealServingDividesTotal() {
        assertEquals(130.0, ServingCalculator.mealServingGrams(260.0, 2.0), 0.0001)
        assertEquals(86.67, ServingCalculator.mealServingGrams(260.0, 3.0), 0.0001)
        assertEquals(260.0, ServingCalculator.mealServingGrams(260.0, 1.0), 0.0001)
    }

    @Test fun mealServingWithInvalidServingsGivesWholeMeal() {
        assertEquals(260.0, ServingCalculator.mealServingGrams(260.0, 0.0), 0.0001)
    }

    @Test fun countStepsUp() {
        assertEquals(1.0, ServingCalculator.stepCount(0.5, up = true), 0.0001)
        assertEquals(1.0, ServingCalculator.stepCount(0.2, up = true), 0.0001)
        assertEquals(2.0, ServingCalculator.stepCount(1.0, up = true), 0.0001)
        assertEquals(2.5, ServingCalculator.stepCount(1.5, up = true), 0.0001)
        assertEquals(100.0, ServingCalculator.stepCount(99.5, up = true), 0.0001)
        assertEquals(100.0, ServingCalculator.stepCount(100.0, up = true), 0.0001)
    }

    @Test fun countStepsDown() {
        assertEquals(1.0, ServingCalculator.stepCount(2.0, up = false), 0.0001)
        assertEquals(0.5, ServingCalculator.stepCount(1.0, up = false), 0.0001)
        assertEquals(0.5, ServingCalculator.stepCount(1.5, up = false), 0.0001)
        assertEquals(0.5, ServingCalculator.stepCount(0.5, up = false), 0.0001)
    }

    @Test fun defaultIsFirstPortionOrGrams() {
        assertEquals(0, ServingCalculator.defaultPortionIndex(3))
        assertNull(ServingCalculator.defaultPortionIndex(0))
    }

    @Test fun entryNoteShowsCountAndName() {
        assertEquals("2 × Scheibe", ServingCalculator.entryNote(2.0, "Scheibe"))
        assertEquals("1,5 × Portion", ServingCalculator.entryNote(1.5, "Portion"))
        assertEquals("1 × Stück", ServingCalculator.entryNote(1.0, "Stück"))
    }

    // ---------- Eingaben ----------

    @Test fun portionNameRules() {
        assertEquals(FoodParseResult.Valid("Scheibe"), ServingInputParser.parseName("  Scheibe "))
        assertTrue(ServingInputParser.parseName("   ") is FoodParseResult.Invalid)
        assertTrue(ServingInputParser.parseName("x".repeat(31)) is FoodParseResult.Invalid)
        assertEquals(FoodParseResult.Valid("x".repeat(30)), ServingInputParser.parseName("x".repeat(30)))
    }

    @Test fun portionGramsReuseFoodRule() {
        assertEquals(FoodParseResult.Valid(30.0), ServingInputParser.parseGrams("30 g"))
        assertTrue(ServingInputParser.parseGrams("0") is FoodParseResult.Invalid)
        assertTrue(ServingInputParser.parseGrams("10001") is FoodParseResult.Invalid)
    }

    @Test fun countAcceptsHalvesAndCommas() {
        assertEquals(FoodParseResult.Valid(2.0), ServingInputParser.parseCount("2"))
        assertEquals(FoodParseResult.Valid(1.5), ServingInputParser.parseCount("1,5"))
        assertEquals(FoodParseResult.Valid(0.5), ServingInputParser.parseCount("0.5"))
        assertEquals(FoodParseResult.Valid(100.0), ServingInputParser.parseCount("100"))
    }

    @Test fun countRejectsInvalid() {
        assertTrue(ServingInputParser.parseCount("") is FoodParseResult.Invalid)
        assertTrue(ServingInputParser.parseCount("0") is FoodParseResult.Invalid)
        assertTrue(ServingInputParser.parseCount("100,5") is FoodParseResult.Invalid)
        assertTrue(ServingInputParser.parseCount("-1") is FoodParseResult.Invalid)
        assertTrue(ServingInputParser.parseCount("abc") is FoodParseResult.Invalid)
        assertTrue((ServingInputParser.parseCount("0") as FoodParseResult.Invalid).message.contains("Anzahl"))
    }

    @Test fun servingsOfAMeal() {
        assertEquals(FoodParseResult.Valid(4.0), ServingInputParser.parseServings("4"))
        assertTrue(ServingInputParser.parseServings("0") is FoodParseResult.Invalid)
        assertTrue(ServingInputParser.parseServings("101") is FoodParseResult.Invalid)
        assertTrue((ServingInputParser.parseServings("x") as FoodParseResult.Invalid).message.contains("Portionen"))
    }

    @Test fun portionListRules() {
        val ok = listOf(PortionData("Scheibe", 30.0), PortionData("Portion", 150.0))
        assertTrue(ServingInputParser.validatePortions(ok) is FoodParseResult.Valid)
        assertTrue(ServingInputParser.validatePortions(emptyList()) is FoodParseResult.Valid)

        val duplicate = listOf(PortionData("Scheibe", 30.0), PortionData(" scheibe ", 40.0))
        assertTrue(ServingInputParser.validatePortions(duplicate) is FoodParseResult.Invalid)

        val tooMany = (1..11).map { PortionData("P$it", 10.0) }
        assertTrue(ServingInputParser.validatePortions(tooMany) is FoodParseResult.Invalid)
        assertTrue(ServingInputParser.validatePortions(tooMany.take(10)) is FoodParseResult.Valid)
    }

    @Test fun suggestionsAreValidAndUnique() {
        val names = ServingInputParser.suggestions
        assertTrue(names.isNotEmpty())
        assertEquals(names.size, names.map { it.lowercase() }.distinct().size)
        names.forEach { assertTrue(ServingInputParser.parseName(it) is FoodParseResult.Valid) }
    }
}
