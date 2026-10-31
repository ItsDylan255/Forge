package com.example.fitnesstracker.ui.food

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.domain.food.FoodFormat
import com.example.fitnesstracker.domain.food.FoodInputParser
import com.example.fitnesstracker.domain.food.FoodParseResult
import com.example.fitnesstracker.domain.food.Macros
import com.example.fitnesstracker.domain.food.PortionData
import com.example.fitnesstracker.domain.food.ServingCalculator
import com.example.fitnesstracker.domain.food.ServingInputParser
import com.example.fitnesstracker.i18n.tr
import com.example.fitnesstracker.ui.gym.GymHeader
import com.example.fitnesstracker.ui.gym.GymPage
import com.example.fitnesstracker.ui.gym.StatBox
import com.example.fitnesstracker.ui.weight.ForgeCard

/** Auswahl der Einheit: -1 = Gramm, sonst die Nummer der Portion. */
private const val GRAM_UNIT = -1

/**
 * Menge wählen für ein Lebensmittel oder eine Mahlzeit, in Gramm oder in Portionen
 * (zum Beispiel 2 × Scheibe). Kalorien und Makros erscheinen live.
 * Vorauswahl: die erste Portion mit Anzahl 1, gibt es keine, dann Gramm.
 * [onAdd] bekommt die Gramm und, bei Eingabe per Portion, den Hinweis für das Tagebuch.
 */
@Composable
internal fun AmountPage(
    name: String,
    subtitle: String,
    dayLabel: String,
    defaultGrams: Double,
    quickAmounts: List<Pair<String, Double>>,
    portions: List<PortionData>,
    editLabel: String,
    macrosFor: (grams: Double) -> Macros,
    onAdd: (grams: Double, servingNote: String?) -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    var unit by rememberSaveable(name) {
        mutableStateOf(ServingCalculator.defaultPortionIndex(portions.size) ?: GRAM_UNIT)
    }
    var gramsInput by rememberSaveable(name) { mutableStateOf(FoodFormat.number(defaultGrams)) }
    var countInput by rememberSaveable(name) { mutableStateOf("1") }
    var error by rememberSaveable(name) { mutableStateOf<String?>(null) }

    // Wurden die Portionen inzwischen geändert, gilt Gramm
    val portion = portions.getOrNull(unit)

    val count = (ServingInputParser.parseCount(countInput) as? FoodParseResult.Valid)?.value
    val typedGrams = (FoodInputParser.parseGrams(gramsInput) as? FoodParseResult.Valid)?.value
    val effectiveGrams: Double? =
        if (portion != null) count?.let { ServingCalculator.gramsFor(portion.grams, it) } else typedGrams
    val preview = effectiveGrams?.let { macrosFor(it) }

    fun selectUnit(newUnit: Int) {
        if (newUnit == GRAM_UNIT) {
            // Die bisherige Menge in Gramm mitnehmen
            effectiveGrams?.let { gramsInput = FoodFormat.number(it) }
        } else {
            countInput = "1"
        }
        unit = newUnit
        error = null
    }

    GymPage {
        GymHeader(title = name, onBack = onBack)

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = tr("Eintrag für: $dayLabel", "Entry for: $dayLabel"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        ForgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = preview?.let { FoodFormat.kcal(it.kcal) } ?: "\u2014",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 40.sp,
                        lineHeight = 46.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = when {
                        effectiveGrams == null -> "\u2014"
                        portion != null && count != null ->
                            "${ServingCalculator.entryNote(count, portion.name)} \u00B7 ${FoodFormat.grams(effectiveGrams)}"
                        else -> tr("für ${FoodFormat.grams(effectiveGrams)}", "for ${FoodFormat.grams(effectiveGrams)}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("Protein", preview?.let { FoodFormat.macro(it.protein) } ?: "\u2014", Modifier.weight(1f))
            StatBox(tr("Kohlenhydrate", "Carbs"), preview?.let { FoodFormat.macro(it.carbs) } ?: "\u2014", Modifier.weight(1f))
            StatBox(tr("Fett", "Fat"), preview?.let { FoodFormat.macro(it.fat) } ?: "\u2014", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- Einheit ----------
        if (portions.isNotEmpty()) {
            Text(
                text = tr("Einheit", "Unit"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UnitChip(tr("Gramm", "Grams"), selected = portion == null) { selectUnit(GRAM_UNIT) }
                portions.forEachIndexed { index, item ->
                    UnitChip(
                        label = "${item.name} \u00B7 ${FoodFormat.grams(item.grams)}",
                        selected = portion != null && index == unit
                    ) { selectUnit(index) }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- Menge ----------
        if (portion != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        countInput = FoodFormat.number(ServingCalculator.stepCount(count ?: 1.0, up = false))
                        error = null
                    }
                ) { Text("\u2212") }

                OutlinedTextField(
                    value = countInput,
                    onValueChange = {
                        countInput = it
                        error = null
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(tr("Anzahl (${portion.name})", "Amount (${portion.name})")) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedButton(
                    onClick = {
                        countInput = FoodFormat.number(ServingCalculator.stepCount(count ?: 1.0, up = true))
                        error = null
                    }
                ) { Text("+") }
            }
        } else {
            OutlinedTextField(
                value = gramsInput,
                onValueChange = {
                    gramsInput = it
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("Menge in Gramm", "Amount in grams")) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { message -> { Text(message) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickAmounts.forEach { (label, grams) ->
                    UnitChip(label, selected = false) {
                        gramsInput = FoodFormat.number(grams)
                        error = null
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (portion != null) {
                    when (val result = ServingInputParser.parseCount(countInput)) {
                        is FoodParseResult.Valid -> {
                            val grams = ServingCalculator.gramsFor(portion.grams, result.value)
                            if (grams > FoodInputParser.MAX_GRAMS) {
                                error = tr("Die Menge darf höchstens 10.000 g betragen.", "The amount can be at most 10,000 g.")
                            } else {
                                onAdd(grams, ServingCalculator.entryNote(result.value, portion.name))
                            }
                        }
                        is FoodParseResult.Invalid -> error = result.message
                    }
                } else {
                    when (val result = FoodInputParser.parseGrams(gramsInput)) {
                        is FoodParseResult.Valid -> onAdd(result.value, null)
                        is FoodParseResult.Invalid -> error = result.message
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(tr("Hinzufügen", "Add")) }

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text(editLabel) }
    }
}

/** Auswahl-Knopf: ausgewählt in der Hauptfarbe, sonst gedämpft. */
@Composable
private fun UnitChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}
