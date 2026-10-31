package com.example.fitnesstracker.domain.food

import com.example.fitnesstracker.i18n.tr

/** Eingabeprüfung für Portionen, Anzahl und Portionen einer Mahlzeit. */
object ServingInputParser {

    const val MAX_PORTIONS = 10
    const val MAX_NAME_LENGTH = 30
    const val MAX_SERVINGS = 100.0

    /** Vorschläge zum Antippen beim Anlegen einer Portion. */
    val suggestions: List<String> get() = listOf(tr("Stück", "Piece"), tr("Scheibe", "Slice"), tr("Portion", "Serving"), tr("EL", "tbsp"), tr("TL", "tsp"), tr("Tasse", "Cup"), tr("Glas", "Glass"))

    private val DECIMAL = Regex("""^(\d+[.,]?\d*|[.,]\d+)$""")

    fun parseName(input: String): FoodParseResult<String> {
        val cleaned = input.trim()
        return when {
            cleaned.isEmpty() -> FoodParseResult.Invalid(tr("Der Name der Portion darf nicht leer sein.", "The serving name can't be empty."))
            cleaned.length > MAX_NAME_LENGTH ->
                FoodParseResult.Invalid(tr("Der Name der Portion darf höchstens $MAX_NAME_LENGTH Zeichen haben.", "The serving name can have at most $MAX_NAME_LENGTH characters."))
            else -> FoodParseResult.Valid(cleaned)
        }
    }

    /** Gramm einer Portion: größer als 0, höchstens 10.000 (wie bei jeder Menge). */
    fun parseGrams(input: String): FoodParseResult<Double> = FoodInputParser.parseGrams(input)

    /** Anzahl der Portionen beim Eintragen: größer als 0, höchstens 100, auch 0,5 oder 1,5. */
    fun parseCount(input: String): FoodParseResult<Double> =
        parsePositive(input, tr("die Anzahl", "the amount"), tr("Anzahl", "Amount"), ServingCalculator.MAX_COUNT)

    /** "Ergibt … Portionen" einer Mahlzeit: größer als 0, höchstens 100. */
    fun parseServings(input: String): FoodParseResult<Double> =
        parsePositive(input, tr("die Zahl der Portionen", "the number of servings"), tr("Portionen", "Servings"), MAX_SERVINGS)

    /** Höchstens [MAX_PORTIONS] Portionen, jeder Name nur einmal (ohne Groß-/Kleinschreibung). */
    fun validatePortions(portions: List<PortionData>): FoodParseResult<List<PortionData>> = when {
        portions.size > MAX_PORTIONS ->
            FoodParseResult.Invalid(tr("Höchstens $MAX_PORTIONS Portionen pro Lebensmittel.", "At most $MAX_PORTIONS servings per food."))
        portions.map { it.name.trim().lowercase() }.distinct().size != portions.size ->
            FoodParseResult.Invalid(tr("Jede Portion braucht einen eigenen Namen.", "Each serving needs its own name."))
        else -> FoodParseResult.Valid(portions)
    }

    private fun parsePositive(
        input: String,
        what: String,
        label: String,
        max: Double
    ): FoodParseResult<Double> {
        val cleaned = input.trim()
        if (cleaned.isEmpty()) return FoodParseResult.Invalid(tr("Bitte gib $what ein.", "Please enter $what."))

        val value = (if (DECIMAL.matches(cleaned)) cleaned.replace(',', '.').toDoubleOrNull() else null)
            ?: return FoodParseResult.Invalid(tr("$label: bitte eine Zahl eingeben, z. B. 2 oder 1,5.", "$label: please enter a number, e.g. 2 or 1.5."))

        return when {
            value <= 0.0 -> FoodParseResult.Invalid(tr("$label muss größer als 0 sein.", "$label must be greater than 0."))
            value > max -> FoodParseResult.Invalid(tr("$label darf höchstens ${max.toInt()} betragen.", "$label can be at most ${max.toInt()}."))
            else -> FoodParseResult.Valid(value)
        }
    }
}
