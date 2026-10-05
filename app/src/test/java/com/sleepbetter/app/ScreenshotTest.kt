package com.sleepbetter.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.sleepbetter.app.ui.components.MochiAndToffee
import com.sleepbetter.app.ui.components.MochiView
import com.sleepbetter.app.ui.components.SceneFrames
import com.sleepbetter.app.ui.components.Species
import com.sleepbetter.core.audio.SoundId
import com.sleepbetter.app.ui.theme.AppTheme
import com.sleepbetter.app.ui.theme.Palette
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
    @Test fun homeJade() = shot(Destination.HOME, "10-theme-jade-home", theme = AppTheme.JADE)
    @Test fun soundsCoolBlue() = shot(Destination.SOUNDS, "11-theme-cool-blue-sounds", theme = AppTheme.COOL_BLUE)
    @Test fun insightsPlum() = shot(Destination.INSIGHTS, "12-theme-plum-noir-insights", theme = AppTheme.PLUM_NOIR)
    @Test fun windDownAmber() = shot(Destination.WIND_DOWN, "13-theme-amber-wind-down", theme = AppTheme.AMBER)

    /** Every friend, plus Mochi's moods, sleeping and focus looks. */
    @Test fun characters() {
        Palette.theme = AppTheme.MOON_MILK
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SleepBetterTheme {
                Column(Modifier.fillMaxSize().background(Palette.Paper).padding(12.dp)) {
                    Species.entries.chunked(3).forEach { row ->
                        Row { row.forEach { MochiView(Modifier.size(125.dp), species = it, mood = 3f) } }
                    }
                    MochiAndToffee(Modifier.size(width = 300.dp, height = 176.dp))
                    Row {
                        listOf(0.5f, 2f, 4f).forEach { MochiView(Modifier.size(120.dp), mood = it) }
                    }
                    Row {
                        MochiView(Modifier.size(120.dp), sleeping = true)
                        MochiView(Modifier.size(120.dp), headphones = true, mood = 3.5f)
                        MochiView(Modifier.size(120.dp), species = Species.FOX, silhouette = true)
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage("build/screenshots/14-characters.png")
    }

    /** The reminder notification's flip-book frames and the playback artwork, as the system will get them. */
    @Test fun notificationFrames() {
        val sounds = setOf(SoundId.RAIN, SoundId.THUNDER, SoundId.TENT, SoundId.NIGHT_FOREST)
        val times = List(6) { 2f + it * 0.15f }
        SceneFrames.frames(sounds, 320, 140, times, lightningAt = times[1]).forEachIndexed { i, frame ->
            save(frame, "15-notification-frame-$i.png")
        }
        save(SceneFrames.still(sounds, 360, 360), "16-playback-artwork.png")
    }

    private fun save(bitmap: android.graphics.Bitmap, name: String) {
        val file = java.io.File("build/screenshots/$name").apply { parentFile?.mkdirs() }
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun shot(destination: Destination, name: String, seed: Boolean = true, theme: AppTheme? = null) {
        val app = ApplicationProvider.getApplicationContext<SleepBetterApp>()
        if (seed) seedWeek(app)
        val vm = AppViewModel(app)
        theme?.let(vm::setTheme)
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
