package com.example.fitnesstracker.domain.steps

enum class StepGoalError(val message: String) {
    EMPTY("Bitte gib ein Schrittziel ein."),
    NOT_A_NUMBER("Bitte gib eine ganze Zahl ein, z. B. 10000."),
    TOO_LOW("Das Ziel muss mindestens ${StepGoalParser.MIN_GOAL} Schritte betragen."),
    TOO_HIGH("Das Ziel darf höchstens ${StepGoalParser.MAX_GOAL} Schritte betragen.")
}

sealed interface StepGoalParseResult {
    data class Valid(val goal: Int) : StepGoalParseResult
    data class Invalid(val error: StepGoalError) : StepGoalParseResult
}

object StepGoalParser {

    const val MIN_GOAL = 100
    const val MAX_GOAL = 100_000

    private val DIGITS = Regex("""^\d+$""")

    /** Erlaubt auch "10.000" oder "10 000" (Tausendertrenner werden entfernt). */
    fun parse(input: String): StepGoalParseResult {
        val cleaned = input.trim()
            .replace(".", "")
            .replace(" ", "")
            .replace("\u00A0", "")

        if (cleaned.isEmpty()) return StepGoalParseResult.Invalid(StepGoalError.EMPTY)
        if (!DIGITS.matches(cleaned)) return StepGoalParseResult.Invalid(StepGoalError.NOT_A_NUMBER)

        val value = cleaned.toLongOrNull() ?: return StepGoalParseResult.Invalid(StepGoalError.TOO_HIGH)

        return when {
            value < MIN_GOAL -> StepGoalParseResult.Invalid(StepGoalError.TOO_LOW)
            value > MAX_GOAL -> StepGoalParseResult.Invalid(StepGoalError.TOO_HIGH)
            else -> StepGoalParseResult.Valid(value.toInt())
        }
    }
}
