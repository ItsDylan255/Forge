package com.example.fitnesstracker.domain.gym

/**
 * Art einer Übung. [code] steht in der Datenbank.
 * REPS = Gewicht × Wiederholungen, TIME = Sekunden (z. B. Plank), ASSISTED = Gewicht ist die Unterstützung.
 */
enum class ExerciseKind(val code: Int) {
    REPS(0),
    TIME(1),
    ASSISTED(2);

    companion object {
        /** Unbekannte Codes gelten als REPS. */
        fun fromCode(code: Int): ExerciseKind = entries.firstOrNull { it.code == code } ?: REPS
    }
}
