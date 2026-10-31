package com.example.fitnesstracker.ui.gym

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.GymExercise
import com.example.fitnesstracker.data.local.GymPlan
import com.example.fitnesstracker.data.local.GymRoutine
import com.example.fitnesstracker.data.local.GymRoutineExercise
import com.example.fitnesstracker.data.local.GymSession
import com.example.fitnesstracker.data.local.GymSet
import com.example.fitnesstracker.domain.gym.ExerciseKind
import com.example.fitnesstracker.domain.gym.GymInputParser
import com.example.fitnesstracker.domain.gym.GymParseResult
import com.example.fitnesstracker.domain.gym.PlanRef
import com.example.fitnesstracker.domain.gym.RoutinePlans
import com.example.fitnesstracker.domain.gym.RoutineRef
import com.example.fitnesstracker.domain.gym.SetSide
import com.example.fitnesstracker.service.LiveSync
import com.example.fitnesstracker.service.RestTimer
import com.example.fitnesstracker.service.RestTimerStore
import com.example.fitnesstracker.wear.WearBridge
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Eine bereits geprüfte Übung für das Speichern einer Routine. */
data class RoutineItemInput(
    val exerciseId: Long,
    val plannedSets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val restSeconds: Int,
    val unilateral: Boolean = false,
    val supersetWithNext: Boolean = false
)

data class RoutineEditItem(
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String?,
    val plannedSets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val restSeconds: Int,
    val unilateral: Boolean = false,
    val supersetWithNext: Boolean = false,
    val kind: ExerciseKind = ExerciseKind.REPS
)

data class RoutineEditData(val name: String, val planId: Long?, val items: List<RoutineEditItem>)

fun GymRoutine.toRef() = RoutineRef(
    id = id,
    name = name,
    planId = planId,
    planPosition = planPosition,
    archived = archivedAt != null
)

fun GymPlan.toRef() = PlanRef(id = id, name = name, archived = archivedAt != null)

class GymViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = DatabaseProvider.getDatabase(application).gymDao()
    private val weightDao = DatabaseProvider.getDatabase(application).weightEntryDao()

    val exercises = dao.observeExercises().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val routines = dao.observeRoutines().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val plans = dao.observePlans().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val routineExercises = dao.observeRoutineExercises().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val sessions = dao.observeSessions().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    /** false, solange die Datenbank die Einheiten noch nicht geliefert hat. */
    val sessionsLoaded = dao.observeSessions().map { true }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false
    )

    val sets = dao.observeSets().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    /** Die gerade laufende Einheit (endedAt == null), sonst null. */
    val activeSession = sessions
        .map { list -> list.firstOrNull { it.endedAt == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ---------- Pausen-Timer ----------

    /** null = gerade keine Pause. Liegt im [RestTimerStore], damit auch die Uhr sie steuern kann. */
    val restTimer: StateFlow<RestTimer?> = RestTimerStore.timer

    // Hält Benachrichtigung, Pausen-Signal und Uhr auf dem neuesten Stand
    init {
        LiveSync.start(application)
    }

    /** Startet den Countdown. Ein bereits laufender wird ersetzt. 0 Sekunden = keine Pause. */
    fun startRest(exerciseName: String, seconds: Int) = RestTimerStore.start(exerciseName, seconds)

    /** Beendet die Pause (vorzeitig oder nach Ablauf). */
    fun skipRest() = RestTimerStore.skip()

    // ---------- Übungen ----------

    /**
     * Legt eine Übung an ([id] == null) oder ändert sie.
     * [onDone] bekommt eine Fehlermeldung (oder null) und die Id der gespeicherten Übung.
     */
    fun saveExercise(
        id: Long?,
        name: String,
        muscleGroup: String?,
        /** Art der Übung. Null = beim Anlegen "Wiederholungen", beim Ändern unverändert. */
        kind: ExerciseKind? = null,
        onDone: (error: String?, savedId: Long?) -> Unit
    ) {
        viewModelScope.launch {
            val cleanName = when (val result = GymInputParser.parseName(name)) {
                is GymParseResult.Valid -> result.value
                is GymParseResult.Invalid -> {
                    onDone(result.message, null)
                    return@launch
                }
            }

            val duplicate = dao.findExerciseByName(cleanName)
            if (duplicate != null && duplicate.id != id) {
                onDone("Eine Übung mit diesem Namen gibt es schon.", null)
                return@launch
            }

            val now = System.currentTimeMillis()

            if (id == null) {
                val newId = dao.insertExercise(
                    GymExercise(
                        name = cleanName,
                        muscleGroup = muscleGroup,
                        kind = (kind ?: ExerciseKind.REPS).code,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                onDone(null, newId)
            } else {
                val old = dao.getExercise(id)
                if (old != null) {
                    dao.updateExercise(
                        old.copy(
                            name = cleanName,
                            muscleGroup = muscleGroup,
                            kind = kind?.code ?: old.kind,
                            updatedAt = now
                        )
                    )
                }
                onDone(null, id)
            }
        }
    }

    fun deleteExercise(id: Long) {
        viewModelScope.launch { dao.deleteExercise(id) }
    }

    // ---------- Routinen ----------

    suspend fun loadRoutineForEdit(routineId: Long): RoutineEditData? {
        val routine = dao.getRoutine(routineId) ?: return null
        val byId = dao.getAllExercises().associateBy { it.id }

        val items = dao.getRoutineItems(routineId).mapNotNull { item ->
            val exercise = byId[item.exerciseId] ?: return@mapNotNull null
            RoutineEditItem(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                muscleGroup = exercise.muscleGroup,
                plannedSets = item.plannedSets,
                repsMin = item.repsMin,
                repsMax = item.repsMax,
                restSeconds = item.restSeconds,
                unilateral = item.unilateral,
                supersetWithNext = item.supersetWithNext,
                kind = ExerciseKind.fromCode(exercise.kind)
            )
        }
        return RoutineEditData(routine.name, routine.planId, items)
    }

    fun saveRoutine(
        routineId: Long?,
        name: String,
        planId: Long?,
        items: List<RoutineItemInput>,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = routineId?.let { dao.getRoutine(it) }

            // Neu im Plan: hinten anhängen. Gleicher Plan: Position behalten.
            val position = when {
                planId == null -> 0
                existing?.planId == planId -> existing.planPosition
                else -> dao.maxPlanPosition(planId) + 1
            }

            val routine = GymRoutine(
                id = existing?.id ?: 0L,
                name = name.trim(),
                planId = planId,
                planPosition = position,
                archivedAt = existing?.archivedAt,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )

            val rows = items.mapIndexed { index, item ->
                GymRoutineExercise(
                    routineId = 0L,
                    exerciseId = item.exerciseId,
                    position = index,
                    plannedSets = item.plannedSets,
                    repsMin = item.repsMin,
                    repsMax = item.repsMax,
                    restSeconds = item.restSeconds,
                    unilateral = item.unilateral,
                    // Das Häkchen an der letzten Übung hat keine Wirkung und wird nicht gespeichert
                    supersetWithNext = item.supersetWithNext && index < items.lastIndex
                )
            }

            dao.saveRoutine(routine, rows)
            onDone()
        }
    }

    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch { dao.deleteRoutine(routineId) }
    }

    /** Archiviert die Routine ([archived] true) oder holt sie zurück. Der Verlauf bleibt unverändert. */
    fun setRoutineArchived(routineId: Long, archived: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.setRoutineArchived(routineId, if (archived) now else null, now)
        }
    }

    /** Verschiebt die Routine innerhalb ihres Plans um eine Stelle. */
    fun moveRoutineInPlan(routineId: Long, up: Boolean) {
        viewModelScope.launch {
            val planId = dao.getRoutine(routineId)?.planId ?: return@launch
            val inPlan = RoutinePlans.activeRoutines(
                routines.value.filter { it.planId == planId }.map { it.toRef() },
                plans.value.map { it.toRef() }
            )
            dao.setPlanOrder(RoutinePlans.moved(inPlan, routineId, up), System.currentTimeMillis())
        }
    }

    // ---------- Trainingspläne ----------

    /**
     * Legt einen Plan an ([planId] == null) oder benennt ihn um.
     * [onDone] bekommt eine Fehlermeldung (oder null) und die Id des Plans.
     */
    fun savePlan(planId: Long?, name: String, onDone: (error: String?, savedId: Long?) -> Unit) {
        viewModelScope.launch {
            val cleanName = when (val result = GymInputParser.parseName(name)) {
                is GymParseResult.Valid -> result.value
                is GymParseResult.Invalid -> {
                    onDone(result.message, null)
                    return@launch
                }
            }

            val duplicate = dao.findPlanByName(cleanName)
            if (duplicate != null && duplicate.id != planId) {
                onDone("Einen Plan mit diesem Namen gibt es schon.", null)
                return@launch
            }

            val now = System.currentTimeMillis()
            if (planId == null) {
                val newId = dao.insertPlan(GymPlan(name = cleanName, createdAt = now, updatedAt = now))
                onDone(null, newId)
            } else {
                dao.getPlan(planId)?.let { dao.updatePlan(it.copy(name = cleanName, updatedAt = now)) }
                onDone(null, planId)
            }
        }
    }

    /** Archiviert den Plan samt Routinen. Die Routinen selbst werden nicht verändert. */
    fun setPlanArchived(planId: Long, archived: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.setPlanArchived(planId, if (archived) now else null, now)
        }
    }

    /**
     * Kopiert den Plan samt aktiven Routinen unter neuem Namen. Mit [deload] hat jede Übung einen Satz
     * weniger (mindestens einer). [onDone] bekommt eine Fehlermeldung oder null.
     */
    fun duplicatePlan(planId: Long, name: String, deload: Boolean, onDone: (error: String?) -> Unit) {
        viewModelScope.launch {
            val cleanName = when (val result = GymInputParser.parseName(name)) {
                is GymParseResult.Valid -> result.value
                is GymParseResult.Invalid -> {
                    onDone(result.message)
                    return@launch
                }
            }
            if (dao.findPlanByName(cleanName) != null) {
                onDone("Einen Plan mit diesem Namen gibt es schon.")
                return@launch
            }

            dao.duplicatePlan(planId, cleanName, System.currentTimeMillis()) { item ->
                if (deload) item.copy(plannedSets = (item.plannedSets - 1).coerceAtLeast(1)) else item
            }
            onDone(null)
        }
    }

    /** Löscht nur den Plan, seine Routinen sind danach ohne Plan. */
    fun deletePlan(planId: Long) {
        viewModelScope.launch { dao.deletePlan(planId) }
    }

    // ---------- Einheiten ----------

    /** Startet eine Einheit. Läuft schon eine, wird keine zweite angelegt. */
    fun startSession(routineId: Long?, onStarted: () -> Unit) {
        viewModelScope.launch {
            val running = dao.observeSessions().first().firstOrNull { it.endedAt == null }

            if (running == null) {
                val routine = routineId?.let { dao.getRoutine(it) }
                val plan = routine?.planId?.let { dao.getPlan(it) }
                val now = System.currentTimeMillis()
                dao.insertSession(
                    GymSession(
                        routineId = routine?.id,
                        routineName = routine?.name ?: "Freies Workout",
                        planId = plan?.id,
                        planName = plan?.name,
                        dateEpochDay = LocalDate.now().toEpochDay(),
                        startedAt = now,
                        endedAt = null,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                // Öffnet das Training auf der Uhr, falls eine verbunden ist
                WearBridge.openOnWatch(getApplication())
            }
            onStarted()
        }
    }

    fun addSet(
        sessionId: Long,
        exerciseId: Long,
        exerciseName: String,
        weightKg: Double,
        reps: Int,
        side: SetSide = SetSide.BOTH
    ) {
        viewModelScope.launch {
            val next = dao.maxSetNumber(sessionId, exerciseId, side.code) + 1

            // Art und (bei assistierten Übungen) Körpergewicht werden mit dem Satz festgehalten,
            // damit sich alte Trainings später nicht verändern
            val kind = ExerciseKind.fromCode(dao.getExercise(exerciseId)?.kind ?: 0)
            val bodyWeight = if (kind == ExerciseKind.ASSISTED) {
                weightDao.getAllEntries().first().lastOrNull()?.weightKg
            } else {
                null
            }
            dao.insertSet(
                GymSet(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    setNumber = next,
                    weightKg = weightKg,
                    reps = reps,
                    side = side.code,
                    kind = kind.code,
                    bodyWeightKg = bodyWeight,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch { dao.deleteSet(setId) }
    }

    /** Beendet die Einheit und speichert damit die Dauer (endedAt − startedAt). */
    fun finishSession(sessionId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            RestTimerStore.skip()

            val session = dao.getSession(sessionId)
            if (session != null && session.endedAt == null) {
                val now = System.currentTimeMillis()
                dao.updateSession(session.copy(endedAt = now, updatedAt = now))
            }
            onDone()
        }
    }

    /** Verwirft eine laufende Einheit samt Sätzen. */
    fun discardSession(sessionId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            RestTimerStore.skip()
            dao.deleteSession(sessionId)
            onDone()
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            RestTimerStore.skip()
            dao.deleteSession(sessionId)
        }
    }
}
