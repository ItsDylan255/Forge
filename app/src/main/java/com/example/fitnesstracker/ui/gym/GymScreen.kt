package com.example.fitnesstracker.ui.gym

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnesstracker.data.local.GymExercise
import com.example.fitnesstracker.data.local.GymRoutine
import com.example.fitnesstracker.data.local.GymRoutineExercise
import com.example.fitnesstracker.data.local.GymSession
import com.example.fitnesstracker.domain.gym.GymFormat
import com.example.fitnesstracker.domain.gym.toData
import com.example.fitnesstracker.ui.weight.ForgeCard
import java.time.LocalDate

/**
 * Einstieg des Gym-Tabs. Die Unterseiten werden über eine einfache Route gesteuert:
 * home, library, editor:<id|0>, workout, session:<id>, summary:<id>, exercise:<id>
 */
@Composable
fun GymScreen(viewModel: GymViewModel) {
    var route by rememberSaveable { mutableStateOf("home") }
    var homeTab by rememberSaveable { mutableStateOf(0) }

    BackHandler(enabled = route != "home") { route = "home" }

    when {
        route == "library" ->
            ExerciseLibraryScreen(viewModel = viewModel, onBack = { route = "home" })

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
                routines = routines,
                routineItems = routineExercises,
                exercises = exercises,
                hasActive = active != null,
                onStart = { routineId ->
                    viewModel.startSession(routineId) { onNavigate("workout") }
                },
                onEdit = { onNavigate("editor:$it") },
                onNew = { onNavigate("editor:0") },
                onLibrary = { onNavigate("library") }
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
    routines: List<GymRoutine>,
    routineItems: List<GymRoutineExercise>,
    exercises: List<GymExercise>,
    hasActive: Boolean,
    onStart: (Long?) -> Unit,
    onEdit: (Long) -> Unit,
    onNew: () -> Unit,
    onLibrary: () -> Unit
) {
    val nameById = exercises.associate { it.id to it.name }

    Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("+ Neue Routine") }

    Spacer(modifier = Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onStart(null) },
            enabled = !hasActive,
            modifier = Modifier.weight(1f)
        ) { Text("Freies Workout") }

        OutlinedButton(onClick = onLibrary, modifier = Modifier.weight(1f)) { Text("Übungen") }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (routines.isEmpty()) {
        EmptyHint("Noch keine Routine. Lege zuerst unter „Übungen“ deine Übungen an und erstelle danach deine erste Routine, zum Beispiel „Push Day“.")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        routines.forEach { routine ->
            val items = routineItems.filter { it.routineId == routine.id }.sortedBy { it.position }
            val preview = items.mapNotNull { nameById[it.exerciseId] }

            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = routine.name, style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (items.isEmpty()) "Keine Übungen"
                        else "${items.size} Übungen · " +
                                preview.take(3).joinToString(", ") +
                                if (preview.size > 3) " …" else "",
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
                            onClick = { onStart(routine.id) },
                            enabled = !hasActive && items.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("Starten") }

                        TextButton(onClick = { onEdit(routine.id) }) { Text("Bearbeiten") }
                    }
                }
            }
        }
    }
}
