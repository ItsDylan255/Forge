package com.example.fitnesstracker.ui.food

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnesstracker.i18n.tr
import com.example.fitnesstracker.ui.calories.CalorieViewModel
import com.example.fitnesstracker.domain.food.FoodCalculator
import com.example.fitnesstracker.domain.food.FoodFormat
import com.example.fitnesstracker.domain.food.LoggedFoodRef
import com.example.fitnesstracker.domain.food.Macros
import com.example.fitnesstracker.domain.food.MealCalculator
import com.example.fitnesstracker.domain.food.PortionData
import com.example.fitnesstracker.domain.food.ServingCalculator
import com.example.fitnesstracker.domain.food.RecentFoods
import com.example.fitnesstracker.domain.food.toData
import com.example.fitnesstracker.ui.gym.ConfirmDialog
import com.example.fitnesstracker.ui.gym.GymTabs
import java.time.LocalDate

// Seiten des Essen-Bereichs: Hauptseite, Hinzufügen und die Mengen-Seite ("food:<id>", "meal:<id>")
private const val PAGE_MAIN = "main"
private const val PAGE_ADD = "add"
private const val PAGE_HISTORY = "history"

@Composable
fun FoodScreen(
    viewModel: FoodViewModel,
    calorieViewModel: CalorieViewModel = viewModel(),
    mealViewModel: MealViewModel = viewModel(),
    onOpenSettings: () -> Unit = {}
) {
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val log by viewModel.log.collectAsStateWithLifecycle()
    val portionRows by viewModel.portions.collectAsStateWithLifecycle()
    val calorieProfile by calorieViewModel.profile.collectAsStateWithLifecycle()
    val calorieTarget by calorieViewModel.target.collectAsStateWithLifecycle()
    val meals by mealViewModel.meals.collectAsStateWithLifecycle()

    val today = LocalDate.now().toEpochDay()

    // ---------- UI-Zustand ----------
    var selectedDay by rememberSaveable { mutableStateOf(today) }

    // Seiten: main, add, food:<id>, meal:<id>, history und day:<Tag>
    var page by rememberSaveable { mutableStateOf(PAGE_MAIN) }
    var addTab by rememberSaveable { mutableStateOf(0) } // 0 = Lebensmittel, 1 = Mahlzeiten
    var addQuery by rememberSaveable { mutableStateOf("") }
    var editingEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteEntryId by rememberSaveable { mutableStateOf<Long?>(null) }

    var creatingFood by rememberSaveable { mutableStateOf(false) }
    var editFoodId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteFoodId by rememberSaveable { mutableStateOf<Long?>(null) }
    var foodError by rememberSaveable { mutableStateOf<String?>(null) }
    var creatingMeal by rememberSaveable { mutableStateOf(false) }
    var editMealId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteMealId by rememberSaveable { mutableStateOf<Long?>(null) }
    var mealError by rememberSaveable { mutableStateOf<String?>(null) }

    // ---------- Berechnungen ----------
    val logData = log.map { it.toData() }
    val dayEntries = logData.filter { it.dateEpochDay == selectedDay }
    val dayTotals = FoodCalculator.dayTotals(logData, selectedDay)

    // Das Tagesziel kommt aus dem Kalorienziel. Ohne Profil oder Gewicht gibt es keinen Balken.
    val calorieGoal: Int? = calorieTarget?.targetKcal

    // Zuletzt eingetragene Lebensmittel, für die Hinzufügen-Seite
    val recentFoodIds = RecentFoods.ids(
        log.map { LoggedFoodRef(it.foodItemId, it.dateEpochDay, it.id) },
        limit = 5
    )

    // ---------- Seiten ----------
    // Zurück-Taste: Menge -> Suche -> Tag, Tagesübersicht -> Verlauf -> Tag
    BackHandler(enabled = page != PAGE_MAIN) {
        page = when {
            page.startsWith("day:") -> PAGE_HISTORY
            page == PAGE_HISTORY || page == PAGE_ADD -> PAGE_MAIN
            else -> PAGE_ADD
        }
    }

    val pageFood = if (page.startsWith("food:")) {
        foods.firstOrNull { it.id == page.removePrefix("food:").toLongOrNull() }
    } else {
        null
    }
    val pageMeal = if (page.startsWith("meal:")) {
        meals.firstOrNull { it.id == page.removePrefix("meal:").toLongOrNull() }
    } else {
        null
    }
    val pageDay = if (page.startsWith("day:")) page.removePrefix("day:").toLongOrNull() else null
    val dayLabel = FoodFormat.dayLabel(selectedDay, today)

    when {
        pageFood != null -> {
            val per100g = Macros(
                pageFood.caloriesPer100g,
                pageFood.proteinPer100g,
                pageFood.carbsPer100g,
                pageFood.fatPer100g
            )
            AmountPage(
                name = pageFood.name,
                subtitle = tr("Pro 100 g: ${pageFood.per100gText()}", "Per 100 g: ${pageFood.per100gText()}"),
                dayLabel = dayLabel,
                defaultGrams = 100.0,
                quickAmounts = listOf(50.0, 100.0, 150.0, 200.0).map { FoodFormat.grams(it) to it },
                portions = portionRows
                    .filter { it.foodItemId == pageFood.id }
                    .map { PortionData(it.name, it.grams) },
                editLabel = tr("Lebensmittel bearbeiten", "Edit food"),
                macrosFor = { grams -> FoodCalculator.scale(per100g, grams) },
                onAdd = { grams, note ->
                    viewModel.addEntry(pageFood, grams, selectedDay, note)
                    page = PAGE_MAIN
                },
                onEdit = {
                    foodError = null
                    editFoodId = pageFood.id
                },
                onBack = { page = PAGE_ADD }
            )
        }

        pageMeal != null -> {
            val totalGrams = MealCalculator.totalGrams(pageMeal.ingredients)
            AmountPage(
                name = pageMeal.name,
                subtitle = tr("${pageMeal.ingredients.size} Zutaten \u00B7 ${FoodFormat.grams(totalGrams)} gesamt", "${pageMeal.ingredients.size} ingredients · ${FoodFormat.grams(totalGrams)} total"),
                dayLabel = dayLabel,
                defaultGrams = totalGrams,
                quickAmounts = listOf(
                    "\u00BC" to totalGrams / 4,
                    "\u00BD" to totalGrams / 2,
                    tr("Ganze Mahlzeit", "Whole meal") to totalGrams
                ),
                // Eine Portion ist der Anteil laut "Ergibt (Portionen)"; bei 1 ist sie die ganze Mahlzeit
                portions = listOf(
                    PortionData(tr("Portion", "Serving"), ServingCalculator.mealServingGrams(totalGrams, pageMeal.servings))
                ),
                editLabel = tr("Mahlzeit bearbeiten", "Edit meal"),
                macrosFor = { grams -> MealCalculator.portion(pageMeal.ingredients, grams) },
                onAdd = { grams, note ->
                    viewModel.addMealEntry(
                        name = pageMeal.name,
                        grams = grams,
                        macros = MealCalculator.portion(pageMeal.ingredients, grams),
                        dateEpochDay = selectedDay,
                        servingNote = note
                    )
                    page = PAGE_MAIN
                },
                onEdit = {
                    mealError = null
                    editMealId = pageMeal.id
                },
                onBack = { page = PAGE_ADD }
            )
        }

        page == PAGE_HISTORY -> FoodHistoryPage(
            entries = logData,
            today = today,
            onBack = { page = PAGE_MAIN },
            onOpenToday = {
                selectedDay = today
                page = PAGE_MAIN
            },
            onOpenDay = { page = "day:$it" }
        )

        pageDay != null -> FoodDayOverviewPage(
            epochDay = pageDay,
            today = today,
            entries = logData,
            onBack = { page = PAGE_HISTORY },
            onOpenInDay = {
                selectedDay = pageDay
                page = PAGE_MAIN
            }
        )

        page != PAGE_MAIN -> AddEntryPage(
            foods = foods,
            meals = meals,
            recentFoodIds = recentFoodIds,
            dayLabel = dayLabel,
            tab = addTab,
            onTabChange = { addTab = it },
            query = addQuery,
            onQueryChange = { addQuery = it },
            onPickFood = { page = "food:$it" },
            onPickMeal = { page = "meal:$it" },
            onNewFood = {
                foodError = null
                creatingFood = true
            },
            onNewMeal = {
                mealError = null
                creatingMeal = true
            },
            onBack = { page = PAGE_MAIN }
        )

        else -> Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = tr("Essen", "Food"),
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                FoodDayTab(
                    selectedDay = selectedDay,
                    today = today,
                    entries = dayEntries,
                    totals = dayTotals,
                    calorieGoal = calorieGoal,
                    calorieTarget = calorieTarget,
                    hasProfile = calorieProfile != null,
                    onOpenSettings = onOpenSettings,
                    onPrevious = { selectedDay -= 1 },
                    onNext = { if (selectedDay < today) selectedDay += 1 },
                    onOpenHistory = { page = PAGE_HISTORY },
                    onEntryClick = { editingEntryId = it }
                )

                // Platz, damit der Plus-Button den letzten Eintrag nicht verdeckt
                Spacer(modifier = Modifier.height(96.dp))
            }

            FloatingActionButton(
                onClick = {
                    addQuery = ""
                    addTab = 0
                    page = PAGE_ADD
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Text(text = "+", fontSize = 30.sp, fontWeight = FontWeight.Light)
            }
        }
    }

    // ---------- Dialoge: Mahlzeiten anlegen, bearbeiten, loeschen ----------
    if (creatingMeal) {
        MealEditDialog(
            title = tr("Neue Mahlzeit", "New meal"),
            initial = null,
            foods = foods,
            externalError = mealError,
            onSubmit = { name, ingredients, servings ->
                mealViewModel.saveMeal(null, name, ingredients, servings) { error ->
                    if (error != null) {
                        mealError = error
                    } else {
                        creatingMeal = false
                        mealError = null
                    }
                }
            },
            onDismiss = {
                creatingMeal = false
                mealError = null
            }
        )
    }

    val editingMeal = meals.firstOrNull { it.id == editMealId }
    if (editingMeal != null) {
        MealEditDialog(
            title = tr("Mahlzeit bearbeiten", "Edit meal"),
            initial = editingMeal,
            foods = foods,
            externalError = mealError,
            onSubmit = { name, ingredients, servings ->
                mealViewModel.saveMeal(editingMeal.id, name, ingredients, servings) { error ->
                    if (error != null) {
                        mealError = error
                    } else {
                        editMealId = null
                        mealError = null
                    }
                }
            },
            onDismiss = {
                editMealId = null
                mealError = null
            },
            onDelete = { deleteMealId = editingMeal.id }
        )
    }

    val mealToDelete = meals.firstOrNull { it.id == deleteMealId }
    if (mealToDelete != null) {
        ConfirmDialog(
            title = tr("Mahlzeit l\u00F6schen?", "Delete meal?"),
            message = tr("\u201E${mealToDelete.name}\u201C wird aus deiner Liste gel\u00F6scht. ", "“${mealToDelete.name}” will be removed from your list. ") +
                    tr("Bereits eingetragene Mahlzeiten bleiben unver\u00E4ndert erhalten.", "Meals you already logged stay unchanged."),
            confirmLabel = tr("L\u00F6schen", "Delete"),
            onConfirm = {
                mealViewModel.deleteMeal(mealToDelete.id)
                deleteMealId = null
                editMealId = null
                if (page.startsWith("meal:")) page = PAGE_ADD
            },
            onDismiss = { deleteMealId = null }
        )
    }

    // ---------- Dialoge: Eintrag aendern oder loeschen ----------
    val editingEntry = logData.firstOrNull { it.id == editingEntryId }
    val entryToDelete = logData.firstOrNull { it.id == deleteEntryId }

    if (editingEntry != null && entryToDelete == null) {
        FoodEntryDialog(
            entry = editingEntry,
            onSaveGrams = { grams ->
                viewModel.updateEntryGrams(editingEntry.id, grams)
                editingEntryId = null
            },
            onDelete = { deleteEntryId = editingEntry.id },
            onDismiss = { editingEntryId = null }
        )
    }

    if (entryToDelete != null) {
        ConfirmDialog(
            title = tr("Eintrag l\u00F6schen?", "Delete entry?"),
            message = "\u201E${entryToDelete.name}\u201C (${FoodFormat.grams(entryToDelete.grams)}) " +
                    tr("wird aus diesem Tag entfernt.", "will be removed from this day."),
            confirmLabel = tr("L\u00F6schen", "Delete"),
            onConfirm = {
                viewModel.deleteEntry(entryToDelete.id)
                deleteEntryId = null
                editingEntryId = null
            },
            onDismiss = { deleteEntryId = null }
        )
    }

    // ---------- Dialoge: Lebensmittel anlegen, bearbeiten, loeschen ----------
    if (creatingFood) {
        FoodEditDialog(
            title = tr("Neues Lebensmittel", "New food"),
            initial = null,
            externalError = foodError,
            onSubmit = { name, kcal, protein, carbs, fat, portions ->
                viewModel.saveFood(null, name, kcal, protein, carbs, fat, portions) { error, saved ->
                    if (error != null) {
                        foodError = error
                    } else {
                        creatingFood = false
                        foodError = null
                        // Auf der Hinzufügen-Seite geht es direkt mit der Menge weiter
                        if (saved != null && page == PAGE_ADD) page = "food:${saved.id}"
                    }
                }
            },
            onDismiss = {
                creatingFood = false
                foodError = null
            }
        )
    }

    val editingFood = foods.firstOrNull { it.id == editFoodId }
    if (editingFood != null) {
        FoodEditDialog(
            title = tr("Lebensmittel bearbeiten", "Edit food"),
            initial = editingFood,
            initialPortions = portionRows
                .filter { it.foodItemId == editingFood.id }
                .map { PortionData(it.name, it.grams) },
            externalError = foodError,
            onSubmit = { name, kcal, protein, carbs, fat, portions ->
                viewModel.saveFood(editingFood.id, name, kcal, protein, carbs, fat, portions) { error, _ ->
                    if (error != null) {
                        foodError = error
                    } else {
                        editFoodId = null
                        foodError = null
                    }
                }
            },
            onDismiss = {
                editFoodId = null
                foodError = null
            },
            onDelete = { deleteFoodId = editingFood.id }
        )
    }

    val foodToDelete = foods.firstOrNull { it.id == deleteFoodId }
    if (foodToDelete != null) {
        ConfirmDialog(
            title = tr("Lebensmittel l\u00F6schen?", "Delete food?"),
            message = tr("\u201E${foodToDelete.name}\u201C wird aus deiner Liste gel\u00F6scht. ", "“${foodToDelete.name}” will be removed from your list. ") +
                    tr("Bereits eingetragene Mahlzeiten bleiben unver\u00E4ndert erhalten.", "Meals you already logged stay unchanged."),
            confirmLabel = tr("L\u00F6schen", "Delete"),
            onConfirm = {
                viewModel.deleteFood(foodToDelete.id)
                deleteFoodId = null
                editFoodId = null
                if (page.startsWith("food:")) page = PAGE_ADD
            },
            onDismiss = { deleteFoodId = null }
        )
    }
}
