package com.sleepbetter.core.audio

/**
 * A stereo recording decoded to 16-bit PCM, interleaved L/R. Kept as shorts
 * (half the memory of floats) and converted per sample while playing.
 */
class StereoClip(val pcm: ShortArray, val sampleRate: Int) {
    val frames: Int get() = pcm.size / 2
}

/** Sources that keep their own stereo image (field recordings). The mixer balances them instead of panning a mono signal. */
interface StereoSource : SoundSource {
    /** Fills [left] and [right] with [frames] samples each, overwriting them. */
    fun renderStereo(left: FloatArray, right: FloatArray, frames: Int)

    override fun render(out: FloatArray, frames: Int) {
        val r = FloatArray(frames)
        renderStereo(out, r, frames)
        for (i in 0 until frames) out[i] = (out[i] + r[i]) * 0.5f
    }
}

/** What a sound plays when a real recording is available. */
sealed interface Recording {
    /** A loop already made seamless at build time (its end crossfades into its start). */
    class Loop(val clip: StereoClip) : Recording

    /** Thunder: separate strikes played at random moments over a quiet rumble. */
    class Strikes(val clips: List<StereoClip>) : Recording
}

private const val SHORT_SCALE = 1f / 32768f

/** Thunder should be felt over the rain, not lost under it; the mixer's soft clip catches the peaks. */
private const val STRIKE_BOOST = 1.7f

/**
 * Plays a seamless recorded loop forever, starting at a random point so two
 * nights never begin the same way. Recordings are normalised to about
 * -20 dBFS RMS at build time.
 */
class LoopSource(private val clip: StereoClip, seed: Long) : StereoSource {
    private var pos = ((Rng(seed).nextFloat() * clip.frames).toInt()).coerceIn(0, clip.frames - 1)

    override fun renderStereo(left: FloatArray, right: FloatArray, frames: Int) {
        val pcm = clip.pcm
        val n = clip.frames
        for (i in 0 until frames) {
            left[i] = pcm[2 * pos] * SHORT_SCALE
            right[i] = pcm[2 * pos + 1] * SHORT_SCALE
            if (++pos >= n) pos = 0
        }
    }
}

/**
 * Heavy, natural thunder from real recordings. Strikes come at random
 * intervals, never the same clip twice in a row, at random loudness; now and
 * then a second strike rolls in before the first has faded, like a real
 * storm. Underneath, a soft low rumble keeps the sky alive. Every strike
 * bumps [eventCount] so the lightning in the scene flashes with it.
 */
class SampledThunderSource(private val clips: List<StereoClip>, private val sampleRate: Int, seed: Long) : StereoSource, EventfulSource {
    private val rng = Rng(seed)
    private val bedNoise = BrownNoise(rng)
    private val bedFilter = OnePoleLowPass(sampleRate, 110f)

    private class Voice(val clip: StereoClip, val gain: Float) {
        var pos = 0
    }

    private val voices = ArrayList<Voice>(2)
    private var last = -1
    private var untilNext = (rng.range(0.8f, 2.5f) * sampleRate).toInt()

    @Volatile
    override var eventCount: Int = 0
        private set

    override fun renderStereo(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            if (--untilNext <= 0) strike()
            val bed = bedFilter.process(bedNoise.next()) * 0.09f
            var l = bed
            var r = bed
            for (v in voices) {
                if (v.pos < v.clip.frames) {
                    l += v.clip.pcm[2 * v.pos] * SHORT_SCALE * v.gain * STRIKE_BOOST
                    r += v.clip.pcm[2 * v.pos + 1] * SHORT_SCALE * v.gain * STRIKE_BOOST
                    v.pos++
                }
            }
            left[i] = l
            right[i] = r
        }
        voices.removeAll { it.pos >= it.clip.frames }
    }

    private fun strike() {
        if (clips.isEmpty()) {
            untilNext = Int.MAX_VALUE
            return
        }
        var pick = (rng.nextFloat() * clips.size).toInt().coerceIn(0, clips.size - 1)
        if (clips.size > 1 && pick == last) pick = (pick + 1) % clips.size
        last = pick
        if (voices.size >= 2) voices.removeAt(0)
        val clip = clips[pick]
        voices += Voice(clip, rng.range(0.75f, 1f))
        eventCount++
        // Usually wait for this one to roll away; sometimes another comes in early.
        val clipSeconds = clip.frames / sampleRate.toFloat()
        val gap = if (rng.chance(0.3f)) rng.range(1.5f, clipSeconds * 0.6f) else clipSeconds * 0.8f + rng.range(2f, 8f)
        untilNext = (gap * sampleRate).toInt().coerceAtLeast(sampleRate)
    }
}

/**
 * Procedural sea, used when no recording is available: waves as slow swells
 * of low noise, each breaking into a hiss of foam that washes back out.
 */
class SeaSource(private val sampleRate: Int, seed: Long) : SoundSource {
    private val rng = Rng(seed)
    private val deep = BrownNoise(rng)
    private val deepFilter = OnePoleLowPass(sampleRate, 380f)
    private val foamFilter = OnePoleHighPass(sampleRate, 900f)
    private val foamSmooth = OnePoleLowPass(sampleRate, 5200f)
    private var phase = 0f
    private var period = rng.range(7f, 11f)
    private var strength = rng.range(0.6f, 1f)

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            phase += 1f / (period * sampleRate)
            if (phase >= 1f) {
                phase -= 1f
                period = rng.range(7f, 12f)
                strength = rng.range(0.55f, 1f)
            }
            // Swell builds, breaks around 60%, then the wash drains away.
            val swell = if (phase < 0.6f) (phase / 0.6f).let { it * it } else 1f - (phase - 0.6f) / 0.4f * 0.85f
            val foam = if (phase in 0.55f..1f) {
                val p = (phase - 0.55f) / 0.45f
                (1f - p) * (1f - p) * strength
            } else 0f
            val body = deepFilter.process(deep.next()) * (0.25f + 0.75f * swell) * strength
            val hiss = foamSmooth.process(foamFilter.process(rng.bipolar())) * foam * 0.45f
            out[i] = body + hiss
        }
    }
}
