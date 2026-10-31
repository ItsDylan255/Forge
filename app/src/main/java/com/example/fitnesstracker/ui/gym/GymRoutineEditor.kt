package com.example.fitnesstracker.ui.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnesstracker.domain.gym.MuscleGroups
import com.example.fitnesstracker.domain.gym.ExerciseKind
import com.example.fitnesstracker.domain.gym.GymDefaults
import com.example.fitnesstracker.domain.gym.GymFormat
import com.example.fitnesstracker.domain.gym.GymInputParser
import com.example.fitnesstracker.domain.gym.GymParseResult
import com.example.fitnesstracker.i18n.tr
import com.example.fitnesstracker.ui.weight.ForgeCard

private data class RoutineItemDraft(
    val key: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String?,
    val setsInput: String,
    val repsMinInput: String,
    val repsMaxInput: String,
    val restSeconds: Int,
    val unilateral: Boolean = false,
    val supersetWithNext: Boolean = false,
    /** Neu hinzugefügt: Standardwerte der Art werden gesetzt, sobald die Übung in der Liste ist. */
    val defaultsPending: Boolean = false
)

/** Teilt die Liste in Gruppen: Übungen mit Superset-Häkchen gehören mit der nächsten zusammen. */
private fun groupRanges(flags: List<Boolean>): List<IntRange> {
    val result = mutableListOf<IntRange>()
    var start = 0
    flags.forEachIndexed { i, link ->
        if (!(link && i < flags.lastIndex)) {
            result.add(start..i)
            start = i + 1
        }
    }
    return result
}

