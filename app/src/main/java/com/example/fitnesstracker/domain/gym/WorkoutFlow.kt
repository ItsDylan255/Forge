package com.example.fitnesstracker.domain.gym

/** Seite eines Satzes. BOTH = normale Übung, RIGHT/LEFT = einseitige Übung. [code] steht in der Datenbank. */
enum class SetSide(val code: Int) {
    BOTH(0),
    RIGHT(1),
    LEFT(2);

    companion object {
        /** Unbekannte Codes gelten als BOTH. */
        fun fromCode(code: Int): SetSide = entries.firstOrNull { it.code == code } ?: BOTH
    }
}

/** Eine Übung der Routine mit allem, was für den Ablauf zählt. */
data class PlanItem(
    val exerciseId: Long,
    val plannedSets: Int,
    val restSeconds: Int,
    /** Jeder Satz besteht aus rechts und links. */
    val unilateral: Boolean = false,
    /** Diese Übung bildet mit der nächsten ein Superset. */
    val supersetWithNext: Boolean = false
)

/**
 * Ein einzelner Eintrag im Ablauf. [setNumber] zählt pro Übung und Seite.
 * [restAfterSeconds] > 0 nur am Ende einer Runde, 0 = direkt weiter ohne Pause.
 */
data class WorkoutSlot(
    val exerciseId: Long,
    val setNumber: Int,
    val side: SetSide,
    val restAfterSeconds: Int
)

/** Ein bereits geloggter Satz (nur das, was für den Ablauf nötig ist). */
data class LoggedSet(val exerciseId: Long, val side: SetSide)

/** Reiner Ablauf eines Trainings: Gruppen, Reihenfolge der Sätze und nächster offener Satz. */
object WorkoutFlow {

    /**
     * Teilt die Routine in Seiten: aufeinanderfolgende Übungen mit supersetWithNext bilden eine Gruppe.
     * Das Häkchen an der letzten Übung der Liste wird ignoriert.
     */
    fun groups(items: List<PlanItem>): List<List<PlanItem>> {
        val result = mutableListOf<List<PlanItem>>()
        var current = mutableListOf<PlanItem>()

        items.forEachIndexed { index, item ->
            current.add(item)
            val continues = item.supersetWithNext && index < items.lastIndex
            if (!continues) {
                result.add(current)
                current = mutableListOf()
            }
        }
        return result
    }

    /**
     * Reihenfolge aller Sätze einer Gruppe, Runde für Runde: A1, B1, A2, B2 ...
     * Einseitige Übungen liefern pro Satz erst rechts, dann links. Die Pause kommt nur am Ende
     * einer Runde und ist die der letzten Übung dieser Runde. Hat eine Übung weniger Sätze,
     * fällt sie in den späteren Runden weg.
     */
    fun slots(group: List<PlanItem>): List<WorkoutSlot> {
        val rounds = group.maxOfOrNull { it.plannedSets } ?: 0
        val result = mutableListOf<WorkoutSlot>()

        for (round in 1..rounds) {
            val inRound = group.filter { round <= it.plannedSets }
            inRound.forEachIndexed { itemIndex, item ->
                val sides = if (item.unilateral) listOf(SetSide.RIGHT, SetSide.LEFT) else listOf(SetSide.BOTH)
                sides.forEachIndexed { sideIndex, side ->
                    val endOfRound = itemIndex == inRound.lastIndex && sideIndex == sides.lastIndex
                    result.add(
                        WorkoutSlot(
                            exerciseId = item.exerciseId,
                            setNumber = round,
                            side = side,
                            restAfterSeconds = if (endOfRound) item.restSeconds else 0
                        )
                    )
                }
            }
        }
        return result
    }

    private fun counts(logged: List<LoggedSet>): Map<Pair<Long, SetSide>, Int> =
        logged.groupingBy { it.exerciseId to it.side }.eachCount()

    private fun isDone(slot: WorkoutSlot, counts: Map<Pair<Long, SetSide>, Int>): Boolean =
        (counts[slot.exerciseId to slot.side] ?: 0) >= slot.setNumber

    /** Der nächste offene Satz, null wenn alles erledigt ist. */
    fun nextSlot(slots: List<WorkoutSlot>, logged: List<LoggedSet>): WorkoutSlot? {
        val counts = counts(logged)
        return slots.firstOrNull { !isDone(it, counts) }
    }

    /** Anzahl der erledigten Sätze (für Anzeigen wie "3 von 6"). */
    fun completed(slots: List<WorkoutSlot>, logged: List<LoggedSet>): Int {
        val counts = counts(logged)
        return slots.count { isDone(it, counts) }
    }
}
