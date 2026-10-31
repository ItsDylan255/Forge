package com.example.fitnesstracker.ui.food

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.FoodItem
import com.example.fitnesstracker.data.local.FoodLogEntry
import com.example.fitnesstracker.data.local.FoodPortion
import com.example.fitnesstracker.domain.food.FoodCalculator
import com.example.fitnesstracker.domain.food.Macros
import com.example.fitnesstracker.domain.food.PortionData
import com.example.fitnesstracker.i18n.tr
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoodViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = DatabaseProvider.getDatabase(application).foodDao()

    val foods = dao.observeFoods().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val log = dao.observeLog().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    /** Alle Portionen aller Lebensmittel, nach Lebensmittel und Reihenfolge sortiert. */
    val portions = dao.observePortions().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    // ---------- Lebensmittel ----------

    /**
     * Legt ein Lebensmittel an ([id] == null) oder aendert es.
     * Die Werte sind schon geprueft. [onDone] bekommt eine Fehlermeldung (oder null)
     * und das gespeicherte Lebensmittel.
     */
    fun saveFood(
        id: Long?,
        name: String,
        kcalPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        /** Portionen des Lebensmittels. Null = unverändert lassen. */
        portions: List<PortionData>? = null,
        onDone: (error: String?, saved: FoodItem?) -> Unit
    ) {
        viewModelScope.launch {
            val cleanName = name.trim()

            // Der Name muss eindeutig sein (ohne Beachtung der Gross-/Kleinschreibung)
            val duplicate = dao.findFoodByName(cleanName)
            if (duplicate != null && duplicate.id != id) {
                onDone(tr("Ein Lebensmittel mit diesem Namen gibt es schon.", "A food with this name already exists."), null)
                return@launch
            }

            val now = System.currentTimeMillis()
            val old = id?.let { dao.getFood(it) }

            val food = FoodItem(
                id = old?.id ?: 0L,
                name = cleanName,
                caloriesPer100g = kcalPer100g,
                proteinPer100g = proteinPer100g,
                carbsPer100g = carbsPer100g,
                fatPer100g = fatPer100g,
                createdAt = old?.createdAt ?: now,
                updatedAt = now
            )

            val savedId = if (old == null) {
                dao.insertFood(food)
            } else {
                dao.updateFood(food)
                old.id
            }

            if (portions != null) {
                dao.savePortions(
                    savedId,
                    portions.mapIndexed { index, portion ->
                        FoodPortion(
                            foodItemId = savedId,
                            name = portion.name.trim(),
                            grams = portion.grams,
                            position = index
                        )
                    }
                )
            }

            onDone(null, dao.getFood(savedId))
        }
    }

    fun deleteFood(id: Long) {
        viewModelScope.launch { dao.deleteFood(id) }
    }

    // ---------- Eintraege ----------

    /** Rechnet auf die Menge hoch und speichert die Werte als Snapshot im Eintrag. */
    fun addEntry(food: FoodItem, grams: Double, dateEpochDay: Long, servingNote: String? = null) {
        viewModelScope.launch {
            val per100g = Macros(
                kcal = food.caloriesPer100g,
                protein = food.proteinPer100g,
                carbs = food.carbsPer100g,
                fat = food.fatPer100g
            )
            val macros = FoodCalculator.scale(per100g, grams)
            val now = System.currentTimeMillis()

            dao.insertLogEntry(
                FoodLogEntry(
                    id = 0L,
                    foodItemId = food.id,
                    foodName = food.name,
                    grams = grams,
                    calories = macros.kcal,
                    protein = macros.protein,
                    carbs = macros.carbs,
                    fat = macros.fat,
                    dateEpochDay = dateEpochDay,
                    servingNote = servingNote,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    /**
     * Trägt eine Mahlzeit als einen Eintrag ein. [macros] sind die Werte der gegessenen Menge
     * (Snapshot). Der Eintrag hat keinen Verweis auf ein Lebensmittel.
     */
    fun addMealEntry(
        name: String,
        grams: Double,
        macros: Macros,
        dateEpochDay: Long,
        servingNote: String? = null
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()

            dao.insertLogEntry(
                FoodLogEntry(
                    id = 0L,
                    foodItemId = null,
                    foodName = name,
                    grams = grams,
                    calories = macros.kcal,
                    protein = macros.protein,
                    carbs = macros.carbs,
                    fat = macros.fat,
                    dateEpochDay = dateEpochDay,
                    servingNote = servingNote,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    /** Aendert die Menge eines Eintrags. Kalorien und Makros werden proportional umgerechnet. */
    fun updateEntryGrams(entryId: Long, newGrams: Double) {
        viewModelScope.launch {
            val old = dao.getLogEntry(entryId) ?: return@launch

            val macros = FoodCalculator.rescale(
                Macros(old.calories, old.protein, old.carbs, old.fat),
                old.grams,
                newGrams
            )

            dao.updateLogEntry(
                FoodLogEntry(
                    id = old.id,
                    foodItemId = old.foodItemId,
                    foodName = old.foodName,
                    grams = newGrams,
                    calories = macros.kcal,
                    protein = macros.protein,
                    carbs = macros.carbs,
                    fat = macros.fat,
                    dateEpochDay = old.dateEpochDay,
                    createdAt = old.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch { dao.deleteLogEntry(entryId) }
    }
}
