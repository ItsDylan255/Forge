package com.example.fitnesstracker.domain.steps

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object StepFormat {

    private val locale: Locale = Locale.GERMANY
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", locale)

    /** Zahl mit Tausenderpunkt, z. B. "10.000". */
    fun steps(count: Int): String = NumberFormat.getIntegerInstance(locale).format(count)

    fun date(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateFormatter)

    fun percent(percent: Int): String = "$percent %"
}
