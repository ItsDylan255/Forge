package com.example.fitnesstracker.domain.weight

import com.example.fitnesstracker.data.local.WeightEntry

fun WeightEntry.toMeasurement(): WeightMeasurement =
    WeightMeasurement(
        dateEpochDay = dateEpochDay,
        weightKg = weightKg,
        id = id
    )