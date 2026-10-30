package com.example.fitnesstracker.domain.gym

/** Eine beendete Einheit mit Plan und Routine, unabhängig von Room. */
data class PlanSessionRef(val planId: Long?, val routineId: Long?, val routineName: String, val dateEpochDay: Long, val startedAt: Long)

/**
 * Stand eines Trainingsplans.
 * [week] = Kalenderwoche seit der ersten Einheit des Plans (1 = erste Woche),
 * [rounds] = abgeschlossene Durchgänge (jede aktive Routine des Plans mindestens einmal),
 * [doneThisWeek] = verschiedene aktive Routinen des Plans in dieser Woche von [routineCount].
 */
data class PlanProgress(
    val week: Int,
    val rounds: Int,
    val doneThisWeek: Int,
    val routineCount: Int,
    val lastRoutineName: String,
    val lastEpochDay: Long
)

object PlanProgressCalculator {

    /**
     * Fortschritt von [planId] mit den aktiven Routinen [activeRoutineIds]. Null, solange es noch
     * keine Einheit mit diesem Plan gibt. Einheiten gelöschter Routinen zählen für Woche und
     * „zuletzt“, aber nicht für Durchgänge und Wochenstand.
     */
    fun progress(
        planId: Long,
        activeRoutineIds: Set<Long>,
        sessions: List<PlanSessionRef>,
        today: Long
    ): PlanProgress? {
        val own = sessions.filter { it.planId == planId }.sortedBy { it.startedAt }
        val first = own.firstOrNull() ?: return null
        val last = own.last()

        val week = ((GymCalculator.weekStart(today) - GymCalculator.weekStart(first.dateEpochDay)) / 7 + 1)
            .toInt().coerceAtLeast(1)

        // Durchgang: Routinen sammeln, bis jede aktive Routine einmal dabei war
        var rounds = 0
        val inRound = mutableSetOf<Long>()
        if (activeRoutineIds.isNotEmpty()) {
            own.forEach { session ->
                val id = session.routineId ?: return@forEach
                if (id !in activeRoutineIds) return@forEach
                inRound.add(id)
                if (inRound.containsAll(activeRoutineIds)) {
                    rounds++
                    inRound.clear()
                }
            }
        }

        val thisWeek = GymCalculator.weekStart(today)
        val doneThisWeek = own
            .filter { GymCalculator.weekStart(it.dateEpochDay) == thisWeek }
            .mapNotNull { it.routineId }
            .filter { it in activeRoutineIds }
            .toSet()
            .size

        return PlanProgress(
            week = week,
            rounds = rounds,
            doneThisWeek = doneThisWeek,
            routineCount = activeRoutineIds.size,
            lastRoutineName = last.routineName,
            lastEpochDay = last.dateEpochDay
        )
    }

    /** „heute“, „gestern“ oder „vor 3 Tagen“. */
    fun daysAgoText(epochDay: Long, today: Long): String = when (val days = today - epochDay) {
        0L -> "heute"
        1L -> "gestern"
        else -> if (days < 0) "heute" else "vor $days Tagen"
    }
}
