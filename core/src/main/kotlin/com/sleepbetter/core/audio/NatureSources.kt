package com.sleepbetter.core.audio

import kotlin.math.exp

/*
 * Procedural nature sounds. They never repeat, so they need no loop points and
 * no licences. Field recordings can replace any of them later (see
 * sounds/LICENSES.csv); the mixer only depends on SoundSource.
 */

enum class RainStyle { GENTLE, DOWNPOUR, TENT, CAR }

/** Rain: a pink-noise bed plus thousands of individual drops, shaped per surface. */
class RainSource(sampleRate: Int, seed: Long, private val style: RainStyle) : SoundSource {
    private val rng = Rng(seed)
    private val pink = PinkNoise(rng)
    private val bedHigh = OnePoleHighPass(sampleRate, 180f)
    private val bedLow = Biquad(sampleRate).setLowPass(
        when (style) {
            RainStyle.GENTLE -> 6000f
            RainStyle.DOWNPOUR -> 9000f
            RainStyle.TENT -> 2600f
            RainStyle.CAR -> 1100f
        },
    )
    private val drops = HitPool(sampleRate, rng, 28)
    private val thuds = HitPool(sampleRate, rng, 6)
    private val drift = SlowDrift(sampleRate, rng, 0.85f, 1.15f, 120f, 360f)
    private val perSample = 1f / sampleRate

    private val bedGain = when (style) {
        RainStyle.GENTLE -> 0.35f
        RainStyle.DOWNPOUR -> 0.6f
        RainStyle.TENT -> 0.22f
        RainStyle.CAR -> 0.35f
    }
    private val dropRate = when (style) {
        RainStyle.GENTLE -> 45f
        RainStyle.DOWNPOUR -> 140f
        RainStyle.TENT -> 70f
        RainStyle.CAR -> 90f
    }

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            val intensity = drift.next()
            if (rng.chance(dropRate * intensity * perSample)) spawnDrop()
            if (rng.chance(thudRate() * perSample)) spawnThud()
            val bed = bedLow.process(bedHigh.process(pink.next())) * bedGain * intensity
            out[i] = bed + drops.next() + thuds.next()
        }
    }

    private fun thudRate(): Float = when (style) {
        RainStyle.TENT -> 0.6f // heavy drips from the tent edge
        RainStyle.CAR -> 1.2f // fat drops on the roof
        else -> 0f
    }

    private fun spawnDrop() {
        when (style) {
            RainStyle.GENTLE, RainStyle.DOWNPOUR ->
                drops.trigger(rng.range(2200f, 7000f), 1.4f, rng.range(3f, 10f), rng.range(0.15f, 0.45f))
            RainStyle.TENT ->
                drops.trigger(rng.range(700f, 2400f), 2.2f, rng.range(8f, 22f), rng.range(0.25f, 0.6f))
            RainStyle.CAR ->
                if (rng.chance(0.8f)) {
                    drops.trigger(rng.range(280f, 900f), 2.5f, rng.range(12f, 35f), rng.range(0.3f, 0.7f))
                } else {
                    drops.trigger(rng.range(1800f, 3800f), 1.5f, rng.range(4f, 9f), rng.range(0.08f, 0.2f))
                }
        }
    }

    private fun spawnThud() {
        thuds.trigger(rng.range(140f, 320f), 3f, rng.range(45f, 90f), rng.range(0.5f, 0.9f))
    }
}

/** Distant rumble with occasional thunder strikes. Each strike bumps [eventCount] for the lightning flash. */
class ThunderSource(private val sampleRate: Int, seed: Long) : SoundSource, EventfulSource {
    private val rng = Rng(seed)
    private val bedNoise = BrownNoise(rng)
    private val bedFilter = OnePoleLowPass(sampleRate, 140f)
    private val strikeNoise = BrownNoise(rng)
    private val strikeFilter = Biquad(sampleRate)
    private val crackle = OnePoleHighPass(sampleRate, 2000f)

    @Volatile
    override var eventCount: Int = 0
        private set

