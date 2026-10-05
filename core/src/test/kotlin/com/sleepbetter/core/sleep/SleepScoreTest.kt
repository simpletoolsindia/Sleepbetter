package com.sleepbetter.core.sleep

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SleepScoreTest {
    private val zone = ZoneOffset.UTC
    private val target = LocalTime.of(22, 50).minuteOfDay()
    private val calc = SleepScoreCalculator(target, zone = zone)

    /** A night starting on day [day] at [bed], lasting [minutes]. */
    private fun night(day: Int, bed: LocalTime, minutes: Long, rating: Int? = null): SleepSession {
        val start = LocalDate.of(2026, 9, 1).plusDays(day.toLong()).atTime(bed).toInstant(zone)
        return SleepSession(start, start.plusSeconds(minutes * 60), rating)
    }

    @Test
    fun steadySevenAndAHalfHoursIsGood() {
        val week = (0 until 7).map { night(it, LocalTime.of(22, 45 + it % 2 * 10), 450, rating = 4) }
        val r = calc.report(week)
        assertEquals(RiskLevel.GOOD, r.level)
        assertTrue(r.score >= 90, "score=${r.score}")
        assertEquals(7, r.steadyNights)
        assertEquals(0, r.debtMinutes)
    }

    @Test
    fun shortNightsAreAtRiskEvenIfRegular() {
        val week = (0 until 7).map { night(it, LocalTime.of(1, 0), 5 * 60 + 10) }
        val r = calc.report(week)
        assertEquals(RiskLevel.AT_RISK, r.level)
        assertEquals(7, r.shortNights)
    }

    @Test
    fun averageUnderSevenHoursCapsAtMedium() {
        val week = (0 until 7).map { night(it, LocalTime.of(22, 50), 6 * 60 + 45, rating = 5) }
        val r = calc.report(week)
        assertEquals(RiskLevel.MEDIUM, r.level)
    }

    @Test
    fun bedtimesAcrossMidnightCountAsClose() {
        // 23:50 and 00:10 are 20 minutes apart, not 23 hours.
        val week = (0 until 6).map { night(it, if (it % 2 == 0) LocalTime.of(23, 50) else LocalTime.of(0, 10), 460) }
        val r = calc.report(week)
        assertTrue(r.bedtimeSpreadMinutes <= 10, "spread=${r.bedtimeSpreadMinutes}")
        assertEquals(20, clockDistance(LocalTime.of(23, 50).minuteOfDay(), LocalTime.of(0, 10).minuteOfDay()))
    }

    @Test
    fun irregularBedtimesLowerTheScore() {
        val steady = (0 until 7).map { night(it, LocalTime.of(22, 50), 450) }
        val hours = listOf(20, 22, 0, 2)
        val messy = (0 until 7).map { night(it, LocalTime.of(hours[it % 4], 0), 450) }
        assertTrue(calc.report(messy).score < calc.report(steady).score - 15)
    }

    @Test
    fun tooFewNightsSaysSo() {
        val r = calc.report(listOf(night(0, LocalTime.of(22, 50), 450)))
        assertFalse(r.enoughData)
        val tips = Suggestions.forReport(r, emptyList(), "22:50")
        assertEquals("A few more nights", tips.first().title)
    }

    @Test
    fun lateCoffeeTagProducesATip() {
        val week = (0 until 7).map {
            night(it, LocalTime.of(22, 50), 400).copy(tags = setOf(NightTag.LATE_COFFEE))
        }
        val tips = Suggestions.forReport(calc.report(week), week, "22:50")
        assertTrue(tips.any { it.title.contains("coffee") })
    }
}
