package com.example.fitnesstracker.ui.food

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.domain.food.FoodCalculator
import com.example.fitnesstracker.domain.food.FoodFormat
import com.example.fitnesstracker.domain.food.FoodLogData
import com.example.fitnesstracker.domain.food.Macros
import com.example.fitnesstracker.domain.calories.CalorieNote
import com.example.fitnesstracker.domain.calories.CalorieTarget
import com.example.fitnesstracker.ui.calories.calorieNoteText
import com.example.fitnesstracker.ui.gym.EmptyHint
import com.example.fitnesstracker.ui.gym.GroupLabel
import com.example.fitnesstracker.ui.gym.StatBox
import com.example.fitnesstracker.ui.weight.ForgeCard
import com.example.fitnesstracker.ui.weight.SectionLabel

private fun macroText(actual: Double, goalGrams: Int?): String =
    if (goalGrams == null) FoodFormat.macro(actual)
    else "${FoodFormat.macro(actual).removeSuffix(" g")} / $goalGrams g"

/** Menge eines Eintrags: "2 × Scheibe · 60 g" bei Eingabe per Portion, sonst "60 g". */
internal fun amountText(entry: FoodLogData): String =
    entry.servingNote?.let { "$it \u00B7 ${FoodFormat.grams(entry.grams)}" } ?: FoodFormat.grams(entry.grams)

@Composable
private fun GoalBar(fraction: Float) {
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

/**
 * Tagesansicht: Tag wechseln, Summen (Kalorien und Makros) und die Eintraege des Tages.
 * [calorieGoal] ist das Tagesziel in kcal. Solange es null ist (kommt mit dem Kalorienziel),
 * wird kein Fortschrittsbalken gezeigt.
 */
@Composable
internal fun FoodDayTab(
    selectedDay: Long,
    today: Long,
    entries: List<FoodLogData>,
    totals: Macros,
    calorieGoal: Int?,
    calorieTarget: CalorieTarget?,
    hasProfile: Boolean,
    onOpenSettings: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenHistory: () -> Unit,
    onEntryClick: (entryId: Long) -> Unit
) {
    // ---------- Tag wechseln ----------
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrevious) { Text("\u2039", fontSize = 28.sp) }

        Text(
            text = FoodFormat.dayLabel(selectedDay, today),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenHistory)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        TextButton(onClick = onNext, enabled = selectedDay < today) {
            Text("\u203A", fontSize = 28.sp)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // ---------- Summen ----------
    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionLabel("Kalorien")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = FoodFormat.kcal(totals.kcal),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 40.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            val fraction = FoodCalculator.goalFraction(totals.kcal, calorieGoal)
            val percent = FoodCalculator.goalPercent(totals.kcal, calorieGoal)

            if (calorieGoal != null && fraction != null && percent != null) {
                Text(
                    text = "von ${FoodFormat.kcal(calorieGoal.toDouble())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                GoalBar(fraction.toFloat())

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = FoodFormat.percent(percent),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (totals.kcal >= calorieGoal) "Ziel erreicht"
                        else "Noch ${FoodFormat.kcal(calorieGoal - totals.kcal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Erklärung zum Tagesziel (zum Beispiel "Zieldatum zu knapp") oder Weg zum Profil
    when {
        calorieTarget != null -> calorieNoteText(calorieTarget)?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = if (calorieTarget.note == CalorieNote.TOO_TIGHT) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        !hasProfile -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Noch kein Kalorienziel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            TextButton(onClick = onOpenSettings) { Text("Profil anlegen") }
        }

        else -> Text(
            text = "Trage dein Gewicht im Gewicht-Tab ein, um dein Tagesziel zu berechnen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Mit Ziel: "Ist / Soll", zum Beispiel "92,5 / 150 g"
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatBox("Protein", macroText(totals.protein, calorieTarget?.macros?.proteinG), Modifier.weight(1f))
        StatBox("Kohlenhydrate", macroText(totals.carbs, calorieTarget?.macros?.carbsG), Modifier.weight(1f))
        StatBox("Fett", macroText(totals.fat, calorieTarget?.macros?.fatG), Modifier.weight(1f))
    }

    // ---------- Eintraege ----------
    // Ohne Eintraege bleibt der Bereich leer
    if (entries.isEmpty()) return

    GroupLabel("Einträge")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { entry ->
            ForgeCard(modifier = Modifier.fillMaxWidth(), onClick = { onEntryClick(entry.id) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = entry.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${amountText(entry)} \u00B7 " +
                                    "P ${FoodFormat.macro(entry.macros.protein)} \u00B7 " +
                                    "K ${FoodFormat.macro(entry.macros.carbs)} \u00B7 " +
                                    "F ${FoodFormat.macro(entry.macros.fat)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = FoodFormat.kcal(entry.macros.kcal),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}
