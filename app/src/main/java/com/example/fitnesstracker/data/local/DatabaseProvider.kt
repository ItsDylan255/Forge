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

// Neu: legt die Gym-Tabellen an. Bestehende Tabellen (auch WorkoutRoutine) bleiben unberührt.
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `gym_exercise` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `muscleGroup` TEXT,
                `isCustom` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_gym_exercise_name` ON `gym_exercise` (`name`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `gym_routine` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `gym_routine_exercise` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `routineId` INTEGER NOT NULL,
                `exerciseId` INTEGER NOT NULL,
                `position` INTEGER NOT NULL,
                `plannedSets` INTEGER NOT NULL,
                `repsMin` INTEGER NOT NULL,
                `repsMax` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_gym_routine_exercise_routineId` ON `gym_routine_exercise` (`routineId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_gym_routine_exercise_exerciseId` ON `gym_routine_exercise` (`exerciseId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `gym_session` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `routineId` INTEGER,
                `routineName` TEXT NOT NULL,
                `dateEpochDay` INTEGER NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `endedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `gym_set` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sessionId` INTEGER NOT NULL,
                `exerciseId` INTEGER NOT NULL,
                `exerciseName` TEXT NOT NULL,
                `setNumber` INTEGER NOT NULL,
                `weightKg` REAL NOT NULL,
                `reps` INTEGER NOT NULL,
                `rpe` REAL,
                `note` TEXT,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_gym_set_sessionId` ON `gym_set` (`sessionId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_gym_set_exerciseId` ON `gym_set` (`exerciseId`)"
        )
    }
}

// Neu: Zieldatum für das Zielgewicht. Bestehende Ziele behalten alle Werte (Datum = leer).
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `weight_goal` ADD COLUMN `targetDateEpochDay` INTEGER")
    }
}

// Neu: Pause pro Übung, keine vorgefertigten Übungen, kein RPE/Notiz, kein "Eigene"-Kennzeichen.
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1) Vorgefertigte Übungen (isCustom = 0) entfernen, auch aus Routinen.
        //    Bereits geloggte Sätze behalten ihren Übungsnamen und bleiben im Verlauf.
        db.execSQL(
            "DELETE FROM `gym_routine_exercise` WHERE `exerciseId` IN " +
                    "(SELECT `id` FROM `gym_exercise` WHERE `isCustom` = 0)"
        )
        db.execSQL("DELETE FROM `gym_exercise` WHERE `isCustom` = 0")

        // 2) gym_exercise ohne die Spalte isCustom neu aufbauen
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `gym_exercise_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `muscleGroup` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "INSERT INTO `gym_exercise_new` (`id`, `name`, `muscleGroup`, `createdAt`, `updatedAt`) " +
                    "SELECT `id`, `name`, `muscleGroup`, `createdAt`, `updatedAt` FROM `gym_exercise`"
        )
        db.execSQL("DROP TABLE `gym_exercise`")
        db.execSQL("ALTER TABLE `gym_exercise_new` RENAME TO `gym_exercise`")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_gym_exercise_name` ON `gym_exercise` (`name`)"
        )

        // 3) Pause pro Übung in der Routine, bestehende Einträge bekommen den Standard (90 Sekunden)
        db.execSQL(
            "ALTER TABLE `gym_routine_exercise` ADD COLUMN `restSeconds` INTEGER NOT NULL DEFAULT 90"
        )

        // 4) gym_set ohne die Spalten rpe und note neu aufbauen
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `gym_set_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sessionId` INTEGER NOT NULL,
                `exerciseId` INTEGER NOT NULL,
                `exerciseName` TEXT NOT NULL,
                `setNumber` INTEGER NOT NULL,
                `weightKg` REAL NOT NULL,
                `reps` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "INSERT INTO `gym_set_new` " +
                    "(`id`, `sessionId`, `exerciseId`, `exerciseName`, `setNumber`, `weightKg`, `reps`, `createdAt`) " +
                    "SELECT `id`, `sessionId`, `exerciseId`, `exerciseName`, `setNumber`, `weightKg`, `reps`, `createdAt` " +
                    "FROM `gym_set`"
        )
        db.execSQL("DROP TABLE `gym_set`")
        db.execSQL("ALTER TABLE `gym_set_new` RENAME TO `gym_set`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_set_sessionId` ON `gym_set` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_set_exerciseId` ON `gym_set` (`exerciseId`)")
    }
}

