@file:Suppress("DEPRECATION") // quadraticBezierTo: renamed quadraticTo in newer Compose; this works in all versions.

package com.sleepbetter.app.ui.tonight

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.sleepbetter.app.audio.EngineFrame
import com.sleepbetter.app.ui.components.drawCat
import com.sleepbetter.app.ui.components.drawFox
import com.sleepbetter.app.ui.components.drawKoala
import com.sleepbetter.app.ui.components.drawOwl
import com.sleepbetter.app.ui.components.drawPanda
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.core.audio.SoundId
import com.sleepbetter.core.sleep.Visitor
import kotlin.math.sin

/**
 * The Night Island: every active sound appears as an object that springs into
 * place. Rain falls only while rain plays, and lightning flashes exactly when
 * the engine fires a thunder strike. Designed in a 390×470 space and scaled.
 *
 * [dim] (0..1) darkens everything for sleep mode.
 */
@Composable
fun IslandScene(
    active: Set<SoundId>,
    frame: EngineFrame,
    time: Float,
    visitors: List<Visitor>,
    modifier: Modifier = Modifier,
    dim: Float = 0f,
    still: Boolean = false,
) {
    val appear = SoundId.entries.map { id ->
        animateFloatAsState(if (id in active) 1f else 0f, spring(dampingRatio = 0.45f, stiffness = 260f), label = id.name).value
    }
    fun a(id: SoundId) = appear[id.ordinal]

    // Lightning: note the time whenever the engine's strike counter changes.
    val initialStrikes = remember { frame.thunderStrikes }
    val flashAt = remember(frame.thunderStrikes) { if (frame.thunderStrikes == initialStrikes) -10f else time }
    val measurer = rememberTextMeasurer()
    val t = if (still) 0f else time
    val description = buildString {
        append("Your island")
        if (active.isNotEmpty()) append(" with ").append(active.joinToString(", ") { it.label.lowercase() })
    }

    Canvas(modifier.semantics { contentDescription = description }) {
        val k = size.width / 390f
        withTransform({ scale(k, k, pivot = Offset.Zero) }) {
            drawStars(t)
            drawMoon()
            if (a(SoundId.BROWN_NOISE) > 0.01f) drawWind(t, a(SoundId.BROWN_NOISE))

            val cloud = maxOf(a(SoundId.RAIN), a(SoundId.DOWNPOUR), a(SoundId.THUNDER)).coerceIn(0f, 1.2f)
            if (cloud > 0.01f) {
                drawRain(t, cloud.coerceAtMost(1f), heavy = a(SoundId.DOWNPOUR))
                drawCloud(t, cloud, dark = maxOf(a(SoundId.DOWNPOUR), a(SoundId.THUNDER)))
            }
            val sinceFlash = t - flashAt
            if (SoundId.THUNDER in active && sinceFlash in 0f..0.6f) {
                val flicker = if (sinceFlash < 0.12f) 1f else if (sinceFlash < 0.2f) 0.15f else (0.6f - sinceFlash) / 0.4f * 0.7f
                drawRect(Color(0xFFDCE3FF).copy(alpha = flicker * 0.45f * (1f - dim * 0.8f)), Offset(-200f, -200f), Size(800f, 900f))
                drawBolt(flicker)
            }

            drawIsland()
            drawTree()
            if (Visitor.KOALA in visitors) translate(60f, 236f) { scale(28f / 120f, pivot = Offset.Zero) { drawKoala(true, 1f) } }
            popIn(a(SoundId.BIRDS), Offset(80f, 236f)) { drawBirds(t) }
            popIn(a(SoundId.NIGHT_FOREST), Offset(107f, 286f)) {
                translate(92f, 250f) { scale(30f / 120f, pivot = Offset.Zero) { drawOwl(1f) } }
            }
            if (a(SoundId.NIGHT_FOREST) > 0.01f) drawFireflies(t, a(SoundId.NIGHT_FOREST).coerceAtMost(1f))
            popIn(a(SoundId.CAR), Offset(173f, 320f)) { drawCar() }
            popIn(a(SoundId.TENT), Offset(277f, 328f)) { drawTent(t) }
            if (Visitor.CAT in visitors) translate(300f, 312f) { scale(34f / 120f, pivot = Offset.Zero) { drawCat(true, 1f) } }
            popIn(a(SoundId.FOCUS_MUSIC), Offset(321f, 350f)) { drawRadio(t, measurer) }
            popIn(a(SoundId.WATER_DROPS), Offset(93f, 347f)) { drawPond(t) }
            popIn(a(SoundId.CAMPFIRE), Offset(146f, 352f)) { drawFire(t) }
            if (Visitor.EMBER in visitors) {
                translate(84f, 318f) { scale(40f / 120f, pivot = Offset.Zero) { drawFox(true, 1f) } }
            }
            popIn(a(SoundId.STREAM), Offset(350f, 330f)) { drawWaterfall(t) }

            // Pip, asleep in the middle, breathing.
            val breath = 1f + 0.035f * sin(t * 2.4f)
            translate(168f, 290f) {
                scale(76f / 120f, 76f / 120f * breath, pivot = Offset(0f, 100f)) { drawPanda(sleeping = true, headphones = false, blink = 1f) }
            }
            drawZs(t, measurer)

            if (dim > 0f) drawRect(Color.Black.copy(alpha = dim * 0.55f), Offset(-200f, -200f), Size(800f, 900f))
        }
    }
}

