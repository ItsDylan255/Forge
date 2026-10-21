package com.example.fitnesstracker.domain.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextWorkoutTest {

    private val push = RoutineRef(1, "Push")
    private val pull = RoutineRef(2, "Pull")
    private val legs = RoutineRef(3, "Legs")

    @Test fun noRoutinesGivesNull() = assertNull(NextWorkoutPicker.pick(emptyList(), emptyList()))

    @Test fun neverTrainedComesFirst() {
        val sessions = listOf(SessionRef(1, 10, 1000), SessionRef(2, 11, 2000))
        val r = NextWorkoutPicker.pick(listOf(push, pull, legs), sessions)!!
        assertEquals(legs, r.routine)
        assertNull(r.lastEpochDay)
    }

    @Test fun longestAgoWins() {
        val sessions = listOf(
            SessionRef(1, 12, 3000), SessionRef(2, 10, 1000), SessionRef(3, 11, 2000)
        )
        val r = NextWorkoutPicker.pick(listOf(push, pull, legs), sessions)!!
        assertEquals(pull, r.routine)
        assertEquals(10L, r.lastEpochDay)
    }

    @Test fun usesNewestSessionPerRoutine() {
        val sessions = listOf(
            SessionRef(1, 1, 100), SessionRef(1, 20, 5000),
            SessionRef(2, 10, 1000), SessionRef(3, 11, 2000)
        )
        assertEquals(pull, NextWorkoutPicker.pick(listOf(push, pull, legs), sessions)!!.routine)
    }

    @Test fun freeWorkoutsAreIgnored() {
        val sessions = listOf(SessionRef(null, 50, 9000))
        val r = NextWorkoutPicker.pick(listOf(push), sessions)!!
        assertEquals(push, r.routine)
        assertNull(r.lastEpochDay)
    }

    @Test fun tieBrokenByName() {
        val r = NextWorkoutPicker.pick(listOf(push, legs, pull), emptyList())!!
        assertEquals(legs, r.routine)
    }
}
