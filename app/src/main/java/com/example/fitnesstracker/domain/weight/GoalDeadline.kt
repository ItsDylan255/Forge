package com.example.fitnesstracker.domain.weight

/** Restzeit bis zum Zieldatum. */
object GoalDeadline {

    /** Tage von [today] bis [targetEpochDay]. Null ohne Zieldatum, negativ = abgelaufen. */
    fun daysLeft(targetEpochDay: Long?, today: Long): Long? =
        targetEpochDay?.let { it - today }

    /** Anzeige: "12 Tage", "1 Tag", "Heute" oder "Abgelaufen". Null ohne Zieldatum. */
    fun text(daysLeft: Long?): String? = when {
        daysLeft == null -> null
        daysLeft > 1L -> "$daysLeft Tage"
        daysLeft == 1L -> "1 Tag"
        daysLeft == 0L -> "Heute"
        else -> "Abgelaufen"
    }
}
