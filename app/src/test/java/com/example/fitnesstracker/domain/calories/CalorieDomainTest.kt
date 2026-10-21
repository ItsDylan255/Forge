package com.example.fitnesstracker.domain.calories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieDomainTest {

    private val split = MacroSplit(30, 40, 30)
    private val male = CalorieProfile(180, 30, Sex.MALE, ActivityLevel.MODERATE, split)

    @Test fun bmrMale() =
        assertEquals(1780.0, CalorieCalculator.bmr(80.0, 180, 30, Sex.MALE), 0.001)

    @Test fun bmrFemale() =
        assertEquals(1614.0, CalorieCalculator.bmr(80.0, 180, 30, Sex.FEMALE), 0.001)

    @Test fun tdeeUsesPal() =
        assertEquals(2759.0, CalorieCalculator.tdee(1780.0, ActivityLevel.MODERATE), 0.001)

    @Test fun macrosInGrams() {
        val m = CalorieCalculator.macros(2000, split)
        assertEquals(150, m.proteinG)
        assertEquals(200, m.carbsG)
        assertEquals(67, m.fatG)
    }

    @Test fun noGoalGivesTdee() {
        val r = CalorieCalculator.calculate(male, 80.0, null, 0)
        assertEquals(2759, r.targetKcal)
        assertEquals(CalorieNote.NO_GOAL, r.note)
    }

    @Test fun loseWithDeficit() {
        val g = CalorieGoalInput(90.0, 80.0, 100)
        val r = CalorieCalculator.calculate(male, 85.0, g, 0)
        // BMR(85)=1830 -> TDEE 2836,5; Defizit 5*7700/100=385 -> 2451,5
        assertEquals(CalorieDirection.LOSE, r.direction)
        assertEquals(CalorieNote.NONE, r.note)
        assertEquals(2452, r.targetKcal)
    }

    @Test fun normalDeficitHasNoEstimate() {
        val g = CalorieGoalInput(90.0, 80.0, 100)
        val r = CalorieCalculator.calculate(male, 85.0, g, 0)
        assertEquals(385, r.appliedDeltaKcal)
        assertNull(r.estimatedEpochDay)
    }

    @Test fun deficitIsLimitedToQuarterOfTdee() {
        // 85 kg: BMR 1830, TDEE 2836,5, Grenze 25 % = 709,125 -> Ziel 2127,375
        val g = CalorieGoalInput(90.0, 80.0, 10)
        val r = CalorieCalculator.calculate(male, 85.0, g, 0)
        assertEquals(CalorieNote.TOO_TIGHT, r.note)
        assertEquals(2127, r.targetKcal)
        assertEquals(709, r.appliedDeltaKcal)
        // 5 kg * 7700 / 709,125 = 54,3 Tage -> 55
        assertEquals(55L, r.estimatedEpochDay)
    }

    @Test fun deficitNeverGoesBelowBmr() {
        // Sitzend: TDEE 2136, 25 % = 534 -> 1602 liegt unter dem BMR 1780
        val sedentary = CalorieProfile(180, 30, Sex.MALE, ActivityLevel.SEDENTARY, split)
        val g = CalorieGoalInput(90.0, 70.0, 10)
        val r = CalorieCalculator.calculate(sedentary, 80.0, g, 0)
        assertEquals(CalorieNote.TOO_TIGHT, r.note)
        assertEquals(r.bmr, r.targetKcal)
        assertEquals(356, r.appliedDeltaKcal)
        // 10 kg * 7700 / 356 = 216,3 Tage -> 217
        assertEquals(217L, r.estimatedEpochDay)
    }

    @Test fun deficitIsCappedAtThousandForHighTdee() {
        // 150 kg, sehr aktiv: TDEE 4385,8, 25 % wären 1096 -> Grenze 1000
        val heavy = CalorieProfile(190, 30, Sex.MALE, ActivityLevel.VERY_ACTIVE, split)
        val g = CalorieGoalInput(160.0, 120.0, 10)
        val r = CalorieCalculator.calculate(heavy, 150.0, g, 0)
        assertEquals(CalorieNote.TOO_TIGHT, r.note)
        assertEquals(1000, r.appliedDeltaKcal)
        assertEquals(3386, r.targetKcal)
    }

    @Test fun reachableDeadlineIsNotLimited() {
        val g = CalorieGoalInput(90.0, 80.0, 100)
        val r = CalorieCalculator.calculate(male, 85.0, g, 0)
        assertEquals(CalorieNote.NONE, r.note)
        assertEquals(2452, r.targetKcal)
    }

    @Test fun gainAddsSurplus() {
        // 3 kg * 7700 / 60 Tage = 385 kcal Plus
        val g = CalorieGoalInput(70.0, 75.0, 60)
        val r = CalorieCalculator.calculate(male, 72.0, g, 0)
        assertEquals(CalorieDirection.GAIN, r.direction)
        assertEquals(CalorieNote.NONE, r.note)
        assertEquals(r.tdee + 385, r.targetKcal)
        assertEquals(385, r.appliedDeltaKcal)
    }

    @Test fun surplusIsLimitedToFiveHundred() {
        // 3 kg in 10 Tagen wären 2310 kcal Plus
        val g = CalorieGoalInput(70.0, 75.0, 10)
        val r = CalorieCalculator.calculate(male, 72.0, g, 0)
        assertEquals(CalorieNote.TOO_TIGHT, r.note)
        assertEquals(r.tdee + 500, r.targetKcal)
        assertEquals(500, r.appliedDeltaKcal)
        // 3 kg * 7700 / 500 = 46,2 Tage -> 47
        assertEquals(47L, r.estimatedEpochDay)
    }

    @Test fun noGoalHasNoDelta() {
        val r = CalorieCalculator.calculate(male, 80.0, null, 0)
        assertEquals(0, r.appliedDeltaKcal)
        assertNull(r.estimatedEpochDay)
    }

    @Test fun reachedGivesMaintain() {
        val g = CalorieGoalInput(90.0, 80.0, 100)
        val r = CalorieCalculator.calculate(male, 79.0, g, 0)
        assertEquals(CalorieDirection.MAINTAIN, r.direction)
        assertEquals(CalorieNote.REACHED, r.note)
        assertEquals(r.tdee, r.targetKcal)
    }

    @Test fun noDateGivesTdee() {
        val g = CalorieGoalInput(90.0, 80.0, null)
        val r = CalorieCalculator.calculate(male, 85.0, g, 0)
        assertEquals(CalorieNote.NO_DATE, r.note)
        assertEquals(r.tdee, r.targetKcal)
    }

    @Test fun datePassedGivesTdee() {
        val g = CalorieGoalInput(90.0, 80.0, 5)
        val r = CalorieCalculator.calculate(male, 85.0, g, 10)
        assertEquals(CalorieNote.DATE_PASSED, r.note)
    }

    @Test fun nullStartUsesCurrentWeight() {
        val g = CalorieGoalInput(null, 70.0, 100)
        val r = CalorieCalculator.calculate(male, 80.0, g, 0)
        assertEquals(CalorieDirection.LOSE, r.direction)
    }

    @Test fun heightParsing() {
        assertEquals(CalorieParse.Ok(180), CalorieInputParser.parseHeight("180 cm"))
        assertEquals(CalorieParse.Ok(178), CalorieInputParser.parseHeight("177,6"))
        assertTrue(CalorieInputParser.parseHeight("99") is CalorieParse.Error)
        assertTrue(CalorieInputParser.parseHeight("251") is CalorieParse.Error)
        assertTrue(CalorieInputParser.parseHeight("abc") is CalorieParse.Error)
    }

    @Test fun ageParsing() {
        assertEquals(CalorieParse.Ok(30), CalorieInputParser.parseAge("30"))
        assertTrue(CalorieInputParser.parseAge("9") is CalorieParse.Error)
        assertTrue(CalorieInputParser.parseAge("101") is CalorieParse.Error)
    }

    @Test fun splitParsing() {
        assertEquals(
            CalorieParse.Ok(MacroSplit(30, 40, 30)),
            CalorieInputParser.parseSplit("30 %", "40", "30")
        )
        assertTrue(CalorieInputParser.parseSplit("30", "40", "20") is CalorieParse.Error)
        assertTrue(CalorieInputParser.parseSplit("x", "40", "30") is CalorieParse.Error)
    }
}
