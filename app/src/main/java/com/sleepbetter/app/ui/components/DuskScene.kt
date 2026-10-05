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
import androidx.compose.ui.graphics.PathMeasure
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
            val rain = maxOf(a(SoundId.RAIN), a(SoundId.DOWNPOUR), a(SoundId.TENT), a(SoundId.CAR)).coerceIn(0f, 1f)
            val heavy = a(SoundId.DOWNPOUR).coerceIn(0f, 1f)
            val stormy = maxOf(rain, a(SoundId.THUNDER).coerceIn(0f, 1f))
            // Wind sets the rain's slant and gusts now and then; brown noise makes it windier.
            val windy = a(SoundId.BROWN_NOISE).coerceIn(0f, 1f)
            val wind = 0.1f + 0.12f * heavy + 0.1f * windy + 0.06f * sin(t * 0.23f) + 0.04f * sin(t * 0.61f + 1f)
            drawStars(t, h, 1f - 0.85f * stormy)
            drawMoon(h)
            // Rain clouds dim the whole sky.
            if (stormy > 0.01f) drawRect(Palette.Night.copy(alpha = 0.28f * stormy), Offset.Zero, Size(390f, h))
            if (showMochi) {
                // Toffee dozes against the moon, Mochi curled up on top.
                withTransform({ translate(214f, h * 0.2f + 8f); scale(0.56f, 0.56f, pivot = Offset.Zero) }) {
                    val breath = 1f + 0.03f * sin(t * 2.2f + 1.3f)
                    scale(1f, breath, pivot = Offset(60f, 96f)) {
                        drawCharacter(Species.TOFFEE, Species.TOFFEE.body, Species.TOFFEE.shade, sleeping = true, mood = 3f, blink = 1f, wobble = t + 2f, headphones = false, withBody = false)
                    }
                }
                withTransform({ translate(258f, h * 0.2f - 6f); scale(0.62f, 0.62f, pivot = Offset.Zero) }) {
                    val breath = 1f + 0.03f * sin(t * 2.2f)
                    scale(1f, breath, pivot = Offset(60f, 96f)) {
                        drawCharacter(Species.MOCHI, Color.White, Color(0xFFE4DDF7), sleeping = true, mood = 3f, blink = 1f, wobble = t, headphones = false, withBody = false)
                    }
                }
                drawZs(t, h)
            }
            drawClouds(t, h, stormy)
            if (windy > 0.01f) drawWind(t, h, windy)
            if (a(SoundId.BIRDS) > 0.01f) drawBirds(t, h, a(SoundId.BIRDS).coerceAtMost(1f) * (1f - 0.6f * rain))

            val sinceFlash = time - flashAt
            if (SoundId.THUNDER in active && sinceFlash in 0f..0.9f) {
                drawLightning(h, sinceFlash, seed = (flashAt * 1000f).toInt(), dim = dim)
            }

            drawHills(h)
            // Rain falls in front of the far hills and splashes on them.
            if (rain > 0.01f) drawRain(t, h, rain, heavy, wind)
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
            // Mist rises off wet ground.
            if (rain > 0.01f) drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = (0.06f + 0.08f * heavy) * rain)), h * 0.55f, h),
                Offset(0f, h * 0.55f), Size(390f, h * 0.45f),
            )
            if (dim > 0f) drawRect(Palette.Night.copy(alpha = dim), Offset.Zero, Size(390f, h))
        }
    }
}

private inline fun DrawScope.pop(amount: Float, pivot: Offset, block: DrawScope.() -> Unit) {
    if (amount <= 0.01f) return
    scale(amount, amount, pivot) { block() }
}

/** Cheap, stable pseudo-random number in 0..1 for drop [i] in its [j]th life: no two drops or cycles alike. */
private fun rnd(i: Int, j: Int = 0): Float {
    var x = i * 374761393 + j * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0x7fffffff) / 2147483647f
}

private val starSpots = listOf(
    24f to 0.06f, 70f to 0.16f, 118f to 0.05f, 160f to 0.22f, 206f to 0.09f, 236f to 0.3f, 352f to 0.07f,
    40f to 0.32f, 96f to 0.4f, 190f to 0.36f, 374f to 0.34f, 140f to 0.12f, 316f to 0.42f,
)

