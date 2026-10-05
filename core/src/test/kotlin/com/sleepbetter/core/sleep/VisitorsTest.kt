package com.sleepbetter.core.sleep

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VisitorsTest {
    @Test
    fun picoAndLuluAreThereFromDayOne() {
        val p = VisitorRules.progress(totalSteadyNights = 0, focusSessions = 0)
        assertEquals(listOf(Visitor.PIP, Visitor.LULU), p.unlocked)
        assertNull(VisitorRules.arrivedAt(0))
        assertEquals(Visitor.PANDA, p.next)
        assertEquals(2, p.nightsToNext)
    }

    @Test
    fun emberArrivesAfterThreeSteadyNights() {
        assertEquals(Visitor.EMBER, VisitorRules.arrivedAt(3))
        assertEquals(Visitor.REX, VisitorRules.arrivedAt(4))
        assertNull(VisitorRules.arrivedAt(6))
        assertTrue(Visitor.EMBER in VisitorRules.progress(3, 0).unlocked)
    }

    @Test
    fun pebbleArrivesAfterFiveSteadyNights() {
        assertEquals(Visitor.PEBBLE, VisitorRules.arrivedAt(5))
        assertEquals(Visitor.PEBBLE, VisitorRules.progress(4, 0).next)
        assertEquals(1, VisitorRules.progress(4, 0).nightsToNext)
    }

    @Test
    fun dinoGangArrivesInOrder() {
        assertEquals(Visitor.TRIKE, VisitorRules.arrivedAt(9))
        assertEquals(Visitor.STEGO, VisitorRules.arrivedAt(16))
        assertEquals(Visitor.ANKY, VisitorRules.arrivedAt(25))
        assertTrue(Visitor.PTERO in VisitorRules.progress(0, 8).unlocked)
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
