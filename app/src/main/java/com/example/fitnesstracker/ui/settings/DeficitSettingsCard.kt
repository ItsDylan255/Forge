package com.example.fitnesstracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fitnesstracker.data.local.CaloriePreferences
import com.example.fitnesstracker.domain.calories.CalorieSettings
import com.example.fitnesstracker.domain.calories.DeficitMode
import com.example.fitnesstracker.i18n.L10n
import com.example.fitnesstracker.i18n.tr
import com.example.fitnesstracker.ui.weight.ForgeCard

/**
 * Defizit-Einstellungen: maximales Defizit, Automatik an/aus und – bei ausgeschalteter Automatik –
 * ein festes Defizit in kcal oder ein prozentualer Gewichtsverlust pro Woche.
 */
@Composable
internal fun DeficitSettingsCard(settings: CalorieSettings, onSave: (CalorieSettings) -> Unit) {
    var maxText by rememberSaveable(settings) { mutableStateOf(settings.maxDeficitKcal?.toString() ?: "") }
    var auto by rememberSaveable(settings) { mutableStateOf(settings.mode == DeficitMode.AUTO) }
    var percentMode by rememberSaveable(settings) { mutableStateOf(settings.mode == DeficitMode.PERCENT_PER_WEEK) }
    var fixedText by rememberSaveable(settings) { mutableStateOf(settings.fixedDeficitKcal.toString()) }
    var percentText by rememberSaveable(settings) { mutableStateOf(percentLabel(settings.percentPerWeek)) }

    val maxParsed: Int? = maxText.trim().toIntOrNull()
    val maxValid = maxText.isBlank() ||
        (maxParsed != null && maxParsed in CaloriePreferences.MIN_MAX_DEFICIT..CaloriePreferences.MAX_MAX_DEFICIT)
    val fixedParsed = fixedText.trim().toIntOrNull()
    val fixedValid = fixedParsed != null && fixedParsed in CaloriePreferences.MIN_FIXED_DEFICIT..CaloriePreferences.MAX_FIXED_DEFICIT
    val percentParsed = percentText.trim().replace(',', '.').toDoubleOrNull()
    val percentValid = percentParsed != null && percentParsed in CaloriePreferences.MIN_PERCENT..CaloriePreferences.MAX_PERCENT

    val manualValid = auto || (if (percentMode) percentValid else fixedValid)
    val valid = maxValid && manualValid

    val edited = CalorieSettings(
        reductionKcal = settings.reductionKcal,
        maxDeficitKcal = if (maxText.isBlank()) null else maxParsed,
        mode = when {
            auto -> DeficitMode.AUTO
            percentMode -> DeficitMode.PERCENT_PER_WEEK
            else -> DeficitMode.FIXED_KCAL
        },
        fixedDeficitKcal = if (fixedValid) fixedParsed!! else settings.fixedDeficitKcal,
        percentPerWeek = if (percentValid) percentParsed!! else settings.percentPerWeek
    )
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val error = MaterialTheme.colorScheme.error

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            // ---------- Maximales Defizit ----------
            Text(tr("Maximales Defizit", "Maximum deficit"), style = MaterialTheme.typography.titleSmall)
            Text(
                text = tr(
                    "Das Tagesziel liegt nie mehr als diesen Wert unter deinem Gesamtumsatz (und nie unter dem Grundumsatz). Leer = Standard: 25 % des Umsatzes, höchstens 1.000 kcal.",
                    "Your daily target never goes more than this below your TDEE (and never below your BMR). Empty = default: 25 % of TDEE, at most 1,000 kcal."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = maxText,
                onValueChange = { maxText = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text(tr("kcal pro Tag", "kcal per day")) },
                placeholder = { Text(tr("Standard", "Default")) },
                singleLine = true,
                isError = !maxValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            if (!maxValid) {
                Text(
                    tr("${CaloriePreferences.MIN_MAX_DEFICIT} bis ${CaloriePreferences.MAX_MAX_DEFICIT} kcal oder leer", "${CaloriePreferences.MIN_MAX_DEFICIT} to ${CaloriePreferences.MAX_MAX_DEFICIT} kcal or empty"),
                    style = MaterialTheme.typography.bodySmall,
                    color = error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---------- Automatik ----------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(tr("Automatisch anpassen", "Adjust automatically"), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (auto) tr(
                            "Das Defizit wird aus Zielgewicht und Zieldatum berechnet und passt sich mit jedem Gewichtseintrag an.",
                            "The deficit is calculated from your goal weight and date and adapts with every weigh-in."
                        ) else tr(
                            "Aus: Du legst das Defizit selbst fest. Das Zieldatum wird nicht mehr verwendet.",
                            "Off: you set the deficit yourself. The target date is no longer used."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }
                Switch(checked = auto, onCheckedChange = { auto = it }, modifier = Modifier.padding(start = 12.dp))
            }

            // ---------- Manuelles Defizit ----------
            if (!auto) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !percentMode,
                        onClick = { percentMode = false },
                        label = { Text(tr("Feste kcal", "Fixed kcal")) }
                    )
                    FilterChip(
                        selected = percentMode,
                        onClick = { percentMode = true },
                        label = { Text(tr("% pro Woche", "% per week")) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (percentMode) {
                    OutlinedTextField(
                        value = percentText,
                        onValueChange = { percentText = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(4) },
                        label = { Text(tr("% des Gewichts pro Woche", "% of body weight per week")) },
                        singleLine = true,
                        isError = !percentValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (percentValid) tr(
                            "Beispiel: 1 % bei 80 kg = 0,8 kg pro Woche ≈ 880 kcal Defizit pro Tag. Wird mit jedem Gewichtseintrag neu berechnet.",
                            "Example: 1 % at 80 kg = 0.8 kg per week ≈ 880 kcal deficit per day. Recalculated with every weigh-in."
                        ) else tr(
                            "${percentLabel(CaloriePreferences.MIN_PERCENT)} bis ${percentLabel(CaloriePreferences.MAX_PERCENT)} %",
                            "${percentLabel(CaloriePreferences.MIN_PERCENT)} to ${percentLabel(CaloriePreferences.MAX_PERCENT)} %"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (percentValid) muted else error
                    )
                } else {
                    OutlinedTextField(
                        value = fixedText,
                        onValueChange = { fixedText = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text(tr("Defizit in kcal pro Tag", "Deficit in kcal per day")) },
                        singleLine = true,
                        isError = !fixedValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!fixedValid) {
                        Text(
                            tr("${CaloriePreferences.MIN_FIXED_DEFICIT} bis ${CaloriePreferences.MAX_FIXED_DEFICIT} kcal", "${CaloriePreferences.MIN_FIXED_DEFICIT} to ${CaloriePreferences.MAX_FIXED_DEFICIT} kcal"),
                            style = MaterialTheme.typography.bodySmall,
                            color = error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onSave(edited) },
                enabled = valid && edited != settings,
                modifier = Modifier.fillMaxWidth()
            ) { Text(tr("Speichern", "Save")) }
        }
    }
}

private fun percentLabel(value: Double): String =
    String.format(L10n.locale, "%.1f", value)
