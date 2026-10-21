package com.example.fitnesstracker.ui.calories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.domain.calories.CalorieDirection
import com.example.fitnesstracker.domain.calories.CalorieNote
import com.example.fitnesstracker.domain.calories.CalorieTarget
import com.example.fitnesstracker.domain.weight.WeightFormat
import com.example.fitnesstracker.ui.weight.ForgeCard
import com.example.fitnesstracker.ui.weight.SectionLabel

/**
 * Erklärung zum berechneten Tagesziel, auch für die Tagesansicht im Essen-Tab.
 * Null, wenn es nichts Besonderes zu sagen gibt (normales Ziel).
 */
internal fun calorieNoteText(t: CalorieTarget): String? = when (t.note) {
    CalorieNote.NO_GOAL -> "Kein Gewichtsziel gesetzt: Erhaltungsbedarf."
    CalorieNote.NO_DATE -> "Kein Zieldatum gesetzt: Erhaltungsbedarf."
    CalorieNote.DATE_PASSED -> "Zieldatum ist vorbei: Erhaltungsbedarf."
    CalorieNote.TOO_TIGHT -> tooTightText(t)
    CalorieNote.REACHED -> "Gewichtsziel erreicht: Erhaltungsbedarf."
    CalorieNote.NONE -> null
}

/**
 * Deutlicher Hinweis: Das Zieldatum ist mit dem erlaubten Defizit bzw. Plus nicht zu schaffen.
 * Nennt die Grenze und, wenn möglich, den Tag, an dem das Gewicht damit erreicht wird.
 */
private fun tooTightText(t: CalorieTarget): String {
    val what = if (t.direction == CalorieDirection.GAIN) "Überschuss" else "Defizit"
    val eta = t.estimatedEpochDay?.let {
        "So erreichst du dein Gewicht voraussichtlich erst am ${WeightFormat.date(it)}."
    } ?: "So ist dein Gewicht in absehbarer Zeit nicht erreichbar."

    return "Zieldatum zu knapp: Das Tagesziel ist auf ${kcalText(t.appliedDeltaKcal)} $what begrenzt, " +
            "damit es gesund bleibt. $eta"
}

private fun kcalText(v: Int): String = "%,d kcal".format(java.util.Locale.GERMANY, v)

/**
 * Karte mit Tagesziel, Makros in Gramm und Zugang zum Profil.
 * [target] ist null, solange Profil oder Gewichtseintrag fehlt.
 */
@Composable
fun CalorieGoalCard(
    hasProfile: Boolean,
    target: CalorieTarget?,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
    showEditButton: Boolean = true
) {
    ForgeCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionLabel("Kalorienziel")
            Spacer(modifier = Modifier.height(8.dp))

            when {
                !hasProfile -> Text(
                    text = "Lege dein Profil an, um dein Tagesziel zu berechnen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                target == null -> Text(
                    text = "Trage zuerst dein Gewicht im Gewicht-Tab ein.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> {
                    Text(
                        text = kcalText(target.targetKcal),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 30.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    calorieNoteText(target)?.let { note ->
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (target.note == CalorieNote.TOO_TIGHT) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "P ${target.macros.proteinG} g \u00B7 K ${target.macros.carbsG} g \u00B7 F ${target.macros.fatG} g",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Grundumsatz ${kcalText(target.bmr)} \u00B7 Gesamtumsatz ${kcalText(target.tdee)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showEditButton) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onEditProfile) {
                        Text(if (hasProfile) "Profil bearbeiten" else "Profil anlegen")
                    }
                }
            }
        }
    }
}
