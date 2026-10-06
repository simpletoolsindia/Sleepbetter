package com.sleepbetter.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.sleepbetter.app.ui.components.BestFriends
import com.sleepbetter.app.ui.components.DinoEgg
import com.sleepbetter.app.ui.components.DinoWalker
import com.sleepbetter.app.ui.components.MochiView
import com.sleepbetter.app.ui.components.SceneFrames
import com.sleepbetter.app.ui.components.Species
import com.sleepbetter.core.audio.SoundId
import com.sleepbetter.app.ui.theme.AppTheme
import com.sleepbetter.app.ui.theme.Appearance
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.SleepBetterTheme
import com.sleepbetter.app.data.AutoSummary
import com.sleepbetter.app.ui.home.AutoSleepCard
import com.sleepbetter.core.sleep.AutoSleepDetector
import com.sleepbetter.core.sleep.NightTag
import com.sleepbetter.core.sleep.PhoneUse
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
    @Test fun homeDark() = shot(Destination.HOME, "17-dark-home", dark = true)
    @Test fun soundsDark() = shot(Destination.SOUNDS, "18-dark-sounds", dark = true)
    @Test fun insightsDark() = shot(Destination.INSIGHTS, "19-dark-insights", dark = true)
    @Test fun friendsDark() = shot(Destination.FRIENDS, "20-dark-friends", dark = true)
    @Test fun checkInDark() = shot(Destination.CHECK_IN, "21-dark-check-in", dark = true)
    @Test fun homeJadeDark() = shot(Destination.HOME, "22-dark-jade-home", theme = AppTheme.JADE, dark = true)

    /** Every friend, plus Pico's moods, sleeping and focus looks. */
    @Test fun characters() {
        Palette.theme = AppTheme.MOON_MILK
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SleepBetterTheme {
                Column(Modifier.fillMaxSize().background(Palette.Paper).padding(12.dp)) {
                    Species.entries.chunked(4).forEach { row ->
                        Row { row.forEach { MochiView(Modifier.size(94.dp), species = it, mood = 3f) } }
                    }
                    BestFriends(Modifier.size(width = 300.dp, height = 176.dp))
                    Row {
                        listOf(0.5f, 2f, 4f).forEach { MochiView(Modifier.size(120.dp), mood = it) }
                    }
                    Row {
                        MochiView(Modifier.size(120.dp), sleeping = true)
                        MochiView(Modifier.size(120.dp), headphones = true, mood = 3.5f)
                        MochiView(Modifier.size(120.dp), species = Species.FOX, silhouette = true)
                    }
                    Row {
                        listOf(0.2f, 0.6f, 0.95f).forEach { DinoEgg(it, Modifier.size(70.dp)) }
                    }
                    DinoWalker(Species.PICO)
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage("build/screenshots/14-characters.png")
    }

    /** The auto sleep tracking card in each state, light and dark. */
    @Test fun autoSleep() {
        Palette.theme = AppTheme.MOON_MILK
        val zone = ZoneId.systemDefault()
        val morning = LocalDate.now()
        fun at(day: LocalDate, h: Int, m: Int) = day.atTime(h, m).atZone(zone).toInstant()
        val eve = morning.minusDays(1)
        val uses = listOf(
            PhoneUse(at(eve, 20, 5), at(eve, 20, 40)), PhoneUse(at(eve, 21, 30), at(eve, 21, 55)),
            PhoneUse(at(eve, 22, 40), at(eve, 23, 22)), PhoneUse(at(morning, 2, 48), at(morning, 2, 50)),
            PhoneUse(at(morning, 7, 4), at(morning, 7, 30)), PhoneUse(at(morning, 8, 10), at(morning, 8, 25)),
        )
        val summary = AutoSummary(AutoSleepDetector.detect(uses, morning, zone)!!, uses)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SleepBetterTheme {
                Column(
                    Modifier.fillMaxSize().background(Palette.Paper).verticalScroll(rememberScrollState()).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AutoSleepCard(true, true, summary, {}, {}, {}, {}, {})
                    AutoSleepCard(false, false, null, {}, {}, {}, {}, {})
                    AutoSleepCard(true, false, null, {}, {}, {}, {}, {})
                }
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.onRoot().captureRoboImage("build/screenshots/23-auto-sleep.png")
    }

    /** The reminder notification's flip-book frames and the playback artwork, as the system will get them. */
    @Test fun notificationFrames() {
        val sounds = setOf(SoundId.RAIN, SoundId.THUNDER, SoundId.TENT, SoundId.NIGHT_FOREST)
        val times = List(8) { 2f + it * 0.07f }
        SceneFrames.frames(sounds, 270, 180, times, lightningAt = times[2]).forEachIndexed { i, frame ->
            save(frame, "15-notification-frame-$i.png")
        }
        save(SceneFrames.still(sounds, 360, 360), "16-playback-artwork.png")
    }

    private fun save(bitmap: android.graphics.Bitmap, name: String) {
        val file = java.io.File("build/screenshots/$name").apply { parentFile?.mkdirs() }
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun shot(destination: Destination, name: String, seed: Boolean = true, theme: AppTheme? = null, dark: Boolean = false) {
        val app = ApplicationProvider.getApplicationContext<SleepBetterApp>()
        if (seed) seedWeek(app)
        val vm = AppViewModel(app)
        theme?.let(vm::setTheme)
        if (dark) vm.setAppearance(Appearance.DARK)
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
