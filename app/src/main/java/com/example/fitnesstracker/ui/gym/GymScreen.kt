package com.example.fitnesstracker.ui.gym

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnesstracker.data.local.GymExercise
import com.example.fitnesstracker.data.local.GymPlan
import com.example.fitnesstracker.data.local.GymRoutine
import com.example.fitnesstracker.data.local.GymRoutineExercise
import com.example.fitnesstracker.data.local.GymSession
import com.example.fitnesstracker.domain.gym.GymFormat
import com.example.fitnesstracker.domain.gym.PlanProgress
import com.example.fitnesstracker.domain.gym.PlanProgressCalculator
import com.example.fitnesstracker.domain.gym.PlanSessionRef
import com.example.fitnesstracker.domain.gym.RoutinePlans
import com.example.fitnesstracker.domain.gym.toData
import com.example.fitnesstracker.ui.weight.ForgeCard
import java.time.LocalDate

/**
 * Einstieg des Gym-Tabs. Die Unterseiten werden über eine einfache Route gesteuert:
 * home, library, archive, editor:<id|0>, newin:<planId>, workout, session:<id>, summary:<id>, exercise:<id>
 */
@Composable
fun GymScreen(viewModel: GymViewModel) {
    var route by rememberSaveable { mutableStateOf("home") }
    var homeTab by rememberSaveable { mutableStateOf(0) }

    BackHandler(enabled = route != "home") { route = "home" }

    when {
        route == "library" ->
            ExerciseLibraryScreen(viewModel = viewModel, onBack = { route = "home" })

        route == "archive" ->
            RoutineArchiveScreen(viewModel = viewModel, onBack = { route = "home" })

        route.startsWith("newin:") ->
            RoutineEditorScreen(
                viewModel = viewModel,
                routineId = null,
                onDone = { route = "home" },
                initialPlanId = route.removePrefix("newin:").toLongOrNull()
            )

        route.startsWith("editor:") ->
            RoutineEditorScreen(
                viewModel = viewModel,
                routineId = route.removePrefix("editor:").toLongOrNull()?.takeIf { it != 0L },
                onDone = { route = "home" }
            )

        route == "workout" ->
            WorkoutScreen(
                viewModel = viewModel,
                onFinished = { sessionId -> route = "summary:$sessionId" },
                onExit = { route = "home" }
            )

        route.startsWith("summary:") ->
            SessionDetailScreen(
                viewModel = viewModel,
                sessionId = route.removePrefix("summary:").toLongOrNull() ?: 0L,
                isSummary = true,
                onClose = { route = "home" }
            )

        route.startsWith("session:") ->
            SessionDetailScreen(
                viewModel = viewModel,
                sessionId = route.removePrefix("session:").toLongOrNull() ?: 0L,
                isSummary = false,
                onClose = { route = "home" }
            )

        route.startsWith("exercise:") ->
            ExerciseProgressScreen(
                viewModel = viewModel,
                exerciseId = route.removePrefix("exercise:").toLongOrNull() ?: 0L,
                onBack = { route = "home" }
            )

        else ->
            GymHome(
                viewModel = viewModel,
                homeTab = homeTab,
                onTab = { homeTab = it },
                onNavigate = { route = it }
            )
    }
}