/** Scales the block around [pivot] by [amount]; the spring overshoot makes objects "pop". */
private inline fun DrawScope.popIn(amount: Float, pivot: Offset, block: DrawScope.() -> Unit) {
    if (amount <= 0.01f) return
    scale(amount, amount, pivot) { block() }
}

private val stars = listOf(
    26f to 30f, 70f to 96f, 128f to 20f, 196f to 60f, 250f to 18f, 360f to 130f, 30f to 200f, 352f to 230f,
    150f to 150f, 300f to 160f, 90f to 170f, 220f to 110f,
)

private fun DrawScope.drawStars(t: Float) {
    stars.forEachIndexed { i, (x, y) ->
        val alpha = 0.25f + 0.75f * (0.5f + 0.5f * sin(t * 1.7f + i * 1.3f))
        drawCircle(Color(0xFFF3F1FF).copy(alpha = alpha), 1.6f, Offset(x, y))
    }
}

private fun DrawScope.drawMoon() {
    drawCircle(Brush.radialGradient(listOf(Color(0x33F2E3B8), Color.Transparent), Offset(328f, 53f), 70f), 70f, Offset(328f, 53f))
    val moon = Path().apply { addOval(Rect(Offset(328f, 53f), 31f)) }
    val bite = Path().apply { addOval(Rect(Offset(340f, 42f), 28f)) }
    drawPath(Path.combine(PathOperation.Difference, moon, bite), Palette.Moonlight)
}

