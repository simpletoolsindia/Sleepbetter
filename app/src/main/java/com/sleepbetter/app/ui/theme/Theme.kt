package com.sleepbetter.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sleepbetter.app.R
import com.sleepbetter.core.audio.SoundId
import com.sleepbetter.core.sleep.RiskLevel

/** The colours that change with the theme. Status colours (sage, butter, peach) never change, so levels always read the same. */
data class ThemeColors(
    val paper: Color,
    val card: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkMuted: Color,
    val line: Color,
    val accent: Color,
    val accentDeep: Color,
    val sky: Color,
    val rose: Color,
    val duskTop: Color,
    val duskMid: Color,
    val duskLow: Color,
    val hillFar: Color,
    val hillMid: Color,
    val hillNear: Color,
    val night: Color,
)

/**
 * Colour themes, picked from 2026 colour research (Pinterest Palette 2026:
 * Cool Blue, Jade, Plum Noir, Persimmon; Pantone's Cloud Dancer white) and
 * sleep research (soft blues and greens calm; warm amber light disturbs
 * melatonin least at night).
 */
enum class AppTheme(val label: String, val blurb: String, val colors: ThemeColors) {
    MOON_MILK(
        "Moon milk", "Soft lavender, the classic",
        ThemeColors(
            paper = Color(0xFFF5F2EE), card = Color.White, ink = Color(0xFF2B2238), inkSoft = Color(0xFF5E5470), inkMuted = Color(0xFF8A8197), line = Color(0xFFE6E0EA),
            accent = Color(0xFFC9BCF7), accentDeep = Color(0xFF6C55D9), sky = Color(0xFFBFDDF7), rose = Color(0xFFF6C3D6),
            duskTop = Color(0xFF241B57), duskMid = Color(0xFF5B45C2), duskLow = Color(0xFFE8A5C8),
            hillFar = Color(0xFF8A70E0), hillMid = Color(0xFF5A44B8), hillNear = Color(0xFF2E2373), night = Color(0xFF15102F),
        ),
    ),
    JADE(
        "Jade mist", "Calm greens, mint to moss",
        ThemeColors(
            paper = Color(0xFFF0F4EF), card = Color.White, ink = Color(0xFF1C2B27), inkSoft = Color(0xFF4A5C56), inkMuted = Color(0xFF7D8D87), line = Color(0xFFDCE6E0),
            accent = Color(0xFFADDCC8), accentDeep = Color(0xFF1F7A5C), sky = Color(0xFFC6E4E6), rose = Color(0xFFF3D6C8),
            duskTop = Color(0xFF0D2A2E), duskMid = Color(0xFF1E6A64), duskLow = Color(0xFFA9D9C0),
            hillFar = Color(0xFF4FA48C), hillMid = Color(0xFF26705F), hillNear = Color(0xFF103D38), night = Color(0xFF07181A),
        ),
    ),
    COOL_BLUE(
        "Cool blue", "Clear sky on cloud white",
        ThemeColors(
            paper = Color(0xFFF0EEE9), card = Color.White, ink = Color(0xFF1B2540), inkSoft = Color(0xFF4B5672), inkMuted = Color(0xFF7F889E), line = Color(0xFFDDE2EA),
            accent = Color(0xFFB9D2F5), accentDeep = Color(0xFF2D5BD0), sky = Color(0xFFC4E6F2), rose = Color(0xFFF2CFDA),
            duskTop = Color(0xFF0A1C44), duskMid = Color(0xFF2B5BB5), duskLow = Color(0xFF9FCBEF),
            hillFar = Color(0xFF5E8EDB), hillMid = Color(0xFF2F5AAE), hillNear = Color(0xFF14295E), night = Color(0xFF06112A),
        ),
    ),
    PLUM_NOIR(
        "Plum noir", "Velvet plum and berry",
        ThemeColors(
            paper = Color(0xFFF6EFEE), card = Color.White, ink = Color(0xFF2E1A26), inkSoft = Color(0xFF634858), inkMuted = Color(0xFF94808B), line = Color(0xFFEADDE2),
            accent = Color(0xFFE5C3D7), accentDeep = Color(0xFF8A2D62), sky = Color(0xFFD8D4F2), rose = Color(0xFFF6C9C0),
            duskTop = Color(0xFF2A0F22), duskMid = Color(0xFF6E2A55), duskLow = Color(0xFFEBA996),
            hillFar = Color(0xFFA25683), hillMid = Color(0xFF6E2C58), hillNear = Color(0xFF3A1230), night = Color(0xFF170812),
        ),
    ),
    AMBER(
        "Amber hour", "Warm candlelight for late nights",
        ThemeColors(
            paper = Color(0xFFF8F1E7), card = Color.White, ink = Color(0xFF33231A), inkSoft = Color(0xFF6A5346), inkMuted = Color(0xFF9A8578), line = Color(0xFFEDE0D2),
            accent = Color(0xFFF7D2A8), accentDeep = Color(0xFFA6541A), sky = Color(0xFFF1DCC3), rose = Color(0xFFF5C4B4),
            duskTop = Color(0xFF2A1408), duskMid = Color(0xFF8C3F17), duskLow = Color(0xFFF3AE62),
            hillFar = Color(0xFFC0703A), hillMid = Color(0xFF85401C), hillNear = Color(0xFF45200D), night = Color(0xFF170B04),
        ),
    ),
}