@Composable
private fun GymHome(
    viewModel: GymViewModel,
    homeTab: Int,
    onTab: (Int) -> Unit,
    onNavigate: (String) -> Unit
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val routineExercises by viewModel.routineExercises.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val sets by viewModel.sets.collectAsStateWithLifecycle()
    val active by viewModel.activeSession.collectAsStateWithLifecycle()

    val todayEpochDay = LocalDate.now().toEpochDay()

    // Verlauf und Fortschritt zählen nur beendete Einheiten
    val finished = sessions.filter { it.endedAt != null }
    val finishedIds = finished.map { it.id }.toSet()
    val sessionData = finished.map { it.toData() }
    val setData = sets.filter { it.sessionId in finishedIds }.map { it.toData() }

    GymPage {
        GymHeader(title = "Gym")

        Spacer(modifier = Modifier.height(16.dp))

        active?.let { running ->
            ActiveSessionBanner(session = running, onResume = { onNavigate("workout") })
            Spacer(modifier = Modifier.height(16.dp))
        }

        GymTabs(
            labels = listOf("Routinen", "Verlauf", "Fortschritt"),
            selected = homeTab,
            onSelect = onTab
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (homeTab) {
            0 -> RoutinesTab(
                viewModel = viewModel,
                routines = routines,
                plans = plans,
                routineItems = routineExercises,
                exercises = exercises,
                sessions = finished,
                today = todayEpochDay,
                hasActive = active != null,
                onStart = { routineId ->
                    viewModel.startSession(routineId) { onNavigate("workout") }
                },
                onNavigate = onNavigate
            )

            1 -> HistoryTab(
                sessions = finished,
                sessionData = sessionData,
                setData = setData,
                today = todayEpochDay,
                onOpen = { onNavigate("session:$it") }
            )

            else -> ProgressTab(
                bests = com.example.fitnesstracker.domain.gym.GymCalculator
                    .personalBests(sessionData, setData),
                onOpen = { onNavigate("exercise:$it") }
            )
        }
    }
}

@Composable
private fun ActiveSessionBanner(session: GymSession, onResume: () -> Unit) {
    ForgeCard(modifier = Modifier.fillMaxWidth(), onClick = onResume) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WORKOUT LÄUFT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = session.routineName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Gestartet um ${GymFormat.clock(session.startedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onResume) { Text("Fortsetzen") }
        }
    }
}

