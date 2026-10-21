package com.example.fitnesstracker.domain.gym

sealed interface GymParseResult<out T> {
    data class Valid<T>(val value: T) : GymParseResult<T>
    data class Invalid(val message: String) : GymParseResult<Nothing>
}

object GymInputParser {

    const val MAX_WEIGHT_KG = 1000.0
    const val MAX_REPS = 1000
    const val MAX_PLANNED_SETS = 20
    const val MAX_NAME_LENGTH = 60

    /** Längste erlaubte Haltezeit: eine Stunde. */
    const val MAX_SECONDS = 3600

    private val DECIMAL = Regex("""^(\d+[.,]?\d*|[.,]\d+)$""")
    private val INTEGER = Regex("""^\d+$""")
    private val KG_SUFFIX = Regex("""(?i)\s*kg$""")
    private val SECONDS_SUFFIX = Regex("""(?i)\s*(sek\.?|s)$""")
    private val CLOCK = Regex("""^(\d+):([0-5]\d)$""")

    /** Gewicht in kg. 0 ist erlaubt (Körpergewichtsübungen). Komma oder Punkt, "kg" optional. */
    fun parseWeight(input: String): GymParseResult<Double> {
        val cleaned = input.trim().replace(KG_SUFFIX, "").trim()
        if (cleaned.isEmpty()) return GymParseResult.Invalid("Bitte gib ein Gewicht ein.")
        if (!DECIMAL.matches(cleaned)) {
            return GymParseResult.Invalid("Bitte gib eine Zahl ein, z. B. 82,5.")
        }
        val value = cleaned.replace(',', '.').toDoubleOrNull()
            ?: return GymParseResult.Invalid("Bitte gib eine Zahl ein, z. B. 82,5.")
        if (value > MAX_WEIGHT_KG) {
            return GymParseResult.Invalid("Das Gewicht darf höchstens ${MAX_WEIGHT_KG.toInt()} kg betragen.")
        }
        return GymParseResult.Valid(value)
    }

    fun parseReps(input: String): GymParseResult<Int> =
        parseInt(input, 1, MAX_REPS, "Wiederholungen")

    fun parsePlannedSets(input: String): GymParseResult<Int> =
        parseInt(input, 1, MAX_PLANNED_SETS, "Sätze")

    /**
     * Wiederholungsbereich aus zwei getrennten Feldern ("Min. Wdh." und "Max. Wdh.").
     * Beide Werte müssen ganze Zahlen von 1 bis 1000 sein, Min darf nicht größer als Max sein.
     * Gleiche Werte (z. B. 10 und 10) sind erlaubt.
     */
    fun parseRepsBounds(minInput: String, maxInput: String): GymParseResult<IntRange> {
        val min = when (val result = parseInt(minInput, 1, MAX_REPS, "Min. Wdh.")) {
            is GymParseResult.Valid -> result.value
            is GymParseResult.Invalid -> return result
        }
        val max = when (val result = parseInt(maxInput, 1, MAX_REPS, "Max. Wdh.")) {
            is GymParseResult.Valid -> result.value
            is GymParseResult.Invalid -> return result
        }

        return if (min > max) {
            GymParseResult.Invalid("Min. Wdh. darf nicht größer als Max. Wdh. sein.")
        } else {
            GymParseResult.Valid(min..max)
        }
    }

    /** Zusatzgewicht oder Unterstützung: leer = 0, sonst wie [parseWeight]. */
    fun parseOptionalWeight(input: String): GymParseResult<Double> =
        if (input.isBlank()) GymParseResult.Valid(0.0) else parseWeight(input)

    /** Sekunden einer Zeitübung: "45", "45 s", "45 Sek" oder "1:30". Ganze Sekunden von 1 bis 3600. */
    fun parseSeconds(input: String): GymParseResult<Int> = parseSecondsLabeled(input, "Sekunden")

    /** Zeitbereich aus zwei Feldern ("Min. Sek." und "Max. Sek."), Min darf nicht größer als Max sein. */
    fun parseSecondsBounds(minInput: String, maxInput: String): GymParseResult<IntRange> {
        val min = when (val result = parseSecondsLabeled(minInput, "Min. Sek.")) {
            is GymParseResult.Valid -> result.value
            is GymParseResult.Invalid -> return result
        }
        val max = when (val result = parseSecondsLabeled(maxInput, "Max. Sek.")) {
            is GymParseResult.Valid -> result.value
            is GymParseResult.Invalid -> return result
        }

        return if (min > max) {
            GymParseResult.Invalid("Min. Sek. darf nicht größer als Max. Sek. sein.")
        } else {
            GymParseResult.Valid(min..max)
        }
    }

    private fun parseSecondsLabeled(input: String, label: String): GymParseResult<Int> {
        val cleaned = input.trim().replace(SECONDS_SUFFIX, "").trim()
        if (cleaned.isEmpty()) return GymParseResult.Invalid("Bitte gib $label ein.")

        val clock = CLOCK.matchEntire(cleaned)
        val value: Int? = when {
            clock != null -> {
                val minutes = clock.groupValues[1].toIntOrNull()
                val seconds = clock.groupValues[2].toInt()
                minutes?.let { it * 60 + seconds }
            }
            INTEGER.matches(cleaned) -> cleaned.toIntOrNull()
            else -> return GymParseResult.Invalid("$label: bitte Sekunden oder m:ss eingeben.")
        }

        return if (value == null || value < 1 || value > MAX_SECONDS) {
            GymParseResult.Invalid("$label: 1 bis $MAX_SECONDS.")
        } else {
            GymParseResult.Valid(value)
        }
    }

    fun parseName(input: String): GymParseResult<String> {
        val cleaned = input.trim()
        return when {
            cleaned.isEmpty() -> GymParseResult.Invalid("Der Name darf nicht leer sein.")
            cleaned.length > MAX_NAME_LENGTH ->
                GymParseResult.Invalid("Der Name darf höchstens $MAX_NAME_LENGTH Zeichen haben.")
            else -> GymParseResult.Valid(cleaned)
        }
    }

    private fun parseInt(input: String, min: Int, max: Int, label: String): GymParseResult<Int> {
        val cleaned = input.trim()
        if (cleaned.isEmpty()) return GymParseResult.Invalid("Bitte gib $label ein.")
        if (!INTEGER.matches(cleaned)) return GymParseResult.Invalid("$label: bitte eine ganze Zahl eingeben.")
        val value = cleaned.toIntOrNull()
        return if (value == null || value < min || value > max) {
            GymParseResult.Invalid("$label: $min bis $max.")
        } else {
            GymParseResult.Valid(value)
        }
    }
}
