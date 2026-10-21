package com.example.fitnesstracker.domain.food

/** Eingabeprüfung für Mahlzeiten. Name und Gramm nutzen die Regeln von [FoodInputParser]. */
object MealInputParser {

    const val MAX_INGREDIENTS = 30

    fun parseName(input: String): FoodParseResult<String> = FoodInputParser.parseName(input)

    /** Gegessene Menge einer Mahlzeit in Gramm (größer als 0, höchstens 10.000). */
    fun parseEatenGrams(input: String): FoodParseResult<Double> = FoodInputParser.parseGrams(input)

    /** Eine Mahlzeit braucht mindestens eine und höchstens [MAX_INGREDIENTS] Zutaten. */
    fun validateIngredients(
        ingredients: List<MealIngredientData>
    ): FoodParseResult<List<MealIngredientData>> = when {
        ingredients.isEmpty() ->
            FoodParseResult.Invalid("Füge mindestens eine Zutat hinzu.")
        ingredients.size > MAX_INGREDIENTS ->
            FoodParseResult.Invalid("Eine Mahlzeit darf höchstens $MAX_INGREDIENTS Zutaten haben.")
        else -> FoodParseResult.Valid(ingredients)
    }
}