/**
 * Paper, ink and pastel blocks that each mean one thing: accent = sleep,
 * butter = moon/bedtime, sage = good, peach = needs care, sky = sounds.
 * Reads follow [theme], so changing it recolours the whole app at once.
 */
object Palette {
    var theme by mutableStateOf(AppTheme.MOON_MILK)

    /** Light, dark, or follow the phone. */
    var appearance by mutableStateOf(Appearance.AUTO)

    /**
     * 0 = light, 1 = dark, animated in between, so switching modes morphs
     * every colour in the app smoothly instead of snapping.
     */
    var darkness by mutableFloatStateOf(0f)

    private val c get() = theme.colors
    private val d get() = theme.dark

    /** Blends a light colour towards its dark partner by [darkness]. */
    private fun m(light: Color, dark: Color): Color = when (val k = darkness) {
        0f -> light
        1f -> dark
        else -> lerp(light, dark, k)
    }

    val Paper get() = m(c.paper, d.paper)
    val Card get() = m(c.card, d.card)
    val Ink get() = m(c.ink, d.ink)
    val InkSoft get() = m(c.inkSoft, d.inkSoft)
    val InkMuted get() = m(c.inkMuted, d.inkMuted)
    val Line get() = m(c.line, d.line)

    /** Text and icons on an [Ink] background. */
    val OnInk get() = m(Color.White, Color(0xFF1A1626))

    /** Always dark: text on the moon-yellow buttons and the white buttons over the night scene. */
    val DarkInk = Color(0xFF2B2238)

    val Accent get() = m(c.accent, d.accent)
    val AccentDeep get() = m(c.accentDeep, d.accentDeep)
    val Butter get() = m(BUTTER, darkTone(BUTTER))
    val Sage get() = m(SAGE, darkTone(SAGE))
    val SageDeep get() = m(Color(0xFF4F7A3A), Color(0xFFA6D98C))
    val Peach get() = m(PEACH, darkTone(PEACH))
    val PeachDeep get() = m(Color(0xFFB4502A), Color(0xFFFFA684))
    val Sky get() = m(c.sky, d.sky)
    val Rose get() = m(c.rose, d.rose)

    /** A soft translucent layer for tracks and badges on coloured cards: white by day, a faint glow at night. */
    fun veil(alpha: Float): Color = Color.White.copy(alpha = alpha * (1f - 0.75f * darkness))

    /** A pastel tile colour (sounds, moods) adjusted for the current mode. */
    fun tile(pastel: Color): Color = m(pastel, darkTone(pastel))

    /** The text-safe partner of a tile colour, lifted for dark mode. */
    fun onTile(deep: Color): Color = m(deep, lerp(deep, Color.White, 0.55f))

    // Dusk scene (bedtime): already a night palette, the same in both modes.
    val DuskTop get() = c.duskTop
    val DuskMid get() = c.duskMid
    val DuskLow get() = c.duskLow
    val HillFar get() = c.hillFar
    val HillMid get() = c.hillMid
    val HillNear get() = c.hillNear
    val Night get() = c.night
    val Moon = Color(0xFFFFE6A3)

    private val BUTTER = Color(0xFFFFE08A)
    private val SAGE = Color(0xFFBFD8A9)
    private val PEACH = Color(0xFFFFC2A6)

    /** A pastel turned into a deep, muted tone that sits calmly on a dark page. */
    private fun darkTone(pastel: Color) = lerp(theme.dark.paper, pastel, 0.3f)
}

/** Light, dark, or follow the phone's setting. */
enum class Appearance(val label: String, val emoji: String) {
    AUTO("Auto", "🌗"),
    LIGHT("Light", "☀️"),
    DARK("Dark", "🌙"),
}

/** A calm dark palette made from a light theme: deep night paper tinted by the theme's own dusk, light ink, muted pastels. */
private fun darkOf(l: ThemeColors): ThemeColors {
    val paper = lerp(Color(0xFF0F0D17), l.duskTop, 0.38f)
    fun tone(pastel: Color) = lerp(paper, pastel, 0.3f)
    return l.copy(
        paper = paper,
        card = lerp(paper, Color.White, 0.07f),
        ink = Color(0xFFF3F0FA),
        inkSoft = Color(0xFFCBC5DA),
        inkMuted = Color(0xFF9C95AF),
        line = lerp(paper, Color.White, 0.14f),
        accent = tone(l.accent),
        accentDeep = lerp(l.accentDeep, Color.White, 0.5f),
        sky = tone(l.sky),
        rose = tone(l.rose),
    )
}

private val darkCache = HashMap<AppTheme, ThemeColors>()

/** This theme's dark palette. */
val AppTheme.dark: ThemeColors get() = darkCache.getOrPut(this) { darkOf(colors) }

