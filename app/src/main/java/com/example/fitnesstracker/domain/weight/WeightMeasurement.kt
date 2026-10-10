package com.example.fitnesstracker.domain.weight

/**
 * Eine einzelne Messung, unabhängig von Room.
 * Die Berechnungen im Paket "domain.weight" arbeiten nur mit dieser Klasse.
 */
data class WeightMeasurement(
    val dateEpochDay: Long,
    val weightKg: Double,
    val id: Long = 0L
)