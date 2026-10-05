@file:Suppress("DEPRECATION") // quadraticBezierTo: renamed quadraticTo in newer Compose.

package com.sleepbetter.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/*
 * Illustrated dinos: layered vector art (body, belly, thighs, arms, tail,
 * head) in the classic cute-dino style, each layer animated on its own.
 * The paths live in DinoPaths, generated from the same source as the
 * design previews; colours come from a palette per species.
 */

internal enum class DinoGroup { STATIC, TAIL, TORSO, ARM_L, ARM_R, HEAD, BODY, LEG_FRONT, LEG_BACK, ARM }
internal enum class DinoInk { BODY, SHADE, SPOT, BELLY, STRIPE, PLATE, CHEEK, LINE, WHITE, SHADOW }

internal class DinoPart(val group: DinoGroup, d: String, val fill: DinoInk?, val stroke: DinoInk?, val width: Float, val alpha: Float) {
    val path: Path by lazy { PathParser().parsePathString(d).toPath() }
}

/** The colours of one dino. */
class DinoPalette(
    val body: Color,
    val shade: Color,
    val spot: Color,
    val belly: Color,
    val stripe: Color,
    val plate: Color,
    val cheek: Color,
)

private val Ink = Color(0xFF2A2440)
private val Silhouette = Color(0xFFE6E0EA)
private val SilhouetteLine = Color(0xFFCFC6D8)

/** Palettes for the illustrated dinos; null for characters drawn the older way. */
fun Species.dinoPalette(): DinoPalette? = when (this) {
    Species.PICO -> DinoPalette(Color(0xFF8BD46E), Color(0xFF6DBB55), Color(0xFF5DA847), Color(0xFFFFE9A8), Color(0xFFEFC970), Color(0xFFFFC94D), Color(0xFFFF9F6B))
    Species.LULU -> DinoPalette(Color(0xFFFFB8CF), Color(0xFFF296B4), Color(0xFFE77FA2), Color(0xFFFFF1F6), Color(0xFFF7C9D9), Color(0xFFC7B2FA), Color(0xFFFF7FA0))
    Species.REX -> DinoPalette(Color(0xFFFF9E86), Color(0xFFE57E68), Color(0xFFE06F5A), Color(0xFFFFE6C9), Color(0xFFF2C79C), Color(0xFFFFD27A), Color(0xFFFF7F7F))
    Species.TRIKE -> DinoPalette(Color(0xFFFFCF96), Color(0xFFEDB06F), Color(0xFFE09A55), Color(0xFFFFF3DF), Color(0xFFF2D3A6), Color(0xFFFF9F6B), Color(0xFFFF8F6B))
    Species.STEGO -> DinoPalette(Color(0xFF7FD3CF), Color(0xFF5DBBB7), Color(0xFF4FA9A5), Color(0xFFFFF4D6), Color(0xFFEFD9A0), Color(0xFFFF9DB0), Color(0xFFFF8FA3))
    Species.BRONTO -> DinoPalette(Color(0xFFA9B8F5), Color(0xFF8797E3), Color(0xFF7584D4), Color(0xFFFFF0D6), Color(0xFFF2D7A8), Color(0xFFFFD98A), Color(0xFFFF9AA8))
    Species.PTERO -> DinoPalette(Color(0xFFD7B8F2), Color(0xFFBC97E3), Color(0xFFA884D6), Color(0xFFFFF0F6), Color(0xFFF2CFE2), Color(0xFF9C7BD6), Color(0xFFFF8FB0))
    Species.ANKY -> DinoPalette(Color(0xFFD3B98F), Color(0xFFB89C70), Color(0xFFA3895F), Color(0xFFFFF1D6), Color(0xFFEBD3A6), Color(0xFF8E7656), Color(0xFFFF9A8A))
    else -> null
}

/** What the dino is doing this frame. */
internal class DinoMotion(
    val breath: Float = 0f,
    val tilt: Float = 0f,
    val wag: Float = 0f,
    val blink: Float = 1f,
    val wave: Float = 0f,
    val sleeping: Boolean = false,
    val mood: Float = 3f,
    val headphones: Boolean = false,
    val silhouette: Boolean = false,
)

