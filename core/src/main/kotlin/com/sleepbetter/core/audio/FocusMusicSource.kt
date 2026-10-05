package com.sleepbetter.core.audio

import kotlin.math.exp

/**
 * Generative focus music: a slow pad and a soft piano-like arpeggio over a
 * four-chord loop. Low tempo, no vocals, no sudden changes. Each beat bumps
 * [eventCount] so the UI can pulse on the real beat.
 */
class FocusMusicSource(private val sampleRate: Int, seed: Long, val bpm: Float = 72f) : SoundSource, EventfulSource {
    private val rng = Rng(seed)

    // Cmaj7, Am7, Fmaj7, G6, voiced around middle C.
    private val chords = arrayOf(
        intArrayOf(48, 55, 59, 64),
        intArrayOf(45, 52, 55, 60),
        intArrayOf(41, 48, 52, 57),
        intArrayOf(43, 50, 52, 59),
    )
    private val samplesPerBeat = (60f / bpm * sampleRate).toInt()
    private val beatsPerChord = 8
    private var sampleInBeat = 0
    private var beat = 0

    private val padPhases = FloatArray(8)
    private val padFreqs = FloatArray(8)
    private val padLevel = SmoothedValue(0f)

    private val pluckPhases = FloatArray(3)
    private var pluckFreq = 0f
    private var pluckEnv = 0f
    private val pluckDecay = exp(-1.0 / (1.1 * sampleRate)).toFloat()
    private val warmth = OnePoleLowPass(sampleRate, 2400f)

    @Volatile
    override var eventCount: Int = 0
        private set

    /** 0 at the start of a beat, rising to 1 just before the next. */
    @Volatile
    var beatPhase: Float = 0f
        private set

    init {
        setChord(0)
        padLevel.setTarget(1f, 2 * sampleRate)
    }

    private fun setChord(index: Int) {
        val chord = chords[index % chords.size]
        for (i in chord.indices) {
            val f = midiToHz(chord[i])
            padFreqs[i * 2] = f
            padFreqs[i * 2 + 1] = f * 1.003f // gentle detune for width
        }
    }

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            if (sampleInBeat == 0) onBeat()
            var pad = 0f
            for (v in padPhases.indices) {
                padPhases[v] = wrapPhase(padPhases[v] + padFreqs[v] / sampleRate)
                pad += SineTable.at(padPhases[v])
            }
            pad *= 0.022f * padLevel.next()

            var pluck = 0f
            if (pluckEnv > 0.0005f) {
                for (h in pluckPhases.indices) {
                    pluckPhases[h] = wrapPhase(pluckPhases[h] + pluckFreq * (h + 1) / sampleRate)
                    pluck += SineTable.at(pluckPhases[h]) / (h + 1)
                }
                pluck *= pluckEnv * 0.09f
                pluckEnv *= pluckDecay
            }
            out[i] = warmth.process(pad + pluck)
            sampleInBeat++
            if (sampleInBeat >= samplesPerBeat) sampleInBeat = 0
        }
        beatPhase = sampleInBeat.toFloat() / samplesPerBeat
    }

    private fun onBeat() {
        if (beat % beatsPerChord == 0) setChord(beat / beatsPerChord)
        val chord = chords[(beat / beatsPerChord) % chords.size]
        // Skip some beats so the melody breathes.
        if (beat % 2 == 0 || rng.chance(0.35f)) {
            val note = chord[(beat + rng.range(0f, 3.99f).toInt()) % chord.size] + 12
            pluckFreq = midiToHz(note)
            pluckEnv = rng.range(0.6f, 1f)
            pluckPhases.fill(0f)
        }
        beat++
        eventCount++
    }
}
