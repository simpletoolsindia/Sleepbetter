@file:Suppress("DEPRECATION") // quadraticBezierTo: renamed quadraticTo in newer Compose.

package com.sleepbetter.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sleepbetter.core.audio.SoundId

/** A small set of rounded line icons, drawn on a 24-unit grid. */
enum class Glyph {
    HOME, SOUNDS, MOON, CHART, FRIENDS, PLAY, PAUSE, BACK, FOCUS, BREATH, BELL, PLUS, MINUS, CHECK, SPARK, CLOCK,
    RAIN, DOWNPOUR, THUNDER, TENT, CAR, FIRE, FOREST, BIRDS, DROP, STREAM, NOISE, MUSIC,
}

fun SoundId.glyph(): Glyph = when (this) {
    SoundId.RAIN -> Glyph.RAIN
    SoundId.DOWNPOUR -> Glyph.DOWNPOUR
    SoundId.THUNDER -> Glyph.THUNDER
    SoundId.TENT -> Glyph.TENT
    SoundId.CAR -> Glyph.CAR
    SoundId.CAMPFIRE -> Glyph.FIRE
    SoundId.NIGHT_FOREST -> Glyph.FOREST
    SoundId.BIRDS -> Glyph.BIRDS
    SoundId.WATER_DROPS -> Glyph.DROP
    SoundId.STREAM -> Glyph.STREAM
    SoundId.BROWN_NOISE -> Glyph.NOISE
    SoundId.FOCUS_MUSIC -> Glyph.MUSIC
}

@Composable
fun GlyphIcon(glyph: Glyph, tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp, strokeWidth: Float = 1.9f) {
    Canvas(modifier.size(size)) {
        scale(this.size.width / 24f, pivot = Offset.Zero) { drawGlyph(glyph, tint, strokeWidth) }
    }
}

private fun p(build: Path.() -> Unit) = Path().apply(build)

