package com.sleepbetter.core.sleep

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VisitorsTest {
    @Test
    fun mochiAndToffeeAreThereFromDayOne() {
        val p = VisitorRules.progress(totalSteadyNights = 0, focusSessions = 0)
        assertEquals(listOf(Visitor.PIP, Visitor.TOFFEE), p.unlocked)
        assertNull(VisitorRules.arrivedAt(0))
        assertEquals(Visitor.EMBER, p.next)
        assertEquals(3, p.nightsToNext)
    }

    @Test
    fun emberArrivesAfterThreeSteadyNights() {
        assertEquals(Visitor.EMBER, VisitorRules.arrivedAt(3))
        assertNull(VisitorRules.arrivedAt(4))
        assertTrue(Visitor.EMBER in VisitorRules.progress(3, 0).unlocked)
    }

    @Test
    fun dozyComesForFocusSessions() {
        assertTrue(Visitor.DOZY !in VisitorRules.progress(20, 3).unlocked)
        assertTrue(Visitor.DOZY in VisitorRules.progress(0, 4).unlocked)
    }

    @Test
    fun everyoneEventuallyArrives() {
        val p = VisitorRules.progress(totalSteadyNights = 30, focusSessions = 10)
        assertEquals(Visitor.entries.toSet(), p.unlocked.toSet())
        assertNull(p.next)
    }
}
