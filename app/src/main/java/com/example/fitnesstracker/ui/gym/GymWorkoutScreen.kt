package com.example.fitnesstracker.ui.gym

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnesstracker.data.local.GymRoutineExercise
import com.example.fitnesstracker.data.local.GymSet
import com.example.fitnesstracker.domain.gym.ExerciseKind
import com.example.fitnesstracker.domain.gym.GymCalculator
import com.example.fitnesstracker.domain.gym.GymDefaults
import com.example.fitnesstracker.domain.gym.LastPerformance
import com.example.fitnesstracker.domain.gym.LoggedSet
import com.example.fitnesstracker.domain.gym.OneRepMax
import com.example.fitnesstracker.domain.gym.PlanItem
import com.example.fitnesstracker.domain.gym.ProgressionHint
import com.example.fitnesstracker.domain.gym.SetSide
import com.example.fitnesstracker.domain.gym.WorkoutFlow
import com.example.fitnesstracker.domain.gym.WorkoutSlot
import com.example.fitnesstracker.domain.gym.toData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.fitnesstracker.domain.gym.GymFormat
import com.example.fitnesstracker.domain.gym.GymInputParser
import com.example.fitnesstracker.domain.gym.GymParseResult
import com.example.fitnesstracker.ui.weight.ForgeCard
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen(
    viewModel: GymViewModel,
    onFinished: (sessionId: Long) -> Unit,
    onExit: () -> Unit
) {
    val session by viewModel.activeSession.collectAsStateWithLifecycle()
    val sessionsLoaded by viewModel.sessionsLoaded.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val routineExercises by viewModel.routineExercises.collectAsStateWithLifecycle()
    val allSets by viewModel.sets.collectAsStateWithLifecycle()
    val allSessions by viewModel.sessions.collectAsStateWithLifecycle()
    val restTimer by viewModel.restTimer.collectAsStateWithLifecycle()


    // Benachrichtigung mit Restzeit: ab Android 13 muss sie erlaubt werden (Abfrage nur wenn nötig)
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var finishing by remember { mutableStateOf(false) }
    var extraIdsCsv by rememberSaveable { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf(-1L) }
    val stateHolder = rememberSaveableStateHolder()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    // Laufende Uhr (Trainingsdauer und Pausen-Countdown)
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            now = System.currentTimeMillis()
        }
    }

    // Die Pause beendet sich nach Ablauf selbst. Wird sie vorher übersprungen oder durch einen
    // neuen Satz ersetzt, ändert sich der Schlüssel und diese Wartezeit wird abgebrochen.
    LaunchedEffect(restTimer?.endsAtMillis) {
        val timer = restTimer ?: return@LaunchedEffect

        val wait = timer.endsAtMillis - System.currentTimeMillis()
        if (wait > 0) delay(wait)

        // Das Signal (Ton und Vibration) kommt vom Dienst, damit es auch bei gesperrtem Bildschirm
        // und nur einmal ertönt
        viewModel.skipRest()
    }

    val current = session
    if (current == null) {
        // Die Datenbank hat geantwortet und es läuft nichts: zurück zur Startseite
        if (sessionsLoaded && !finishing) {
            LaunchedEffect(Unit) { onExit() }
        }
        return
    }

    val sessionSets = allSets.filter { it.sessionId == current.id }.sortedBy { it.id }
    val plan = routineExercises
        .filter { it.routineId == current.routineId }
        .sortedBy { it.position }

    val extraIds = extraIdsCsv.split(",").mapNotNull { it.toLongOrNull() }
    val nameById = exercises.associate { it.id to it.name }
    val kindById = exercises.associate { it.id to ExerciseKind.fromCode(it.kind) }

    // Seiten: Gruppen der Routine (ein Superset = eine Seite), danach Übungen ohne Plan einzeln
    val planItems = plan.map {
        PlanItem(it.exerciseId, it.plannedSets, it.restSeconds, it.unilateral, it.supersetWithNext)
    }
    val planIds = planItems.map { it.exerciseId }.toSet()
    val pages: List<List<Long>> =
        WorkoutFlow.groups(planItems).map { group -> group.map { it.exerciseId } } +
            (sessionSets.map { it.exerciseId } + extraIds)
                .distinct()
                .filter { it !in planIds }
                .map { listOf(it) }
    val allPageIds = pages.flatten().toSet()

    Box(modifier = Modifier.fillMaxSize()) {
        GymPage {
            GymHeader(
                title = current.routineName,
                onBack = onExit,
                trailing = {
                    Text(
                        text = GymFormat.timer(now - current.startedAt),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (pages.isEmpty()) {
                EmptyHint("Füge eine Übung hinzu, um Sätze zu loggen.")
            }

            // Eine Seite pro Übung oder Superset. Eingetragene Sätze liegen in der Datenbank
            // und bleiben beim Wechsel erhalten.
            val pageIndex = pages.indexOfFirst { selectedId in it }.let { if (it < 0) 0 else it }
            val page = pages.getOrNull(pageIndex)

            if (page != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { selectedId = pages[pageIndex - 1].first() },
                        enabled = pageIndex > 0
                    ) { Text("\u2039", style = MaterialTheme.typography.titleLarge) }

                    Text(
                        text = "Übung ${pageIndex + 1} von ${pages.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = { selectedId = pages[pageIndex + 1].first() },
                        enabled = pageIndex < pages.size - 1
                    ) { Text("\u203A", style = MaterialTheme.typography.titleLarge) }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ablauf der Seite: Runde für Runde, einseitige Übungen erst rechts, dann links
                val groupItems = planItems.filter { it.exerciseId in page }
                val slots = WorkoutFlow.slots(groupItems)
                val activeSlot = WorkoutFlow.nextSlot(
                    slots,
                    sessionSets
                        .filter { it.exerciseId in page }
                        .map { LoggedSet(it.exerciseId, SetSide.fromCode(it.side)) }
                )

                if (page.size > 1) {
                    Text(
                        text = "Superset",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                }

                page.forEachIndexed { position, exerciseId ->
                    if (position > 0) Spacer(modifier = Modifier.height(12.dp))

                    // Eigener Zustand pro Übung, damit angefangene Eingaben beim Wechseln bleiben
                    stateHolder.SaveableStateProvider(exerciseId) {
                        val setsOfExercise = sessionSets.filter { it.exerciseId == exerciseId }
                        val lastSet = allSets.filter { it.exerciseId == exerciseId }.maxByOrNull { it.id }
                        val name = nameById[exerciseId]
                            ?: setsOfExercise.lastOrNull()?.exerciseName
                            ?: "Übung"
                        val planned = plan.firstOrNull { it.exerciseId == exerciseId }
                        val kind = kindById[exerciseId] ?: ExerciseKind.REPS

                        val sessionData = allSessions.map { it.toData() }
                        val setData = allSets.map { it.toData() }
                        val last = GymCalculator.lastPerformance(exerciseId, current.id, sessionData, setData)
                        val record = if (kind == ExerciseKind.REPS) {
                            OneRepMax.newRecord(exerciseId, current.id, sessionData, setData)
                        } else {
                            null
                        }
                        val hint = if (planned != null && last != null) {
                            // Einseitige Übungen haben pro Satz zwei Einträge (rechts und links)
                            val setsPerRound = if (planned.unilateral) 2 else 1
                            GymCalculator.progressionHint(
                                planned.plannedSets * setsPerRound, planned.repsMin, planned.repsMax, last.sets
                            )
                        } else {
                            ProgressionHint.NONE
                        }

                        ExerciseBlock(
                            exerciseId = exerciseId,
                            name = name,
                            planned = planned,
                            kind = kind,
                            sets = setsOfExercise,
                            slots = slots.filter { it.exerciseId == exerciseId },
                            activeSlot = activeSlot,
                            lastSet = lastSet,
                            last = last,
                            hint = hint,
                            record = record,
                            restActive = restTimer != null,
                            onAddSet = { weightKg, reps, side, restSeconds ->
                                viewModel.addSet(current.id, exerciseId, name, weightKg, reps, side)
                                // Pause nur am Ende einer Runde (0 = direkt weiter, z. B. im Superset)
                                viewModel.startRest(name, restSeconds)
                            },
                            onDeleteSet = { viewModel.deleteSet(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("+ Übung hinzufügen")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { showFinish = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Workout beenden")
            }

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(onClick = { showDiscard = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Workout verwerfen", color = MaterialTheme.colorScheme.error)
            }

            // Platz, damit die Pausen-Leiste unten nichts verdeckt
            if (restTimer != null) {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }

        restTimer?.let { timer ->
            RestTimerBar(
                timer = timer,
                now = now,
                onSkip = { viewModel.skipRest() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            viewModel = viewModel,
            exercises = exercises,
            excludeIds = allPageIds,
            onPick = { id, _, _ ->
                selectedId = id
                extraIdsCsv = if (extraIdsCsv.isEmpty()) "$id" else "$extraIdsCsv,$id"
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }

    if (showFinish) {
        if (sessionSets.isEmpty()) {
            ConfirmDialog(
                title = "Noch keine Sätze",
                message = "Ohne geloggte Sätze lässt sich das Workout nicht speichern. Verwerfen?",
                confirmLabel = "Verwerfen",
                onConfirm = {
                    showFinish = false
                    finishing = true
                    viewModel.discardSession(current.id) { onExit() }
                },
                onDismiss = { showFinish = false }
            )
        } else {
            ConfirmDialog(
                title = "Workout beenden?",
                message = "Die Dauer wird gespeichert und du siehst die Zusammenfassung.",
                confirmLabel = "Beenden",
                destructive = false,
                onConfirm = {
                    showFinish = false
                    finishing = true
                    viewModel.finishSession(current.id) { onFinished(current.id) }
                },
                onDismiss = { showFinish = false }
            )
        }
    }

    if (showDiscard) {
        ConfirmDialog(
            title = "Workout verwerfen?",
            message = "Alle Sätze dieses Workouts werden gelöscht.",
            confirmLabel = "Verwerfen",
            onConfirm = {
                showDiscard = false
                finishing = true
                viewModel.discardSession(current.id) { onExit() }
            },
            onDismiss = { showDiscard = false }
        )
    }
}

/**
 * Feste Leiste am unteren Rand, solange eine Pause läuft: Restzeit groß, ein Balken
 * und ein Knopf zum sofortigen Beenden. Die Leiste verschwindet von selbst, wenn die Zeit um ist.
 */
@Composable
private fun RestTimerBar(
    timer: RestTimer,
    now: Long,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remaining = (timer.endsAtMillis - now).coerceAtLeast(0L)
    // Aufrunden, damit z. B. 1:30 als erstes angezeigt wird und nicht schon 1:29
    val shownMillis = (remaining + 999L) / 1000L * 1000L
    val fraction = if (timer.totalMillis > 0L) remaining.toFloat() / timer.totalMillis else 0f

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PAUSE · ${timer.exerciseName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = GymFormat.timer(shownMillis),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Button(onClick = onSkip) { Text("Überspringen") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

private fun dateText(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY))

private fun sideLabel(side: SetSide): String = when (side) {
    SetSide.BOTH -> ""
    SetSide.RIGHT -> " \u00B7 Rechts"
    SetSide.LEFT -> " \u00B7 Links"
}

/** Eine Zeile der Satzliste. [done] = schon geloggt, [active] = hier wird gerade eingetragen. */
private data class SetRow(
    val label: String,
    val side: SetSide,
    val restSeconds: Int,
    val done: GymSet?,
    val active: Boolean
)

/**
 * Eine Übung mit allen Sätzen laut Ablauf. Nur der nächste offene Satz der ganzen Seite
 * ([activeSlot]) hat Eingabefelder, die übrigen sind gesperrt. Solange eine Pause läuft
 * ([restActive]), ist auch der offene Satz gesperrt.
 */
@Composable
private fun ExerciseBlock(
    exerciseId: Long,
    name: String,
    planned: GymRoutineExercise?,
    kind: ExerciseKind,
    sets: List<GymSet>,
    slots: List<WorkoutSlot>,
    activeSlot: WorkoutSlot?,
    lastSet: GymSet?,
    last: LastPerformance?,
    hint: ProgressionHint,
    /** Neues geschätztes 1RM in diesem Training (neu, vorher), sonst null. */
    record: Pair<Double, Double>?,
    restActive: Boolean,
    onAddSet: (weightKg: Double, reps: Int, side: SetSide, restSeconds: Int) -> Unit,
    onDeleteSet: (Long) -> Unit
) {
    var weightInput by rememberSaveable(exerciseId) {
        // Zusatzgewicht und Unterstützung sind optional: 0 bleibt leer
        mutableStateOf(
            lastSet?.takeIf { kind == ExerciseKind.REPS || it.weightKg != 0.0 }
                ?.let { GymFormat.number(it.weightKg) } ?: ""
        )
    }
    var repsInput by rememberSaveable(exerciseId) {
        mutableStateOf(lastSet?.reps?.toString() ?: planned?.repsMin?.toString() ?: "")
    }
    var error by rememberSaveable(exerciseId) { mutableStateOf<String?>(null) }

    // Stoppuhr für Zeitübungen: 0 = aus, sonst Startzeit in Millisekunden
    var stopwatchStart by rememberSaveable(exerciseId) { mutableStateOf(0L) }
    var tick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(stopwatchStart) {
        while (stopwatchStart != 0L) {
            tick = System.currentTimeMillis()
            delay(250)
        }
    }

    fun addSet(side: SetSide, restSeconds: Int) {
        val weightResult = if (kind == ExerciseKind.REPS) {
            GymInputParser.parseWeight(weightInput)
        } else {
            GymInputParser.parseOptionalWeight(weightInput)
        }
        val weight = when (weightResult) {
            is GymParseResult.Valid -> weightResult.value
            is GymParseResult.Invalid -> {
                error = weightResult.message
                return
            }
        }

        val repsResult = if (kind == ExerciseKind.TIME) {
            GymInputParser.parseSeconds(repsInput)
        } else {
            GymInputParser.parseReps(repsInput)
        }
        val reps = when (repsResult) {
            is GymParseResult.Valid -> repsResult.value
            is GymParseResult.Invalid -> {
                error = repsResult.message
                return
            }
        }

        error = null
        stopwatchStart = 0L
        onAddSet(weight, reps, side, restSeconds)
    }

    val rangeText = planned?.let {
        if (kind == ExerciseKind.TIME) GymFormat.repsRange(kind, it.repsMin, it.repsMax)
        else if (it.repsMin == it.repsMax) "${it.repsMin} Wdh."
        else "${it.repsMin}\u2013${it.repsMax} Wdh."
    }
    val weightLabel = when (kind) {
        ExerciseKind.REPS -> "kg"
        ExerciseKind.ASSISTED -> "Unterstützung"
        ExerciseKind.TIME -> "Zusatz (kg)"
    }

    // Geplante Übung: Zeilen aus dem Ablauf. Freie Übung: geloggte Sätze plus immer ein offener Satz.
    val rows: List<SetRow> = if (planned != null) {
        slots.map { slot ->
            val done = sets
                .filter { it.side == slot.side.code }
                .sortedBy { it.id }
                .getOrNull(slot.setNumber - 1)
            SetRow(
                label = "Satz ${slot.setNumber}${sideLabel(slot.side)}",
                side = slot.side,
                restSeconds = slot.restAfterSeconds,
                done = done,
                active = done == null && slot == activeSlot
            )
        }
    } else {
        sets.mapIndexed { i, s ->
            SetRow("Satz ${i + 1}", SetSide.BOTH, GymDefaults.REST_SECONDS, s, false)
        } + SetRow("Satz ${sets.size + 1}", SetSide.BOTH, GymDefaults.REST_SECONDS, null, true)
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = name, style = MaterialTheme.typography.titleLarge)

            if (planned != null) {
                Text(
                    text = "${GymFormat.plan(kind, planned.plannedSets, planned.repsMin, planned.repsMax)}" +
                            (if (planned.unilateral) " \u00B7 einseitig" else "") +
                            " \u00B7 Pause ${GymFormat.restLabel(planned.restSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---------- Letztes Training ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                if (last != null) {
                    Text(
                        text = "Letztes Training \u00B7 ${dateText(last.dateEpochDay)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = muted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val counters = mutableMapOf<SetSide, Int>()
                    last.sets.forEach { s ->
                        val number = (counters[s.side] ?: 0) + 1
                        counters[s.side] = number
                        Text(
                            text = "Satz $number${sideLabel(s.side)}: ${GymFormat.setText(s.kind, s.weightKg, s.reps)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Text(
                        text = "Noch kein Training mit dieser \u00DCbung.",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }
            }

            // ---------- Neuer Rekord (geschätztes 1RM) ----------
            record?.let { (now, before) ->
                Text(
                    text = "Neuer Rekord! Geschätztes 1RM ${GymFormat.kg(OneRepMax.roundToPlate(now))} " +
                            "(vorher ${GymFormat.kg(OneRepMax.roundToPlate(before))})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // ---------- Dezenter Hinweis zur Progression ----------
            GymFormat.progressionText(kind, hint, planned?.repsMin ?: 0, planned?.repsMax ?: 0)?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---------- Sätze ----------
            rows.forEach { row ->
                val done = row.done

                when {
                    done != null -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = row.label, style = MaterialTheme.typography.bodySmall, color = muted)
                        Text(
                            text = GymFormat.setText(kind, done.weightKg, done.reps),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { onDeleteSet(done.id) }) {
                            Text("\u2715", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    row.active -> Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = row.label, style = MaterialTheme.typography.titleMedium)
                            if (rangeText != null) {
                                Text(text = rangeText, style = MaterialTheme.typography.bodySmall, color = muted)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = {
                                    weightInput = it
                                    error = null
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(weightLabel) },
                                singleLine = true,
                                enabled = !restActive,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = repsInput,
                                onValueChange = {
                                    repsInput = it
                                    error = null
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(if (kind == ExerciseKind.TIME) "Sekunden" else "Wdh.") },
                                singleLine = true,
                                enabled = !restActive,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        if (kind == ExerciseKind.TIME) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (stopwatchStart == 0L) {
                                            stopwatchStart = System.currentTimeMillis()
                                            tick = stopwatchStart
                                        } else {
                                            // Stopp: die gehaltenen Sekunden landen im Eingabefeld
                                            val seconds = ((System.currentTimeMillis() - stopwatchStart) / 1000L)
                                                .toInt()
                                                .coerceIn(1, GymInputParser.MAX_SECONDS)
                                            repsInput = seconds.toString()
                                            stopwatchStart = 0L
                                            error = null
                                        }
                                    },
                                    enabled = !restActive,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (stopwatchStart == 0L) "Stoppuhr starten" else "Stoppen")
                                }

                                if (stopwatchStart != 0L) {
                                    Text(
                                        text = GymFormat.timer(tick - stopwatchStart),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                        }

                        error?.let { message ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { addSet(row.side, row.restSeconds) },
                            enabled = !restActive,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (restActive) "Pause l\u00E4uft \u2026" else "Satz speichern")
                        }
                    }

                    else -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = row.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = muted.copy(alpha = 0.6f)
                        )
                        if (rangeText != null) {
                            Text(
                                text = rangeText,
                                style = MaterialTheme.typography.bodySmall,
                                color = muted.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            if (planned != null && rows.isNotEmpty() && rows.all { it.done != null }) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Alle S\u00E4tze erledigt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }
        }
    }
}
