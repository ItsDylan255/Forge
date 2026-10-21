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

internal fun activityLabel(a: ActivityLevel): String = when (a) {
    ActivityLevel.SEDENTARY -> "Sitzend (1,2)"
    ActivityLevel.LIGHT -> "Leicht aktiv, 1–3 Tage Sport (1,4)"
    ActivityLevel.MODERATE -> "Moderat, 3–5 Tage Sport (1,55)"
    ActivityLevel.VERY_ACTIVE -> "Sehr aktiv, 6–7 Tage Sport (1,725)"
}

internal fun sexLabel(s: Sex): String = if (s == Sex.MALE) "Männlich" else "Weiblich"

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
        title = { Text("Profil") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = height, onValueChange = { height = it; error = null },
                    label = { Text("Größe (cm)") }, singleLine = true,
                    keyboardOptions = numberKeys, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = age, onValueChange = { age = it; error = null },
                    label = { Text("Alter (Jahre)") }, singleLine = true,
                    keyboardOptions = numberKeys, modifier = Modifier.fillMaxWidth()
                )
                Text("Geschlecht", style = MaterialTheme.typography.labelMedium)
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
                Text("Aktivität", style = MaterialTheme.typography.labelMedium)
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
                Text("Makro-Verteilung (%)", style = MaterialTheme.typography.labelMedium)
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
                        label = { Text("Fett") }, singleLine = true,
                        keyboardOptions = numberKeys, modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    "Summe: $sum %",
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
        confirmButton = { TextButton(onClick = { trySave() }) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
