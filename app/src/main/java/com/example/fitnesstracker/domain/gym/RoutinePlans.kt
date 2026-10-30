package com.example.fitnesstracker.domain.gym

/**
 * Room-unabhängige Sicht auf eine Routine. [planId] null = ohne Plan,
 * [planPosition] = Reihenfolge im Plan, [archived] = selbst archiviert.
 */
data class RoutineRef(
    val id: Long,
    val name: String,
    val planId: Long? = null,
    val planPosition: Int = 0,
    val archived: Boolean = false
)

/** Room-unabhängige Sicht auf einen Trainingsplan. */
data class PlanRef(val id: Long, val name: String, val archived: Boolean = false)

/** Ein Abschnitt der Routinenliste. [plan] null = Routinen ohne Plan. */
data class PlanGroup(val plan: PlanRef?, val routines: List<RoutineRef>)

/** Inhalt des Archivs: archivierte Pläne (mit allen Routinen) und einzeln archivierte Routinen. */
data class RoutineArchive(val plans: List<PlanGroup>, val routines: List<RoutineRef>) {
    val isEmpty: Boolean get() = plans.isEmpty() && routines.isEmpty()
    val count: Int get() = plans.size + routines.size
}

object RoutinePlans {

    /** Reihenfolge innerhalb eines Plans: Position, dann Name. */
    val planOrder: Comparator<RoutineRef> =
        compareBy<RoutineRef>({ it.planPosition }, { it.name.lowercase() })

    private val nameOrder: Comparator<RoutineRef> = compareBy { it.name.lowercase() }

    /**
     * Aktiv ist eine Routine, wenn weder sie noch ihr Plan archiviert ist.
     * Der Plan-Status wird nicht auf die Routinen kopiert, damit „Plan wiederherstellen“
     * den vorherigen Zustand jeder Routine behält.
     */
    fun activeRoutines(routines: List<RoutineRef>, plans: List<PlanRef>): List<RoutineRef> {
        val archivedPlans = plans.filter { it.archived }.map { it.id }.toSet()
        return routines.filter { !it.archived && it.planId !in archivedPlans }
    }

    /**
     * Aktive Pläne (nach Name, auch leere) mit ihren aktiven Routinen, danach „ohne Plan“
     * (nur wenn es dort Routinen gibt). Routinen mit unbekanntem Plan landen bei „ohne Plan“.
     */
    fun groups(routines: List<RoutineRef>, plans: List<PlanRef>): List<PlanGroup> {
        val active = activeRoutines(routines, plans)
        val activePlans = plans.filter { !it.archived }.sortedBy { it.name.lowercase() }
        val activePlanIds = activePlans.map { it.id }.toSet()

        val planGroups = activePlans.map { plan ->
            PlanGroup(plan, active.filter { it.planId == plan.id }.sortedWith(planOrder))
        }
        val loose = active.filter { it.planId !in activePlanIds }.sortedWith(nameOrder)

        return if (loose.isEmpty()) planGroups else planGroups + PlanGroup(null, loose)
    }

    /** Archivierte Pläne mit allen ihren Routinen, dazu archivierte Routinen aktiver oder keiner Pläne. */
    fun archive(routines: List<RoutineRef>, plans: List<PlanRef>): RoutineArchive {
        val archivedPlans = plans.filter { it.archived }.sortedBy { it.name.lowercase() }
        val archivedPlanIds = archivedPlans.map { it.id }.toSet()

        return RoutineArchive(
            plans = archivedPlans.map { plan ->
                PlanGroup(plan, routines.filter { it.planId == plan.id }.sortedWith(planOrder))
            },
            routines = routines
                .filter { it.archived && it.planId !in archivedPlanIds }
                .sortedWith(nameOrder)
        )
    }

    /**
     * Neue Reihenfolge (Ids) eines Plans, nachdem [routineId] um eins nach oben oder unten
     * verschoben wurde. Am Rand bleibt alles wie es ist. Doppelte Positionen werden dabei aufgelöst.
     */
    fun moved(inPlan: List<RoutineRef>, routineId: Long, up: Boolean): List<Long> {
        val ids = inPlan.sortedWith(planOrder).map { it.id }.toMutableList()
        val index = ids.indexOf(routineId)
        val target = if (up) index - 1 else index + 1
        if (index >= 0 && target in ids.indices) {
            ids[index] = ids[target].also { ids[target] = routineId }
        }
        return ids
    }
}
