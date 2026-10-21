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
        GymHeader(title = "Hinzufügen", onBack = onBack)

        Text(
            text = "Eintrag für: $dayLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Suchen") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        GymTabs(
            labels = listOf("Lebensmittel", "Mahlzeiten"),
            selected = tab,
            onSelect = onTabChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (tab == 0) {
            OutlinedButton(onClick = onNewFood, modifier = Modifier.fillMaxWidth()) {
                Text("+ Neues Lebensmittel")
            }

            val filtered = foods.filter { it.name.contains(search, ignoreCase = true) }

            when {
                foods.isEmpty() -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    EmptyHint("Noch keine Lebensmittel. Lege zum Beispiel „Haferflocken“ mit den Werten pro 100 g an.")
                }

                filtered.isEmpty() -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    EmptyHint("Nichts gefunden.")
                }

                else -> {
                    // Ohne Suche stehen die zuletzt benutzten Lebensmittel oben
                    val recents = if (search.isEmpty()) {
                        recentFoodIds.mapNotNull { id -> foods.firstOrNull { it.id == id } }
                    } else {
                        emptyList()
                    }

                    if (recents.isNotEmpty()) {
                        GroupLabel("Zuletzt verwendet")
                        FoodRows(recents, onPickFood)
                        GroupLabel("Alle Lebensmittel")
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    FoodRows(filtered, onPickFood)
                }
            }
        } else {
            OutlinedButton(onClick = onNewMeal, modifier = Modifier.fillMaxWidth()) {
                Text("+ Neue Mahlzeit")
            }

            val filtered = meals.filter { it.name.contains(search, ignoreCase = true) }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                meals.isEmpty() ->
                    EmptyHint("Noch keine Mahlzeiten. Lege zum Beispiel „Haferbrei“ aus Haferflocken und Milch an.")

                filtered.isEmpty() -> EmptyHint("Nichts gefunden.")

                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filtered.forEach { meal ->
                        val totals = MealCalculator.totals(meal.ingredients)
                        val grams = MealCalculator.totalGrams(meal.ingredients)

                        ForgeCard(modifier = Modifier.fillMaxWidth(), onClick = { onPickMeal(meal.id) }) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                Text(text = meal.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${meal.ingredients.size} Zutaten \u00B7 ${FoodFormat.grams(grams)} \u00B7 " +
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
                        text = "Pro 100 g: ${food.per100gText()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
