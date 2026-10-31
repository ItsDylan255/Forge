package com.example.fitnesstracker.service

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestTimerStoreTest {

    @After fun reset() = RestTimerStore.skip()

    @Test fun startAndSkip() {
        RestTimerStore.start("Bankdrücken", 90, now = 1_000)
        assertEquals(RestTimer("Bankdrücken", 91_000, 90_000), RestTimerStore.timer.value)
        RestTimerStore.skip()
        assertNull(RestTimerStore.timer.value)
    }

    @Test fun zeroSecondsMeansNoRest() {
        RestTimerStore.start("Bankdrücken", 0, now = 1_000)
        assertNull(RestTimerStore.timer.value)
    }

    @Test fun extendAddsToRunningRest() {
        RestTimerStore.start("Bankdrücken", 90, now = 1_000)
        RestTimerStore.extend(30, now = 50_000)
        assertEquals(RestTimer("Bankdrücken", 121_000, 120_000), RestTimerStore.timer.value)
    }

    @Test fun extendAfterEndStartsFromNow() {
        RestTimerStore.start("Bankdrücken", 60, now = 0)
        RestTimerStore.extend(30, now = 70_000)
        val timer = RestTimerStore.timer.value!!
        assertEquals(100_000L, timer.endsAtMillis)
        assertEquals(100_000L, timer.totalMillis)
    }

    @Test fun extendWithoutRestDoesNothing() {
        RestTimerStore.extend(30, now = 0)
        assertNull(RestTimerStore.timer.value)
    }
}
