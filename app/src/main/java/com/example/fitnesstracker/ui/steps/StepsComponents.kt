package com.example.fitnesstracker.ui.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.fitnesstracker.domain.steps.StepBar
import com.example.fitnesstracker.domain.steps.StepCalculator
import com.example.fitnesstracker.domain.steps.StepChartData
import com.example.fitnesstracker.domain.steps.StepChartRange
import com.example.fitnesstracker.domain.steps.StepDay
import com.example.fitnesstracker.domain.steps.StepFormat
import com.example.fitnesstracker.domain.steps.StepGoalProgress
import com.example.fitnesstracker.domain.steps.StepSummary
import com.example.fitnesstracker.ui.weight.ForgeCard
import com.example.fitnesstracker.ui.weight.SectionLabel

// ------------------------------------------------------------
// Hinweis-Karte (kein Sensor / Berechtigung fehlt)
// ------------------------------------------------------------

@Composable
internal fun StepsNoticeCard(
    message: String,
    primaryLabel: String? = null,
    onPrimary: () -> Unit = {},
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {}
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )

            if (primaryLabel != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) {
                    Text(primaryLabel)
                }
            }

            if (secondaryLabel != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) {
                    Text(secondaryLabel)
                }
            }
        }
    }
}

// ------------------------------------------------------------
// Heute: große Zahl + Fortschritt
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
internal fun TodayCard(
    progress: StepGoalProgress,
    onEditGoal: () -> Unit
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionLabel("Heute")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = StepFormat.steps(progress.steps),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 48.sp,
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = "von ${StepFormat.steps(progress.goal)} Schritten",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProgressBar(progress.barFraction.toFloat())

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = StepFormat.percent(progress.percent),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (progress.reached) "Ziel erreicht"
                    else "Noch ${StepFormat.steps(progress.remaining)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onEditGoal) {
                Text("Ziel ändern")
            }
        }
    }
}

// ------------------------------------------------------------
// Statistik: Woche / Monat
// ------------------------------------------------------------

@Composable
internal fun SummaryCard(week: StepSummary, month: StepSummary) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                SummaryCell("7 Tage · Summe", StepFormat.steps(week.totalSteps.toInt()))
                SummaryDivider()
                SummaryCell("7 Tage · Ø pro Tag", week.averagePerDay?.let { StepFormat.steps(it) } ?: "—")
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )

            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                SummaryCell("30 Tage · Summe", StepFormat.steps(month.totalSteps.toInt()))
                SummaryDivider()
                SummaryCell("30 Tage · Ø pro Tag", month.averagePerDay?.let { StepFormat.steps(it) } ?: "—")
            }
        }
    }
}

@Composable
private fun RowScope.SummaryCell(label: String, value: String) {
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
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxHeight()
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}

// ------------------------------------------------------------
// Diagramm
// ------------------------------------------------------------

@Composable
internal fun StepChartSection(
    bars: List<StepBar>,
    goal: Int,
    range: StepChartRange,
    onRangeChange: (StepChartRange) -> Unit
) {
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        if (StepChartData.hasAnySteps(bars)) {
            StepChart(bars = bars, goal = goal, modifier = Modifier.fillMaxWidth())
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Noch keine Schrittdaten im gewählten Zeitraum.",
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
        StepChartRange.values().forEach { option ->
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
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ------------------------------------------------------------
// Verlauf
// ------------------------------------------------------------

@Composable
internal fun StepHistoryList(
    items: List<StepDay>,
    goal: Int,
    onItemClick: (epochDay: Long) -> Unit
) {
    if (items.isEmpty()) {
        Text(
            text = "Hier erscheinen deine gezählten Schritte.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { day ->
            val progress = StepCalculator.goalProgress(day.steps, goal)

            ForgeCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onItemClick(day.dateEpochDay) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = StepFormat.date(day.dateEpochDay),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = StepFormat.steps(day.steps),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = StepFormat.percent(progress.percent) + " vom Ziel",
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
// Dialoge
// ------------------------------------------------------------

@Composable
internal fun StepDayDialog(
    day: StepDay,
    goal: Int,
    onDismiss: () -> Unit
) {
    val progress = StepCalculator.goalProgress(day.steps, goal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(StepFormat.date(day.dateEpochDay)) },
        text = {
            Column {
                Text(
                    text = StepFormat.steps(day.steps) + " Schritte",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tagesziel: ${StepFormat.steps(goal)} (${StepFormat.percent(progress.percent)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = if (progress.reached) "Ziel erreicht"
                    else "Es fehlten ${StepFormat.steps(progress.remaining)} Schritte",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } }
    )
}

@Composable
internal fun StepGoalDialog(
    input: String,
    error: String?,
    onInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tagesziel") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Schritte pro Tag") },
                placeholder = { Text("z. B. 10000") },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { message -> { Text(message) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
