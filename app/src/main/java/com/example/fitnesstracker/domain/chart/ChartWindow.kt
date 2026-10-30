package com.example.fitnesstracker.domain.chart

/**
 * Verschiebbares Zeitfenster eines Diagramms. Das Fenster reicht von `end - span` bis `end`
 * (beide Tage inklusive) und lässt sich nur so weit verschieben, dass es nicht vor dem ersten
 * und nicht nach dem letzten Eintrag liegt.
 */
object ChartWindow {

    /**
     * Klemmt [end] so, dass der Fensteranfang nicht vor [firstDay] und das Ende nicht nach [lastDay] liegt.
     * Ist der Datenbereich kürzer als das Fenster, bleibt nur [lastDay] (kein Verschieben möglich).
     */
    fun clampEnd(end: Long, span: Int, firstDay: Long, lastDay: Long): Long {
        val minEnd = minOf(firstDay + span, lastDay)
        return end.coerceIn(minEnd, lastDay)
    }

    /** Ob sich das Fenster überhaupt verschieben lässt. */
    fun canPan(span: Int, firstDay: Long, lastDay: Long): Boolean = firstDay + span < lastDay
}