fun SoundId.tint(): Color = Palette.tile(baseTint())

private fun SoundId.baseTint(): Color = when (this) {
    SoundId.RAIN, SoundId.DOWNPOUR -> Color(0xFFBFD3FA)
    SoundId.THUNDER -> Color(0xFFD3C8FA)
    SoundId.TENT -> Color(0xFFFFD1B8)
    SoundId.CAR -> Color(0xFFF8C6D3)
    SoundId.CAMPFIRE -> Color(0xFFFFD98F)
    SoundId.NIGHT_FOREST -> Color(0xFFBFE3CF)
    SoundId.BIRDS -> Color(0xFFDDEBB3)
    SoundId.WATER_DROPS, SoundId.STREAM -> Color(0xFFBDE6EE)
    SoundId.BROWN_NOISE -> Color(0xFFE5D6C6)
    SoundId.FOCUS_MUSIC -> Color(0xFFF4C7E8)
    SoundId.SEA -> Color(0xFFB3E6EE)
}

/** Text-safe partner of [tint] for icons on the tint (lighter in dark mode). */
fun SoundId.deep(): Color = Palette.onTile(baseDeep())

private fun SoundId.baseDeep(): Color = when (this) {
    SoundId.RAIN, SoundId.DOWNPOUR -> Color(0xFF2F5BB8)
    SoundId.THUNDER -> Color(0xFF5B45C2)
    SoundId.TENT -> Color(0xFFA34B1F)
    SoundId.CAR -> Color(0xFFA3365A)
    SoundId.CAMPFIRE -> Color(0xFF8A5A00)
    SoundId.NIGHT_FOREST -> Color(0xFF23704B)
    SoundId.BIRDS -> Color(0xFF4F6B12)
    SoundId.WATER_DROPS, SoundId.STREAM -> Color(0xFF16687A)
    SoundId.BROWN_NOISE -> Color(0xFF6B4E33)
    SoundId.FOCUS_MUSIC -> Color(0xFF9A2F83)
    SoundId.SEA -> Color(0xFF0F6E80)
}

/** Level colours always come with an icon and a word, never colour alone. */
fun RiskLevel.tint(): Color = when (this) {
    RiskLevel.GOOD -> Palette.Sage
    RiskLevel.MEDIUM -> Palette.Butter
    RiskLevel.AT_RISK -> Palette.Peach
}

fun RiskLevel.deep(): Color = when (this) {
    RiskLevel.GOOD -> Palette.SageDeep
    RiskLevel.MEDIUM -> Palette.onTile(Color(0xFF7A5A00))
    RiskLevel.AT_RISK -> Palette.PeachDeep
}

@OptIn(ExperimentalTextApi::class) // weights from the variable font
private fun outfit(weight: Int) = Font(
    R.font.outfit,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Outfit: one geometric family, friendly at every size. */
val Outfit = FontFamily(outfit(400), outfit(500), outfit(600), outfit(700), outfit(800))

object Type {
    /** The big numbers: hours slept, score, countdowns. */
    val Numeral = TextStyle(fontFamily = Outfit, fontWeight = FontWeight(700), fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = (-2).sp)
    val Display = TextStyle(fontFamily = Outfit, fontWeight = FontWeight(700), fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp)
    val Title = TextStyle(fontFamily = Outfit, fontWeight = FontWeight(600), fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp)
    val Heading = TextStyle(fontFamily = Outfit, fontWeight = FontWeight(600), fontSize = 17.sp, lineHeight = 22.sp)
    val Body = TextStyle(fontFamily = Outfit, fontWeight = FontWeight(400), fontSize = 15.sp, lineHeight = 21.sp)
    val Label = TextStyle(fontFamily = Outfit, fontWeight = FontWeight(600), fontSize = 14.sp, lineHeight = 18.sp)
    val Small = TextStyle(fontFamily = Outfit, fontWeight = FontWeight(500), fontSize = 12.sp, lineHeight = 16.sp)
}

/**
 * Applies the colour theme and light/dark mode. Switching modes animates
 * [Palette.darkness], so every colour morphs across in a moment.
 */
@Composable
fun SleepBetterTheme(content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (Palette.appearance) {
        Appearance.AUTO -> systemDark
        Appearance.LIGHT -> false
        Appearance.DARK -> true
    }
    val first = remember { mutableStateOf(true) }
    LaunchedEffect(dark) {
        val target = if (dark) 1f else 0f
        if (first.value) {
            Palette.darkness = target
            first.value = false
        } else {
            androidx.compose.animation.core.animate(Palette.darkness, target, animationSpec = tween(520, easing = FastOutSlowInEasing)) { v, _ -> Palette.darkness = v }
        }
    }
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = scheme.copy(
            primary = Palette.AccentDeep,
            onPrimary = Palette.OnInk,
            background = Palette.Paper,
            onBackground = Palette.Ink,
            surface = Palette.Card,
            onSurface = Palette.Ink,
            surfaceContainerLow = Palette.Card,
            surfaceContainerHigh = Palette.Card,
        ),
        content = content,
    )
}
