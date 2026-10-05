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
    /** Stable two-letter code used in shared mix links. Never change these. */
    val code: String,
) {
    RAIN("Rain", 0xFF5B8CFF, 0.02f, -0.35f, 2.54f, "RN"),
    DOWNPOUR("Downpour", 0xFF3E6BFF, 0f, -0.5f, 1.41f, "DP"),
    THUNDER("Thunder", 0xFF9B6BFF, 0.5f, -0.45f, 2.89f, "TH"),
    TENT("Tent", 0xFFFF9F6B, -0.4f, 0.2f, 2.83f, "TE"),
    CAR("Car", 0xFFFF6B8B, 0.4f, 0.3f, 2.76f, "CA"),
    CAMPFIRE("Campfire", 0xFFFFB547, 0.15f, 0.4f, 2.65f, "CF"),
    NIGHT_FOREST("Night forest", 0xFF3FD3A0, -0.5f, -0.35f, 3.99f, "NF"),
    BIRDS("Birds", 0xFFA6E35D, -0.2f, -0.55f, 4.03f, "BI"),
    WATER_DROPS("Water drops", 0xFF4FD1E8, -0.5f, 0.4f, 2.62f, "WD"),
    STREAM("Stream", 0xFF6BB8FF, 0.55f, 0.1f, 2.48f, "ST"),
    BROWN_NOISE("Brown noise", 0xFFC9B39A, 0f, 0.55f, 1.26f, "BN"),
    FOCUS_MUSIC("Focus music", 0xFFFF7AD9, -0.5f, 0f, 1.99f, "FM"),
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
