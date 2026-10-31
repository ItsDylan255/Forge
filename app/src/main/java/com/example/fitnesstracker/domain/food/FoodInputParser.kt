package com.example.fitnesstracker.domain.food

import com.example.fitnesstracker.i18n.tr

sealed interface FoodParseResult<out T> {
    data class Valid<T>(val value: T) : FoodParseResult<T>
    data class Invalid(val message: String) : FoodParseResult<Nothing>
}

object FoodInputParser {

    const val MAX_KCAL_PER_100G = 1000.0
    const val MAX_MACRO_PER_100G = 100.0
    const val MAX_GRAMS = 10_000.0
    const val MAX_NAME_LENGTH = 80

    private val DECIMAL = Regex("""^(\d+[.,]?\d*|[.,]\d+)$""")

    fun parseName(input: String): FoodParseResult<String> {
        val cleaned = input.trim()
        return when {
            cleaned.isEmpty() -> FoodParseResult.Invalid(tr("Der Name darf nicht leer sein.", "The name can't be empty."))
            cleaned.length > MAX_NAME_LENGTH ->
                FoodParseResult.Invalid(tr("Der Name darf h\u00F6chstens $MAX_NAME_LENGTH Zeichen haben.", "The name can have at most $MAX_NAME_LENGTH characters."))
            else -> FoodParseResult.Valid(cleaned)
        }
    }

    /** Kalorien pro 100 g: Pflichtfeld, 0 bis 1000. */
    fun parseKcal(input: String): FoodParseResult<Double> =
        parseNumber(input, tr("Kalorien", "Calories"), MAX_KCAL_PER_100G, emptyMeansZero = false)

    /** Protein, Kohlenhydrate oder Fett pro 100 g: leer = 0, sonst 0 bis 100. */
    fun parseMacro(input: String, label: String): FoodParseResult<Double> =
        parseNumber(input, label, MAX_MACRO_PER_100G, emptyMeansZero = true)

    /** Gramm einer Mahlzeit: groesser als 0, hoechstens 10.000. */
    fun parseGrams(input: String): FoodParseResult<Double> {
        val cleaned = input.trim().replace(Regex("""(?i)\s*g$"""), "").trim()
        if (cleaned.isEmpty()) return FoodParseResult.Invalid(tr("Bitte gib die Menge in Gramm ein.", "Please enter the amount in grams."))
        if (!DECIMAL.matches(cleaned)) {
            return FoodParseResult.Invalid(tr("Bitte gib eine Zahl ein, z. B. 200.", "Please enter a number, e.g. 200."))
        }

        val value = cleaned.replace(',', '.').toDoubleOrNull()
            ?: return FoodParseResult.Invalid(tr("Bitte gib eine Zahl ein, z. B. 200.", "Please enter a number, e.g. 200."))

        return when {
            value <= 0.0 -> FoodParseResult.Invalid(tr("Die Menge muss gr\u00F6\u00DFer als 0 sein.", "The amount must be greater than 0."))
            value > MAX_GRAMS -> FoodParseResult.Invalid(tr("Die Menge darf h\u00F6chstens 10.000 g betragen.", "The amount can be at most 10,000 g."))
            else -> FoodParseResult.Valid(value)
        }
    }

    private fun parseNumber(
        input: String,
        label: String,
        max: Double,
        emptyMeansZero: Boolean
    ): FoodParseResult<Double> {
        val cleaned = input.trim()
        if (cleaned.isEmpty()) {
            return if (emptyMeansZero) FoodParseResult.Valid(0.0)
            else FoodParseResult.Invalid(tr("Bitte gib $label ein.", "$label is required."))
        }
        if (!DECIMAL.matches(cleaned)) {
            return FoodParseResult.Invalid(tr("$label: bitte eine Zahl von 0 bis ${max.toInt()} eingeben.", "$label: please enter a number from 0 to ${max.toInt()}."))
        }

        val value = cleaned.replace(',', '.').toDoubleOrNull()
        return if (value == null || value > max) {
            FoodParseResult.Invalid(tr("$label: bitte eine Zahl von 0 bis ${max.toInt()} eingeben.", "$label: please enter a number from 0 to ${max.toInt()}."))
        } else {
            FoodParseResult.Valid(value)
        }
    }
}
