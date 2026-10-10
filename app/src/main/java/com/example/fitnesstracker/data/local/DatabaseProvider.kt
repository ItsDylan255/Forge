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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { database ->
                    INSTANCE = database
                }
        }
    }
}