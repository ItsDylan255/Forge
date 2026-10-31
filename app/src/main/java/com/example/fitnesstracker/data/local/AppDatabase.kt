package com.example.fitnesstracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
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
        FoodPortion::class,
        StepSample::class,
        GymPlan::class,
        ProgressPhoto::class
    ],
    version = 17,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun weightEntryDao(): WeightEntryDao

    abstract fun weightGoalDao(): WeightGoalDao

    abstract fun foodItemDao(): FoodItemDao

    abstract fun foodLogEntryDao(): FoodLogEntryDao

    abstract fun stepEntryDao(): StepEntryDao

    abstract fun gymDao(): GymDao

    abstract fun foodDao(): FoodDao

    abstract fun userProfileDao(): UserProfileDao

    abstract fun mealDao(): MealDao

    abstract fun stepSampleDao(): StepSampleDao

    abstract fun progressPhotoDao(): ProgressPhotoDao
}