@Composable
private fun RoutinesTab(
    viewModel: GymViewModel,
    routines: List<GymRoutine>,
    plans: List<GymPlan>,
    routineItems: List<GymRoutineExercise>,
    exercises: List<GymExercise>,
    sessions: List<GymSession>,
    today: Long,
    hasActive: Boolean,
    onStart: (Long?) -> Unit,
    onNavigate: (String) -> Unit
) {
    val nameById = exercises.associate { it.id to it.name }
    val itemsByRoutine = routineItems.groupBy { it.routineId }
    val routineRefs = routines.map { it.toRef() }
    val planRefs = plans.map { it.toRef() }
    val groups = RoutinePlans.groups(routineRefs, planRefs)
    val archive = RoutinePlans.archive(routineRefs, planRefs)
    val planSessions = sessions.map {
        PlanSessionRef(it.planId, it.routineId, it.routineName, it.dateEpochDay, it.startedAt)
    }

    var newPlan by remember { mutableStateOf(false) }
    var renamePlan by remember { mutableStateOf<GymPlan?>(null) }
    var deletePlan by remember { mutableStateOf<GymPlan?>(null) }
    var duplicatePlan by remember { mutableStateOf<GymPlan?>(null) }
    var duplicateError by remember { mutableStateOf<String?>(null) }
    var planError by remember { mutableStateOf<String?>(null) }

    Button(onClick = { onNavigate("editor:0") }, modifier = Modifier.fillMaxWidth()) { Text("+ Neue Routine") }

    Spacer(modifier = Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onStart(null) },
            enabled = !hasActive,
            modifier = Modifier.weight(1f)
        ) { Text("Freies Workout") }

        OutlinedButton(onClick = { onNavigate("library") }, modifier = Modifier.weight(1f)) { Text("Übungen") }
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(onClick = { newPlan = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Neuer Trainingsplan") }

    if (groups.isEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        EmptyHint("Noch keine Routine. Lege zuerst unter „Übungen“ deine Übungen an und erstelle danach deine erste Routine, zum Beispiel „Push Day“.")
    }

    groups.forEach { group ->
        val plan = group.plan?.let { ref -> plans.firstOrNull { it.id == ref.id } }

        Row(verticalAlignment = Alignment.Bottom) {
            Box(modifier = Modifier.weight(1f)) {
                GroupLabel(plan?.name ?: if (groups.size == 1) "Routinen" else "Ohne Plan")
            }
            if (plan != null) {
                OverflowMenu(
                    listOf(
                        MenuAction("Routine hinzufügen") { onNavigate("newin:${plan.id}") },
                        MenuAction("Umbenennen") { renamePlan = plan },
                        MenuAction("Duplizieren") { duplicatePlan = plan },
                        MenuAction("Plan archivieren") { viewModel.setPlanArchived(plan.id, archived = true) },
                        MenuAction("Plan löschen", destructive = true) { deletePlan = plan }
                    )
                )
            }
        }

        if (plan != null) {
            PlanProgressCalculator.progress(
                planId = plan.id,
                activeRoutineIds = group.routines.map { it.id }.toSet(),
                sessions = planSessions,
                today = today
            )?.let { progress ->
                PlanProgressLine(progress, today)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (group.routines.isEmpty()) {
            EmptyHint("Noch keine Routine in diesem Plan. Füge über ⋮ eine hinzu.")
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            group.routines.forEachIndexed { index, routine ->
                val items = itemsByRoutine[routine.id].orEmpty().sortedBy { it.position }
                RoutineCard(
                    name = routine.name,
                    items = items,
                    exerciseNames = items.mapNotNull { nameById[it.exerciseId] },
                    canStart = !hasActive && items.isNotEmpty(),
                    onStart = { onStart(routine.id) },
                    onEdit = { onNavigate("editor:${routine.id}") },
                    menu = listOfNotNull(
                        if (plan != null && index > 0)
                            MenuAction("Nach oben") { viewModel.moveRoutineInPlan(routine.id, up = true) }
                        else null,
                        if (plan != null && index < group.routines.lastIndex)
                            MenuAction("Nach unten") { viewModel.moveRoutineInPlan(routine.id, up = false) }
                        else null,
                        MenuAction("Archivieren") { viewModel.setRoutineArchived(routine.id, archived = true) }
                    )
                )
            }
        }
    }

    if (!archive.isEmpty) {
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { onNavigate("archive") }, modifier = Modifier.fillMaxWidth()) {
            Text("Archiv (${archive.count}) ›")
        }
    }

    if (newPlan || renamePlan != null) {
        val editing = renamePlan
        PlanNameDialog(
            title = if (editing == null) "Neuer Trainingsplan" else "Plan umbenennen",
            initialName = editing?.name.orEmpty(),
            error = planError,
            onSave = { name ->
                viewModel.savePlan(editing?.id, name) { message, _ ->
                    planError = message
                    if (message == null) {
                        newPlan = false
                        renamePlan = null
                    }
                }
            },
            onDismiss = {
                newPlan = false
                renamePlan = null
                planError = null
            }
        )
    }

    duplicatePlan?.let { plan ->
        DuplicatePlanDialog(
            planName = plan.name,
            error = duplicateError,
            onSave = { name, deload ->
                viewModel.duplicatePlan(plan.id, name, deload) { message ->
                    duplicateError = message
                    if (message == null) duplicatePlan = null
                }
            },
            onDismiss = {
                duplicatePlan = null
                duplicateError = null
            }
        )
    }

    deletePlan?.let { plan ->
        ConfirmDialog(
            title = "Plan löschen?",
            message = "Nur der Plan „${plan.name}“ wird gelöscht. Seine Routinen bleiben erhalten und sind danach ohne Plan.",
            confirmLabel = "Löschen",
            onConfirm = {
                viewModel.deletePlan(plan.id)
                deletePlan = null
            },
            onDismiss = { deletePlan = null }
        )
    }
}

/** „Woche 3 · 2 Durchgänge · zuletzt Leg Day 2 (gestern)“ und ein Balken für diese Woche. */
@Composable
private fun PlanProgressLine(progress: PlanProgress, today: Long) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Text(
        text = "Woche ${progress.week} · " +
                (if (progress.rounds == 1) "1 Durchgang" else "${progress.rounds} Durchgänge") +
                " · zuletzt ${progress.lastRoutineName} " +
                "(${PlanProgressCalculator.daysAgoText(progress.lastEpochDay, today)})",
        style = MaterialTheme.typography.bodySmall,
        color = muted
    )

    if (progress.routineCount > 0) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { progress.doneThisWeek.toFloat() / progress.routineCount },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${progress.doneThisWeek} von ${progress.routineCount} diese Woche",
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        }
    }
}

@Composable
private fun RoutineCard(
    name: String,
    items: List<GymRoutineExercise>,
    exerciseNames: List<String>,
    canStart: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    menu: List<MenuAction>
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (items.isEmpty()) "Keine Übungen"
                else "${items.size} Übungen · " +
                        exerciseNames.take(3).joinToString(", ") +
                        if (exerciseNames.size > 3) " …" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onStart,
                    enabled = canStart,
                    modifier = Modifier.weight(1f)
                ) { Text("Starten") }

                TextButton(onClick = onEdit) { Text("Bearbeiten") }

                OverflowMenu(menu)
            }
        }
    }
}
