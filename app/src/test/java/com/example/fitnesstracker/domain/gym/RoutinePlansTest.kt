package com.example.fitnesstracker.domain.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutinePlansTest {

    private val basic = PlanRef(1, "Fitnessbasic")
    private val alt = PlanRef(2, "Alter Plan", archived = true)
    private val empty = PlanRef(3, "Aufbau")

    private val leg2 = RoutineRef(10, "Leg Day 2", planId = 1, planPosition = 1)
    private val leg1 = RoutineRef(11, "Leg Day 1", planId = 1, planPosition = 0)
    private val oldPush = RoutineRef(12, "Push", planId = 2, planPosition = 0)
    private val cardio = RoutineRef(13, "cardio")
    private val abs = RoutineRef(14, "Abs", archived = true)
    private val archivedInPlan = RoutineRef(15, "Upper", planId = 1, planPosition = 2, archived = true)

    private val all = listOf(leg2, leg1, oldPush, cardio, abs, archivedInPlan)
    private val plans = listOf(basic, alt, empty)

    @Test fun groupsActivePlansByNameThenLoose() {
        val groups = RoutinePlans.groups(all, plans)
        assertEquals(listOf(empty, basic, null), groups.map { it.plan })
        assertEquals(emptyList<RoutineRef>(), groups[0].routines)
        assertEquals(listOf(leg1, leg2), groups[1].routines)
        assertEquals(listOf(cardio), groups[2].routines)
    }

    @Test fun noLooseGroupWithoutLooseRoutines() {
        val groups = RoutinePlans.groups(listOf(leg1), listOf(basic))
        assertEquals(listOf(basic), groups.map { it.plan })
    }

    @Test fun routineWithUnknownPlanIsLoose() {
        val orphan = RoutineRef(20, "Orphan", planId = 99)
        assertEquals(listOf(orphan), RoutinePlans.groups(listOf(orphan), emptyList()).single().routines)
    }

    @Test fun archiveContainsArchivedPlansWithAllRoutinesAndSingleRoutines() {
        val archive = RoutinePlans.archive(all, plans)
        assertEquals(listOf(alt), archive.plans.map { it.plan })
        assertEquals(listOf(oldPush), archive.plans.single().routines)
        assertEquals(listOf(abs, archivedInPlan), archive.routines)
        assertEquals(3, archive.count)
    }

    @Test fun archivedRoutineInArchivedPlanIsListedOnlyUnderPlan() {
        val r = oldPush.copy(archived = true)
        val archive = RoutinePlans.archive(listOf(r), listOf(alt))
        assertEquals(listOf(r), archive.plans.single().routines)
        assertTrue(archive.routines.isEmpty())
    }

    @Test fun restoringPlanKeepsRoutineArchived() {
        val r = oldPush.copy(archived = true)
        val active = RoutinePlans.activeRoutines(listOf(r, oldPush.copy(id = 30)), listOf(alt.copy(archived = false)))
        assertEquals(listOf(30L), active.map { it.id })
    }

    @Test fun movedSwapsWithNeighbor() {
        assertEquals(listOf(10L, 11L), RoutinePlans.moved(listOf(leg1, leg2), 10, up = true))
        assertEquals(listOf(10L, 11L), RoutinePlans.moved(listOf(leg1, leg2), 11, up = false))
    }

    @Test fun movedAtEdgeKeepsOrder() {
        assertEquals(listOf(11L, 10L), RoutinePlans.moved(listOf(leg1, leg2), 11, up = true))
        assertEquals(listOf(11L, 10L), RoutinePlans.moved(listOf(leg1, leg2), 10, up = false))
    }

    @Test fun movedResolvesDuplicatePositions() {
        val a = RoutineRef(1, "A", planId = 1)
        val b = RoutineRef(2, "B", planId = 1)
        assertEquals(listOf(2L, 1L), RoutinePlans.moved(listOf(a, b), 2, up = true))
    }
}
