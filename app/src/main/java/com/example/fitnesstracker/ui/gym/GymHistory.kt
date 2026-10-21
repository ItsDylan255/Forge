package com.example.fitnesstracker.ui.gym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnesstracker.data.local.GymSession
import com.example.fitnesstracker.domain.gym.ExerciseBest
import com.example.fitnesstracker.domain.gym.GymCalculator
import com.example.fitnesstracker.domain.gym.ExerciseKind
import com.example.fitnesstracker.domain.gym.GymFormat
import com.example.fitnesstracker.domain.gym.SetSide
import com.example.fitnesstracker.domain.gym.WorkoutSessionData
import com.example.fitnesstracker.domain.gym.WorkoutSetData
import com.example.fitnesstracker.domain.gym.toData
import com.example.fitnesstracker.ui.weight.ForgeCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMANY)

// ------------------------------------------------------------
// Tab "Verlauf": Volumen-Diagramm + Liste vergangener Einheiten
// ------------------------------------------------------------

@Composable
internal fun HistoryTab(
    sessions: List<GymSession>,
    sessionData: List<WorkoutSessionData>,
    setData: List<WorkoutSetData>,
    today: Long,
    onOpen: (Long) -> Unit
) {
    var mode by rememberSaveable { mutableStateOf(0) } // 0 = pro Woche, 1 = pro Training

    if (sessions.isEmpty()) {
        EmptyHint("Noch kein beendetes Training. Starte eine Routine, dann erscheint es hier.")
        return
    }

    val labels: List<String>
    val values: List<Double>

    if (mode == 0) {
        val weeks = GymCalculator.weeklyVolumes(sessionData, setData, 8, today)
        labels = weeks.map { LocalDate.ofEpochDay(it.startEpochDay).format(shortDate) }
        values = weeks.map { it.volumeKg }
    } else {
        val perSession = GymCalculator.sessionVolumes(sessionData, setData, 10)
        labels = perSession.map { LocalDate.ofEpochDay(it.dateEpochDay).format(shortDate) }
        values = perSession.map { it.volumeKg }
    }

    GymTabs(
        labels = listOf("Pro Woche", "Pro Training"),
        selected = mode,
        onSelect = { mode = it }
    )

    Spacer(modifier = Modifier.height(12.dp))

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        GymBarChart(labels = labels, values = values, modifier = Modifier.fillMaxWidth())
    }

    Text(
        text = "Volumen = Gewicht × Wiederholungen",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )

    GroupLabel("Trainings")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sessions.forEach { session ->
            val summary = GymCalculator.summarize(session.toData(), setData)

            ForgeCard(modifier = Modifier.fillMaxWidth(), onClick = { onOpen(session.id) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = session.routineName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = GymFormat.date(session.dateEpochDay),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = GymFormat.volume(summary.totalVolumeKg),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = listOfNotNull(
                                summary.durationMillis?.let { GymFormat.duration(it) },
                                "${summary.setCount} Sätze"
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------
// Tab "Fortschritt": Bestleistungen pro Übung
// ------------------------------------------------------------

@Composable
internal fun ProgressTab(
    bests: List<ExerciseBest>,
    onOpen: (exerciseId: Long) -> Unit
) {
    if (bests.isEmpty()) {
        EmptyHint("Sobald du ein Training beendet hast, siehst du hier deine Bestleistungen.")
        return
    }

    Text(
        text = "Tippe eine Übung an, um den Verlauf zu sehen.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        bests.forEach { best ->
            ForgeCard(modifier = Modifier.fillMaxWidth(), onClick = { onOpen(best.exerciseId) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = best.exerciseName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = GymFormat.date(best.dateEpochDay),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = GymFormat.setText(best.kind, best.bestWeightKg, best.repsAtBest),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------
// Detail einer Einheit (auch als Zusammenfassung direkt nach dem Beenden)
// ------------------------------------------------------------

private fun sideSuffix(code: Int): String = when (SetSide.fromCode(code)) {
    SetSide.BOTH -> ""
    SetSide.RIGHT -> " \u00B7 Rechts"
    SetSide.LEFT -> " \u00B7 Links"
}

@Composable
fun SessionDetailScreen(
    viewModel: GymViewModel,
    sessionId: Long,
    isSummary: Boolean,
    onClose: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val sets by viewModel.sets.collectAsStateWithLifecycle()
    val sessionsLoaded by viewModel.sessionsLoaded.collectAsStateWithLifecycle()

    var showDelete by rememberSaveable { mutableStateOf(false) }

    val session = sessions.firstOrNull { it.id == sessionId }

    if (session == null) {
        if (sessionsLoaded) LaunchedEffect(Unit) { onClose() }
        return
    }

    val finished = sessions.filter { it.endedAt != null }
    val finishedData = finished.map { it.toData() }
    val allSetData = sets.map { it.toData() }
    val ownSets = sets.filter { it.sessionId == sessionId }.sortedBy { it.id }

    val summary = GymCalculator.summarize(session.toData(), allSetData)
    val newBests = GymCalculator.newBestsIn(sessionId, finishedData, allSetData)
    val groups = ownSets.groupBy { it.exerciseId }

    GymPage {
        GymHeader(
            title = if (isSummary) "Workout beendet" else session.routineName,
            onBack = if (isSummary) null else onClose
        )

        Text(
            text = if (isSummary) "${session.routineName} · ${GymFormat.date(session.dateEpochDay)}"
            else GymFormat.date(session.dateEpochDay),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(
                label = "Dauer",
                value = summary.durationMillis?.let { GymFormat.duration(it) } ?: "—",
                modifier = Modifier.weight(1f)
            )
            StatBox(
                label = "Volumen",
                value = GymFormat.volume(summary.totalVolumeKg),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(
                label = "Übungen",
                value = summary.exerciseCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatBox(
                label = "Sätze",
                value = summary.setCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        if (newBests.isNotEmpty()) {
            GroupLabel("Neue Bestleistungen")

            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    newBests.forEach { best ->
                        Text(
                            text = "${best.exerciseName}: ${GymFormat.metric(best.kind, best.weightKg)} " +
                                    "(vorher ${GymFormat.metric(best.kind, best.previousKg)})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        GroupLabel("Sätze")

        if (groups.isEmpty()) {
            EmptyHint("In diesem Training wurden keine Sätze gespeichert.")
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            groups.forEach { (_, list) ->
                val volume = list.sumOf { GymCalculator.setVolume(it.toData()) }
                val kind = ExerciseKind.fromCode(list.last().kind)

                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = list.last().exerciseName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            // Zeitübungen haben kein Volumen
                            if (kind != ExerciseKind.TIME) {
                                Text(
                                    text = GymFormat.volume(volume),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Einseitige Übungen zählen die Sätze je Seite
                        val counters = mutableMapOf<Int, Int>()
                        list.forEach { gymSet ->
                            val number = (counters[gymSet.side] ?: 0) + 1
                            counters[gymSet.side] = number
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Satz $number${sideSuffix(gymSet.side)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = GymFormat.setText(ExerciseKind.fromCode(gymSet.kind), gymSet.weightKg, gymSet.reps),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isSummary) {
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Fertig") }
        } else {
            TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Training löschen", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Training löschen?",
            message = "Das Training und alle Sätze werden endgültig gelöscht.",
            confirmLabel = "Löschen",
            onConfirm = {
                showDelete = false
                viewModel.deleteSession(sessionId)
                onClose()
            },
            onDismiss = { showDelete = false }
        )
    }
}

// ------------------------------------------------------------
// Fortschritt einer einzelnen Übung (Gewicht über Zeit)
// ------------------------------------------------------------

@Composable
fun ExerciseProgressScreen(
    viewModel: GymViewModel,
    exerciseId: Long,
    onBack: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val sets by viewModel.sets.collectAsStateWithLifecycle()

    val finishedData = sessions.filter { it.endedAt != null }.map { it.toData() }
    val setData = sets.map { it.toData() }

    val points = GymCalculator.exerciseProgress(exerciseId, finishedData, setData)
    val best = GymCalculator.personalBests(finishedData, setData).firstOrNull { it.exerciseId == exerciseId }

    GymPage {
        GymHeader(title = best?.exerciseName ?: "Übung", onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        if (best != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox(
                    label = "Bestleistung",
                    value = GymFormat.setText(best.bestWeightKg, best.repsAtBest),
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Trainingstage",
                    value = points.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = when (best?.kind) {
                ExerciseKind.TIME -> "LÄNGSTE ZEIT PRO TRAINING"
                ExerciseKind.ASSISTED -> "GERINGSTE UNTERSTÜTZUNG PRO TRAINING"
                else -> "HÖCHSTGEWICHT PRO TRAINING"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (points.size >= 2) {
            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                GymLineChart(
                    epochDays = points.map { it.dateEpochDay },
                    values = points.map { it.maxWeightKg },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            EmptyHint("Für das Diagramm brauchst du mindestens zwei Trainings mit dieser Übung.")
        }

        GroupLabel("Pro Training")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            points.reversed().forEach { point ->
                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = GymFormat.date(point.dateEpochDay),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = GymFormat.metric(best?.kind ?: ExerciseKind.REPS, point.maxWeightKg),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
