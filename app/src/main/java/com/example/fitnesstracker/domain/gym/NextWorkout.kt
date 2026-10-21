package com.example.fitnesstracker.domain.gym

/** Room-unabhängige Sicht auf eine Routine. */
data class RoutineRef(val id: Long, val name: String)

/** Eine beendete Einheit mit Routine (routineId null = freies Workout). */
data class SessionRef(val routineId: Long?, val dateEpochDay: Long, val startedAt: Long)

/** Vorschlag für das nächste Training. [lastEpochDay] null = noch nie trainiert. */
data class NextWorkout(val routine: RoutineRef, val lastEpochDay: Long?)

object NextWorkoutPicker {

    /**
     * Die Routine, die am längsten nicht trainiert wurde. Noch nie trainierte Routinen kommen zuerst.
     * Bei Gleichstand entscheidet der Name (ohne Groß-/Kleinschreibung). Null ohne Routinen.
     */
    fun pick(routines: List<RoutineRef>, sessions: List<SessionRef>): NextWorkout? {
        if (routines.isEmpty()) return null

        val lastByRoutine = sessions
            .filter { it.routineId != null }
            .groupBy { it.routineId!! }
            .mapValues { (_, list) -> list.maxBy { it.startedAt } }

        val best = routines.minWith(
            compareBy<RoutineRef>(
                { lastByRoutine[it.id]?.startedAt ?: Long.MIN_VALUE },
                { it.name.lowercase() }
            )
        )
        return NextWorkout(best, lastByRoutine[best.id]?.dateEpochDay)
    }
}
