package com.sleepbetter.core.audio

import kotlin.math.sin

/**
 * One layer of the mix. Each call fills [out] with [frames] mono samples,
 * overwriting what was there. Sources keep running state, so consecutive
 * calls continue seamlessly: nothing ever loops, so there is no loop point
 * to hear.
 */
interface SoundSource {
    fun render(out: FloatArray, frames: Int)
}

/** Keeps an oscillator phase (in cycles) inside [0, 1) so float precision never degrades over a long night. */
internal fun wrapPhase(p: Float): Float = if (p >= 1f) p - kotlin.math.floor(p) else p

/** Sources that produce visible events (thunder strikes, music beats) implement this. */
interface EventfulSource {
    /** Increases by one every time something the UI should react to happens. */
    val eventCount: Int
}

/** Shared sine lookup table; much cheaper than Math.sin per sample. */
internal object SineTable {
    private const val SIZE = 4096
    private val table = FloatArray(SIZE + 1) { sin(TWO_PI * it / SIZE) }

    /** [phase] in cycles, any value; only the fractional part matters. */
    fun at(phase: Float): Float {
        val p = phase - kotlin.math.floor(phase)
        val idx = p * SIZE
        val i = idx.toInt()
        val frac = idx - i
        return table[i] + (table[i + 1] - table[i]) * frac
    }
}

/**
 * A pool of short filtered-noise "hits": raindrops, crackles, taps on fabric.
 * Each hit is noise through a band-pass filter with an exponential decay.
 */
internal class HitPool(
    private val sampleRate: Int,
    private val rng: Rng,
    size: Int,
) {
    private val filters = Array(size) { Biquad(sampleRate) }
    private val env = FloatArray(size)
    private val decay = FloatArray(size)
    private val gain = FloatArray(size)
    private val active = BooleanArray(size)

    fun trigger(centerHz: Float, q: Float, decayMs: Float, level: Float) {
        val slot = active.indexOfFirst { !it }
        if (slot < 0) return
        filters[slot].reset()
        filters[slot].setBandPass(centerHz, q)
        env[slot] = 1f
        decay[slot] = kotlin.math.exp(-1.0 / (decayMs * 0.001 * sampleRate)).toFloat()
        gain[slot] = level
        active[slot] = true
    }

    fun next(): Float {
        var sum = 0f
        for (i in active.indices) {
            if (!active[i]) continue
            sum += filters[i].process(rng.bipolar() * env[i]) * gain[i]
            env[i] *= decay[i]
            if (env[i] < 0.0005f) active[i] = false
        }
        return sum
    }
}

/** Slow random drift between [low] and [high]; makes weather "breathe". */
internal class SlowDrift(
    private val sampleRate: Int,
    private val rng: Rng,
    private val low: Float,
    private val high: Float,
    private val minSeconds: Float,
    private val maxSeconds: Float,
) {
    private val value = SmoothedValue((low + high) / 2f)

    fun next(): Float {
        if (value.isSettled) {
            val seconds = rng.range(minSeconds, maxSeconds)
            value.setTarget(rng.range(low, high), (seconds * sampleRate).toInt())
        }
        return value.next()
    }
}
