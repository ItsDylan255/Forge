package com.example.fitnesstracker.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Gespeicherte Mahlzeit aus mehreren Zutaten. Der Name ist eindeutig. */
@Entity(
    tableName = "meal",
    indices = [Index(value = ["name"], unique = true)]
)
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    /** Ergibt so viele Portionen (Standard 1). */
    val servings: Double = 1.0,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Zutat einer Mahlzeit (Reihenfolge = position). Name und Werte pro 100 g sind ein Snapshot
 * des Lebensmittels beim Hinzufügen, es gibt keinen Verweis auf food_item.
 */
@Entity(
    tableName = "meal_ingredient",
    indices = [Index(value = ["mealId"])]
)
data class MealIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val mealId: Long,
    val position: Int,
    val name: String,
    val grams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double
)

@Dao
abstract class MealDao {

    @Query("SELECT * FROM meal ORDER BY name COLLATE NOCASE ASC")
    abstract fun observeMeals(): Flow<List<Meal>>

    @Query("SELECT * FROM meal_ingredient ORDER BY mealId ASC, position ASC")
    abstract fun observeIngredients(): Flow<List<MealIngredient>>

    @Query("SELECT * FROM meal WHERE id = :id")
    abstract suspend fun getMeal(id: Long): Meal?

    @Query("SELECT * FROM meal WHERE name = :name COLLATE NOCASE LIMIT 1")
    abstract suspend fun findMealByName(name: String): Meal?

    @Insert
    abstract suspend fun insertMeal(meal: Meal): Long

    @Update
    abstract suspend fun updateMeal(meal: Meal)

    @Insert
    abstract suspend fun insertIngredients(items: List<MealIngredient>)

    @Query("DELETE FROM meal_ingredient WHERE mealId = :mealId")
    abstract suspend fun deleteIngredients(mealId: Long)

    @Query("DELETE FROM meal WHERE id = :id")
    abstract suspend fun deleteMealRow(id: Long)

    /** Legt die Mahlzeit an (id == 0) oder ändert sie und ersetzt dabei alle Zutaten. */
    @Transaction
    open suspend fun saveMeal(meal: Meal, items: List<MealIngredient>): Long {
        val id = if (meal.id == 0L) {
            insertMeal(meal)
        } else {
            updateMeal(meal)
            meal.id
        }
        deleteIngredients(id)
        insertIngredients(items.map { it.copy(mealId = id) })
        return id
    }

    /** Löscht die Mahlzeit samt Zutaten. Eingetragene Mahlzeiten im Tag bleiben (Snapshot). */
    @Transaction
    open suspend fun deleteMeal(id: Long) {
        deleteIngredients(id)
        deleteMealRow(id)
    }
}
