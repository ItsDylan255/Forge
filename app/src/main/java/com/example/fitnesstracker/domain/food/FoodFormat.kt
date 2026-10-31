package com.example.fitnesstracker.domain.food

import com.example.fitnesstracker.i18n.L10n
import com.example.fitnesstracker.i18n.tr
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object FoodFormat {

    private val locale: Locale get() = L10n.locale
    private val dateFormatter get() = DateTimeFormatter.ofPattern(tr("EEE, dd.MM.yyyy", "EEE, dd/MM/yyyy"), locale)
    private val shortFormatter get() = DateTimeFormatter.ofPattern(tr("dd.MM.", "dd/MM"), locale)

    /** Ganze Zahl mit Tausenderpunkt: 2150 -> "2.150". */
    fun int(value: Double): String =
        NumberFormat.getIntegerInstance(locale).format(Math.round(value))

    fun kcal(value: Double): String = "${int(value)} kcal"

    /** Zahl ohne unnoetige Nachkommastellen: 200 -> "200", 12.5 -> "12,5". Auch zum Vorausfuellen. */
    fun number(value: Double): String {
        if (!value.isFinite()) return ""
        return BigDecimal(value.toString())
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
            .let { if (L10n.isEnglish) it else it.replace('.', ',') }
    }

    fun grams(value: Double): String = "${number(value)} g"

    /** Makros mit einer Nachkommastelle: "12,5 g". */
    fun macro(value: Double): String = String.format(locale, "%.1f g", value)

    fun percent(percent: Int): String = "$percent %"

    fun shortDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(shortFormatter)

    /** "Heute", "Gestern", "Morgen" oder z. B. "Mo., 12.10.2026". */
    fun dayLabel(epochDay: Long, today: Long): String = when (epochDay - today) {
        0L -> tr("Heute", "Today")
        -1L -> tr("Gestern", "Yesterday")
        1L -> tr("Morgen", "Tomorrow")
        else -> LocalDate.ofEpochDay(epochDay).format(dateFormatter)
    }
}