    private var untilNext = (rng.range(4f, 10f) * sampleRate).toInt()
    private var strikeLength = 0
    private var strikePos = 0
    private var attack = 1
    private var strikeGain = 0f
    private var crackleLength = 0

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            var s = bedFilter.process(bedNoise.next()) * 0.1f
            if (strikeLength == 0) {
                if (--untilNext <= 0) startStrike()
            } else {
                s += strikeSample()
            }
            out[i] = s
        }
    }

    private fun startStrike() {
        strikeLength = (rng.range(4f, 9f) * sampleRate).toInt()
        strikePos = 0
        val close = rng.chance(0.3f)
        attack = ((if (close) rng.range(0.02f, 0.08f) else rng.range(0.3f, 0.9f)) * sampleRate).toInt()
        crackleLength = if (close) (0.18f * sampleRate).toInt() else 0
        strikeGain = rng.range(0.55f, 1f)
        eventCount++
    }

    private fun strikeSample(): Float {
        val t = strikePos.toFloat() / strikeLength
        val env = if (strikePos < attack) {
            strikePos.toFloat() / attack
        } else {
            exp(-4f * (strikePos - attack).toFloat() / (strikeLength - attack))
        }
        if (strikePos % 64 == 0) strikeFilter.setLowPass(900f * (1f - t) + 80f, 0.9f)
        var s = strikeFilter.process(strikeNoise.next()) * env * strikeGain * 1.6f
        if (strikePos < crackleLength) {
            val c = 1f - strikePos.toFloat() / crackleLength
            s += crackle.process(rng.bipolar()) * c * c * 0.35f
        }
        strikePos++
        if (strikePos >= strikeLength) {
            strikeLength = 0
            untilNext = (rng.range(25f, 90f) * sampleRate).toInt()
        }
        return s
    }
}

/** Campfire: a low roar, steady crackle and the occasional pop. */
class CampfireSource(sampleRate: Int, seed: Long) : SoundSource {
    private val rng = Rng(seed)
    private val roar = BrownNoise(rng)
    private val roarFilter = OnePoleLowPass(sampleRate, 380f)
    private val hiss = OnePoleHighPass(sampleRate, 4500f)
    private val crackles = HitPool(sampleRate, rng, 16)
    private val flicker = SlowDrift(sampleRate, rng, 0.7f, 1.2f, 0.4f, 2.5f)
    private val perSample = 1f / sampleRate
    private var burst = 0

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            val f = flicker.next()
            if (rng.chance(7f * f * perSample) || (burst > 0 && rng.chance(40f * perSample))) {
                crackles.trigger(rng.range(1500f, 6000f), 0.9f, rng.range(0.8f, 3f), rng.range(0.25f, 0.9f))
                if (burst > 0) burst--
            }
            if (rng.chance(0.25f * perSample)) {
                crackles.trigger(rng.range(500f, 1400f), 1.6f, rng.range(6f, 12f), 1.1f)
                burst = rng.range(3f, 7f).toInt()
            }
            out[i] = roarFilter.process(roar.next()) * 0.28f * f +
                hiss.process(rng.bipolar()) * 0.015f +
                crackles.next()
        }
    }
}

/** Night forest: soft wind, a few crickets and a distant owl now and then. */
class NightForestSource(private val sampleRate: Int, seed: Long) : SoundSource {
    private val rng = Rng(seed)
    private val wind = PinkNoise(rng)
    private val windFilter = OnePoleLowPass(sampleRate, 480f)
    private val windDrift = SlowDrift(sampleRate, rng, 0.5f, 1.3f, 3f, 9f)
    private val crickets = Array(3) { Cricket(rng.range(3800f, 4800f), rng.range(0.5f, 0.9f), rng.range(0.035f, 0.06f)) }
    private var owlIn = (rng.range(8f, 25f) * sampleRate).toInt()
    private var owlPos = -1
    private var owlPhase = 0f
    private val hootLength = (0.38f * sampleRate).toInt()
    private val hootGap = (0.22f * sampleRate).toInt()