@Composable
fun RoutineEditorScreen(
    viewModel: GymViewModel,
    routineId: Long?,
    onDone: () -> Unit,
    /** Vorauswahl des Plans beim Anlegen (z. B. „+ Routine“ unter einem Plan). */
    initialPlanId: Long? = null
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var planId by remember { mutableStateOf(initialPlanId) }
    var showPlanPicker by remember { mutableStateOf(false) }
    val drafts = remember { mutableStateListOf<RoutineItemDraft>() }
    var nextKey by remember { mutableStateOf(1L) }
    var loaded by remember { mutableStateOf(routineId == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    fun kindOf(exerciseId: Long): ExerciseKind =
        exercises.firstOrNull { it.id == exerciseId }?.let { ExerciseKind.fromCode(it.kind) }
            ?: ExerciseKind.REPS

    // Neu hinzugefügte Zeitübungen starten mit 30-60 Sekunden statt 8-12 Wiederholungen
    LaunchedEffect(exercises, drafts.size) {
        drafts.forEachIndexed { index, draft ->
            if (draft.defaultsPending) {
                val exercise = exercises.firstOrNull { it.id == draft.exerciseId }
                if (exercise != null) {
                    drafts[index] = if (ExerciseKind.fromCode(exercise.kind) == ExerciseKind.TIME) {
                        draft.copy(repsMinInput = "30", repsMaxInput = "60", defaultsPending = false)
                    } else {
                        draft.copy(defaultsPending = false)
                    }
                }
            }
        }
    }

    // Beim Bearbeiten: vorhandene Routine einmal laden
    LaunchedEffect(routineId) {
        if (routineId != null) {
            viewModel.loadRoutineForEdit(routineId)?.let { data ->
                name = data.name
                planId = data.planId
                drafts.clear()
                data.items.forEach { item ->
                    drafts.add(
                        RoutineItemDraft(
                            key = nextKey++,
                            exerciseId = item.exerciseId,
                            exerciseName = item.exerciseName,
                            muscleGroup = item.muscleGroup,
                            setsInput = item.plannedSets.toString(),
                            repsMinInput = item.repsMin.toString(),
                            repsMaxInput = item.repsMax.toString(),
                            restSeconds = item.restSeconds,
                            unilateral = item.unilateral,
                            supersetWithNext = item.supersetWithNext
                        )
                    )
                }
            }
            loaded = true
        }
    }

    // Das Superset-Häkchen an der letzten Übung hat keine Wirkung und wird zurückgesetzt
    fun fixLast() {
        val last = drafts.lastIndex
        if (last >= 0 && drafts[last].supersetWithNext) {
            drafts[last] = drafts[last].copy(supersetWithNext = false)
        }
    }

    @Composable
    fun DraftAt(index: Int) {
        DraftCard(
            draft = drafts[index],
            kind = kindOf(drafts[index].exerciseId),
            canMoveUp = index > 0,
            canMoveDown = index < drafts.lastIndex,
            canLink = index < drafts.lastIndex,
            onChange = { updated ->
                drafts[index] = updated
                error = null
            },
            onUp = {
                val moved = drafts[index]
                drafts[index] = drafts[index - 1]
                drafts[index - 1] = moved
                fixLast()
            },
            onDown = {
                val moved = drafts[index]
                drafts[index] = drafts[index + 1]
                drafts[index + 1] = moved
                fixLast()
            },
            onRemove = {
                // Endete mit dieser Übung eine Gruppe, darf die vorherige nicht an die nächste Gruppe rutschen
                val removed = drafts[index]
                if (!removed.supersetWithNext && index > 0 && drafts[index - 1].supersetWithNext) {
                    drafts[index - 1] = drafts[index - 1].copy(supersetWithNext = false)
                }
                drafts.removeAt(index)
                fixLast()
            }
        )
    }

    fun save() {
        val cleanName = when (val result = GymInputParser.parseName(name)) {
            is GymParseResult.Valid -> result.value
            is GymParseResult.Invalid -> {
                error = result.message
                return
            }
        }

        if (drafts.isEmpty()) {
            error = tr("Füge mindestens eine Übung hinzu.", "Add at least one exercise.")
            return
        }

        val inputs = mutableListOf<RoutineItemInput>()
        for (draft in drafts) {
            val sets = when (val result = GymInputParser.parsePlannedSets(draft.setsInput)) {
                is GymParseResult.Valid -> result.value
                is GymParseResult.Invalid -> {
                    error = "${draft.exerciseName}: ${result.message}"
                    return
                }
            }
            val bounds = if (kindOf(draft.exerciseId) == ExerciseKind.TIME) {
                GymInputParser.parseSecondsBounds(draft.repsMinInput, draft.repsMaxInput)
            } else {
                GymInputParser.parseRepsBounds(draft.repsMinInput, draft.repsMaxInput)
            }
            val reps = when (val result = bounds) {
                is GymParseResult.Valid -> result.value
                is GymParseResult.Invalid -> {
                    error = "${draft.exerciseName}: ${result.message}"
                    return
                }
            }
            inputs.add(
                RoutineItemInput(
                    exerciseId = draft.exerciseId,
                    plannedSets = sets,
                    repsMin = reps.first,
                    repsMax = reps.last,
                    restSeconds = draft.restSeconds,
                    unilateral = draft.unilateral,
                    supersetWithNext = draft.supersetWithNext
                )
            )
        }

        error = null
        viewModel.saveRoutine(routineId, cleanName, planId, inputs) { onDone() }
    }

    GymPage {
        GymHeader(
            title = if (routineId == null) tr("Neue Routine", "New routine") else tr("Routine bearbeiten", "Edit routine"),
            onBack = onDone
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!loaded) {
            EmptyHint(tr("Lädt …", "Loading …"))
            return@GymPage
        }

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tr("Name der Routine", "Routine name")) },
            placeholder = { Text(tr("z. B. Push Day", "e.g. Push Day")) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = { showPlanPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = tr("Trainingsplan: ", "Training plan: ") +
                        (plans.firstOrNull { it.id == planId }?.name ?: tr("Kein Plan", "No plan"))
            )
        }

        GroupLabel(tr("Übungen", "Exercises"))

        if (drafts.isEmpty()) {
            EmptyHint(tr("Noch keine Übung. Füge unten die erste hinzu.", "No exercises yet. Add the first one below."))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            groupRanges(drafts.map { it.supersetWithNext }).forEach { range ->
                if (range.first == range.last) {
                    DraftAt(range.first)
                } else {
                    // Superset: verbundene Übungen mit Klammerlinie links
                    Column {
                        Text(
                            text = "Superset",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 11.dp, bottom = 4.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (i in range) DraftAt(i)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(tr("+ Übung hinzufügen", "+ Add exercise"))
        }

        error?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Text(tr("Routine speichern", "Save routine")) }

        if (routineId != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    viewModel.setRoutineArchived(routineId, archived = true)
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(tr("Routine archivieren", "Archive routine")) }

            TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Routine löschen", "Delete routine"), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            viewModel = viewModel,
            exercises = exercises,
            excludeIds = drafts.map { it.exerciseId }.toSet(),
            onPick = { id, exerciseName, muscleGroup ->
                drafts.add(
                    RoutineItemDraft(
                        key = nextKey++,
                        exerciseId = id,
                        exerciseName = exerciseName,
                        muscleGroup = muscleGroup,
                        setsInput = "3",
                        repsMinInput = "8",
                        repsMaxInput = "12",
                        restSeconds = GymDefaults.REST_SECONDS,
                        defaultsPending = true
                    )
                )
                error = null
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }

    if (showPlanPicker) {
        PlanPickerDialog(
            viewModel = viewModel,
            plans = plans,
            selectedId = planId,
            onPick = {
                planId = it
                showPlanPicker = false
            },
            onDismiss = { showPlanPicker = false }
        )
    }

    if (showDelete && routineId != null) {
        ConfirmDialog(
            title = tr("Routine löschen?", "Delete routine?"),
            message = tr("Die Routine wird gelöscht. Bereits absolvierte Trainings im Verlauf bleiben erhalten.", "The routine will be deleted. Completed workouts in your history are kept."),
            confirmLabel = tr("Löschen", "Delete"),
            onConfirm = {
                viewModel.deleteRoutine(routineId)
                showDelete = false
                onDone()
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun DraftCard(
    draft: RoutineItemDraft,
    kind: ExerciseKind,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canLink: Boolean,
    onChange: (RoutineItemDraft) -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = draft.exerciseName, style = MaterialTheme.typography.titleMedium)
                    val subtitle = listOfNotNull(
                        draft.muscleGroup?.let(MuscleGroups::label),
                        kind.takeIf { it != ExerciseKind.REPS }?.let { GymFormat.kindLabel(it) }
                    ).joinToString(" \u00B7 ")
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    TextButton(onClick = onUp, enabled = canMoveUp) { Text("↑") }
                    TextButton(onClick = onDown, enabled = canMoveDown) { Text("↓") }
                    TextButton(onClick = onRemove) {
                        Text("✕", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Zeile 1: Sätze und Pause (gilt für alle Sätze dieser Übung)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft.setsInput,
                    onValueChange = { onChange(draft.copy(setsInput = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text(tr("Sätze", "Sets")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                RestStepper(
                    seconds = draft.restSeconds,
                    onChange = { onChange(draft.copy(restSeconds = it)) },
                    modifier = Modifier.weight(2f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Zeile 2: Wiederholungen, Min. und Max. getrennt bearbeitbar
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.repsMinInput,
                    onValueChange = { onChange(draft.copy(repsMinInput = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text(if (kind == ExerciseKind.TIME) tr("Min. Sek.", "Min. sec") else tr("Min. Wdh.", "Min. reps")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = draft.repsMaxInput,
                    onValueChange = { onChange(draft.copy(repsMaxInput = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text(if (kind == ExerciseKind.TIME) tr("Max. Sek.", "Max. sec") else tr("Max. Wdh.", "Max. reps")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ToggleRow(
                label = tr("Einseitig (rechts und links)", "Unilateral (right and left)"),
                checked = draft.unilateral,
                onChange = { onChange(draft.copy(unilateral = it)) }
            )

            if (canLink) {
                ToggleRow(
                    label = tr("Mit nächster Übung verbinden (Superset)", "Link with next exercise (superset)"),
                    checked = draft.supersetWithNext,
                    onChange = { onChange(draft.copy(supersetWithNext = it)) }
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
