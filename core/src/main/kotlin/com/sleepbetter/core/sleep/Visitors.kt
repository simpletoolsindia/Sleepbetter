package com.sleepbetter.core.sleep

/**
 * Animals that move onto the user's island. Most arrive after a number of
 * steady nights (bed within 30 minutes of the target); Dozy comes for focus
 * sessions instead.
 */
enum class Visitor(
    val displayName: String,
    val steadyNightsNeeded: Int,
    val focusSessionsNeeded: Int = 0,
    val blurb: String,
) {
    PIP("Pip", 0, blurb = "Yawns 30 minutes before your bedtime and sleeps on the island all night."),
    EMBER("Ember the fox", 3, blurb = "Curls up by the campfire. Turn on Campfire to see Ember there."),
    HOOT("Hoot", 7, blurb = "Sits in the tree whenever Night forest is playing."),
    DOZY("Dozy", 0, focusSessionsNeeded = 4, blurb = "Wears headphones and nods along during focus sessions."),
    KOALA("Someone fluffy", 10, blurb = "A sleepy koala who naps through anything."),
    CAT("Someone purring", 14, blurb = "A cat who purrs when Brown noise is on."),
}

data class VisitorProgress(
    val unlocked: List<Visitor>,
    /** The next visitor to arrive by steady nights, if any are left. */
    val next: Visitor?,
    val nightsToNext: Int,
)

object VisitorRules {
    fun progress(totalSteadyNights: Int, focusSessions: Int): VisitorProgress {
        val unlocked = Visitor.entries.filter {
            totalSteadyNights >= it.steadyNightsNeeded && focusSessions >= it.focusSessionsNeeded
        }
        val next = Visitor.entries
            .filter { it.focusSessionsNeeded == 0 && it.steadyNightsNeeded > totalSteadyNights }
            .minByOrNull { it.steadyNightsNeeded }
        return VisitorProgress(unlocked, next, next?.let { it.steadyNightsNeeded - totalSteadyNights } ?: 0)
    }

    /** The visitor that arrived exactly at this count, for the morning reveal. */
    fun arrivedAt(totalSteadyNights: Int): Visitor? =
        Visitor.entries.firstOrNull { it.focusSessionsNeeded == 0 && it.steadyNightsNeeded == totalSteadyNights && it != Visitor.PIP }
}