    private inner class Cricket(val freq: Float, val period: Float, val level: Float) {
        var phase = 0f
        var t = rng.range(0f, 1f) * period
        val pulses = if (rng.chance(0.5f)) 3 else 4
        var chirpLevel = level

        fun next(): Float {
            t += 1f / sampleRate
            if (t >= period) {
                t -= period * rng.range(0.95f, 1.05f)
                chirpLevel = level * rng.range(0.7f, 1f)
            }
            val pulseSlot = (t / 0.04f).toInt()
            val inPulse = (t % 0.04f) / 0.018f
            val gate = if (pulseSlot < pulses && inPulse < 1f) SineTable.at(inPulse * 0.5f) else 0f
            phase = wrapPhase(phase + freq / sampleRate)
            return SineTable.at(phase) * gate * chirpLevel
        }
    }

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            var s = windFilter.process(wind.next()) * 0.14f * windDrift.next()
            for (c in crickets) s += c.next()
            s += owl()
            out[i] = s
        }
    }

    private fun owl(): Float {
        if (owlPos < 0) {
            if (--owlIn <= 0) owlPos = 0
            return 0f
        }
        val total = hootLength * 2 + hootGap
        val pos = owlPos++
        if (owlPos >= total) {
            owlPos = -1
            owlIn = (rng.range(20f, 60f) * sampleRate).toInt()
        }
        val local = when {
            pos < hootLength -> pos
            pos < hootLength + hootGap -> return 0f
            else -> pos - hootLength - hootGap
        }
        val x = local.toFloat() / hootLength
        val env = SineTable.at(x * 0.5f)
        val freq = if (pos < hootLength) 410f - 30f * x else 395f - 40f * x
        owlPhase = wrapPhase(owlPhase + freq / sampleRate)
        return SineTable.at(owlPhase) * env * env * 0.12f
    }
}

/** Morning birds: short phrases of whistled chirps with gaps, over faint leaves. */
class BirdSource(private val sampleRate: Int, seed: Long) : SoundSource {
    private val rng = Rng(seed)
    private val leaves = PinkNoise(rng)
    private val leavesFilter = OnePoleHighPass(sampleRate, 1200f)
    private var wait = (rng.range(0.5f, 2f) * sampleRate).toInt()
    private var notesLeft = 0
    private var noteLen = 0
    private var notePos = 0
    private var f0 = 0f
    private var f1 = 0f
    private var phase = 0f
    private var vib = 0f
    private var level = 0f

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            var s = leavesFilter.process(leaves.next()) * 0.025f
            if (noteLen > 0) {
                s += note()
            } else if (--wait <= 0) {
                if (notesLeft == 0) notesLeft = rng.range(3f, 8f).toInt()
                startNote()
            }
            out[i] = s
        }
    }

    private fun startNote() {
        noteLen = (rng.range(0.06f, 0.22f) * sampleRate).toInt()
        notePos = 0
        f0 = rng.range(2200f, 5200f)
        f1 = f0 * rng.range(0.65f, 1.4f)
        level = rng.range(0.06f, 0.14f)
        notesLeft--
    }

    private fun note(): Float {
        val x = notePos.toFloat() / noteLen
        vib = wrapPhase(vib + 28f / sampleRate)
        val freq = (f0 + (f1 - f0) * x) * (1f + 0.03f * SineTable.at(vib))
        phase = wrapPhase(phase + freq / sampleRate)
        val env = SineTable.at(x * 0.5f)
        notePos++
        if (notePos >= noteLen) {
            noteLen = 0
            wait = if (notesLeft > 0) (rng.range(0.03f, 0.12f) * sampleRate).toInt() else (rng.range(1.5f, 6f) * sampleRate).toInt()
        }
        return SineTable.at(phase) * env * level
    }
}

