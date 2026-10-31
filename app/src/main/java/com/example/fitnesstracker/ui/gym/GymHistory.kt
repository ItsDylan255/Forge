package com.example.fitnesstracker.ui.gym

import com.example.fitnesstracker.domain.gym.OneRepMax
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import com.example.fitnesstracker.i18n.L10n
import com.example.fitnesstracker.i18n.tr
import com.example.fitnesstracker.ui.weight.ForgeCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val shortDate: DateTimeFormatter get() = DateTimeFormatter.ofPattern(tr("dd.MM.", "dd/MM"), L10n.locale)

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
        EmptyHint(tr("Noch kein beendetes Training. Starte eine Routine, dann erscheint es hier.", "No finished workouts yet. Start a routine and it shows up here."))
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
        labels = listOf(tr("Pro Woche", "Per week"), tr("Pro Training", "Per workout")),
        selected = mode,
        onSelect = { mode = it }
    )

    Spacer(modifier = Modifier.height(12.dp))

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        GymBarChart(labels = labels, values = values, modifier = Modifier.fillMaxWidth())
    }

    Text(
        text = tr("Volumen = Gewicht × Wiederholungen", "Volume = weight × reps"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )

    GroupLabel(tr("Trainings", "Workouts"))

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
                                tr("${summary.setCount} Sätze", "${summary.setCount} sets")
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
        EmptyHint(tr("Sobald du ein Training beendet hast, siehst du hier deine Bestleistungen.", "Once you finish a workout, your personal bests show up here."))
        return
    }

    Text(
        text = tr("Tippe eine Übung an, um den Verlauf zu sehen.", "Tap an exercise to see its history."),
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
    SetSide.RIGHT -> tr(" \u00B7 Rechts", " · Right")
    SetSide.LEFT -> tr(" \u00B7 Links", " · Left")
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
            title = if (isSummary) tr("Workout beendet", "Workout finished") else session.routineName,
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
                label = tr("Dauer", "Duration"),
                value = summary.durationMillis?.let { GymFormat.duration(it) } ?: "—",
                modifier = Modifier.weight(1f)
            )
            StatBox(
                label = tr("Volumen", "Volume"),
                value = GymFormat.volume(summary.totalVolumeKg),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(
                label = tr("Übungen", "Exercises"),
                value = summary.exerciseCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatBox(
                label = tr("Sätze", "Sets"),
                value = summary.setCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        if (newBests.isNotEmpty()) {
            GroupLabel(tr("Neue Bestleistungen", "New personal bests"))

            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    newBests.forEach { best ->
                        Text(
                            text = "${best.exerciseName}: ${GymFormat.metric(best.kind, best.weightKg)} " +
                                    tr("(vorher ${GymFormat.metric(best.kind, best.previousKg)})", "(before ${GymFormat.metric(best.kind, best.previousKg)})"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        GroupLabel(tr("Sätze", "Sets"))

        if (groups.isEmpty()) {
            EmptyHint(tr("In diesem Training wurden keine Sätze gespeichert.", "No sets were saved in this workout."))
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
                                    text = tr("Satz $number${sideSuffix(gymSet.side)}", "Set $number${sideSuffix(gymSet.side)}"),
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
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(tr("Fertig", "Done")) }
        } else {
            TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Training löschen", "Delete workout"), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = tr("Training löschen?", "Delete workout?"),
            message = tr("Das Training und alle Sätze werden endgültig gelöscht.", "The workout and all its sets will be deleted permanently."),
            confirmLabel = tr("Löschen", "Delete"),
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

    // Gesamt-Verlauf: was in jedem Training insgesamt bei dieser Übung zusammenkam
    val totals = GymCalculator.exerciseTotals(exerciseId, finishedData, setData)
    val points = totals.points
    val best = GymCalculator.personalBests(finishedData, setData).firstOrNull { it.exerciseId == exerciseId }

    // Geschätztes Maximalgewicht (nur bei Übungen mit Wiederholungen und Sätzen bis 10 Wdh.)
    val oneRepMax = OneRepMax.best(exerciseId, finishedData, setData)
    val oneRepMaxPoints = OneRepMax.progress(exerciseId, finishedData, setData)
    var chartMode by rememberSaveable { mutableStateOf(0) } // 0 = Gesamt, 1 = 1RM
    var showTable by rememberSaveable { mutableStateOf(false) }

    GymPage {
        GymHeader(title = best?.exerciseName ?: tr("Übung", "Exercise"), onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        if (best != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox(
                    label = tr("Bestleistung", "Personal best"),
                    value = GymFormat.setText(best.kind, best.bestWeightKg, best.repsAtBest),
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = tr("Trainingstage", "Workout days"),
                    value = points.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            oneRepMax?.let { orm ->
                Spacer(modifier = Modifier.height(8.dp))
                StatBox(
                    label = tr("Geschätztes 1RM ›", "Estimated 1RM ›"),
                    value = GymFormat.kg(OneRepMax.roundToPlate(orm.estimateKg)),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showTable = true }
                )
                Text(
                    text = tr("Berechnet aus ${GymFormat.setText(orm.weightKg, orm.reps)} am ${GymFormat.date(orm.dateEpochDay)}", "Calculated from ${GymFormat.setText(orm.weightKg, orm.reps)} on ${GymFormat.date(orm.dateEpochDay)}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (oneRepMaxPoints.size >= 2) {
            GymTabs(
                labels = listOf(tr("Gesamt", "Total"), tr("Geschätztes 1RM", "Estimated 1RM")),
                selected = chartMode,
                onSelect = { chartMode = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        val showOneRepMax = chartMode == 1 && oneRepMaxPoints.size >= 2

        Text(
            text = if (showOneRepMax) tr("GESCHÄTZTES 1RM (KG)", "ESTIMATED 1RM (KG)") else GymFormat.totalLabel(totals.metric),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (showOneRepMax) {
            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                GymLineChart(
                    epochDays = oneRepMaxPoints.map { it.dateEpochDay },
                    values = oneRepMaxPoints.map { it.maxWeightKg },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (points.size >= 2) {
            ForgeCard(modifier = Modifier.fillMaxWidth()) {
                GymLineChart(
                    epochDays = points.map { it.dateEpochDay },
                    values = points.map { it.total },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            EmptyHint(tr("Für das Diagramm brauchst du mindestens zwei Trainings mit dieser Übung.", "The chart needs at least two workouts with this exercise."))
        }

        GroupLabel(tr("Pro Training", "Per workout"))

        // Neueste zuerst. Das Training davor steht in der Liste direkt dahinter.
        val newestFirst = points.reversed()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            newestFirst.forEachIndexed { index, point ->
                val previous = newestFirst.getOrNull(index + 1)

                ForgeCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = GymFormat.date(point.dateEpochDay),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = tr("${point.setCount} Sätze", "${point.setCount} sets"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text(
                                text = GymFormat.total(totals.metric, point.total),
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (previous != null) {
                                Text(
                                    text = GymFormat.totalDelta(
                                        totals.metric,
                                        point.total - previous.total,
                                        previous.total
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val tableFor = oneRepMax
    if (showTable && tableFor != null) {
        OneRepMaxDialog(oneRepMaxKg = tableFor.estimateKg, onDismiss = { showTable = false })
    }
}

/** Prozent-Tabelle zum geschätzten 1RM: welches Gewicht für wie viele Wiederholungen. */
@Composable
private fun OneRepMaxDialog(oneRepMaxKg: Double, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Geschätztes 1RM: ${GymFormat.kg(OneRepMax.roundToPlate(oneRepMaxKg))}", "Estimated 1RM: ${GymFormat.kg(OneRepMax.roundToPlate(oneRepMaxKg))}")) },
        text = {
            Column {
                OneRepMax.table(oneRepMaxKg).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${row.percent} %", style = MaterialTheme.typography.bodyMedium)
                        Text(GymFormat.kg(row.weightKg), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (row.reps == 1) tr("1 Wdh.", "1 rep") else tr("~${row.reps} Wdh.", "~${row.reps} reps"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tr("Schätzung nach Epley aus deinen Sätzen mit bis zu 10 Wiederholungen. ", "Epley estimate from your sets with up to 10 reps. ") +
                            tr("Kein Test nötig, aber nur ein Richtwert.", "No max test needed, but only a guideline."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Schließen", "Close")) } }
    )
}
