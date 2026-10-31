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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fitnesstracker.data.local.FoodItem
import com.example.fitnesstracker.domain.food.FoodCalculator
import com.example.fitnesstracker.domain.food.FoodFormat
import com.example.fitnesstracker.domain.food.FoodInputParser
import com.example.fitnesstracker.domain.food.FoodLogData
import com.example.fitnesstracker.domain.food.FoodParseResult
import com.example.fitnesstracker.domain.food.Macros
import com.example.fitnesstracker.domain.food.PortionData
import com.example.fitnesstracker.domain.food.ServingInputParser
import com.example.fitnesstracker.i18n.tr

/** Kurzbeschreibung eines Lebensmittels: "52 kcal · P 0,3 g · K 14,0 g · F 0,2 g" (pro 100 g). */
internal fun FoodItem.per100gText(): String =
    "${FoodFormat.kcal(caloriesPer100g)} \u00B7 P ${FoodFormat.macro(proteinPer100g)} \u00B7 " +
            "K ${FoodFormat.macro(carbsPer100g)} \u00B7 F ${FoodFormat.macro(fatPer100g)}"

private fun FoodItem.per100g() = Macros(
    kcal = caloriesPer100g,
    protein = proteinPer100g,
    carbs = carbsPer100g,
    fat = fatPer100g
)

/** Eine Portion während der Bearbeitung. Die Gramm bleiben als Text, bis gespeichert wird. */
private data class PortionDraft(val name: String, val grams: String)