/** Stars hold steady and only now and then twinkle, each at its own slow pace. */
private fun DrawScope.drawStars(t: Float, h: Float, visible: Float) {
    if (visible <= 0.02f) return
    starSpots.forEachIndexed { i, (x, fy) ->
        val rate = 0.5f + 1.3f * rnd(i, 7)
        val twinkle = sin(t * rate + i * 1.9f).let { it * it * it } // mostly calm, short dips
        val alpha = (0.75f + 0.25f * twinkle) * visible
        val r = if (i % 3 == 0) 1.9f else 1.3f
        drawCircle(Color.White.copy(alpha = alpha * 0.25f), r * 2.6f, Offset(x, h * fy))
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

/**
 * Soft clouds built from overlapping puffs with fuzzy edges. On a clear night
 * a few pale wisps drift by; when it rains, heavy grey clouds roll in.
 */
private fun DrawScope.drawClouds(t: Float, h: Float, storm: Float) {
    fun cloud(cx: Float, cy: Float, w: Float, color: Color, alpha: Float, seed: Int) {
        for (p in 0 until 6) {
            val px = cx + (rnd(seed, p) - 0.5f) * w
            val py = cy + (rnd(seed, p + 10) - 0.5f) * w * 0.18f
            val r = w * (0.18f + 0.16f * rnd(seed, p + 20)) * (1f + 0.04f * sin(t * 0.3f + p))
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.5f), Color.Transparent), Offset(px, py), r), r, Offset(px, py))
        }
    }
    val span = 390f + 320f
    // Pale wisps, always.
    for (i in 0..2) {
        val speed = 4f + 3f * rnd(i, 3)
        val x = ((t * speed + rnd(i, 4) * span) % span) - 160f
        cloud(x, h * (0.3f + 0.12f * i), 130f + 40f * rnd(i, 5), Color.White, 0.1f * (1f - 0.5f * storm), i)
    }
    if (storm <= 0.01f) return
    // A band of rain clouds across the top.
    val grey = Color(0xFF8E87AE)
    for (i in 0..6) {
        val speed = 7f + 5f * rnd(i, 13)
        val x = ((t * speed + i * span / 7f) % span) - 160f
        cloud(x, h * (0.02f + 0.08f * rnd(i, 14)), 170f + 60f * rnd(i, 15), grey, 0.55f * storm, 100 + i)
    }
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

/** Gusts: faint curved streaks sweep across now and then, never in lockstep. */
private fun DrawScope.drawWind(t: Float, h: Float, a: Float) {
    for (i in 0..2) {
        val period = 5f + 3f * rnd(i, 30)
        val local = t + rnd(i, 31) * period
        val cycle = (local / period).toInt()
        val p = (local % period) / period
        val y = h * (0.35f + 0.3f * rnd(i, 40 + cycle))
        val x = -120f + p * 640f
        val fade = sin(p * Math.PI.toFloat()).coerceAtLeast(0f)
        val bend = 10f + 10f * rnd(i, 50 + cycle)
        drawPath(
            Path().apply { moveTo(x, y); cubicTo(x + 40f, y - bend, x + 80f, y + bend * 0.6f, x + 130f, y - 2f) },
            Color.White.copy(alpha = 0.22f * a * fade), style = Stroke(1.6f, cap = StrokeCap.Round),
        )
    }
}

/** Birds fly at their own speeds, flap in bursts and glide in between. */
private fun DrawScope.drawBirds(t: Float, h: Float, a: Float) {
    if (a <= 0.01f) return
    for (i in 0..2) {
        val speed = 18f + 10f * rnd(i, 60)
        val x = (t * speed + i * 150f) % 480f - 50f
        val y = h * (0.28f + 0.06f * i) + 8f * sin(t * 0.7f + i * 2f)
        val burst = sin(t * 0.9f + i * 1.7f) > 0f // flapping or gliding
        val flap = if (burst) 4f * sin(t * 11f + i) else 1f
        val s = 0.8f + 0.3f * rnd(i, 61)
        drawPath(
            Path().apply {
                moveTo(x, y); quadraticBezierTo(x + 6f * s, y - (4f + flap) * s, x + 12f * s, y)
                quadraticBezierTo(x + 18f * s, y - (4f + flap) * s, x + 24f * s, y)
            },
            Palette.HillNear.copy(alpha = a), style = Stroke(1.8f, cap = StrokeCap.Round),
        )
    }
}

private class RainLayer(val count: Int, val speed: Float, val length: Float, val width: Float, val alpha: Float, val ground: ClosedFloatingPointRange<Float>?)

