package com.example.fitnesstracker.domain.food

/** Eingabeprüfung für Portionen, Anzahl und Portionen einer Mahlzeit. */
object ServingInputParser {

    const val MAX_PORTIONS = 10
    const val MAX_NAME_LENGTH = 30
    const val MAX_SERVINGS = 100.0

    /** Vorschläge zum Antippen beim Anlegen einer Portion. */
    val suggestions: List<String> = listOf("Stück", "Scheibe", "Portion", "EL", "TL", "Tasse", "Glas")

    private val DECIMAL = Regex("""^(\d+[.,]?\d*|[.,]\d+)$""")

    fun parseName(input: String): FoodParseResult<String> {
        val cleaned = input.trim()
        return when {
            cleaned.isEmpty() -> FoodParseResult.Invalid("Der Name der Portion darf nicht leer sein.")
            cleaned.length > MAX_NAME_LENGTH ->
                FoodParseResult.Invalid("Der Name der Portion darf höchstens $MAX_NAME_LENGTH Zeichen haben.")
            else -> FoodParseResult.Valid(cleaned)
        }
    }

    /** Gramm einer Portion: größer als 0, höchstens 10.000 (wie bei jeder Menge). */
    fun parseGrams(input: String): FoodParseResult<Double> = FoodInputParser.parseGrams(input)

    /** Anzahl der Portionen beim Eintragen: größer als 0, höchstens 100, auch 0,5 oder 1,5. */
    fun parseCount(input: String): FoodParseResult<Double> =
        parsePositive(input, "die Anzahl", "Anzahl", ServingCalculator.MAX_COUNT)

    /** "Ergibt … Portionen" einer Mahlzeit: größer als 0, höchstens 100. */
    fun parseServings(input: String): FoodParseResult<Double> =
        parsePositive(input, "die Zahl der Portionen", "Portionen", MAX_SERVINGS)

    /** Höchstens [MAX_PORTIONS] Portionen, jeder Name nur einmal (ohne Groß-/Kleinschreibung). */
    fun validatePortions(portions: List<PortionData>): FoodParseResult<List<PortionData>> = when {
        portions.size > MAX_PORTIONS ->
            FoodParseResult.Invalid("Höchstens $MAX_PORTIONS Portionen pro Lebensmittel.")
        portions.map { it.name.trim().lowercase() }.distinct().size != portions.size ->
            FoodParseResult.Invalid("Jede Portion braucht einen eigenen Namen.")
        else -> FoodParseResult.Valid(portions)
    }

    private fun parsePositive(
        input: String,
        what: String,
        label: String,
        max: Double
    ): FoodParseResult<Double> {
        val cleaned = input.trim()
        if (cleaned.isEmpty()) return FoodParseResult.Invalid("Bitte gib $what ein.")

        val value = (if (DECIMAL.matches(cleaned)) cleaned.replace(',', '.').toDoubleOrNull() else null)
            ?: return FoodParseResult.Invalid("$label: bitte eine Zahl eingeben, z. B. 2 oder 1,5.")

        return when {
            value <= 0.0 -> FoodParseResult.Invalid("$label muss größer als 0 sein.")
            value > max -> FoodParseResult.Invalid("$label darf höchstens ${max.toInt()} betragen.")
            else -> FoodParseResult.Valid(value)
        }
    }
}
