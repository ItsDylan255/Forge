package com.example.fitnesstracker.domain.gym

import com.example.fitnesstracker.i18n.L10n

/**
 * Auswahl für die optionale Muskelgruppe einer Übung.
 * Es gibt keine vorgefertigten Übungen mehr: Alle Übungen legt der Nutzer selbst an.
 * Gespeichert wird immer der deutsche Name, angezeigt wird er über [label] in der App-Sprache.
 */
object MuscleGroups {
    val all = listOf("Brust", "Rücken", "Beine", "Schultern", "Bizeps", "Trizeps", "Bauch", "Ganzkörper")

    private val english = mapOf(
        "Brust" to "Chest",
        "Rücken" to "Back",
        "Beine" to "Legs",
        "Schultern" to "Shoulders",
        "Bizeps" to "Biceps",
        "Trizeps" to "Triceps",
        "Bauch" to "Abs",
        "Ganzkörper" to "Full body"
    )

    fun label(group: String): String = if (L10n.isEnglish) english[group] ?: group else group
}
