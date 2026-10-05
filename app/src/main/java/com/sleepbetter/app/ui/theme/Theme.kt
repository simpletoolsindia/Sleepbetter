package com.sleepbetter.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

/** "Night Island" meets "Sound Stage": deep night, lantern gold, and glowing sound colours. */
object Palette {
    val Night = Color(0xFF0F1133)
    val SleepBlack = Color(0xFF07081F)
    val Sheet = Color(0xFF1F2350)
    val Ink = Color(0xFFF3F1FF)
    val InkSoft = Color(0xFFD6D4F5)
    val InkMuted = Color(0xFFB3B1D9)
    val Lantern = Color(0xFFF4B860)
    val Moss = Color(0xFF4E8A64)
    val MossLight = Color(0xFF5E9C72)
    val Clay = Color(0xFF3B2D63)
    val Moonlight = Color(0xFFF2E3B8)
    val GlassFill = Color(0x14FFFFFF)
    val GlassEdge = Color(0x29FFFFFF)

    // Status colours: validated for colour-blind separation on Sheet; always shown with an icon and label.
    val Good = Color(0xFF45A87C)
    val Medium = Color(0xFFBF8426)
    val AtRisk = Color(0xFFD6548A)
}

fun SoundId.uiColor(): Color = Color(color)

fun RiskLevel.color(): Color = when (this) {
    RiskLevel.GOOD -> Palette.Good
    RiskLevel.MEDIUM -> Palette.Medium
    RiskLevel.AT_RISK -> Palette.AtRisk
}

/** Bricolage Grotesque (variable, narrowed) for display; Atkinson Hyperlegible for reading with sleepy eyes. */
@OptIn(ExperimentalTextApi::class) // variable-font settings on resource fonts
val Display = FontFamily(
    Font(
        R.font.bricolage_grotesque,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700), FontVariation.width(85f)),
    ),
    Font(
        R.font.bricolage_grotesque,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800), FontVariation.width(80f)),
    ),
    Font(
        R.font.bricolage_grotesque,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(300), FontVariation.width(85f)),
    ),
)

val Body = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

object Type {
    val Hero = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, lineHeight = 44.sp, letterSpacing = (-0.8).sp)
    val Title = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp)
    val Section = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp)
    val Body = TextStyle(fontFamily = com.sleepbetter.app.ui.theme.Body, fontSize = 15.sp, lineHeight = 22.sp)
    val Label = TextStyle(fontFamily = com.sleepbetter.app.ui.theme.Body, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    val Small = TextStyle(fontFamily = com.sleepbetter.app.ui.theme.Body, fontSize = 13.sp, lineHeight = 18.sp)
}

@Composable
fun SleepBetterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Lantern,
            onPrimary = Color(0xFF1B1840),
            background = Palette.Night,
            onBackground = Palette.Ink,
            surface = Palette.Sheet,
            onSurface = Palette.Ink,
            secondary = Palette.MossLight,
        ),
        content = content,
    )
}
