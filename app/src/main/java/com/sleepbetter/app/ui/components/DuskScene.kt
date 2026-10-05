@file:Suppress("DEPRECATION") // quadraticBezierTo: renamed quadraticTo in newer Compose.

package com.sleepbetter.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sleepbetter.app.audio.EngineFrame
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.core.audio.SoundId
import kotlin.math.sin

/**
 * The bedtime landscape: a dusk sky, a crescent moon with Mochi asleep on it,
 * and layered hills. Whatever is playing appears in the scene: rain, a tent, a
 * campfire, fireflies, birds, a waterfall. Lightning flashes exactly when the
 * engine plays a thunder strike. Laid out on a 390-unit-wide grid.
 */
@Composable
fun DuskScene(
    active: Set<SoundId>,
    frame: EngineFrame,
    time: Float,
    modifier: Modifier = Modifier,
    showMochi: Boolean = true,
    still: Boolean = false,
    dim: Float = 0f,
) {
    val appear = SoundId.entries.map { id ->
        animateFloatAsState(if (id in active) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 220f), label = id.name).value
    }
    fun a(id: SoundId) = appear[id.ordinal]
    val initialStrikes = remember { frame.thunderStrikes }
    val flashAt = remember(frame.thunderStrikes) { if (frame.thunderStrikes == initialStrikes) -10f else time }
    val t = if (still) 0f else time
    val description = if (active.isEmpty()) "A quiet dusk landscape" else "Dusk landscape with " + active.joinToString(", ") { it.label.lowercase() }

    Canvas(modifier.semantics { contentDescription = description }) {
        val k = size.width / 390f
        val h = size.height / k
        withTransform({ scale(k, k, pivot = Offset.Zero) }) {
            drawRect(
                Brush.verticalGradient(listOf(Palette.DuskTop, Palette.DuskMid, Palette.DuskLow), 0f, h),
                Offset.Zero, Size(390f, h),
            )
            drawStars(t, h)
            drawMoon(h)
            if (showMochi) {
                withTransform({ translate(258f, h * 0.2f - 6f); scale(0.62f, 0.62f, pivot = Offset.Zero) }) {
                    val breath = 1f + 0.03f * sin(t * 2.2f)
                    scale(1f, breath, pivot = Offset(60f, 96f)) {
                        drawCharacter(Species.MOCHI, Color.White, Color(0xFFE4DDF7), sleeping = true, mood = 3f, blink = 1f, wobble = t, headphones = false, withBody = false)
                    }
                }
                drawZs(t, h)
            }
            drawClouds(t, h)
            if (a(SoundId.BROWN_NOISE) > 0.01f) drawWind(t, h, a(SoundId.BROWN_NOISE).coerceAtMost(1f))
            if (a(SoundId.BIRDS) > 0.01f) drawBirds(t, h, a(SoundId.BIRDS).coerceAtMost(1f))

            val rain = maxOf(a(SoundId.RAIN), a(SoundId.DOWNPOUR), a(SoundId.TENT), a(SoundId.CAR)).coerceIn(0f, 1f)
            if (rain > 0.01f) drawRain(t, h, rain, heavy = a(SoundId.DOWNPOUR).coerceIn(0f, 1f))
            val sinceFlash = t - flashAt
            if (SoundId.THUNDER in active && sinceFlash in 0f..0.6f) {
                val flicker = if (sinceFlash < 0.1f) 1f else if (sinceFlash < 0.18f) 0.2f else (0.6f - sinceFlash) / 0.42f * 0.7f
                drawRect(Color(0xFFF1ECFF).copy(alpha = flicker * 0.4f * (1f - dim)), Offset.Zero, Size(390f, h))
                drawBolt(h, flicker)
            }

            drawHills(h)
            pop(a(SoundId.TENT), Offset(96f, h - 92f)) { drawTent(t, h) }
            pop(a(SoundId.CAR), Offset(300f, h - 64f)) { drawCar(h) }
            pop(a(SoundId.STREAM), Offset(205f, h - 60f)) { drawStream(t, h) }
            pop(a(SoundId.WATER_DROPS), Offset(330f, h - 118f)) { drawPond(t, h) }
            pop(a(SoundId.CAMPFIRE), Offset(160f, h - 40f)) { drawFire(t, h) }
            if (a(SoundId.NIGHT_FOREST) > 0.01f) {
                drawTrees(h, a(SoundId.NIGHT_FOREST).coerceAtMost(1f))
                drawFireflies(t, h, a(SoundId.NIGHT_FOREST).coerceAtMost(1f))
            }
            if (a(SoundId.FOCUS_MUSIC) > 0.01f) drawNotes(t, h, a(SoundId.FOCUS_MUSIC).coerceAtMost(1f))
            if (dim > 0f) drawRect(Palette.Night.copy(alpha = dim), Offset.Zero, Size(390f, h))
        }
    }
}

