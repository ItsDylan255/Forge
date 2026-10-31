package com.example.fitnesstracker.ui.calories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fitnesstracker.domain.calories.ActivityLevel
import com.example.fitnesstracker.domain.calories.CalorieInputParser
import com.example.fitnesstracker.domain.calories.CalorieParse
import com.example.fitnesstracker.domain.calories.CalorieProfile
import com.example.fitnesstracker.domain.calories.Sex
import com.example.fitnesstracker.i18n.tr

internal fun activityLabel(a: ActivityLevel): String = when (a) {
    ActivityLevel.SEDENTARY -> tr("Sitzend (1,2)", "Sedentary (1.2)")
    ActivityLevel.LIGHT -> tr("Leicht aktiv, 1–3 Tage Sport (1,4)", "Lightly active, 1–3 days of exercise (1.4)")
    ActivityLevel.MODERATE -> tr("Moderat, 3–5 Tage Sport (1,55)", "Moderate, 3–5 days of exercise (1.55)")
    ActivityLevel.VERY_ACTIVE -> tr("Sehr aktiv, 6–7 Tage Sport (1,725)", "Very active, 6–7 days of exercise (1.725)")
}

internal fun sexLabel(s: Sex): String = if (s == Sex.MALE) tr("Männlich", "Male") else tr("Weiblich", "Female")

/** Dialog zum Anlegen/Bearbeiten des Profils. Prüft alle Eingaben über den Parser. */
@Composable
fun CalorieProfileDialog(
    initial: CalorieProfile?,
    onSave: (CalorieProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var height by rememberSaveable { mutableStateOf(initial?.heightCm?.toString() ?: "") }
    var age by rememberSaveable { mutableStateOf(initial?.ageYears?.toString() ?: "") }
    var sex by rememberSaveable { mutableStateOf((initial?.sex ?: Sex.MALE).name) }
    var activity by rememberSaveable { mutableStateOf((initial?.activity ?: ActivityLevel.LIGHT).name) }
    var protein by rememberSaveable { mutableStateOf((initial?.split?.proteinPercent ?: 30).toString()) }
    var carbs by rememberSaveable { mutableStateOf((initial?.split?.carbsPercent ?: 40).toString()) }
    var fat by rememberSaveable { mutableStateOf((initial?.split?.fatPercent ?: 30).toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun trySave() {
        val h = CalorieInputParser.parseHeight(height)
        if (h is CalorieParse.Error) { error = h.message; return }
        val a = CalorieInputParser.parseAge(age)
        if (a is CalorieParse.Error) { error = a.message; return }
        val s = CalorieInputParser.parseSplit(protein, carbs, fat)
        if (s is CalorieParse.Error) { error = s.message; return }
        onSave(
            CalorieProfile(
                heightCm = (h as CalorieParse.Ok).value,
                ageYears = (a as CalorieParse.Ok).value,
                sex = Sex.valueOf(sex),
                activity = ActivityLevel.valueOf(activity),
                split = (s as CalorieParse.Ok).value
            )
        )
    }

    val sum = (protein.trim().removeSuffix("%").trim().toIntOrNull() ?: 0) +
        (carbs.trim().removeSuffix("%").trim().toIntOrNull() ?: 0) +
        (fat.trim().removeSuffix("%").trim().toIntOrNull() ?: 0)
    val numberKeys = KeyboardOptions(keyboardType = KeyboardType.Number)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Profil", "Profile")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = height, onValueChange = { height = it; error = null },
                    label = { Text(tr("Größe (cm)", "Height (cm)")) }, singleLine = true,
                    keyboardOptions = numberKeys, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = age, onValueChange = { age = it; error = null },
                    label = { Text(tr("Alter (Jahre)", "Age (years)")) }, singleLine = true,
                    keyboardOptions = numberKeys, modifier = Modifier.fillMaxWidth()
                )
                Text(tr("Geschlecht", "Sex"), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Sex.entries.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { sex = option.name }
                        ) {
                            RadioButton(selected = sex == option.name, onClick = null)
                            Text(sexLabel(option), modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
                Text(tr("Aktivität", "Activity"), style = MaterialTheme.typography.labelMedium)
                ActivityLevel.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activity = option.name }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = activity == option.name, onClick = null)
                        Text(activityLabel(option), modifier = Modifier.padding(start = 6.dp))
                    }
                }
                Text(tr("Makro-Verteilung (%)", "Macro split (%)"), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein, onValueChange = { protein = it; error = null },
                        label = { Text("Protein") }, singleLine = true,
                        keyboardOptions = numberKeys, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs, onValueChange = { carbs = it; error = null },
                        label = { Text("Carbs") }, singleLine = true,
                        keyboardOptions = numberKeys, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fat, onValueChange = { fat = it; error = null },
                        label = { Text(tr("Fett", "Fat")) }, singleLine = true,
                        keyboardOptions = numberKeys, modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    tr("Summe: $sum %", "Total: $sum %"),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sum == 100) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = { trySave() }) { Text(tr("Speichern", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Abbrechen", "Cancel")) } }
    )
}