/**
 * Rain in three depths. Far drops are many, faint, short and slower; near
 * drops are few, long and fast. Every drop has its own speed, length and a
 * new random spot each time it falls, all leaning with the gusting wind.
 * Mid and near drops splash where they land: a ripple and two droplets.
 */
private fun DrawScope.drawRain(t: Float, h: Float, a: Float, heavy: Float, wind: Float) {
    val layers = listOf(
        RainLayer(46, 430f, 10f, 0.9f, 0.26f, null),
        RainLayer(30, 640f, 16f, 1.2f, 0.4f, (h - 118f)..(h - 50f)),
        RainLayer(16, 900f, 26f, 1.7f, 0.55f, (h - 40f)..(h - 6f)),
    )
    val splashTime = 0.24f
    layers.forEachIndexed { li, layer ->
        val n = (layer.count * a * (0.55f + 0.9f * heavy)).toInt()
        val speedK = 1f + 0.35f * heavy
        for (i in 0 until n) {
            val id = li * 1000 + i
            val sp = layer.speed * speedK * (0.85f + 0.3f * rnd(id, 1))
            val ground = layer.ground?.let { it.start + (it.endInclusive - it.start) * rnd(id, 2) } ?: (h + 30f)
            val fallTime = (ground + 40f) / sp
            val period = fallTime + if (layer.ground != null) splashTime else 0f
            val local = t + rnd(id, 3) * period * 7f
            val cycle = (local / period).toInt()
            val u = local - cycle * period
            val x0 = rnd(id, 100 + cycle) * 440f - 30f
            val len = layer.length * (0.75f + 0.5f * rnd(id, 4)) * (1f + 0.4f * heavy)
            if (u < fallTime) {
                val y = -40f + u * sp
                val x = x0 + wind * (y + 40f)
                // A faint tail and a brighter head read as motion blur.
                drawLine(Color.White.copy(alpha = layer.alpha * 0.35f * a), Offset(x - wind * len, y - len), Offset(x, y), layer.width, StrokeCap.Round)
                drawLine(Color.White.copy(alpha = layer.alpha * a), Offset(x - wind * len * 0.35f, y - len * 0.35f), Offset(x, y), layer.width, StrokeCap.Round)
            } else if (layer.ground != null) {
                val p = (u - fallTime) / splashTime
                val x = x0 + wind * (ground + 40f)
                val big = if (li == 2) 1.4f else 0.9f
                val w = (2f + 9f * p) * big
                drawOval(Color.White.copy(alpha = 0.5f * (1f - p) * a), Offset(x - w, ground - w * 0.22f), Size(w * 2f, w * 0.44f), style = Stroke(0.9f))
                val hop = 4f * p * (1f - p) * 9f * big
                drawCircle(Color.White.copy(alpha = 0.6f * (1f - p) * a), 0.9f * big, Offset(x - 5f * p * big, ground - hop))
                drawCircle(Color.White.copy(alpha = 0.6f * (1f - p) * a), 0.8f * big, Offset(x + 6f * p * big, ground - hop * 0.8f))
            }
        }
    }
}

/**
 * Lightning as it really looks: a jagged bolt that forks, new every strike,
 * with a double flicker (flash, dark, flash) that lights the clouds and then fades.
 */
