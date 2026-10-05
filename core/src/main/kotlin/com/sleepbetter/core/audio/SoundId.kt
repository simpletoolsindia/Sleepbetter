package com.sleepbetter.core.audio

/**
 * Every sound the mixer offers. [stageX]/[stageY] is the default spot on the
 * Sound Stage (unit disk, listener at the centre, negative y is "far").
 * [color] is the sound's identity colour (ARGB) in the UI.
 */
enum class SoundId(
    val label: String,
    val color: Long,
    val stageX: Float,
    val stageY: Float,
    /**
     * Loudness calibration so every sound sits at a similar level (measured raw
     * RMS over two minutes, scaled to about -18 dBFS for steady beds and about
     * -21 dBFS for sparse sounds). SoundCalibrationTest keeps these honest.
     */
    val gain: Float,
) {
    RAIN("Rain", 0xFF5B8CFF, 0.02f, -0.35f, 2.54f),
    DOWNPOUR("Downpour", 0xFF3E6BFF, 0f, -0.5f, 1.41f),
    THUNDER("Thunder", 0xFF9B6BFF, 0.5f, -0.45f, 2.89f),
    TENT("Tent", 0xFFFF9F6B, -0.4f, 0.2f, 2.83f),
    CAR("Car", 0xFFFF6B8B, 0.4f, 0.3f, 2.76f),
    CAMPFIRE("Campfire", 0xFFFFB547, 0.15f, 0.4f, 2.65f),
    NIGHT_FOREST("Night forest", 0xFF3FD3A0, -0.5f, -0.35f, 3.99f),
    BIRDS("Birds", 0xFFA6E35D, -0.2f, -0.55f, 4.03f),
    WATER_DROPS("Water drops", 0xFF4FD1E8, -0.5f, 0.4f, 2.62f),
    STREAM("Stream", 0xFF6BB8FF, 0.55f, 0.1f, 2.48f),
    BROWN_NOISE("Brown noise", 0xFFC9B39A, 0f, 0.55f, 1.26f),
    FOCUS_MUSIC("Focus music", 0xFFFF7AD9, -0.5f, 0f, 1.99f),
    ;

    fun createSource(sampleRate: Int, seed: Long): SoundSource = when (this) {
        RAIN -> RainSource(sampleRate, seed, RainStyle.GENTLE)
        DOWNPOUR -> RainSource(sampleRate, seed, RainStyle.DOWNPOUR)
        THUNDER -> ThunderSource(sampleRate, seed)
        TENT -> RainSource(sampleRate, seed, RainStyle.TENT)
        CAR -> RainSource(sampleRate, seed, RainStyle.CAR)
        CAMPFIRE -> CampfireSource(sampleRate, seed)
        NIGHT_FOREST -> NightForestSource(sampleRate, seed)
        BIRDS -> BirdSource(sampleRate, seed)
        WATER_DROPS -> WaterDropSource(sampleRate, seed)
        STREAM -> StreamSource(sampleRate, seed)
        BROWN_NOISE -> NoiseSource(seed, NoiseColor.BROWN)
        FOCUS_MUSIC -> FocusMusicSource(sampleRate, seed)
    }
}

/** Ready-made mixes, matching the plan's presets. */
enum class Preset(val label: String, val sounds: Set<SoundId>) {
    COZY_TENT("Cozy tent", setOf(SoundId.TENT, SoundId.THUNDER, SoundId.NIGHT_FOREST)),
    DEEP_FOCUS("Deep focus", setOf(SoundId.RAIN, SoundId.BROWN_NOISE)),
    ROAD_TRIP_NAP("Road-trip nap", setOf(SoundId.CAR, SoundId.BROWN_NOISE)),
    CAMPFIRE_READING("Campfire reading", setOf(SoundId.CAMPFIRE, SoundId.NIGHT_FOREST, SoundId.RAIN)),
    STORM_SLEEPER("Storm sleeper", setOf(SoundId.DOWNPOUR, SoundId.THUNDER)),
    MORNING_STUDY("Morning study", setOf(SoundId.BIRDS, SoundId.RAIN, SoundId.FOCUS_MUSIC)),
    CAVE_CALM("Cave calm", setOf(SoundId.WATER_DROPS, SoundId.BROWN_NOISE)),
}
