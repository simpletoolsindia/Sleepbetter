package com.sleepbetter.core.sleep

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** A stretch of time the phone was in use (screen on). */
data class PhoneUse(val start: Instant, val end: Instant) {
    val minutes: Long get() = Duration.between(start, end).toMinutes()
}

/**
 * A night worked out from phone use: the phone went down at [phoneDown] and
 * was properly picked up again at [pickedUp]. [asleep] allows a few minutes
 * to drift off. [wakeUps] counts the short checks in between (the time, a
 * message) that did not end the night.
 */
data class AutoNight(
    val morning: LocalDate,
    val phoneDown: Instant,
    val pickedUp: Instant,
    val asleep: Instant,
    val wakeUps: Int,
) {
    val awake: Instant get() = pickedUp
    val minutes: Int get() = Duration.between(asleep, awake).toMinutes().toInt()
    fun toSession(): SleepSession = SleepSession(asleep, awake, auto = true)
}

/**
 * Finds the night's sleep in the phone's screen-on history. The idea: people
 * put their phone down to sleep and pick it up when they get up, so the
 * longest quiet stretch of the night is the sleep. Brief looks at the phone
 * in the middle of the night count as wake-ups rather than ending it.
 */
object AutoSleepDetector {
    /** Shorter quiet stretches are naps or evenings, not a night. */
    const val MIN_NIGHT_MINUTES = 180L

    /** A check this short never ends the night (the time, an alarm, a message). */
    const val GLANCE_MINUTES = 3L

    /** A longer look still doesn't end the night if the phone stays quiet for an hour on both sides. */
    const val INTERRUPTION_MINUTES = 15L
    const val QUIET_AROUND_MINUTES = 60L

    /** Typical time to drift off after putting the phone down. */
    const val FALL_ASLEEP_MINUTES = 12L

    /** The night ending on [morning] is looked for between 18:00 the evening before and 16:00 that day. */
    fun windowFor(morning: LocalDate, zone: ZoneId): Pair<Instant, Instant> =
        morning.minusDays(1).atTime(LocalTime.of(18, 0)).atZone(zone).toInstant() to
            morning.atTime(LocalTime.of(16, 0)).atZone(zone).toInstant()

    /**
     * The night that ended on the morning of [morning], or null if the phone
     * history doesn't show one (not picked up yet, never put down long enough,
     * or no history). [uses] may cover more than the night; only the window
     * counts.
     */
    fun detect(uses: List<PhoneUse>, morning: LocalDate, zone: ZoneId): AutoNight? {
        val (from, to) = windowFor(morning, zone)
        val inWindow = merge(
            uses.filter { it.end.isAfter(from) && it.start.isBefore(to) }
                .map { PhoneUse(maxOf(it.start, from), minOf(it.end, to)) },
        )
        if (inWindow.size < 2) return null

        // Which uses end a quiet stretch, and which are only a look at the time.
        val real = inWindow.indices.filter { i -> !isGlance(inWindow, i) }
        if (real.size < 2) return null

        var best: AutoNight? = null
        var bestScore = Long.MIN_VALUE
        for (k in 0 until real.size - 1) {
            val down = inWindow[real[k]].end
            val up = inWindow[real[k + 1]].start
            val quiet = Duration.between(down, up).toMinutes()
            if (quiet < MIN_NIGHT_MINUTES) continue
            // Prefer the stretch that covers the small hours, then the longest.
            val score = coreOverlap(down, up, morning, zone) * 3 + quiet
            if (score > bestScore) {
                bestScore = score
                val asleep = down.plus(Duration.ofMinutes(minOf(FALL_ASLEEP_MINUTES, quiet / 10)))
                best = AutoNight(morning, down, up, asleep, wakeUps = real[k + 1] - real[k] - 1)
            }
        }
        return best
    }

    private fun isGlance(uses: List<PhoneUse>, i: Int): Boolean {
        val u = uses[i]
        if (u.minutes <= GLANCE_MINUTES) return true
        if (u.minutes > INTERRUPTION_MINUTES) return false
        val before = if (i > 0) Duration.between(uses[i - 1].end, u.start).toMinutes() else 0
        val after = if (i < uses.size - 1) Duration.between(u.end, uses[i + 1].start).toMinutes() else 0
        return before >= QUIET_AROUND_MINUTES && after >= QUIET_AROUND_MINUTES
    }

    /** Minutes of the stretch that fall between 01:00 and 05:00. */
    private fun coreOverlap(down: Instant, up: Instant, morning: LocalDate, zone: ZoneId): Long {
        val coreStart = morning.atTime(LocalTime.of(1, 0)).atZone(zone).toInstant()
        val coreEnd = morning.atTime(LocalTime.of(5, 0)).atZone(zone).toInstant()
        val s = maxOf(down, coreStart)
        val e = minOf(up, coreEnd)
        return if (e.isAfter(s)) Duration.between(s, e).toMinutes() else 0
    }

    /** Sorted, with touching or overlapping uses joined. */
    fun merge(uses: List<PhoneUse>): List<PhoneUse> {
        val out = ArrayList<PhoneUse>()
        for (u in uses.filter { !it.end.isBefore(it.start) }.sortedBy { it.start }) {
            val last = out.lastOrNull()
            if (last != null && !u.start.isAfter(last.end.plusSeconds(30))) {
                out[out.size - 1] = PhoneUse(last.start, maxOf(last.end, u.end))
            } else {
                out += u
            }
        }
        return out
    }
}
