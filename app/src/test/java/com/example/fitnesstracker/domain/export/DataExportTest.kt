package com.example.fitnesstracker.domain.export

import com.example.fitnesstracker.domain.gym.ExerciseKind
import com.example.fitnesstracker.domain.gym.SetSide
import com.example.fitnesstracker.domain.gym.WorkoutSetData
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataExportTest {

    // 2026-10-26 ist ein Montag
    private val monday = LocalDate.of(2026, 10, 26).toEpochDay()
    private val start = LocalDate.of(2026, 10, 26).atTime(18, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private val data = ExportData(
        weights = listOf(monday to 80.456, monday + 1 to 80.0),
        steps = listOf(monday to 9000),
        food = listOf(
            FoodExportRow(monday, "Haferflocken, kernig", 80.0, "1 × Schale", 300.4, 10.0, 50.0, 5.55),
            FoodExportRow(monday, "Milch", 200.0, null, 100.0, 7.0, 10.0, 3.0)
        ),
        sessions = listOf(SessionExportRow(1, "Leg Day 1", monday, start, start + 45 * 60_000, "Fitnessbasic")),
        sets = listOf(
            WorkoutSetData(1, 10, "Kniebeuge", 100.0, 5, order = 1),
            WorkoutSetData(1, 10, "Kniebeuge", 100.0, 5, order = 2),
            WorkoutSetData(1, 11, "Plank", 0.0, 60, order = 3, kind = ExerciseKind.TIME),
            WorkoutSetData(1, 12, "Ausfallschritt", 20.0, 10, order = 4, side = SetSide.LEFT)
        )
    )

    private fun lines(csv: String) = csv.trimEnd().split("\r\n")

    @Test fun numbersUseDotAndNoTrailingZeros() {
        assertEquals("80.46", DataExport.number(80.456, 2))
        assertEquals("80", DataExport.number(80.0, 2))
        assertEquals("100", DataExport.number(100.0, 0))
    }

    @Test fun fieldsWithCommaOrQuoteAreQuoted() {
        assertEquals("a,\"b,c\",\"d \"\"e\"\"\",\r\n", DataExport.row(listOf("a", "b,c", "d \"e\"", null)))
    }

    @Test fun daysCombineAllAreas() {
        val rows = lines(DataExport.daysCsv(data))
        assertEquals(
            "datum,wochentag,gewicht_kg,schritte,kalorien_kcal,protein_g,kohlenhydrate_g,fett_g,trainings,routinen,saetze,volumen_kg",
            rows[0]
        )
        assertEquals("2026-10-26,Mo,80.46,9000,400,17,60,8.6,1,Leg Day 1,4,1200", rows[1])
        assertEquals("2026-10-27,Di,80,,,,,,0,,,", rows[2])
    }

    @Test fun sessionsHaveDurationAndVolume() {
        val rows = lines(DataExport.sessionsCsv(data.sessions, data.sets, ZoneOffset.UTC))
        assertEquals("1,2026-10-26,Leg Day 1,Fitnessbasic,18:00,18:45,45,3,4,1200", rows[1])
    }

    @Test fun setsSplitRepsAndSeconds() {
        val rows = lines(DataExport.setsCsv(data.sessions, data.sets))
        assertEquals("1,2026-10-26,Leg Day 1,Kniebeuge,Wiederholungen,,100,5,,", rows[1])
        assertEquals("1,2026-10-26,Leg Day 1,Plank,Zeit,,0,,60,", rows[3])
        assertEquals("1,2026-10-26,Leg Day 1,Ausfallschritt,Wiederholungen,links,20,10,,", rows[4])
    }

    @Test fun foodKeepsNamesWithCommas() {
        val rows = lines(DataExport.foodCsv(data.food))
        assertEquals("2026-10-26,\"Haferflocken, kernig\",80,1 × Schale,300,10,50,5.6", rows[1])
    }

    @Test fun archiveContainsAllFiles() {
        val files = DataExport.files(data, ZoneOffset.UTC, LocalDate.of(2026, 10, 30))
        assertEquals(
            listOf("LIESMICH.txt", "tage.csv", "gewicht.csv", "schritte.csv", "ernaehrung.csv",
                "training_einheiten.csv", "training_saetze.csv"),
            files.keys.toList()
        )
        assertTrue(files.getValue("LIESMICH.txt").contains("30.10.2026"))
        assertEquals("forge-export-2026-10-30.zip", DataExport.fileName(LocalDate.of(2026, 10, 30)))
    }
}
