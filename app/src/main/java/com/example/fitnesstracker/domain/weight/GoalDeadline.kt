package com.example.fitnesstracker.domain.weight

import com.example.fitnesstracker.i18n.tr

/** Restzeit bis zum Zieldatum. */
object GoalDeadline {

    /** Tage von [today] bis [targetEpochDay]. Null ohne Zieldatum, negativ = abgelaufen. */
    fun daysLeft(targetEpochDay: Long?, today: Long): Long? =
        targetEpochDay?.let { it - today }

    /** Anzeige: "12 Tage", "1 Tag", "Heute" oder "Abgelaufen". Null ohne Zieldatum. */
    fun text(daysLeft: Long?): String? = when {
        daysLeft == null -> null
        daysLeft > 1L -> tr("$daysLeft Tage", "$daysLeft days")
        daysLeft == 1L -> tr("1 Tag", "1 day")
        daysLeft == 0L -> tr("Heute", "Today")
        else -> tr("Abgelaufen", "Expired")
    }
}