private fun DrawScope.drawPart(part: DinoPart, pal: DinoPalette, silhouette: Boolean) {
    fun color(ink: DinoInk): Color = if (silhouette) {
        when (ink) {
            DinoInk.LINE -> SilhouetteLine
            DinoInk.SHADOW -> Ink.copy(alpha = 0.1f)
            else -> Silhouette
        }
    } else when (ink) {
        DinoInk.BODY -> pal.body
        DinoInk.SHADE -> pal.shade
        DinoInk.SPOT -> pal.spot
        DinoInk.BELLY -> pal.belly
        DinoInk.STRIPE -> pal.stripe
        DinoInk.PLATE -> pal.plate
        DinoInk.CHEEK -> pal.cheek
        DinoInk.LINE -> Ink
        DinoInk.WHITE -> Color.White
        DinoInk.SHADOW -> Ink.copy(alpha = 0.15f)
    }
    if (silhouette && (part.fill == DinoInk.CHEEK || part.fill == DinoInk.SPOT || part.alpha < 1f)) return
    part.fill?.let { drawPath(part.path, color(it).copy(alpha = color(it).alpha * part.alpha)) }
    part.stroke?.let { drawPath(part.path, color(it), style = Stroke(part.width, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
}

/**
 * The sitting dino on its 200-unit artboard. Breathing, a head tilt, a
 * wagging tail, blinking and a wave come from [m]; the face follows mood.
 */
internal fun DrawScope.drawDinoFront(species: Species, pal: DinoPalette, m: DinoMotion) {
    val parts = DinoPaths.front
    val firstHead = parts.indexOfFirst { it.group == DinoGroup.HEAD }
    parts.forEachIndexed { i, part ->
        if (i == firstHead) withTransform({ rotate(m.tilt, Offset(100f, 122f)) }) { behindHead(species, pal, m) }
        withTransform({
            when (part.group) {
                DinoGroup.TAIL -> rotate(m.wag, Offset(134f, 160f))
                DinoGroup.TORSO -> scale(1f + 0.02f * m.breath, 1f - 0.025f * m.breath, Offset(100f, 188f))
                DinoGroup.ARM_R -> rotate(m.wave, Offset(118f, 128f))
                DinoGroup.HEAD -> rotate(m.tilt, Offset(100f, 122f))
                else -> Unit
            }
        }) { drawPart(part, pal, m.silhouette) }
    }
    if (!m.silhouette) withTransform({ rotate(m.tilt, Offset(100f, 122f)) }) { frontFace(species, m) }
}

/** Things behind the head: Trixie's frill, Petra's crest. */
private fun DrawScope.behindHead(species: Species, pal: DinoPalette, m: DinoMotion) {
    val line = if (m.silhouette) SilhouetteLine else Ink
    val stroke = Stroke(4f, join = StrokeJoin.Round)
    when (species) {
        Species.TRIKE -> {
            val c = Offset(100f, 70f)
            for (k in 0..8) {
                val a = PI.toFloat() * (1.02f + k * 0.12f)
                val p = Offset(c.x + 74f * kotlin.math.cos(a), c.y + 60f * sin(a))
                drawCircle(if (m.silhouette) Silhouette else pal.plate, 13f, p)
                drawCircle(line, 13f, p, style = stroke)
            }
            val frill = Path().apply { addOval(Rect(c, 72f)) }
            drawPath(frill, if (m.silhouette) Silhouette else pal.plate.copy(alpha = 1f))
            drawPath(frill, line, style = stroke)
        }
        Species.PTERO -> {
            val crest = Path().apply { moveTo(118f, 36f); quadraticBezierTo(160f, 2f, 196f, 8f); quadraticBezierTo(170f, 26f, 146f, 46f); close() }
            drawPath(crest, if (m.silhouette) Silhouette else pal.plate)
            drawPath(crest, line, style = stroke)
        }
        else -> Unit
    }
}

/** Eyes, mouth and each dino's little extra, on the sitting pose. */
private fun DrawScope.frontFace(species: Species, m: DinoMotion) {
    val eyes = listOf(Offset(78f, 80f), Offset(122f, 80f))
    val thick = Stroke(4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when {
        m.sleeping -> eyes.forEach { e ->
            drawPath(Path().apply { moveTo(e.x - 12f, e.y + 2f); quadraticBezierTo(e.x, e.y + 11f, e.x + 12f, e.y + 2f) }, Ink, style = thick)
        }
        m.mood > 3.5f -> eyes.forEach { e ->
            drawPath(Path().apply { moveTo(e.x - 12f, e.y + 5f); quadraticBezierTo(e.x, e.y - 11f, e.x + 12f, e.y + 5f) }, Ink, style = thick)
        }
        else -> {
            scale(1f, m.blink, Offset(100f, 80f)) {
                eyes.forEach { e ->
                    drawOval(Ink, Offset(e.x - 13f, e.y - 15f), Size(26f, 30f))
                    drawCircle(Color.White, 5.5f, Offset(e.x + 5f, e.y - 7f))
                    drawCircle(Color.White, 2.5f, Offset(e.x - 5f, e.y + 7f))
                }
            }
            if (m.mood < 1.5f) {
                // Tired, a little sad: inner ends of the lids raised.
                val droop = (1.5f - m.mood) * 6f
                drawLine(Ink, Offset(64f, 62f + droop * 0.4f), Offset(88f, 58f - droop), 4f, StrokeCap.Round)
                drawLine(Ink, Offset(136f, 62f + droop * 0.4f), Offset(112f, 58f - droop), 4f, StrokeCap.Round)
            }
        }
    }
    when {
        m.sleeping -> drawOval(Ink, Offset(96f, 102f), Size(8f, 6f))
        m.mood > 3.5f -> {
            val open = Path().apply { moveTo(86f, 98f); lineTo(114f, 98f); quadraticBezierTo(114f, 116f, 100f, 116f); quadraticBezierTo(86f, 116f, 86f, 98f); close() }
            drawPath(open, Ink)
            drawOval(Color(0xFFFF7A8A), Offset(92f, 106f), Size(16f, 8f))
        }
        else -> {
            val d = 12f * ((m.mood - 2f) / 2f).coerceIn(-1f, 1f)
            drawPath(Path().apply { moveTo(88f, 101f); quadraticBezierTo(100f, 101f + d, 112f, 101f) }, Ink, style = thick)
        }
    }
    when (species) {
        Species.LULU -> {
            // Lulu still wears a piece of her eggshell.
            val shell = Path().apply {
                moveTo(56f, 46f)
                listOf(64f to 34f, 72f to 46f, 80f to 33f, 88f to 46f, 96f to 33f, 104f to 46f, 112f to 33f, 120f to 46f, 128f to 33f, 136f to 46f, 144f to 36f, 148f to 46f).forEach { (x, y) -> lineTo(x, y) }
                cubicTo(150f, 18f, 126f, 6f, 102f, 6f)
                cubicTo(78f, 6f, 52f, 18f, 56f, 46f)
                close()
            }
            drawPath(shell, Color(0xFFFFFBF2))
            drawPath(shell, Ink, style = Stroke(4f, join = StrokeJoin.Round))
            drawCircle(Color(0xFF8BD46E), 4f, Offset(84f, 20f))
            drawCircle(Color(0xFFC7B2FA), 3f, Offset(110f, 16f))
            drawCircle(Color(0xFFFFC94D), 3.5f, Offset(124f, 26f))
        }
        Species.REX -> if (!m.sleeping && m.mood <= 3.5f) {
            // Two little fangs.
            listOf(93f, 107f).forEach { x ->
                val fang = Path().apply { moveTo(x - 3.5f, 103f); lineTo(x + 3.5f, 103f); lineTo(x, 110f); close() }
                drawPath(fang, Color.White)
                drawPath(fang, Ink, style = Stroke(2f, join = StrokeJoin.Round))
            }
        }
        Species.TRIKE -> {
            // Three soft horns.
            val horn = Color(0xFFFFF6E6)
            listOf(
                Path().apply { moveTo(70f, 50f); quadraticBezierTo(62f, 24f, 56f, 16f); quadraticBezierTo(76f, 26f, 86f, 46f); close() },
                Path().apply { moveTo(130f, 50f); quadraticBezierTo(138f, 24f, 144f, 16f); quadraticBezierTo(124f, 26f, 114f, 46f); close() },
                Path().apply { moveTo(93f, 66f); quadraticBezierTo(98f, 52f, 100f, 48f); quadraticBezierTo(102f, 52f, 107f, 66f); close() },
            ).forEach {
                drawPath(it, horn)
                drawPath(it, Ink, style = Stroke(3.5f, join = StrokeJoin.Round))
            }
        }
        else -> Unit
    }
    if (m.headphones) {
        val band = Color(0xFF6C55D9)
        val arc = Path().apply { moveTo(40f, 88f); cubicTo(36f, 14f, 164f, 14f, 160f, 88f) }
        drawPath(arc, Ink, style = Stroke(12f, cap = StrokeCap.Round))
        drawPath(arc, band, style = Stroke(7f, cap = StrokeCap.Round))
        listOf(Offset(26f, 72f), Offset(152f, 72f)).forEach { o ->
            drawRoundRect(band, o, Size(22f, 34f), androidx.compose.ui.geometry.CornerRadius(11f))
            drawRoundRect(Ink, o, Size(22f, 34f), androidx.compose.ui.geometry.CornerRadius(11f), style = Stroke(4f))
        }
    }
}

/**
 * The walking dino (facing left) on its 210 x 200 artboard. [phase] runs
 * 0..1 per stride: legs swap, arm swings, the body bobs and the tail sways.
 */
internal fun DrawScope.drawDinoSide(pal: DinoPalette, phase: Float, blink: Float) {
    val s = sin(phase * 2f * PI.toFloat())
    val bob = abs(sin(phase * 2f * PI.toFloat())) * 4f
    DinoPaths.side.forEach { part ->
        withTransform({
            if (part.group != DinoGroup.STATIC) translate(top = -bob)
            when (part.group) {
                DinoGroup.LEG_FRONT -> rotate(16f * s, Offset(100f, 146f))
                DinoGroup.LEG_BACK -> rotate(-16f * s, Offset(128f, 146f))
                DinoGroup.ARM -> rotate(-12f - 10f * s, Offset(92f, 110f))
                DinoGroup.TAIL -> rotate(-4f * s, Offset(138f, 158f))
                DinoGroup.HEAD -> rotate(1.5f * s, Offset(100f, 90f))
                else -> Unit
            }
        }) { drawPart(part, pal, silhouette = false) }
    }
    // One big glossy eye.
    withTransform({ translate(top = -bob); rotate(1.5f * s, Offset(100f, 90f)) }) {
        scale(1f, blink, Offset(96f, 52f)) {
            drawOval(Ink, Offset(86f, 40f), Size(20f, 24f))
            drawCircle(Color.White, 4.2f, Offset(99.5f, 47f))
            drawCircle(Color.White, 2f, Offset(93f, 57f))
        }
    }
}

/**
 * A dino strolling back and forth along a strip. At each end it turns to
 * face you and waves before setting off again. Respects reduce motion.
 */
@Composable
fun DinoWalker(species: Species, modifier: Modifier = Modifier, height: Dp = 86.dp) {
    val pal = species.dinoPalette() ?: return
    val still = rememberReduceMotion()
    val clock by rememberClock()
    Box(modifier.fillMaxWidth().height(height)) {
        Canvas(Modifier.fillMaxWidth().height(height).semantics { contentDescription = "${species.description} out for a walk" }) {
            val t = if (still) 0f else clock
            val k = size.height / 200f
            val dinoW = 210f * k
            val track = (size.width - dinoW).coerceAtLeast(1f)
            val walk = 9f
            val pause = 2.6f
            val cycle = 2f * (walk + pause)
            val u = t % cycle
            val leg = when {
                u < walk -> 0
                u < walk + pause -> 1
                u < 2 * walk + pause -> 2
                else -> 3
            }
            val blink = if ((t % 3.7f) < 0.12f) 0.1f else 1f
            when (leg) {
                0, 2 -> {
                    val p = ((if (leg == 0) u else u - walk - pause) / walk).coerceIn(0f, 1f)
                    val eased = p * p * (3f - 2f * p)
                    val goingRight = leg == 0
                    val x = if (goingRight) track * eased else track * (1f - eased)
                    // The art faces left, so mirror it when walking right.
                    withTransform({
                        translate(left = x)
                        scale(k, k, Offset.Zero)
                        if (goingRight) scale(-1f, 1f, Offset(105f, 100f))
                    }) { drawDinoSide(pal, phase = (t / 1.1f) % 1f, blink = blink) }
                }
                else -> {
                    val x = if (leg == 1) track else 0f
                    val w = ((u - (if (leg == 1) walk else 2 * walk + pause)) / pause).coerceIn(0f, 1f)
                    val waveAmt = sin(w * PI.toFloat()).coerceAtLeast(0f)
                    val wave = -130f * waveAmt + 14f * sin(t * 14f) * waveAmt
                    withTransform({ translate(left = x + 5f * k); scale(k, k, Offset.Zero) }) {
                        drawDinoFront(species, pal, DinoMotion(breath = 0.5f + 0.5f * sin(t * 2f), tilt = 4f * sin(t * 1.7f), wag = 7f * sin(t * 4f), blink = blink, wave = wave, mood = 3.6f))
                    }
                }
            }
        }
    }
}
