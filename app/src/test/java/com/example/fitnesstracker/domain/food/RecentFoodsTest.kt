package com.example.fitnesstracker.domain.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentFoodsTest {

    private fun ref(food: Long?, day: Long, entry: Long) = LoggedFoodRef(food, day, entry)

    @Test fun newestFirstAndDistinct() {
        val entries = listOf(ref(1, 10, 1), ref(2, 11, 2), ref(1, 12, 3), ref(3, 12, 4))
        assertEquals(listOf(3L, 1L, 2L), RecentFoods.ids(entries, 10))
    }

    @Test fun sameDayUsesEntryOrder() {
        val entries = listOf(ref(1, 10, 5), ref(2, 10, 6))
        assertEquals(listOf(2L, 1L), RecentFoods.ids(entries, 10))
    }

    @Test fun mealsWithoutFoodAreIgnored() {
        val entries = listOf(ref(null, 12, 3), ref(1, 11, 2))
        assertEquals(listOf(1L), RecentFoods.ids(entries, 10))
    }

    @Test fun limitIsApplied() {
        val entries = (1L..8L).map { ref(it, it, it) }
        assertEquals(listOf(8L, 7L, 6L), RecentFoods.ids(entries, 3))
    }

    @Test fun emptyGivesEmpty() = assertTrue(RecentFoods.ids(emptyList(), 5).isEmpty())
}
