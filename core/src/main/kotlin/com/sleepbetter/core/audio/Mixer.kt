package com.sleepbetter.core.audio

import java.util.EnumMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Maps a spot on the Sound Stage (unit disk) to loudness and stereo position. */
object Spatial {
    /** 0 at the edge of the stage, 1 at the listener. */
    fun closeness(x: Float, y: Float): Float = 1f - sqrt(x * x + y * y).coerceAtMost(1f)

    /** Linear gain. Never fully silent, so a sound on the rim is still faintly there. */
    fun gain(x: Float, y: Float): Float {
        val c = closeness(x, y)
        return 0.06f + 0.94f * c * sqrt(c)
    }

    /** -1 hard left, 1 hard right, kept a little inside the extremes. */
    fun pan(x: Float): Float = (x * 0.85f).coerceIn(-0.85f, 0.85f)

    /** Pulls a point back onto the unit disk. */
    fun clampToStage(x: Float, y: Float): Pair<Float, Float> {
        val d = sqrt(x * x + y * y)
        return if (d <= 1f) x to y else (x / d) to (y / d)
    }
}

/**
 * Mixes the active [SoundId] layers into interleaved stereo float PCM.
 *
 * The UI thread only enqueues commands; [render] runs on the audio thread and
 * applies them at the start of each block, so there is no locking in the
 * audio path. Readable state for the UI ([levels], [thunderStrikes], ...) is
 * written by the audio thread and read without locks; it only drives visuals.
 */
class Mixer(private val sampleRate: Int, private val seed: Long = 7L) {
    private sealed interface Command {
        data class SetActive(val id: SoundId, val active: Boolean) : Command
        data class SetPosition(val id: SoundId, val x: Float, val y: Float) : Command
        data class SetMaster(val volume: Float) : Command
        data class SetTimer(val seconds: Int?, val fadeSeconds: Int) : Command
        data class ExtendTimer(val seconds: Int) : Command
    }

    private class Layer(val source: SoundSource, x: Float, y: Float) {
        var active = false
        var x = x
        var y = y
        val gain = SmoothedValue(0f)
        val pan = SmoothedValue(Spatial.pan(x))
        var level = 0f
    }

    private val commands = ConcurrentLinkedQueue<Command>()
    private val layers = EnumMap<SoundId, Layer>(SoundId::class.java)
    private var mono = FloatArray(0)
    private val master = SmoothedValue(0.8f)
    private val timer = PlaybackTimer(sampleRate)

    /** Smoothed loudness per sound (index = ordinal), 0..~1. For pulsing visuals. */
    val levels = FloatArray(SoundId.entries.size)

    @Volatile var thunderStrikes: Int = 0
        private set

    @Volatile var musicBeats: Int = 0
        private set

    @Volatile var beatPhase: Float = 0f
        private set

    @Volatile var remainingSeconds: Int? = null
        private set

    /** True once the sleep timer has fully faded out. */
    @Volatile var finished: Boolean = false
        private set

    fun setActive(id: SoundId, active: Boolean) = commands.add(Command.SetActive(id, active))
    fun setPosition(id: SoundId, x: Float, y: Float) = commands.add(Command.SetPosition(id, x, y))
    fun setMasterVolume(volume: Float) = commands.add(Command.SetMaster(volume))

    /** [seconds] null plays until stopped. */
    fun setTimer(seconds: Int?, fadeSeconds: Int = 180) = commands.add(Command.SetTimer(seconds, fadeSeconds))
    /** Positive adds time, negative removes it (down to one second). */
    fun extendTimer(seconds: Int) = commands.add(Command.ExtendTimer(seconds))

