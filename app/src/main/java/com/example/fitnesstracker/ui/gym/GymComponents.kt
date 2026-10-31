package com.example.fitnesstracker.ui.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fitnesstracker.data.local.GymExercise
import com.example.fitnesstracker.domain.gym.ExerciseKind
import com.example.fitnesstracker.domain.gym.GymDefaults
import com.example.fitnesstracker.domain.gym.GymFormat
import com.example.fitnesstracker.domain.gym.MuscleGroups
import com.example.fitnesstracker.i18n.tr
import com.example.fitnesstracker.ui.weight.ForgeCard
import com.example.fitnesstracker.ui.weight.SectionLabel

/** Scrollbare Seite mit den üblichen Rändern. */
@Composable
internal fun GymPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp)
    ) {
        content()
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
internal fun GymHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    Column {
        if (onBack != null) {
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) { Text(tr("‹ Zurück", "‹ Back")) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            trailing()
        }
    }
}

@Composable
internal fun GymTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            labels.forEachIndexed { index, label ->
                val isSelected = index == selected

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                            else Color.Transparent
                        )
                        .clickable { onSelect(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Kleine Kennzahl-Box (z. B. Dauer, Volumen). */
@Composable
internal fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    ForgeCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Abbrechen", "Cancel")) } }
    )
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Auswahl der Muskelgruppe. Nochmal antippen = Auswahl aufheben. */
@Composable
internal fun MuscleChips(selected: String?, onSelect: (String?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MuscleGroups.all.forEach { group ->
            SelectableChip(
                label = MuscleGroups.label(group),
                selected = group == selected,
                onClick = { onSelect(if (group == selected) null else group) }
            )
        }
    }
}

/** Auswahl der Übungsart: Wiederholungen, Zeit oder Assistiert. */
@Composable
internal fun KindChips(selected: ExerciseKind, onSelect: (ExerciseKind) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ExerciseKind.entries.forEach { kind ->
            SelectableChip(
                label = GymFormat.kindLabel(kind),
                selected = kind == selected,
                onClick = { onSelect(kind) }
            )
        }
    }
}

private fun kindHint(kind: ExerciseKind): String = when (kind) {
    ExerciseKind.REPS -> tr("Gewicht × Wiederholungen.", "Weight × reps.")
    ExerciseKind.TIME -> tr("Haltezeit in Sekunden, z. B. Plank. Zusatzgewicht ist optional.", "Hold time in seconds, e.g. plank. Added weight is optional.")
    ExerciseKind.ASSISTED -> tr("Das Gewicht ist die Unterstützung, z. B. Assisted Dip. Weniger ist besser.", "The weight is the assistance, e.g. assisted dip. Less is better.")
}

@Composable
internal fun ExerciseEditDialog(
    title: String,
    initialName: String,
    initialMuscle: String?,
    error: String?,
    onSave: (name: String, muscleGroup: String?, kind: ExerciseKind) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    initialKind: ExerciseKind = ExerciseKind.REPS
) {
    var name by remember { mutableStateOf(initialName) }
    var muscle by remember { mutableStateOf(initialMuscle) }
    var kind by remember { mutableStateOf(initialKind) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = tr("Muskelgruppe (optional)", "Muscle group (optional)"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                MuscleChips(selected = muscle, onSelect = { muscle = it })

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = tr("Art der Übung", "Exercise type"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                KindChips(selected = kind, onSelect = { kind = it })

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = kindHint(kind),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Beim Ändern der Art bleiben schon geloggte Sätze unverändert
                if (onDelete != null && kind != initialKind) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tr("Bereits geloggte Sätze behalten ihre bisherige Art.", "Sets you already logged keep their previous type."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onDelete) {
                        Text(tr("Übung löschen", "Delete exercise"), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, muscle, kind) }) { Text(tr("Speichern", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Abbrechen", "Cancel")) } }
    )
}

/** Auswahl aus der Übungsbibliothek mit Suche und der Möglichkeit, direkt eine neue Übung anzulegen. */
@Composable
internal fun ExercisePickerDialog(
    viewModel: GymViewModel,
    exercises: List<GymExercise>,
    excludeIds: Set<Long>,
    onPick: (id: Long, name: String, muscleGroup: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    val filtered = exercises.filter {
        it.id !in excludeIds && it.name.contains(query.trim(), ignoreCase = true)
    }

    if (creating) {
        ExerciseEditDialog(
            title = tr("Neue Übung", "New exercise"),
            initialName = query.trim(),
            initialMuscle = null,
            error = createError,
            onSave = { name, muscle, kind ->
                viewModel.saveExercise(null, name, muscle, kind) { error, newId ->
                    if (error != null) {
                        createError = error
                    } else if (newId != null) {
                        creating = false
                        onPick(newId, name.trim(), muscle)
                    }
                }
            },
            onDismiss = {
                creating = false
                createError = null
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(tr("Übung wählen", "Choose exercise")) },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr("Suchen", "Search")) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filtered.isEmpty()) {
                        Text(
                            text = if (exercises.isEmpty()) tr("Noch keine Übung angelegt. Lege unten deine erste an.", "No exercises yet. Create your first one below.")
                            else tr("Nichts gefunden.", "Nothing found."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(filtered, key = { it.id }) { exercise ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onPick(exercise.id, exercise.name, exercise.muscleGroup)
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = listOfNotNull(
                                        exercise.muscleGroup?.let(MuscleGroups::label),
                                        ExerciseKind.fromCode(exercise.kind)
                                            .takeIf { it != ExerciseKind.REPS }
                                            ?.let { GymFormat.kindLabel(it) }
                                    ).joinToString(" \u00B7 "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    TextButton(onClick = { creating = true }) { Text(tr("+ Neue Übung anlegen", "+ Create new exercise")) }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Schließen", "Close")) } }
        )
    }
}

@Composable
internal fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Überschrift über einer Gruppe von Karten. */
@Composable
internal fun GroupLabel(text: String) {
    Spacer(modifier = Modifier.height(20.dp))
    SectionLabel(text)
    Spacer(modifier = Modifier.height(8.dp))
}

/**
 * Pause einer Übung einstellen: − und + in 15-Sekunden-Schritten. Bei 0 gibt es keine Pause.
 * Die Pause gilt für alle Sätze der Übung.
 */
@Composable
internal fun RestStepper(
    seconds: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(4.dp)

    Row(
        modifier = modifier
            .height(56.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = { onChange((seconds - GymDefaults.REST_STEP_SECONDS).coerceAtLeast(0)) },
            enabled = seconds > 0,
            modifier = Modifier.width(48.dp),
            contentPadding = PaddingValues(0.dp)
        ) { Text("−", style = MaterialTheme.typography.titleLarge) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = tr("Pause", "Rest"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = GymFormat.restLabel(seconds),
                style = MaterialTheme.typography.titleMedium
            )
        }

        TextButton(
            onClick = {
                onChange((seconds + GymDefaults.REST_STEP_SECONDS).coerceAtMost(GymDefaults.MAX_REST_SECONDS))
            },
            enabled = seconds < GymDefaults.MAX_REST_SECONDS,
            modifier = Modifier.width(48.dp),
            contentPadding = PaddingValues(0.dp)
        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}
