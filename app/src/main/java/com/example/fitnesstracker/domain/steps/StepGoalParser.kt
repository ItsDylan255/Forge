package com.example.fitnesstracker.domain.steps

import com.example.fitnesstracker.i18n.tr

enum class StepGoalError(private val de: String, private val en: String) {
    EMPTY("Bitte gib ein Schrittziel ein.", "Please enter a step goal."),
    NOT_A_NUMBER("Bitte gib eine ganze Zahl ein, z. B. 10000.", "Please enter a whole number, e.g. 10000."),
    TOO_LOW("Das Ziel muss mindestens ${StepGoalParser.MIN_GOAL} Schritte betragen.", "The goal must be at least ${StepGoalParser.MIN_GOAL} steps."),
    TOO_HIGH("Das Ziel darf höchstens ${StepGoalParser.MAX_GOAL} Schritte betragen.", "The goal can be at most ${StepGoalParser.MAX_GOAL} steps.");

    /** Text in der aktuellen Sprache. */
    val message: String get() = tr(de, en)
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
