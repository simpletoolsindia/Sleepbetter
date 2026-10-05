package com.sleepbetter.core.sleep

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

/** Things a user can tag on a night, shown as chips in the morning. */
enum class NightTag(val label: String) {
    LATE_COFFEE("Coffee after 2 pm"),
    PHONE_IN_BED("Phone in bed"),
    LATE_WORKOUT("Late workout"),
    STRESS("Stressful day"),
    ALCOHOL("Alcohol"),
    NAP("Daytime nap"),
}

/**
 * One night of sleep. [rating] is the user's own 1..5 answer to "How did you
 * sleep?", or null if they skipped it.
 */
data class SleepSession(
    val start: Instant,
    val end: Instant,
    val rating: Int? = null,
    val tags: Set<NightTag> = emptySet(),
    val minutesToFallAsleep: Int? = null,
) {
    init {
        require(!end.isBefore(start)) { "end must not be before start" }
        require(rating == null || rating in 1..5) { "rating must be 1..5" }
    }

    val minutes: Int get() = Duration.between(start, end).toMinutes().toInt()
}

/** Minutes after midnight for a time of day. */
fun LocalTime.minuteOfDay(): Int = hour * 60 + minute

/**
 * Bedtimes straddle midnight, so 23:50 and 00:10 must count as 20 minutes
 * apart. Measuring from noon keeps a whole night on one side of the wrap.
 */
fun minutesFromNoon(minuteOfDay: Int): Int = ((minuteOfDay - 12 * 60) % 1440 + 1440) % 1440

/** Smallest distance between two times of day, in minutes (0..720). */
fun clockDistance(aMinuteOfDay: Int, bMinuteOfDay: Int): Int {
    val d = abs(aMinuteOfDay - bMinuteOfDay) % 1440
    return if (d > 720) 1440 - d else d
}

fun SleepSession.bedtimeMinuteOfDay(zone: ZoneId): Int = start.atZone(zone).toLocalTime().minuteOfDay()