/** Lebensmittel anlegen oder bearbeiten (Werte pro 100 g, dazu beliebige Portionen wie "Scheibe" mit 30 g). */
@Composable
internal fun FoodEditDialog(
    title: String,
    initial: FoodItem?,
    externalError: String?,
    onSubmit: (
        name: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        portions: List<PortionData>
    ) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    initialPortions: List<PortionData> = emptyList()
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var kcal by remember { mutableStateOf(initial?.let { FoodFormat.number(it.caloriesPer100g) } ?: "") }
    var protein by remember { mutableStateOf(initial?.let { FoodFormat.number(it.proteinPer100g) } ?: "") }
    var carbs by remember { mutableStateOf(initial?.let { FoodFormat.number(it.carbsPer100g) } ?: "") }
    var fat by remember { mutableStateOf(initial?.let { FoodFormat.number(it.fatPer100g) } ?: "") }
    val portions = remember {
        mutableStateListOf<PortionDraft>().apply {
            initialPortions.forEach { add(PortionDraft(it.name, FoodFormat.number(it.grams))) }
        }
    }
    var localError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val cleanName = when (val r = FoodInputParser.parseName(name)) {
            is FoodParseResult.Valid -> r.value
            is FoodParseResult.Invalid -> { localError = r.message; return }
        }
        val k = when (val r = FoodInputParser.parseKcal(kcal)) {
            is FoodParseResult.Valid -> r.value
            is FoodParseResult.Invalid -> { localError = r.message; return }
        }
        val p = when (val r = FoodInputParser.parseMacro(protein, "Protein")) {
            is FoodParseResult.Valid -> r.value
            is FoodParseResult.Invalid -> { localError = r.message; return }
        }
        val c = when (val r = FoodInputParser.parseMacro(carbs, tr("Kohlenhydrate", "Carbs"))) {
            is FoodParseResult.Valid -> r.value
            is FoodParseResult.Invalid -> { localError = r.message; return }
        }
        val f = when (val r = FoodInputParser.parseMacro(fat, tr("Fett", "Fat"))) {
            is FoodParseResult.Valid -> r.value
            is FoodParseResult.Invalid -> { localError = r.message; return }
        }

        // Portionen prüfen. Komplett leere Zeilen werden übersprungen.
        val portionList = mutableListOf<PortionData>()
        for (draft in portions) {
            if (draft.name.isBlank() && draft.grams.isBlank()) continue

            val portionName = when (val r = ServingInputParser.parseName(draft.name)) {
                is FoodParseResult.Valid -> r.value
                is FoodParseResult.Invalid -> { localError = r.message; return }
            }
            val portionGrams = when (val r = ServingInputParser.parseGrams(draft.grams)) {
                is FoodParseResult.Valid -> r.value
                is FoodParseResult.Invalid -> { localError = "$portionName: ${r.message}"; return }
            }
            portionList.add(PortionData(portionName, portionGrams))
        }
        when (val r = ServingInputParser.validatePortions(portionList)) {
            is FoodParseResult.Valid -> Unit
            is FoodParseResult.Invalid -> { localError = r.message; return }
        }

        localError = null
        onSubmit(cleanName, k, p, c, f, portionList)
    }

    val shownError = localError ?: externalError

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; localError = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = tr("Werte pro 100 g", "Values per 100 g"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it; localError = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Kalorien (kcal)", "Calories (kcal)")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it; localError = null },
                        modifier = Modifier.weight(1f),
                        label = { Text("Protein") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it; localError = null },
                        modifier = Modifier.weight(1f),
                        label = { Text(tr("Kohlenh.", "Carbs")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it; localError = null },
                        modifier = Modifier.weight(1f),
                        label = { Text(tr("Fett", "Fat")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                // ---------- Portionen ----------
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = tr("Portionen", "Servings"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = tr("Zum Beispiel \u201EScheibe\u201C mit 30 g. Beim Eintragen w\u00E4hlst du dann die Anzahl.", "For example “Slice” with 30 g. When logging, you then pick the amount."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                portions.forEachIndexed { index, draft ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = {
                                portions[index] = draft.copy(name = it)
                                localError = null
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Name") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = draft.grams,
                            onValueChange = {
                                portions[index] = draft.copy(grams = it)
                                localError = null
                            },
                            modifier = Modifier.width(88.dp),
                            label = { Text("g") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        TextButton(
                            onClick = {
                                portions.removeAt(index)
                                localError = null
                            }
                        ) { Text("\u2715", color = MaterialTheme.colorScheme.error) }
                    }
                }

                if (portions.size < ServingInputParser.MAX_PORTIONS) {
                    // Vorschl\u00E4ge zum Antippen, nur Namen, die noch nicht benutzt sind
                    val unused = ServingInputParser.suggestions.filter { suggestion ->
                        portions.none { it.name.trim().equals(suggestion, ignoreCase = true) }
                    }

                    if (unused.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            unused.forEach { suggestion ->
                                Text(
                                    text = "+ $suggestion",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            portions.add(PortionDraft(suggestion, ""))
                                            localError = null
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    TextButton(onClick = { portions.add(PortionDraft("", "")) }) {
                        Text(tr("+ Eigene Portion", "+ Custom serving"))
                    }
                }

                shownError?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDelete) {
                        Text(tr("Lebensmittel l\u00F6schen", "Delete food"), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { submit() }) { Text(tr("Speichern", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Abbrechen", "Cancel")) } }
    )
}

/** Menge eines Eintrags aendern oder den Eintrag loeschen. */
@Composable
internal fun FoodEntryDialog(
    entry: FoodLogData,
    onSaveGrams: (Double) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var gramsInput by remember { mutableStateOf(FoodFormat.number(entry.grams)) }
    var error by remember { mutableStateOf<String?>(null) }

    val parsed = FoodInputParser.parseGrams(gramsInput)
    val preview = (parsed as? FoodParseResult.Valid)?.let {
        FoodCalculator.rescale(entry.macros, entry.grams, it.value)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.name) },
        text = {
            Column {
                OutlinedTextField(
                    value = gramsInput,
                    onValueChange = { gramsInput = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Menge in Gramm", "Amount in grams")) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                if (preview != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = FoodFormat.kcal(preview.kcal),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "P ${FoodFormat.macro(preview.protein)} \u00B7 " +
                                "K ${FoodFormat.macro(preview.carbs)} \u00B7 " +
                                "F ${FoodFormat.macro(preview.fat)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Eintrag l\u00F6schen", "Delete entry"), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (val result = FoodInputParser.parseGrams(gramsInput)) {
                        is FoodParseResult.Valid -> onSaveGrams(result.value)
                        is FoodParseResult.Invalid -> error = result.message
                    }
                }
            ) { Text(tr("Speichern", "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Abbrechen", "Cancel")) } }
    )
}
