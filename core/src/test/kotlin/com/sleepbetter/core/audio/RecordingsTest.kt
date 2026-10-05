package com.sleepbetter.core.audio

import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

class RecordingsTest {
    private val rate = 48_000

    private fun tone(seconds: Float, hz: Float): StereoClip {
        val n = (seconds * rate).toInt()
        val pcm = ShortArray(n * 2)
        for (i in 0 until n) {
            val v = (sin(2 * Math.PI * hz * i / rate) * 8000).toInt().toShort()
            pcm[2 * i] = v
            pcm[2 * i + 1] = v
        }
        return StereoClip(pcm, rate)
    }

    private fun renderSeconds(mixer: Mixer, seconds: Int): FloatArray {
        val buf = FloatArray(960 * 2)
        val peak = FloatArray(1)
        repeat(seconds * rate / 960) {
            mixer.render(buf, 960)
            for (v in buf) peak[0] = maxOf(peak[0], kotlin.math.abs(v))
        }
        return peak
    }

    @Test
    fun aRecordedLoopPlaysInsteadOfTheGeneratedSound() {
        val mixer = Mixer(rate)
        mixer.useRecording(SoundId.RAIN, Recording.Loop(tone(1f, 440f)))
        mixer.setActive(SoundId.RAIN, true)
        assertTrue(renderSeconds(mixer, 5)[0] > 0.05f, "the loop should be audible")
    }

    @Test
    fun recordedThunderStrikesAndFlashesTheLightning() {
        val mixer = Mixer(rate)
        mixer.useRecording(SoundId.THUNDER, Recording.Strikes(listOf(tone(2f, 60f), tone(3f, 80f))))
        mixer.setActive(SoundId.THUNDER, true)
        renderSeconds(mixer, 60)
        assertTrue(mixer.thunderStrikes >= 2, "expected several strikes in a minute, got ${mixer.thunderStrikes}")
    }

    @Test
    fun theSeaSitsAtTheSameLoudnessAsEverythingElse() {
        val src = SoundId.SEA.createSource(rate, 3L)
        val buf = FloatArray(960)
        var sum = 0.0
        var n = 0L
        repeat(rate * 60 / 960) {
            src.render(buf, buf.size)
            for (v in buf) { val s = v * SoundId.SEA.gain; sum += s * s; n++ }
        }
        val db = 20 * kotlin.math.log10(kotlin.math.sqrt(sum / n))
        assertTrue(db in -24.0..-15.0, "sea at $db dBFS")
    }
}
