package com.sleepbetter.core.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/** Small, fast xorshift random generator. Deterministic for a given seed, so tests are repeatable. */
class Rng(seed: Long) {
    private var state: Long = if (seed == 0L) 0x2545F4914F6CDD1DL else seed

    fun nextLong(): Long {
        var x = state
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        state = x
        return x
    }

    /** Uniform in [0, 1). */
    fun nextFloat(): Float = (nextLong() ushr 40).toFloat() / 16_777_216f

    /** Uniform in [-1, 1). */
    fun bipolar(): Float = nextFloat() * 2f - 1f

    fun range(from: Float, to: Float): Float = from + (to - from) * nextFloat()

    fun chance(probability: Float): Boolean = nextFloat() < probability
}

/** One-pole low-pass filter. Cheap and smooth, good for taming noise. */
class OnePoleLowPass(private val sampleRate: Int, cutoffHz: Float) {
    private var a = 0f
    private var y = 0f

    init {
        setCutoff(cutoffHz)
    }

    fun setCutoff(cutoffHz: Float) {
        a = (1.0 - exp(-2.0 * PI * cutoffHz / sampleRate)).toFloat()
    }

    fun process(x: Float): Float {
        y += a * (x - y)
        return y
    }
}

/** One-pole high-pass, built as input minus its low-passed copy. */
class OnePoleHighPass(sampleRate: Int, cutoffHz: Float) {
    private val lp = OnePoleLowPass(sampleRate, cutoffHz)
    fun process(x: Float): Float = x - lp.process(x)
}

/** RBJ biquad with band-pass (0 dB peak) and low-pass shapes. */
class Biquad(private val sampleRate: Int) {
    private var b0 = 0f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f
    private var z1 = 0f
    private var z2 = 0f

    fun setBandPass(centerHz: Float, q: Float): Biquad {
        val w0 = 2.0 * PI * centerHz.coerceIn(20f, sampleRate * 0.45f) / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val a0 = 1.0 + alpha
        b0 = (alpha / a0).toFloat()
        b1 = 0f
        b2 = (-alpha / a0).toFloat()
        a1 = (-2.0 * cos(w0) / a0).toFloat()
        a2 = ((1.0 - alpha) / a0).toFloat()
        return this
    }

    fun setLowPass(cutoffHz: Float, q: Float = 0.707f): Biquad {
        val w0 = 2.0 * PI * cutoffHz.coerceIn(20f, sampleRate * 0.45f) / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = cos(w0)
        val a0 = 1.0 + alpha
        b0 = (((1.0 - cosW0) / 2.0) / a0).toFloat()
        b1 = ((1.0 - cosW0) / a0).toFloat()
        b2 = b0
        a1 = (-2.0 * cosW0 / a0).toFloat()
        a2 = ((1.0 - alpha) / a0).toFloat()
        return this
    }

    fun reset() {
        z1 = 0f
        z2 = 0f
    }

    /** Transposed direct form II. */
    fun process(x: Float): Float {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }
}

/** Pink noise (equal energy per octave) using Paul Kellet's refined filter. Sounds like steady rain. */
class PinkNoise(private val rng: Rng) {
    private var b0 = 0f
    private var b1 = 0f
    private var b2 = 0f
    private var b3 = 0f
    private var b4 = 0f
    private var b5 = 0f
    private var b6 = 0f

    fun next(): Float {
        val white = rng.bipolar()
        b0 = 0.99886f * b0 + white * 0.0555179f
        b1 = 0.99332f * b1 + white * 0.0750759f
        b2 = 0.96900f * b2 + white * 0.1538520f
        b3 = 0.86650f * b3 + white * 0.3104856f
        b4 = 0.55000f * b4 + white * 0.5329522f
        b5 = -0.7616f * b5 - white * 0.0168980f
        val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362f
        b6 = white * 0.115926f
        return pink * 0.11f
    }
}

/** Brown (red) noise: a leaky integrator of white noise. Deep, like a distant waterfall. */
class BrownNoise(private val rng: Rng) {
    private var last = 0f
    fun next(): Float {
        last = (last + 0.02f * rng.bipolar()) / 1.02f
        return last * 3.5f
    }
}

/** A value that glides linearly to its target, so gain changes never click. */
class SmoothedValue(initial: Float) {
    var current: Float = initial
        private set
    var target: Float = initial
        private set
    private var step = 0f
    private var remaining = 0

    fun setTarget(value: Float, rampSamples: Int) {
        target = value
        if (rampSamples <= 0) {
            current = value
            remaining = 0
            step = 0f
        } else {
            remaining = rampSamples
            step = (value - current) / rampSamples
        }
    }

    fun next(): Float {
        if (remaining > 0) {
            current += step
            remaining--
            if (remaining == 0) current = target
        }
        return current
    }

    val isSettled: Boolean get() = remaining == 0
}

/** MIDI note number to frequency in Hz. */
fun midiToHz(note: Int): Float = (440.0 * 2.0.pow((note - 69) / 12.0)).toFloat()

/** Smooth saturation that keeps the output inside [-1, 1] without hard clipping. */
fun softClip(x: Float): Float {
    val c = x.coerceIn(-3f, 3f)
    return c * (27f + c * c) / (27f + 9f * c * c)
}

internal const val TWO_PI = (2.0 * PI).toFloat()
