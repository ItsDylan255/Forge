package com.example.fitnesstracker.domain.wear

import com.example.fitnesstracker.domain.gym.ExerciseKind
import com.example.fitnesstracker.domain.gym.GymCalculator
import com.example.fitnesstracker.domain.gym.GymFormat
import com.example.fitnesstracker.domain.gym.LoggedSet
import com.example.fitnesstracker.domain.gym.OneRepMax
import com.example.fitnesstracker.domain.gym.PlanItem
import com.example.fitnesstracker.domain.gym.SetSide
import com.example.fitnesstracker.domain.gym.WorkoutFlow
import com.example.fitnesstracker.domain.gym.WorkoutSessionData
import com.example.fitnesstracker.domain.gym.WorkoutSetData

/** Eine Übung der laufenden Routine mit allem, was die Uhr anzeigen soll. */
data class WatchPlanItem(
    val exerciseId: Long,
    val plannedSets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val restSeconds: Int,
    val unilateral: Boolean = false,
    val supersetWithNext: Boolean = false
)

/** Die laufende Pause, wie sie die Uhr braucht. */
data class WatchRest(val label: String, val endsAtMillis: Long, val totalMillis: Long)

/**
 * Was die Uhr während eines Trainings zeigt. Alle Texte sind fertig formatiert,
 * damit die Uhr nichts rechnen muss. [exerciseName] null = noch keine Übung im Training.
 */
data class WatchWorkoutState(
    val sessionId: Long,
    val routineName: String,
    val startedAt: Long,
    val exerciseName: String?,
    val setText: String?,
    val targetText: String?,
    val lastText: String?,
    val progressText: String?,
    val superset: Boolean,
    val recordText: String?,
    val rest: WatchRest?
)

/**
 * Bestimmt die Übung, an der gerade gearbeitet wird: die Seite des zuletzt geloggten Satzes, solange
 * dort noch etwas offen ist, sonst die erste Seite mit offenen Sätzen. Ist alles erledigt, bleibt die
 * Seite des letzten Satzes stehen.
 */
object WatchWorkout {

    fun state(
        sessionId: Long,
        routineName: String,
        startedAt: Long,
        plan: List<WatchPlanItem>,
        names: Map<Long, String>,
        kinds: Map<Long, ExerciseKind>,
        sessions: List<WorkoutSessionData>,
        sets: List<WorkoutSetData>,
        rest: WatchRest?
    ): WatchWorkoutState {
        val sessionSets = sets.filter { it.sessionId == sessionId }.sortedBy { it.order }
        val planItems = plan.map {
            PlanItem(it.exerciseId, it.plannedSets, it.restSeconds, it.unilateral, it.supersetWithNext)
        }
        val planIds = planItems.map { it.exerciseId }.toSet()

        // Wie auf dem Handy: eine Seite pro Übung oder Superset, Übungen ohne Plan einzeln dahinter
        val pages: List<List<PlanItem>> = WorkoutFlow.groups(planItems) +
            sessionSets.map { it.exerciseId }.distinct().filter { it !in planIds }
                .map { listOf(PlanItem(it, plannedSets = 0, restSeconds = 0)) }

        val empty = WatchWorkoutState(
            sessionId, routineName, startedAt,
            exerciseName = null, setText = null, targetText = null, lastText = null,
            progressText = null, superset = false, recordText = null, rest = rest
        )
        if (pages.isEmpty()) return empty

        fun logged(page: List<PlanItem>) = sessionSets
            .filter { set -> page.any { it.exerciseId == set.exerciseId } }
            .map { LoggedSet(it.exerciseId, it.side) }

        fun openSlot(page: List<PlanItem>) = WorkoutFlow.nextSlot(WorkoutFlow.slots(page), logged(page))

        val lastPage = sessionSets.lastOrNull()?.let { last ->
            pages.indexOfFirst { page -> page.any { it.exerciseId == last.exerciseId } }
        } ?: -1
        // Übungen ohne Plan haben keine offenen Sätze, bleiben aber stehen, solange an ihnen gearbeitet wird
        val pageIndex = when {
            lastPage >= 0 && (openSlot(pages[lastPage]) != null || pages[lastPage].first().exerciseId !in planIds) ->
                lastPage
            else -> pages.indexOfFirst { openSlot(it) != null }.takeIf { it >= 0 }
                ?: lastPage.takeIf { it >= 0 }
                ?: 0
        }
        val page = pages[pageIndex]
        val slot = openSlot(page)

        // Offener Satz, sonst die Übung des letzten Satzes auf dieser Seite
        val exerciseId = slot?.exerciseId
            ?: sessionSets.lastOrNull { set -> page.any { it.exerciseId == set.exerciseId } }?.exerciseId
            ?: page.first().exerciseId
        val planned = plan.firstOrNull { it.exerciseId == exerciseId }
        val kind = kinds[exerciseId] ?: ExerciseKind.REPS
        val name = names[exerciseId]
            ?: sessionSets.lastOrNull { it.exerciseId == exerciseId }?.exerciseName
            ?: "Übung"
        val doneOfExercise = sessionSets.count { it.exerciseId == exerciseId }

        val setText = when {
            slot != null -> "Satz ${slot.setNumber} von ${planned?.plannedSets ?: slot.setNumber}" + sideText(slot.side)
            planned != null && planned.plannedSets > 0 -> "Alle Sätze erledigt"
            else -> "Satz ${doneOfExercise + 1}"
        }

        val targetText = planned?.takeIf { it.plannedSets > 0 }?.let {
            val range = GymFormat.repsRange(kind, it.repsMin, it.repsMax)
            if (kind == ExerciseKind.TIME) "Ziel: $range" else "Ziel: $range Wdh."
        }

        val last = GymCalculator.lastPerformance(exerciseId, sessionId, sessions, sets)
        val lastSet = last?.let { performance ->
            val side = slot?.side ?: SetSide.BOTH
            val sameSide = performance.sets.filter { it.side == side }.ifEmpty { performance.sets }
            val number = slot?.setNumber ?: sameSide.size
            sameSide.getOrNull(number - 1) ?: sameSide.last()
        }
        val lastText = lastSet?.let { "Letztes Mal: " + GymFormat.setText(it.kind, it.weightKg, it.reps) }

        // Rekord bei irgendeiner Übung der Seite (im Superset zählen beide)
        val recordText = page.firstNotNullOfOrNull { item ->
            if ((kinds[item.exerciseId] ?: ExerciseKind.REPS) != ExerciseKind.REPS) return@firstNotNullOfOrNull null
            OneRepMax.newRecord(item.exerciseId, sessionId, sessions, sets)?.let { (now, _) ->
                "Neuer Rekord! 1RM ${GymFormat.kg(OneRepMax.roundToPlate(now))}"
            }
        }

        return WatchWorkoutState(
            sessionId = sessionId,
            routineName = routineName,
            startedAt = startedAt,
            exerciseName = name,
            setText = setText,
            targetText = targetText,
            lastText = lastText,
            progressText = "Übung ${pageIndex + 1} von ${pages.size}",
            superset = page.size > 1,
            recordText = recordText,
            rest = rest
        )
    }

    private fun sideText(side: SetSide): String = when (side) {
        SetSide.BOTH -> ""
        SetSide.RIGHT -> " · rechts"
        SetSide.LEFT -> " · links"
    }
}

/** Die Tageswerte für Kachel und Komplikation. Null = an diesem Tag noch kein Wert. */
data class WatchToday(
    val dateEpochDay: Long,
    val steps: Int,
    val stepGoal: Int,
    val weightKg: Double?,
    val kcal: Int?,
    val kcalTarget: Int?
)
