package com.example.fitnesstracker.domain.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealDomainTest {

    private val oats = MealIngredientData("Hafer", 60.0, Macros(370.0, 13.5, 59.0, 7.0))
    private val milk = MealIngredientData("Milch", 200.0, Macros(64.0, 3.4, 4.8, 3.6))
    private val meal = listOf(oats, milk)

    private fun assertMacros(e: Macros, a: Macros) {
        assertEquals(e.kcal, a.kcal, 0.001)
        assertEquals(e.protein, a.protein, 0.001)
        assertEquals(e.carbs, a.carbs, 0.001)
        assertEquals(e.fat, a.fat, 0.001)
    }

    @Test fun singleIngredient() =
        assertMacros(Macros(222.0, 8.1, 35.4, 4.2), MealCalculator.ingredientMacros(oats))

    @Test fun totalsSumAllIngredients() =
        assertMacros(Macros(350.0, 14.9, 45.0, 11.4), MealCalculator.totals(meal))

    @Test fun totalGrams() = assertEquals(260.0, MealCalculator.totalGrams(meal), 0.001)

    @Test fun portionHalfGivesHalf() =
        assertMacros(Macros(175.0, 7.45, 22.5, 5.7), MealCalculator.portion(meal, 130.0))

    @Test fun portionOfAllEqualsTotals() =
        assertMacros(MealCalculator.totals(meal), MealCalculator.portion(meal, 260.0))

    @Test fun portionAboveTotalScalesUp() =
        assertMacros(Macros(700.0, 29.8, 90.0, 22.8), MealCalculator.portion(meal, 520.0))

    @Test fun per100gOfMeal() {
        val p = MealCalculator.per100g(meal)
        assertNotNull(p)
        assertEquals(350.0 / 2.6, p!!.kcal, 0.001)
    }

    @Test fun emptyMealIsZero() {
        assertMacros(Macros.ZERO, MealCalculator.totals(emptyList()))
        assertMacros(Macros.ZERO, MealCalculator.portion(emptyList(), 100.0))
        assertNull(MealCalculator.per100g(emptyList()))
    }

    @Test fun ingredientValidation() {
        assertTrue(MealInputParser.validateIngredients(emptyList()) is FoodParseResult.Invalid)
        assertTrue(MealInputParser.validateIngredients(meal) is FoodParseResult.Valid)
        val tooMany = List(31) { oats }
        assertTrue(MealInputParser.validateIngredients(tooMany) is FoodParseResult.Invalid)
    }

    @Test fun nameAndEatenGramsReuseFoodRules() {
        assertTrue(MealInputParser.parseName("  ") is FoodParseResult.Invalid)
        assertEquals(FoodParseResult.Valid("Haferbrei"), MealInputParser.parseName(" Haferbrei "))
        assertEquals(FoodParseResult.Valid(130.5), MealInputParser.parseEatenGrams("130,5 g"))
        assertTrue(MealInputParser.parseEatenGrams("0") is FoodParseResult.Invalid)
    }
}