/** Water dripping in a cave: rising "plop" tones through a small reverb. */
class WaterDropSource(private val sampleRate: Int, seed: Long) : SoundSource {
    private val rng = Rng(seed)
    private val air = BrownNoise(rng)
    private val airFilter = OnePoleLowPass(sampleRate, 220f)
    private val reverb = SmallReverb(sampleRate)
    private var wait = (0.3f * sampleRate).toInt()
    private var dropPos = -1
    private val dropLen = (0.09f * sampleRate).toInt()
    private var f0 = 0f
    private var level = 0f
    private var phase = 0f

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            var dry = 0f
            if (dropPos >= 0) {
                val t = dropPos.toFloat() / sampleRate
                val freq = f0 * exp(minOf(t, 0.025f) * 38f)
                phase = wrapPhase(phase + freq / sampleRate)
                dry = SineTable.at(phase) * exp(-t * 55f) * level
                if (++dropPos >= dropLen) dropPos = -1
            } else if (--wait <= 0) {
                dropPos = 0
                phase = 0f
                f0 = rng.range(480f, 950f)
                level = rng.range(0.25f, 0.5f)
                wait = (rng.range(0.4f, 2.6f) * sampleRate).toInt()
            }
            out[i] = dry * 0.7f + reverb.process(dry) * 0.6f + airFilter.process(air.next()) * 0.04f
        }
    }
}

/** A stream: several band-pass noise bands whose level and pitch wobble like gurgling water. */
class StreamSource(private val sampleRate: Int, seed: Long) : SoundSource {
    private val rng = Rng(seed)
    private val pink = PinkNoise(rng)
    private val bed = Biquad(sampleRate).setLowPass(3200f)
    private val bands = Array(3) { Biquad(sampleRate) }
    private val centers = floatArrayOf(450f, 950f, 2100f)
    private val levelDrift = Array(3) { SlowDrift(sampleRate, rng, 0.1f, 1f, 0.08f, 0.35f) }
    private val pitchDrift = Array(3) { SlowDrift(sampleRate, rng, 0.75f, 1.3f, 0.1f, 0.5f) }
    private var counter = 0

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            if (counter++ % 64 == 0) {
                for (b in bands.indices) bands[b].setBandPass(centers[b] * pitchDrift[b].next(), 2.5f)
            } else {
                for (b in bands.indices) pitchDrift[b].next()
            }
            val white = rng.bipolar()
            var s = bed.process(pink.next()) * 0.18f
            for (b in bands.indices) s += bands[b].process(white) * levelDrift[b].next() * 0.35f
            out[i] = s
        }
    }
}

enum class NoiseColor { WHITE, PINK, BROWN }

/** Plain colored noise, for masking and focus. */
class NoiseSource(seed: Long, private val color: NoiseColor) : SoundSource {
    private val rng = Rng(seed)
    private val pink = PinkNoise(rng)
    private val brown = BrownNoise(rng)

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            out[i] = when (color) {
                NoiseColor.WHITE -> rng.bipolar() * 0.15f
                NoiseColor.PINK -> pink.next() * 0.4f
                NoiseColor.BROWN -> brown.next() * 0.5f
            }
        }
    }
}

/** Schroeder-style reverb: four combs and two all-passes. Enough for a cave. */
internal class SmallReverb(sampleRate: Int) {
    private val combs = floatArrayOf(0.0297f, 0.0371f, 0.0411f, 0.0437f).map { Delay((it * sampleRate).toInt()) }
    private val allpasses = floatArrayOf(0.005f, 0.0017f).map { Delay((it * sampleRate).toInt()) }

    private class Delay(length: Int) {
        val buf = FloatArray(maxOf(1, length))
        var idx = 0
        fun read(): Float = buf[idx]
        fun write(v: Float) {
            buf[idx] = v
            idx = (idx + 1) % buf.size
        }
    }

    fun process(x: Float): Float {
        var sum = 0f
        for (c in combs) {
            val y = c.read()
            c.write(x + y * 0.8f)
            sum += y
        }
        var s = sum * 0.25f
        for (a in allpasses) {
            val d = a.read()
            val y = -s * 0.7f + d
            a.write(s + d * 0.7f)
            s = y
        }
        return s
    }
}
