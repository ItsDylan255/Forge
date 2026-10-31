package com.example.fitnesstracker.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitnesstracker.data.local.FoodItem
import com.example.fitnesstracker.domain.food.FoodFormat
import com.example.fitnesstracker.domain.food.MealCalculator
import com.example.fitnesstracker.i18n.tr
import com.example.fitnesstracker.ui.gym.EmptyHint
import com.example.fitnesstracker.ui.gym.GroupLabel
import com.example.fitnesstracker.ui.gym.GymHeader
import com.example.fitnesstracker.ui.gym.GymPage
import com.example.fitnesstracker.ui.gym.GymTabs
import com.example.fitnesstracker.ui.weight.ForgeCard

/**
 * Seite zum Hinzufügen: Suche, Lebensmittel oder Mahlzeiten und oben "Zuletzt verwendet".
 * Tippen auf einen Eintrag öffnet die Mengen-Seite, neue Lebensmittel und Mahlzeiten werden hier angelegt.
 */
@Composable
internal fun AddEntryPage(
    foods: List<FoodItem>,
    meals: List<MealUi>,
    recentFoodIds: List<Long>,
    dayLabel: String,
    tab: Int,
    onTabChange: (Int) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onPickFood: (foodId: Long) -> Unit,
    onPickMeal: (mealId: Long) -> Unit,
    onNewFood: () -> Unit,
    onNewMeal: () -> Unit,
    onBack: () -> Unit
) {
    val search = query.trim()

    GymPage {
        GymHeader(title = tr("Hinzufügen", "Add"), onBack = onBack)

        Text(
            text = tr("Eintrag für: $dayLabel", "Entry for: $dayLabel"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tr("Suchen", "Search")) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        GymTabs(
            labels = listOf(tr("Lebensmittel", "Foods"), tr("Mahlzeiten", "Meals")),
            selected = tab,
            onSelect = onTabChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (tab == 0) {
            OutlinedButton(onClick = onNewFood, modifier = Modifier.fillMaxWidth()) {
                Text(tr("+ Neues Lebensmittel", "+ New food"))
            }

            val filtered = foods.filter { it.name.contains(search, ignoreCase = true) }

            when {
                foods.isEmpty() -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    EmptyHint(tr("Noch keine Lebensmittel. Lege zum Beispiel „Haferflocken“ mit den Werten pro 100 g an.", "No foods yet. Add one, for example “Oats” with its values per 100 g."))
                }

                filtered.isEmpty() -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    EmptyHint(tr("Nichts gefunden.", "Nothing found."))
                }

                else -> {
                    // Ohne Suche stehen die zuletzt benutzten Lebensmittel oben
                    val recents = if (search.isEmpty()) {
                        recentFoodIds.mapNotNull { id -> foods.firstOrNull { it.id == id } }
                    } else {
                        emptyList()
                    }

                    if (recents.isNotEmpty()) {
                        GroupLabel(tr("Zuletzt verwendet", "Recently used"))
                        FoodRows(recents, onPickFood)
                        GroupLabel(tr("Alle Lebensmittel", "All foods"))
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    FoodRows(filtered, onPickFood)
                }
            }
        } else {
            OutlinedButton(onClick = onNewMeal, modifier = Modifier.fillMaxWidth()) {
                Text(tr("+ Neue Mahlzeit", "+ New meal"))
            }

            val filtered = meals.filter { it.name.contains(search, ignoreCase = true) }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                meals.isEmpty() ->
                    EmptyHint(tr("Noch keine Mahlzeiten. Lege zum Beispiel „Haferbrei“ aus Haferflocken und Milch an.", "No meals yet. Create one, for example “Porridge” from oats and milk."))

                filtered.isEmpty() -> EmptyHint(tr("Nichts gefunden.", "Nothing found."))

                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filtered.forEach { meal ->
                        val totals = MealCalculator.totals(meal.ingredients)
                        val grams = MealCalculator.totalGrams(meal.ingredients)

                        ForgeCard(modifier = Modifier.fillMaxWidth(), onClick = { onPickMeal(meal.id) }) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                Text(text = meal.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = tr("${meal.ingredients.size} Zutaten \u00B7 ${FoodFormat.grams(grams)} \u00B7 ", "${meal.ingredients.size} ingredients · ${FoodFormat.grams(grams)} · ") +
                                            FoodFormat.kcal(totals.kcal),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodRows(foods: List<FoodItem>, onPick: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        foods.forEach { food ->
            ForgeCard(modifier = Modifier.fillMaxWidth(), onClick = { onPick(food.id) }) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(text = food.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = tr("Pro 100 g: ${food.per100gText()}", "Per 100 g: ${food.per100gText()}"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
