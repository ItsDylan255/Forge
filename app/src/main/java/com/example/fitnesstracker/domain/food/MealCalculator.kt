package com.example.fitnesstracker.domain.food

/** Eine Zutat einer Mahlzeit. [per100g] ist der Snapshot der Werte pro 100 g beim Hinzufügen. */
data class MealIngredientData(
    val name: String,
    val grams: Double,
    val per100g: Macros
)

/** Rechenlogik für Mahlzeiten aus mehreren Zutaten. */
object MealCalculator {

    /** Kalorien und Makros einer einzelnen Zutat. */
    fun ingredientMacros(ingredient: MealIngredientData): Macros =
        FoodCalculator.scale(ingredient.per100g, ingredient.grams)

    /** Summe aller Zutaten. */
    fun totals(ingredients: List<MealIngredientData>): Macros =
        ingredients.fold(Macros.ZERO) { sum, i -> sum + ingredientMacros(i) }

    /** Gesamtgewicht aller Zutaten in Gramm. */
    fun totalGrams(ingredients: List<MealIngredientData>): Double =
        ingredients.sumOf { it.grams }

    /** Werte für die gegessene Menge: Summe proportional auf [eatenGrams] umgerechnet. */
    fun portion(ingredients: List<MealIngredientData>, eatenGrams: Double): Macros {
        val total = totalGrams(ingredients)
        if (total <= 0.0) return Macros.ZERO
        return totals(ingredients) * (eatenGrams / total)
    }

    /** Werte pro 100 g der fertigen Mahlzeit. Null ohne Zutaten. */
    fun per100g(ingredients: List<MealIngredientData>): Macros? {
        val total = totalGrams(ingredients)
        if (total <= 0.0) return null
        return totals(ingredients) * (100.0 / total)
    }
}