// Neu: legt nur die Profil-Tabelle an. Bestehende Tabellen bleiben unberührt.
private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_profile` (
                `id` INTEGER NOT NULL,
                `heightCm` INTEGER NOT NULL,
                `ageYears` INTEGER NOT NULL,
                `sex` TEXT NOT NULL,
                `activityLevel` TEXT NOT NULL,
                `proteinPercent` INTEGER NOT NULL,
                `carbsPercent` INTEGER NOT NULL,
                `fatPercent` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}

// Neu: legt nur die Mahlzeiten-Tabellen an. Bestehende Tabellen bleiben unberührt.
private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `meal` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_meal_name` ON `meal` (`name`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `meal_ingredient` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `mealId` INTEGER NOT NULL,
                `position` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `grams` REAL NOT NULL,
                `caloriesPer100g` REAL NOT NULL,
                `proteinPer100g` REAL NOT NULL,
                `carbsPer100g` REAL NOT NULL,
                `fatPer100g` REAL NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_meal_ingredient_mealId` ON `meal_ingredient` (`mealId`)"
        )
    }
}

// Neu: einseitige Übungen und Supersets. Bestehende Daten bekommen die Standardwerte (aus).
private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `gym_routine_exercise` ADD COLUMN `unilateral` INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE `gym_routine_exercise` ADD COLUMN `supersetWithNext` INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL("ALTER TABLE `gym_set` ADD COLUMN `side` INTEGER NOT NULL DEFAULT 0")
    }
}

// Neu: Übungsarten (Wiederholungen, Zeit, Assistiert). Bestehende Daten bleiben "Wiederholungen".
private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `gym_exercise` ADD COLUMN `kind` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `gym_set` ADD COLUMN `kind` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `gym_set` ADD COLUMN `bodyWeightKg` REAL")
    }
}

// Neu: Portionen pro Lebensmittel, Portionen pro Mahlzeit und Anzeige-Hinweis im Tagebuch.
// Bestehende Daten bleiben unverändert (keine Portionen, 1 Portion, nur Gramm).
private val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `food_portion` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `foodItemId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `grams` REAL NOT NULL,
                `position` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_food_portion_foodItemId` ON `food_portion` (`foodItemId`)"
        )
        db.execSQL("ALTER TABLE `meal` ADD COLUMN `servings` REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE `food_log_entry` ADD COLUMN `servingNote` TEXT")
    }
}

// Neu: Messwerte des Schrittzählers (Grundlage der neuen Schrittberechnung).
// Unmögliche Tageswerte (mehr als 60.000 Schritte) aus der alten Zählung werden entfernt.
private val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `step_sample` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `timeMillis` INTEGER NOT NULL,
                `total` INTEGER NOT NULL,
                `bootMillis` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_step_sample_timeMillis` ON `step_sample` (`timeMillis`)")
        db.execSQL("DELETE FROM `step_entry` WHERE `steps` > 60000")
    }
}

// Neu: Trainingspläne und Archiv für Routinen und Pläne.
// Bestehende Routinen bleiben aktiv und sind ohne Plan.
private val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `gym_plan` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `archivedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("ALTER TABLE `gym_routine` ADD COLUMN `planId` INTEGER")
        db.execSQL("ALTER TABLE `gym_routine` ADD COLUMN `planPosition` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `gym_routine` ADD COLUMN `archivedAt` INTEGER")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_routine_planId` ON `gym_routine` (`planId`)")
    }
}

// Neu: Einheiten merken sich ihren Trainingsplan (für den Plan-Fortschritt).
// Bestehende Einheiten bekommen den Plan, zu dem ihre Routine gerade gehört.
// Die alte, nie benutzte Tabelle workout_routines fällt weg.
private val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `gym_session` ADD COLUMN `planId` INTEGER")
        db.execSQL("ALTER TABLE `gym_session` ADD COLUMN `planName` TEXT")
        db.execSQL(
            """
            UPDATE `gym_session` SET
                `planId` = (SELECT r.`planId` FROM `gym_routine` r WHERE r.`id` = `gym_session`.`routineId`),
                `planName` = (SELECT p.`name` FROM `gym_routine` r JOIN `gym_plan` p ON p.`id` = r.`planId`
                              WHERE r.`id` = `gym_session`.`routineId`)
            WHERE `routineId` IS NOT NULL
            """.trimIndent()
        )
        db.execSQL("DROP TABLE IF EXISTS `workout_routines`")
    }
}

// Neu: Fortschrittsfotos mit den Werten vom Tag der Aufnahme
private val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `progress_photo` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `fileName` TEXT NOT NULL,
                `dateEpochDay` INTEGER NOT NULL,
                `weightKg` REAL,
                `goalWeightKg` REAL,
                `workoutsTotal` INTEGER NOT NULL,
                `workouts30Days` INTEGER NOT NULL,
                `avgSteps7Days` INTEGER,
                `avgKcal7Days` INTEGER,
                `kcalTarget` INTEGER,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_progress_photo_dateEpochDay` ON `progress_photo` (`dateEpochDay`)")
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
                .build()
                .also { database ->
                    INSTANCE = database
                }
        }
    }
}