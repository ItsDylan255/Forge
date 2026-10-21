package com.example.fitnesstracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkoutRoutine::class,
        WeightEntry::class,
        WeightGoal::class,
        FoodItem::class,
        FoodLogEntry::class,
        StepEntry::class,
        GymExercise::class,
        GymRoutine::class,
        GymRoutineExercise::class,
        GymSession::class,
        GymSet::class,
        UserProfile::class,
        Meal::class,
        MealIngredient::class,
        FoodPortion::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutRoutineDao(): WorkoutRoutineDao

    abstract fun weightEntryDao(): WeightEntryDao

    abstract fun weightGoalDao(): WeightGoalDao

    abstract fun foodItemDao(): FoodItemDao

    abstract fun foodLogEntryDao(): FoodLogEntryDao

    abstract fun stepEntryDao(): StepEntryDao

    abstract fun gymDao(): GymDao

    abstract fun foodDao(): FoodDao

    abstract fun userProfileDao(): UserProfileDao

    abstract fun mealDao(): MealDao
}