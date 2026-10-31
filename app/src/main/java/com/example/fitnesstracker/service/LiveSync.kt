package com.example.fitnesstracker.service

import android.content.Context
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.StepPreferences
import com.example.fitnesstracker.domain.calories.CalorieCalculator
import com.example.fitnesstracker.domain.calories.CalorieGoalInput
import com.example.fitnesstracker.domain.calories.toCalorieProfile
import com.example.fitnesstracker.domain.gym.ExerciseKind
import com.example.fitnesstracker.domain.gym.toData
import com.example.fitnesstracker.domain.wear.WatchPlanItem
import com.example.fitnesstracker.domain.wear.WatchRest
import com.example.fitnesstracker.domain.wear.WatchToday
import com.example.fitnesstracker.domain.wear.WatchWorkout
import com.example.fitnesstracker.wear.WearBridge
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Hält alles, was außerhalb der Bildschirme den aktuellen Stand braucht, auf dem neuesten Stand:
 * die Trainings-Benachrichtigung (mit Pausen-Signal) und die Uhr. Läuft, solange der Prozess lebt.
 * [start] darf beliebig oft aufgerufen werden.
 */
object LiveSync {

    private var started = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Synchronized
    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        scope.launch { syncNotification(app) }
        scope.launch { syncWorkout(app) }
        scope.launch { syncToday(app) }
    }

    /** Startet, aktualisiert oder beendet die Trainings-Benachrichtigung. */
    private suspend fun syncNotification(app: Context) {
        val gym = DatabaseProvider.getDatabase(app).gymDao()

        combine(
            gym.observeSessions().map { list -> list.firstOrNull { it.endedAt == null } },
            RestTimerStore.timer
        ) { session, rest -> session?.routineName to session?.startedAt to rest }
            .distinctUntilChanged()
            .collect { (sessionInfo, rest) ->
                WorkoutNotifier.sync(
                    context = app,
                    sessionName = sessionInfo.first,
                    startedAt = sessionInfo.second,
                    restLabel = rest?.exerciseName,
                    restEndsAt = rest?.endsAtMillis,
                    restTotalMillis = rest?.totalMillis
                )
            }
    }

    private suspend fun syncWorkout(app: Context) {
        val gym = DatabaseProvider.getDatabase(app).gymDao()

        combine(
            gym.observeSessions(),
            gym.observeSets(),
            gym.observeRoutineExercises(),
            gym.observeExercises(),
            RestTimerStore.timer
        ) { sessions, sets, routineItems, exercises, rest ->
            val session = sessions.firstOrNull { it.endedAt == null }

            session?.let {
                WatchWorkout.state(
                    sessionId = it.id,
                    routineName = it.routineName,
                    startedAt = it.startedAt,
                    plan = routineItems
                        .filter { item -> item.routineId == it.routineId }
                        .sortedBy { item -> item.position }
                        .map { item ->
                            WatchPlanItem(
                                item.exerciseId, item.plannedSets, item.repsMin, item.repsMax,
                                item.restSeconds, item.unilateral, item.supersetWithNext
                            )
                        },
                    names = exercises.associate { e -> e.id to e.name },
                    kinds = exercises.associate { e -> e.id to ExerciseKind.fromCode(e.kind) },
                    sessions = sessions.map { s -> s.toData() },
                    sets = sets.map { s -> s.toData() },
                    rest = rest?.let { r -> WatchRest(r.exerciseName, r.endsAtMillis, r.totalMillis) }
                )
            }
        }
            .distinctUntilChanged()
            .collect { WearBridge.sendWorkout(app, it) }
    }

    private suspend fun syncToday(app: Context) {
        val db = DatabaseProvider.getDatabase(app)
        val stepPrefs = StepPreferences(app)

        combine(
            db.stepEntryDao().observeAll(),
            db.weightEntryDao().getAllEntries(),
            db.foodDao().observeLog(),
            db.userProfileDao().observeProfile(),
            db.weightGoalDao().observeGoal()
        ) { steps, weights, food, profile, goal ->
            val today = LocalDate.now().toEpochDay()
            val latestWeight = weights.maxByOrNull { it.dateEpochDay }?.weightKg
            val foodToday = food.filter { it.dateEpochDay == today }

            val target = profile?.toCalorieProfile()?.let { p ->
                latestWeight?.let { current ->
                    val input = goal?.let { CalorieGoalInput(it.startWeightKg, it.targetWeightKg, it.targetDateEpochDay) }
                    CalorieCalculator.calculate(p, current, input, today)
                }
            }

            WatchToday(
                dateEpochDay = today,
                steps = steps.firstOrNull { it.dateEpochDay == today }?.steps ?: 0,
                stepGoal = stepPrefs.getStepGoal(),
                weightKg = weights.firstOrNull { it.dateEpochDay == today }?.weightKg,
                kcal = foodToday.takeIf { it.isNotEmpty() }?.sumOf { it.calories }?.roundToInt(),
                kcalTarget = target?.targetKcal
            )
        }
            .distinctUntilChanged()
            .collect { WearBridge.sendToday(app, it) }
    }
}
