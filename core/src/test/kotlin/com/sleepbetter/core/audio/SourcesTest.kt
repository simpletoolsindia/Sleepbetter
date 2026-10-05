package com.sleepbetter.core.audio

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

class SourcesTest {
    private val rate = 48_000

    private fun renderSeconds(source: SoundSource, seconds: Int): FloatArray {
        val block = 960
        val out = FloatArray(rate * seconds)
        val buf = FloatArray(block)
        var pos = 0
        while (pos < out.size) {
            val n = minOf(block, out.size - pos)
            source.render(buf, n)
            System.arraycopy(buf, 0, out, pos, n)
            pos += n
        }
        return out
    }

    @Test
    fun everySoundIsAudibleFiniteAndBounded() {
        for (id in SoundId.entries) {
            val samples = renderSeconds(id.createSource(rate, 42L), 30)
            val rms = sqrt(samples.sumOf { (it * it).toDouble() } / samples.size)
            val peak = samples.maxOf { abs(it) }
            assertTrue(samples.all { it.isFinite() }, "$id produced NaN or infinity")
            assertTrue(rms > 1e-3, "$id is too quiet (rms=$rms)")
            assertTrue(peak < 2.5f, "$id peaks too high (peak=$peak)")
        }
    }

    @Test
    fun soundsDoNotJumpBetweenBlocks() {
        // A click shows up as a huge jump between neighbouring samples. Rendering in
        // odd block sizes must sound the same as one long block.
        for (id in listOf(SoundId.RAIN, SoundId.BROWN_NOISE, SoundId.FOCUS_MUSIC, SoundId.STREAM)) {
            val source = id.createSource(rate, 3L)
            val out = FloatArray(rate * 5)
            var pos = 0
            var size = 1
            while (pos < out.size) {
                val n = minOf(size, out.size - pos)
                val buf = FloatArray(n)
                source.render(buf, n)
                System.arraycopy(buf, 0, out, pos, n)
                pos += n
                size = size * 3 % 1021 + 1
            }
            var maxJump = 0f
            for (i in 1 until out.size) maxJump = maxOf(maxJump, abs(out[i] - out[i - 1]))
            assertTrue(maxJump < 1.0f, "$id jumps by $maxJump between samples")
        }
    }

    @Test
    fun thunderStrikesWithinAMinute() {
        val thunder = ThunderSource(rate, 11L)
        renderSeconds(thunder, 60)
        assertTrue(thunder.eventCount >= 1, "no thunder strike in 60 s")
    }

    @Test
    fun focusMusicKeepsTempo() {
        val music = FocusMusicSource(rate, 5L, bpm = 72f)
        renderSeconds(music, 10)
        // 72 BPM for 10 s is 12 beats (the first beat fires at time zero).
        assertTrue(music.eventCount in 12..13, "beats=${music.eventCount}")
    }

    @Test
    fun longNightStaysStable() {
        // Two simulated hours of the oscillator-heavy sources must stay finite.
        for (id in listOf(SoundId.NIGHT_FOREST, SoundId.BIRDS, SoundId.WATER_DROPS)) {
            val source = id.createSource(rate, 9L)
            val buf = FloatArray(4800)
            repeat(rate * 7200 / 4800 / 20) { source.render(buf, buf.size) } // 6 minutes of blocks
            assertTrue(buf.all { it.isFinite() }, "$id became unstable")
        }
    }
}
