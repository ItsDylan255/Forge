package com.example.fitnesstracker.domain.weight

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object WeightFormat {

    private val locale: Locale = Locale.GERMANY
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", locale)

    private const val MINUS = "\u2212"
    private const val PLUS_MINUS = "\u00B1"

    /** Text zum Vorausfüllen eines Eingabefelds: exakter Wert mit Komma, z. B. "95,4" oder "95". */
    fun forInput(kg: Double): String {
        if (!kg.isFinite()) return ""
        return BigDecimal(kg.toString())
            .stripTrailingZeros()
            .toPlainString()
            .replace('.', ',')
    }

    /** Anzeige mit einer Nachkommastelle, z. B. "95,4 kg". */
    fun weight(kg: Double): String = String.format(locale, "%.1f kg", kg)

    /** Veränderung mit Vorzeichen, z. B. "+0,4 kg", "−1,2 kg" oder "±0,0 kg". */
    fun delta(deltaKg: Double): String {
        val rounded = Math.round(deltaKg * 10) / 10.0
        return when {
            rounded > 0 -> String.format(locale, "+%.1f kg", rounded)
            rounded < 0 -> String.format(locale, "$MINUS%.1f kg", abs(rounded))
            else -> String.format(locale, "$PLUS_MINUS%.1f kg", 0.0)
        }
    }

    fun date(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(dateFormatter)
}