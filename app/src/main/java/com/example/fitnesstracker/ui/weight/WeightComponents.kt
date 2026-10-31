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
import com.example.fitnesstracker.domain.weight.GoalDeadline
import com.example.fitnesstracker.domain.weight.GoalDirection
import com.example.fitnesstracker.domain.weight.GoalProgress
import com.example.fitnesstracker.domain.weight.WeightChange
import com.example.fitnesstracker.domain.weight.WeightFormat
import com.example.fitnesstracker.domain.weight.WeightHistoryItem
import com.example.fitnesstracker.domain.weight.WeightChartSlice
import com.example.fitnesstracker.domain.weight.WeightMeasurement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.Dp
import com.example.fitnesstracker.data.local.WeightGoal
import com.example.fitnesstracker.domain.weight.WeightForecast
import com.example.fitnesstracker.i18n.L10n
import com.example.fitnesstracker.i18n.tr
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
        text = text.uppercase(L10n.locale),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Umschalter "Gewicht" / "Ziel". */
@Composable
internal fun WeightTabs(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf(tr("Gewicht", "Weight"), tr("Ziel", "Goal"))

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
// Info-Boxen (gleiches Raster für Statistik und Ziel)
// ------------------------------------------------------------

private val BoxHeight = 80.dp
private val boxDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern(tr("dd.MM.yy", "dd/MM/yy"), L10n.locale)

/**
 * Eine kleine Info-Box. Mit [onClick] ist sie antippbar,
 * mit [muted] wird der Wert gedämpft dargestellt (z. B. "Festlegen" oder "—").
 */
@Composable
internal fun RowScope.InfoBox(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    muted: Boolean = false
) {
    ForgeCard(
        modifier = Modifier
            .weight(1f)
            .height(BoxHeight),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

private fun WeightChange?.asText(): String =
    this?.let { WeightFormat.delta(it.deltaKg) } ?: "—"

/** Tab "Gewicht": sechs Statistik-Boxen (2 Zeilen mit je 3 Spalten). */
@Composable
internal fun StatsGrid(
    latest: WeightMeasurement?,
    changeToPrevious: WeightChange?,
    changeTotal: WeightChange?,
    change7: WeightChange?,
    change30: WeightChange?,
    change90: WeightChange?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoBox(tr("Aktuell", "Current"), latest?.let { WeightFormat.weight(it.weightKg) } ?: "—")
            InfoBox(tr("Änderung", "Change"), changeToPrevious.asText())
            InfoBox(tr("Gesamt", "Total"), changeTotal.asText())
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoBox(tr("7 Tage", "7 days"), change7.asText())
            InfoBox(tr("30 Tage", "30 days"), change30.asText())
            InfoBox(tr("90 Tage", "90 days"), change90.asText())
        }
    }
}

/**
 * Tab "Ziel": sechs Ziel-Boxen (2 Zeilen mit je 3 Spalten).
 * Zielgewicht und Zieldatum sind antippbar.
 */
@Composable
internal fun GoalGrid(
    goal: WeightGoal?,
    currentKg: Double?,
    progress: GoalProgress?,
    forecast: WeightForecast?,
    onTargetClick: () -> Unit,
    onDateClick: () -> Unit
) {
    val target = goal?.targetWeightKg
    val start = goal?.startWeightKg

    // Bei Start unter Ziel ist es ein Zunehmen-Ziel
    val gaining = if (start != null && target != null) start < target
    else progress?.direction == GoalDirection.GAIN

    // Tage bis zum Zieldatum (null ohne Datum)
    val daysText: String? = GoalDeadline.text(
        GoalDeadline.daysLeft(goal?.targetDateEpochDay, LocalDate.now().toEpochDay())
    )

    val remainingText: String? = progress?.let {
        if (it.direction == GoalDirection.REACHED) WeightFormat.weight(0.0)
        else WeightFormat.weight(it.remainingKg)
    }

    val percentText: String? = progress?.progressFraction?.let {
        String.format(L10n.locale, "%.0f %%", it * 100.0)
    }

    val dateText: String? = goal?.targetDateEpochDay?.let {
        LocalDate.ofEpochDay(it).format(boxDateFormatter)
    }

    val forecastText: String? = when (forecast) {
        is WeightForecast.Predicted -> LocalDate.ofEpochDay(forecast.epochDay).format(boxDateFormatter)
        WeightForecast.Reached -> tr("Erreicht", "Reached")
        // Kurze Gründe, damit die Box nicht einfach leer bleibt
        WeightForecast.NotEnoughData -> tr("Mehr Daten", "More data")
        WeightForecast.NoProgress -> tr("Kein Trend", "No trend")
        WeightForecast.TooFar -> tr("> 10 Jahre", "> 10 years")
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoBox(
                label = tr("Bis Zieldatum", "Until target date"),
                value = daysText ?: "—",
                muted = daysText == null
            )
            InfoBox(
                label = tr("Zielgewicht", "Target weight"),
                value = target?.let { WeightFormat.weight(it) } ?: tr("Festlegen", "Set"),
                onClick = onTargetClick,
                muted = target == null
            )
            InfoBox(
                label = if (gaining) tr("Noch zuzunehmen", "Left to gain") else tr("Noch abzunehmen", "Left to lose"),
                value = remainingText ?: "—",
                muted = remainingText == null
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoBox(
                label = tr("Zieldatum", "Target date"),
                value = dateText ?: if (goal == null) "—" else tr("Festlegen", "Set"),
                onClick = onDateClick,
                muted = dateText == null
            )
            InfoBox(
                label = tr("Fortschritt", "Progress"),
                value = percentText ?: "—",
                muted = percentText == null
            )
            InfoBox(
                label = tr("Prognose", "Forecast"),
                value = forecastText ?: "—",
                muted = forecast !is WeightForecast.Predicted && forecast != WeightForecast.Reached
            )
        }
    }
}

// ------------------------------------------------------------
// Diagramm (bleibt in beiden Tabs sichtbar)
// ------------------------------------------------------------

@Composable
internal fun ChartCard(
    slice: WeightChartSlice?,
    targetKg: Double?,
    height: Dp,
    showPoints: Boolean,
    onPointClick: (entryId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    ForgeCard(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (slice != null && slice.visible.isNotEmpty()) {
            WeightChart(
                slice = slice,
                targetKg = targetKg,
                modifier = Modifier.fillMaxSize(),
                showPoints = showPoints,
                onPointClick = { onPointClick(it.id) }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tr("Keine Messung im gewählten Zeitraum.", "No measurement in the selected range."),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun RangeSelector(
    selected: ChartRange,
    onSelect: (ChartRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ChartRange.values().forEach { option ->
            val isSelected = option == selected

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                        else Color.Transparent
                    )
                    .clickable { onSelect(option) }
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
        title = { Text(tr("Gewicht eintragen", "Log weight")) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = tr("Datum", "Date"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = WeightFormat.date(dateEpochDay),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    TextButton(onClick = onChangeDate) { Text(tr("Ändern", "Change")) }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Gewicht in kg", "Weight in kg")) },
                    placeholder = { Text("z. B. 83,0") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                if (existingKgForDate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tr("Für dieses Datum gibt es schon einen Eintrag ", "There's already an entry for this date ") +
                                "(${WeightFormat.weight(existingKgForDate)}). " +
                                tr("Er wird überschrieben.", "It will be overwritten."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(tr("Speichern", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Abbrechen", "Cancel")) } }
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
        title = { Text(tr("Eintrag vom ${WeightFormat.date(dateEpochDay)}", "Entry from ${WeightFormat.date(dateEpochDay)}")) },
        text = {
            Column {
                Text(
                    text = tr("Aktuell gespeichert: ${WeightFormat.weight(weightKg)}", "Currently saved: ${WeightFormat.weight(weightKg)}"),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("Löschen", "Delete"), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onEdit) { Text(tr("Bearbeiten", "Edit")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Abbrechen", "Cancel")) } }
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
        title = { Text(tr("Gewicht bearbeiten", "Edit weight")) },
        text = {
            Column {
                Text(
                    text = tr("Datum: ${WeightFormat.date(dateEpochDay)}", "Date: ${WeightFormat.date(dateEpochDay)}"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Neues Gewicht in kg", "New weight in kg")) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(tr("Speichern", "Save")) } },
        dismissButton = { TextButton(onClick = onBack) { Text(tr("Zurück", "Back")) } }
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
                Text(tr("Löschen", "Delete"), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Abbrechen", "Cancel")) } }
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
        title = { Text(tr("Ungewöhnlicher Wert", "Unusual value")) },
        text = {
            Text(
                tr("Du hast ${WeightFormat.weight(weightKg)} eingegeben. ", "You entered ${WeightFormat.weight(weightKg)}. ") +
                        tr("Das liegt außerhalb des üblichen Bereichs. Ist der Wert richtig?", "That's outside the usual range. Is it correct?")
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(tr("Ja, speichern", "Yes, save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Korrigieren", "Fix")) } }
    )
}

/** Zielgewicht festlegen oder ändern. Entfernen ist hier möglich, aber nur mit Sicherheitsfrage. */
@Composable
internal fun GoalWeightDialog(
    hasGoal: Boolean,
    input: String,
    error: String?,
    onInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Zielgewicht", "Target weight")) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Zielgewicht in kg", "Target weight in kg")) },
                    placeholder = { Text("z. B. 85,0") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                if (hasGoal) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onRemove) {
                        Text(tr("Ziel entfernen", "Remove goal"), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(tr("Speichern", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Abbrechen", "Cancel")) } }
    )
}

/** Einfacher Hinweis mit einer Aktion. */
@Composable
internal fun InfoMessageDialog(
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() ?: onConfirm() },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = onDismiss?.let { dismiss ->
            { TextButton(onClick = dismiss) { Text(tr("Abbrechen", "Cancel")) } }
        }
    )
}
