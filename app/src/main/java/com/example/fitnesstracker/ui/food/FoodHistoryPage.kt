package com.example.fitnesstracker.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.domain.food.FoodCalculator
import com.example.fitnesstracker.domain.food.FoodFormat
import com.example.fitnesstracker.domain.food.FoodHistory
import com.example.fitnesstracker.domain.food.FoodLogData
import com.example.fitnesstracker.i18n.tr
import com.example.fitnesstracker.ui.gym.EmptyHint
import com.example.fitnesstracker.ui.gym.GroupLabel
import com.example.fitnesstracker.ui.gym.GymHeader
import com.example.fitnesstracker.ui.gym.GymPage
import com.example.fitnesstracker.ui.gym.StatBox
import com.example.fitnesstracker.ui.weight.ForgeCard
import com.example.fitnesstracker.ui.weight.SectionLabel

/** Verlauf: alle Tage mit Einträgen, nach Monaten gruppiert. Tippen öffnet die Übersicht des Tages. */
@Composable
internal fun FoodHistoryPage(
    entries: List<FoodLogData>,
    today: Long,
    onBack: () -> Unit,
    onOpenToday: () -> Unit,
    onOpenDay: (epochDay: Long) -> Unit
) {
    val days = FoodHistory.days(entries)

    GymPage {
        GymHeader(title = tr("Verlauf", "History"), onBack = onBack)

        OutlinedButton(onClick = onOpenToday, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Heute öffnen", "Open today"))
        }

        if (days.isEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            EmptyHint(tr("Noch nichts eingetragen. Sobald du etwas einträgst, erscheinen hier die Tage.", "Nothing logged yet. Once you log something, your days show up here."))
        } else {
            days.groupBy { FoodHistory.monthLabel(it.epochDay) }.forEach { (month, monthDays) ->
                GroupLabel(month)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    monthDays.forEach { day ->
                        ForgeCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOpenDay(day.epochDay) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = FoodFormat.dayLabel(day.epochDay, today),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = tr("${day.entryCount} Einträge \u00B7 ", "${day.entryCount} entries · ") +
                                                "P ${FoodFormat.macro(day.totals.protein)} \u00B7 " +
                                                "${tr("K", "C")} ${FoodFormat.macro(day.totals.carbs)} \u00B7 " +
                                                "F ${FoodFormat.macro(day.totals.fat)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = FoodFormat.kcal(day.totals.kcal),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Übersicht eines Tages: Kalorien gesamt, Makros und alles, was gegessen wurde. */
@Composable
internal fun FoodDayOverviewPage(
    epochDay: Long,
    today: Long,
    entries: List<FoodLogData>,
    onBack: () -> Unit,
    onOpenInDay: () -> Unit
) {
    val dayEntries = entries.filter { it.dateEpochDay == epochDay }
    val totals = FoodCalculator.dayTotals(entries, epochDay)

    GymPage {
        GymHeader(title = FoodFormat.dayLabel(epochDay, today), onBack = onBack)

        ForgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                SectionLabel(tr("Kalorien gesamt", "Total calories"))

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = FoodFormat.kcal(totals.kcal),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 40.sp,
                        lineHeight = 46.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = tr("${dayEntries.size} Einträge", "${dayEntries.size} entries"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("Protein", FoodFormat.macro(totals.protein), Modifier.weight(1f))
            StatBox(tr("Kohlenhydrate", "Carbs"), FoodFormat.macro(totals.carbs), Modifier.weight(1f))
            StatBox(tr("Fett", "Fat"), FoodFormat.macro(totals.fat), Modifier.weight(1f))
        }

        GroupLabel(tr("Gegessen", "Eaten"))

        if (dayEntries.isEmpty()) {
            EmptyHint(tr("An diesem Tag wurde nichts eingetragen.", "Nothing was logged on this day."))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dayEntries.forEach { entry ->
                    ForgeCard(modifier = Modifier.fillMaxWidth()) {
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
                                            "${tr("K", "C")} ${FoodFormat.macro(entry.macros.carbs)} \u00B7 " +
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

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = onOpenInDay, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Im Tag öffnen", "Open in day"))
        }
    }
}
