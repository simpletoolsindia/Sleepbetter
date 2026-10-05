package com.sleepbetter.core.sleep

data class Suggestion(val title: String, val body: String)

/** Rule-based tips, most important first. Plain, kind and specific; never a diagnosis. */
object Suggestions {
    fun forReport(report: SleepReport, recent: List<SleepSession>, targetBedtime: String): List<Suggestion> {
        if (!report.enoughData) {
            return listOf(
                Suggestion(
                    "A few more nights",
                    "Log three nights and we can show how steady your sleep is.",
                ),
            )
        }
        val tips = mutableListOf<Suggestion>()
        val shortBy = (7 * 60 - report.averageMinutes).coerceAtLeast(0)
        if (report.level == RiskLevel.AT_RISK) {
            tips += Suggestion(
                "Start with one small change",
                "Pick a fixed wake time for the next five days and start your wind-down 45 minutes before bed.",
            )
        }
        if (shortBy > 0) {
            val hours = report.debtMinutes / 60
            tips += Suggestion(
                "Go to bed 20 minutes earlier",
                "You are about $hours hours short this week. Twenty minutes earlier for three nights closes most of that gap.",
            )
        }
        if (report.bedtimeSpreadMinutes > 60) {
            tips += Suggestion(
                "Keep one bedtime",
                "Your bedtimes vary by more than an hour. Aim for $targetBedtime, within 30 minutes, even at weekends.",
            )
        }
        val tagged = recent.flatMap { it.tags }
        if (tagged.count { it == NightTag.LATE_COFFEE } >= 2) {
            tips += Suggestion(
                "Move your last coffee earlier",
                "Caffeine lasts 5 to 6 hours. Try having your last cup before 2 pm.",
            )
        }
        if (tagged.count { it == NightTag.PHONE_IN_BED } >= 2) {
            tips += Suggestion(
                "Leave the phone outside the bed",
                "Start your sleep mix, then put the phone face down out of reach.",
            )
        }
        if (tips.isEmpty()) {
            tips += Suggestion(
                "Keep the rhythm",
                "You went to bed within 30 minutes of $targetBedtime on ${report.steadyNights} nights. Keep it going.",
            )
        }
        return tips
    }

    /** Shown under every health message. */
    const val DISCLAIMER =
        "This is a wellness guide, not a diagnosis. If poor sleep lasts for months, or you snore loudly or wake up gasping, talk to a doctor."
}