private fun DrawScope.drawGlyph(g: Glyph, c: Color, w: Float) {
    val s = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun line(path: Path) = drawPath(path, c, style = s)
    fun fill(path: Path) = drawPath(path, c)
    val cloud = p { moveTo(7f, 14f); quadraticBezierTo(3f, 14f, 3.5f, 10.5f); quadraticBezierTo(4f, 7.5f, 7.5f, 8f); quadraticBezierTo(9f, 4f, 13f, 4.5f); quadraticBezierTo(17f, 5f, 17.5f, 8.5f); quadraticBezierTo(21f, 9f, 20.5f, 12f); quadraticBezierTo(20f, 14f, 17f, 14f); close() }
    when (g) {
        Glyph.HOME -> line(p { moveTo(4f, 11f); lineTo(12f, 4.5f); lineTo(20f, 11f); moveTo(6f, 9.5f); lineTo(6f, 19f); quadraticBezierTo(6f, 20f, 7f, 20f); lineTo(17f, 20f); quadraticBezierTo(18f, 20f, 18f, 19f); lineTo(18f, 9.5f); moveTo(10f, 20f); lineTo(10f, 15f); lineTo(14f, 15f); lineTo(14f, 20f) })
        Glyph.SOUNDS -> line(p { moveTo(4f, 10f); lineTo(4f, 14f); moveTo(8f, 7f); lineTo(8f, 17f); moveTo(12f, 4f); lineTo(12f, 20f); moveTo(16f, 8f); lineTo(16f, 16f); moveTo(20f, 11f); lineTo(20f, 13f) })
        Glyph.MOON -> line(p { moveTo(19.5f, 14.5f); quadraticBezierTo(17.5f, 20f, 12f, 20f); quadraticBezierTo(4.5f, 20f, 4f, 12.5f); quadraticBezierTo(4f, 6f, 10f, 4f); quadraticBezierTo(8f, 10f, 11.5f, 13f); quadraticBezierTo(15f, 16f, 19.5f, 14.5f); close() })
        Glyph.CHART -> line(p { moveTo(5f, 20f); lineTo(5f, 13f); moveTo(10f, 20f); lineTo(10f, 7f); moveTo(15f, 20f); lineTo(15f, 11f); moveTo(20f, 20f); lineTo(20f, 4f) })
        Glyph.FRIENDS -> {
            line(p { addOval(Rect(Offset(9f, 9f), 5f)) })
            line(p { moveTo(3f, 20f); quadraticBezierTo(9f, 13f, 15f, 20f) })
            line(p { moveTo(15.5f, 5f); quadraticBezierTo(21f, 6f, 19f, 11f); moveTo(17f, 14f); quadraticBezierTo(21f, 15f, 21f, 19f) })
        }
        Glyph.PLAY -> fill(p { moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close() })
        Glyph.PAUSE -> {
            drawRoundRect(c, Offset(6.5f, 5f), Size(4f, 14f), androidx.compose.ui.geometry.CornerRadius(1.5f))
            drawRoundRect(c, Offset(13.5f, 5f), Size(4f, 14f), androidx.compose.ui.geometry.CornerRadius(1.5f))
        }
        Glyph.BACK -> line(p { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) })
        Glyph.FOCUS -> {
            line(p { moveTo(4f, 14f); lineTo(4f, 12f); quadraticBezierTo(4f, 4f, 12f, 4f); quadraticBezierTo(20f, 4f, 20f, 12f); lineTo(20f, 14f) })
            drawRoundRect(c, Offset(3f, 13f), Size(5f, 7f), androidx.compose.ui.geometry.CornerRadius(2f))
            drawRoundRect(c, Offset(16f, 13f), Size(5f, 7f), androidx.compose.ui.geometry.CornerRadius(2f))
        }
        Glyph.BREATH -> {
            line(p { addOval(Rect(Offset(12f, 12f), 8f)) })
            line(p { addOval(Rect(Offset(12f, 12f), 3.5f)) })
        }
        Glyph.BELL -> line(p { moveTo(6f, 16f); lineTo(6f, 11f); quadraticBezierTo(6f, 5f, 12f, 5f); quadraticBezierTo(18f, 5f, 18f, 11f); lineTo(18f, 16f); lineTo(19.5f, 17.5f); lineTo(4.5f, 17.5f); close(); moveTo(10f, 20f); quadraticBezierTo(12f, 21.5f, 14f, 20f) })
        Glyph.PLUS -> line(p { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) })
        Glyph.MINUS -> line(p { moveTo(5f, 12f); lineTo(19f, 12f) })
        Glyph.CHECK -> line(p { moveTo(5f, 12.5f); lineTo(10f, 17f); lineTo(19f, 7f) })
        Glyph.SPARK -> fill(p { moveTo(12f, 3f); quadraticBezierTo(13f, 11f, 21f, 12f); quadraticBezierTo(13f, 13f, 12f, 21f); quadraticBezierTo(11f, 13f, 3f, 12f); quadraticBezierTo(11f, 11f, 12f, 3f); close() })
        Glyph.CLOCK -> {
            line(p { addOval(Rect(Offset(12f, 12f), 8.5f)) })
            line(p { moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15f, 14f) })
        }
        Glyph.RAIN -> { line(cloud); line(p { moveTo(8f, 17f); lineTo(7f, 20f); moveTo(12f, 17f); lineTo(11f, 20f); moveTo(16f, 17f); lineTo(15f, 20f) }) }
        Glyph.DOWNPOUR -> { line(cloud); line(p { moveTo(7f, 16.5f); lineTo(5.5f, 21f); moveTo(11f, 16.5f); lineTo(9.5f, 21f); moveTo(15f, 16.5f); lineTo(13.5f, 21f); moveTo(19f, 16.5f); lineTo(17.5f, 21f) }) }
        Glyph.THUNDER -> { line(cloud); line(p { moveTo(13f, 14.5f); lineTo(10.5f, 18f); lineTo(13.5f, 18f); lineTo(11f, 21.5f) }) }
        Glyph.TENT -> line(p { moveTo(3f, 20f); lineTo(12f, 5f); lineTo(21f, 20f); close(); moveTo(9.5f, 20f); lineTo(12f, 15f); lineTo(14.5f, 20f) })
        Glyph.CAR -> {
            line(p { moveTo(4f, 16f); lineTo(4f, 13f); lineTo(6.5f, 8f); quadraticBezierTo(7f, 7f, 8f, 7f); lineTo(16f, 7f); quadraticBezierTo(17f, 7f, 17.5f, 8f); lineTo(20f, 13f); lineTo(20f, 16f); close(); moveTo(4f, 13f); lineTo(20f, 13f) })
            line(p { addOval(Rect(Offset(8f, 17f), 1.8f)); addOval(Rect(Offset(16f, 17f), 1.8f)) })
        }
        Glyph.FIRE -> line(p { moveTo(12f, 3.5f); quadraticBezierTo(18f, 9f, 17f, 14f); quadraticBezierTo(16f, 19f, 12f, 19f); quadraticBezierTo(7f, 19f, 7f, 14f); quadraticBezierTo(7f, 11f, 9.5f, 9.5f); quadraticBezierTo(10f, 12f, 11.5f, 12f); quadraticBezierTo(10.5f, 7f, 12f, 3.5f); close(); moveTo(5f, 21f); lineTo(19f, 19f); moveTo(19f, 21f); lineTo(5f, 19f) })
        Glyph.FOREST -> line(p { moveTo(8f, 4f); lineTo(3.5f, 12f); lineTo(6f, 12f); lineTo(3.5f, 17f); lineTo(12.5f, 17f); lineTo(10f, 12f); lineTo(12.5f, 12f); close(); moveTo(8f, 17f); lineTo(8f, 20f); moveTo(20f, 8f); quadraticBezierTo(16f, 8.5f, 16f, 5f); quadraticBezierTo(16f, 3.5f, 17f, 3f); quadraticBezierTo(13.5f, 3.5f, 13.5f, 6.5f); quadraticBezierTo(13.5f, 10f, 17f, 10f); quadraticBezierTo(19f, 10f, 20f, 8f); close() })
        Glyph.BIRDS -> line(p { moveTo(3f, 10f); quadraticBezierTo(5.5f, 7f, 8.5f, 10f); quadraticBezierTo(11f, 7f, 14f, 10f); moveTo(11f, 16f); quadraticBezierTo(13.5f, 13f, 16f, 16f); quadraticBezierTo(18.5f, 13f, 21f, 16f) })
        Glyph.DROP -> line(p { moveTo(12f, 3.5f); quadraticBezierTo(18.5f, 11f, 18f, 14.5f); quadraticBezierTo(17.5f, 20f, 12f, 20f); quadraticBezierTo(6.5f, 20f, 6f, 14.5f); quadraticBezierTo(5.5f, 11f, 12f, 3.5f); close() })
        Glyph.STREAM -> line(p {
            for (row in 0..2) {
                val y = 8f + row * 5f
                moveTo(3f, y); quadraticBezierTo(5.25f, y - 2f, 7.5f, y); quadraticBezierTo(9.75f, y + 2f, 12f, y); quadraticBezierTo(14.25f, y - 2f, 16.5f, y); quadraticBezierTo(18.75f, y + 2f, 21f, y)
            }
        })
        Glyph.NOISE -> line(p { moveTo(2.5f, 12f); quadraticBezierTo(4.5f, 4f, 6.5f, 12f); quadraticBezierTo(8.5f, 20f, 10.5f, 12f); quadraticBezierTo(12.5f, 4f, 14.5f, 12f); quadraticBezierTo(16.5f, 20f, 18.5f, 12f); quadraticBezierTo(20f, 7f, 21.5f, 12f) })
        Glyph.MUSIC -> {
            line(p { moveTo(9f, 17f); lineTo(9f, 5.5f); lineTo(19f, 3.5f); lineTo(19f, 15f) })
            fill(p { addOval(Rect(Offset(6.5f, 17f), 2.8f)); addOval(Rect(Offset(16.5f, 15f), 2.8f)) })
        }
    }
}
