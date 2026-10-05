package com.sleepbetter.core.mix

import com.sleepbetter.core.audio.SoundId
import com.sleepbetter.core.audio.Spatial

/** One sound in a mix, placed on the Sound Stage (unit disk; nearer the centre is louder). */
data class MixLayer(val sound: SoundId, val x: Float = sound.stageX, val y: Float = sound.stageY)

/** A named set of sounds with their places. Templates, saved mixes and shared mixes all use this. */
data class Mix(val name: String, val layers: List<MixLayer>) {
    val sounds: Set<SoundId> get() = layers.map { it.sound }.toSet()

    companion object {
        const val MAX_NAME = 40

        /** Builds a mix that is always valid: a clean name, at most one layer per sound, places on the stage. */
        fun of(name: String, layers: List<MixLayer>): Mix {
            val clean = cleanName(name).ifEmpty { "My mix" }
            val unique = layers.distinctBy { it.sound }.map {
                val (x, y) = Spatial.clampToStage(it.x.finiteOr(it.sound.stageX), it.y.finiteOr(it.sound.stageY))
                it.copy(x = x, y = y)
            }
            return Mix(clean, unique)
        }

        fun cleanName(name: String): String =
            name.filter { !it.isISOControl() && it != '|' }.trim().replace(Regex("\\s+"), " ").take(MAX_NAME)

        private fun Float.finiteOr(fallback: Float) = if (isFinite()) this else fallback
    }
}

enum class MixCategory(val label: String, val emoji: String) {
    RAIN("Rain", "🌧️"),
    STORM("Storms", "⛈️"),
    NATURE("Nature", "🌿"),
    COZY("Cozy", "🛋️"),
    FOCUS("Focus", "🎧"),
}

data class MixTemplate(val mix: Mix, val category: MixCategory, val blurb: String, val emoji: String)

/**
 * Ready-made mixes. Placement sets the balance: heavy rain sits close, thunder
 * further back so it rumbles rather than startles.
 */
object MixTemplates {
    private fun t(name: String, emoji: String, category: MixCategory, blurb: String, vararg layers: MixLayer) =
        MixTemplate(Mix.of(name, layers.toList()), category, blurb, emoji)

    private fun l(sound: SoundId, x: Float, y: Float) = MixLayer(sound, x, y)

    val all: List<MixTemplate> = listOf(
        t("Light rain", "🌦️", MixCategory.RAIN, "Soft, steady rain on leaves", l(SoundId.RAIN, 0f, -0.2f)),
        t("Heavy rain", "🌧️", MixCategory.RAIN, "A full downpour, close by", l(SoundId.DOWNPOUR, 0f, -0.2f), l(SoundId.RAIN, -0.4f, -0.4f)),
        t("Rain drops only", "💧", MixCategory.RAIN, "Slow drips, nothing else", l(SoundId.WATER_DROPS, 0f, -0.15f)),
        t("Rain on the tent", "⛺", MixCategory.RAIN, "Taps on fabric overhead", l(SoundId.TENT, 0f, -0.15f), l(SoundId.RAIN, 0.5f, -0.5f)),
        t("Rain on the car", "🚗", MixCategory.RAIN, "Drumming on the roof", l(SoundId.CAR, 0f, -0.1f), l(SoundId.BROWN_NOISE, 0f, 0.6f)),
        t("Heavy rain and thunder", "⛈️", MixCategory.STORM, "Downpour with rolling thunder", l(SoundId.DOWNPOUR, 0f, -0.2f), l(SoundId.THUNDER, 0.45f, -0.5f)),
        t("Thunderstorm", "🌩️", MixCategory.STORM, "The storm overhead", l(SoundId.DOWNPOUR, 0f, -0.1f), l(SoundId.THUNDER, 0.2f, -0.3f), l(SoundId.BROWN_NOISE, 0f, 0.6f)),
        t("Forest rain and thunder", "🌲", MixCategory.STORM, "Rain in the trees, a far-off storm", l(SoundId.RAIN, 0f, -0.25f), l(SoundId.NIGHT_FOREST, -0.5f, -0.3f), l(SoundId.THUNDER, 0.55f, -0.6f)),
        t("Forest and thunder", "🌳", MixCategory.STORM, "Crickets, wind and distant thunder", l(SoundId.NIGHT_FOREST, 0f, -0.2f), l(SoundId.THUNDER, 0.5f, -0.6f)),
        t("Distant storm", "🌫️", MixCategory.STORM, "Thunder far away, nothing else", l(SoundId.THUNDER, 0f, -0.75f), l(SoundId.RAIN, 0f, -0.6f)),
        t("Night forest", "🦉", MixCategory.NATURE, "Crickets, wind and the odd owl", l(SoundId.NIGHT_FOREST, 0f, -0.2f)),
        t("Morning birds", "🐦", MixCategory.NATURE, "Birdsong by a stream", l(SoundId.BIRDS, -0.2f, -0.3f), l(SoundId.STREAM, 0.4f, 0f)),
        t("River", "🏞️", MixCategory.NATURE, "Water over stones", l(SoundId.STREAM, 0f, -0.15f)),
        t("Cave drips", "🪨", MixCategory.NATURE, "Echoing drops in a cave", l(SoundId.WATER_DROPS, 0f, -0.2f), l(SoundId.BROWN_NOISE, 0f, 0.7f)),
        t("Cozy tent", "🏕️", MixCategory.COZY, "Tent, thunder and crickets", l(SoundId.TENT, -0.2f, 0.1f), l(SoundId.THUNDER, 0.5f, -0.5f), l(SoundId.NIGHT_FOREST, -0.5f, -0.35f)),
        t("Campfire night", "🔥", MixCategory.COZY, "A crackling fire under the stars", l(SoundId.CAMPFIRE, 0f, 0.2f), l(SoundId.NIGHT_FOREST, -0.4f, -0.4f)),
        t("Campfire in the rain", "☔", MixCategory.COZY, "Fire under a shelter, rain outside", l(SoundId.CAMPFIRE, 0f, 0.15f), l(SoundId.RAIN, 0f, -0.45f)),
        t("Deep focus", "🧠", MixCategory.FOCUS, "Rain over brown noise", l(SoundId.RAIN, 0f, -0.3f), l(SoundId.BROWN_NOISE, 0f, 0.4f)),
        t("Study music", "🎹", MixCategory.FOCUS, "Soft piano with gentle rain", l(SoundId.FOCUS_MUSIC, 0f, -0.1f), l(SoundId.RAIN, 0.4f, -0.5f)),
        t("Brown noise", "🟤", MixCategory.FOCUS, "Deep, even noise to mask the world", l(SoundId.BROWN_NOISE, 0f, -0.1f)),
    )

    /** A short list for the home screen. */
    val featured: List<MixTemplate> = listOf("Heavy rain and thunder", "Cozy tent", "Forest rain and thunder", "Rain drops only", "Campfire night", "Deep focus")
        .mapNotNull { name -> all.firstOrNull { it.mix.name == name } }
}
