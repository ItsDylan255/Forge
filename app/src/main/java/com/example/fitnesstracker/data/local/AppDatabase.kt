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
        StepEntry::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutRoutineDao(): WorkoutRoutineDao

    abstract fun weightEntryDao(): WeightEntryDao

    abstract fun weightGoalDao(): WeightGoalDao

    abstract fun foodItemDao(): FoodItemDao

    abstract fun foodLogEntryDao(): FoodLogEntryDao

    abstract fun stepEntryDao(): StepEntryDao
}