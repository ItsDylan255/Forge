package com.example.fitnesstracker.domain.steps

/** Ein Eintrag im Mess-Protokoll der Schritte. [source] zum Beispiel "App", "Hintergrund", "Nacht". */
data class StepReadEntry(val atMillis: Long, val source: String, val text: String)

/**
 * Kleines Protokoll der letzten Schritt-Messungen, als Text gespeichert (eine Zeile pro Eintrag).
 * Es zeigt in den Einstellungen, wann gemessen wurde und warum eine Messung nichts brachte.
 */
object StepReadLog {

    const val MAX_ENTRIES = 30
    const val APP_SOURCE = "App"
    const val SERVICE_SOURCE = "Dienst"

    private val THROTTLED_SOURCES = setOf(APP_SOURCE, SERVICE_SOURCE)

    /** App (offener Screen) und Dienst messen ständig, im Protokoll steht davon nur alle 10 Minuten eine Zeile. */
    const val APP_LOG_INTERVAL_MILLIS = 10 * 60 * 1000L

    fun shouldLog(log: String, source: String, nowMillis: Long): Boolean =
        source !in THROTTLED_SOURCES ||
                entries(log).none { it.source == source && nowMillis - it.atMillis < APP_LOG_INTERVAL_MILLIS }

    /** Hängt einen Eintrag an und behält nur die letzten [MAX_ENTRIES]. */
    fun append(log: String, atMillis: Long, source: String, text: String): String {
        val line = "$atMillis|${clean(source)}|${clean(text)}"
        return (log.lines().filter { it.isNotBlank() } + line).takeLast(MAX_ENTRIES).joinToString("\n")
    }

    /** Alle lesbaren Einträge, der älteste zuerst. Kaputte Zeilen werden übersprungen. */
    fun entries(log: String): List<StepReadEntry> =
        log.lines().mapNotNull { line ->
            val parts = line.split("|", limit = 3)
            val at = parts.getOrNull(0)?.toLongOrNull()
            if (parts.size == 3 && at != null) StepReadEntry(at, parts[1], parts[2]) else null
        }

    private fun clean(value: String): String = value.replace('|', ' ').replace('\n', ' ').trim()
}