private inline fun DrawScope.pop(amount: Float, pivot: Offset, block: DrawScope.() -> Unit) {
    if (amount <= 0.01f) return
    scale(amount, amount, pivot) { block() }
}

private val starSpots = listOf(
    24f to 0.06f, 70f to 0.16f, 118f to 0.05f, 160f to 0.22f, 206f to 0.09f, 236f to 0.3f, 352f to 0.07f,
    40f to 0.32f, 96f to 0.4f, 190f to 0.36f, 374f to 0.34f, 140f to 0.12f, 316f to 0.42f,
)

private fun DrawScope.drawStars(t: Float, h: Float) {
    starSpots.forEachIndexed { i, (x, fy) ->
        val alpha = 0.3f + 0.7f * (0.5f + 0.5f * sin(t * 1.5f + i * 1.9f))
        val r = if (i % 3 == 0) 1.9f else 1.3f
        drawCircle(Color.White.copy(alpha = alpha), r, Offset(x, h * fy))
    }
}

private fun DrawScope.drawMoon(h: Float) {
    val c = Offset(300f, h * 0.2f + 26f)
    drawCircle(Brush.radialGradient(listOf(Palette.Moon.copy(alpha = 0.35f), Color.Transparent), c, 110f), 110f, c)
    val moon = Path().apply { addOval(Rect(c, 46f)) }
    val bite = Path().apply { addOval(Rect(Offset(c.x - 22f, c.y - 20f), 40f)) }
    drawPath(Path.combine(PathOperation.Difference, moon, bite), Palette.Moon)
}

