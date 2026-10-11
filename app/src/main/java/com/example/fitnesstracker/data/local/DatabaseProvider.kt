package com.example.fitnesstracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `weight_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `weightKg` REAL NOT NULL,
                `dateEpochDay` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
            `index_weight_entries_dateEpochDay`
            ON `weight_entries` (`dateEpochDay`)
            """.trimIndent()
        )
    }
}

// Neu: legt nur die Zielgewicht-Tabelle an. Bestehende Tabellen bleiben unberührt.
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `weight_goal` (
                `id` INTEGER NOT NULL,
                `targetWeightKg` REAL NOT NULL,
                `startWeightKg` REAL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}
// Neu: legt die Food-Tabellen an. Bestehende Tabellen bleiben unberührt.
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `food_item` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `caloriesPer100g` REAL NOT NULL,
                `proteinPer100g` REAL NOT NULL,
                `carbsPer100g` REAL NOT NULL,
                `fatPer100g` REAL NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
            `index_food_item_name`
            ON `food_item` (`name`)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `food_log_entry` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `foodItemId` INTEGER,
                `foodName` TEXT NOT NULL,
                `grams` REAL NOT NULL,
                `calories` REAL NOT NULL,
                `protein` REAL NOT NULL,
                `carbs` REAL NOT NULL,
                `fat` REAL NOT NULL,
                `dateEpochDay` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_food_log_entry_dateEpochDay`
            ON `food_log_entry` (`dateEpochDay`)
            """.trimIndent()
        )
    }
}

// Neu: legt nur die Schritte-Tabelle an. Bestehende Tabellen bleiben unberührt.
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `step_entry` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `dateEpochDay` INTEGER NOT NULL,
                `steps` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
            `index_step_entry_dateEpochDay`
            ON `step_entry` (`dateEpochDay`)
            """.trimIndent()
        )
    }
}

object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "fitness_tracker_database"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                .also { database ->
                    INSTANCE = database
                }
        }
    }
}