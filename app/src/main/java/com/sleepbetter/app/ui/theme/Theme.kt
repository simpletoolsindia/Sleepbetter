package com.sleepbetter.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

/**
 * "Moon milk" by day, dusk at bedtime. Soft paper, deep plum ink, and pastel
 * blocks that each mean one thing: lavender = sleep, butter = moon/bedtime,
 * sage = good, peach = needs care, sky = sounds.
 */
object Palette {
    val Paper = Color(0xFFF5F2EE)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF2B2238)
    val InkSoft = Color(0xFF5E5470)
    val InkMuted = Color(0xFF8A8197)
    val Line = Color(0xFFE6E0EA)

    val Lavender = Color(0xFFC9BCF7)
    val LavenderDeep = Color(0xFF6C55D9)
    val Butter = Color(0xFFFFE08A)
    val Sage = Color(0xFFBFD8A9)
    val SageDeep = Color(0xFF4F7A3A)
    val Peach = Color(0xFFFFC2A6)
    val PeachDeep = Color(0xFFB4502A)
    val Sky = Color(0xFFBFDDF7)
    val Rose = Color(0xFFF6C3D6)

    // Dusk scene (bedtime).
    val DuskTop = Color(0xFF241B57)
    val DuskMid = Color(0xFF5B45C2)
    val DuskLow = Color(0xFFE8A5C8)
    val Night = Color(0xFF15102F)
    val Moon = Color(0xFFFFE6A3)
}

fun SoundId.tint(): Color = when (this) {
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
}

/** Text-safe darker partner of [tint] for icons on the tint. */
fun SoundId.deep(): Color = when (this) {
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
}

/** Level colours always come with an icon and a word, never colour alone. */
fun RiskLevel.tint(): Color = when (this) {
    RiskLevel.GOOD -> Palette.Sage
    RiskLevel.MEDIUM -> Palette.Butter
    RiskLevel.AT_RISK -> Palette.Peach
}

fun RiskLevel.deep(): Color = when (this) {
    RiskLevel.GOOD -> Palette.SageDeep
    RiskLevel.MEDIUM -> Color(0xFF7A5A00)
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

@Composable
fun SleepBetterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.LavenderDeep,
            onPrimary = Color.White,
            background = Palette.Paper,
            onBackground = Palette.Ink,
            surface = Palette.Card,
            onSurface = Palette.Ink,
        ),
        content = content,
    )
}