private fun DrawScope.drawLightning(h: Float, s: Float, seed: Int, dim: Float) {
    val f = when {
        s < 0.05f -> 1f
        s < 0.11f -> 0.2f
        s < 0.17f -> 0.85f
        s < 0.22f -> 0.3f
        else -> ((0.9f - s) / 0.68f).coerceIn(0f, 1f) * 0.45f
    } * (1f - dim)
    if (f <= 0.01f) return
    val startX = 70f + 250f * rnd(seed, 1)
    val top = Offset(startX, h * 0.06f)
    // Sky lights up around the bolt, and a little everywhere.
    drawRect(Color(0xFFE9E4FF).copy(alpha = 0.22f * f), Offset.Zero, Size(390f, h))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFF3F0FF).copy(alpha = 0.55f * f), Color.Transparent), top, 260f), 260f, top)
    val bolt = Path()
    val branches = mutableListOf<Path>()
    var p = top
    bolt.moveTo(p.x, p.y)
    val steps = 12
    val endY = h * (0.5f + 0.15f * rnd(seed, 2))
    for (k in 1..steps) {
        val y = top.y + (endY - top.y) * k / steps
        val x = p.x + (rnd(seed, 10 + k) - 0.5f) * 30f
        p = Offset(x, y)
        bolt.lineTo(x, y)
        if (k in 3..8 && rnd(seed, 30 + k) > 0.72f) {
            val br = Path().apply { moveTo(x, y) }
            var q = p
            val dir = if (rnd(seed, 40 + k) > 0.5f) 1f else -1f
            repeat(4) { m ->
                q = Offset(q.x + dir * (8f + 10f * rnd(seed, 50 + k * 5 + m)), q.y + 10f + 8f * rnd(seed, 70 + k * 5 + m))
                br.lineTo(q.x, q.y)
            }
            branches += br
        }
    }
    val glow = Color(0xFFCFC4FF)
    drawPath(bolt, glow.copy(alpha = 0.35f * f), style = Stroke(9f, cap = StrokeCap.Round))
    branches.forEach { drawPath(it, glow.copy(alpha = 0.25f * f), style = Stroke(5f, cap = StrokeCap.Round)) }
    drawPath(bolt, Color.White.copy(alpha = f), style = Stroke(2.2f, cap = StrokeCap.Round))
    branches.forEach { drawPath(it, Color.White.copy(alpha = 0.7f * f), style = Stroke(1.2f, cap = StrokeCap.Round)) }
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

/** A stream with light glinting on it: highlights of different lengths slide downstream at their own speeds. */
private fun DrawScope.drawStream(t: Float, h: Float) {
    val path = Path().apply { moveTo(232f, h - 126f); cubicTo(250f, h - 100f, 190f, h - 80f, 214f, h - 50f); cubicTo(228f, h - 30f, 200f, h - 12f, 206f, h + 4f) }
    drawPath(path, Color(0xFF7FB8D8), style = Stroke(9f, cap = StrokeCap.Round))
    drawPath(path, Color(0xFFA9DCEA), style = Stroke(5f, cap = StrokeCap.Round))
    val measure = PathMeasure().apply { setPath(path, false) }
    val total = measure.length
    for (i in 0 until 7) {
        val speed = 26f + 22f * rnd(i, 80)
        val len = 6f + 12f * rnd(i, 81)
        val start = ((t * speed + rnd(i, 82) * total) % (total + len)) - len
        val seg = Path()
        measure.getSegment(start.coerceAtLeast(0f), (start + len).coerceAtMost(total), seg, true)
        val shimmer = 0.5f + 0.5f * sin(t * 3f + i * 1.3f)
        drawPath(seg, Color.White.copy(alpha = 0.5f + 0.4f * shimmer), style = Stroke(1.6f, cap = StrokeCap.Round))
    }
}

/** A pond where drops land at random spots and their rings spread and slow down. */
private fun DrawScope.drawPond(t: Float, h: Float) {
    val c = Offset(330f, h - 118f)
    drawOval(Color(0xFF8EC5F2).copy(alpha = 0.8f), Offset(c.x - 30f, c.y - 7f), Size(60f, 14f))
    drawOval(Color.White.copy(alpha = 0.18f), Offset(c.x - 22f, c.y - 5f), Size(26f, 4f))
    for (i in 0..2) {
        val period = 1.8f + 1.4f * rnd(i, 90)
        val local = t + rnd(i, 91) * period
        val cycle = (local / period).toInt()
        val p = (local % period) / period
        val ease = 1f - (1f - p) * (1f - p) * (1f - p) // fast then slowing, like real ripples
        val spot = Offset(c.x + (rnd(i, 92 + cycle) - 0.5f) * 30f, c.y + (rnd(i, 93 + cycle) - 0.5f) * 5f)
        for (ring in 0..1) {
            val w = (4f + 24f * ease) * (1f - ring * 0.35f)
            drawOval(Color.White.copy(alpha = 0.75f * (1f - p) * (1f - ring * 0.4f)), Offset(spot.x - w / 2f, spot.y - w / 8f), Size(w, w / 4f), style = Stroke(1.1f))
        }
    }
}

/**
 * A campfire with three flame tongues that flicker on their own, a glow
 * that breathes with them, and embers that drift up on the heat and fade.
 */
