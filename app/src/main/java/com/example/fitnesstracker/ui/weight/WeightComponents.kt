package com.example.fitnesstracker.ui.weight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.domain.weight.ChartRange
import com.example.fitnesstracker.domain.weight.GoalDirection
import com.example.fitnesstracker.domain.weight.GoalProgress
import com.example.fitnesstracker.domain.weight.WeightChange
import com.example.fitnesstracker.domain.weight.WeightFormat
import com.example.fitnesstracker.domain.weight.WeightHistoryItem
import com.example.fitnesstracker.domain.weight.WeightMeasurement
import java.util.Locale

// ------------------------------------------------------------
// Grundbausteine (Forge-Look: ruhige Karten mit feiner Linie)
// ------------------------------------------------------------

@Composable
internal fun ForgeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border,
            content = content
        )
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(Locale.GERMANY),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Umschalter "Gewicht" / "Ziel". */
@Composable
internal fun WeightTabs(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("Gewicht", "Ziel")

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------
// Weight-Tab
// ------------------------------------------------------------

private fun WeightChange?.asText(): String =
    this?.let { WeightFormat.delta(it.deltaKg) } ?: "—"

@Composable
internal fun StatsCard(
    latest: WeightMeasurement?,
    changeToPrevious: WeightChange?,
    changeTotal: WeightChange?,
    change7: WeightChange?,
    change30: WeightChange?,
    change90: WeightChange?
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                StatCell("Aktuell", latest?.let { WeightFormat.weight(it.weightKg) } ?: "—")
                StatDivider()
                StatCell("Änderung", changeToPrevious.asText())
                StatDivider()
                StatCell("Gesamt", changeTotal.asText())
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )

            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                StatCell("7 Tage", change7.asText())
                StatDivider()
                StatCell("30 Tage", change30.asText())
                StatDivider()
                StatCell("90 Tage", change90.asText())
            }
        }
    }
}

@Composable
private fun RowScope.StatCell(label: String, value: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp),
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

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxHeight()
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}

@Composable
internal fun ChartSection(
    points: List<WeightMeasurement>,
    hasEnoughPoints: Boolean,
    targetKg: Double?,
    range: ChartRange,
    onRangeChange: (ChartRange) -> Unit
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        if (hasEnoughPoints) {
            WeightChart(
                measurements = points,
                targetKg = targetKg,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Für das Diagramm brauchst du mindestens zwei Messungen im gewählten Zeitraum.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ChartRange.values().forEach { option ->
            val isSelected = option == range

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                        else Color.Transparent
                    )
                    .clickable { onRangeChange(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    softWrap = false,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun HistoryList(
    items: List<WeightHistoryItem>,
    onItemClick: (entryId: Long) -> Unit
) {
    if (items.isEmpty()) {
        Text(
            text = "Hier erscheinen deine gespeicherten Messungen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            ForgeCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onItemClick(item.measurement.id) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = WeightFormat.date(item.measurement.dateEpochDay),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = WeightFormat.weight(item.measurement.weightKg),
                            style = MaterialTheme.typography.titleMedium
                        )
                        item.deltaToPreviousKg?.let { delta ->
                            Text(
                                text = WeightFormat.delta(delta),
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

// ------------------------------------------------------------
// Ziel-Tab
// ------------------------------------------------------------

@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
internal fun GoalCard(
    targetKg: Double?,
    startKg: Double?,
    progress: GoalProgress?,
    isEditing: Boolean,
    input: String,
    error: String?,
    onInputChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionLabel("Mein Zielgewicht")

            Spacer(modifier = Modifier.height(8.dp))

            if (targetKg == null) {
                Text(
                    text = "Noch kein Ziel gesetzt",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = WeightFormat.weight(targetKg),
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (progress == null) {
                    Text(
                        text = "Trage dein Gewicht ein, um deinen Fortschritt zu sehen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val remainingText = when (progress.direction) {
                        GoalDirection.REACHED -> "Ziel erreicht"
                        GoalDirection.LOSE ->
                            "Noch ${WeightFormat.weight(progress.remainingKg)} abnehmen"
                        GoalDirection.GAIN ->
                            "Noch ${WeightFormat.weight(progress.remainingKg)} zunehmen"
                    }
                    val fraction = progress.progressFraction

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = remainingText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (fraction != null) {
                            Text(
                                text = String.format(
                                    Locale.GERMANY, "%.0f %%", fraction * 100.0
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    if (fraction != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ProgressBar(fraction.toFloat())
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Start: ${startKg?.let { WeightFormat.weight(it) } ?: "—"}" +
                                "  →  Aktuell: ${WeightFormat.weight(progress.currentKg)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Zielgewicht in kg") },
                    placeholder = { Text("z. B. 85,0") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                    Text("Ziel speichern")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Abbrechen")
                }
            } else {
                Button(onClick = onStartEdit, modifier = Modifier.fillMaxWidth()) {
                    Text(if (targetKg == null) "Zielgewicht festlegen" else "Zielgewicht ändern")
                }

                if (targetKg != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                        Text("Ziel entfernen", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------
// Dialoge
// ------------------------------------------------------------

@Composable
internal fun AddWeightDialog(
    dateEpochDay: Long,
    existingKgForDate: Double?,
    input: String,
    error: String?,
    onInputChange: (String) -> Unit,
    onChangeDate: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gewicht eintragen") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Datum",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = WeightFormat.date(dateEpochDay),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    TextButton(onClick = onChangeDate) { Text("Ändern") }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Gewicht in kg") },
                    placeholder = { Text("z. B. 83,0") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                if (existingKgForDate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Für dieses Datum gibt es schon einen Eintrag " +
                                "(${WeightFormat.weight(existingKgForDate)}). " +
                                "Er wird überschrieben.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

/** Auswahl nach Antippen eines Verlaufseintrags: Bearbeiten / Löschen / Abbrechen. */
@Composable
internal fun EntryActionDialog(
    dateEpochDay: Long,
    weightKg: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag vom ${WeightFormat.date(dateEpochDay)}") },
        text = {
            Column {
                Text(
                    text = "Aktuell gespeichert: ${WeightFormat.weight(weightKg)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onEdit) { Text("Bearbeiten") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
internal fun EditEntryDialog(
    dateEpochDay: Long,
    input: String,
    error: String?,
    onInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onBack,
        title = { Text("Gewicht bearbeiten") },
        text = {
            Column {
                Text(
                    text = "Datum: ${WeightFormat.date(dateEpochDay)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Neues Gewicht in kg") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onBack) { Text("Zurück") } }
    )
}

@Composable
internal fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Löschen", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

/** Rückfrage bei Werten außerhalb 20–400 kg. Nichts wird abgelehnt, nur nachgefragt. */
@Composable
internal fun UnusualValueDialog(
    weightKg: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ungewöhnlicher Wert") },
        text = {
            Text(
                "Du hast ${WeightFormat.weight(weightKg)} eingegeben. " +
                        "Das liegt außerhalb des üblichen Bereichs. Ist der Wert richtig?"
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Ja, speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Korrigieren") } }
    )
}
