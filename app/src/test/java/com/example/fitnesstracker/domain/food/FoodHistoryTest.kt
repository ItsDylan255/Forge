package com.example.fitnesstracker.domain.food

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodHistoryTest {

    private fun entry(id: Long, day: Long, kcal: Double, protein: Double = 0.0) =
        FoodLogData(id, day, "Essen $id", 100.0, Macros(kcal, protein, 0.0, 0.0))

    @Test fun daysAreSummedAndNewestFirst() {
        val entries = listOf(entry(1, 10, 200.0, 10.0), entry(2, 12, 500.0), entry(3, 10, 300.0, 5.0))
        val days = FoodHistory.days(entries)

        assertEquals(listOf(12L, 10L), days.map { it.epochDay })
        assertEquals(500.0, days[0].totals.kcal, 0.001)
        assertEquals(500.0, days[1].totals.kcal, 0.001)
        assertEquals(15.0, days[1].totals.protein, 0.001)
        assertEquals(2, days[1].entryCount)
    }

    @Test fun daysWithoutEntriesDoNotAppear() {
        assertTrue(FoodHistory.days(emptyList()).isEmpty())
        assertEquals(1, FoodHistory.days(listOf(entry(1, 5, 100.0))).size)
    }

    @Test fun monthLabelIsGerman() {
        assertEquals("Oktober 2026", FoodHistory.monthLabel(LocalDate.of(2026, 10, 12).toEpochDay()))
        assertEquals("März 2027", FoodHistory.monthLabel(LocalDate.of(2027, 3, 1).toEpochDay()))
    }
}
