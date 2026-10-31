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
import com.example.fitnesstracker.i18n.L10n
import com.example.fitnesstracker.i18n.tr
import com.example.fitnesstracker.ui.weight.ForgeCard
import com.example.fitnesstracker.ui.weight.SectionLabel

/**
 * Erklärung zum berechneten Tagesziel, auch für die Tagesansicht im Essen-Tab.
 * Null, wenn es nichts Besonderes zu sagen gibt (normales Ziel).
 */
internal fun calorieNoteText(t: CalorieTarget): String? = when (t.note) {
    CalorieNote.NO_GOAL -> tr("Kein Gewichtsziel gesetzt: Erhaltungsbedarf.", "No weight goal set: maintenance calories.")
    CalorieNote.NO_DATE -> tr("Kein Zieldatum gesetzt: Erhaltungsbedarf.", "No target date set: maintenance calories.")
    CalorieNote.DATE_PASSED -> tr("Zieldatum ist vorbei: Erhaltungsbedarf.", "Target date has passed: maintenance calories.")
    CalorieNote.TOO_TIGHT -> tooTightText(t)
    CalorieNote.REACHED -> tr("Gewichtsziel erreicht: Erhaltungsbedarf.", "Weight goal reached: maintenance calories.")
    CalorieNote.NONE -> null
}

/**
 * Deutlicher Hinweis: Das Zieldatum ist mit dem erlaubten Defizit bzw. Plus nicht zu schaffen.
 * Nennt die Grenze und, wenn möglich, den Tag, an dem das Gewicht damit erreicht wird.
 */
private fun tooTightText(t: CalorieTarget): String {
    val what = if (t.direction == CalorieDirection.GAIN) tr("Überschuss", "surplus") else tr("Defizit", "deficit")
    val eta = t.estimatedEpochDay?.let {
        tr("So erreichst du dein Gewicht voraussichtlich erst am ${WeightFormat.date(it)}.", "At this pace you'll likely reach your weight on ${WeightFormat.date(it)}.")
    } ?: tr("So ist dein Gewicht in absehbarer Zeit nicht erreichbar.", "At this pace you won't reach your weight any time soon.")

    return tr("Zieldatum zu knapp: Das Tagesziel ist auf ${kcalText(t.appliedDeltaKcal)} $what begrenzt, ", "Target date too close: the daily target is capped at a ${kcalText(t.appliedDeltaKcal)} $what ") +
            tr("damit es gesund bleibt. $eta", "to keep it healthy. $eta")
}

private fun kcalText(v: Int): String = "%,d kcal".format(L10n.locale, v)

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
            SectionLabel(tr("Kalorienziel", "Calorie goal"))
            Spacer(modifier = Modifier.height(8.dp))

            when {
                !hasProfile -> Text(
                    text = tr("Lege dein Profil an, um dein Tagesziel zu berechnen.", "Create your profile to calculate your daily target."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                target == null -> Text(
                    text = tr("Trage zuerst dein Gewicht im Gewicht-Tab ein.", "Log your weight in the Weight tab first."),
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
                        text = tr("Grundumsatz ${kcalText(target.bmr)} \u00B7 Gesamtumsatz ${kcalText(target.tdee)}", "BMR ${kcalText(target.bmr)} · TDEE ${kcalText(target.tdee)}"),
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
                        Text(if (hasProfile) tr("Profil bearbeiten", "Edit profile") else tr("Profil anlegen", "Create profile"))
                    }
                }
            }
        }
    }
}