    /** Fills [out] with [frames] stereo frames (2 floats each). */
    fun render(out: FloatArray, frames: Int) {
        applyCommands()
        if (mono.size < frames) mono = FloatArray(frames)
        java.util.Arrays.fill(out, 0, frames * 2, 0f)

        for ((id, layer) in layers) {
            if (!layer.active && layer.gain.isSettled && layer.gain.current == 0f) {
                levels[id.ordinal] *= 0.8f
                continue
            }
            layer.source.render(mono, frames)
            val calibration = id.gain
            var energy = 0f
            var lastPan = Float.NaN
            var left = 0f
            var right = 0f
            for (i in 0 until frames) {
                val g = layer.gain.next()
                val p = layer.pan.next()
                if (p != lastPan) {
                    // Equal-power pan; only recomputed while the pan is moving.
                    val angle = (p + 1f) * (Math.PI.toFloat() / 4f)
                    left = cos(angle)
                    right = sin(angle)
                    lastPan = p
                }
                val s = mono[i] * g * calibration
                out[2 * i] += s * left
                out[2 * i + 1] += s * right
                energy += s * s
            }
            val rms = sqrt(energy / frames)
            layer.level = layer.level * 0.7f + rms * 0.3f
            levels[id.ordinal] = (layer.level * 4f).coerceAtMost(1f)
            when (val src = layer.source) {
                is ThunderSource -> thunderStrikes = src.eventCount
                is FocusMusicSource -> {
                    musicBeats = src.eventCount
                    beatPhase = src.beatPhase
                }
                else -> Unit
            }
        }

        for (i in 0 until frames) {
            val g = master.next() * timer.nextGain()
            out[2 * i] = softClip(out[2 * i] * g)
            out[2 * i + 1] = softClip(out[2 * i + 1] * g)
        }
        remainingSeconds = timer.remainingSeconds
        finished = timer.finished
    }

    private fun applyCommands() {
        while (true) {
            when (val c = commands.poll() ?: break) {
                is Command.SetActive -> {
                    val layer = layers.getOrPut(c.id) {
                        Layer(c.id.createSource(sampleRate, seed + c.id.ordinal * 7919L), c.id.stageX, c.id.stageY)
                    }
                    layer.active = c.active
                    // Fade in over 3 s, out over 1.5 s.
                    val ramp = if (c.active) 3 * sampleRate else sampleRate * 3 / 2
                    layer.gain.setTarget(if (c.active) Spatial.gain(layer.x, layer.y) else 0f, ramp)
                }
                is Command.SetPosition -> {
                    val layer = layers.getOrPut(c.id) {
                        Layer(c.id.createSource(sampleRate, seed + c.id.ordinal * 7919L), c.x, c.y)
                    }
                    val (x, y) = Spatial.clampToStage(c.x, c.y)
                    layer.x = x
                    layer.y = y
                    val ramp = sampleRate * 3 / 10
                    layer.pan.setTarget(Spatial.pan(x), ramp)
                    if (layer.active) layer.gain.setTarget(Spatial.gain(x, y), ramp)
                }
                is Command.SetMaster -> master.setTarget(c.volume.coerceIn(0f, 1f), sampleRate / 4)
                is Command.SetTimer -> {
                    timer.start(c.seconds, c.fadeSeconds)
                    finished = false
                }
                is Command.ExtendTimer -> timer.extend(c.seconds)
            }
        }
    }
}

/** Counts down in samples and fades the mix out over the last [fadeSeconds]. */
class PlaybackTimer(private val sampleRate: Int) {
    private var remaining: Long = -1 // -1 = no timer
    private var fadeSamples: Long = 0

    fun start(seconds: Int?, fadeSeconds: Int) {
        remaining = if (seconds == null) -1 else seconds.toLong() * sampleRate
        fadeSamples = fadeSeconds.toLong() * sampleRate
    }

    /** Adds (or with a negative value, removes) time. Never ends or clears the timer by itself. */
    fun extend(seconds: Int) {
        if (remaining > 0) remaining = maxOf(sampleRate.toLong(), remaining + seconds.toLong() * sampleRate)
    }

    fun nextGain(): Float {
        if (remaining < 0) return 1f
        if (remaining == 0L) return 0f
        remaining--
        return if (fadeSamples > 0 && remaining < fadeSamples) remaining.toFloat() / fadeSamples else 1f
    }

    val remainingSeconds: Int? get() = if (remaining < 0) null else (remaining / sampleRate).toInt()
    val finished: Boolean get() = remaining == 0L
}