private fun DrawScope.drawWind(t: Float, a: Float) {
    val shift = 60f * sin(t / 9f)
    val alpha = 0.5f * a.coerceAtMost(1f)
    translate(shift, 0f) {
        drawPath(
            Path().apply { moveTo(10f, 180f); cubicTo(90f, 140f, 160f, 220f, 240f, 180f); cubicTo(300f, 150f, 360f, 160f, 390f, 180f) },
            Color(0xFF8E8BD6).copy(alpha = alpha), style = Stroke(2f, cap = StrokeCap.Round),
        )
        drawPath(
            Path().apply { moveTo(-20f, 205f); cubicTo(70f, 175f, 150f, 235f, 230f, 205f); cubicTo(300f, 180f, 350f, 190f, 400f, 205f) },
            Color(0xFF6F6CBF).copy(alpha = alpha), style = Stroke(1.5f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawCloud(t: Float, a: Float, dark: Float) {
    val color = if (dark > 0.5f) Color(0xFF3A3F7A) else Color(0xFF4A4F8C)
    val dy = 5f * sin(t / 1.1f)
    scale(a, a, Offset(195f, 120f)) {
        translate(0f, dy) {
            drawRoundRect(color, Offset(80f, 106f), Size(230f, 34f), CornerRadius(20f))
            drawOval(color, Offset(114f, 76f), Size(74f, 50f))
            drawOval(color, Offset(180f, 68f), Size(96f, 62f))
        }
    }
}

private fun DrawScope.drawRain(t: Float, a: Float, heavy: Float) {
    val count = 14 + (8 * heavy.coerceIn(0f, 1f)).toInt()
    val speed = if (heavy > 0.5f) 300f else 210f
    for (i in 0 until count) {
        val x = 74f + (i * 241f % 246f)
        val y = 128f + ((t * speed + i * 67f) % 190f)
        val len = if (heavy > 0.5f) 22f else 15f
        drawLine(Color(0xFF9DB2FF).copy(alpha = 0.75f * a), Offset(x, y), Offset(x - 2f, y + len), 1.5f, StrokeCap.Round)
    }
}

private fun DrawScope.drawBolt(alpha: Float) {
    drawPath(
        Path().apply { moveTo(256f, 128f); lineTo(240f, 162f); lineTo(252f, 162f); lineTo(244f, 198f); lineTo(268f, 154f); lineTo(255f, 154f); lineTo(264f, 128f); close() },
        Color(0xFFFFE9A6).copy(alpha = alpha.coerceIn(0f, 1f)),
    )
}

private fun DrawScope.drawIsland() {
    translate(0f, 292f) {
        drawPath(
            Path().apply {
                moveTo(28f, 40f)
                cubicTo(40f, 90f, 110f, 120f, 150f, 160f)
                cubicTo(170f, 182f, 182f, 196f, 195f, 196f)
                cubicTo(208f, 196f, 220f, 182f, 240f, 160f)
                cubicTo(280f, 120f, 350f, 90f, 362f, 40f)
                close()
            },
            Palette.Clay,
        )
        drawLine(Color(0xFF4A3A7A), Offset(60f, 60f), Offset(160f, 140f), 6f, StrokeCap.Round)
        drawOval(Palette.Moss, Offset(28f, 0f), Size(334f, 80f))
        drawOval(Palette.MossLight, Offset(37f, 2f), Size(316f, 64f))
    }
}

private fun DrawScope.drawTree() {
    drawRoundRect(Color(0xFF5B3F33), Offset(70f, 266f), Size(14f, 66f), CornerRadius(6f))
    drawLine(Color(0xFF5B3F33), Offset(77f, 290f), Offset(102f, 276f), 7f, StrokeCap.Round)
    drawCircle(Color(0xFF2F6B4F), 44f, Offset(77f, 242f))
    drawCircle(Color(0xFF3A7D5C), 26f, Offset(58f, 234f))
    drawCircle(Color(0xFF3A7D5C), 22f, Offset(94f, 226f))
}

private fun DrawScope.drawBirds(t: Float) {
    drawOval(Color(0xFFF2A6A0), Offset(49f, 215f), Size(18f, 14f))
    drawCircle(Color(0xFFF2A6A0), 5f, Offset(64f, 217f))
    drawOval(Color(0xFFA6C8F2), Offset(90f, 208f), Size(16f, 12f))
    drawCircle(Color(0xFFA6C8F2), 4.5f, Offset(92f, 210f))
    val fx = 140f + 30f * sin(t / 1.9f)
    val fy = 150f - 10f * sin(t / 1.3f)
    drawPath(
        Path().apply { moveTo(fx, fy + 8f); quadraticBezierTo(fx + 6f, fy, fx + 13f, fy + 8f); quadraticBezierTo(fx + 20f, fy, fx + 26f, fy + 8f) },
        Color(0xFFE7E5FF), style = Stroke(2f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawFireflies(t: Float, a: Float) {
    val spots = listOf(30f to 300f, 128f to 232f, 330f to 270f, 200f to 250f, 290f to 380f, 60f to 380f)
    spots.forEachIndexed { i, (x, y) ->
        val glow = (0.15f + 0.85f * (0.5f + 0.5f * sin(t * 1.6f + i * 2.1f))) * a
        val dx = 10f * sin(t * 0.7f + i)
        val dy = 8f * sin(t * 0.9f + i * 1.7f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFEAFF9A).copy(alpha = glow), Color.Transparent), Offset(x + dx, y + dy), 9f), 9f, Offset(x + dx, y + dy))
    }
}

private fun DrawScope.drawCar() {
    translate(140f, 280f) {
        drawPath(Path().apply { moveTo(4f, 30f); lineTo(4f, 20f); lineTo(12f, 8f); lineTo(46f, 8f); lineTo(58f, 20f); lineTo(62f, 20f); lineTo(62f, 30f); close() }, Color(0xFFE58A4E))
        drawPath(Path().apply { moveTo(15f, 11f); lineTo(27f, 11f); lineTo(27f, 20f); lineTo(10f, 20f); close(); moveTo(31f, 11f); lineTo(44f, 11f); lineTo(53f, 20f); lineTo(31f, 20f); close() }, Color(0xFFBFD6FF))
        drawCircle(Color(0xFF1B1840), 6f, Offset(17f, 31f))
        drawCircle(Color(0xFF1B1840), 6f, Offset(49f, 31f))
        drawCircle(Palette.InkMuted, 2.4f, Offset(17f, 31f))
        drawCircle(Palette.InkMuted, 2.4f, Offset(49f, 31f))
    }
}

private fun DrawScope.drawTent(t: Float) {
    translate(232f, 252f) {
        drawPath(Path().apply { moveTo(45f, 4f); lineTo(86f, 72f); lineTo(4f, 72f); close() }, Color(0xFFD98C6A))
        drawPath(Path().apply { moveTo(45f, 4f); lineTo(86f, 72f); lineTo(62f, 72f); close() }, Color(0xFFB96F55))
        val lamp = 0.85f + 0.15f * sin(t * 3.1f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFF4B860).copy(alpha = 0.35f * lamp), Color.Transparent), Offset(45f, 60f), 40f), 40f, Offset(45f, 60f))
        drawPath(Path().apply { moveTo(45f, 30f); lineTo(58f, 72f); lineTo(32f, 72f); close() }, Color(0xFFFFD98A).copy(alpha = lamp))
    }
}

private fun DrawScope.drawRadio(t: Float, measurer: androidx.compose.ui.text.TextMeasurer) {
    translate(304f, 322f) {
        drawRoundRect(Color(0xFF8C7CF0), Offset(2f, 6f), Size(30f, 20f), CornerRadius(5f))
        drawCircle(Color(0xFF1B1840), 5f, Offset(12f, 16f))
        drawRoundRect(Color(0xFFF3F1FF), Offset(20f, 12f), Size(8f, 3f), CornerRadius(1.5f))
    }
    for (i in 0..1) {
        val p = ((t / 3f) + i * 0.5f) % 1f
        drawText(
            measurer, if (i == 0) "♪" else "♫",
            topLeft = Offset(314f + i * 10f - 14f * p, 300f - 46f * p),
            style = TextStyle(color = Color(0xFFF4B860).copy(alpha = (1f - p)), fontSize = 14.sp),
        )
    }
}

private fun DrawScope.drawPond(t: Float) {
    drawOval(Color(0xFF3F6FB0), Offset(58f, 336f), Size(70f, 22f))
    drawOval(Color(0xFF5684C4), Offset(64f, 339f), Size(58f, 13f))
    for (i in 0..1) {
        val p = ((t / 2.6f) + i * 0.5f) % 1f
        val w = 22f * (0.2f + 1.4f * p)
        drawOval(Color(0xFFCFE0FF).copy(alpha = 0.9f * (1f - p)), Offset(93f - w / 2f, 345f - w / 4.4f), Size(w, w / 2.2f), style = Stroke(1.5f))
    }
}

private fun DrawScope.drawFire(t: Float) {
    translate(124f, 304f) {
        drawCircle(Brush.radialGradient(listOf(Color(0x59F4B860), Color.Transparent), Offset(22f, 26f), 52f), 52f, Offset(22f, 26f))
        val fy = 1f + 0.08f * sin(t * 17f) + 0.05f * sin(t * 29f)
        val fx = 1f - 0.05f * sin(t * 13f)
        scale(fx, fy, Offset(22f, 42f)) {
            drawPath(Path().apply { moveTo(22f, 4f); cubicTo(30f, 16f, 34f, 22f, 34f, 30f); cubicTo(34f, 44f, 10f, 44f, 10f, 30f); cubicTo(10f, 22f, 16f, 18f, 22f, 4f); close() }, Color(0xFFF08A4B))
            drawPath(Path().apply { moveTo(22f, 18f); cubicTo(27f, 25f, 28f, 28f, 28f, 32f); cubicTo(28f, 40f, 16f, 40f, 16f, 32f); cubicTo(16f, 28f, 19f, 25f, 22f, 18f); close() }, Color(0xFFFFD98A))
        }
        drawLine(Color(0xFF5B3F33), Offset(6f, 44f), Offset(38f, 36f), 5f, StrokeCap.Round)
        drawLine(Color(0xFF5B3F33), Offset(6f, 36f), Offset(38f, 44f), 5f, StrokeCap.Round)
        for (i in 0..3) {
            val p = ((t / 2.4f) + i * 0.27f) % 1f
            drawCircle(Color(0xFFFFB35C).copy(alpha = 1f - p), 2f, Offset(22f + 10f * sin(i * 2f + p * 4f), 20f - 90f * p))
        }
    }
}

private fun DrawScope.drawWaterfall(t: Float) {
    translate(334f, 324f) {
        drawPath(
            Path().apply { moveTo(6f, 4f); cubicTo(14f, 10f, 18f, 30f, 18f, 150f) },
            Color(0xFF7FA8E8), style = Stroke(7f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), -t * 40f)),
        )
        drawPath(
            Path().apply { moveTo(12f, 6f); cubicTo(22f, 14f, 26f, 34f, 26f, 150f) },
            Color(0xFFB9D0F5), style = Stroke(3f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), -t * 57f)),
        )
    }
}

private fun DrawScope.drawZs(t: Float, measurer: androidx.compose.ui.text.TextMeasurer) {
    for (i in 0..1) {
        val p = ((t / 3.8f) + i * 0.5f) % 1f
        val alpha = if (p < 0.2f) p / 0.2f else 1f - (p - 0.2f) / 0.8f
        drawText(
            measurer, "z",
            topLeft = Offset(226f + 16f * p, 284f - 40f * p),
            style = TextStyle(color = Color(0xFFC8C6F0).copy(alpha = alpha), fontSize = (14f + 6f * p + i * 4f).sp),
        )
    }
}
