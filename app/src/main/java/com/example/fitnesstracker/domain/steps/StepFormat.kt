package com.example.fitnesstracker.domain.steps

import com.example.fitnesstracker.i18n.L10n
import com.example.fitnesstracker.i18n.tr
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object StepFormat {

    private val locale: Locale get() = L10n.locale
    private val dateFormatter get() = DateTimeFormatter.ofPattern(tr("dd.MM.yyyy", "dd/MM/yyyy"), locale)

    /** Zahl mit Tausenderpunkt, z. B. "10.000". */
    fun steps(count: Int): String = NumberFormat.getIntegerInstance(locale).format(count)

    fun date(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateFormatter)

    fun percent(percent: Int): String = "$percent %"
}
