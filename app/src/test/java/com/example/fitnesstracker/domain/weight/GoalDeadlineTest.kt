package com.example.fitnesstracker.domain.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoalDeadlineTest {

    @Test fun noDateGivesNull() {
        assertNull(GoalDeadline.daysLeft(null, 100))
        assertNull(GoalDeadline.text(null))
    }

    @Test fun daysLeftCountsDifference() {
        assertEquals(12L, GoalDeadline.daysLeft(112, 100))
        assertEquals(0L, GoalDeadline.daysLeft(100, 100))
        assertEquals(-3L, GoalDeadline.daysLeft(97, 100))
    }

    @Test fun textVariants() {
        assertEquals("12 Tage", GoalDeadline.text(12))
        assertEquals("2 Tage", GoalDeadline.text(2))
        assertEquals("1 Tag", GoalDeadline.text(1))
        assertEquals("Heute", GoalDeadline.text(0))
        assertEquals("Abgelaufen", GoalDeadline.text(-1))
    }
}
