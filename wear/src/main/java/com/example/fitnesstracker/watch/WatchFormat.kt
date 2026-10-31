package com.example.fitnesstracker.watch

import java.text.NumberFormat

object WatchFormat {

    private val locale get() = WatchLang.locale

    /** 8432 -> "8.432" */
    fun steps(steps: Int): String = NumberFormat.getIntegerInstance(locale).format(steps)

    /** 80.4 -> "80,4 kg" (Englisch: "80.4 kg"), ganze Werte ohne Nachkomma. */
    fun kg(weightKg: Double): String =
        String.format(locale, "%.1f kg", weightKg).replace(",0 kg", " kg").replace(".0 kg", " kg")

    /** Laufende Uhr: "05:32" oder "1:05:32". */
    fun timer(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format(locale, "%d:%02d:%02d", hours, minutes, seconds)
        else String.format(locale, "%02d:%02d", minutes, seconds)
    }

    /** Restzeit einer Pause, aufgerundet: "1:30", "0:05". */
    fun countdown(millis: Long): String {
        val seconds = ((millis.coerceAtLeast(0L) + 999L) / 1000L)
        return String.format(locale, "%d:%02d", seconds / 60, seconds % 60)
    }
}
