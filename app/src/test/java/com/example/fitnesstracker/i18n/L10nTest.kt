package com.example.fitnesstracker.i18n

import com.example.fitnesstracker.domain.gym.GymFormat
import com.example.fitnesstracker.domain.gym.MuscleGroups
import com.example.fitnesstracker.domain.weight.ChartRange
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class L10nTest {

    @After
    fun reset() = L10n.setEnglishForTest(false)

    @Test
    fun `German is the default`() {
        assertEquals("Speichern", tr("Speichern", "Save"))
        assertEquals("82,5 kg", GymFormat.kg(82.5))
        assertEquals("7 Tage", ChartRange.WEEK.label)
    }

    @Test
    fun `English switches texts and number format`() {
        L10n.setEnglishForTest(true)
        assertEquals("Save", tr("Speichern", "Save"))
        assertEquals("82.5 kg", GymFormat.kg(82.5))
        assertEquals("7 days", ChartRange.WEEK.label)
        assertEquals("1 h 05 min", GymFormat.duration(65 * 60_000L))
    }

    @Test
    fun `muscle groups stay German in storage and are translated for display`() {
        L10n.setEnglishForTest(true)
        assertEquals("Rücken", MuscleGroups.all[1])
        assertEquals("Back", MuscleGroups.label("Rücken"))
        assertEquals("Eigene Gruppe", MuscleGroups.label("Eigene Gruppe"))
    }
}
