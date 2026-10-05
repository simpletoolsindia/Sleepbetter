package com.sleepbetter.core.sleep

import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** The three wellness levels shown to the user. Never a diagnosis. */
enum class RiskLevel(val label: String) {
    GOOD("Good"),
    MEDIUM("Medium risk"),
    AT_RISK("At risk"),
}

data class SleepReport(
    /** 0..100. */
    val score: Int,
    val level: RiskLevel,
    val nights: Int,
    val averageMinutes: Int,
    /** Sum of shortfall below the goal over the window, in minutes. */
    val debtMinutes: Int,
    /** Spread of bedtimes (standard deviation), in minutes. */
    val bedtimeSpreadMinutes: Int,
    /** Nights that started within 30 minutes of the target bedtime. */
    val steadyNights: Int,
    /** Nights under 6 hours. */
    val shortNights: Int,
) {
    /** Fewer than three nights is too little to judge. */
    val enoughData: Boolean get() = nights >= 3
}

/**
 * Sleep-health score from the last seven nights, following the plan:
 * duration 40, regularity 30, sleep debt 15, quality 15.
 *
 * Evidence: AASM/SRS recommend 7+ hours for adults and call 6 or fewer
 * inadequate; UK Biobank found regularity predicts mortality better than
 * duration.
 */
class SleepScoreCalculator(
    private val targetBedtimeMinuteOfDay: Int,
    private val goalMinutes: Int = 7 * 60,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    fun report(sessions: List<SleepSession>): SleepReport {
        val recent = sessions.sortedBy { it.start }.takeLast(7)
        if (recent.isEmpty()) {
            return SleepReport(0, RiskLevel.MEDIUM, 0, 0, 0, 0, 0, 0)
        }
        val minutes = recent.map { it.minutes }
        val avg = minutes.average()
        val debt = minutes.sumOf { (goalMinutes - it).coerceAtLeast(0) }
        val bedtimes = recent.map { minutesFromNoon(it.bedtimeMinuteOfDay(zone)).toDouble() }
        val mean = bedtimes.average()
        val spread = sqrt(bedtimes.sumOf { (it - mean) * (it - mean) } / bedtimes.size)
        val steady = recent.count { clockDistance(it.bedtimeMinuteOfDay(zone), targetBedtimeMinuteOfDay) <= 30 }
        val short = minutes.count { it < 6 * 60 }

        val durationPart = durationPoints(avg) * 40
        val regularityPart = linear(spread, best = 30.0, worst = 120.0) * 30
        val debtPart = linear(debt.toDouble(), best = 60.0, worst = 420.0) * 15
        val ratings = recent.mapNotNull { it.rating }
        val score = if (ratings.isEmpty()) {
            // No answers yet: score the other 85 points out of 100.
            (durationPart + regularityPart + debtPart) / 85.0 * 100.0
        } else {
            durationPart + regularityPart + debtPart + linear(ratings.average(), best = 4.0, worst = 1.0) * 15
        }
        val rounded = score.roundToInt().coerceIn(0, 100)

        var level = when {
            rounded >= 75 -> RiskLevel.GOOD
            rounded >= 50 -> RiskLevel.MEDIUM
            else -> RiskLevel.AT_RISK
        }
        // Hard rules from the plan, whatever the points say.
        if (avg < 7 * 60 && level == RiskLevel.GOOD) level = RiskLevel.MEDIUM
        if (recent.size >= 5 && short >= 5) level = RiskLevel.AT_RISK

        return SleepReport(
            score = rounded,
            level = level,
            nights = recent.size,
            averageMinutes = avg.roundToInt(),
            debtMinutes = debt,
            bedtimeSpreadMinutes = spread.roundToInt(),
            steadyNights = steady,
            shortNights = short,
        )
    }

    /** Full marks for 7..9 h, falling to zero at 5 h or 10.5 h. */
    private fun durationPoints(avgMinutes: Double): Double = when {
        avgMinutes < goalMinutes -> linear(avgMinutes, best = goalMinutes.toDouble(), worst = 5 * 60.0)
        avgMinutes <= 9 * 60 -> 1.0
        else -> linear(avgMinutes, best = 9 * 60.0, worst = 10.5 * 60)
    }

    /** 1 at [best], 0 at [worst], linear between; works whichever way round they are. */
    private fun linear(value: Double, best: Double, worst: Double): Double =
        ((value - worst) / (best - worst)).coerceIn(0.0, 1.0)
}
