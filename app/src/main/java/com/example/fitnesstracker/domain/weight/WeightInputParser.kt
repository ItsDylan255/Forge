package com.example.fitnesstracker.domain.weight

enum class WeightInputError(val message: String) {
    EMPTY("Bitte gib ein Gewicht ein."),
    NOT_A_NUMBER("Bitte gib eine Zahl ein, z. B. 95,4."),
    NOT_POSITIVE("Das Gewicht muss größer als 0 sein.")
}

sealed interface WeightParseResult {

    /**
     * [isUnusual] = true bedeutet nur: Der Wert liegt außerhalb des üblichen
     * Bereichs. Er ist trotzdem gültig und darf nach Rückfrage gespeichert werden.
     */
    data class Valid(
        val weightKg: Double,
        val isUnusual: Boolean
    ) : WeightParseResult

    data class Invalid(val error: WeightInputError) : WeightParseResult
}

object WeightInputParser {

    // Nur Hinweisgrenzen für eine Rückfrage (z. B. Tippfehler wie 954 statt 95,4).
    // Sie verbieten nichts.
    const val UNUSUAL_BELOW_KG = 20.0
    const val UNUSUAL_ABOVE_KG = 400.0

    // Ziffern mit höchstens einem Dezimaltrenner (Komma oder Punkt).
    // Bewusst streng, damit z. B. "NaN", "1e3", "95d" oder "1.234,5" nicht durchrutschen.
    private val NUMBER = Regex("""^(\d+[.,]?\d*|[.,]\d+)$""")
    private val KG_SUFFIX = Regex("""(?i)\s*kg$""")

    fun parse(input: String): WeightParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return WeightParseResult.Invalid(WeightInputError.EMPTY)
        }

        val cleaned = trimmed.replace(KG_SUFFIX, "").trim()
        val isNegative = cleaned.startsWith("-")
        val unsigned = if (isNegative) cleaned.removePrefix("-").trim() else cleaned

        if (!NUMBER.matches(unsigned)) {
            return WeightParseResult.Invalid(WeightInputError.NOT_A_NUMBER)
        }

        val value = unsigned.replace(',', '.').toDouble()

        return when {
            !value.isFinite() ->
                WeightParseResult.Invalid(WeightInputError.NOT_A_NUMBER)

            isNegative || value <= 0.0 ->
                WeightParseResult.Invalid(WeightInputError.NOT_POSITIVE)

            else -> WeightParseResult.Valid(
                weightKg = value,
                isUnusual = value < UNUSUAL_BELOW_KG || value > UNUSUAL_ABOVE_KG
            )
        }
    }
}