package com.example.fitnesstracker.ui.food

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.Meal
import com.example.fitnesstracker.data.local.MealIngredient
import com.example.fitnesstracker.domain.food.MealIngredientData
import com.example.fitnesstracker.domain.food.toData
import com.example.fitnesstracker.i18n.tr
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Mahlzeit mit ihren Zutaten für die Anzeige. */
data class MealUi(
    val id: Long,
    val name: String,
    val ingredients: List<MealIngredientData>,
    /** Ergibt so viele Portionen (Standard 1). */
    val servings: Double = 1.0
)

class MealViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = DatabaseProvider.getDatabase(application).mealDao()

    val meals: StateFlow<List<MealUi>> = combine(
        dao.observeMeals(),
        dao.observeIngredients()
    ) { meals, ingredients ->
        val byMeal = ingredients.groupBy { it.mealId }
        meals.map { meal ->
            MealUi(meal.id, meal.name, byMeal[meal.id].orEmpty().map { it.toData() }, meal.servings)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Legt eine Mahlzeit an ([id] == null) oder ändert sie. Name und Zutaten sind schon geprüft.
     * [onDone] bekommt eine Fehlermeldung (oder null).
     */
    fun saveMeal(
        id: Long?,
        name: String,
        ingredients: List<MealIngredientData>,
        /** Ergibt so viele Portionen. Null = beim Anlegen 1, beim Ändern unverändert. */
        servings: Double? = null,
        onDone: (error: String?) -> Unit
    ) {
        viewModelScope.launch {
            val cleanName = name.trim()

            // Der Name muss eindeutig sein (ohne Beachtung der Groß-/Kleinschreibung)
            val duplicate = dao.findMealByName(cleanName)
            if (duplicate != null && duplicate.id != id) {
                onDone(tr("Eine Mahlzeit mit diesem Namen gibt es schon.", "A meal with this name already exists."))
                return@launch
            }

            val now = System.currentTimeMillis()
            val old = id?.let { dao.getMeal(it) }

            val meal = Meal(
                id = old?.id ?: 0L,
                name = cleanName,
                servings = servings ?: old?.servings ?: 1.0,
                createdAt = old?.createdAt ?: now,
                updatedAt = now
            )

            val rows = ingredients.mapIndexed { index, item ->
                MealIngredient(
                    mealId = 0L,
                    position = index,
                    name = item.name,
                    grams = item.grams,
                    caloriesPer100g = item.per100g.kcal,
                    proteinPer100g = item.per100g.protein,
                    carbsPer100g = item.per100g.carbs,
                    fatPer100g = item.per100g.fat
                )
            }

            dao.saveMeal(meal, rows)
            onDone(null)
        }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch { dao.deleteMeal(id) }
    }
}
