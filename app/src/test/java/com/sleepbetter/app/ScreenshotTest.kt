package com.sleepbetter.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.sleepbetter.app.ui.theme.SleepBetterTheme
import com.sleepbetter.core.sleep.NightTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Renders every screen on a Pixel-sized phone with a realistic week of
 * sleep, for design review. Output: app/build/screenshots/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h851dp-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun home() = shot(Destination.HOME, "01-home")
    @Test fun sounds() = shot(Destination.SOUNDS, "02-sounds")
    @Test fun insights() = shot(Destination.INSIGHTS, "03-insights")
    @Test fun friends() = shot(Destination.FRIENDS, "04-friends")
    @Test fun focus() = shot(Destination.FOCUS, "05-focus")
    @Test fun windDown() = shot(Destination.WIND_DOWN, "06-wind-down")
    @Test fun sleep() = shot(Destination.SLEEP, "07-sleep")
    @Test fun checkIn() = shot(Destination.CHECK_IN, "08-check-in")
    @Test fun homeFirstRun() = shot(Destination.HOME, "09-home-first-run", seed = false)

    private fun shot(destination: Destination, name: String, seed: Boolean = true) {
        val app = ApplicationProvider.getApplicationContext<SleepBetterApp>()
        if (seed) seedWeek(app)
        val vm = AppViewModel(app)
        compose.mainClock.autoAdvance = false
        compose.setContent { SleepBetterTheme { SleepBetterUi(vm, destination) {} } }
        compose.mainClock.advanceTimeBy(2_500)
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    /** Seven nights around a 22:50 bedtime, mostly steady, one late night. */
    private fun seedWeek(app: SleepBetterApp) {
        val zone = ZoneId.systemDefault()
        val bedtimes = listOf(22 * 60 + 41, 22 * 60 + 55, 23 * 60 + 20, 22 * 60 + 48, 22 * 60 + 52, 23 * 60 + 47, 23 * 60 + 4)
        val lengths = listOf(452, 431, 395, 461, 447, 352, 432)
        for (i in bedtimes.indices) {
            val day = LocalDate.now().minusDays((7 - i).toLong())
            val start = day.atTime(LocalTime.of(bedtimes[i] / 60, bedtimes[i] % 60)).atZone(zone).toInstant()
            app.repository.startSleep(start)
            app.repository.endSleep(start.plusSeconds(lengths[i] * 60L))
            app.repository.rateLastNight(4, if (i == 5) setOf(NightTag.LATE_COFFEE) else emptySet())
        }
    }
}
