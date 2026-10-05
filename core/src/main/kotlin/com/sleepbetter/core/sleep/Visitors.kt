package com.sleepbetter.core.sleep

/**
 * Animals that move onto the user's island. Pico and Lulu, two baby dinos,
 * are there from the start; most others arrive after a number of
 * steady nights (bed within 30 minutes of the target); Dozy comes for focus
 * sessions instead.
 */
enum class Visitor(
    val displayName: String,
    val steadyNightsNeeded: Int,
    val focusSessionsNeeded: Int = 0,
    val blurb: String,
) {
    PIP("Pico", 0, blurb = "Pico yawns 30 minutes before bedtime and sleeps on the moon all night."),
    LULU("Lulu", 0, blurb = "Lulu is Pico's best friend. She still wears a bit of her eggshell and holds Pico's paw when it thunders."),
    PANDA("Mochi the panda", 2, blurb = "Mochi loves soft rain. Try Light rain tonight."),
    EMBER("Ember the fox", 3, blurb = "Ember loves the campfire. Turn on Campfire tonight."),
    PEBBLE("Pebble the elephant", 5, blurb = "Pebble wears a cosy nightcap and sprinkles a little rain with its trunk. Try Water drops tonight."),
    HOOT("Hoot the owl", 7, blurb = "Hoot keeps watch whenever Night forest is playing."),
    DOZY("Dozy the dino", 0, focusSessionsNeeded = 4, blurb = "Dozy nods along to the music in focus sessions."),
    KOALA("Koko the koala", 10, blurb = "Koko can nap through any thunderstorm."),
    TOFFEE("Toffee the capybara", 12, blurb = "Toffee is the calmest friend of all. Try River tonight."),
    CAT("Purr the cat", 14, blurb = "Purr hums along when Brown noise is on."),
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
        Visitor.entries.firstOrNull { it.focusSessionsNeeded == 0 && it.steadyNightsNeeded > 0 && it.steadyNightsNeeded == totalSteadyNights }
}
