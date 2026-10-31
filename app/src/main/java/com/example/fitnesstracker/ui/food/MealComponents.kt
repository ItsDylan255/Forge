package com.example.fitnesstracker.ui.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fitnesstracker.data.local.FoodItem
import com.example.fitnesstracker.domain.food.FoodCalculator
import com.example.fitnesstracker.domain.food.FoodFormat
import com.example.fitnesstracker.domain.food.FoodInputParser
import com.example.fitnesstracker.domain.food.FoodParseResult
import com.example.fitnesstracker.domain.food.Macros
import com.example.fitnesstracker.domain.food.MealCalculator
import com.example.fitnesstracker.domain.food.MealIngredientData
import com.example.fitnesstracker.domain.food.MealInputParser
import com.example.fitnesstracker.domain.food.ServingInputParser
import com.example.fitnesstracker.i18n.tr

/** Eine Zutat während der Bearbeitung. Die Gramm bleiben als Text, bis gespeichert wird. */
private data class IngredientDraft(
    val name: String,
    val per100g: Macros,
    val grams: String
)

/** Zutat aus der Lebensmittel-Liste wählen. */
@Composable
internal fun FoodPickerDialog(
    foods: List<FoodItem>,
    onPick: (FoodItem) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = foods.filter { it.name.contains(query.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Zutat w\u00E4hlen", "Choose ingredient")) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Suchen", "Search")) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filtered.isEmpty()) {
                    Text(
                        text = if (foods.isEmpty()) tr("Lege zuerst unter Lebensmittel Zutaten an.", "Add ingredients under Foods first.")
                        else tr("Nichts gefunden.", "Nothing found."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(filtered, key = { it.id }) { food ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(food) }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(food.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = food.per100gText(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Schlie\u00DFen", "Close")) } }
    )
}

/** Mahlzeit anlegen oder bearbeiten: Name, Zutaten mit Gramm und Live-Summe. */
@Composable
internal fun MealEditDialog(
    title: String,
    initial: MealUi?,
    foods: List<FoodItem>,
    externalError: String?,
    onSubmit: (name: String, ingredients: List<MealIngredientData>, servings: Double) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var servingsInput by remember { mutableStateOf(initial?.servings?.let { FoodFormat.number(it) } ?: "1") }
    val drafts = remember {
        mutableStateListOf<IngredientDraft>().apply {
            initial?.ingredients?.forEach {
                add(IngredientDraft(it.name, it.per100g, FoodFormat.number(it.grams)))
            }
        }
    }
    var picking by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val cleanName = when (val r = MealInputParser.parseName(name)) {
            is FoodParseResult.Valid -> r.value
            is FoodParseResult.Invalid -> { localError = r.message; return }
        }

        val items = mutableListOf<MealIngredientData>()
        for (d in drafts) {
            when (val r = MealInputParser.parseEatenGrams(d.grams)) {
                is FoodParseResult.Valid -> items.add(MealIngredientData(d.name, r.value, d.per100g))
                is FoodParseResult.Invalid -> { localError = "${d.name}: ${r.message}"; return }
            }
        }

        when (val r = MealInputParser.validateIngredients(items)) {
            is FoodParseResult.Valid -> Unit
            is FoodParseResult.Invalid -> { localError = r.message; return }
        }

        val servings = when (val r = ServingInputParser.parseServings(servingsInput)) {
            is FoodParseResult.Valid -> r.value
            is FoodParseResult.Invalid -> { localError = r.message; return }
        }

        localError = null
        onSubmit(cleanName, items, servings)
    }

    // Live-Summe aus allen Zutaten mit gültiger Menge
    val valid = drafts.mapNotNull { d ->
        (MealInputParser.parseEatenGrams(d.grams) as? FoodParseResult.Valid)?.let {
            MealIngredientData(d.name, it.value, d.per100g)
        }
    }
    val totals = MealCalculator.totals(valid)
    val totalGrams = MealCalculator.totalGrams(valid)
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

                OutlinedTextField(
                    value = servingsInput,
                    onValueChange = { servingsInput = it; localError = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Ergibt (Portionen)", "Makes (servings)")) },
                    supportingText = { Text(tr("Zum Beispiel 4, wenn der Topf für 4 Portionen reicht.", "For example 4 if the pot makes 4 servings.")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = tr("Zutaten", "Ingredients"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (drafts.isEmpty()) {
                    Text(
                        text = tr("Noch keine Zutat. Tippe auf \u201E+ Zutat\u201C.", "No ingredients yet. Tap “+ Ingredient”."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                drafts.forEachIndexed { index, draft ->
                    val grams = (MealInputParser.parseEatenGrams(draft.grams) as? FoodParseResult.Valid)?.value
                    val kcalText = grams?.let {
                        FoodFormat.kcal(FoodCalculator.scale(draft.per100g, it).kcal)
                    } ?: "\u2014"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(draft.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = kcalText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = draft.grams,
                            onValueChange = {
                                drafts[index] = draft.copy(grams = it)
                                localError = null
                            },
                            modifier = Modifier.width(96.dp),
                            label = { Text("g") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        TextButton(onClick = { drafts.removeAt(index) }) {
                            Text("\u2715", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                TextButton(onClick = { picking = true }) { Text(tr("+ Zutat", "+ Ingredient")) }

                if (valid.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tr("Gesamt: ${FoodFormat.grams(totalGrams)} \u00B7 ${FoodFormat.kcal(totals.kcal)}", "Total: ${FoodFormat.grams(totalGrams)} · ${FoodFormat.kcal(totals.kcal)}"),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "P ${FoodFormat.macro(totals.protein)} \u00B7 " +
                                "${tr("K", "C")} ${FoodFormat.macro(totals.carbs)} \u00B7 " +
                                "F ${FoodFormat.macro(totals.fat)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        Text(tr("Mahlzeit l\u00F6schen", "Delete meal"), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { submit() }) { Text(tr("Speichern", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Abbrechen", "Cancel")) } }
    )

    if (picking) {
        FoodPickerDialog(
            foods = foods,
            onPick = { food ->
                drafts.add(
                    IngredientDraft(
                        name = food.name,
                        per100g = Macros(
                            food.caloriesPer100g,
                            food.proteinPer100g,
                            food.carbsPer100g,
                            food.fatPer100g
                        ),
                        grams = "100"
                    )
                )
                localError = null
                picking = false
            },
            onDismiss = { picking = false }
        )
    }
}
