package com.sleepbetter.core.audio

import kotlin.math.log10
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

/** Every sound, at the listener's position, lands near the same loudness. */
class SoundCalibrationTest {
    @Test
    fun calibratedSoundsSitBetweenMinus24AndMinus15Dbfs() {
        val rate = 48_000
        for (id in SoundId.entries) {
            val src = id.createSource(rate, 42L)
            val buf = FloatArray(960)
            var sum = 0.0
            var n = 0L
            repeat(rate * 90 / 960) {
                src.render(buf, buf.size)
                for (v in buf) {
                    val s = v * id.gain
                    sum += s * s
                    n++
                }
            }
            val db = 20 * log10(sqrt(sum / n))
            assertTrue(db in -24.0..-15.0, "$id is at %.1f dBFS".format(db))
        }
    }
}