private fun DrawScope.drawZs(t: Float, h: Float) {
    for (i in 0..2) {
        val p = ((t / 3.6f) + i / 3f) % 1f
        val x = 304f + 22f * p + i * 4f
        val y = h * 0.2f - 10f - 46f * p
        val s = 4f + 4f * p
        val alpha = if (p < 0.2f) p / 0.2f else 1f - (p - 0.2f) / 0.8f
        drawPath(
            Path().apply { moveTo(x, y); lineTo(x + s, y); lineTo(x, y + s); lineTo(x + s, y + s) },
            Color.White.copy(alpha = alpha * 0.9f),
            style = Stroke(1.6f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawClouds(t: Float, h: Float) {
    fun cloud(x: Float, y: Float, w: Float, alpha: Float) {
        val c = Color.White.copy(alpha = alpha)
        drawRoundRect(c, Offset(x, y), Size(w, w * 0.22f), androidx.compose.ui.geometry.CornerRadius(w * 0.11f))
        drawCircle(c, w * 0.17f, Offset(x + w * 0.35f, y + 2f))
        drawCircle(c, w * 0.13f, Offset(x + w * 0.62f, y + 4f))
    }
    val drift = (t * 6f) % 520f
    cloud(-140f + drift, h * 0.36f, 120f, 0.14f)
    cloud(150f + drift * 0.6f % 400f - 60f, h * 0.48f, 90f, 0.1f)
}

private fun DrawScope.drawHills(h: Float) {
    fun hill(base: Float, amp: Float, phase: Float, color: Color) {
        val path = Path().apply {
            moveTo(0f, h)
            lineTo(0f, base)
            var x = 0f
            while (x <= 390f) {
                lineTo(x, base - amp * (0.6f * sin(x / 70f + phase) + 0.4f * sin(x / 33f + phase * 2f)))
                x += 6f
            }
            lineTo(390f, h)
            close()
        }
        drawPath(path, color)
    }
    hill(h - 120f, 18f, 0.4f, Palette.HillFar)
    hill(h - 84f, 16f, 2.1f, Palette.HillMid)
    hill(h - 44f, 12f, 4.2f, Palette.HillNear)
}

private fun DrawScope.drawWind(t: Float, h: Float, a: Float) {
    val shift = 50f * sin(t / 8f)
    translate(shift, 0f) {
        drawPath(
            Path().apply { moveTo(-20f, h * 0.55f); cubicTo(80f, h * 0.5f, 160f, h * 0.6f, 260f, h * 0.55f); cubicTo(320f, h * 0.52f, 360f, h * 0.54f, 410f, h * 0.55f) },
            Color.White.copy(alpha = 0.28f * a), style = Stroke(2f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawBirds(t: Float, h: Float, a: Float) {
    for (i in 0..2) {
        val x = (t * 22f + i * 130f) % 460f - 40f
        val y = h * (0.3f + 0.05f * i) + 6f * sin(t * 1.3f + i)
        val flap = 3f * sin(t * 8f + i)
        drawPath(
            Path().apply { moveTo(x, y); quadraticBezierTo(x + 6f, y - 5f - flap, x + 12f, y); quadraticBezierTo(x + 18f, y - 5f - flap, x + 24f, y) },
            Palette.HillNear.copy(alpha = a), style = Stroke(2f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawRain(t: Float, h: Float, a: Float, heavy: Float) {
    val count = 26 + (18 * heavy).toInt()
    val speed = 260f + 160f * heavy
    for (i in 0 until count) {
        val x = (i * 151f) % 400f
        val y = ((t * speed + i * 97f) % (h + 40f)) - 20f
        val len = 12f + 8f * heavy
        drawLine(Color.White.copy(alpha = 0.45f * a), Offset(x, y), Offset(x - 3f, y + len), 1.4f, StrokeCap.Round)
    }
}

private fun DrawScope.drawBolt(h: Float, alpha: Float) {
    val x = 150f
    val y = h * 0.12f
    drawPath(
        Path().apply { moveTo(x + 16f, y); lineTo(x, y + 40f); lineTo(x + 12f, y + 40f); lineTo(x + 2f, y + 80f); lineTo(x + 30f, y + 30f); lineTo(x + 17f, y + 30f); lineTo(x + 26f, y); close() },
        Palette.Moon.copy(alpha = alpha.coerceIn(0f, 1f)),
    )
}

private fun DrawScope.drawTent(t: Float, h: Float) {
    val bx = 60f
    val by = h - 92f
    drawPath(Path().apply { moveTo(bx + 36f, by - 52f); lineTo(bx + 74f, by); lineTo(bx - 2f, by); close() }, Color(0xFFFFB98E))
    drawPath(Path().apply { moveTo(bx + 36f, by - 52f); lineTo(bx + 74f, by); lineTo(bx + 52f, by); close() }, Color(0xFFF29A68))
    val lamp = 0.8f + 0.2f * sin(t * 3f)
    drawCircle(Brush.radialGradient(listOf(Palette.Moon.copy(alpha = 0.45f * lamp), Color.Transparent), Offset(bx + 36f, by - 10f), 42f), 42f, Offset(bx + 36f, by - 10f))
    drawPath(Path().apply { moveTo(bx + 36f, by - 30f); lineTo(bx + 46f, by); lineTo(bx + 26f, by); close() }, Palette.Moon.copy(alpha = lamp))
}

private fun DrawScope.drawCar(h: Float) {
    val bx = 268f
    val by = h - 64f
    drawRoundRect(Color(0xFFF6C3D6), Offset(bx, by - 18f), Size(64f, 16f), androidx.compose.ui.geometry.CornerRadius(6f))
    drawRoundRect(Color(0xFFF6C3D6), Offset(bx + 12f, by - 30f), Size(36f, 16f), androidx.compose.ui.geometry.CornerRadius(7f))
    drawRoundRect(Palette.Moon.copy(alpha = 0.9f), Offset(bx + 16f, by - 27f), Size(12f, 9f), androidx.compose.ui.geometry.CornerRadius(3f))
    drawRoundRect(Palette.Moon.copy(alpha = 0.9f), Offset(bx + 31f, by - 27f), Size(13f, 9f), androidx.compose.ui.geometry.CornerRadius(3f))
    drawCircle(Palette.Night, 6f, Offset(bx + 14f, by - 2f))
    drawCircle(Palette.Night, 6f, Offset(bx + 50f, by - 2f))
}

private fun DrawScope.drawStream(t: Float, h: Float) {
    drawPath(
        Path().apply { moveTo(232f, h - 126f); cubicTo(250f, h - 100f, 190f, h - 80f, 214f, h - 50f); cubicTo(228f, h - 30f, 200f, h - 12f, 206f, h + 4f) },
        Color(0xFFBDE6EE), style = Stroke(5f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), -t * 36f)),
    )
}

private fun DrawScope.drawPond(t: Float, h: Float) {
    val c = Offset(330f, h - 118f)
    drawOval(Color(0xFF8EC5F2).copy(alpha = 0.8f), Offset(c.x - 30f, c.y - 7f), Size(60f, 14f))
    for (i in 0..1) {
        val p = ((t / 2.4f) + i * 0.5f) % 1f
        val w = 14f + 34f * p
        drawOval(Color.White.copy(alpha = 0.8f * (1f - p)), Offset(c.x - w / 2f, c.y - w / 8f), Size(w, w / 4f), style = Stroke(1.4f))
    }
}

private fun DrawScope.drawFire(t: Float, h: Float) {
    val c = Offset(160f, h - 48f)
    drawCircle(Brush.radialGradient(listOf(Color(0x88FFB547), Color.Transparent), c, 70f), 70f, c)
    val fy = 1f + 0.1f * sin(t * 17f) + 0.05f * sin(t * 29f)
    scale(1f - 0.05f * sin(t * 13f), fy, Offset(c.x, c.y + 8f)) {
        drawPath(Path().apply { moveTo(c.x, c.y - 22f); cubicTo(c.x + 10f, c.y - 8f, c.x + 12f, c.y - 2f, c.x + 10f, c.y + 4f); cubicTo(c.x + 8f, c.y + 10f, c.x - 8f, c.y + 10f, c.x - 10f, c.y + 4f); cubicTo(c.x - 12f, c.y - 2f, c.x - 8f, c.y - 8f, c.x, c.y - 22f); close() }, Color(0xFFFF9A6B))
        drawPath(Path().apply { moveTo(c.x, c.y - 10f); cubicTo(c.x + 6f, c.y - 2f, c.x + 6f, c.y + 2f, c.x + 5f, c.y + 5f); cubicTo(c.x + 3f, c.y + 8f, c.x - 3f, c.y + 8f, c.x - 5f, c.y + 5f); cubicTo(c.x - 6f, c.y + 2f, c.x - 6f, c.y - 2f, c.x, c.y - 10f); close() }, Palette.Moon)
    }
    drawLine(Color(0xFF6B4E33), Offset(c.x - 14f, c.y + 12f), Offset(c.x + 14f, c.y + 7f), 4f, StrokeCap.Round)
    drawLine(Color(0xFF6B4E33), Offset(c.x - 14f, c.y + 7f), Offset(c.x + 14f, c.y + 12f), 4f, StrokeCap.Round)
    for (i in 0..3) {
        val p = ((t / 2.2f) + i * 0.27f) % 1f
        drawCircle(Color(0xFFFFD98F).copy(alpha = 1f - p), 1.8f, Offset(c.x + 10f * sin(i * 2f + p * 5f), c.y - 20f - 70f * p))
    }
}

private fun DrawScope.drawTrees(h: Float, a: Float) {
    val color = Palette.HillNear.copy(alpha = a)
    listOf(18f to 1f, 40f to 0.8f, 352f to 0.9f, 374f to 1.1f).forEach { (x, s) ->
        val base = h - 60f
        drawPath(Path().apply { moveTo(x, base - 70f * s); lineTo(x + 18f * s, base); lineTo(x - 18f * s, base); close() }, color)
    }
}

private fun DrawScope.drawFireflies(t: Float, h: Float, a: Float) {
    val spots = listOf(40f to 0.7f, 120f to 0.62f, 230f to 0.68f, 300f to 0.58f, 360f to 0.74f, 80f to 0.56f)
    spots.forEachIndexed { i, (x, fy) ->
        val glow = (0.2f + 0.8f * (0.5f + 0.5f * sin(t * 1.6f + i * 2.1f))) * a
        val p = Offset(x + 10f * sin(t * 0.7f + i), h * fy + 8f * sin(t * 0.9f + i * 1.7f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFF4FFB0).copy(alpha = glow), Color.Transparent), p, 10f), 10f, p)
    }
}

private fun DrawScope.drawNotes(t: Float, h: Float, a: Float) {
    for (i in 0..2) {
        val p = ((t / 3f) + i / 3f) % 1f
        val x = 236f - 30f * p + i * 8f
        val y = h * 0.28f - 50f * p
        val c = Color.White.copy(alpha = (1f - p) * a)
        drawCircle(c, 3.2f, Offset(x, y))
        drawLine(c, Offset(x + 3f, y), Offset(x + 3f, y - 12f), 1.6f, StrokeCap.Round)
    }
}
