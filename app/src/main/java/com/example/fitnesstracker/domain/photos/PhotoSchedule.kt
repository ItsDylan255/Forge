package com.example.fitnesstracker.domain.photos

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Wie oft ein Fortschrittsfoto fällig ist. */
sealed interface PhotoSchedule {
    data object Off : PhotoSchedule

    /** Alle [days] Tage nach dem letzten Foto. */
    data class EveryDays(val days: Int) : PhotoSchedule

    /** Jede Woche an [day]. */
    data class Weekly(val day: DayOfWeek) : PhotoSchedule

    /** Am ersten [day] jedes Monats, z. B. am ersten Sonntag. */
    data class MonthlyFirst(val day: DayOfWeek) : PhotoSchedule

    companion object {
        const val MIN_DAYS = 1
        const val MAX_DAYS = 90
    }
}

/** Nächster Foto-Termin und ob er schon verstrichen ist. */
data class PhotoDue(
    val dueEpochDay: Long,
    val today: Long
) {
    val isToday: Boolean get() = dueEpochDay == today
    val isOverdue: Boolean get() = today > dueEpochDay
    val daysOverdue: Long get() = (today - dueEpochDay).coerceAtLeast(0)
}

object PhotoReminderCalculator {

    /**
     * Nächster Termin für ein Foto.
     *
     * [anchorEpochDay] ist der Tag, an dem der Rhythmus eingestellt wurde: Termine davor zählen nicht,
     * sonst wäre nach dem Einstellen sofort alles überfällig. Ohne bisheriges Foto ist der erste Termin
     * der erste passende Tag ab dem Einstellen.
     */
    fun nextDue(schedule: PhotoSchedule, lastPhotoEpochDay: Long?, anchorEpochDay: Long): Long? = when (schedule) {
        PhotoSchedule.Off -> null

        is PhotoSchedule.EveryDays ->
            if (lastPhotoEpochDay == null) anchorEpochDay
            else maxOf(lastPhotoEpochDay + schedule.days.coerceAtLeast(1), anchorEpochDay)

        is PhotoSchedule.Weekly -> {
            val from = LocalDate.ofEpochDay(earliest(lastPhotoEpochDay, anchorEpochDay))
            from.with(TemporalAdjusters.nextOrSame(schedule.day)).toEpochDay()
        }

        is PhotoSchedule.MonthlyFirst -> {
            val from = LocalDate.ofEpochDay(earliest(lastPhotoEpochDay, anchorEpochDay))
            val thisMonth = from.with(TemporalAdjusters.firstInMonth(schedule.day))
            val due = if (thisMonth.isBefore(from)) {
                from.plusMonths(1).with(TemporalAdjusters.firstInMonth(schedule.day))
            } else {
                thisMonth
            }
            due.toEpochDay()
        }
    }

    fun status(schedule: PhotoSchedule, lastPhotoEpochDay: Long?, anchorEpochDay: Long, today: Long): PhotoDue? =
        nextDue(schedule, lastPhotoEpochDay, anchorEpochDay)?.let { PhotoDue(it, today) }

    /** Erinnern, sobald der Termin da ist, und danach jeden Tag, bis ein Foto gemacht wurde. */
    fun shouldRemind(due: PhotoDue?): Boolean = due != null && due.today >= due.dueEpochDay

    // Frühester Tag, der als Termin in Frage kommt: nach dem letzten Foto und nicht vor dem Einstellen
    private fun earliest(lastPhotoEpochDay: Long?, anchorEpochDay: Long): Long =
        maxOf(lastPhotoEpochDay?.plus(1) ?: anchorEpochDay, anchorEpochDay)
}
