package com.sleepbetter.core.sleep

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoSleepTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val morning = LocalDate.of(2026, 10, 6)

    /** A use from [from] to [to], "HH:mm"; evening times fall on the day before [morning]. */
    private fun use(from: String, to: String): PhoneUse {
        fun at(t: String): java.time.Instant {
            val time = LocalTime.parse(t)
            val day = if (time.hour >= 17) morning.minusDays(1) else morning
            return day.atTime(time).atZone(zone).toInstant()
        }
        return PhoneUse(at(from), at(to))
    }

    private fun clock(i: java.time.Instant) = i.atZone(zone).toLocalTime().toString()

    @Test
    fun `the longest quiet stretch of the night is the sleep`() {
        val night = AutoSleepDetector.detect(
            listOf(use("20:00", "20:40"), use("22:10", "23:35"), use("07:05", "07:30"), use("09:00", "09:20")),
            morning, zone,
        )
        assertNotNull(night)
        assertEquals("23:35", clock(night.phoneDown))
        assertEquals("07:05", clock(night.pickedUp))
        assertEquals("23:47", clock(night.asleep))
        assertEquals(0, night.wakeUps)
        assertTrue(night.toSession().auto)
    }

    @Test
    fun `a quick look at the time is a wake-up, not the end of the night`() {
        val night = AutoSleepDetector.detect(
            listOf(use("22:30", "23:15"), use("03:10", "03:12"), use("04:40", "04:50"), use("06:45", "07:20")),
            morning, zone,
        )
        assertNotNull(night)
        assertEquals("23:15", clock(night.phoneDown))
        assertEquals("06:45", clock(night.pickedUp))
        assertEquals(2, night.wakeUps)
    }

    @Test
    fun `a long session at night splits it and the small hours win`() {
        val night = AutoSleepDetector.detect(
            listOf(use("21:00", "21:30"), use("00:30", "01:30"), use("06:30", "07:00")),
            morning, zone,
        )
        assertNotNull(night)
        assertEquals("01:30", clock(night.phoneDown))
        assertEquals("06:30", clock(night.pickedUp))
    }

    @Test
    fun `no night while the phone has not been picked up yet`() {
        assertNull(AutoSleepDetector.detect(listOf(use("22:00", "23:30")), morning, zone))
    }

    @Test
    fun `short gaps are not a night`() {
        assertNull(
            AutoSleepDetector.detect(
                listOf(use("22:00", "23:00"), use("01:00", "02:00"), use("04:00", "05:00"), use("06:30", "08:00")),
                morning, zone,
            ),
        )
    }

    @Test
    fun `overlapping uses are joined`() {
        val merged = AutoSleepDetector.merge(listOf(use("22:00", "22:30"), use("22:20", "22:50"), use("23:00", "23:05")))
        assertEquals(2, merged.size)
        assertEquals("22:50", clock(merged[0].end))
    }
}
