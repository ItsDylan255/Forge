package com.example.fitnesstracker.ui.gym

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnesstracker.data.local.GymPlan
import com.example.fitnesstracker.domain.gym.RoutinePlans
import com.example.fitnesstracker.domain.gym.RoutineRef
import com.example.fitnesstracker.i18n.tr
import com.example.fitnesstracker.ui.weight.ForgeCard

/** Ein Eintrag im ⋮-Menü. */
internal data class MenuAction(
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

/** ⋮-Knopf mit aufklappendem Menü. */
@Composable
internal fun OverflowMenu(actions: List<MenuAction>) {
    var open by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { open = true }) { Text("⋮") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = action.label,
                            color = if (action.destructive) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        open = false
                        action.onClick()
                    }
                )
            }
        }
    }
}

/** Name eines Plans eingeben (neu oder umbenennen). */
@Composable
internal fun PlanNameDialog(
    title: String,
    initialName: String,
    error: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("Name des Plans", "Plan name")) },
                placeholder = { Text(tr("z. B. Fitnessbasic", "e.g. Full body")) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { message -> { Text(message) } }
            )
        },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text(tr("Speichern", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Abbrechen", "Cancel")) } }
    )
}

/** Plan kopieren: neuer Name, optional als Deload (ein Satz weniger pro Übung). */
@Composable
internal fun DuplicatePlanDialog(
    planName: String,
    error: String?,
    onSave: (name: String, deload: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(tr("$planName Kopie", "$planName copy")) }
    var deload by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Plan duplizieren", "Duplicate plan")) },
        text = {
            Column {
                Text(
                    text = tr("Alle aktiven Routinen werden mit ihren Übungen kopiert. Der alte Plan und sein Verlauf bleiben unverändert.", "All active routines are copied with their exercises. The old plan and its history stay unchanged."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Name des neuen Plans", "Name of the new plan")) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Deload", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = tr("Ein Satz weniger pro Übung (mindestens einer)", "One set less per exercise (at least one)"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = deload, onCheckedChange = { deload = it })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, deload) }) { Text(tr("Duplizieren", "Duplicate")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Abbrechen", "Cancel")) } }
    )
}

/** Auswahl des Trainingsplans einer Routine, mit der Möglichkeit, direkt einen neuen anzulegen. */
@Composable
internal fun PlanPickerDialog(
    viewModel: GymViewModel,
    plans: List<GymPlan>,
    selectedId: Long?,
    onPick: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (creating) {
        PlanNameDialog(
            title = tr("Neuer Plan", "New plan"),
            initialName = "",
            error = error,
            onSave = { name ->
                viewModel.savePlan(null, name) { message, id ->
                    error = message
                    if (message == null && id != null) {
                        creating = false
                        onPick(id)
                    }
                }
            },
            onDismiss = {
                creating = false
                error = null
            }
        )
        return
    }

    val options: List<Pair<Long?, String>> =
        listOf<Pair<Long?, String>>(null to tr("Kein Plan", "No plan")) +
                plans.filter { it.archivedAt == null }.map { it.id to it.name }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Trainingsplan", "Training plan")) },
        text = {
            Column {
                options.forEach { (id, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = id == selectedId, onClick = { onPick(id) })
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                TextButton(onClick = { creating = true }) { Text(tr("+ Neuer Plan", "+ New plan")) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Schließen", "Close")) } }
    )
}

/** Archivierte Pläne und Routinen: wiederherstellen oder endgültig löschen. */
@Composable
fun RoutineArchiveScreen(viewModel: GymViewModel, onBack: () -> Unit) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()

    val archive = RoutinePlans.archive(routines.map { it.toRef() }, plans.map { it.toRef() })

    var deletePlanId by remember { mutableStateOf<Long?>(null) }
    var deleteRoutineId by remember { mutableStateOf<Long?>(null) }

    GymPage {
        GymHeader(title = tr("Archiv", "Archive"), onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        if (archive.isEmpty) {
            EmptyHint(tr("Das Archiv ist leer. Archivierte Routinen und Pläne verschwinden aus der Auswahl, ihr Verlauf bleibt erhalten.", "The archive is empty. Archived routines and plans disappear from the selection, their history is kept."))
            return@GymPage
        }

        if (archive.plans.isNotEmpty()) {
            GroupLabel(tr("Pläne", "Plans"))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                archive.plans.forEach { group ->
                    val plan = group.plan ?: return@forEach
                    ArchiveCard(
                        title = plan.name,
                        subtitle = if (group.routines.isEmpty()) tr("Keine Routinen", "No routines")
                        else group.routines.joinToString(", ") { it.name },
                        onRestore = { viewModel.setPlanArchived(plan.id, archived = false) },
                        onDelete = { deletePlanId = plan.id }
                    )
                }
            }
        }

        if (archive.routines.isNotEmpty()) {
            GroupLabel(tr("Routinen", "Routines"))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                archive.routines.forEach { routine ->
                    ArchiveCard(
                        title = routine.name,
                        subtitle = planNameOf(routine, plans),
                        onRestore = { viewModel.setRoutineArchived(routine.id, archived = false) },
                        onDelete = { deleteRoutineId = routine.id }
                    )
                }
            }
        }
    }

    deletePlanId?.let { id ->
        ConfirmDialog(
            title = tr("Plan löschen?", "Delete plan?"),
            message = tr("Nur der Plan wird gelöscht. Seine Routinen bleiben erhalten und sind danach ohne Plan.", "Only the plan is deleted. Its routines are kept and will have no plan."),
            confirmLabel = tr("Löschen", "Delete"),
            onConfirm = {
                viewModel.deletePlan(id)
                deletePlanId = null
            },
            onDismiss = { deletePlanId = null }
        )
    }

    deleteRoutineId?.let { id ->
        ConfirmDialog(
            title = tr("Routine löschen?", "Delete routine?"),
            message = tr("Die Routine wird endgültig gelöscht. Bereits absolvierte Trainings im Verlauf bleiben erhalten.", "The routine will be deleted permanently. Completed workouts in your history are kept."),
            confirmLabel = tr("Löschen", "Delete"),
            onConfirm = {
                viewModel.deleteRoutine(id)
                deleteRoutineId = null
            },
            onDismiss = { deleteRoutineId = null }
        )
    }
}

private fun planNameOf(routine: RoutineRef, plans: List<GymPlan>): String? =
    routine.planId?.let { id -> plans.firstOrNull { it.id == id }?.name }

@Composable
private fun ArchiveCard(
    title: String,
    subtitle: String?,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onRestore) { Text(tr("Wiederherstellen", "Restore")) }
            OverflowMenu(listOf(MenuAction(tr("Löschen", "Delete"), destructive = true, onClick = onDelete)))
        }
    }
}
