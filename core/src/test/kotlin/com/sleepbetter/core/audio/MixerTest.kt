package com.sleepbetter.core.audio

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MixerTest {
    private val rate = 48_000

    private fun Mixer.renderSeconds(seconds: Float): FloatArray {
        val frames = 480
        val out = FloatArray(frames * 2)
        var last = out
        repeat((seconds * rate / frames).toInt()) {
            render(out, frames)
            last = out.copyOf()
        }
        return last
    }

    private fun rms(stereo: FloatArray, channel: Int): Float {
        var sum = 0.0
        var n = 0
        for (i in channel until stereo.size step 2) {
            sum += stereo[i] * stereo[i]
            n++
        }
        return sqrt(sum / n).toFloat()
    }

    @Test
    fun closerSoundsAreLouder() {
        assertTrue(Spatial.gain(0f, 0.1f) > Spatial.gain(0f, 0.9f))
        assertEquals(1f, Spatial.gain(0f, 0f), 1e-6f)
        assertTrue(Spatial.gain(1f, 0f) > 0f, "a sound on the rim must stay faintly audible")
    }

    @Test
    fun positionsOutsideTheStageAreClamped() {
        val (x, y) = Spatial.clampToStage(3f, 4f)
        assertEquals(1f, sqrt(x * x + y * y), 1e-5f)
    }

    @Test
    fun silentUntilSomethingIsActive() {
        val mixer = Mixer(rate)
        val out = mixer.renderSeconds(0.5f)
        assertTrue(out.all { it == 0f })
    }

    @Test
    fun panningPlacesSoundLeftOrRight() {
        val mixer = Mixer(rate)
        mixer.setPosition(SoundId.BROWN_NOISE, -0.9f, 0f)
        mixer.setActive(SoundId.BROWN_NOISE, true)
        val left = mixer.renderSeconds(4f)
        assertTrue(rms(left, 0) > rms(left, 1) * 2, "sound placed left should be louder on the left")
    }

    @Test
    fun outputNeverClipsWithEverythingOn() {
        val mixer = Mixer(rate)
        SoundId.entries.forEach {
            mixer.setPosition(it, 0f, 0f)
            mixer.setActive(it, true)
        }
        val frames = 480
        val out = FloatArray(frames * 2)
        repeat(rate * 10 / frames) {
            mixer.render(out, frames)
            assertTrue(out.all { s -> s.isFinite() && abs(s) <= 1f })
        }
    }

    @Test
    fun sleepTimerFadesToSilenceAndFinishes() {
        val mixer = Mixer(rate)
        mixer.setActive(SoundId.RAIN, true)
        mixer.setTimer(seconds = 6, fadeSeconds = 2)
        val midFade = mixer.renderSeconds(5f)
        assertTrue(rms(midFade, 0) > 0f)
        val end = mixer.renderSeconds(1.5f)
        assertTrue(end.all { it == 0f }, "mix must be silent after the timer")
        assertTrue(mixer.finished)
    }

    @Test
    fun timerOffMeansPlayUntilStopped() {
        val mixer = Mixer(rate)
        mixer.setTimer(null)
        mixer.renderSeconds(0.1f)
        assertNull(mixer.remainingSeconds)
    }

    @Test
    fun timerCanBeExtended() {
        val timer = PlaybackTimer(10)
        timer.start(seconds = 2, fadeSeconds = 0)
        timer.extend(3)
        assertEquals(5, timer.remainingSeconds)
        timer.extend(-60)
        assertEquals(1, timer.remainingSeconds, "shortening must never switch the timer off")
    }
}
