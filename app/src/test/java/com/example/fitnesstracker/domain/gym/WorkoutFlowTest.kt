package com.example.fitnesstracker.domain.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutFlowTest {

    private fun item(
        id: Long, sets: Int, rest: Int = 60,
        uni: Boolean = false, link: Boolean = false
    ) = PlanItem(id, sets, rest, uni, link)

    private fun WorkoutSlot.key() = "$exerciseId:$setNumber:${side.name}:$restAfterSeconds"

    @Test fun sideCodes() {
        assertEquals(SetSide.RIGHT, SetSide.fromCode(1))
        assertEquals(SetSide.LEFT, SetSide.fromCode(2))
        assertEquals(SetSide.BOTH, SetSide.fromCode(0))
        assertEquals(SetSide.BOTH, SetSide.fromCode(99))
    }

    @Test fun groupsLinkNeighbours() {
        val g = WorkoutFlow.groups(listOf(item(1, 3, link = true), item(2, 3), item(3, 3)))
        assertEquals(listOf(listOf(1L, 2L), listOf(3L)), g.map { grp -> grp.map { it.exerciseId } })
    }

    @Test fun lastLinkFlagIsIgnored() {
        val g = WorkoutFlow.groups(listOf(item(1, 3), item(2, 3, link = true)))
        assertEquals(2, g.size)
    }

    @Test fun emptyRoutineHasNoGroups() = assertEquals(0, WorkoutFlow.groups(emptyList()).size)

    @Test fun singleExerciseRestAfterEverySet() {
        val s = WorkoutFlow.slots(listOf(item(1, 3, rest = 90)))
        assertEquals(listOf("1:1:BOTH:90", "1:2:BOTH:90", "1:3:BOTH:90"), s.map { it.key() })
    }

    @Test fun unilateralRightThenLeftRestAfterLeft() {
        val s = WorkoutFlow.slots(listOf(item(1, 2, rest = 45, uni = true)))
        assertEquals(
            listOf("1:1:RIGHT:0", "1:1:LEFT:45", "1:2:RIGHT:0", "1:2:LEFT:45"),
            s.map { it.key() }
        )
    }

    @Test fun supersetAlternatesAndRestsAfterLast() {
        val s = WorkoutFlow.slots(listOf(item(1, 2, rest = 60, link = true), item(2, 2, rest = 90)))
        assertEquals(
            listOf("1:1:BOTH:0", "2:1:BOTH:90", "1:2:BOTH:0", "2:2:BOTH:90"),
            s.map { it.key() }
        )
    }

    @Test fun supersetWithDifferentSetCounts() {
        val s = WorkoutFlow.slots(listOf(item(1, 3, rest = 60, link = true), item(2, 2, rest = 90)))
        assertEquals(
            listOf("1:1:BOTH:0", "2:1:BOTH:90", "1:2:BOTH:0", "2:2:BOTH:90", "1:3:BOTH:60"),
            s.map { it.key() }
        )
    }

    @Test fun supersetWithUnilateralFirst() {
        val s = WorkoutFlow.slots(listOf(item(1, 1, rest = 60, uni = true, link = true), item(2, 1, rest = 90)))
        assertEquals(
            listOf("1:1:RIGHT:0", "1:1:LEFT:0", "2:1:BOTH:90"),
            s.map { it.key() }
        )
    }

    @Test fun nextSlotFollowsOrder() {
        val slots = WorkoutFlow.slots(listOf(item(1, 2, link = true), item(2, 2)))
        assertEquals("1:1:BOTH:0", WorkoutFlow.nextSlot(slots, emptyList())!!.key())

        val l1 = listOf(LoggedSet(1, SetSide.BOTH))
        assertEquals("2:1:BOTH:60", WorkoutFlow.nextSlot(slots, l1)!!.key())

        val l2 = l1 + LoggedSet(2, SetSide.BOTH)
        assertEquals("1:2:BOTH:0", WorkoutFlow.nextSlot(slots, l2)!!.key())
        assertEquals(2, WorkoutFlow.completed(slots, l2))
    }

    @Test fun nextSlotUnilateralNeedsLeftAfterRight() {
        val slots = WorkoutFlow.slots(listOf(item(1, 2, uni = true)))
        val logged = listOf(LoggedSet(1, SetSide.RIGHT))
        assertEquals("1:1:LEFT:60", WorkoutFlow.nextSlot(slots, logged)!!.key())
    }

    @Test fun allDoneGivesNull() {
        val slots = WorkoutFlow.slots(listOf(item(1, 2)))
        val logged = List(3) { LoggedSet(1, SetSide.BOTH) }
        assertNull(WorkoutFlow.nextSlot(slots, logged))
        assertEquals(2, WorkoutFlow.completed(slots, logged))
    }

    @Test fun deletingASetReopensIt() {
        val slots = WorkoutFlow.slots(listOf(item(1, 3)))
        val logged = List(2) { LoggedSet(1, SetSide.BOTH) }
        assertEquals(3, WorkoutFlow.nextSlot(slots, logged)!!.setNumber)
        assertEquals(2, WorkoutFlow.nextSlot(slots, logged.dropLast(1))!!.setNumber)
    }
}
