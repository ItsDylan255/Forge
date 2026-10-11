package com.example.fitnesstracker.domain.steps

import com.example.fitnesstracker.data.local.StepEntry

/** Ein Tageswert, unabhängig von Room. Alle Berechnungen im Paket arbeiten nur damit. */
data class StepDay(
    val dateEpochDay: Long,
    val steps: Int
)

fun StepEntry.toStepDay(): StepDay = StepDay(dateEpochDay = dateEpochDay, steps = steps)
