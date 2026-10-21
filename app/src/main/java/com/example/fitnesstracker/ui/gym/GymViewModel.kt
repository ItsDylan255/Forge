package com.example.fitnesstracker.ui.gym

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.GymExercise
import com.example.fitnesstracker.data.local.GymRoutine
import com.example.fitnesstracker.data.local.GymRoutineExercise
import com.example.fitnesstracker.data.local.GymSession
import com.example.fitnesstracker.data.local.GymSet
import com.example.fitnesstracker.domain.gym.ExerciseKind
import com.example.fitnesstracker.domain.gym.GymInputParser
import com.example.fitnesstracker.domain.gym.GymParseResult
import com.example.fitnesstracker.domain.gym.SetSide
import com.example.fitnesstracker.service.WorkoutNotifier
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

data class RoutineEditData(val name: String, val items: List<RoutineEditItem>)

/**
 * Der laufende Pausen-Countdown. Er liegt im ViewModel, damit er beim Wechsel zwischen
 * Tabs oder Bildschirmen weiterläuft. Die Restzeit ergibt sich aus [endsAtMillis].
 */
data class RestTimer(
    val exerciseName: String,
    val endsAtMillis: Long,
    val totalMillis: Long
)

class GymViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = DatabaseProvider.getDatabase(application).gymDao()
    private val weightDao = DatabaseProvider.getDatabase(application).weightEntryDao()

    val exercises = dao.observeExercises().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val routines = dao.observeRoutines().stateIn(
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

    private val _restTimer = MutableStateFlow<RestTimer?>(null)

    /** null = gerade keine Pause. */
    val restTimer: StateFlow<RestTimer?> = _restTimer.asStateFlow()

    // Hält die Benachrichtigung (und das Signal am Pausenende) auf dem neuesten Stand.
    // Erst nach dem ersten Laden der Einheiten, sonst würde der Start "kein Training" melden.
    init {
        viewModelScope.launch {
            combine(sessionsLoaded, activeSession, restTimer) { loaded, session, rest ->
                Triple(loaded, session, rest)
            }.collect { (loaded, session, rest) ->
                if (loaded) {
                    WorkoutNotifier.sync(
                        context = getApplication(),
                        sessionName = session?.routineName,
                        startedAt = session?.startedAt,
                        restLabel = rest?.exerciseName,
                        restEndsAt = rest?.endsAtMillis,
                        restTotalMillis = rest?.totalMillis
                    )
                }
            }
        }
    }

    /** Startet den Countdown. Ein bereits laufender wird ersetzt. 0 Sekunden = keine Pause. */
    fun startRest(exerciseName: String, seconds: Int) {
        if (seconds <= 0) {
            _restTimer.value = null
            return
        }

        val total = seconds * 1000L
        _restTimer.value = RestTimer(
            exerciseName = exerciseName,
            endsAtMillis = System.currentTimeMillis() + total,
            totalMillis = total
        )
    }

    /** Beendet die Pause (vorzeitig oder nach Ablauf). */
    fun skipRest() {
        _restTimer.value = null
    }

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
        return RoutineEditData(routine.name, items)
    }

    fun saveRoutine(
        routineId: Long?,
        name: String,
        items: List<RoutineItemInput>,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = routineId?.let { dao.getRoutine(it) }

            val routine = GymRoutine(
                id = existing?.id ?: 0L,
                name = name.trim(),
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

    // ---------- Einheiten ----------

    /** Startet eine Einheit. Läuft schon eine, wird keine zweite angelegt. */
    fun startSession(routineId: Long?, onStarted: () -> Unit) {
        viewModelScope.launch {
            val running = dao.observeSessions().first().firstOrNull { it.endedAt == null }

            if (running == null) {
                val routine = routineId?.let { dao.getRoutine(it) }
                val now = System.currentTimeMillis()
                dao.insertSession(
                    GymSession(
                        routineId = routine?.id,
                        routineName = routine?.name ?: "Freies Workout",
                        dateEpochDay = LocalDate.now().toEpochDay(),
                        startedAt = now,
                        endedAt = null,
                        createdAt = now,
                        updatedAt = now
                    )
                )
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
            _restTimer.value = null

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
            _restTimer.value = null
            dao.deleteSession(sessionId)
            onDone()
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            _restTimer.value = null
            dao.deleteSession(sessionId)
        }
    }
}