private fun DrawScope.drawFire(t: Float, h: Float) {
    val c = Offset(160f, h - 48f)
    fun noise(x: Float, k: Int) = 0.5f * sin(x * 7.3f + k) + 0.3f * sin(x * 13.1f + k * 2.1f) + 0.2f * sin(x * 23.7f + k * 3.7f)
    val glowR = 62f + 10f * noise(t, 1)
    drawCircle(Brush.radialGradient(listOf(Color(0x99FFB547), Color(0x33FF8A3D), Color.Transparent), c, glowR), glowR, c)
    fun tongue(dx: Float, height: Float, width: Float, color: Color, k: Int) {
        val sway = 4f * noise(t * 0.8f, k)
        val hh = height * (1f + 0.18f * noise(t, k + 5))
        val tip = Offset(c.x + dx + sway, c.y + 6f - hh)
        drawPath(
            Path().apply {
                moveTo(c.x + dx - width, c.y + 6f)
                cubicTo(c.x + dx - width * 1.1f, c.y - hh * 0.3f, tip.x - width * 0.4f, tip.y + hh * 0.35f, tip.x, tip.y)
                cubicTo(tip.x + width * 0.4f, tip.y + hh * 0.35f, c.x + dx + width * 1.1f, c.y - hh * 0.3f, c.x + dx + width, c.y + 6f)
                close()
            },
            color,
        )
    }
    tongue(-6f, 20f, 7f, Color(0xFFFF7A4D), 1)
    tongue(6f, 22f, 7f, Color(0xFFFF7A4D), 2)
    tongue(0f, 30f, 10f, Color(0xFFFF9A6B), 3)
    tongue(-2f, 18f, 6f, Color(0xFFFFC46B), 4)
    tongue(1f, 11f, 4f, Palette.Moon, 6)
    drawLine(Color(0xFF6B4E33), Offset(c.x - 15f, c.y + 12f), Offset(c.x + 15f, c.y + 7f), 4.5f, StrokeCap.Round)
    drawLine(Color(0xFF6B4E33), Offset(c.x - 15f, c.y + 7f), Offset(c.x + 15f, c.y + 12f), 4.5f, StrokeCap.Round)
    for (i in 0..6) {
        val period = 1.6f + 1.4f * rnd(i, 120)
        val local = t + rnd(i, 121) * period
        val cycle = (local / period).toInt()
        val p = (local % period) / period
        val drift = (rnd(i, 122 + cycle) - 0.5f) * 30f
        val x = c.x + drift * p + 3f * sin(t * 4f + i)
        val y = c.y - 18f - 80f * p
        val glow = (1f - p) * (0.6f + 0.4f * sin(t * 15f + i))
        drawCircle(Color(0xFFFFD98F).copy(alpha = glow.coerceIn(0f, 1f)), 1.1f + 0.8f * (1f - p), Offset(x, y))
    }
}

private fun DrawScope.drawTrees(h: Float, a: Float) {
    val color = Palette.HillNear.copy(alpha = a)
    listOf(18f to 1f, 40f to 0.8f, 352f to 0.9f, 374f to 1.1f).forEach { (x, s) ->
        val base = h - 60f
        drawPath(Path().apply { moveTo(x, base - 70f * s); lineTo(x + 18f * s, base); lineTo(x - 18f * s, base); close() }, color)
    }
}

/** Fireflies wander on smooth, looping paths and blink: a soft rise, a glow, then dark for a while. */
private fun DrawScope.drawFireflies(t: Float, h: Float, a: Float) {
    val spots = listOf(40f to 0.7f, 120f to 0.62f, 230f to 0.68f, 300f to 0.58f, 360f to 0.74f, 80f to 0.56f, 180f to 0.75f)
    spots.forEachIndexed { i, (x, fy) ->
        val rate = 0.25f + 0.2f * rnd(i, 130)
        val phase = (t * rate + rnd(i, 131)) % 1f
        val blink = if (phase < 0.3f) sin(phase / 0.3f * Math.PI.toFloat()) else 0f
        val glow = (0.08f + 0.92f * blink) * a
        val p = Offset(
            x + 14f * sin(t * 0.37f + i) + 6f * sin(t * 1.1f + i * 2.3f),
            h * fy + 10f * sin(t * 0.29f + i * 1.7f) + 4f * sin(t * 0.9f + i),
        )
        drawCircle(Brush.radialGradient(listOf(Color(0xFFF4FFB0).copy(alpha = glow * 0.8f), Color.Transparent), p, 12f), 12f, p)
        drawCircle(Color(0xFFFBFFD8).copy(alpha = glow), 1.3f, p)
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
