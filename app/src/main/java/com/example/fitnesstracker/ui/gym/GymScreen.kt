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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.fitnesstracker.i18n.tr
import com.example.fitnesstracker.ui.photos.PhotoDetailScreen
import com.example.fitnesstracker.ui.photos.PhotoNavigation
import com.example.fitnesstracker.ui.photos.PhotoViewModel
import com.example.fitnesstracker.ui.photos.PhotosTab
import com.example.fitnesstracker.ui.weight.ForgeCard
import java.time.LocalDate

private const val PHOTOS_TAB = 3

/**
 * Einstieg des Gym-Tabs. Die Unterseiten werden über eine einfache Route gesteuert:
 * home, library, archive, editor:<id|0>, newin:<planId>, workout, session:<id>, summary:<id>, exercise:<id>,
 * photo:<id>
 */
@Composable
fun GymScreen(viewModel: GymViewModel, photoViewModel: PhotoViewModel = viewModel()) {
    var route by rememberSaveable { mutableStateOf("home") }
    var homeTab by rememberSaveable { mutableStateOf(0) }

    BackHandler(enabled = route != "home") { route = "home" }

    // Startseite oder Erinnerung wollen direkt zu den Fotos
    val photosRequested by PhotoNavigation.requested.collectAsStateWithLifecycle()
    LaunchedEffect(photosRequested) {
        if (photosRequested) {
            route = "home"
            homeTab = PHOTOS_TAB
            PhotoNavigation.consume()
        }
    }

    when {
        route.startsWith("photo:") ->
            PhotoDetailScreen(
                viewModel = photoViewModel,
                photoId = route.removePrefix("photo:").toLongOrNull() ?: 0L,
                onBack = { route = "home" }
            )

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
                photoViewModel = photoViewModel,
                homeTab = homeTab,
                onTab = { homeTab = it },
                onNavigate = { route = it }
            )
    }
}

@Composable
private fun GymHome(
    viewModel: GymViewModel,
    photoViewModel: PhotoViewModel,
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
            labels = listOf(tr("Routinen", "Routines"), tr("Verlauf", "History"), tr("Fortschritt", "Progress"), tr("Fotos", "Photos")),
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

            2 -> ProgressTab(
                bests = com.example.fitnesstracker.domain.gym.GymCalculator
                    .personalBests(sessionData, setData),
                onOpen = { onNavigate("exercise:$it") }
            )

            else -> PhotosTab(
                viewModel = photoViewModel,
                onOpen = { onNavigate("photo:$it") }
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
                    text = tr("WORKOUT LÄUFT", "WORKOUT IN PROGRESS"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = session.routineName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = tr("Gestartet um ${GymFormat.clock(session.startedAt)}", "Started at ${GymFormat.clock(session.startedAt)}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onResume) { Text(tr("Fortsetzen", "Resume")) }
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

    Button(onClick = { onNavigate("editor:0") }, modifier = Modifier.fillMaxWidth()) { Text(tr("+ Neue Routine", "+ New routine")) }

    Spacer(modifier = Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onStart(null) },
            enabled = !hasActive,
            modifier = Modifier.weight(1f)
        ) { Text(tr("Freies Workout", "Free workout")) }

        OutlinedButton(onClick = { onNavigate("library") }, modifier = Modifier.weight(1f)) { Text(tr("Übungen", "Exercises")) }
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(onClick = { newPlan = true }, modifier = Modifier.fillMaxWidth()) { Text(tr("+ Neuer Trainingsplan", "+ New training plan")) }

    if (groups.isEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        EmptyHint(tr("Noch keine Routine. Lege zuerst unter „Übungen“ deine Übungen an und erstelle danach deine erste Routine, zum Beispiel „Push Day“.", "No routines yet. First create your exercises under “Exercises”, then build your first routine, for example “Push Day”."))
    }

    groups.forEach { group ->
        val plan = group.plan?.let { ref -> plans.firstOrNull { it.id == ref.id } }

        Row(verticalAlignment = Alignment.Bottom) {
            Box(modifier = Modifier.weight(1f)) {
                GroupLabel(plan?.name ?: if (groups.size == 1) tr("Routinen", "Routines") else tr("Ohne Plan", "No plan"))
            }
            if (plan != null) {
                OverflowMenu(
                    listOf(
                        MenuAction(tr("Routine hinzufügen", "Add routine")) { onNavigate("newin:${plan.id}") },
                        MenuAction(tr("Umbenennen", "Rename")) { renamePlan = plan },
                        MenuAction(tr("Duplizieren", "Duplicate")) { duplicatePlan = plan },
                        MenuAction(tr("Plan archivieren", "Archive plan")) { viewModel.setPlanArchived(plan.id, archived = true) },
                        MenuAction(tr("Plan löschen", "Delete plan"), destructive = true) { deletePlan = plan }
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
            EmptyHint(tr("Noch keine Routine in diesem Plan. Füge über ⋮ eine hinzu.", "No routines in this plan yet. Add one via ⋮."))
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
                            MenuAction(tr("Nach oben", "Move up")) { viewModel.moveRoutineInPlan(routine.id, up = true) }
                        else null,
                        if (plan != null && index < group.routines.lastIndex)
                            MenuAction(tr("Nach unten", "Move down")) { viewModel.moveRoutineInPlan(routine.id, up = false) }
                        else null,
                        MenuAction(tr("Archivieren", "Archive")) { viewModel.setRoutineArchived(routine.id, archived = true) }
                    )
                )
            }
        }
    }

    if (!archive.isEmpty) {
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { onNavigate("archive") }, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Archiv (${archive.count}) ›", "Archive (${archive.count}) ›"))
        }
    }

    if (newPlan || renamePlan != null) {
        val editing = renamePlan
        PlanNameDialog(
            title = if (editing == null) tr("Neuer Trainingsplan", "New training plan") else tr("Plan umbenennen", "Rename plan"),
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
            title = tr("Plan löschen?", "Delete plan?"),
            message = tr("Nur der Plan „${plan.name}“ wird gelöscht. Seine Routinen bleiben erhalten und sind danach ohne Plan.", "Only the plan “${plan.name}” is deleted. Its routines are kept and will have no plan."),
            confirmLabel = tr("Löschen", "Delete"),
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
        text = tr("Woche ${progress.week} · ", "Week ${progress.week} · ") +
                (if (progress.rounds == 1) tr("1 Durchgang", "1 round") else tr("${progress.rounds} Durchgänge", "${progress.rounds} rounds")) +
                tr(" · zuletzt ${progress.lastRoutineName} ", " · last ${progress.lastRoutineName} ") +
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
                text = tr("${progress.doneThisWeek} von ${progress.routineCount} diese Woche", "${progress.doneThisWeek} of ${progress.routineCount} this week"),
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
                text = if (items.isEmpty()) tr("Keine Übungen", "No exercises")
                else tr("${items.size} Übungen · ", "${items.size} exercises · ") +
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
                ) { Text(tr("Starten", "Start")) }

                TextButton(onClick = onEdit) { Text(tr("Bearbeiten", "Edit")) }

                OverflowMenu(menu)
            }
        }
    }
}
