package com.example.fitnesstracker.domain.food

import com.example.fitnesstracker.data.local.MealIngredient

/** Room-Zutat -> Domain-Zutat. */
fun MealIngredient.toData(): MealIngredientData = MealIngredientData(
    name = name,
    grams = grams,
    per100g = Macros(caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g)
)
