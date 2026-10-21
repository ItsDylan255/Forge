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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnesstracker.domain.gym.ExerciseKind
import com.example.fitnesstracker.domain.gym.GymFormat
import com.example.fitnesstracker.domain.gym.MuscleGroups
import com.example.fitnesstracker.ui.weight.ForgeCard

@Composable
fun ExerciseLibraryScreen(
    viewModel: GymViewModel,
    onBack: () -> Unit
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()

    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    val grouped = exercises.groupBy { it.muscleGroup ?: "Ohne Gruppe" }
    val groupOrder = MuscleGroups.all + "Ohne Gruppe"
    val orderedGroups = grouped.keys.sortedWith(
        compareBy({ groupOrder.indexOf(it).let { index -> if (index < 0) Int.MAX_VALUE else index } }, { it })
    )

    GymPage {
        GymHeader(title = "Übungen", onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                error = null
                creating = true
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("+ Neue Übung") }

        if (exercises.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            EmptyHint("Noch keine Übungen. Lege deine erste Übung an, danach kannst du sie in Routinen verwenden.")
        }

        orderedGroups.forEach { group ->
            GroupLabel(group)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                grouped.getValue(group).forEach { exercise ->
                    ForgeCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            error = null
                            editId = exercise.id
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = exercise.name, style = MaterialTheme.typography.bodyMedium)

                            // Normale Übungen brauchen keinen Zusatz, nur Zeit und Assistiert
                            val kind = ExerciseKind.fromCode(exercise.kind)
                            if (kind != ExerciseKind.REPS) {
                                Text(
                                    text = GymFormat.kindLabel(kind),
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

    if (creating) {
        ExerciseEditDialog(
            title = "Neue Übung",
            initialName = "",
            initialMuscle = null,
            error = error,
            onSave = { name, muscle, kind ->
                viewModel.saveExercise(null, name, muscle, kind) { saveError, _ ->
                    if (saveError != null) error = saveError
                    else {
                        creating = false
                        error = null
                    }
                }
            },
            onDismiss = {
                creating = false
                error = null
            }
        )
    }

    val editing = exercises.firstOrNull { it.id == editId }
    if (editing != null) {
        ExerciseEditDialog(
            title = "Übung bearbeiten",
            initialName = editing.name,
            initialMuscle = editing.muscleGroup,
            initialKind = ExerciseKind.fromCode(editing.kind),
            error = error,
            onSave = { name, muscle, kind ->
                viewModel.saveExercise(editing.id, name, muscle, kind) { saveError, _ ->
                    if (saveError != null) error = saveError
                    else {
                        editId = null
                        error = null
                    }
                }
            },
            onDismiss = {
                editId = null
                error = null
            },
            onDelete = { deleteId = editing.id }
        )
    }

    val toDelete = exercises.firstOrNull { it.id == deleteId }
    if (toDelete != null) {
        ConfirmDialog(
            title = "Übung löschen?",
            message = "„${toDelete.name}“ wird auch aus allen Routinen entfernt. " +
                    "Bereits geloggte Sätze im Verlauf bleiben erhalten.",
            confirmLabel = "Löschen",
            onConfirm = {
                viewModel.deleteExercise(toDelete.id)
                deleteId = null
                editId = null
            },
            onDismiss = { deleteId = null }
        )
    }
}
